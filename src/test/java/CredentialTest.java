import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CredentialTest {

    @Test
    public void testCredential() {
        Credential cred = new Credential("aimar", "1234");
        assertEquals("aimar", cred.getUsername());
        assertEquals("1234", cred.getPassword());
    }
}