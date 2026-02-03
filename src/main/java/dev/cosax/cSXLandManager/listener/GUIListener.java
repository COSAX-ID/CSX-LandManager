package dev.cosax.cSXLandManager.listener;

import dev.cosax.cSXLandManager.CSXLandManager;
import dev.cosax.cSXLandManager.config.Config;
import dev.cosax.cSXLandManager.gui.ClaimGUI;
import dev.cosax.cSXLandManager.gui.DurationSelectorGUI;
import dev.cosax.cSXLandManager.manager.GUIManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Handles GUI interactions.
 */
public class GUIListener implements Listener {

    private final CSXLandManager plugin;
    private final Config config;
    private final GUIManager guiManager;

    // Map of player UUID to their currently open ClaimGUI
    private final Map<UUID, ClaimGUI> openClaimGUIs = new HashMap<>();

    // Map of player UUID to their currently open DurationSelectorGUI
    private final Map<UUID, DurationSelectorGUI> openDurationGUIs = new HashMap<>();

    public GUIListener(CSXLandManager plugin, GUIManager guiManager) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.guiManager = guiManager;
    }

    /**
     * Registers an open ClaimGUI for a player.
     */
    public void registerOpenGUI(Player player, ClaimGUI gui) {
        openClaimGUIs.put(player.getUniqueId(), gui);
    }

    /**
     * Registers an open DurationSelectorGUI for a player.
     */
    public void registerOpenGUI(Player player, DurationSelectorGUI gui) {
        openDurationGUIs.put(player.getUniqueId(), gui);
    }

    /**
     * Unregisters all open GUIs for a player.
     */
    public void unregisterOpenGUI(Player player) {
        openClaimGUIs.remove(player.getUniqueId());
        openDurationGUIs.remove(player.getUniqueId());
    }

    /**
     * Clean up when player quits.
     */
    public void cleanup(Player player) {
        unregisterOpenGUI(player);
    }

    /**
     * Gets the currently open ClaimGUI for a player.
     */
    public ClaimGUI getOpenClaimGUI(Player player) {
        return openClaimGUIs.get(player.getUniqueId());
    }

    /**
     * Gets the currently open DurationSelectorGUI for a player.
     */
    public DurationSelectorGUI getOpenDurationGUI(Player player) {
        return openDurationGUIs.get(player.getUniqueId());
    }

    /**
     * Gets the currently open GUI (legacy method).
     */
    public ClaimGUI getOpenGUI(Player player) {
        return getOpenClaimGUI(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory inventory = event.getClickedInventory();
        if (inventory == null) return;

        String title = event.getView().getTitle();

        // Handle Duration Selector GUI
        if (title.contains("Select Rent Duration")) {
            event.setCancelled(true);
            DurationSelectorGUI durationGUI = getOpenDurationGUI(player);
            if (durationGUI == null) {
                // Close inventory synchronously
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.closeInventory();
                });
                return;
            }

            int slot = event.getSlot();
            durationGUI.handleClick(player, slot).thenAccept(result -> {
                // If result is null or message is "waiting", don't do anything
                // (GUI is closed and waiting for chat input)
                if (result == null) return;

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (result.message() != null && !result.message().equals("waiting")) {
                        player.sendMessage(result.message());
                    }

                    if (result.close()) {
                        player.closeInventory();
                    } else if (!result.close() && result.message() == null) {
                        // Back button - reopen claim GUI
                        guiManager.openLandGUI(player);
                    }
                });
            }).exceptionally(e -> {
                plugin.getLogger().log(Level.SEVERE, "Error handling duration click", e);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.closeInventory();
                });
                return null;
            });
            return;
        }

        // Handle Claim GUI
        if (!title.equals(config.getGuiMainTitle()) &&
            !title.equals(config.getGuiConfirmTitle()) &&
            !title.equals(config.getGuiAdminTitle())) {
            return;
        }

        // Cancel the event to prevent item movement
        event.setCancelled(true);

        // Get the open GUI
        ClaimGUI gui = getOpenGUI(player);
        if (gui == null) {
            // Close inventory synchronously
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.closeInventory();
            });
            return;
        }

        // Handle the click
        int slot = event.getSlot();
        gui.handleClick(player, slot).thenAccept(result -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (result.message() != null) {
                    player.sendMessage(result.message());
                }

                // Handle different actions
                if (result.action() == null) {
                    // Action failed or no action (GUI already closed elsewhere)
                    // But if there's a message, check if we should refresh
                    if (result.message() != null && !result.message().equals("waiting")) {
                        // For toggle/remove actions that return null action, we still need to refresh
                        // Check the message content to determine if we should refresh
                        String msg = result.message();
                        if (msg.contains("Renting disabled") || msg.contains("Renting enabled") ||
                            msg.contains("Sale disabled") || msg.contains("Claim is now for sale") ||
                            msg.contains("removed from sale")) {
                            guiManager.openLandGUI(player);
                        }
                    }
                    return;
                }

                switch (result.action()) {
                    case CLOSE -> player.closeInventory();
                    case RENT, BUY, EXTEND, TOGGLE_AUTO_RENEW, REMOVE_FROM_SALE, CANCEL_RENT,
                         TOGGLE_RENT, TOGGLE_SALE -> {
                        // Refresh GUI to show updated state
                        guiManager.openLandGUI(player);
                    }
                    case SET_RENT_PRICE, SET_SELL_PRICE -> {
                        // GUI already closed, player is in chat input mode
                        // Don't reopen
                    }
                    case REFRESH -> {
                        // Explicit refresh
                        guiManager.openLandGUI(player);
                    }
                }
            });
        }).exceptionally(e -> {
            plugin.getLogger().log(Level.SEVERE, "Error handling GUI click", e);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.closeInventory();
            });
            return null;
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Check if this is our GUI
        String title = event.getView().getTitle();
        if (!title.equals(config.getGuiMainTitle()) &&
            !title.equals(config.getGuiConfirmTitle()) &&
            !title.equals(config.getGuiAdminTitle())) {
            return;
        }

        // Cancel all drag events to prevent item movement
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        // Clean up GUI registration when inventory is closed
        String title = event.getView().getTitle();
        if (title.equals(config.getGuiMainTitle()) ||
            title.equals(config.getGuiConfirmTitle()) ||
            title.equals(config.getGuiAdminTitle())) {
            unregisterOpenGUI(player);
        }
    }
}

