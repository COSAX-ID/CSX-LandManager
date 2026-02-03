package dev.cosax.cSXLandManager.manager;

import dev.cosax.cSXLandManager.CSXLandManager;
import dev.cosax.cSXLandManager.model.ClaimData;
import dev.cosax.cSXLandManager.model.RentStatus;
import dev.cosax.cSXLandManager.storage.StorageManager;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Manages GriefPrevention claim integration.
 */
public class ClaimManager {

    private final CSXLandManager plugin;
    private final GriefPrevention griefPrevention;
    private final StorageManager storageManager;

    public ClaimManager(CSXLandManager plugin, StorageManager storageManager) {
        this.plugin = plugin;
        this.storageManager = storageManager;

        // Get GriefPrevention instance
        this.griefPrevention = (GriefPrevention) Bukkit.getPluginManager().getPlugin("GriefPrevention");

        if (griefPrevention == null) {
            throw new IllegalStateException("GriefPrevention plugin not found!");
        }
    }

    /**
     * Gets the claim at a player's location.
     */
    public Claim getClaimAtLocation(Location location) {
        return griefPrevention.dataStore.getClaimAt(location, true, null);
    }

    /**
     * Gets the claim at a player's location (async-safe).
     */
    public CompletableFuture<Optional<Claim>> getClaimAtPlayer(Player player) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(getClaimAtLocation(player.getLocation())));
    }

    /**
     * Gets a claim by its ID.
     */
    public Claim getClaimById(long claimId) {
        return griefPrevention.dataStore.getClaim(claimId);
    }

    /**
     * Gets claim data for a claim ID.
     */
    public CompletableFuture<Optional<ClaimData>> getClaimData(long claimId) {
        return storageManager.loadClaimData(claimId);
    }

    /**
     * Gets claim data for a claim at player's location.
     */
    public CompletableFuture<Optional<ClaimData>> getClaimDataAtPlayer(Player player) {
        return getClaimAtPlayer(player).thenCompose(claimOpt -> {
            if (claimOpt.isEmpty()) {
                return CompletableFuture.completedFuture(Optional.empty());
            }
            return getClaimData(claimOpt.get().getID());
        });
    }

    /**
     * Loads or creates claim data for a GriefPrevention claim.
     */
    public CompletableFuture<ClaimData> getOrCreateClaimData(Claim claim) {
        return storageManager.loadClaimData(claim.getID()).thenCompose(dataOpt -> {
            if (dataOpt.isPresent()) {
                return CompletableFuture.completedFuture(dataOpt.get());
            }

            // Create new claim data and properly chain the save operation
            ClaimData newData = new ClaimData(claim.getID(), claim.getOwnerID());
            return storageManager.saveClaimData(newData).thenApply(v -> newData);
        });
    }

    /**
     * Checks if a player is the owner of a claim.
     */
    public boolean isClaimOwner(Player player, Claim claim) {
        UUID ownerId = claim.getOwnerID();
        return ownerId != null && ownerId.equals(player.getUniqueId());
    }

    /**
     * Checks if a player is the owner of the claim at their location.
     */
    public CompletableFuture<Boolean> isClaimOwnerAtLocation(Player player) {
        return getClaimAtPlayer(player).thenApply(claimOpt ->
            claimOpt.isPresent() && isClaimOwner(player, claimOpt.get())
        );
    }

    /**
     * Gets the area (size) of a claim in blocks.
     */
    public int getClaimArea(Claim claim) {
        return claim.getArea();
    }

    /**
     * Gets claim owner name.
     */
    public String getClaimOwnerName(Claim claim) {
        UUID ownerId = claim.getOwnerID();
        if (ownerId == null) {
            return "Admin/ Wilderness";
        }

        OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerId);
        return owner.getName() != null ? owner.getName() : "Unknown";
    }

    /**
     * Gets player name from UUID.
     */
    public String getPlayerName(UUID uuid) {
        if (uuid == null) return "Unknown";
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName() != null ? player.getName() : "Unknown";
    }

    /**
     * Transfers claim ownership to another player.
     */
    public CompletableFuture<Boolean> transferOwnership(Claim claim, UUID newOwner) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UUID oldOwner = claim.getOwnerID();
                long claimId = claim.getID().hashCode(); // Use hashCode as unique ID

                // Update GriefPrevention claim - try multiple methods for better compatibility
                boolean gpUpdateSuccess = false;
                Exception lastException = null;

                // Method 1: Try to access 'owner' field (most common in GP 16.x)
                try {
                    java.lang.reflect.Field ownerField = Claim.class.getDeclaredField("owner");
                    ownerField.setAccessible(true);
                    ownerField.set(claim, newOwner.toString());
                    gpUpdateSuccess = true;
                    plugin.getLogger().fine("Updated claim ownership using 'owner' field");
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    lastException = e;
                    plugin.getLogger().fine("Could not access 'owner' field, trying alternatives...");
                }

                // Method 2: Try 'ownerID' field (some versions use this)
                if (!gpUpdateSuccess) {
                    try {
                        java.lang.reflect.Field ownerField = Claim.class.getDeclaredField("ownerID");
                        ownerField.setAccessible(true);
                        ownerField.set(claim, newOwner);
                        gpUpdateSuccess = true;
                        plugin.getLogger().fine("Updated claim ownership using 'ownerID' field");
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        lastException = e;
                        plugin.getLogger().fine("Could not access 'ownerID' field, trying next method...");
                    }
                }

                // Method 3: Try public field access (some GP versions have public owner field)
                if (!gpUpdateSuccess) {
                    try {
                        java.lang.reflect.Field ownerField = Claim.class.getField("owner");
                        ownerField.set(claim, newOwner.toString());
                        gpUpdateSuccess = true;
                        plugin.getLogger().fine("Updated claim ownership using public 'owner' field");
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        lastException = e;
                        plugin.getLogger().fine("Could not access public 'owner' field");
                    }
                }

                // Method 4: Try getOwnerID() and setOwnerID() if available via reflection
                if (!gpUpdateSuccess) {
                    try {
                        try {
                            java.lang.reflect.Method setOwnerMethod = Claim.class.getDeclaredMethod("setOwnerID", UUID.class);
                            setOwnerMethod.setAccessible(true);
                            setOwnerMethod.invoke(claim, newOwner);
                            gpUpdateSuccess = true;
                            plugin.getLogger().fine("Updated claim ownership using setOwnerID() method");
                        } catch (NoSuchMethodException e) {
                            // Try with String parameter
                            java.lang.reflect.Method setOwnerMethod = Claim.class.getDeclaredMethod("setOwnerID", String.class);
                            setOwnerMethod.setAccessible(true);
                            setOwnerMethod.invoke(claim, newOwner.toString());
                            gpUpdateSuccess = true;
                            plugin.getLogger().fine("Updated claim ownership using setOwnerID(String) method");
                        }
                    } catch (Exception e) {
                        lastException = e;
                        plugin.getLogger().fine("Could not use setOwnerID() method");
                    }
                }

                // If all methods failed, log detailed error
                if (!gpUpdateSuccess) {
                    plugin.getLogger().severe("Could not update GriefPrevention claim ownership. All methods failed.");
                    if (lastException != null) {
                        plugin.getLogger().log(Level.SEVERE, "Last exception details:", lastException);
                    }
                    plugin.getLogger().info("Available fields in Claim class:");
                    for (java.lang.reflect.Field field : Claim.class.getDeclaredFields()) {
                        plugin.getLogger().info("  - " + field.getName() + " (" + field.getType().getName() + ")");
                    }
                    return false;
                }

                // Save GriefPrevention data
                griefPrevention.dataStore.saveClaim(claim);
                plugin.getLogger().info("GriefPrevention claim saved successfully for claim ID: " + claimId);

                // Update claim data using proper async chaining
                return storageManager.loadClaimData(claimId).thenCompose(dataOpt -> {
                    if (dataOpt.isPresent()) {
                        ClaimData data = dataOpt.get();
                        data.setOwner(newOwner);
                        data.setStatus(RentStatus.PRIVATE);
                        data.setRenter(null);
                        data.setRentPrice(0);
                        return storageManager.saveClaimData(data).thenApply(v -> true);
                    }
                    return CompletableFuture.completedFuture(true);
                }).join();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to transfer ownership", e);
                return false;
            }
        });
    }

    /**
     * Adds builder trust to a claim for a player.
     */
    public CompletableFuture<Boolean> addTrust(Claim claim, UUID player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PlayerData playerData = griefPrevention.dataStore.getPlayerData(player);
                String playerName = Bukkit.getOfflinePlayer(player).getName();

                if (!claim.managers.contains(playerName)) {
                    claim.managers.add(playerName);
                    griefPrevention.dataStore.saveClaim(claim);
                    return true;
                }
                return false;
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to add trust", e);
                return false;
            }
        });
    }

    /**
     * Removes builder trust from a claim for a player.
     */
    public CompletableFuture<Boolean> removeTrust(Claim claim, UUID player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PlayerData playerData = griefPrevention.dataStore.getPlayerData(player);
                String playerName = Bukkit.getOfflinePlayer(player).getName();

                if (claim.managers.remove(playerName)) {
                    griefPrevention.dataStore.saveClaim(claim);
                    return true;
                }
                return false;
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to remove trust", e);
                return false;
            }
        });
    }

    /**
     * Checks if a player has builder trust on a claim.
     */
    public boolean hasTrust(Claim claim, UUID player) {
        String playerName = Bukkit.getOfflinePlayer(player).getName();
        return claim.managers.contains(playerName);
    }

    /**
     * Checks if GriefPrevention is available.
     */
    public boolean isGriefPreventionAvailable() {
        return griefPrevention != null && griefPrevention.isEnabled();
    }

    /**
     * Visualizes a claim's boundaries for a player.
     * Note: This method requires GriefPrevention's visualization features.
     */
    public void visualizeClaim(Player player, Claim claim) {
        // Visualization is handled by GriefPrevention's built-in system
        // when players use golden shovels or enter claims
        // This is a placeholder for future enhancement
    }
}
