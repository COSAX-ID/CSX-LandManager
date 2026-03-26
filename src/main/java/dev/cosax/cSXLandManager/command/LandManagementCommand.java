package dev.cosax.cSXLandManager.command;

import dev.cosax.cSXLandManager.CSXLandManager;
import dev.cosax.cSXLandManager.config.Config;
import dev.cosax.cSXLandManager.config.Messages;
import dev.cosax.cSXLandManager.manager.ClaimManager;
import dev.cosax.cSXLandManager.manager.GUIManager;
import dev.cosax.cSXLandManager.manager.RentManager;
import dev.cosax.cSXLandManager.model.ClaimData;
import dev.cosax.cSXLandManager.model.RentStatus;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Command handler for /landmanagement.
 */
public class LandManagementCommand implements CommandExecutor, TabCompleter {

    private final CSXLandManager plugin;
    private final Config config;
    private final Messages messages;
    private final ClaimManager claimManager;
    private final RentManager rentManager;
    private final GUIManager guiManager;

    public LandManagementCommand(CSXLandManager plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.messages = plugin.getMessages();
        this.claimManager = plugin.getClaimManager();
        this.rentManager = plugin.getRentManager();
        this.guiManager = plugin.getGUIManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Only players can use this command
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.getOnlyPlayers());
            return true;
        }

        // Check base permission
        if (!player.hasPermission("landmgmt.use")) {
            player.sendMessage(messages.getNoPermission());
            return true;
        }

        // No arguments - open GUI
        if (args.length == 0) {
            guiManager.openLandGUI(player);
            return true;
        }

        // Handle subcommands
        String subCommand = args[0].toLowerCase();

        return switch (subCommand) {
            case "setprice" -> handleSetPrice(player, args);
            case "sell", "setsellprice" -> handleSetSellPrice(player, args);
            case "unsell", "removeforsale" -> handleUnsell(player);
            case "cancelrent" -> handleCancelRent(player);
            case "shop" -> handleShop(player);
            case "reload" -> handleReload(player);
            default -> {
                player.sendMessage("§cUnknown subcommand. Use: /" + label + " [setprice|sell|unsell|cancelrent|shop]");
                guiManager.playErrorSound(player);
                yield true;
            }
        };
    }

    /**
     * Handles the setprice subcommand.
     * Usage: /landmanagement setprice <price>
     */
    private boolean handleSetPrice(Player player, String[] args) {
        if (!player.hasPermission("landmgmt.sell")) {
            player.sendMessage(messages.getNoPermission());
            guiManager.playErrorSound(player);
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /landmanagement setprice <price>");
            guiManager.playErrorSound(player);
            return true;
        }

        // Parse price
        double price;
        try {
            price = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(messages.getInvalidNumber(args[1]));
            guiManager.playErrorSound(player);
            return true;
        }

        // Validate price
        if (price < config.getMinRentPrice() || (config.getMaxRentPrice() > 0 && price > config.getMaxRentPrice())) {
            player.sendMessage("§cPrice must be between " + config.getMinRentPrice() + " and " +
                    (config.getMaxRentPrice() > 0 ? config.getMaxRentPrice() : "unlimited"));
            guiManager.playErrorSound(player);
            return true;
        }

        // Get claim at player's location
        Claim claim = claimManager.getClaimAtLocation(player.getLocation());
        if (claim == null) {
            player.sendMessage(messages.getNotInClaim());
            guiManager.playErrorSound(player);
            return true;
        }

        // Check if player is the owner
        if (!claimManager.isClaimOwner(player, claim) && !player.hasPermission("landmgmt.admin")) {
            player.sendMessage(messages.getNotYourClaim());
            guiManager.playErrorSound(player);
            return true;
        }

        // Update claim data asynchronously
        claimManager.getOrCreateClaimData(claim).thenAccept(claimData -> {
            claimData.setRentPrice(price);
            claimData.setStatus(RentStatus.AVAILABLE);
            plugin.getStorageManager().saveClaimData(claimData).thenRun(() -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage(messages.getPriceSet(price));
                    player.sendMessage("§eClaim is now available for rent at " + messages.formatPrice(price) + " per " +
                            messages.formatDuration(config.getDefaultRentDuration()));
                    guiManager.playSuccessSound(player);
                });
            }).exceptionally(e -> {
                plugin.getLogger().log(Level.SEVERE, "Failed to set rent price", e);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage(messages.getErrorOccurred());
                    guiManager.playErrorSound(player);
                });
                return null;
            });
        });

        return true;
    }

    /**
     * Handles the sell subcommand.
     * Usage: /landmanagement sell <price>
     */
    private boolean handleSetSellPrice(Player player, String[] args) {
        if (!player.hasPermission("landmgmt.sell")) {
            player.sendMessage(messages.getNoPermission());
            guiManager.playErrorSound(player);
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /landmanagement sell <price>");
            guiManager.playErrorSound(player);
            return true;
        }

        // Parse price
        double price;
        try {
            price = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(messages.getInvalidNumber(args[1]));
            guiManager.playErrorSound(player);
            return true;
        }

        // Validate price
        if (price < config.getMinSellPrice() || (config.getMaxSellPrice() > 0 && price > config.getMaxSellPrice())) {
            player.sendMessage("§cPrice must be between " + config.getMinSellPrice() + " and " +
                    (config.getMaxSellPrice() > 0 ? config.getMaxSellPrice() : "unlimited"));
            guiManager.playErrorSound(player);
            return true;
        }

        // Get claim at player's location
        Claim claim = claimManager.getClaimAtLocation(player.getLocation());
        if (claim == null) {
            player.sendMessage(messages.getNotInClaim());
            guiManager.playErrorSound(player);
            return true;
        }

        // Check if player is the owner
        if (!claimManager.isClaimOwner(player, claim) && !player.hasPermission("landmgmt.admin")) {
            player.sendMessage(messages.getNotYourClaim());
            guiManager.playErrorSound(player);
            return true;
        }

        // Update claim data asynchronously
        claimManager.getOrCreateClaimData(claim).thenAccept(claimData -> {
            claimData.setSellPrice(price);
            claimData.setStatus(RentStatus.FOR_SALE);
            plugin.getStorageManager().saveClaimData(claimData).thenRun(() -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage(messages.getClaimListed(price));
                    guiManager.playSuccessSound(player);
                });
            }).exceptionally(e -> {
                plugin.getLogger().log(Level.SEVERE, "Failed to set sell price", e);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage(messages.getErrorOccurred());
                    guiManager.playErrorSound(player);
                });
                return null;
            });
        });

        return true;
    }

    /**
     * Handles the unsell subcommand.
     * Usage: /landmanagement unsell
     */
    private boolean handleUnsell(Player player) {
        if (!player.hasPermission("landmgmt.sell")) {
            player.sendMessage(messages.getNoPermission());
            guiManager.playErrorSound(player);
            return true;
        }

        // Get claim at player's location
        Claim claim = claimManager.getClaimAtLocation(player.getLocation());
        if (claim == null) {
            player.sendMessage(messages.getNotInClaim());
            guiManager.playErrorSound(player);
            return true;
        }

        // Check if player is the owner
        if (!claimManager.isClaimOwner(player, claim) && !player.hasPermission("landmgmt.admin")) {
            player.sendMessage(messages.getNotYourClaim());
            guiManager.playErrorSound(player);
            return true;
        }

        // Update claim data asynchronously
        claimManager.getOrCreateClaimData(claim).thenAccept(claimData -> {
            claimData.setSellPrice(0);
            claimData.setStatus(RentStatus.PRIVATE);
            plugin.getStorageManager().saveClaimData(claimData).thenRun(() -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage(messages.getClaimRemovedSale());
                    guiManager.playSuccessSound(player);
                });
            }).exceptionally(e -> {
                plugin.getLogger().log(Level.SEVERE, "Failed to remove from sale", e);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage(messages.getErrorOccurred());
                    guiManager.playErrorSound(player);
                });
                return null;
            });
        });

        return true;
    }

    /**
     * Handles the cancelrent subcommand.
     * Usage: /landmanagement cancelrent
     */
    private boolean handleCancelRent(Player player) {
        if (!player.hasPermission("landmgmt.admin")) {
            player.sendMessage(messages.getAdminOnly());
            guiManager.playErrorSound(player);
            return true;
        }

        // Get claim at player's location
        Claim claim = claimManager.getClaimAtLocation(player.getLocation());
        if (claim == null) {
            player.sendMessage(messages.getNotInClaim());
            guiManager.playErrorSound(player);
            return true;
        }

        // Get claim data and cancel rent
        claimManager.getOrCreateClaimData(claim).thenAccept(claimData -> {
            if (!claimData.isRented()) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage(messages.getNoActiveRent());
                    guiManager.playErrorSound(player);
                });
                return;
            }

            rentManager.endRent(player, claimData, true).thenAccept(result -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (result.success()) {
                        player.sendMessage(result.message());
                        guiManager.playSuccessSound(player);
                    } else {
                        player.sendMessage(result.message());
                        guiManager.playErrorSound(player);
                    }
                });
            });
        });

        return true;
    }

    /**
     * Handles the shop subcommand.
     * Usage: /landmanagement shop
     */
    private boolean handleShop(Player player) {
        if (!player.hasPermission("landmgmt.shop")) {
            player.sendMessage(messages.getNoPermission());
            guiManager.playErrorSound(player);
            return true;
        }

        // Check if shop is enabled
        if (!config.isShopEnabled()) {
            player.sendMessage("§cShop feature is disabled.");
            guiManager.playErrorSound(player);
            return true;
        }

        // Open shop GUI
        new dev.cosax.cSXLandManager.gui.ShopGUI(plugin).open(player);
        return true;
    }

    /**
     * Handles the reload subcommand.
     */
    private boolean handleReload(Player player) {
        if (!player.hasPermission("landmgmt.admin")) {
            player.sendMessage(messages.getAdminOnly());
            guiManager.playErrorSound(player);
            return true;
        }

        plugin.reloadConfig();
        config.reload();
        messages.reload();

        player.sendMessage(messages.getPluginReloaded());
        guiManager.playSuccessSound(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("landmgmt.sell")) {
                completions.add("setprice");
                completions.add("sell");
                completions.add("unsell");
            }
            if (sender.hasPermission("landmgmt.shop")) {
                completions.add("shop");
            }
            if (sender.hasPermission("landmgmt.admin")) {
                completions.add("cancelrent");
                completions.add("reload");
            }
        }

        // Filter completions based on input
        if (args.length == 1 && !args[0].isEmpty()) {
            String input = args[0].toLowerCase();
            completions.removeIf(s -> !s.toLowerCase().startsWith(input));
        }

        return completions;
    }
}
