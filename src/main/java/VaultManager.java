import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class VaultManager {
    private final String vaultPath;
    private Map<String, Credential> vault;

    public VaultManager(String vaultPath) {
        this.vaultPath = vaultPath;
        this.vault = loadVault();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Credential> loadVault() {
        File file = new File(vaultPath);
        if (!file.exists()) return new HashMap<>();
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (Map<String, Credential>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return new HashMap<>();
        }
    }

    private void saveVault() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(vaultPath))) {
            oos.writeObject(vault);
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
        saveVault();
    }

    public Credential getCredential(String id) {
        return vault.get(id);
    }

    public Iterable<String> getLoginIds() {
        return vault.keySet();
    }
}
