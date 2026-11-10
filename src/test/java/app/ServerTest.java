package app;

import org.junit.jupiter.api.Test;
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
}