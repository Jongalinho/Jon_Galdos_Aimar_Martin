package app;

import org.junit.jupiter.api.Test;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("java:S106")
class ServerTest {

    @Test
    void testServerConnectionAndSync() throws Exception {
        int testPort = 5050;

        Thread serverThread = new Thread(() -> {
            try {
                Server.main(new String[]{String.valueOf(testPort)});
            } catch (Exception e) {
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        Thread.sleep(300);

        try (Socket socket = new Socket("localhost", testPort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject(new java.util.HashMap<String, Credential>());
            out.writeObject(new java.util.HashMap<String, java.util.Set<String>>());
            out.writeObject(new java.util.HashMap<String, Long>());
            out.flush();

            Object response = in.readObject();
            assertNotNull(response);
        }

        File serverVaultFile = new File("server_vault.dat");
        if (serverVaultFile.exists()) {
            serverVaultFile.delete();
        }
    }

    @Test
    void testServerElseBranchWithoutExistingVault() throws Exception {
        int testPort = 5060;

        File serverVaultFile = new File("server_vault.dat");
        if (serverVaultFile.exists()) {
            serverVaultFile.delete();
        }

        Thread serverThread = new Thread(() -> {
            try {
                Server.main(new String[]{String.valueOf(testPort)});
            } catch (Exception e) {}
        });
        serverThread.setDaemon(true);
        serverThread.start();

        Thread.sleep(300);

        try (Socket socket = new Socket("localhost", testPort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject(new java.util.HashMap<String, Credential>());
            out.writeObject(new java.util.HashMap<String, java.util.Set<String>>());
            out.writeObject(new java.util.HashMap<String, Long>());
            out.flush();

            Object response = in.readObject();
            assertNotNull(response);
        }

        if (serverVaultFile.exists()) {
            serverVaultFile.delete();
        }
    }
}