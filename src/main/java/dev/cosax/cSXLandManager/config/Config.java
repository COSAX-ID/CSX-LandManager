package dev.cosax.cSXLandManager.config;

import dev.cosax.cSXLandManager.CSXLandManager;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/**
 * Configuration handler for CSX LandManager.
 * Loads and provides access to config.yml settings.
 */
public class Config {

    private final CSXLandManager plugin;
    private final FileConfiguration config;

    // Cached values
    private StorageType storageType;
    private long defaultRentDuration;
    private long expirationCheckInterval;
    private boolean autoRenewEnabled;
    private boolean economyEnabled;
    private boolean adminBypassPayment;
    private boolean soundsEnabled;
    private int guiSize;
    private double minRentPrice;
    private double maxRentPrice;
    private double minSellPrice;
    private double maxSellPrice;
    private long maxRentDuration;
    private long minRentDuration;
    private boolean debug;

    // Database settings
    private String dbHost;
    private int dbPort;
    private String dbDatabase;
    private String dbUsername;
    private String dbPassword;
    private int dbPoolSize;
    private long dbConnectionTimeout;
    private long dbMaxLifetime;

    public Config(CSXLandManager plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
        loadValues();
    }

    /**
     * Reloads the configuration from disk.
     */
    public void reload() {
        plugin.reloadConfig();
        loadValues();
    }

    /**
     * Loads all configuration values into memory.
     */
    private void loadValues() {
        // Storage type
        String storageTypeStr = config.getString("storage.type", "YAML").toUpperCase();
        try {
            this.storageType = StorageType.valueOf(storageTypeStr);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid storage type: " + storageTypeStr + ", defaulting to YAML");
            this.storageType = StorageType.YAML;
        }

        // Database settings
        this.dbHost = config.getString("storage.mysql.host", "localhost");
        this.dbPort = config.getInt("storage.mysql.port", 3306);
        this.dbDatabase = config.getString("storage.mysql.database", "minecraft");
        this.dbUsername = config.getString("storage.mysql.username", "minecraft");
        this.dbPassword = config.getString("storage.mysql.password", "password");
        this.dbPoolSize = config.getInt("storage.mysql.pool-size", 10);
        this.dbConnectionTimeout = config.getLong("storage.mysql.connection-timeout", 30000);
        this.dbMaxLifetime = config.getLong("storage.mysql.max-lifetime", 1800000);

        // Rent settings
        this.autoRenewEnabled = config.getBoolean("rent.auto-renew-enabled", true);
        this.defaultRentDuration = config.getLong("rent.default-duration", 604800000L); // 1 week
        this.expirationCheckInterval = config.getInt("rent.expiration-check-interval", 1200); // 1 minute

        // Economy settings
        this.economyEnabled = config.getBoolean("economy.enabled", true);
        this.adminBypassPayment = config.getBoolean("economy.admin-bypass-payment", true);

        // GUI settings
        this.guiSize = config.getInt("gui.size", 54);
        this.soundsEnabled = config.getBoolean("gui.sounds.enabled", true);

        // Claim settings
        this.minRentPrice = config.getDouble("claim.min-rent-price", 0.0);
        this.maxRentPrice = config.getDouble("claim.max-rent-price", 0.0);
        this.minSellPrice = config.getDouble("claim.min-sell-price", 0.0);
        this.maxSellPrice = config.getDouble("claim.max-sell-price", 0.0);
        this.maxRentDuration = config.getLong("claim.max-rent-duration", 0);
        this.minRentDuration = config.getLong("claim.min-rent-duration", 60000L); // 1 minute

        // Debug
        this.debug = config.getBoolean("debug", false);
    }

    // Getters
    public StorageType getStorageType() { return storageType; }
    public long getDefaultRentDuration() { return defaultRentDuration; }
    public long getExpirationCheckInterval() { return expirationCheckInterval; }
    public boolean isAutoRenewEnabled() { return autoRenewEnabled; }
    public boolean isEconomyEnabled() { return economyEnabled; }
    public boolean isAdminBypassPayment() { return adminBypassPayment; }
    public boolean isSoundsEnabled() { return soundsEnabled; }
    public int getGuiSize() { return guiSize; }
    public double getMinRentPrice() { return minRentPrice; }
    public double getMaxRentPrice() { return maxRentPrice; }
    public double getMinSellPrice() { return minSellPrice; }
    public double getMaxSellPrice() { return maxSellPrice; }
    public long getMaxRentDuration() { return maxRentDuration; }
    public long getMinRentDuration() { return minRentDuration; }
    public boolean isDebug() { return debug; }

    // Database getters
    public String getDbHost() { return dbHost; }
    public int getDbPort() { return dbPort; }
    public String getDbDatabase() { return dbDatabase; }
    public String getDbUsername() { return dbUsername; }
    public String getDbPassword() { return dbPassword; }
    public int getDbPoolSize() { return dbPoolSize; }
    public long getDbConnectionTimeout() { return dbConnectionTimeout; }
    public long getDbMaxLifetime() { return dbMaxLifetime; }

    // GUI string getters
    public String getGuiMainTitle() {
        return colorize(config.getString("gui.titles.main", "&6Land Management"));
    }

    public String getGuiConfirmTitle() {
        return colorize(config.getString("gui.titles.confirm", "&eConfirm Action"));
    }

    public String getGuiAdminTitle() {
        return colorize(config.getString("gui.titles.admin", "&cAdmin Controls"));
    }

    public String getSoundClick() {
        return config.getString("gui.sounds.click", "UI_BUTTON_CLICK");
    }

    public String getSoundSuccess() {
        return config.getString("gui.sounds.success", "ENTITY_PLAYER_LEVELUP");
    }

    public String getSoundError() {
        return config.getString("gui.sounds.error", "ENTITY_VILLAGER_NO");
    }

    public String getSoundRentSuccess() {
        return config.getString("gui.sounds.rent-success", "ENTITY_EXPERIENCE_ORB_PICKUP");
    }

    public String getSoundBuySuccess() {
        return config.getString("gui.sounds.buy-success", "ENTITY_PLAYER_LEVELUP");
    }

    /**
     * Colorizes a string using Bukkit's color codes.
     */
    private String colorize(String string) {
        if (string == null) return "";
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', string);
    }

    /**
     * Enum for available storage types.
     */
    public enum StorageType {
        YAML,
        SQLITE,
        H2,
        MYSQL,
        MARIADB
    }
}
