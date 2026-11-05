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
            return;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            vault = (Map<String, Credential>) ois.readObject();
            loadCategories(ois);
        } catch (IOException | ClassNotFoundException e) {
            vault = new HashMap<>();
            categories = new HashMap<>();
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
        } catch (IOException e) {
            System.out.println("Error saving vault: " + e.getMessage());
        }
    }

    public void addLogin(String id, String username, String password) {
        vault.put(id, new Credential(username, password));
        saveVault();
    }

    public void deleteLogin(String id) {
        vault.remove(id);
        for (Set<String> ids : categories.values()) {
            ids.remove(id);
        }
        saveVault();
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
}