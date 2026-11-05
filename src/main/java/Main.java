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
            System.out.println("(5) View Categories and Login IDs");
            System.out.println("(6) Add ID to Category");
            System.out.println("(7) Remove ID from Category");
            System.out.println("(8) Add Category");
            System.out.println("(9) Delete Category");
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
                    case "5":
                        System.out.println("Categories and Login IDs:");
                        for (String cat : vault.getCategories()) {
                            System.out.println("Category: [" + cat + "]");
                            boolean hasLogins = false;
                            for (String id : vault.getLoginIds()) {
                                Credential c = vault.getCredential(id);
                                if (c != null && c.getCategories().contains(cat)) {
                                    System.out.println("  - " + id);
                                    hasLogins = true;
                                }
                            }
                            if (!hasLogins) System.out.println("  (No logins assigned)");
                        }
                        break;
                    case "6":
                        System.out.print("Enter Login ID: ");
                        String idToAdd = scanner.nextLine();
                        System.out.print("Enter Category Name: ");
                        String catToAdd = scanner.nextLine();
                        if (vault.addLoginToCategory(idToAdd, catToAdd)) {
                            System.out.println("Login ID added to category successfully.");
                        } else {
                            System.out.println("Error: Login ID or Category not found.");
                        }
                        break;
                    case "7":
                        System.out.print("Enter Login ID: ");
                        String idToRemove = scanner.nextLine();
                        System.out.print("Enter Category Name: ");
                        String catToRemove = scanner.nextLine();
                        if (vault.removeLoginFromCategory(idToRemove, catToRemove)) {
                            System.out.println("Login ID removed from category successfully.");
                        } else {
                            System.out.println("Error: Category not found or ID not in category.");
                        }
                        break;
                    case "8":
                        System.out.print("Enter new Category Name: ");
                        String newCat = scanner.nextLine();
                        if (vault.addCategory(newCat)) {
                            System.out.println("Category added successfully.");
                        } else {
                            System.out.println("Category already exists.");
                        }
                        break;
                    case "9":
                        System.out.print("Enter Category Name to delete: ");
                        String delCat = scanner.nextLine();
                        if (vault.deleteCategory(delCat)) {
                            System.out.println("Category deleted successfully.");
                        } else {
                            System.out.println("Category not found.");
                        }
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