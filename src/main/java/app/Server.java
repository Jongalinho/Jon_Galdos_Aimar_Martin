package app;
import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("java:S106")
public class Server {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        int port = 5000;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        
        System.out.println("Vault Server started on port " + port + "...");
        String serverVaultPath = "server_vault.dat";
        VaultManager serverVault = new VaultManager(serverVaultPath);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                     ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {
                      
                    System.out.println("Client connected from " + clientSocket.getInetAddress());
                      
                    Map<String, Credential> clientVault = (Map<String, Credential>) in.readObject();
                    Map<String, Set<String>> clientCategories = (Map<String, Set<String>>) in.readObject();
                    Map<String, Long> clientDeleted = (Map<String, Long>) in.readObject();
                      
                    File tempClientFile = File.createTempFile("client_sync_temp", ".dat");
                    try (ObjectOutputStream tempOos = new ObjectOutputStream(new FileOutputStream(tempClientFile))) {
                        tempOos.writeObject(clientVault);
                        tempOos.writeObject(clientCategories);
                        tempOos.writeObject(clientDeleted);
                    }

                    serverVault.syncWithLocalFile(tempClientFile.getAbsolutePath());
                    tempClientFile.delete();

                    File serverFile = new File(serverVaultPath);
                    if (serverFile.exists()) {
                        try (ObjectInputStream serverOis = new ObjectInputStream(new FileInputStream(serverFile))) {
                            Map<String, Credential> resVault = (Map<String, Credential>) serverOis.readObject();
                            Map<String, Set<String>> resCat = (Map<String, Set<String>>) serverOis.readObject();
                            Map<String, Long> resDel = (Map<String, Long>) serverOis.readObject();
                            
                            out.writeObject(resVault);
                            out.writeObject(resCat);
                            out.writeObject(resDel);
                        }
                    } else {
                        out.writeObject(clientVault);
                        out.writeObject(clientCategories);
                        out.writeObject(clientDeleted);
                    }
                    out.flush();
                      
                    System.out.println("Sync processed for client.");
                } catch (Exception e) {
                    System.out.println("Error handling client: " + e.getMessage());
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Server exception: " + e.getMessage());
        }
    }
}