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
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * GUI for browsing available claims for rent or sale.
 * Accessed via /lm shop command.
 */
public class ShopGUI {

    private final CSXLandManager plugin;
    private final Config config;
    private final Messages messages;
    private final ClaimManager claimManager;
    private final EconomyManager economyManager;
    private final GUIManager guiManager;

    // Layout constants
    private static final int PREVIOUS_PAGE_SLOT = 45;
    private static final int NEXT_PAGE_SLOT = 53;
    private static final int CLOSE_SLOT = 49;

    // Pagination
    private int currentPage = 0;
    private final int itemsPerPage = 45;
    private List<ClaimData> availableClaims = new ArrayList<>();

    public ShopGUI(CSXLandManager plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.messages = plugin.getMessages();
        this.claimManager = plugin.getClaimManager();
        this.economyManager = plugin.getEconomyManager();
        this.guiManager = plugin.getGUIManager();
    }

    /**
     * Opens the shop GUI for a player.
     */
    public void open(Player player) {
        // Check permission
        if (!player.hasPermission("landmgmt.shop")) {
            player.sendMessage(messages.getNoPermission());
            guiManager.playErrorSound(player);
            return;
        }

        // Load available claims asynchronously
        player.sendMessage("§eLoading shop...");

        CompletableFuture.supplyAsync(() -> {
            List<ClaimData> allClaims = new ArrayList<>();
            try {
                // Load all claim data from storage
                List<ClaimData> loadedClaims = plugin.getStorageManager().getAllClaims().join();
                for (ClaimData data : loadedClaims) {
                    // Include claims that are for sale OR available for rent
                    if (data.isForSale() || data.getStatus() == RentStatus.AVAILABLE) {
                        allClaims.add(data);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load claims for shop", e);
            }
            return allClaims;
        }).thenAccept(claims -> {
            availableClaims = claims;
            currentPage = 0;

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (availableClaims.isEmpty()) {
                    player.sendMessage(messages.getShopNoListings());
                    guiManager.playErrorSound(player);
                    return;
                }
                openPage(player, currentPage);
            });
        });
    }

