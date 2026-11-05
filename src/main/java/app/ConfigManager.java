package app;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigManager {
    private static final String DEFAULT_CONFIG = "config.properties";
    private String vaultPath = "vault.dat";

    public ConfigManager(String[] args) {
        String configPath = DEFAULT_CONFIG;
        
        for (int i = 0; i < args.length; i++) {
            if ("-c".equals(args[i]) && i + 1 < args.length) {
                configPath = args[i + 1];
                break;
            }
        }
        loadConfig(configPath);
    }

    private void loadConfig(String configPath) {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(configPath)) {
            props.load(in);
            this.vaultPath = props.getProperty("vault.path", "vault.dat");
        } catch (IOException e) {
            System.out.println("Config file not found, using default vault path.");
        }
    }

    public String getVaultPath() {
        return vaultPath;
    }
}
