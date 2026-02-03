package dev.cosax.cSXLandManager.listener;

import dev.cosax.cSXLandManager.CSXLandManager;
import dev.cosax.cSXLandManager.storage.StorageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Handles player-related events.
 */
public class PlayerListener implements Listener {

    private final CSXLandManager plugin;
    private final StorageManager storageManager;
    private final ChatListener chatListener;
    private final GUIListener guiListener;

    public PlayerListener(CSXLandManager plugin, StorageManager storageManager, ChatListener chatListener, GUIListener guiListener) {
        this.plugin = plugin;
        this.storageManager = storageManager;
        this.chatListener = chatListener;
        this.guiListener = guiListener;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        // Cleanup chat listener
        chatListener.cleanup(player);

        // Cleanup GUI listener
        guiListener.cleanup(player);

        // Perform cleanup when a player quits
        // This is a safety check to ensure data consistency
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Check if player has any rented claims
                storageManager.getClaimsByRenter(playerUuid).thenAccept(claims -> {
                    if (!claims.isEmpty()) {
                        plugin.getLogger().info("Player " + player.getName() + " has " +
                                claims.size() + " rented claim(s) at logout");
                    }
                }).exceptionally(e -> {
                    plugin.getLogger().log(Level.WARNING, "Failed to check rented claims for player " +
                            player.getName(), e);
                    return null;
                });
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error in PlayerQuitEvent handler", e);
            }
        });
    }
}
