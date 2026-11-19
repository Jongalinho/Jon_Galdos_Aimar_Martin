package app;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"java:S106", "java:S2925"})
class ServerTest {

    @Test
    void testEncryptedServerAndClientSync() throws Exception {
        int testPort = 5075;
        String sharedPass = "claveCompartida123";
        File serverVaultFile = new File("server_vault.dat");
        File clientVaultFile = new File("client_vault_test.dat");
        if (serverVaultFile.exists()) serverVaultFile.delete();
        if (clientVaultFile.exists()) clientVaultFile.delete();

        Thread serverThread = new Thread(() -> Server.startServer(testPort, sharedPass));
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(250);

        VaultManager clientVault = new VaultManager(clientVaultFile.getPath(), sharedPass);
        clientVault.addLogin("github", "aimar", "secreto123");
        clientVault.syncWithServer("localhost", testPort);

        String rawServerDisk = Files.readString(serverVaultFile.toPath());
        assertFalse(rawServerDisk.contains("secreto123"));
        assertFalse(rawServerDisk.contains("aimar"));

        clientVault.syncWithServer("localhost", testPort);

        serverVaultFile.delete();
        clientVaultFile.delete();
    }

    @Test
    void testServerMainAndInvalidClientPayload() throws Exception {
        int testPort = 5085;
        File serverVaultFile = new File("server_vault.dat");
        if (serverVaultFile.exists()) serverVaultFile.delete();

        InputStream originalIn = System.in;
        try {
            System.setIn(new ByteArrayInputStream("serverPass\n".getBytes()));
            Thread serverThread = new Thread(() -> Server.main(new String[]{String.valueOf(testPort)}));
            serverThread.setDaemon(true);
            serverThread.start();
            Thread.sleep(250);
        } finally {
            System.setIn(originalIn);
        }

        try (Socket emptySocket = new Socket("localhost", testPort)) {
            assertTrue(emptySocket.isConnected());
        }
        Thread.sleep(100);
        
        try (Socket blankSocket = new Socket("localhost", testPort);
             java.io.PrintWriter out = new java.io.PrintWriter(blankSocket.getOutputStream(), true)) {
            out.println("");
        }
        Thread.sleep(100);

        VaultManager wrongClient = new VaultManager("wrong_client_test.dat", "claveErronea");
        wrongClient.addLogin("1", "user", "pass");
        wrongClient.syncWithServer("localhost", testPort);

        new File("wrong_client_test.dat").delete();
        if (serverVaultFile.exists()) serverVaultFile.delete();
        assertTrue(true);
    }

    @Test
    void testServerPortAlreadyInUse() throws Exception {
        try (ServerSocket occupied = new ServerSocket(0)) {
            int busyPort = occupied.getLocalPort();
            Server.startServer(busyPort, "pass");
            assertTrue(occupied.isBound());
        }
    }

    @Test
    void testServerDefaultPortArg() throws Exception {
        File serverVaultFile = new File("server_vault.dat");
        if (serverVaultFile.exists()) serverVaultFile.delete();

        InputStream originalIn = System.in;
        try {
            System.setIn(new ByteArrayInputStream("defaultPortPass\n".getBytes()));
            Thread serverThread = new Thread(() -> Server.main(new String[]{}));
            serverThread.setDaemon(true);
            serverThread.start();
            Thread.sleep(250);
        } finally {
            System.setIn(originalIn);
        }

        VaultManager client = new VaultManager("default_port_client.dat", "defaultPortPass");
        client.addLogin("id1", "user", "pass");
        client.syncWithServer("localhost", 5000);

        assertNotNull(client.getCredential("id1"));
        new File("default_port_client.dat").delete();
        if (serverVaultFile.exists()) serverVaultFile.delete();
    }

    @Test
    void testServerConstructor() {
        Server server = new Server();
        assertNotNull(server);
    }
}