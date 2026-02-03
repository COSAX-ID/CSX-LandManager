package dev.cosax.cSXLandManager;

import dev.cosax.cSXLandManager.command.LandManagementCommand;
import dev.cosax.cSXLandManager.config.Config;
import dev.cosax.cSXLandManager.config.Messages;
import dev.cosax.cSXLandManager.listener.ChatListener;
import dev.cosax.cSXLandManager.listener.GUIListener;
import dev.cosax.cSXLandManager.listener.PlayerListener;
import dev.cosax.cSXLandManager.manager.ClaimManager;
import dev.cosax.cSXLandManager.manager.EconomyManager;
import dev.cosax.cSXLandManager.manager.GUIManager;
import dev.cosax.cSXLandManager.manager.RentManager;
import dev.cosax.cSXLandManager.storage.StorageManager;
import dev.cosax.cSXLandManager.task.RentExpirationTask;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Main plugin class for CSX LandManager.
 * A GriefPrevention addon that provides GUI-based land management.
 */
public final class CSXLandManager extends JavaPlugin {

    // Managers
    private Config configManager;
    private Messages messages;
    private StorageManager storageManager;
    private ClaimManager claimManager;
    private EconomyManager economyManager;
    private RentManager rentManager;
    private GUIManager guiManager;

    // Tasks and listeners
    private RentExpirationTask rentExpirationTask;
    private GUIListener guiListener;
    private PlayerListener playerListener;
    private ChatListener chatListener;

    // Dependency status
    private boolean griefPreventionAvailable = false;
    private boolean vaultAvailable = false;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();

        // Print startup header
        getLogger().info("==========================================");
        getLogger().info("  CSX LandManager v" + getDescription().getVersion());
        getLogger().info("  Author: cosaxid");
        getLogger().info("==========================================");

        // Check for dependencies
        checkDependencies();

        // Load configuration
        loadConfiguration();

        // Initialize storage
        initializeStorage();

        // Initialize managers
        initializeManagers();

        // Register commands
        registerCommands();

        // Register listeners
        registerListeners();

        // Start tasks
        startTasks();

        // Print startup completion
        long duration = System.currentTimeMillis() - startTime;
        getLogger().info("Plugin enabled successfully in " + duration + "ms!");
        getLogger().info("==========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling CSX LandManager...");

        // Stop tasks
        if (rentExpirationTask != null) {
            rentExpirationTask.stop();
        }

        // Close storage
        if (storageManager != null) {
            storageManager.close().join();
        }

        getLogger().info("Plugin disabled successfully!");
    }

    /**
     * Checks for required and optional dependencies.
     */
    private void checkDependencies() {
        PluginManager pm = Bukkit.getPluginManager();

        // Check GriefPrevention
        if (pm.getPlugin("GriefPrevention") != null && pm.isPluginEnabled("GriefPrevention")) {
            griefPreventionAvailable = true;
            getLogger().info("GriefPrevention found and enabled!");
        } else {
            getLogger().warning("GriefPrevention not found! Some features will not work.");
            getLogger().warning("Please install GriefPrevention: https://www.spigotmc.org/resources/griefprevention.1884/");
        }

        // Check Vault
        if (pm.getPlugin("Vault") != null && pm.isPluginEnabled("Vault")) {
            vaultAvailable = true;
            getLogger().info("Vault found and enabled!");
        } else {
            getLogger().warning("Vault not found! Economy features will not work.");
            getLogger().warning("Please install Vault: https://www.spigotmc.org/resources/vault.631/");
        }
    }

    /**
     * Loads configuration files.
     */
    private void loadConfiguration() {
        // Save default config files
        saveDefaultConfig();
        saveResource("messages.yml", false);

        // Initialize config managers
        configManager = new Config(this);
        messages = new Messages(this);

        getLogger().info("Configuration loaded.");
    }

    /**
     * Initializes the storage system.
     */
    private void initializeStorage() {
        storageManager = new StorageManager(this, configManager);

        // Initialize storage asynchronously and wait for completion
        storageManager.initialize().join();

        if (!storageManager.isReady()) {
            getLogger().severe("Failed to initialize storage! Plugin may not function correctly.");
        }
    }

    /**
     * Initializes all manager classes.
     */
    private void initializeManagers() {
        // Economy Manager (initialize even if Vault unavailable for graceful degradation)
        economyManager = new EconomyManager(this);
        if (!economyManager.isAvailable()) {
            getLogger().warning("Economy is not available. Economy features will be disabled.");
        }

        // Claim Manager (requires GriefPrevention)
        if (griefPreventionAvailable) {
            try {
                claimManager = new ClaimManager(this, storageManager);
                getLogger().info("Claim Manager initialized.");
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Failed to initialize Claim Manager", e);
                claimManager = null;
            }
        } else {
            getLogger().severe("Claim Manager cannot be initialized without GriefPrevention!");
        }

        // Rent Manager (requires Claim Manager)
        if (claimManager != null) {
            rentManager = new RentManager(this, configManager, messages, claimManager,
                    economyManager, storageManager);
            getLogger().info("Rent Manager initialized.");
        }

        // GUI Manager (requires Claim Manager)
        if (claimManager != null) {
            guiManager = new GUIManager(this, configManager, messages, claimManager);
            getLogger().info("GUI Manager initialized.");
        }
    }

    /**
     * Registers plugin commands.
     */
    private void registerCommands() {
        getCommand("landmanagement").setExecutor(new LandManagementCommand(this));
        getLogger().info("Commands registered.");
    }

    /**
     * Registers event listeners.
     */
    private void registerListeners() {
        guiListener = new GUIListener(this, guiManager);
        chatListener = new ChatListener(this, storageManager);
        playerListener = new PlayerListener(this, storageManager, chatListener, guiListener);

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(guiListener, this);
        pm.registerEvents(chatListener, this);
        pm.registerEvents(playerListener, this);

        getLogger().info("Event listeners registered.");
    }

    /**
     * Starts periodic tasks.
     */
    private void startTasks() {
        if (rentManager != null) {
            rentExpirationTask = new RentExpirationTask(this, configManager, rentManager);
            rentExpirationTask.start();
            getLogger().info("Rent expiration task started.");
        }
    }

    // Getter methods for accessing managers

    public Config getConfigManager() {
        return configManager;
    }

    public Messages getMessages() {
        return messages;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public ClaimManager getClaimManager() {
        return claimManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public RentManager getRentManager() {
        return rentManager;
    }

    public GUIManager getGUIManager() {
        return guiManager;
    }

    public GUIListener getGUIListener() {
        return guiListener;
    }

    public ChatListener getChatListener() {
        return chatListener;
    }

    /**
     * Checks if GriefPrevention is available.
     */
    public boolean isGriefPreventionAvailable() {
        return griefPreventionAvailable;
    }

    /**
     * Checks if Vault is available.
     */
    public boolean isVaultAvailable() {
        return vaultAvailable;
    }

    /**
     * Gets the messages.yml FileConfiguration.
     * This is used by the Messages class.
     */
    public FileConfiguration getMessagesConfig() {
        return org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                new java.io.File(getDataFolder(), "messages.yml"));
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        if (configManager != null) {
            configManager.reload();
        }
        if (messages != null) {
            messages.reload();
        }
    }
}
