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
            vault = new HashMap<>();
            categories = new HashMap<>();
            deletedLogins = new HashMap<>();
            return;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            vault = (Map<String, Credential>) ois.readObject();
            loadCategories(ois);
            try {
                deletedLogins = (Map<String, Long>) ois.readObject();
            } catch (Exception e) {
                deletedLogins = new HashMap<>();
            }
        } catch (IOException | ClassNotFoundException e) {
            vault = new HashMap<>();
            categories = new HashMap<>();
            deletedLogins = new HashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadCategories(ObjectInputStream ois) {
        try {
            categories = (Map<String, Set<String>>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            categories = new HashMap<>();
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
            System.out.println("Sync file does not exist yet. Saving current vault as sync file...");
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(syncPath))) {
                oos.writeObject(vault);
                oos.writeObject(categories);
                oos.writeObject(deletedLogins);
            } catch (IOException e) {
                System.out.println("Error creating sync file: " + e.getMessage());
            }
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(syncFile))) {
            @SuppressWarnings("unchecked") Map<String, Credential> remoteVault = (Map<String, Credential>) ois.readObject();
            @SuppressWarnings("unchecked") Map<String, Set<String>> remoteCategories = (Map<String, Set<String>>) ois.readObject();
            @SuppressWarnings("unchecked") Map<String, Long> remoteDeleted = null;
            try { remoteDeleted = (Map<String, Long>) ois.readObject(); } catch (Exception e) { remoteDeleted = new HashMap<>(); }

            boolean changed = false;

            for (Map.Entry<String, Credential> entry : remoteVault.entrySet()) {
                String id = entry.getKey();
                Credential remoteCred = entry.getValue();
                
                if (deletedLogins.containsKey(id) && deletedLogins.get(id) >= remoteCred.getLastEditDate()) continue;

                if (!vault.containsKey(id) || vault.get(id).getLastEditDate() < remoteCred.getLastEditDate()) {
                    vault.put(id, remoteCred);
                    changed = true;
                }
            }

            for (Map.Entry<String, Long> entry : remoteDeleted.entrySet()) {
                String id = entry.getKey();
                long remoteDeleteTime = entry.getValue();
                
                if (vault.containsKey(id) && vault.get(id).getLastEditDate() < remoteDeleteTime) {
                    vault.remove(id);
                    deletedLogins.put(id, remoteDeleteTime);
                    changed = true;
                }
            }

            if (changed) {
                saveVault();
                try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(syncPath))) {
                    oos.writeObject(vault);
                    oos.writeObject(categories);
                    oos.writeObject(deletedLogins);
                }
                System.out.println("Sync completed successfully.");
            } else {
                System.out.println("Vaults are already in sync.");
            }

        } catch (Exception e) {
            System.out.println("Error during synchronization: " + e.getMessage());
        }
    }
}