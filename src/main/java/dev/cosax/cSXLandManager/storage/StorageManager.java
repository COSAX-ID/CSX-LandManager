package dev.cosax.cSXLandManager.storage;

import dev.cosax.cSXLandManager.CSXLandManager;
import dev.cosax.cSXLandManager.config.Config;
import dev.cosax.cSXLandManager.storage.impl.*;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Manages storage operations and delegates to the appropriate storage provider.
 */
public class StorageManager {

    private final CSXLandManager plugin;
    private final Config config;
    private StorageProvider storageProvider;

    public StorageManager(CSXLandManager plugin, Config config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * Initializes the storage provider based on configuration.
     */
    public CompletableFuture<Void> initialize() {
        Config.StorageType type = config.getStorageType();

        plugin.getLogger().info("Initializing storage: " + type);

        switch (type) {
            case YAML -> storageProvider = new YAMLStorage(plugin);
            case SQLITE -> storageProvider = new SQLiteStorage(plugin);
            case H2 -> storageProvider = new H2Storage(plugin);
            case MYSQL -> storageProvider = new MySQLStorage(plugin, config);
            case MARIADB -> storageProvider = new MariaDBStorage(plugin, config);
            default -> {
                plugin.getLogger().warning("Unknown storage type: " + type + ", defaulting to YAML");
                storageProvider = new YAMLStorage(plugin);
            }
        }

        return storageProvider.initialize().whenComplete((v, throwable) -> {
            if (throwable != null) {
                plugin.getLogger().log(Level.SEVERE, "Failed to initialize storage", throwable);
            }
        });
    }

    /**
     * Closes the storage provider.
     */
    public CompletableFuture<Void> close() {
        if (storageProvider == null) {
            return CompletableFuture.completedFuture(null);
        }
        return storageProvider.close();
    }

    /**
     * Gets the current storage provider.
     */
    public StorageProvider getStorageProvider() {
        return storageProvider;
    }

    /**
     * Checks if storage is ready.
     */
    public boolean isReady() {
        return storageProvider != null && storageProvider.isReady();
    }

    // Delegate methods to storage provider

    public CompletableFuture<java.util.Optional<dev.cosax.cSXLandManager.model.ClaimData>> loadClaimData(long claimId) {
        return storageProvider.loadClaimData(claimId);
    }

    public CompletableFuture<Void> saveClaimData(dev.cosax.cSXLandManager.model.ClaimData claimData) {
        return storageProvider.saveClaimData(claimData);
    }

    public CompletableFuture<Void> deleteClaimData(long claimId) {
        return storageProvider.deleteClaimData(claimId);
    }

    public CompletableFuture<java.util.List<dev.cosax.cSXLandManager.model.ClaimData>> getAllClaims() {
        return storageProvider.getAllClaims();
    }

    public CompletableFuture<java.util.List<dev.cosax.cSXLandManager.model.ClaimData>> getClaimsByOwner(java.util.UUID owner) {
        return storageProvider.getClaimsByOwner(owner);
    }

    public CompletableFuture<java.util.List<dev.cosax.cSXLandManager.model.ClaimData>> getClaimsByRenter(java.util.UUID renter) {
        return storageProvider.getClaimsByRenter(renter);
    }

    public CompletableFuture<java.util.List<dev.cosax.cSXLandManager.model.ClaimData>> getRentedClaims() {
        return storageProvider.getRentedClaims();
    }
}
