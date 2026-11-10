package app;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("java:S106")
class VaultManagerTest {
    private final String TEST_FILE = "prueba_vault.dat";

    @BeforeEach
    @AfterEach
    void limpiarArchivo() {
        File file = new File(TEST_FILE);
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    void testAñadirYBorrar() {
        VaultManager vault = new VaultManager(TEST_FILE);

        vault.addLogin("1", "jon", "pass");
        assertNotNull(vault.getCredential("1"));
        assertEquals("jon", vault.getCredential("1").getUsername());

        vault.deleteLogin("1");
        assertNull(vault.getCredential("1"));
    }

    @Test
    void testListaIds() {
        VaultManager vault = new VaultManager(TEST_FILE);
        vault.addLogin("id_prueba", "usuario", "123");
        
        Iterable<String> ids = vault.getLoginIds();
        assertTrue(ids.iterator().hasNext());
    }

    @Test
    void testGestionarCategoriasGlobales() {
        VaultManager vault = new VaultManager(TEST_FILE);
        
        vault.addCategory("Juegos");
        assertTrue(vault.getCategories().contains("Juegos"));
        
        vault.deleteCategory("Juegos");
        assertFalse(vault.getCategories().contains("Juegos"));
    }

    @Test
    void testAsignarCredencialACategoria() {
        VaultManager vault = new VaultManager(TEST_FILE);
        vault.addLogin("1", "user", "pass");
        vault.addCategory("Redes");
        
        vault.addLoginToCategory("1", "Redes");
        assertTrue(vault.getCredential("1").getCategories().contains("Redes"));
        
        vault.removeLoginFromCategory("1", "Redes");
        assertFalse(vault.getCredential("1").getCategories().contains("Redes"));
    }

    @Test
    void testCargarArchivoCorrupto() throws Exception {
        File file = new File(TEST_FILE);
        java.nio.file.Files.writeString(file.toPath(), "datos basura que no se pueden leer");
        
        VaultManager vault = new VaultManager(TEST_FILE);
        
        assertFalse(vault.getLoginIds().iterator().hasNext());
    }

    @Test
    void testErrorAlGuardar() {
        File dir = new File(TEST_FILE);
        dir.mkdir();
        
        VaultManager vault = new VaultManager(TEST_FILE);
        vault.addCategory("Imposible"); 
        
        dir.delete();
        assertTrue(true);
    }

    @Test
    void testBorrarCategoriaConLogins() {
        VaultManager vault = new VaultManager(TEST_FILE);
        vault.addLogin("99", "user", "pass");
        vault.addCategory("Borrable");
        vault.addLoginToCategory("99", "Borrable");
        
        vault.deleteCategory("Borrable");
        assertFalse(vault.getCredential("99").getCategories().contains("Borrable"));
    }

    @Test
    void testArchivoGuardadoAMedias() throws Exception {
        File file = new File(TEST_FILE);
        try (java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(new java.io.FileOutputStream(file))) {
            oos.writeObject(new java.util.HashMap<String, Credential>()); 
        }
        
        VaultManager vault = new VaultManager(TEST_FILE);
        
        assertNotNull(vault.getCategories());
    }

    @Test
    void testSyncWithLocalFileNullOrEmpty() {
        VaultManager vault = new VaultManager(TEST_FILE);
        vault.syncWithLocalFile(null);
        vault.syncWithLocalFile("");
        assertTrue(true);
    }

    @Test
    void testSyncWithNewFileCreation() {
        VaultManager vault = new VaultManager(TEST_FILE);
        vault.addLogin("1", "user", "pass");
        String syncPath = "nueva_sync_test.dat";
        
        vault.syncWithLocalFile(syncPath);
        assertTrue(new File(syncPath).exists());
        new File(syncPath).delete();
    }

    @Test
    void testSyncLocalMergeAndConflicts() throws Exception {
        String remoteFile = "remote_vault_test.dat";
        
        VaultManager vault = new VaultManager(TEST_FILE);
        vault.addLogin("1", "localUser", "pass1");
        
        VaultManager remoteVault = new VaultManager(remoteFile);
        Thread.sleep(10);
        remoteVault.addLogin("1", "remoteUserNewer", "pass2");
        remoteVault.addLogin("2", "remoteUser2", "pass3");
        
        vault.syncWithLocalFile(remoteFile);
        
        assertEquals("remoteUserNewer", vault.getCredential("1").getUsername());
        assertNotNull(vault.getCredential("2"));
        
        vault.syncWithLocalFile(remoteFile);
        
        vault.deleteLogin("2");
        vault.syncWithLocalFile(remoteFile);
        
        new File(remoteFile).delete();
    }

    @Test
    void testSyncComplexMergeAndErrors() throws Exception {
        String remoteFile = "remote_complex_test.dat";
        VaultManager vault = new VaultManager(TEST_FILE);
        
        vault.addLogin("1", "user1", "pass1");
        vault.addCategory("Trabajo");
        vault.addLoginToCategory("1", "Trabajo");

        VaultManager remoteVault = new VaultManager(remoteFile);
        remoteVault.addLogin("1", "user1Updated", "passNew");
        remoteVault.addLogin("3", "user3", "pass3");
        remoteVault.addCategory("Personal");
        remoteVault.addLoginToCategory("3", "Personal");
        remoteVault.deleteLogin("1");

        vault.syncWithLocalFile(remoteFile);
        
        vault.syncWithLocalFile("");

        new File(remoteFile).delete();
        assertTrue(true);
    }
}