package app;
import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("java:S106")
public class VaultManager {
    private final String vaultPath;
    private final String password; // NUEVO: Atributo password para la Parte 1
    private Map<String, Credential> vault;
    private Map<String, Set<String>> categories;
    private Map<String, Long> deletedLogins;

    // NUEVO CONSTRUCTOR
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
        
        // MODIFICADO: Leer el Base64 y desencriptarlo
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
            System.out.println("Could not load or decrypt vault (wrong password?). Initializing empty.");
        }
        initEmptyVault();
    }

    private void initEmptyVault() {
        vault = new HashMap<>();
        categories = new HashMap<>();
        deletedLogins = new HashMap<>();
    }
    
    private void saveVault() {
        // MODIFICADO: Encriptar a Base64 y guardar el String
        try (PrintWriter writer = new PrintWriter(new FileWriter(vaultPath))) {
            Object[] dataToEncrypt = {vault, categories, deletedLogins};
            String encryptedBase64 = CryptoUtils.encryptObject(dataToEncrypt, password);
            writer.println(encryptedBase64);
        } catch (Exception e) {
            System.out.println("Error saving vault: " + e.getMessage());
        }
    }

    // --- MÉTODOS CRUD INTACTOS ---
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
 
    // --- PARTE 1: SYNC LOCAL CIFRADO ---
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
        // MODIFICADO: Leer el archivo local cifrado
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
            
            if (deletedLogins.containsKey(id) && deletedLogins.get(id) >= remoteCred.getLastEditDate()) {
                continue;
            }

            if (!vault.containsKey(id) || vault.get(id).getLastEditDate() < remoteCred.getLastEditDate()) {
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
            if (!categories.containsKey(catName)) {
                categories.put(catName, new HashSet<>(remoteIds));
                changed = true;
            } else if (categories.get(catName).addAll(remoteIds)) {
                changed = true;
            }
        }
        return changed;
    }

    private void writeSyncFile(File syncFile) {
        // MODIFICADO: Guardar cifrado
        try (PrintWriter writer = new PrintWriter(new FileWriter(syncFile))) {
            Object[] dataToEncrypt = {vault, categories, deletedLogins};
            String encryptedBase64 = CryptoUtils.encryptObject(dataToEncrypt, password);
            writer.println(encryptedBase64);
        } catch (Exception e) {
            System.out.println("Error writing sync file: " + e.getMessage());
        }
    }

    // --- PARTE 2: SYNC SERVER (INTACTO, PARA QUE AIMAR LO MODIFIQUE) ---
    @SuppressWarnings("unchecked")
    public void syncWithServer(String address, int port) {
        try (java.net.Socket socket = new java.net.Socket(address, port);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            
            out.writeObject(vault);
            out.writeObject(categories);
            out.writeObject(deletedLogins);
            out.flush();
            
            Map<String, Credential> remoteVault = (Map<String, Credential>) in.readObject();
            Map<String, Set<String>> remoteCategories = (Map<String, Set<String>>) in.readObject();
            Map<String, Long> remoteDeleted = (Map<String, Long>) in.readObject();
            
            this.vault = remoteVault;
            this.categories = remoteCategories;
            this.deletedLogins = remoteDeleted;
            saveVault();
            
            System.out.println("Synced successfully with server.");
        } catch (Exception e) {
            System.out.println("Error syncing with server: " + e.getMessage());
        }
    }
}