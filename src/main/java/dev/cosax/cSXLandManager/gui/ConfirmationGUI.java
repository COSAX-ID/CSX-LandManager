package dev.cosax.cSXLandManager.gui;

import dev.cosax.cSXLandManager.CSXLandManager;
import dev.cosax.cSXLandManager.config.Config;
import dev.cosax.cSXLandManager.config.Messages;
import dev.cosax.cSXLandManager.manager.ClaimManager;
import dev.cosax.cSXLandManager.manager.EconomyManager;
import dev.cosax.cSXLandManager.manager.GUIManager;
import dev.cosax.cSXLandManager.model.ClaimData;
import dev.cosax.cSXLandManager.model.RentStatus;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Confirmation GUI for purchasing or renting claims.
 * Shows detailed information before confirming the transaction.
 */
public class ConfirmationGUI {

    private final CSXLandManager plugin;
    private final Config config;
    private final Messages messages;
    private final ClaimManager claimManager;
    private final EconomyManager economyManager;
    private final GUIManager guiManager;

    // Layout constants
    private static final int CONFIRM_SLOT = 11;
    private static final int CANCEL_SLOT = 15;
    private static final int INFO_SLOT = 13;

    // Action type
    public enum ActionType {
        BUY,
        RENT
    }

    private final ClaimData claimData;
    private final ActionType actionType;
    private final long rentDuration; // For rent action

    public ConfirmationGUI(CSXLandManager plugin, ClaimData claimData, ActionType actionType, long rentDuration) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.messages = plugin.getMessages();
        this.claimManager = plugin.getClaimManager();
        this.economyManager = plugin.getEconomyManager();
        this.guiManager = plugin.getGUIManager();
        this.claimData = claimData;
        this.actionType = actionType;
        this.rentDuration = rentDuration;
    }

    /**
     * Opens the confirmation GUI.
     */
    public void open(Player player) {
        String title = org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                messages.getShopConfirmTitle());
        Inventory inv = Bukkit.createInventory(null, 27, title);

        // Add info item
        inv.setItem(INFO_SLOT, createInfoItem());

        // Add confirm button
        inv.setItem(CONFIRM_SLOT, createConfirmButton());

        // Add cancel button
        inv.setItem(CANCEL_SLOT, createCancelButton());

        // Add decoration
        addDecoration(inv);

        // Register GUI
        plugin.getGUIListener().registerOpenGUI(player, this);

        player.openInventory(inv);
        guiManager.playClickSound(player);
    }

    /**
     * Creates the info item showing claim details.
     */
    private ItemStack createInfoItem() {
        Claim claim = claimManager.getClaimById(claimData.getClaimId());
        
        Material material = actionType == ActionType.BUY ? Material.EMERALD : Material.GOLD_INGOT;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String actionText = actionType == ActionType.BUY ? "Purchase" : "Rent";
        meta.setDisplayName("§6§l" + actionText + " Confirmation");

        List<String> lore = new ArrayList<>();

        // Size info
        if (claim != null) {
            int area = claimManager.getClaimArea(claim);
            lore.add(messages.getShopConfirmLoreSize().replace("{size}", String.valueOf(area)));

            // Location info
            Location loc = claim.getLesserBoundaryCorner();
            lore.add(messages.getShopConfirmLoreLocation()
                    .replace("{world}", loc.getWorld().getName())
                    .replace("{x}", String.valueOf(loc.getBlockX()))
                    .replace("{z}", String.valueOf(loc.getBlockZ())));
        }

        // Price info
        double price = actionType == ActionType.BUY ? claimData.getSellPrice() : claimData.getRentPrice();
        lore.add(messages.getShopConfirmLorePrice()
                .replace("{price}", economyManager.formatAmount(price)));

        // Duration info (for rent)
        if (actionType == ActionType.RENT) {
            long duration = rentDuration > 0 ? rentDuration : claimData.getRentDuration();
            lore.add(messages.getShopConfirmLoreDuration()
                    .replace("{duration}", messages.formatDuration(duration)));

            // Total price
            lore.add(messages.getShopConfirmLoreTotal()
                    .replace("{total}", economyManager.formatAmount(price)));
        }

        // Owner info
        String ownerName = claimManager.getPlayerName(claimData.getOwner());
        lore.add("");
        lore.add("§7Owner: §f" + (ownerName != null ? ownerName : "Unknown"));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the confirm button.
     */
    private ItemStack createConfirmButton() {
        ItemStack item = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(messages.getShopConfirmConfirm());

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Click to confirm this transaction");
        lore.add("§7You will be charged: §f" + 
                economyManager.formatAmount(actionType == ActionType.BUY ? 
                        claimData.getSellPrice() : claimData.getRentPrice()));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the cancel button.
     */
    private ItemStack createCancelButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(messages.getShopConfirmCancel());

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Click to cancel");
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Adds decoration items.
     */
    private void addDecoration(Inventory inv) {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        glass.setItemMeta(meta);

        int[] protectedSlots = {CONFIRM_SLOT, CANCEL_SLOT, INFO_SLOT};

        for (int i = 0; i < inv.getSize(); i++) {
            boolean isProtected = false;
            for (int slot : protectedSlots) {
                if (i == slot) {
                    isProtected = true;
                    break;
                }
            }

            if (!isProtected && (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR)) {
                inv.setItem(i, glass);
            }
        }
    }

    /**
     * Handles a click in the confirmation GUI.
     */
    public CompletableFuture<ConfirmationResult> handleClick(Player player, int slot) {
        if (slot == CONFIRM_SLOT) {
            return CompletableFuture.completedFuture(new ConfirmationResult(true, claimData, actionType, rentDuration));
        }

        if (slot == CANCEL_SLOT) {
            return CompletableFuture.completedFuture(new ConfirmationResult(false, claimData, actionType, rentDuration));
        }

        // Any other slot - ignore
        guiManager.playClickSound(player);
        return CompletableFuture.completedFuture(new ConfirmationResult(false, claimData, actionType, rentDuration));
    }

    /**
     * Result of a confirmation click.
     */
    public record ConfirmationResult(
            boolean confirmed,
            ClaimData claimData,
            ActionType actionType,
            long rentDuration
    ) {}
}
