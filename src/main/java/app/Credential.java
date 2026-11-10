package app;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Credential implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String username;
    private final String password;
    private Set<String> categories;
    
    // NUEVO: Variable añadida sin tocar el constructor
    private long lastEditDate = System.currentTimeMillis(); 

    public Credential(String username, String password) {
        this.username = username;
        this.password = password;
        this.categories = new HashSet<>();
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }

    public Set<String> getCategories() { 
        if (categories == null) categories = new HashSet<>();
        return categories; 
    }
    
    public void addCategory(String name) { 
        getCategories().add(name); 
    }
    
    public void removeCategory(String name) { 
        getCategories().remove(name); 
    }

    public long getLastEditDate() { 
        return lastEditDate; 
    }
    
    public void updateLastEditDate() { 
        this.lastEditDate = System.currentTimeMillis(); 
    }
}
