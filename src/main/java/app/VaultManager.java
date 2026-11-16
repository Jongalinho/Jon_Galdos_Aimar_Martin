package app;

import java.io.*;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("java:S106")
public class VaultManager {
    private final String vaultPath;
    private final String password;
    private Map<String, Credential> vault;
    private Map<String, Set<String>> categories;
    private Map<String, Long> deletedLogins;

    public VaultManager(String vaultPath, String password) {
        this.vaultPath = vaultPath;
        this.password = password;
        loadVault();
    }

    @SuppressWarnings("unchecked")
    private void loadVault() {
        File file = new File(vaultPath);
        if (!file.exists()) {
            initEmptyVault();
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String encryptedBase64 = reader.readLine();
            if (encryptedBase64 != null && !encryptedBase64.isEmpty()) {
                Object[] data = (Object[]) CryptoUtils.decryptObject(encryptedBase64, password);
                vault = (Map<String, Credential>) data[0];
                categories = (Map<String, Set<String>>) data[1];
                deletedLogins = (Map<String, Long>) data[2];
                return;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt vault: wrong password or corrupted file.", e);
        }
        throw new IllegalStateException("Failed to decrypt vault: empty vault file.");
    }

    private void initEmptyVault() {
        vault = new HashMap<>();
        categories = new HashMap<>();
        deletedLogins = new HashMap<>();
    }

    private void saveVault() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(vaultPath))) {
            Object[] dataToEncrypt = {vault, categories, deletedLogins};
            String encryptedBase64 = CryptoUtils.encryptObject(dataToEncrypt, password);
            writer.println(encryptedBase64);
        } catch (Exception e) {
            System.out.println("Error saving vault: " + e.getMessage());
        }
    }

    public void addLogin(String id, String username, String password) {
        vault.put(id, new Credential(username, password));
        saveVault();
    }

    public void deleteLogin(String id) {
        if (vault.remove(id) != null) {
            for (Set<String> ids : categories.values()) {
                ids.remove(id);
            }
            deletedLogins.put(id, System.currentTimeMillis());
            saveVault();
        }
    }

    public Credential getCredential(String id) {
        return vault.get(id);
    }

    public Iterable<String> getLoginIds() {
        return vault.keySet();
    }

    public boolean addCategory(String name) {
        if (categories.containsKey(name)) {
            return false;
        }
        categories.put(name, new HashSet<>());
        saveVault();
        return true;
    }

    public boolean deleteCategory(String name) {
        if (categories.remove(name) != null) {
            for (Credential cred : vault.values()) {
                cred.removeCategory(name);
            }
            saveVault();
            return true;
        }
        return false;
    }

    public Set<String> getCategories() {
        return categories.keySet();
    }

    public boolean addLoginToCategory(String id, String category) {
        if (!vault.containsKey(id) || !categories.containsKey(category)) {
            return false;
        }
        boolean added = categories.get(category).add(id);
        if (added) {
            vault.get(id).addCategory(category);
            saveVault();
        }
        return added;
    }

    public boolean removeLoginFromCategory(String id, String category) {
        if (!categories.containsKey(category)) {
            return false;
        }
        boolean removed = categories.get(category).remove(id);
        if (removed) {
            if (vault.containsKey(id)) {
                vault.get(id).removeCategory(category);
            }
            saveVault();
        }
        return removed;
    }

    public void syncWithLocalFile(String syncPath) {
        if (syncPath == null || syncPath.isEmpty()) {
            System.out.println("No sync file configured.");
            return;
        }

        File syncFile = new File(syncPath);
        if (!syncFile.exists()) {
            System.out.println("Sync file does not exist yet. Saving current vault as sync file...");
            writeSyncFile(syncFile);
            return;
        }

        processSyncFile(syncFile);
    }

    @SuppressWarnings("unchecked")
    private void processSyncFile(File syncFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(syncFile))) {
            String encryptedBase64 = reader.readLine();
            if (encryptedBase64 != null && !encryptedBase64.isEmpty()) {
                Object[] remoteData = (Object[]) CryptoUtils.decryptObject(encryptedBase64, password);

                Map<String, Credential> remoteVault = (Map<String, Credential>) remoteData[0];
                Map<String, Set<String>> remoteCategories = (Map<String, Set<String>>) remoteData[1];
                Map<String, Long> remoteDeleted = (Map<String, Long>) remoteData[2];

                boolean changed = mergeRemoteData(remoteVault, remoteCategories, remoteDeleted);

                if (changed) {
                    saveVault();
                    writeSyncFile(syncFile);
                    System.out.println("Sync completed successfully.");
                } else {
                    System.out.println("Vaults are already in sync.");
                }
            }
        } catch (Exception e) {
            System.out.println("Error during synchronization: " + e.getMessage());
        }
    }

    private boolean mergeRemoteData(Map<String, Credential> remoteVault,
                                    Map<String, Set<String>> remoteCategories,
                                    Map<String, Long> remoteDeleted) {
        boolean changed = mergeVaultCredentials(remoteVault);
        changed |= mergeRemoteDeletions(remoteDeleted);
        changed |= mergeRemoteCategories(remoteCategories);
        return changed;
    }

    private boolean mergeVaultCredentials(Map<String, Credential> remoteVault) {
        boolean changed = false;
        for (Map.Entry<String, Credential> entry : remoteVault.entrySet()) {
            String id = entry.getKey();
            Credential remoteCred = entry.getValue();

            Long deletedTime = deletedLogins.get(id);
            if (deletedTime != null && deletedTime >= remoteCred.getLastEditDate()) {
                continue;
            }

            Credential localCred = vault.get(id);
            if (localCred == null || localCred.getLastEditDate() < remoteCred.getLastEditDate()) {
                vault.put(id, remoteCred);
                changed = true;
            }
        }
        return changed;
    }

    private boolean mergeRemoteDeletions(Map<String, Long> remoteDeleted) {
        boolean changed = false;
        for (Map.Entry<String, Long> entry : remoteDeleted.entrySet()) {
            String id = entry.getKey();
            long remoteDeleteTime = entry.getValue();

            if (vault.containsKey(id) && vault.get(id).getLastEditDate() < remoteDeleteTime) {
                vault.remove(id);
                deletedLogins.put(id, remoteDeleteTime);
                changed = true;
            }
        }
        return changed;
    }

    private boolean mergeRemoteCategories(Map<String, Set<String>> remoteCategories) {
        boolean changed = false;
        for (Map.Entry<String, Set<String>> catEntry : remoteCategories.entrySet()) {
            String catName = catEntry.getKey();
            Set<String> remoteIds = catEntry.getValue();
            boolean isNew = categories.get(catName) == null;
            if (categories.computeIfAbsent(catName, k -> new HashSet<>()).addAll(remoteIds) || isNew) {
                changed = true;
            }
        }
        return changed;
    }

    private void writeSyncFile(File syncFile) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(syncFile))) {
            Object[] dataToEncrypt = {vault, categories, deletedLogins};
            String encryptedBase64 = CryptoUtils.encryptObject(dataToEncrypt, password);
            writer.println(encryptedBase64);
        } catch (Exception e) {
            System.out.println("Error writing sync file: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public void syncWithServer(String address, int port) {
        try (Socket socket = new Socket(address, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            Object[] dataToSend = {vault, categories, deletedLogins};
            String encryptedPayload = CryptoUtils.encryptObject(dataToSend, password);
            out.println(encryptedPayload);

            String encryptedResponse = in.readLine();
            if (encryptedResponse != null && !encryptedResponse.isEmpty()) {
                Object[] remoteData = (Object[]) CryptoUtils.decryptObject(encryptedResponse, password);
                this.vault = (Map<String, Credential>) remoteData[0];
                this.categories = (Map<String, Set<String>>) remoteData[1];
                this.deletedLogins = (Map<String, Long>) remoteData[2];
                saveVault();
                System.out.println("Synced successfully with server.");
            }
        } catch (Exception e) {
            System.out.println("Error syncing with server: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public String processEncryptedSyncPayload(String encryptedClientPayload) throws GeneralSecurityException {
        try {
            Object[] remoteData = (Object[]) CryptoUtils.decryptObject(encryptedClientPayload, password);
            Map<String, Credential> remoteVault = (Map<String, Credential>) remoteData[0];
            Map<String, Set<String>> remoteCategories = (Map<String, Set<String>>) remoteData[1];
            Map<String, Long> remoteDeleted = (Map<String, Long>) remoteData[2];

            if (mergeRemoteData(remoteVault, remoteCategories, remoteDeleted)) {
                saveVault();
            }

            Object[] updatedData = {vault, categories, deletedLogins};
            return CryptoUtils.encryptObject(updatedData, password);
        } catch (GeneralSecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralSecurityException("Failed to process encrypted sync payload.", e);
        }
    }
}