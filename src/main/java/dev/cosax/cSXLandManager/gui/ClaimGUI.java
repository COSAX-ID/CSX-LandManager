package dev.cosax.cSXLandManager.gui;

import dev.cosax.cSXLandManager.CSXLandManager;
import dev.cosax.cSXLandManager.config.Config;
import dev.cosax.cSXLandManager.config.Messages;
import dev.cosax.cSXLandManager.manager.ClaimManager;
import dev.cosax.cSXLandManager.manager.EconomyManager;
import dev.cosax.cSXLandManager.manager.GUIManager;
import dev.cosax.cSXLandManager.manager.RentManager;
import dev.cosax.cSXLandManager.model.ClaimData;
import dev.cosax.cSXLandManager.model.RentStatus;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * GUI for managing a single claim.
 * Displays claim information and allows players to rent, buy, extend, etc.
 */
public class ClaimGUI {

    private final CSXLandManager plugin;
    private final Claim claim;
    private final ClaimData claimData;
    private final Config config;
    private final Messages messages;
    private final ClaimManager claimManager;
    private final EconomyManager economyManager;
    private final RentManager rentManager;
    private final GUIManager guiManager;

    // GUI layout slots
    private static final int[] INFO_SLOTS = {4, 13, 22, 31};
    private static final int STATUS_SLOT = 49;

    // Player action buttons (non-owner)
    private static final int RENT_BUTTON_SLOT = 19;
    private static final int SET_DURATION_BUTTON_SLOT = 21;
    private static final int BUY_BUTTON_SLOT = 28;
    private static final int EXTEND_BUTTON_SLOT = 25;
    private static final int AUTO_RENEW_BUTTON_SLOT = 34;

    // Owner management buttons
    private static final int SET_RENT_PRICE_SLOT = 10;
    private static final int SET_SELL_PRICE_SLOT = 11;
    private static final int REMOVE_SALE_SLOT = 12;
    private static final int CANCEL_RENT_SLOT = 14;
    private static final int TOGGLE_RENT_SLOT = 16;
    private static final int TOGGLE_SALE_SLOT = 17;

    private static final int CLOSE_BUTTON_SLOT = 53;

    public ClaimGUI(CSXLandManager plugin, Claim claim, ClaimData claimData) {
        this.plugin = plugin;
        this.claim = claim;
        this.claimData = claimData;
        this.config = plugin.getConfigManager();
        this.messages = plugin.getMessages();
        this.claimManager = plugin.getClaimManager();
        this.economyManager = plugin.getEconomyManager();
        this.rentManager = plugin.getRentManager();
        this.guiManager = plugin.getGUIManager();
    }

