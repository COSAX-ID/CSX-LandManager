package dev.cosax.cSXLandManager.listener;

import dev.cosax.cSXLandManager.CSXLandManager;
import dev.cosax.cSXLandManager.storage.StorageManager;
import dev.cosax.cSXLandManager.gui.DurationSelectorGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChatEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Level;

/**
 * Handles player chat input for setting prices and durations.
 */
public class ChatListener implements Listener {

    private final CSXLandManager plugin;
    private final StorageManager storageManager;

    // Map of players waiting for price input (thread-safe)
    private final Map<UUID, PriceInputType> waitingForPrice = new HashMap<>();

    // Map of players waiting for duration input (thread-safe)
    private final Map<UUID, DurationInputType> waitingForDuration = new HashMap<>();

    // Simple duration input flag (mirip dengan price input)
    private final Map<UUID, Boolean> waitingForDurationSimple = new HashMap<>();

    public enum PriceInputType {
        SET_RENT_PRICE,
        SET_SELL_PRICE
    }

    public enum DurationInputType {
        HOURS(DurationSelectorGUI.TimeUnit.HOURS),
        DAYS(DurationSelectorGUI.TimeUnit.DAYS),
        WEEKS(DurationSelectorGUI.TimeUnit.WEEKS),
        MONTHS(DurationSelectorGUI.TimeUnit.MONTHS),
        CUSTOM(DurationSelectorGUI.TimeUnit.CUSTOM);

        private final DurationSelectorGUI.TimeUnit unit;

        DurationInputType(DurationSelectorGUI.TimeUnit unit) {
            this.unit = unit;
        }

        public DurationSelectorGUI.TimeUnit getUnit() {
            return unit;
        }
    }

    public ChatListener(CSXLandManager plugin, StorageManager storageManager) {
        this.plugin = plugin;
        this.storageManager = storageManager;
    }

    /**
     * Registers that a player is waiting for price input.
     */
    public synchronized void registerPriceInput(Player player, PriceInputType type) {
        UUID uuid = player.getUniqueId();
        waitingForPrice.put(uuid, type);
        plugin.getLogger().info("Registered price input for player: " + player.getName() + " (" + uuid + ") - Type: " + type);
    }

    /**
     * Registers that a player is waiting for duration input.
     */
    public synchronized void registerDurationInput(Player player, DurationSelectorGUI.TimeUnit type) {
        UUID uuid = player.getUniqueId();
        DurationInputType inputType = DurationInputType.valueOf(type.name());
        waitingForDuration.put(uuid, inputType);
        plugin.getLogger().info("Registered duration input for player: " + player.getName() + " (" + uuid + ") - Unit: " + type);
    }

    /**
     * Registers that a player is waiting for SIMPLE duration input (mirip dengan price).
     */
    public synchronized void registerDurationInputSimple(Player player) {
        UUID uuid = player.getUniqueId();
        waitingForDurationSimple.put(uuid, true);
        plugin.getLogger().info("Registered SIMPLE duration input for player: " + player.getName() + " (" + uuid + ")");
    }

    /**
     * Unregisters a player from price input waiting.
     */
    public synchronized void unregisterPriceInput(Player player) {
        waitingForPrice.remove(player.getUniqueId());
    }

    /**
     * Unregisters a player from duration input waiting.
     */
    public synchronized void unregisterDurationInput(Player player) {
        waitingForDuration.remove(player.getUniqueId());
    }

    /**
     * Unregisters a player from simple duration input waiting.
     */
    public synchronized void unregisterDurationInputSimple(Player player) {
        waitingForDurationSimple.remove(player.getUniqueId());
    }

