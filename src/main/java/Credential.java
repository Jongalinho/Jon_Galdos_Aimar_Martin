import java.io.Serializable;

public class Credential implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String username;
    private final String password;

    public Credential(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
