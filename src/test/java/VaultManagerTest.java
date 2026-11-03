import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import static org.junit.jupiter.api.Assertions.*;

public class VaultManagerTest {
    private final String TEST_FILE = "prueba_vault.dat";

    @BeforeEach
    @AfterEach
    public void limpiarArchivo() {
        File file = new File(TEST_FILE);
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    public void testAñadirYBorrar() {
        VaultManager vault = new VaultManager(TEST_FILE);

        vault.addLogin("1", "jon", "pass");
        assertNotNull(vault.getCredential("1"));
        assertEquals("jon", vault.getCredential("1").getUsername());

        vault.deleteLogin("1");
        assertNull(vault.getCredential("1"));
    }

    @Test
    public void testListaIds() {
        VaultManager vault = new VaultManager(TEST_FILE);
        vault.addLogin("id_prueba", "usuario", "123");
        
        Iterable<String> ids = vault.getLoginIds();
        assertTrue(ids.iterator().hasNext());
    }
}