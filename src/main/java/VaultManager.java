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
            try {
                // Intenta cargar las categorías si el archivo ya tiene el nuevo formato
                categories = (Map<String, Set<String>>) ois.readObject();
            } catch (EOFException e) {
                // Si es un archivo del Día 1, no tendrá categorías guardadas aún
                categories = new HashMap<>();
            }
        } catch (IOException | ClassNotFoundException e) {
            vault = new HashMap<>();
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
        // Al borrar el login, lo quitamos también de cualquier categoría
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

    // --- MÉTODOS DE CATEGORÍAS PEDIDOS POR AIMAR ---

    public boolean addCategory(String name) {
        if (categories.containsKey(name)) {
            return false; // La categoría ya existe
        }
        categories.put(name, new HashSet<>());
        saveVault();
        return true;
    }

    public boolean deleteCategory(String name) {
        if (categories.remove(name) != null) {
            // Quitar esta categoría de todas las credenciales que la tuvieran
            for (Credential cred : vault.values()) {
                cred.removeCategory(name);
            }
            saveVault();
            return true;
        }
        return false; // La categoría no existía
    }

    public Set<String> getCategories() {
        return categories.keySet();
    }

    public boolean addLoginToCategory(String id, String category) {
        if (!vault.containsKey(id) || !categories.containsKey(category)) {
            return false; // No existe el ID o la categoría
        }
        boolean added = categories.get(category).add(id);
        if (added) {
            vault.get(id).addCategory(category); // Actualiza el objeto Credential
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
                vault.get(id).removeCategory(category); // Actualiza el objeto Credential
            }
            saveVault();
        }
        return removed;
    }
}