    /**
     * Checks if a player is waiting for price input.
     */
    public synchronized boolean isWaitingForInput(Player player) {
        return waitingForPrice.containsKey(player.getUniqueId()) ||
               waitingForDuration.containsKey(player.getUniqueId()) ||
               waitingForDurationSimple.containsKey(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        synchronized (this) {
            // Check if player is waiting for price input
            if (waitingForPrice.containsKey(playerUuid)) {
                event.setCancelled(true);
                plugin.getLogger().info("Chat intercepted for price input: " + player.getName());
                handlePriceInput(player, event.getMessage());
                return;
            }

            // Check if player is waiting for SIMPLE duration input (priority baru)
            if (waitingForDurationSimple.containsKey(playerUuid)) {
                event.setCancelled(true);
                plugin.getLogger().info("Chat intercepted for SIMPLE duration input: " + player.getName() + " - Message: " + event.getMessage());
                handleDurationInputSimple(player, event.getMessage());
                return;
            }

            // Check if player is waiting for duration input (lama)
            if (waitingForDuration.containsKey(playerUuid)) {
                event.setCancelled(true);
                plugin.getLogger().info("Chat intercepted for duration input: " + player.getName() + " - Message: " + event.getMessage());
                handleDurationInput(player, event.getMessage());
                return;
            }
        }
    }

    /**
     * Handles price input.
     */
    private void handlePriceInput(Player player, String message) {
        UUID playerUuid = player.getUniqueId();
        PriceInputType inputType = waitingForPrice.get(playerUuid);
        String trimmed = message.trim();

        // Check for cancel
        if (trimmed.equalsIgnoreCase("cancel")) {
            waitingForPrice.remove(playerUuid);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage("§cPrice setting cancelled.");
                plugin.getGUIManager().playErrorSound(player);
                // Reopen GUI
                plugin.getGUIManager().openLandGUI(player);
            });
            return;
        }

