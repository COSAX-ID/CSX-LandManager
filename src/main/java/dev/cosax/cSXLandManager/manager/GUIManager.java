package dev.cosax.cSXLandManager.manager;

import dev.cosax.cSXLandManager.CSXLandManager;
import dev.cosax.cSXLandManager.config.Config;
import dev.cosax.cSXLandManager.config.Messages;
import dev.cosax.cSXLandManager.gui.ClaimGUI;
import dev.cosax.cSXLandManager.model.ClaimData;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Manages GUI operations.
 */
public class GUIManager {

    private final CSXLandManager plugin;
    private final Config config;
    private final Messages messages;
    private final ClaimManager claimManager;

    public GUIManager(CSXLandManager plugin, Config config, Messages messages, ClaimManager claimManager) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.claimManager = claimManager;
    }

    /**
     * Opens the land management GUI for a player at their current location.
     */
    public void openLandGUI(Player player) {
        // Get claim at player's location
        Claim claim = claimManager.getClaimAtLocation(player.getLocation());

        if (claim == null) {
            player.sendMessage(messages.getNotInClaim());
            playSound(player, config.getSoundError());
            return;
        }

        openLandGUI(player, claim);
    }

    /**
     * Opens the land management GUI for a specific claim.
     */
    public void openLandGUI(Player player, Claim claim) {
        // Check if player has permission
        if (!player.hasPermission("landmgmt.use")) {
            player.sendMessage(messages.getNoPermission());
            playSound(player, config.getSoundError());
            return;
        }

        // Load claim data and open GUI
        plugin.getStorageManager().loadClaimData(claim.getID())
            .thenAccept(dataOpt -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (dataOpt.isPresent()) {
                        // Claim data exists, use it
                        ClaimData claimData = dataOpt.get();
                        ClaimGUI gui = new ClaimGUI(plugin, claim, claimData);
                        gui.open(player);
                    } else {
                        // Create new claim data
                        plugin.getClaimManager().getOrCreateClaimData(claim)
                            .thenAccept(claimData -> {
                                plugin.getServer().getScheduler().runTask(plugin, () -> {
                                    ClaimGUI gui = new ClaimGUI(plugin, claim, claimData);
                                    gui.open(player);
                                });
                            })
                            .exceptionally(e -> {
                                plugin.getLogger().log(Level.SEVERE, "Failed to create claim data", e);
                                plugin.getServer().getScheduler().runTask(plugin, () -> {
                                    player.sendMessage(messages.getErrorOccurred());
                                    playSound(player, config.getSoundError());
                                });
                                return null;
                            });
                    }
                });
            })
            .exceptionally(e -> {
                plugin.getLogger().log(Level.SEVERE, "Failed to load claim data for GUI", e);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage(messages.getErrorOccurred());
                    playSound(player, config.getSoundError());
                });
                return null;
            });
    }

    /**
     * Opens the land management GUI for a specific claim ID.
     */
    public void openLandGUI(Player player, long claimId) {
        Claim claim = claimManager.getClaimById(claimId);

        if (claim == null) {
            player.sendMessage(messages.getClaimNotFound());
            playSound(player, config.getSoundError());
            return;
        }

        openLandGUI(player, claim);
    }

    /**
     * Opens the land management GUI with specific claim data.
     */
    public CompletableFuture<Void> openLandGUIWithData(Player player, ClaimData claimData) {
        return CompletableFuture.runAsync(() -> {
            Claim claim = claimManager.getClaimById(claimData.getClaimId());

            if (claim == null) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage(messages.getClaimNotFound());
                    playSound(player, config.getSoundError());
                });
                return;
            }

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                ClaimGUI gui = new ClaimGUI(plugin, claim, claimData);
                gui.open(player);
            });
        });
    }

    /**
     * Plays a sound to a player.
     */
    public void playSound(Player player, String soundName) {
        if (!config.isSoundsEnabled()) return;

        try {
            org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            // Invalid sound name, ignore
        }
    }

    /**
     * Plays a click sound to a player.
     */
    public void playClickSound(Player player) {
        playSound(player, config.getSoundClick());
    }

    /**
     * Plays a success sound to a player.
     */
    public void playSuccessSound(Player player) {
        playSound(player, config.getSoundSuccess());
    }

    /**
     * Plays an error sound to a player.
     */
    public void playErrorSound(Player player) {
        playSound(player, config.getSoundError());
    }

    /**
     * Plays a rent success sound to a player.
     */
    public void playRentSuccessSound(Player player) {
        playSound(player, config.getSoundRentSuccess());
    }

    /**
     * Plays a buy success sound to a player.
     */
    public void playBuySuccessSound(Player player) {
        playSound(player, config.getSoundBuySuccess());
    }
}
