package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

@SuppressWarnings("java:S106")
public class Server {
    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5000;
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter server password: ");
        String password = scanner.nextLine();
        startServer(port, password);
    }

    public static void startServer(int port, String password) {
        System.out.println("Vault Server started on port " + port + "...");
        String serverVaultPath = "server_vault.dat";
        VaultManager serverVault = new VaultManager(serverVaultPath, password);

        boolean running = true;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (running) {
                running = handleClient(serverSocket, serverVault);
            }
        } catch (IOException e) {
            System.out.println("Server exception: " + e.getMessage());
        }
    }

    private static boolean handleClient(ServerSocket serverSocket, VaultManager serverVault) {
        try (Socket clientSocket = serverSocket.accept();
             BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            System.out.println("Client connected from " + clientSocket.getInetAddress());
            String encryptedClientData = in.readLine();
            if (encryptedClientData != null && !encryptedClientData.isEmpty()) {
                String encryptedResponse = serverVault.processEncryptedSyncPayload(encryptedClientData);
                out.println(encryptedResponse);
                System.out.println("Encrypted sync processed for client.");
            }
        } catch (Exception e) {
            System.out.println("Error handling client: " + e.getMessage());
            return false;
        }
        return true;
    }
}