        // Parse the price
        double price;
        try {
            price = Double.parseDouble(trimmed);
        } catch (NumberFormatException e) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage("§cInvalid price! Please enter a valid number.");
                player.sendMessage("§7Example: §f1000");
                plugin.getGUIManager().playErrorSound(player);
            });
            return;
        }

        // Validate price
        if (price < 0) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage("§cPrice cannot be negative!");
                plugin.getGUIManager().playErrorSound(player);
            });
            return;
        }

        // Get claim at player's location
        me.ryanhamshire.GriefPrevention.Claim claim = plugin.getClaimManager().getClaimAtLocation(player.getLocation());
        if (claim == null) {
            waitingForPrice.remove(playerUuid);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage("§cYou are not in a claim!");
                plugin.getGUIManager().playErrorSound(player);
                plugin.getGUIManager().openLandGUI(player);
            });
            return;
        }

        // Process the price setting
        waitingForPrice.remove(playerUuid);

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            processPriceSet(player, claim, price, inputType);
        });
    }

    /**
     * Handles duration input.
     */
    private void handleDurationInput(Player player, String message) {
        UUID playerUuid = player.getUniqueId();
        DurationInputType inputType = waitingForDuration.get(playerUuid);
        String trimmed = message.trim();

        // Check for cancel
        if (trimmed.equalsIgnoreCase("cancel")) {
            waitingForDuration.remove(playerUuid);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage("§cDuration selection cancelled.");
                plugin.getGUIManager().playErrorSound(player);
                // Reopen duration selector
                me.ryanhamshire.GriefPrevention.Claim claim = plugin.getClaimManager().getClaimAtLocation(player.getLocation());
                if (claim != null) {
                    plugin.getClaimManager().getOrCreateClaimData(claim).thenAccept(claimData -> {
                        new DurationSelectorGUI(plugin, claim, claimData).open(player);
                    });
                }
            });
            return;
        }

        long durationMillis;

        if (inputType == DurationInputType.CUSTOM) {
            // Parse custom duration (e.g., "3 days", "2 weeks", "12 hours")
            durationMillis = parseCustomDuration(trimmed);
            if (durationMillis <= 0) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§cInvalid duration format!");
                    player.sendMessage("§7Examples: §f3 days, 2 weeks, 12 hours");
                    plugin.getGUIManager().playErrorSound(player);
                });
                return;
            }
        } else {
            // Parse simple number
            try {
                int amount = Integer.parseInt(trimmed);
                if (amount <= 0) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        player.sendMessage("§cAmount must be greater than 0!");
                        plugin.getGUIManager().playErrorSound(player);
                    });
                    return;
                }
                durationMillis = inputType.getUnit().getMilliseconds(amount);
            } catch (NumberFormatException e) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§cInvalid number! Please enter a valid amount.");
                    player.sendMessage("§7Example: §f7");
                    plugin.getGUIManager().playErrorSound(player);
                });
                return;
            }
        }

        waitingForDuration.remove(playerUuid);

        // Store the selected duration
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            final long finalDuration = durationMillis;

            // Get claim
            me.ryanhamshire.GriefPrevention.Claim claim = plugin.getClaimManager().getClaimAtLocation(player.getLocation());
            if (claim == null) {
                player.sendMessage("§cYou are not in a claim!");
                plugin.getGUIManager().playErrorSound(player);
                return;
            }

            // Get FRESH claim data and update duration using proper async chaining
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
        });
    }

    /**
     * Parses custom duration string (e.g., "3 days", "2 weeks").
     */
    private long parseCustomDuration(String input) {
        // Pattern: number + space + unit
        Pattern pattern = Pattern.compile("(\\d+)\\s+(hour|hours|day|days|week|weeks|month|months)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(input);

        if (!matcher.matches()) {
            return -1;
        }

        int amount = Integer.parseInt(matcher.group(1));
        String unit = matcher.group(2).toLowerCase();

        return switch (unit) {
            case "hour", "hours" -> amount * DurationSelectorGUI.TimeUnit.HOURS.getMilliseconds();
            case "day", "days" -> amount * DurationSelectorGUI.TimeUnit.DAYS.getMilliseconds();
            case "week", "weeks" -> amount * DurationSelectorGUI.TimeUnit.WEEKS.getMilliseconds();
            case "month", "months" -> amount * DurationSelectorGUI.TimeUnit.MONTHS.getMilliseconds();
            default -> -1;
        };
    }

    /**
     * Processes setting the price.
     */
    private void processPriceSet(Player player, me.ryanhamshire.GriefPrevention.Claim claim,
                                   double price, PriceInputType inputType) {
        final me.ryanhamshire.GriefPrevention.Claim finalClaim = claim;

        // Load FRESH data from storage using proper async chaining
        plugin.getStorageManager().loadClaimData(claim.getID()).thenCompose(dataOpt -> {
            CompletableFuture<dev.cosax.cSXLandManager.model.ClaimData> dataFuture;
            if (dataOpt.isPresent()) {
                dataFuture = CompletableFuture.completedFuture(dataOpt.get());
            } else {
                dataFuture = plugin.getClaimManager().getOrCreateClaimData(claim);
            }

            return dataFuture.thenCompose(claimData -> {
                if (inputType == PriceInputType.SET_RENT_PRICE) {
                    // Set rent price
                    claimData.setRentPrice(price);
                    if (price > 0 && claimData.getStatus() == dev.cosax.cSXLandManager.model.RentStatus.PRIVATE) {
                        claimData.setStatus(dev.cosax.cSXLandManager.model.RentStatus.AVAILABLE);
                    } else if (price == 0) {
                        if (claimData.getStatus() == dev.cosax.cSXLandManager.model.RentStatus.AVAILABLE) {
                            claimData.setStatus(dev.cosax.cSXLandManager.model.RentStatus.PRIVATE);
                        }
                    }
                } else if (inputType == PriceInputType.SET_SELL_PRICE) {
                    // Set sell price
                    claimData.setSellPrice(price);
                    if (price > 0) {
                        claimData.setStatus(dev.cosax.cSXLandManager.model.RentStatus.FOR_SALE);
                    } else {
                        if (claimData.getStatus() == dev.cosax.cSXLandManager.model.RentStatus.FOR_SALE) {
                            claimData.setStatus(dev.cosax.cSXLandManager.model.RentStatus.PRIVATE);
                        }
                    }
                }

                // Save to storage
                return plugin.getStorageManager().saveClaimData(claimData).thenApply(v -> claimData);
            });
        }).thenAccept(claimData -> {
            if (inputType == PriceInputType.SET_RENT_PRICE) {
                player.sendMessage("§aRent price set to §f" + plugin.getEconomyManager().formatAmount(price) + "§a!");
                if (price > 0) {
                    player.sendMessage("§eClaim is now available for rent.");
                }
                plugin.getGUIManager().playSuccessSound(player);
            } else if (inputType == PriceInputType.SET_SELL_PRICE) {
                if (price > 0) {
                    player.sendMessage("§aClaim listed for sale at §f" + plugin.getEconomyManager().formatAmount(price) + "§a!");
                } else {
                    player.sendMessage("§aClaim removed from sale.");
                }
                plugin.getGUIManager().playSuccessSound(player);
            }

            // Reopen GUI after setting price with fresh data
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getGUIManager().openLandGUI(player, finalClaim);
            }, 10L);
        }).exceptionally(e -> {
            plugin.getLogger().log(Level.SEVERE, "Failed to set price", e);
            player.sendMessage("§cFailed to set price!");
            plugin.getGUIManager().playErrorSound(player);
            return null;
        });
    }

    /**
     * Clean up when player quits.
     */
    public synchronized void cleanup(Player player) {
        UUID uuid = player.getUniqueId();
        waitingForPrice.remove(uuid);
        waitingForDuration.remove(uuid);
        waitingForDurationSimple.remove(uuid);
    }

    /**
     * Handles SIMPLE duration input (mirip dengan price setting).
     * Format: "7d", "2w", "1m", "24h" atau "7 days", "2 weeks", dll
     */
    private void handleDurationInputSimple(Player player, String message) {
        UUID playerUuid = player.getUniqueId();
        String trimmed = message.trim();

        // Check for cancel
        if (trimmed.equalsIgnoreCase("cancel")) {
            waitingForDurationSimple.remove(playerUuid);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage("§cDuration setting cancelled.");
                plugin.getGUIManager().playErrorSound(player);
                plugin.getGUIManager().openLandGUI(player);
            });
            return;
        }

        // Parse duration
        long durationMillis = parseSimpleDuration(trimmed);
        if (durationMillis <= 0) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage("§cInvalid duration format!");
                player.sendMessage("§7Examples: §f7d, 2w, 1m, 24h");
                player.sendMessage("§7  §f7 days, 2 weeks, 1 month");
                player.sendMessage("§7Units: §fh(hours) d(days) w(weeks) m(months)");
                plugin.getGUIManager().playErrorSound(player);
            });
            return;
        }

        waitingForDurationSimple.remove(playerUuid);

        // Process the duration setting
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            // Get claim
            me.ryanhamshire.GriefPrevention.Claim claim = plugin.getClaimManager().getClaimAtLocation(player.getLocation());
            if (claim == null) {
                player.sendMessage("§cYou are not in a claim!");
                plugin.getGUIManager().playErrorSound(player);
                return;
            }

            final long finalDuration = durationMillis;

            // Get FRESH claim data and update duration
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
        });
    }

    /**
     * Parses simple duration string.
     * Supports: "7d", "2w", "1m", "24h" or "7 days", "2 weeks", etc.
     */
    private long parseSimpleDuration(String input) {
        if (input == null || input.isEmpty()) {
            return -1;
        }

        // Try format: "7d", "2w", "1m", "24h" (compact format)
        Pattern compactPattern = Pattern.compile("(\\d+)\\s*([hdwHDWM])", Pattern.CASE_INSENSITIVE);
        Matcher compactMatcher = compactPattern.matcher(input);

        if (compactMatcher.matches()) {
            int amount = Integer.parseInt(compactMatcher.group(1));
            String unit = compactMatcher.group(2).toLowerCase();

            return switch (unit) {
                case "h" -> amount * DurationSelectorGUI.TimeUnit.HOURS.getMilliseconds();
                case "d" -> amount * DurationSelectorGUI.TimeUnit.DAYS.getMilliseconds();
                case "w" -> amount * DurationSelectorGUI.TimeUnit.WEEKS.getMilliseconds();
                case "m" -> amount * DurationSelectorGUI.TimeUnit.MONTHS.getMilliseconds();
                default -> -1;
            };
        }

        // Try format: "7 days", "2 weeks", etc (full format)
        Pattern fullPattern = Pattern.compile("(\\d+)\\s+(hour|hours|day|days|week|weeks|month|months)", Pattern.CASE_INSENSITIVE);
        Matcher fullMatcher = fullPattern.matcher(input);

        if (fullMatcher.matches()) {
            int amount = Integer.parseInt(fullMatcher.group(1));
            String unit = fullMatcher.group(2).toLowerCase();

            return switch (unit) {
                case "hour", "hours" -> amount * DurationSelectorGUI.TimeUnit.HOURS.getMilliseconds();
                case "day", "days" -> amount * DurationSelectorGUI.TimeUnit.DAYS.getMilliseconds();
                case "week", "weeks" -> amount * DurationSelectorGUI.TimeUnit.WEEKS.getMilliseconds();
                case "month", "months" -> amount * DurationSelectorGUI.TimeUnit.MONTHS.getMilliseconds();
                default -> -1;
            };
        }

        // Invalid format
        return -1;
    }
}
