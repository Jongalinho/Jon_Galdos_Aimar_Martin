package app;
import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class VaultManager {
    private final String vaultPath;
    private Map<String, Credential> vault;
    private Map<String, Set<String>> categories;
    private Map<String, Long> deletedLogins;

    public VaultManager(String vaultPath) {
        this.vaultPath = vaultPath;
        loadVault();
    }

    @SuppressWarnings("unchecked")
    private void loadVault() {
        File file = new File(vaultPath);
        if (!file.exists()) {
            initEmptyVault();
            return;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            vault = (Map<String, Credential>) ois.readObject();
            categories = readMapSafely(ois, new HashMap<>());
            deletedLogins = readMapSafely(ois, new HashMap<>());
        } catch (IOException | ClassNotFoundException e) {
            initEmptyVault();
        }
    }

    private void initEmptyVault() {
        vault = new HashMap<>();
        categories = new HashMap<>();
        deletedLogins = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private <K, V> Map<K, V> readMapSafely(ObjectInputStream ois, Map<K, V> defaultValue) {
        try {
            return (Map<K, V>) ois.readObject();
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    private void saveVault() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(vaultPath))) {
            oos.writeObject(vault);
            oos.writeObject(categories);
            oos.writeObject(deletedLogins);
        } catch (IOException e) {
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
            createInitialSyncFile(syncFile);
            return;
        }

        processSyncFile(syncFile);
    }

    private void createInitialSyncFile(File syncFile) {
        System.out.println("Sync file does not exist yet. Saving current vault as sync file...");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(syncFile))) {
            oos.writeObject(vault);
            oos.writeObject(categories);
            oos.writeObject(deletedLogins);
        } catch (IOException e) {
            System.out.println("Error creating sync file: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void processSyncFile(File syncFile) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(syncFile))) {
            Map<String, Credential> remoteVault = (Map<String, Credential>) ois.readObject();
            Map<String, Set<String>> remoteCategories = readMapSafely(ois, new HashMap<>());
            Map<String, Long> remoteDeleted = readMapSafely(ois, new HashMap<>());

            boolean changed = mergeRemoteData(remoteVault, remoteCategories, remoteDeleted);

            if (changed) {
                saveVault();
                writeSyncFile(syncFile);
                System.out.println("Sync completed successfully.");
            } else {
                System.out.println("Vaults are already in sync.");
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
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(syncFile))) {
            oos.writeObject(vault);
            oos.writeObject(categories);
            oos.writeObject(deletedLogins);
        } catch (IOException e) {
            System.out.println("Error writing sync file: " + e.getMessage());
        }
    }

    public void syncWithServer(String address, int port) {
        try (java.net.Socket socket = new java.net.Socket(address, port);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            
            out.writeObject(vault);
            out.writeObject(categories);
            out.writeObject(deletedLogins);
            out.flush();
            
            @SuppressWarnings("unchecked")
            Map<String, Credential> remoteVault = (Map<String, Credential>) in.readObject();
            @SuppressWarnings("unchecked")
            Map<String, Set<String>> remoteCategories = (Map<String, Set<String>>) in.readObject();
            @SuppressWarnings("unchecked")
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