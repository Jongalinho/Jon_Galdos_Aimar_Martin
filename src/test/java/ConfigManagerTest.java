import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ConfigManagerTest {

    @Test
    public void testSinArgumentos() {
        String[] args = {};
        ConfigManager config = new ConfigManager(args);

        assertEquals("vault.dat", config.getVaultPath()); 
    }

    @Test
    public void testConArgumento() {
        String[] args = {"-c", "archivo_falso.properties"};
        ConfigManager config = new ConfigManager(args);

        assertEquals("vault.dat", config.getVaultPath()); 
    }
}