package app;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("java:S106")
class ConfigManagerTest {

    @Test
    void testSinArgumentos() {
        String[] args = {};
        ConfigManager config = new ConfigManager(args);

        assertEquals("vault.dat", config.getVaultPath()); 
    }

    @Test
    void testConArgumento() {
        String[] args = {"-c", "archivo_falso.properties"};
        ConfigManager config = new ConfigManager(args);

        assertEquals("vault.dat", config.getVaultPath()); 
    }
    
    @Test
    void testConArgumentoSinNombre() {
        String[] args = {"-c"};
        ConfigManager config = new ConfigManager(args);

        assertEquals("vault.dat", config.getVaultPath()); 
    }

    @Test
    void testArchivoValido() throws Exception {
        java.io.File temp = java.io.File.createTempFile("config", ".properties");
        java.nio.file.Files.writeString(temp.toPath(), "vault.path=mi_vault_custom.dat");
        
        String[] args = {"-c", temp.getAbsolutePath()};
        ConfigManager config = new ConfigManager(args);
        
        assertEquals("mi_vault_custom.dat", config.getVaultPath());
        temp.delete();
    }

    @Test
    void testSyncFilePath() throws Exception {
        java.io.File temp = java.io.File.createTempFile("config_sync", ".properties");
        java.nio.file.Files.writeString(temp.toPath(), "vault.path=vault.dat\nsync.file=sync_data.dat");
        
        String[] args = {"-c", temp.getAbsolutePath()};
        ConfigManager config = new ConfigManager(args);
        
        assertEquals("sync_data.dat", config.getSyncFilePath());
        temp.delete();
    }

    @Test
    void testServerPortConfig() throws Exception {
        java.io.File temp = java.io.File.createTempFile("config_port", ".properties");
        java.nio.file.Files.writeString(temp.toPath(), "server.address=localhost\nserver.port=8080");
        
        String[] args = {"-c", temp.getAbsolutePath()};
        ConfigManager config = new ConfigManager(args);
        
        assertEquals(8080, config.getServerPort());
        assertEquals("localhost", config.getServerAddress());
        temp.delete();
    }

    @Test
    void testGetServerPortExplicitly() throws Exception {
        java.io.File temp = java.io.File.createTempFile("config_port_exp", ".properties");
        java.nio.file.Files.writeString(temp.toPath(), "server.port=9090");
        
        ConfigManager config = new ConfigManager(new String[]{"-c", temp.getAbsolutePath()});
        assertEquals(9090, config.getServerPort());
        temp.delete();
    }
}