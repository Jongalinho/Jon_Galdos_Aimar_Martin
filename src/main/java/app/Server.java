package app;
import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"java:S106", "java:S5443"})
public class Server {
    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5000;
        System.out.println("Vault Server started on port " + port + "...");
        String serverVaultPath = "server_vault.dat";
        VaultManager serverVault = new VaultManager(serverVaultPath);

        boolean running = true;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (running) {
                running = handleClient(serverSocket, serverVault, serverVaultPath);
            }
        } catch (IOException e) {
            System.out.println("Server exception: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean handleClient(ServerSocket serverSocket, VaultManager serverVault, String serverVaultPath) {
        try (Socket clientSocket = serverSocket.accept();
             ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {
              
            System.out.println("Client connected from " + clientSocket.getInetAddress());
              
            Map<String, Credential> clientVault = (Map<String, Credential>) in.readObject();
            Map<String, Set<String>> clientCategories = (Map<String, Set<String>>) in.readObject();
            Map<String, Long> clientDeleted = (Map<String, Long>) in.readObject();
              
            File tempClientFile = File.createTempFile("client_sync_temp", ".dat");
            writeTempFile(tempClientFile, clientVault, clientCategories, clientDeleted);

            serverVault.syncWithLocalFile(tempClientFile.getAbsolutePath());
            
            deleteTempFileSafely(tempClientFile);

            File serverFile = new File(serverVaultPath);
            if (serverFile.exists()) {
                sendServerVault(serverFile, out);
            } else {
                out.writeObject(clientVault);
                out.writeObject(clientCategories);
                out.writeObject(clientDeleted);
            }
            out.flush();
              
            System.out.println("Sync processed for client.");
        } catch (Exception e) {
            System.out.println("Error handling client: " + e.getMessage());
            return false;
        }
        return true;
    }

    private static void deleteTempFileSafely(File file) {
        try {
            java.nio.file.Files.delete(file.toPath());
        } catch (IOException e) {
            file.deleteOnExit();
        }
    }

    private static void writeTempFile(File file, Map<String, Credential> vault, Map<String, Set<String>> categories, Map<String, Long> deleted) throws IOException {
        try (ObjectOutputStream tempOos = new ObjectOutputStream(new FileOutputStream(file))) {
            tempOos.writeObject(vault);
            tempOos.writeObject(categories);
            tempOos.writeObject(deleted);
        }
    }

    @SuppressWarnings("unchecked")
    private static void sendServerVault(File serverFile, ObjectOutputStream out) throws IOException, ClassNotFoundException {
        try (ObjectInputStream serverOis = new ObjectInputStream(new FileInputStream(serverFile))) {
            Map<String, Credential> resVault = (Map<String, Credential>) serverOis.readObject();
            Map<String, Set<String>> resCat = (Map<String, Set<String>>) serverOis.readObject();
            Map<String, Long> resDel = (Map<String, Long>) serverOis.readObject();
            
            out.writeObject(resVault);
            out.writeObject(resCat);
            out.writeObject(resDel);
        }
    }
}