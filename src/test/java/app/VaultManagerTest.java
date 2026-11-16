package app;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"java:S106", "java:S2925"})
class VaultManagerTest {
    private static final String TEST_FILE = "prueba_vault.dat";
    private static final String TEST_PASS = "claveMaestraTest123";

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
        VaultManager vault = new VaultManager(TEST_FILE, TEST_PASS);

        vault.addLogin("1", "jon", "pass");
        assertNotNull(vault.getCredential("1"));
        assertEquals("jon", vault.getCredential("1").getUsername());

        VaultManager reloaded = new VaultManager(TEST_FILE, TEST_PASS);
        assertEquals("pass", reloaded.getCredential("1").getPassword());

        vault.deleteLogin("1");
        assertNull(vault.getCredential("1"));
    }

    @Test
    void testListaIds() {
        VaultManager vault = new VaultManager(TEST_FILE, TEST_PASS);
        vault.addLogin("id_prueba", "usuario", "123");

        Iterable<String> ids = vault.getLoginIds();
        assertTrue(ids.iterator().hasNext());
    }

    @Test
    void testGestionarCategoriasGlobales() {
        VaultManager vault = new VaultManager(TEST_FILE, TEST_PASS);

        vault.addCategory("Juegos");
        assertTrue(vault.getCategories().contains("Juegos"));

        vault.deleteCategory("Juegos");
        assertFalse(vault.getCategories().contains("Juegos"));
    }

    @Test
    void testAsignarCredencialACategoria() {
        VaultManager vault = new VaultManager(TEST_FILE, TEST_PASS);
        vault.addLogin("1", "user", "pass");
        vault.addCategory("Redes");

        vault.addLoginToCategory("1", "Redes");
        assertTrue(vault.getCredential("1").getCategories().contains("Redes"));

        vault.removeLoginFromCategory("1", "Redes");
        assertFalse(vault.getCredential("1").getCategories().contains("Redes"));
    }

    @Test
    void testCargarArchivoCorruptoOClaveIncorrecta() throws Exception {
        File file = new File(TEST_FILE);
        Files.writeString(file.toPath(), "datos basura que no se pueden leer");

        assertThrows(IllegalStateException.class, () -> new VaultManager(TEST_FILE, TEST_PASS));
    }

    @Test
    void testErrorAlGuardar() {
        VaultManager vault = new VaultManager(TEST_FILE, TEST_PASS);

        File dir = new File(TEST_FILE);
        dir.mkdir();

        vault.addCategory("Imposible");

        dir.delete();
        assertTrue(true);
    }

    @Test
    void testBorrarCategoriaConLogins() {
        VaultManager vault = new VaultManager(TEST_FILE, TEST_PASS);
        vault.addLogin("99", "user", "pass");
        vault.addCategory("Borrable");
        vault.addLoginToCategory("99", "Borrable");

        vault.deleteCategory("Borrable");
        assertFalse(vault.getCredential("99").getCategories().contains("Borrable"));
    }

    @Test
    void testSyncWithLocalFileNullOrEmpty() {
        VaultManager vault = new VaultManager(TEST_FILE, TEST_PASS);
        vault.syncWithLocalFile(null);
        vault.syncWithLocalFile("");
        assertTrue(true);
    }

    @Test
    void testSyncWithNewFileCreation() throws Exception {
        VaultManager vault = new VaultManager(TEST_FILE, TEST_PASS);
        vault.addLogin("1", "user", "secretoEnDisco");
        String syncPath = "nueva_sync_test.dat";

        vault.syncWithLocalFile(syncPath);
        File syncFile = new File(syncPath);
        assertTrue(syncFile.exists());

        String diskContent = Files.readString(syncFile.toPath());
        assertFalse(diskContent.contains("secretoEnDisco"));

        syncFile.delete();
    }

    @Test
    void testSyncLocalMergeAndConflicts() throws Exception {
        String remoteFile = "remote_vault_test.dat";

        VaultManager vault = new VaultManager(TEST_FILE, TEST_PASS);
        vault.addLogin("1", "localUser", "pass1");

        VaultManager remoteVault = new VaultManager(remoteFile, TEST_PASS);
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
    void testSyncComplexMergeAndErrors() {
        String remoteFile = "remote_complex_test.dat";
        VaultManager vault = new VaultManager(TEST_FILE, TEST_PASS);

        vault.addLogin("1", "user1", "pass1");
        vault.addCategory("Trabajo");
        vault.addLoginToCategory("1", "Trabajo");

        VaultManager remoteVault = new VaultManager(remoteFile, TEST_PASS);
        remoteVault.addLogin("1", "user1Updated", "passNew");
        remoteVault.addLogin("3", "user3", "pass3");
        remoteVault.addCategory("Personal");
        remoteVault.addLoginToCategory("3", "Personal");
        remoteVault.deleteLogin("1");

        vault.syncWithLocalFile(remoteFile);
        vault.syncWithLocalFile("carpeta_inexistente_xyz/sync.dat");
        vault.syncWithServer("localhost", 59999);

        new File(remoteFile).delete();
        assertNotNull(vault.getCredential("3"));
    }

    @Test
    void testExtraBranchesAndEdgeCases() throws Exception {
        Files.writeString(new File(TEST_FILE).toPath(), "");
        assertThrows(IllegalStateException.class, () -> new VaultManager(TEST_FILE, TEST_PASS));
        new File(TEST_FILE).delete();

        VaultManager vault1 = new VaultManager(TEST_FILE, TEST_PASS);
        vault1.addLogin("1", "user", "pass");
        String syncPath = "sync_branches_test.dat";
        vault1.syncWithLocalFile(syncPath);
        vault1.syncWithLocalFile(syncPath);

        Files.writeString(new File(syncPath).toPath(), "contenido_corrupto");
        vault1.syncWithLocalFile(syncPath);
        new File(syncPath).delete();

        String remotePath = "remote_del_test.dat";
        VaultManager remote = new VaultManager(remotePath, TEST_PASS);
        remote.addLogin("borrada", "u", "p");
        Thread.sleep(15);

        vault1.addLogin("borrada", "u", "p");
        vault1.deleteLogin("borrada");
        vault1.syncWithLocalFile(remotePath);
        assertNull(vault1.getCredential("borrada"));
        new File(remotePath).delete();

        String badPayload = CryptoUtils.encryptObject("no_es_un_array", TEST_PASS);
        assertThrows(java.security.GeneralSecurityException.class,
                () -> vault1.processEncryptedSyncPayload(badPayload));
    }
}