    /**
     * Opens the GUI for a player.
     */
    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, config.getGuiSize(), config.getGuiMainTitle());

        // Add claim info items
        addClaimInfo(inv);

        // Add status indicator
        addStatusIndicator(inv);

        // Add action buttons based on player and claim state
        addActionButtons(inv, player);

        // Add decoration items
        addDecoration(inv);

        // Register GUI with listener
        plugin.getGUIListener().registerOpenGUI(player, this);

        player.openInventory(inv);
        guiManager.playClickSound(player);
    }

    /**
     * Adds claim information items to the GUI.
     */
    private void addClaimInfo(Inventory inv) {
        // Owner info (player head)
        ItemStack ownerItem = createOwnerItem();
        inv.setItem(INFO_SLOTS[0], ownerItem);

        // Claim size
        ItemStack sizeItem = createSizeItem();
        inv.setItem(INFO_SLOTS[1], sizeItem);

        // Rent price (if available)
        if (claimData.getRentPrice() > 0 || claimData.getStatus() == RentStatus.AVAILABLE) {
            ItemStack rentPriceItem = createRentPriceItem();
            inv.setItem(INFO_SLOTS[2], rentPriceItem);
        }

        // Sell price (if for sale)
        if (claimData.isForSale()) {
            ItemStack sellPriceItem = createSellPriceItem();
            inv.setItem(INFO_SLOTS[3], sellPriceItem);
        }
    }

    /**
     * Creates the owner information item.
     */
    private ItemStack createOwnerItem() {
        UUID ownerId = claim.getOwnerID();
        Material material = Material.PLAYER_HEAD;

        ItemStack item = new ItemStack(material);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        if (ownerId != null) {
            OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerId);
            meta.setOwningPlayer(owner);
            meta.setDisplayName(messages.getGuiOwner(owner.getName() != null ? owner.getName() : "Unknown"));
        } else {
            item.setType(Material.BARRIER);
            meta.setDisplayName(messages.getGuiOwner("Admin/ Wilderness"));
        }

        List<String> lore = new ArrayList<>();
        lore.add("§7Claim ID: §f" + claim.getID());
        lore.add("");
        lore.add("§7World: §f" + claim.getLesserBoundaryCorner().getWorld().getName());
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the claim size item.
     */
    private ItemStack createSizeItem() {
        int area = claimManager.getClaimArea(claim);

        ItemStack item = new ItemStack(Material.MAP);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(messages.getGuiSize(area));

        int width = claim.getWidth();
        int height = claim.getHeight();
        List<String> lore = new ArrayList<>();
        lore.add("§7Dimensions: §f" + width + " x " + height);
        lore.add("§7Blocks: §f" + area);
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the rent price item.
     */
    private ItemStack createRentPriceItem() {
        ItemStack item;
        double price = claimData.getRentPrice();

        if (claimData.isRented()) {
            item = new ItemStack(Material.CLOCK);
            ItemMeta meta = item.getItemMeta();
            long remainingTime = claimData.getRemainingRentTime();
            meta.setDisplayName(messages.getGuiRemainingTime(messages.formatDuration(remainingTime)));

            List<String> lore = new ArrayList<>();
            lore.add("§7Rented by: §f" + claimManager.getPlayerName(claimData.getRenter()));
            if (claimData.isAutoRenew()) {
                lore.add("");
                lore.add(messages.getLoreAutoRenewEnabled());
            } else {
                lore.add("");
                lore.add(messages.getLoreAutoRenewDisabled());
            }
            meta.setLore(lore);

            item.setItemMeta(meta);
        } else {
            item = new ItemStack(Material.GOLD_INGOT);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(messages.getGuiRentPrice(price));

            if (claimData.getRentDuration() > 0) {
                List<String> lore = new ArrayList<>();
                lore.add("§7Duration: §f" + messages.formatDuration(claimData.getRentDuration()));
                meta.setLore(lore);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Creates the sell price item.
     */
    private ItemStack createSellPriceItem() {
        double price = claimData.getSellPrice();

        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(messages.getGuiSellPrice(price));

        List<String> lore = new ArrayList<>();
        lore.add("§7Click to purchase this claim");
        lore.add("§7Ownership will be transferred");
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Adds the status indicator to the GUI.
     */
    private void addStatusIndicator(Inventory inv) {
        ItemStack item = new ItemStack(getStatusMaterial(claimData.getStatus()));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(messages.getGuiStatus(getStatusText(claimData.getStatus())));

        List<String> lore = new ArrayList<>();
        lore.add("§7Current claim status");
        meta.setLore(lore);

        item.setItemMeta(meta);
        inv.setItem(STATUS_SLOT, item);
    }

    /**
     * Gets the material for a status.
     */
    private Material getStatusMaterial(RentStatus status) {
        return switch (status) {
            case AVAILABLE -> Material.LIME_DYE;
            case RENTED -> Material.YELLOW_DYE;
            case FOR_SALE -> Material.ORANGE_DYE;
            case PRIVATE -> Material.GRAY_DYE;
        };
    }

    /**
     * Gets the display text for a status.
     */
    private String getStatusText(RentStatus status) {
        return switch (status) {
            case AVAILABLE -> messages.getStatusAvailable();
            case RENTED -> messages.getStatusRented();
            case FOR_SALE -> messages.getStatusForSale();
            case PRIVATE -> messages.getStatusPrivate();
        };
    }

    /**
     * Adds action buttons based on player and claim state.
     */
    private void addActionButtons(Inventory inv, Player player) {
        boolean isOwner = claimManager.isClaimOwner(player, claim);
        boolean isAdmin = player.hasPermission("landmgmt.admin");
        boolean canRent = player.hasPermission("landmgmt.rent");
        boolean canBuy = player.hasPermission("landmgmt.buy");

        // === OWNER MANAGEMENT BUTTONS (row 2) ===
        if (isOwner || isAdmin) {
            // Set rent price button
            inv.setItem(SET_RENT_PRICE_SLOT, createSetRentPriceButton());

            // Set sell price button
            inv.setItem(SET_SELL_PRICE_SLOT, createSetSellPriceButton());

            // Remove from sale button
            if (claimData.isForSale()) {
                inv.setItem(REMOVE_SALE_SLOT, createRemoveSaleButton());
            }

            // Cancel rent button (if claim is rented)
            if (claimData.isRented()) {
                inv.setItem(CANCEL_RENT_SLOT, createCancelRentButton());
            }

            // Toggle rent availability button
            if (claimData.getStatus() == RentStatus.AVAILABLE || claimData.getStatus() == RentStatus.RENTED) {
                inv.setItem(TOGGLE_RENT_SLOT, createToggleRentButton());
            }

            // Toggle sale button
            if (claimData.getStatus() == RentStatus.PRIVATE || claimData.getStatus() == RentStatus.FOR_SALE) {
                inv.setItem(TOGGLE_SALE_SLOT, createToggleSaleButton());
            }
        }

        // === PLAYER ACTION BUTTONS (rows 3-4) ===
        // Only show these if NOT the owner
        if (!isOwner || isAdmin) {
            // Set duration button (for renting)
            if (canRent && claimData.getStatus() == RentStatus.AVAILABLE) {
                inv.setItem(SET_DURATION_BUTTON_SLOT, createSetDurationButton());
            }

            // Rent button
            if (canRent && claimData.getStatus() == RentStatus.AVAILABLE && claimData.getRentDuration() > 0) {
                inv.setItem(RENT_BUTTON_SLOT, createRentButton());
            }

            // Buy button
            if (canBuy && claimData.isForSale()) {
                inv.setItem(BUY_BUTTON_SLOT, createBuyButton());
            }

            // Extend button (for current renter)
            if (claimData.isRenter(player.getUniqueId()) && !claimData.isRentExpired()) {
                inv.setItem(EXTEND_BUTTON_SLOT, createExtendButton());
            }

            // Auto-renew toggle (for current renter with permission)
            if (claimData.isRenter(player.getUniqueId()) && player.hasPermission("landmgmt.autorenew")) {
                inv.setItem(AUTO_RENEW_BUTTON_SLOT, createAutoRenewButton());
            }
        }

        // Close button (always present)
        inv.setItem(CLOSE_BUTTON_SLOT, createCloseButton());
    }

    /**
     * Creates the set duration button.
     */
    private ItemStack createSetDurationButton() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§bSet Rent Duration");

        List<String> lore = new ArrayList<>();
        lore.add("§7Choose how long to rent");
        lore.add("");
        if (claimData.getRentDuration() > 0) {
            lore.add("§7Current: §f" + messages.formatDuration(claimData.getRentDuration()));
        } else {
            lore.add("§7Current: §cNot set");
        }
        lore.add("");
        lore.add("§eClick to select duration");
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the rent button.
     */
    private ItemStack createRentButton() {
        ItemStack item = new ItemStack(Material.LIME_CONCRETE_POWDER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(messages.getButtonRent());

        List<String> lore = new ArrayList<>();
        lore.add(messages.getLoreRent());
        lore.add("");
        lore.add("§7Price: §f" + economyManager.formatAmount(claimData.getRentPrice()));
        lore.add("§7Duration: §f" + messages.formatDuration(claimData.getRentDuration()));
        lore.add("");
        lore.add("§eClick to rent this claim");
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the buy button.
     */
    private ItemStack createBuyButton() {
        ItemStack item = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(messages.getButtonBuy());

        List<String> lore = new ArrayList<>();
        lore.add(messages.getLoreBuy());
        lore.add("");
        lore.add("§7Price: §f" + economyManager.formatAmount(claimData.getSellPrice()));
        lore.add("");
        lore.add("§eClick to purchase this claim");
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the extend rent button.
     */
    private ItemStack createExtendButton() {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(messages.getButtonExtend());

        List<String> lore = new ArrayList<>();
        lore.add(messages.getLoreExtend());
        lore.add("");
        lore.add("§7Price: §f" + economyManager.formatAmount(claimData.getRentPrice()));
        lore.add("§7Duration: §f" + messages.formatDuration(claimData.getRentDuration()));
        lore.add("");
        lore.add("§eClick to extend your rent");
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the auto-renew toggle button.
     */
    private ItemStack createAutoRenewButton() {
        ItemStack item;
        if (claimData.isAutoRenew()) {
            item = new ItemStack(Material.LIME_DYE);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(messages.getAutoRenewEnabled());

            List<String> lore = new ArrayList<>();
            lore.add(messages.getLoreAutoRenewEnabled());
            lore.add("");
            lore.add("§eClick to disable auto-renew");
            meta.setLore(lore);
            item.setItemMeta(meta);
        } else {
            item = new ItemStack(Material.GRAY_DYE);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(messages.getAutoRenewDisabled());

            List<String> lore = new ArrayList<>();
            lore.add(messages.getLoreAutoRenewDisabled());
            lore.add("");
            lore.add("§eClick to enable auto-renew");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Creates the set rent price button (for owner).
     */
    private ItemStack createSetRentPriceButton() {
        ItemStack item = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6Set Rent Price");

        List<String> lore = new ArrayList<>();
        lore.add("§7Click to set rent price for this claim");
        lore.add("");
        if (claimData.getRentPrice() > 0) {
            lore.add("§7Current price: §f" + economyManager.formatAmount(claimData.getRentPrice()));
        } else {
            lore.add("§7Current: §cNot set");
        }
        lore.add("");
        lore.add("§eClick to set new price");
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the set sell price button (for owner).
     */
    private ItemStack createSetSellPriceButton() {
        ItemStack item = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§aSet Sell Price");

        List<String> lore = new ArrayList<>();
        lore.add("§7Click to sell this claim permanently");
        lore.add("");
        if (claimData.getSellPrice() > 0) {
            lore.add("§7Current price: §f" + economyManager.formatAmount(claimData.getSellPrice()));
        } else {
            lore.add("§7Current: §cNot for sale");
        }
        lore.add("");
        lore.add("§eClick to set price");
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the remove from sale button (for owner).
     */
    private ItemStack createRemoveSaleButton() {
        ItemStack item = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§cRemove From Sale");

        List<String> lore = new ArrayList<>();
        lore.add("§7Remove this claim from the market");
        lore.add("");
        lore.add("§7Current price: §f" + economyManager.formatAmount(claimData.getSellPrice()));
        lore.add("");
        lore.add("§eClick to remove from sale");
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the cancel rent button (for owner/admin).
     */
    private ItemStack createCancelRentButton() {
        ItemStack item = new ItemStack(Material.TNT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§4Cancel Rent");

        List<String> lore = new ArrayList<>();
        lore.add("§7End the current rent agreement");
        lore.add("");
        lore.add("§7Renter: §f" + claimManager.getPlayerName(claimData.getRenter()));
        lore.add("");
        lore.add("§cThis will revoke renter's access!");
        lore.add("§eClick to cancel rent");
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the toggle rent availability button (for owner).
     */
    private ItemStack createToggleRentButton() {
        ItemStack item;
        if (claimData.getStatus() == RentStatus.AVAILABLE || claimData.getStatus() == RentStatus.RENTED) {
            item = new ItemStack(Material.LIME_DYE);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§aDisable Renting");

            List<String> lore = new ArrayList<>();
            lore.add("§7Disable renting for this claim");
            lore.add("");
            lore.add("§7Status: §aAvailable for rent");
            if (claimData.getRentPrice() > 0) {
                lore.add("§7Price: §f" + economyManager.formatAmount(claimData.getRentPrice()));
            }
            lore.add("");
            lore.add("§eClick to disable renting");
            meta.setLore(lore);
            item.setItemMeta(meta);
            return item;
        } else {
            item = new ItemStack(Material.GRAY_DYE);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§aEnable Renting");

            List<String> lore = new ArrayList<>();
            lore.add("§7Enable renting for this claim");
            lore.add("");
            lore.add("§7Status: §cNot available");
            lore.add("");
            lore.add("§eClick to enable renting");
            meta.setLore(lore);
            item.setItemMeta(meta);
            return item;
        }
    }

    /**
     * Creates the toggle sale button (for owner).
     */
    private ItemStack createToggleSaleButton() {
        ItemStack item;
        if (claimData.isForSale()) {
            item = new ItemStack(Material.LIME_DYE);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§aDisable Sale");

            List<String> lore = new ArrayList<>();
            lore.add("§7Remove from sale market");
            lore.add("");
            lore.add("§7Status: §aFor sale");
            lore.add("§7Price: §f" + economyManager.formatAmount(claimData.getSellPrice()));
            lore.add("");
            lore.add("§eClick to disable sale");
            meta.setLore(lore);
            item.setItemMeta(meta);
            return item;
        } else {
            item = new ItemStack(Material.GRAY_DYE);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§aEnable Sale");

            List<String> lore = new ArrayList<>();
            lore.add("§7List claim for sale");
            lore.add("");
            lore.add("§7Status: §cNot for sale");
            lore.add("");
            lore.add("§eClick to set price & enable");
            meta.setLore(lore);
            item.setItemMeta(meta);
            return item;
        }
    }

    /**
     * Creates the close button.
     */
    private ItemStack createCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(messages.getButtonClose());

        List<String> lore = new ArrayList<>();
        lore.add("§7Click to close this menu");
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Adds decoration items (glass panes) to fill empty slots.
     */
    private void addDecoration(Inventory inv) {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        glass.setItemMeta(meta);

        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) {
                inv.setItem(i, glass);
            }
        }
    }

    /**
     * Handles a click in the GUI.
     */
    public CompletableFuture<ClickResult> handleClick(Player player, int slot) {
        // Check if clicked on info slots (should be ignored)
        for (int infoSlot : INFO_SLOTS) {
            if (slot == infoSlot) {
                // Just play a click sound, do nothing
                guiManager.playClickSound(player);
                return CompletableFuture.completedFuture(new ClickResult(null, null));
            }
        }

        // Check if clicked on status slot (should be ignored)
        if (slot == STATUS_SLOT) {
            guiManager.playClickSound(player);
            return CompletableFuture.completedFuture(new ClickResult(null, null));
        }

        if (slot == CLOSE_BUTTON_SLOT) {
            unregisterGUI(player);
            return CompletableFuture.completedFuture(new ClickResult(GUIAction.CLOSE, null));
        }

        // Owner management buttons
        if (slot == SET_RENT_PRICE_SLOT) {
            return handleSetRentPriceClick(player);
        }

        if (slot == SET_SELL_PRICE_SLOT) {
            return handleSetSellPriceClick(player);
        }

        if (slot == REMOVE_SALE_SLOT) {
            return handleRemoveSaleClick(player);
        }

        if (slot == CANCEL_RENT_SLOT) {
            return handleCancelRentClick(player);
        }

        if (slot == TOGGLE_RENT_SLOT) {
            return handleToggleRentClick(player);
        }

        if (slot == TOGGLE_SALE_SLOT) {
            return handleToggleSaleClick(player);
        }

        // Player action buttons
        if (slot == SET_DURATION_BUTTON_SLOT) {
            return handleSetDurationClick(player);
        }

        if (slot == RENT_BUTTON_SLOT) {
            return handleRentClick(player);
        }

        if (slot == BUY_BUTTON_SLOT) {
            return handleBuyClick(player);
        }

        if (slot == EXTEND_BUTTON_SLOT) {
            return handleExtendClick(player);
        }

        if (slot == AUTO_RENEW_BUTTON_SLOT) {
            return handleAutoRenewClick(player);
        }

        // Any other slot - just ignore and play click sound
        guiManager.playClickSound(player);
        return CompletableFuture.completedFuture(new ClickResult(null, null));
    }

    /**
     * Unregisters this GUI from the listener.
     */
    private void unregisterGUI(Player player) {
        plugin.getGUIListener().unregisterOpenGUI(player);
    }

    /**
     * Handles the set duration button click.
     */
    private CompletableFuture<ClickResult> handleSetDurationClick(Player player) {
        if (!player.hasPermission("landmgmt.rent")) {
            player.sendMessage(messages.getNoPermission());
            guiManager.playErrorSound(player);
            return CompletableFuture.completedFuture(new ClickResult(null, "No permission"));
        }

        // Validate rent price is set
        if (claimData.getRentPrice() <= 0) {
            player.sendMessage("§cPlease set rent price first!");
            player.sendMessage("§7Click 'Set Rent Price' button.");
            guiManager.playErrorSound(player);
            return CompletableFuture.completedFuture(new ClickResult(null, "No price set"));
        }

        // Register player for duration input (mirip dengan price setting)
        plugin.getChatListener().registerDurationInputSimple(player);

        player.closeInventory();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.sendMessage("§e=== Set Rent Duration ===");
            player.sendMessage("§7Enter duration (number + unit):");
            player.sendMessage("§7Examples: §f7d, 2w, 1m");
            player.sendMessage("§7  §f7 days, 2 weeks, 1 month");
            player.sendMessage("§7Units: §fh(hours) d(days) w(weeks) m(months)");
            player.sendMessage("§7Type §ccancel §7to cancel");
            player.sendMessage("");
            player.sendMessage("§aWaiting for input...");
        }, 1L);

        return CompletableFuture.completedFuture(new ClickResult(null, null));
    }

    /**
     * Handles the rent button click.
     */
    private CompletableFuture<ClickResult> handleRentClick(Player player) {
        plugin.getLogger().info("=== RENT CLICK DEBUG ===");
        plugin.getLogger().info("Player: " + player.getName() + " (" + player.getUniqueId() + ")");
        plugin.getLogger().info("Claim Owner: " + claimData.getOwner());
        plugin.getLogger().info("Claim Status: " + claimData.getStatus());

        // Check permission
        if (!player.hasPermission("landmgmt.rent")) {
            player.sendMessage(messages.getNoPermission());
            guiManager.playErrorSound(player);
            return CompletableFuture.completedFuture(new ClickResult(null, "No permission"));
        }

        // FIX: Check if player is the OWNER - cannot rent own claim!
        if (claimData.isOwner(player.getUniqueId())) {
            plugin.getLogger().info("Blocked: Owner tried to rent own claim");
            player.sendMessage("§cYou cannot rent your own claim!");
            player.sendMessage("§7You already own this land.");
            guiManager.playErrorSound(player);
            return CompletableFuture.completedFuture(new ClickResult(null, "Cannot rent own claim"));
        }

        // Check if claim is being rented by same player
        if (claimData.isRenter(player.getUniqueId())) {
            plugin.getLogger().info("Blocked: Renter tried to rent again");
            player.sendMessage("§cYou are already renting this claim!");
            player.sendMessage("§7Use the 'Extend' button to extend your rent.");
            guiManager.playErrorSound(player);
            return CompletableFuture.completedFuture(new ClickResult(null, "Already renting"));
        }

        // Check if duration is set
        if (claimData.getRentDuration() <= 0) {
            player.sendMessage("§cPlease set rent duration first!");
            player.sendMessage("§7Click the 'Set Duration' button.");
            guiManager.playErrorSound(player);
            return CompletableFuture.completedFuture(new ClickResult(null, "Duration not set"));
        }

        plugin.getLogger().info("Rent validation passed, starting rent process...");

        return rentManager.startRent(player, claimData, claimData.getRentDuration())
                .thenApply(result -> {
                    if (result.success()) {
                        guiManager.playRentSuccessSound(player);
                        plugin.getLogger().info("Rent successful for " + player.getName());
                        return new ClickResult(GUIAction.RENT, result.message());
                    } else {
                        guiManager.playErrorSound(player);
                        plugin.getLogger().warning("Rent failed for " + player.getName() + ": " + result.message());
                        return new ClickResult(null, result.message());
                    }
                });
    }

    /**
     * Handles the buy button click.
     */
    private CompletableFuture<ClickResult> handleBuyClick(Player player) {
        plugin.getLogger().info("=== BUY CLICK DEBUG ===");
        plugin.getLogger().info("Player: " + player.getName() + " (" + player.getUniqueId() + ")");
        plugin.getLogger().info("Claim Owner: " + claimData.getOwner());
        plugin.getLogger().info("Sell Price: " + claimData.getSellPrice());

        // Check permission
        if (!player.hasPermission("landmgmt.buy")) {
            player.sendMessage(messages.getNoPermission());
            guiManager.playErrorSound(player);
            return CompletableFuture.completedFuture(new ClickResult(null, "No permission"));
        }

        // Check if player is owner - FIX: More detailed check
        if (claimData.isOwner(player.getUniqueId())) {
            plugin.getLogger().info("Blocked: Owner tried to buy own claim");
            player.sendMessage("§cYou cannot buy your own claim!");
            player.sendMessage("§7You already own this land.");
            guiManager.playErrorSound(player);
            return CompletableFuture.completedFuture(new ClickResult(null, "Cannot buy own claim"));
        }

        // Check if player is currently renting - cannot buy while renting
        if (claimData.isRenter(player.getUniqueId())) {
            plugin.getLogger().info("Blocked: Renter tried to buy the claim they're renting");
            player.sendMessage("§cYou cannot buy a claim you're currently renting!");
            player.sendMessage("§7Wait for your rent to expire first.");
            guiManager.playErrorSound(player);
            return CompletableFuture.completedFuture(new ClickResult(null, "Cannot buy while renting"));
        }

        // Check price
        double price = claimData.getSellPrice();
        if (price <= 0) {
            player.sendMessage("§cThis claim is not for sale!");
            guiManager.playErrorSound(player);
            return CompletableFuture.completedFuture(new ClickResult(null, "Not for sale"));
        }

        // Process payment ATOMICALLY using processPayment
        // Payment is only bypassed if player has landmgmt.bypass permission
        if (economyManager.isAvailable() && price > 0 && !player.hasPermission("landmgmt.bypass")) {
            if (claimData.getOwner() == null) {
                player.sendMessage("§cCannot buy: No owner found!");
                guiManager.playErrorSound(player);
                return CompletableFuture.completedFuture(new ClickResult(null, "No owner"));
            }

            plugin.getLogger().info("Processing payment for purchase...");

            // Use atomic processPayment
            return economyManager.processPayment(
                    player.getUniqueId(),
                    claimData.getOwner(),
                    price,
                    "Purchase of claim " + claimData.getClaimId()
                ).thenCompose(paymentResult -> {
                    if (!paymentResult.success()) {
                        plugin.getLogger().warning("Purchase payment failed: " + paymentResult.message());
                        guiManager.playErrorSound(player);
                        return CompletableFuture.completedFuture(new ClickResult(null, messages.getBuyFailed(paymentResult.message())));
                    }

                    plugin.getLogger().info("Payment successful! Transferring ownership...");

                    // Payment successful, now transfer ownership
                    return claimManager.transferOwnership(claim, player.getUniqueId())
                        .thenApply(success -> {
                            if (success) {
                                guiManager.playBuySuccessSound(player);
                                // Close inventory synchronously on main thread
                                plugin.getServer().getScheduler().runTask(plugin, () -> {
                                    player.closeInventory();
                                });
                                plugin.getLogger().info("Purchase successful! Claim transferred to " + player.getName());

                                // Notify the seller (previous owner)
                                UUID previousOwner = claimData.getOwner();
                                if (previousOwner != null) {
                                    org.bukkit.OfflinePlayer seller = Bukkit.getOfflinePlayer(previousOwner);
                                    if (seller.isOnline()) {
                                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                                            Player sellerPlayer = seller.getPlayer();
                                            if (sellerPlayer != null) {
                                                sellerPlayer.sendMessage("");
                                                sellerPlayer.sendMessage("§6§l=== LAND SOLD ===");
                                                sellerPlayer.sendMessage("§aYour claim has been purchased!");
                                                sellerPlayer.sendMessage("§7Location: Claim #" + claim.getID());
                                                sellerPlayer.sendMessage("§7Buyer: §e" + player.getName());
                                                sellerPlayer.sendMessage("§7You received: §e" + economyManager.formatAmount(price));
                                                sellerPlayer.sendMessage("§6§l===================");
                                                guiManager.playBuySuccessSound(sellerPlayer);
                                            }
                                        });
                                    }
                                }

                                return new ClickResult(GUIAction.BUY, messages.getBuySuccess(price));
                            } else {
                                plugin.getLogger().severe("Ownership transfer failed after payment!");
                                // Refund buyer since transfer failed
                                economyManager.depositPlayer(player, price);
                                return new ClickResult(null, messages.getBuyFailed("Transfer failed - refunded"));
                            }
                        });
                })
                .exceptionally(e -> {
                    plugin.getLogger().log(Level.SEVERE, "Error processing purchase", e);
                    guiManager.playErrorSound(player);
                    return new ClickResult(null, messages.getErrorOccurred());
                });
        }

        // Free transfer (has bypass permission or price=0)
        String bypassReason = player.hasPermission("landmgmt.bypass") ? "has bypass permission" : "price is 0";
        plugin.getLogger().info("No payment needed (player " + bypassReason + "), transferring ownership...");

        return claimManager.transferOwnership(claim, player.getUniqueId())
            .thenApply(success -> {
                if (success) {
                    guiManager.playBuySuccessSound(player);
                    // Close inventory synchronously on main thread
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        player.closeInventory();
                    });
                    plugin.getLogger().info("Transfer successful!");
                    return new ClickResult(GUIAction.BUY, "§aClaim transferred successfully!");
                } else {
                    plugin.getLogger().severe("Transfer failed!");
                    return new ClickResult(null, messages.getBuyFailed("Transfer failed"));
                }
            });
    }

    /**
     * Handles the extend button click.
     */
    private CompletableFuture<ClickResult> handleExtendClick(Player player) {
        if (!player.hasPermission("landmgmt.rent")) {
            player.sendMessage(messages.getNoPermission());
            guiManager.playErrorSound(player);
            return CompletableFuture.completedFuture(new ClickResult(null, "No permission"));
        }

        return rentManager.extendRent(player, claimData, claimData.getRentDuration())
                .thenApply(result -> {
                    if (result.success()) {
                        guiManager.playRentSuccessSound(player);
                        return new ClickResult(GUIAction.EXTEND, result.message());
                    } else {
                        guiManager.playErrorSound(player);
                        return new ClickResult(null, result.message());
                    }
                });
    }

    /**
     * Handles the auto-renew toggle click.
     */
    private CompletableFuture<ClickResult> handleAutoRenewClick(Player player) {
        if (!player.hasPermission("landmgmt.autorenew")) {
            player.sendMessage(messages.getNoPermission());
            guiManager.playErrorSound(player);
            return CompletableFuture.completedFuture(new ClickResult(null, "No permission"));
        }

        return rentManager.toggleAutoRenew(player, claimData)
                .thenApply(enabled -> {
                    guiManager.playClickSound(player);
                    String message = enabled ? messages.getAutoRenewEnabled() : messages.getAutoRenewDisabled();
                    return new ClickResult(GUIAction.TOGGLE_AUTO_RENEW, message);
                });
    }

    /**
     * Handles the set rent price button click (owner).
     */
    private CompletableFuture<ClickResult> handleSetRentPriceClick(Player player) {
        if (!claimManager.isClaimOwner(player, claim) && !player.hasPermission("landmgmt.admin")) {
            player.sendMessage(messages.getNotYourClaim());
            guiManager.playErrorSound(player);
            return CompletableFuture.completedFuture(new ClickResult(null, "Not owner"));
        }

        // Register player for chat input
        plugin.getChatListener().registerPriceInput(player, dev.cosax.cSXLandManager.listener.ChatListener.PriceInputType.SET_RENT_PRICE);

        player.closeInventory();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.sendMessage("§e=== Set Rent Price ===");
            player.sendMessage("§7Please enter the rent price in chat:");
            player.sendMessage("§7Example: §f1000");
            player.sendMessage("§7Enter §f0 §7to disable renting");
            player.sendMessage("§7Type §ccancel §7to cancel");
            player.sendMessage("");
            player.sendMessage("§aWaiting for input...");
        }, 1L);

        return CompletableFuture.completedFuture(new ClickResult(null, null));
    }

    /**
     * Handles the set sell price button click (owner).
     */
    private CompletableFuture<ClickResult> handleSetSellPriceClick(Player player) {
        if (!claimManager.isClaimOwner(player, claim) && !player.hasPermission("landmgmt.admin")) {
            player.sendMessage(messages.getNotYourClaim());
            guiManager.playErrorSound(player);
            return CompletableFuture.completedFuture(new ClickResult(null, "Not owner"));
        }

        // Register player for chat input
        plugin.getChatListener().registerPriceInput(player, dev.cosax.cSXLandManager.listener.ChatListener.PriceInputType.SET_SELL_PRICE);

        player.closeInventory();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.sendMessage("§e=== Set Sell Price ===");
            player.sendMessage("§7Please enter the sell price in chat:");
            player.sendMessage("§7Example: §f5000");
            player.sendMessage("§7Enter §f0 §7to remove from sale");
            player.sendMessage("§7Type §ccancel §7to cancel");
            player.sendMessage("");
            player.sendMessage("§aWaiting for input...");
        }, 1L);

        return CompletableFuture.completedFuture(new ClickResult(null, null));
    }

    /**
     * Handles the remove from sale button click (owner).
     */
    private CompletableFuture<ClickResult> handleRemoveSaleClick(Player player) {
        if (!claimManager.isClaimOwner(player, claim) && !player.hasPermission("landmgmt.admin")) {
            player.sendMessage(messages.getNotYourClaim());
            guiManager.playErrorSound(player);
            return CompletableFuture.completedFuture(new ClickResult(null, "Not owner"));
        }

        claimData.setSellPrice(0);
        claimData.setStatus(RentStatus.PRIVATE);
        plugin.getStorageManager().saveClaimData(claimData).join();

        guiManager.playSuccessSound(player);
        return CompletableFuture.completedFuture(new ClickResult(GUIAction.REMOVE_FROM_SALE, messages.getClaimRemovedSale()));
    }

    /**
     * Handles the cancel rent button click (owner/admin).
     */
    private CompletableFuture<ClickResult> handleCancelRentClick(Player player) {
        if (!player.hasPermission("landmgmt.admin") && !claimManager.isClaimOwner(player, claim)) {
            player.sendMessage(messages.getNoPermission());
            guiManager.playErrorSound(player);
            return CompletableFuture.completedFuture(new ClickResult(null, "No permission"));
        }

        return rentManager.endRent(player, claimData, player.hasPermission("landmgmt.admin"))
                .thenApply(result -> {
                    if (result.success()) {
                        guiManager.playSuccessSound(player);
                        return new ClickResult(GUIAction.CANCEL_RENT, result.message());
                    } else {
                        guiManager.playErrorSound(player);
                        return new ClickResult(null, result.message());
                    }
                });
    }

    /**
     * Handles the toggle rent availability button click (owner).
     */
    private CompletableFuture<ClickResult> handleToggleRentClick(Player player) {
        if (!claimManager.isClaimOwner(player, claim) && !player.hasPermission("landmgmt.admin")) {
            player.sendMessage(messages.getNotYourClaim());
            guiManager.playErrorSound(player);
            return CompletableFuture.completedFuture(new ClickResult(null, "Not owner"));
        }

        if (claimData.getStatus() == RentStatus.AVAILABLE || claimData.getStatus() == RentStatus.RENTED) {
            // Disable renting
            if (claimData.isRented()) {
                player.sendMessage("§cCannot disable while claim is rented!");
                guiManager.playErrorSound(player);
                return CompletableFuture.completedFuture(new ClickResult(null, "Currently rented"));
            }

            claimData.setStatus(RentStatus.PRIVATE);
            claimData.setRentPrice(0);
            plugin.getStorageManager().saveClaimData(claimData).join();

            guiManager.playSuccessSound(player);
            return CompletableFuture.completedFuture(new ClickResult(GUIAction.TOGGLE_RENT, "§aRenting disabled for this claim"));
        } else {
            // Enable renting (requires price to be set)
            if (claimData.getRentPrice() <= 0) {
                player.sendMessage("§cPlease set a rent price first!");
                guiManager.playErrorSound(player);
                return CompletableFuture.completedFuture(new ClickResult(null, "No price set"));
            }

            claimData.setStatus(RentStatus.AVAILABLE);
            plugin.getStorageManager().saveClaimData(claimData).join();

            guiManager.playSuccessSound(player);
            return CompletableFuture.completedFuture(new ClickResult(GUIAction.TOGGLE_RENT, "§aRenting enabled for this claim"));
        }
    }

    /**
     * Handles the toggle sale button click (owner).
     */
    private CompletableFuture<ClickResult> handleToggleSaleClick(Player player) {
        if (!claimManager.isClaimOwner(player, claim) && !player.hasPermission("landmgmt.admin")) {
            player.sendMessage(messages.getNotYourClaim());
            guiManager.playErrorSound(player);
            return CompletableFuture.completedFuture(new ClickResult(null, "Not owner"));
        }

        if (claimData.isForSale()) {
            // Disable sale
            claimData.setSellPrice(0);
            claimData.setStatus(RentStatus.PRIVATE);
            plugin.getStorageManager().saveClaimData(claimData).join();

            guiManager.playSuccessSound(player);
            return CompletableFuture.completedFuture(new ClickResult(GUIAction.TOGGLE_SALE, "§aSale disabled for this claim"));
        } else {
            // Enable sale (requires price to be set)
            if (claimData.getSellPrice() <= 0) {
                player.sendMessage("§cPlease set a sell price first!");
                guiManager.playErrorSound(player);
                return CompletableFuture.completedFuture(new ClickResult(null, "No price set"));
            }

            claimData.setStatus(RentStatus.FOR_SALE);
            plugin.getStorageManager().saveClaimData(claimData).join();

            guiManager.playSuccessSound(player);
            return CompletableFuture.completedFuture(new ClickResult(GUIAction.TOGGLE_SALE, "§aClaim is now for sale!"));
        }
    }

    /**
     * Result of a GUI click.
     */
    public record ClickResult(
            GUIAction action,
            String message
    ) {}
}
