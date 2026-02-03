package dev.cosax.cSXLandManager.storage.impl;

import dev.cosax.cSXLandManager.CSXLandManager;
import dev.cosax.cSXLandManager.model.ClaimData;
import dev.cosax.cSXLandManager.model.RentStatus;
import dev.cosax.cSXLandManager.storage.StorageProvider;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * YAML-based storage implementation.
 * Stores claim data in a claims.yml file in the plugin data folder.
 */
public class YAMLStorage implements StorageProvider {

    private final CSXLandManager plugin;
    private File dataFile;
    private boolean ready = false;

    public YAMLStorage(CSXLandManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try {
                dataFile = new File(plugin.getDataFolder(), "claims.yml");
                if (!dataFile.exists()) {
                    dataFile.getParentFile().mkdirs();
                    dataFile.createNewFile();
                }
                ready = true;
                plugin.getLogger().info("YAML storage initialized successfully.");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to initialize YAML storage", e);
                ready = false;
            }
        });
    }

    @Override
    public CompletableFuture<Void> close() {
        return CompletableFuture.runAsync(() -> {
            ready = false;
            plugin.getLogger().info("YAML storage closed.");
        });
    }

    @Override
    public CompletableFuture<Optional<ClaimData>> loadClaimData(long claimId) {
        return CompletableFuture.supplyAsync(() -> {
            if (!ready) return Optional.empty();

            try {
                var config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(dataFile);
                String path = "claims." + claimId;

                if (!config.contains(path)) {
                    return Optional.empty();
                }

                ClaimData data = deserializeClaim(config.getConfigurationSection(path));
                return Optional.ofNullable(data);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load claim data for ID: " + claimId, e);
                return Optional.empty();
            }
        });
    }

    @Override
    public CompletableFuture<Void> saveClaimData(ClaimData claimData) {
        return CompletableFuture.runAsync(() -> {
            if (!ready) return;

            try {
                var config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(dataFile);
                String path = "claims." + claimData.getClaimId();

                serializeClaim(config, path, claimData);
                config.save(dataFile);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to save claim data for ID: " + claimData.getClaimId(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteClaimData(long claimId) {
        return CompletableFuture.runAsync(() -> {
            if (!ready) return;

            try {
                var config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(dataFile);
                config.set("claims." + claimId, null);
                config.save(dataFile);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to delete claim data for ID: " + claimId, e);
            }
        });
    }

    @Override
    public CompletableFuture<List<ClaimData>> getAllClaims() {
        return CompletableFuture.supplyAsync(() -> {
            if (!ready) return Collections.emptyList();

            try {
                var config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(dataFile);
                var claimsSection = config.getConfigurationSection("claims");

                if (claimsSection == null) {
                    return Collections.emptyList();
                }

                List<ClaimData> claims = new ArrayList<>();
                for (String key : claimsSection.getKeys(false)) {
                    try {
                        var claimSection = claimsSection.getConfigurationSection(key);
                        ClaimData data = deserializeClaim(claimSection);
                        if (data != null) {
                            claims.add(data);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Failed to load claim: " + key, e);
                    }
                }
                return claims;
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load all claims", e);
                return Collections.emptyList();
            }
        });
    }

    @Override
    public CompletableFuture<List<ClaimData>> getClaimsByOwner(UUID owner) {
        return getAllClaims().thenApply(claims -> {
            List<ClaimData> result = new ArrayList<>();
            for (ClaimData claim : claims) {
                if (owner.equals(claim.getOwner())) {
                    result.add(claim);
                }
            }
            return result;
        });
    }

    @Override
    public CompletableFuture<List<ClaimData>> getClaimsByRenter(UUID renter) {
        return getAllClaims().thenApply(claims -> {
            List<ClaimData> result = new ArrayList<>();
            for (ClaimData claim : claims) {
                if (renter.equals(claim.getRenter())) {
                    result.add(claim);
                }
            }
            return result;
        });
    }

    @Override
    public CompletableFuture<List<ClaimData>> getRentedClaims() {
        return getAllClaims().thenApply(claims -> {
            List<ClaimData> result = new ArrayList<>();
            for (ClaimData claim : claims) {
                if (claim.isRented()) {
                    result.add(claim);
                }
            }
            return result;
        });
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    /**
     * Serializes ClaimData to YAML configuration.
     */
    private void serializeClaim(org.bukkit.configuration.ConfigurationSection config, String path, ClaimData data) {
        config.set(path + ".claimId", data.getClaimId());
        config.set(path + ".owner", data.getOwner() != null ? data.getOwner().toString() : null);
        config.set(path + ".originalOwner", data.getOriginalOwner() != null ? data.getOriginalOwner().toString() : null);
        config.set(path + ".renter", data.getRenter() != null ? data.getRenter().toString() : null);
        config.set(path + ".rentPrice", data.getRentPrice());
        config.set(path + ".sellPrice", data.getSellPrice());
        config.set(path + ".rentDuration", data.getRentDuration());
        config.set(path + ".autoRenew", data.isAutoRenew());
        config.set(path + ".rentStartTime", data.getRentStartTime());
        config.set(path + ".rentEndTime", data.getRentEndTime());
        config.set(path + ".status", data.getStatus().name());
    }

    /**
     * Deserializes ClaimData from YAML configuration.
     */
    private ClaimData deserializeClaim(org.bukkit.configuration.ConfigurationSection section) {
        if (section == null) return null;

        try {
            long claimId = section.getLong("claimId");
            UUID owner = section.getString("owner") != null ? UUID.fromString(section.getString("owner")) : null;
            UUID originalOwner = null;
            if (section.contains("originalOwner") && section.getString("originalOwner") != null) {
                try {
                    originalOwner = UUID.fromString(section.getString("originalOwner"));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().fine("Invalid originalOwner UUID, using null");
                }
            }
            UUID renter = section.getString("renter") != null ? UUID.fromString(section.getString("renter")) : null;
            double rentPrice = section.getDouble("rentPrice");
            double sellPrice = section.getDouble("sellPrice");
            long rentDuration = section.getLong("rentDuration");
            boolean autoRenew = section.getBoolean("autoRenew");
            long rentStartTime = section.getLong("rentStartTime");
            long rentEndTime = section.getLong("rentEndTime");
            RentStatus status = RentStatus.valueOf(section.getString("status", "PRIVATE"));

            return new ClaimData(claimId, owner, originalOwner, renter, rentPrice, sellPrice,
                    rentDuration, autoRenew, rentStartTime, rentEndTime, status);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to deserialize claim data", e);
            return null;
        }
    }
}
