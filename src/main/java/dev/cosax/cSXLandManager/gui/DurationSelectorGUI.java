package dev.cosax.cSXLandManager.gui;

import dev.cosax.cSXLandManager.CSXLandManager;
import dev.cosax.cSXLandManager.config.Config;
import dev.cosax.cSXLandManager.listener.ChatListener;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * GUI for selecting rent duration.
 */
public class DurationSelectorGUI {

    private final CSXLandManager plugin;
    private final Config config;
    private final me.ryanhamshire.GriefPrevention.Claim claim;
    private final dev.cosax.cSXLandManager.model.ClaimData claimData;

    // Duration type slots
    private static final int MINUTES_SLOT = 11;
    private static final int HOURS_SLOT = 13;
    private static final int DAYS_SLOT = 15;
    private static final int WEEKS_SLOT = 20;
    private static final int MONTHS_SLOT = 22;
    private static final int CUSTOM_SLOT = 24;
    private static final int BACK_SLOT = 40;
    private static final int CLOSE_SLOT = 44;

    public DurationSelectorGUI(CSXLandManager plugin, me.ryanhamshire.GriefPrevention.Claim claim,
                               dev.cosax.cSXLandManager.model.ClaimData claimData) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.claim = claim;
        this.claimData = claimData;
    }

    /**
     * Opens the duration selector GUI.
     */
    public void open(Player player) {
        // Create inventory with smaller size (45 slots)
        Inventory inv = org.bukkit.Bukkit.createInventory(null, 45,
                org.bukkit.ChatColor.translateAlternateColorCodes('&', "&eSelect Rent Duration"));

        // Add duration selection buttons
        addDurationButtons(inv);

        // Add decoration
        addDecoration(inv);

        // Register with GUI listener
        plugin.getGUIListener().registerOpenGUI(player, this);

        player.openInventory(inv);
        plugin.getGUIManager().playClickSound(player);
    }

    /**
     * Adds duration selection buttons to the GUI.
     */
    private void addDurationButtons(Inventory inv) {
        // Minutes button
        inv.setItem(MINUTES_SLOT, createDurationButton(
                Material.CLOCK,
                "&cMinutes",
                "§7Rent by minutes",
                "",
                "§7Example: §f30 minutes",
                "§eClick to select"
        ));

        // Hours button
        inv.setItem(HOURS_SLOT, createDurationButton(
                Material.CLOCK,
                "&6Hours",
                "§7Rent by hours",
                "",
                "§7Example: §f24 hours",
                "§eClick to select"
        ));

        // Days button
        inv.setItem(DAYS_SLOT, createDurationButton(
                Material.SUNFLOWER,
                "&eDays",
                "§7Rent by days",
                "",
                "§7Example: §f7 days",
                "§eClick to select"
        ));

        // Weeks button
        inv.setItem(WEEKS_SLOT, createDurationButton(
                Material.DIAMOND_HOE,
                "&aWeeks",
                "§7Rent by weeks",
                "",
                "§7Example: §f2 weeks",
                "§eClick to select"
        ));

        // Months button
        inv.setItem(MONTHS_SLOT, createDurationButton(
                Material.GOLDEN_HOE,
                "&bMonths",
                "§7Rent by months",
                "",
                "§7Example: §f1 month",
                "§eClick to select"
        ));

        // Custom duration button
        inv.setItem(CUSTOM_SLOT, createDurationButton(
                Material.COMPASS,
                "&dCustom Duration",
                "§7Enter custom duration",
                "",
                "§7Specify both unit and amount",
                "§7Example: §f3 days",
                "§eClick to select"
        ));

        // Back button
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName("§cBack");
        List<String> backLore = new ArrayList<>();
        backLore.add("§7Return to claim management");
        backMeta.setLore(backLore);
        backItem.setItemMeta(backMeta);
        inv.setItem(BACK_SLOT, backItem);

        // Close button
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName("§cClose");
        List<String> closeLore = new ArrayList<>();
        closeLore.add("§7Close this menu");
        closeMeta.setLore(closeLore);
        closeItem.setItemMeta(closeMeta);
        inv.setItem(CLOSE_SLOT, closeItem);
    }

    /**
     * Creates a duration button.
     */
    private ItemStack createDurationButton(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', name));

        List<String> finalLore = new ArrayList<>();
        for (String line : lore) {
            finalLore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(finalLore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Adds decoration items.
     */
    private void addDecoration(Inventory inv) {
        ItemStack glass = new ItemStack(org.bukkit.Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        glass.setItemMeta(meta);

        // Skip button slots to prevent glass from covering clickable buttons
        int[] buttonSlots = {MINUTES_SLOT, HOURS_SLOT, DAYS_SLOT, WEEKS_SLOT, MONTHS_SLOT, CUSTOM_SLOT, BACK_SLOT, CLOSE_SLOT};

        for (int i = 0; i < inv.getSize(); i++) {
            // Check if this slot is a button slot
            boolean isButtonSlot = false;
            for (int slot : buttonSlots) {
                if (i == slot) {
                    isButtonSlot = true;
                    break;
                }
            }

            // Only add glass to non-button slots
            if (!isButtonSlot && (inv.getItem(i) == null || inv.getItem(i).getType() == org.bukkit.Material.AIR)) {
                inv.setItem(i, glass);
            }
        }
    }

    /**
     * Handles a click in the duration selector.
     */
    public CompletableFuture<DurationResult> handleClick(Player player, int slot) {
        if (slot == CLOSE_SLOT) {
            return CompletableFuture.completedFuture(new DurationResult(null, null, true));
        }

        if (slot == BACK_SLOT) {
            return CompletableFuture.completedFuture(new DurationResult(null, null, false));
        }

        if (slot == MINUTES_SLOT) {
            return promptForAmount(player, TimeUnit.MINUTES);
        }

        if (slot == HOURS_SLOT) {
            return promptForAmount(player, TimeUnit.HOURS);
        }

        if (slot == DAYS_SLOT) {
            return promptForAmount(player, TimeUnit.DAYS);
        }

        if (slot == WEEKS_SLOT) {
            return promptForAmount(player, TimeUnit.WEEKS);
        }

        if (slot == MONTHS_SLOT) {
            return promptForAmount(player, TimeUnit.MONTHS);
        }

        if (slot == CUSTOM_SLOT) {
            return promptForCustom(player);
        }

        return CompletableFuture.completedFuture(new DurationResult(null, "Invalid selection", false));
    }

    /**
     * Prompts player for amount of selected time unit.
     */
    private CompletableFuture<DurationResult> promptForAmount(Player player, TimeUnit unit) {
        CompletableFuture<DurationResult> future = new CompletableFuture<>();

        plugin.getLogger().info("Prompting for amount - Player: " + player.getName() + ", Unit: " + unit);

        // Register input first, before closing inventory
        plugin.getChatListener().registerDurationInput(player, unit);

        // Verify registration
        plugin.getLogger().info("Player registered for duration input: " + plugin.getChatListener().isWaitingForInput(player));

        player.closeInventory();

        // Send messages and complete future on next tick
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            player.sendMessage("§e=== Set Rent Duration ===");
            player.sendMessage("§7Enter the number of " + unit.getDisplayName().toLowerCase() + ":");
            player.sendMessage("§7Example: §f7");
            player.sendMessage("§7Type §ccancel §7to cancel");
            player.sendMessage("");
            player.sendMessage("§aWaiting for input...");
            plugin.getGUIManager().playClickSound(player);

            // Verify registration again
            plugin.getLogger().info("After messages - Player still waiting: " + plugin.getChatListener().isWaitingForInput(player));

            // Complete future to signal that prompting is done
            future.complete(new DurationResult(null, "waiting", false));
        });

        return future;
    }

    /**
     * Prompts player for custom duration (e.g., "3 days").
     */
    private CompletableFuture<DurationResult> promptForCustom(Player player) {
        CompletableFuture<DurationResult> future = new CompletableFuture<>();

        plugin.getLogger().info("Prompting for custom duration - Player: " + player.getName());

        // Register input first, before closing inventory
        plugin.getChatListener().registerDurationInput(player, TimeUnit.CUSTOM);

        // Verify registration
        plugin.getLogger().info("Player registered for custom input: " + plugin.getChatListener().isWaitingForInput(player));

        player.closeInventory();

        // Send messages and complete future on next tick
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            player.sendMessage("§e=== Custom Duration ===");
            player.sendMessage("§7Enter duration (number + unit):");
            player.sendMessage("§7Examples: §f3 days, 2 weeks, 12 hours");
            player.sendMessage("§7Type §ccancel §7to cancel");
            player.sendMessage("");
            player.sendMessage("§aWaiting for input...");
            plugin.getGUIManager().playClickSound(player);

            // Verify registration again
            plugin.getLogger().info("After messages - Player still waiting: " + plugin.getChatListener().isWaitingForInput(player));

            // Complete future to signal that prompting is done
            future.complete(new DurationResult(null, "waiting", false));
        });

        return future;
    }

    /**
     * Time unit enum.
     */
    public enum TimeUnit {
        MINUTES("Minutes", 60000L),     // 1 minute
        HOURS("Hours", 3600000L),       // 1 hour
        DAYS("Days", 86400000L),        // 1 day
        WEEKS("Weeks", 604800000L),     // 1 week
        MONTHS("Months", 2592000000L),  // 30 days
        CUSTOM("Custom", 0L);

        private final String displayName;
        private final long milliseconds;

        TimeUnit(String displayName, long milliseconds) {
            this.displayName = displayName;
            this.milliseconds = milliseconds;
        }

        public String getDisplayName() {
            return displayName;
        }

        public long getMilliseconds() {
            return milliseconds;
        }

        public long getMilliseconds(long amount) {
            if (this == CUSTOM) {
                throw new IllegalArgumentException("Use CUSTOM.parseDuration() instead");
            }
            return milliseconds * amount;
        }
    }

    /**
     * Result of duration selection.
     */
    public record DurationResult(
            Long durationMillis,
            String message,
            boolean close
    ) {}
}