    /**
     * Opens a specific page of the shop GUI.
     */
    public void openPage(Player player, int page) {
        if (availableClaims.isEmpty()) {
            player.sendMessage(messages.getShopNoListings());
            guiManager.playErrorSound(player);
            return;
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) availableClaims.size() / itemsPerPage));
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        currentPage = page;

        // Create inventory
        String title = org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                messages.getShopTitle() + " §7(Page " + (page + 1) + "/" + totalPages + ")");
        Inventory inv = Bukkit.createInventory(null, config.getShopGuiSize(), title);

        // Add items for current page
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, availableClaims.size());

        for (int i = startIndex; i < endIndex; i++) {
            ClaimData claimData = availableClaims.get(i);
            ItemStack item = createClaimItem(claimData);
            inv.addItem(item);
        }

        // Add navigation buttons
        inv.setItem(PREVIOUS_PAGE_SLOT, createNavigationButton(Material.ARROW, "§cPrevious Page", page > 0));
        inv.setItem(NEXT_PAGE_SLOT, createNavigationButton(Material.ARROW, "§aNext Page", page < totalPages - 1));
        inv.setItem(CLOSE_SLOT, createCloseButton());

        // Add decoration
        addDecoration(inv, startIndex, endIndex);

        // Register GUI
        plugin.getGUIListener().registerOpenGUI(player, this);

        player.openInventory(inv);
        guiManager.playClickSound(player);
    }

    /**
     * Creates an item representing a claim in the shop.
     */
    private ItemStack createClaimItem(ClaimData claimData) {
        Claim claim = claimManager.getClaimById(claimData.getClaimId());
        
        Material material;
        String displayName;
        
        if (claimData.isForSale()) {
            material = Material.EMERALD;
            displayName = "§6§lClaim For Sale §7(#" + claimData.getClaimId() + ")";
        } else {
            material = Material.GOLD_INGOT;
            displayName = "§a§lClaim For Rent §7(#" + claimData.getClaimId() + ")";
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);

        List<String> lore = new ArrayList<>();
        
        // Owner info
        String ownerName = claimManager.getPlayerName(claimData.getOwner());
        lore.add(messages.getShopClaimItemLoreOwner().replace("{owner}", ownerName != null ? ownerName : "Unknown"));
        
        // Location info
        if (claim != null) {
            Location loc = claim.getLesserBoundaryCorner();
            lore.add(messages.getShopClaimItemLoreLocation()
                    .replace("{world}", loc.getWorld().getName())
                    .replace("{x}", String.valueOf(loc.getBlockX()))
                    .replace("{z}", String.valueOf(loc.getBlockZ())));
            
            // Size info
            int area = claimManager.getClaimArea(claim);
            lore.add(messages.getShopClaimItemLoreSize().replace("{size}", String.valueOf(area)));
        }
        
        // Price info
        if (claimData.isForSale()) {
            lore.add(messages.getShopClaimItemLorePrice()
                    .replace("{price}", economyManager.formatAmount(claimData.getSellPrice())));
        }
        
        if (claimData.getStatus() == RentStatus.AVAILABLE && claimData.getRentPrice() > 0) {
            String duration = messages.formatDuration(claimData.getRentDuration());
            lore.add(messages.getShopClaimItemLoreRent()
                    .replace("{rentPrice}", economyManager.formatAmount(claimData.getRentPrice()))
                    .replace("{duration}", duration));
        }
        
        lore.add("");
        lore.add(messages.getShopClaimItemLoreLeft());
        
        String action = claimData.isForSale() ? "buy" : "rent";
        lore.add(messages.getShopClaimItemLoreBuy().replace("{action}", action));
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates a navigation button.
     */
    private ItemStack createNavigationButton(Material material, String name, boolean enabled) {
        ItemStack item = new ItemStack(enabled ? material : Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the close button.
     */
    private ItemStack createCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§cClose");
        List<String> lore = new ArrayList<>();
        lore.add("§7Close this menu");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Adds decoration items.
     */
    private void addDecoration(Inventory inv, int startIndex, int endIndex) {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        glass.setItemMeta(meta);

        // Protect navigation slots
        int[] protectedSlots = {PREVIOUS_PAGE_SLOT, NEXT_PAGE_SLOT, CLOSE_SLOT};

        for (int i = 0; i < inv.getSize(); i++) {
            boolean isProtected = false;
            for (int slot : protectedSlots) {
                if (i == slot) {
                    isProtected = true;
                    break;
                }
            }

            // Also protect item slots
            if (i >= 0 && i < endIndex) {
                isProtected = true;
            }

            if (!isProtected && (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR)) {
                inv.setItem(i, glass);
            }
        }
    }

    /**
     * Handles a click in the shop GUI.
     */
    public CompletableFuture<ShopClickResult> handleClick(Player player, int slot) {
        if (slot == CLOSE_SLOT) {
            return CompletableFuture.completedFuture(new ShopClickResult(ShopAction.CLOSE, null, 0));
        }

        if (slot == PREVIOUS_PAGE_SLOT) {
            if (currentPage > 0) {
                return CompletableFuture.completedFuture(new ShopClickResult(ShopAction.PREVIOUS_PAGE, null, currentPage - 1));
            }
            guiManager.playErrorSound(player);
            return CompletableFuture.completedFuture(new ShopClickResult(null, null, currentPage));
        }

        if (slot == NEXT_PAGE_SLOT) {
            int totalPages = Math.max(1, (int) Math.ceil((double) availableClaims.size() / itemsPerPage));
            if (currentPage < totalPages - 1) {
                return CompletableFuture.completedFuture(new ShopClickResult(ShopAction.NEXT_PAGE, null, currentPage + 1));
            }
            guiManager.playErrorSound(player);
            return CompletableFuture.completedFuture(new ShopClickResult(null, null, currentPage));
        }

        // Check if clicked on a claim item
        if (slot >= 0 && slot < itemsPerPage) {
            int index = (currentPage * itemsPerPage) + slot;
            if (index < availableClaims.size()) {
                ClaimData claimData = availableClaims.get(index);
                return CompletableFuture.completedFuture(new ShopClickResult(ShopAction.CLAIM_CLICKED, claimData, currentPage));
            }
        }

        // Any other slot - ignore
        guiManager.playClickSound(player);
        return CompletableFuture.completedFuture(new ShopClickResult(null, null, currentPage));
    }

    /**
     * Handles right-click on a claim item (teleport).
     */
    public void handleRightClick(Player player, int slot) {
        if (slot >= 0 && slot < itemsPerPage) {
            int index = (currentPage * itemsPerPage) + slot;
            if (index < availableClaims.size()) {
                ClaimData claimData = availableClaims.get(index);
                teleportToClaim(player, claimData);
            }
        }
    }

    /**
     * Teleports player to claim location.
     */
    public void teleportToClaim(Player player, ClaimData claimData) {
        Claim claim = claimManager.getClaimById(claimData.getClaimId());
        if (claim == null) {
            player.sendMessage(messages.getClaimNotFound());
            guiManager.playErrorSound(player);
            return;
        }

        Location loc = claim.getLesserBoundaryCorner().clone().add(0.5, 0, 0.5);
        
        // Find safe location
        while (!loc.getBlock().getType().isAir() && loc.getY() < 255) {
            loc.add(0, 1, 0);
        }
        
        player.teleport(loc);
        player.sendMessage(messages.getShopTeleport());
        guiManager.playSuccessSound(player);
    }

    /**
     * Shop action enum.
     */
    public enum ShopAction {
        CLOSE,
        PREVIOUS_PAGE,
        NEXT_PAGE,
        CLAIM_CLICKED
    }

    /**
     * Result of a shop click.
     */
    public record ShopClickResult(
            ShopAction action,
            ClaimData claimData,
            int page
    ) {}
}
