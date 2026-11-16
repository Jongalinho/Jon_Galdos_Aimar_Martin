package app;

import org.junit.jupiter.api.Test;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("java:S106")
class ServerTest {

    @Test
    @SuppressWarnings("java:S2925")
    void testServerConnectionAndSync() throws Exception {
        int testPort = 5070;
        CountDownLatch latch = new CountDownLatch(1);

        Thread serverThread = new Thread(() -> {
            try {
                latch.countDown();
                Server.main(new String[]{String.valueOf(testPort)});
            } catch (Exception e) {
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        Thread.sleep(100);

        try (Socket socket = new Socket("localhost", testPort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject(java.util.Collections.emptyMap());
            out.writeObject(java.util.Collections.emptyMap());
            out.writeObject(java.util.Collections.emptyMap());
            out.flush();

            Object response = in.readObject();
            assertNotNull(response, "La respuesta del servidor no debe ser nula");
        }

        File serverVaultFile = new File("server_vault.dat");
        if (serverVaultFile.exists()) {
            serverVaultFile.delete();
        }
    }

    @Test
    @SuppressWarnings("java:S2925")
    void testServerElseBranchAndExceptions() throws Exception {
        int testPort = 5080;

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

        Thread.sleep(200);

        try (Socket socket = new Socket("localhost", testPort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.writeObject("Dato invalido para provocar excepcion en el server");
            out.flush();
        }

        Thread.sleep(100);

        assertFalse(serverVaultFile.exists(), "El archivo del servidor no debería haberse creado con datos corruptos");

        if (serverVaultFile.exists()) {
            serverVaultFile.delete();
        }
    }

    @Test
    @SuppressWarnings("java:S2925")
    void testServerMultipleInteractionsAndSyncBranch() throws Exception {
        int testPort = 5090;

        File serverVaultFile = new File("server_vault.dat");
        if (serverVaultFile.exists()) serverVaultFile.delete();

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
            assertNotNull(in.readObject());
        }

        try (Socket socket = new Socket("localhost", testPort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            
            out.writeObject(new java.util.HashMap<String, Credential>());
            out.writeObject(new java.util.HashMap<String, java.util.Set<String>>());
            out.writeObject(new java.util.HashMap<String, Long>());
            out.flush();
            assertNotNull(in.readObject());
        }

        if (serverVaultFile.exists()) serverVaultFile.delete();
        assertTrue(true);
    }

    @Test
    @SuppressWarnings("java:S2925")
    void testServerWithExistingVaultBranch() throws Exception {
        int testPort = 5095;
        File serverVaultFile = new File("server_vault.dat");
        
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(serverVaultFile))) {
            oos.writeObject(new java.util.HashMap<String, Credential>());
            oos.writeObject(new java.util.HashMap<String, java.util.Set<String>>());
            oos.writeObject(new java.util.HashMap<String, Long>());
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
            assertNotNull(in.readObject());
        }

        if (serverVaultFile.exists()) {
            serverVaultFile.delete();
        }
    }
}