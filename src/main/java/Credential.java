import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Credential implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String username;
    private final String password;
    private Set<String> categories; // Nueva estructura

    public Credential(String username, String password) {
        this.username = username;
        this.password = password;
        this.categories = new HashSet<>();
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }

    // --- MÉTODOS PEDIDOS POR AIMAR ---
    public Set<String> getCategories() { 
        // Por si se lee de un archivo antiguo que no tenía esta variable inicializada
        if (categories == null) categories = new HashSet<>();
        return categories; 
    }
    
    public void addCategory(String name) { 
        getCategories().add(name); 
    }
    
    public void removeCategory(String name) { 
        getCategories().remove(name); 
    }
}
