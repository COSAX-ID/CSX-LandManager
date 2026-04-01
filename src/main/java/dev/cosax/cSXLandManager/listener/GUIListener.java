package dev.cosax.cSXLandManager.listener;

import dev.cosax.cSXLandManager.CSXLandManager;
import dev.cosax.cSXLandManager.config.Config;
import dev.cosax.cSXLandManager.gui.ClaimGUI;
import dev.cosax.cSXLandManager.gui.ConfirmationGUI;
import dev.cosax.cSXLandManager.gui.DurationSelectorGUI;
import dev.cosax.cSXLandManager.gui.ShopGUI;
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
import java.util.concurrent.CompletableFuture;
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

    // Map of player UUID to their currently open ShopGUI
    private final Map<UUID, ShopGUI> openShopGUIs = new HashMap<>();

    // Map of player UUID to their currently open ConfirmationGUI
    private final Map<UUID, ConfirmationGUI> openConfirmationGUIs = new HashMap<>();

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
     * Registers an open ShopGUI for a player.
     */
    public void registerOpenGUI(Player player, ShopGUI gui) {
        openShopGUIs.put(player.getUniqueId(), gui);
    }

    /**
     * Registers an open ConfirmationGUI for a player.
     */
    public void registerOpenGUI(Player player, ConfirmationGUI gui) {
        openConfirmationGUIs.put(player.getUniqueId(), gui);
    }

    /**
     * Unregisters all open GUIs for a player.
     */
    public void unregisterOpenGUI(Player player) {
        openClaimGUIs.remove(player.getUniqueId());
        openDurationGUIs.remove(player.getUniqueId());
        openShopGUIs.remove(player.getUniqueId());
        openConfirmationGUIs.remove(player.getUniqueId());
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

    /**
     * Gets the currently open ShopGUI for a player.
     */
    public ShopGUI getOpenShopGUI(Player player) {
        return openShopGUIs.get(player.getUniqueId());
    }

    /**
     * Gets the currently open ConfirmationGUI for a player.
     */
    public ConfirmationGUI getOpenConfirmationGUI(Player player) {
        return openConfirmationGUIs.get(player.getUniqueId());
    }

    /**
     * Handles buy confirmation from ConfirmationGUI.
     * FIX: Properly updates claim data after ownership transfer.
     */
    private void handleBuyConfirmation(Player player, dev.cosax.cSXLandManager.model.ClaimData claimData) {
        plugin.getLogger().info("Processing buy confirmation for " + player.getName());

        // Check if player is owner
        if (claimData.isOwner(player.getUniqueId())) {
            player.sendMessage("§cYou cannot buy your own claim!");
            guiManager.playErrorSound(player);
            guiManager.openLandGUI(player);
            return;
        }

        // Check if player is currently renting
        if (claimData.isRenter(player.getUniqueId())) {
            player.sendMessage("§cYou cannot buy a claim you're currently renting!");
            guiManager.playErrorSound(player);
            guiManager.openLandGUI(player);
            return;
        }

        // Check price
        double price = claimData.getSellPrice();
        if (price <= 0) {
            player.sendMessage("§cThis claim is not for sale!");
            guiManager.playErrorSound(player);
            guiManager.openLandGUI(player);
            return;
        }

        // Get claim
        me.ryanhamshire.GriefPrevention.Claim claim = plugin.getClaimManager().getClaimById(claimData.getClaimId());
        if (claim == null) {
            player.sendMessage("§cClaim not found!");
            guiManager.playErrorSound(player);
            guiManager.openLandGUI(player);
            return;
        }

        // Process payment
        if (plugin.getEconomyManager().isAvailable() && price > 0 && !player.hasPermission("landmgmt.bypass")) {
            if (claimData.getOwner() == null) {
                player.sendMessage("§cCannot buy: No owner found!");
                guiManager.playErrorSound(player);
                return;
            }

            plugin.getEconomyManager().processPayment(
                    player.getUniqueId(),
                    claimData.getOwner(),
                    price,
                    "Purchase of claim " + claimData.getClaimId()
                ).thenAccept(paymentResult -> {
                    if (!paymentResult.success()) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            player.sendMessage("§cPayment failed: " + paymentResult.message());
                            guiManager.playErrorSound(player);
                            guiManager.openLandGUI(player);
                        });
                        return;
                    }

                    plugin.getLogger().info("Buy payment successful for " + player.getName() + ", transferring ownership...");

                    // Transfer ownership
                    plugin.getClaimManager().transferOwnership(claim, player.getUniqueId()).thenAccept(success -> {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            if (success) {
                                // FIX: Update claim data with new owner
                                claimData.setOwner(player.getUniqueId());
                                claimData.setSellPrice(0);
                                claimData.setStatus(dev.cosax.cSXLandManager.model.RentStatus.PRIVATE);
                                claimData.setRenter(null);
                                claimData.setOriginalOwner(null);
                                
                                // Save updated claim data
                                plugin.getStorageManager().saveClaimData(claimData).thenRun(() -> {
                                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                                        player.sendMessage("§aYou have successfully purchased this claim for " +
                                            plugin.getEconomyManager().formatAmount(price) + "!");
                                        guiManager.playBuySuccessSound(player);
                                        player.closeInventory();

                                        // Notify seller
                                        UUID previousOwner = claimData.getOwner();
                                        if (previousOwner != null) {
                                            org.bukkit.OfflinePlayer seller = org.bukkit.Bukkit.getOfflinePlayer(previousOwner);
                                            if (seller.isOnline()) {
                                                Player sellerPlayer = seller.getPlayer();
                                                if (sellerPlayer != null) {
                                                    sellerPlayer.sendMessage("§6§l=== LAND SOLD ===");
                                                    sellerPlayer.sendMessage("§aYour claim has been purchased!");
                                                    sellerPlayer.sendMessage("§7Location: Claim #" + claim.getID());
                                                    sellerPlayer.sendMessage("§7Buyer: §e" + player.getName());
                                                    sellerPlayer.sendMessage("§7You received: §e" +
                                                        plugin.getEconomyManager().formatAmount(price));
                                                    sellerPlayer.sendMessage("§6§l===================");
                                                    guiManager.playBuySuccessSound(sellerPlayer);
                                                }
                                            }
                                        }
                                    });
                                });
                            } else {
                                player.sendMessage("§cFailed to transfer ownership!");
                                guiManager.playErrorSound(player);
                                // Refund
                                plugin.getEconomyManager().depositPlayer(player.getUniqueId(), price);
                            }
                            guiManager.openLandGUI(player);
                        });
                    });
                });
        } else {
            // Free transfer
            plugin.getClaimManager().transferOwnership(claim, player.getUniqueId()).thenAccept(success -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (success) {
                        // FIX: Update claim data with new owner
                        claimData.setOwner(player.getUniqueId());
                        claimData.setSellPrice(0);
                        claimData.setStatus(dev.cosax.cSXLandManager.model.RentStatus.PRIVATE);
                        claimData.setRenter(null);
                        claimData.setOriginalOwner(null);
                        
                        // Save updated claim data
                        plugin.getStorageManager().saveClaimData(claimData).join();
                        
                        player.sendMessage("§aClaim transferred successfully!");
                        guiManager.playBuySuccessSound(player);
                    } else {
                        player.sendMessage("§cTransfer failed!");
                        guiManager.playErrorSound(player);
                    }
                    guiManager.openLandGUI(player);
                });
            });
        }
    }

    /**
     * Handles rent confirmation from ConfirmationGUI.
     */
    private void handleRentConfirmation(Player player, dev.cosax.cSXLandManager.model.ClaimData claimData, long rentDuration) {
        plugin.getLogger().info("Processing rent confirmation for " + player.getName());

        // Validate duration
        if (rentDuration <= 0) {
            player.sendMessage("§cInvalid rent duration!");
            guiManager.playErrorSound(player);
            guiManager.openLandGUI(player);
            return;
        }

        // Check if player is owner
        if (claimData.isOwner(player.getUniqueId())) {
            player.sendMessage("§cYou cannot rent your own claim!");
            guiManager.playErrorSound(player);
            guiManager.openLandGUI(player);
            return;
        }

        // Check if already renting
        if (claimData.isRenter(player.getUniqueId())) {
            player.sendMessage("§cYou are already renting this claim!");
            guiManager.playErrorSound(player);
            guiManager.openLandGUI(player);
            return;
        }

        // Check claim status
        if (claimData.getStatus() != dev.cosax.cSXLandManager.model.RentStatus.AVAILABLE && 
            claimData.getStatus() != dev.cosax.cSXLandManager.model.RentStatus.PRIVATE) {
            player.sendMessage("§cThis claim is not available for rent!");
            guiManager.playErrorSound(player);
            guiManager.openLandGUI(player);
            return;
        }

        // Start rent
        plugin.getRentManager().startRent(player, claimData, rentDuration).thenAccept(result -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (result.success()) {
                    player.sendMessage("§a" + result.message());
                    guiManager.playRentSuccessSound(player);
                } else {
                    player.sendMessage("§c" + result.message());
                    guiManager.playErrorSound(player);
                }
                guiManager.openLandGUI(player);
            });
        });
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
                    // Check if this is the 1-minute quick selection (durationMillis is set)
                    if (result.durationMillis() != null && result.durationMillis() > 0) {
                        // Direct duration selection (1-minute quick option)
                        plugin.getLogger().info("1-minute quick selection: " + result.durationMillis() + "ms");
                        
                        // Get claim
                        me.ryanhamshire.GriefPrevention.Claim claim = plugin.getClaimManager().getClaimAtLocation(player.getLocation());
                        if (claim == null) {
                            player.sendMessage("§cYou are not in a claim!");
                            plugin.getGUIManager().playErrorSound(player);
                            return;
                        }

                        // Get FRESH claim data and update duration
                        long finalDuration = result.durationMillis();
                        plugin.getStorageManager().loadClaimData(claim.getID()).thenCompose(dataOpt -> {
                            CompletableFuture<dev.cosax.cSXLandManager.model.ClaimData> dataFuture;
                            if (dataOpt.isPresent()) {
                                dataFuture = CompletableFuture.completedFuture(dataOpt.get());
                            } else {
                                dataFuture = plugin.getClaimManager().getOrCreateClaimData(claim);
                            }

                            return dataFuture.thenCompose(claimData -> {
                                claimData.setRentDuration(finalDuration);
                                return plugin.getStorageManager().saveClaimData(claimData).thenApply(v -> claimData);
                            });
                        }).thenAccept(claimData -> {
                            player.sendMessage("§aDuration set to " + plugin.getMessages().formatDuration(finalDuration) + "!");
                            player.sendMessage("§eYou can now rent this claim with the selected duration.");
                            plugin.getGUIManager().playSuccessSound(player);

                            // Refresh GUI to show updated duration
                            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                plugin.getGUIManager().openLandGUI(player, claim);
                            }, 10L);
                        }).exceptionally(e -> {
                            plugin.getLogger().log(Level.SEVERE, "Failed to set duration", e);
                            player.sendMessage("§cFailed to set duration!");
                            plugin.getGUIManager().playErrorSound(player);
                            return null;
                        });
                        return;
                    }

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

        // Handle Shop GUI
        if (title.contains("Land Shop")) {
            event.setCancelled(true);
            ShopGUI shopGUI = openShopGUIs.get(player.getUniqueId());
            if (shopGUI == null) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.closeInventory();
                });
                return;
            }

            int slot = event.getSlot();
            
            // Check if right-click (teleport)
            if (event.getClick().isRightClick() && !event.getClick().isShiftClick()) {
                shopGUI.handleRightClick(player, slot);
                return;
            }

            // Left-click opens confirmation
            shopGUI.handleClick(player, slot).thenAccept(result -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (result == null || result.action() == null) {
                        return;
                    }

                    switch (result.action()) {
                        case CLOSE -> player.closeInventory();
                        case PREVIOUS_PAGE, NEXT_PAGE -> 
                            shopGUI.openPage(player, result.page());
                        case CLAIM_CLICKED -> {
                            // Open confirmation GUI for the clicked claim
                            ConfirmationGUI.ActionType actionType = result.claimData().isForSale() ? 
                                ConfirmationGUI.ActionType.BUY : ConfirmationGUI.ActionType.RENT;
                            ConfirmationGUI confirmationGUI = new ConfirmationGUI(
                                plugin, result.claimData(), actionType, result.claimData().getRentDuration());
                            confirmationGUI.open(player);
                        }
                    }
                });
            }).exceptionally(e -> {
                plugin.getLogger().log(Level.SEVERE, "Error handling shop click", e);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.closeInventory();
                });
                return null;
            });
            return;
        }

        // Handle Confirmation GUI
        if (title.contains("Confirm")) {
            event.setCancelled(true);
            ConfirmationGUI confirmationGUI = openConfirmationGUIs.get(player.getUniqueId());
            if (confirmationGUI == null) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.closeInventory();
                });
                return;
            }

            int slot = event.getSlot();
            confirmationGUI.handleClick(player, slot).thenAccept(result -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (!result.confirmed()) {
                        player.sendMessage("§cTransaction cancelled.");
                        guiManager.playErrorSound(player);
                        // Reopen shop or claim GUI
                        guiManager.openLandGUI(player);
                        return;
                    }

                    // Process the confirmed action
                    if (result.actionType() == ConfirmationGUI.ActionType.BUY) {
                        handleBuyConfirmation(player, result.claimData());
                    } else if (result.actionType() == ConfirmationGUI.ActionType.RENT) {
                        handleRentConfirmation(player, result.claimData(), result.rentDuration());
                    }
                });
            }).exceptionally(e -> {
                plugin.getLogger().log(Level.SEVERE, "Error handling confirmation click", e);
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
            !title.equals(config.getGuiAdminTitle()) &&
            !title.contains("Land Shop") &&
            !title.contains("Confirm")) {
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
            title.equals(config.getGuiAdminTitle()) ||
            title.contains("Land Shop") ||
            title.contains("Confirm")) {
            unregisterOpenGUI(player);
        }
    }
}

