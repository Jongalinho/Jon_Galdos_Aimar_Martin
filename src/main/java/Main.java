import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        ConfigManager config = new ConfigManager(args);
        VaultManager vault = new VaultManager(config.getVaultPath());
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.println("\n(1) List Login IDs");
            System.out.println("(2) Query by ID");
            System.out.println("(3) Add Login");
            System.out.println("(4) Delete Login");
            System.out.println("(e) Exit");
            System.out.print("Select an option: ");
            
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    System.out.println("Login IDs:");
                    for (String id : vault.getLoginIds()) {
                        System.out.println("- " + id);
                    }
                    break;
                case "2":
                    System.out.print("Enter ID: ");
                    String qId = scanner.nextLine();
                    Credential cred = vault.getCredential(qId);
                    if (cred != null) {
                        System.out.println("Username: " + cred.getUsername());
                        System.out.println("Password: " + cred.getPassword());
                    } else {
                        System.out.println("ID not found.");
                    }
                    break;
                case "3":
                    System.out.print("Enter ID: ");
                    String newId = scanner.nextLine();
                    System.out.print("Enter Username (optional): ");
                    String user = scanner.nextLine();
                    System.out.print("Enter Password (optional): ");
                    String pass = scanner.nextLine();
                    vault.addLogin(newId, user.isEmpty() ? null : user, pass.isEmpty() ? null : pass);
                    System.out.println("Login added.");
                    break;
                case "4":
                    System.out.print("Enter ID to delete: ");
                    String delId = scanner.nextLine();
                    vault.deleteLogin(delId);
                    System.out.println("Login deleted.");
                    break;
                case "e":
                    System.out.println("Exiting...");
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }
}
