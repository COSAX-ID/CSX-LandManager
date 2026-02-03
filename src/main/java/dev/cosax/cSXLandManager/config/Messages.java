package dev.cosax.cSXLandManager.config;

import dev.cosax.cSXLandManager.CSXLandManager;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.Duration;
import java.util.UUID;

/**
 * Messages handler for CSX LandManager.
 * Loads and provides access to messages.yml settings.
 */
public class Messages {

    private final CSXLandManager plugin;
    private FileConfiguration messagesConfig;

    public Messages(CSXLandManager plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    /**
     * Loads messages from messages.yml.
     */
    public void loadMessages() {
        plugin.saveResource("messages.yml", false);
        messagesConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                new java.io.File(plugin.getDataFolder(), "messages.yml"));
    }

    /**
     * Reloads messages from disk.
     */
    public void reload() {
        plugin.reloadConfig();
        loadMessages();
    }

    /**
     * Gets a colored message from the config.
     */
    private String getMessage(String path) {
        String prefix = colorize(messagesConfig.getString("prefix", "&6[LandManager] &r"));
        String message = messagesConfig.getString(path);
        if (message == null) {
            return prefix + "Missing message: " + path;
        }
        return prefix + colorize(message);
    }

    /**
     * Gets a raw message (without prefix).
     */
    private String getRawMessage(String path) {
        String message = messagesConfig.getString(path);
        if (message == null) {
            return "Missing message: " + path;
        }
        return colorize(message);
    }

    /**
     * Colorizes a string.
     */
    private String colorize(String string) {
        if (string == null) return "";
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', string);
    }

    /**
     * Formats a duration into a human-readable string.
     */
    public String formatDuration(long milliseconds) {
        if (milliseconds <= 0) {
            return getRawMessage("time.expired");
        }

        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        // Use the configured format
        String format = messagesConfig.getString("time.format", "{days}d {hours}h {minutes}m {seconds}s");

        return format
                .replace("{days}", String.valueOf(days))
                .replace("{hours}", String.valueOf(hours % 24))
                .replace("{minutes}", String.valueOf(minutes % 60))
                .replace("{seconds}", String.valueOf(seconds % 60));
    }

    /**
     * Formats a price for display.
     */
    public String formatPrice(double price) {
        if (price == (long) price) {
            return String.format("%,.0f", price);
        } else {
            return String.format("%,.2f", price);
        }
    }

    /**
     * Replaces placeholders in a message.
     */
    private String replacePlaceholders(String message, String... placeholders) {
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
        }
        return message;
    }

    // General messages
    public String getNoPermission() {
        return getMessage("no-permission");
    }

    public String getOnlyPlayers() {
        return getMessage("only-players");
    }

    public String getPluginReloaded() {
        return getMessage("plugin-reloaded");
    }

    public String getInvalidNumber(String number) {
        return getMessage("invalid-number").replace("{number}", number);
    }

    // Claim messages
    public String getNotInClaim() {
        return getMessage("not-in-claim");
    }

    public String getClaimNotFound() {
        return getMessage("claim-not-found");
    }

    public String getNotYourClaim() {
        return getMessage("not-your-claim");
    }

    public String getClaimNotManaged() {
        return getMessage("claim-not-managed");
    }

    public String getClaimAlreadyManaged() {
        return getMessage("claim-already-managed");
    }

    // Rent messages
    public String getRentSuccess(String duration) {
        return getMessage("rent-success").replace("{duration}", duration);
    }

    public String getRentFailed(String reason) {
        return getMessage("rent-failed").replace("{reason}", reason);
    }

    public String getRentAlreadyRented(String player) {
        return getMessage("rent-already-rented").replace("{player}", player);
    }

    public String getRentNotAvailable() {
        return getMessage("rent-not-available");
    }

    public String getRentExtended(String duration) {
        return getMessage("rent-extended").replace("{duration}", duration);
    }

    public String getRentExtendFailed(String reason) {
        return getMessage("rent-extend-failed").replace("{reason}", reason);
    }

    public String getRentExpired() {
        return getMessage("rent-expired");
    }

    public String getRentCancelled() {
        return getMessage("rent-cancelled");
    }

    public String getRentCancelledAdmin() {
        return getMessage("rent-cancelled-admin");
    }

    public String getRentCancelledOwner() {
        return getMessage("rent-cancelled-owner");
    }

    public String getNoActiveRent() {
        return getMessage("no-active-rent");
    }

    public String getInsufficientFunds(double needed, double have) {
        return getMessage("insufficient-funds")
                .replace("{needed}", formatPrice(needed))
                .replace("{have}", formatPrice(have));
    }

    public String getPriceSet(double price) {
        return getMessage("price-set").replace("{price}", formatPrice(price));
    }

    public String getAutoRenewEnabled() {
        return getMessage("auto-renew-enabled");
    }

    public String getAutoRenewDisabled() {
        return getMessage("auto-renew-disabled");
    }

    public String getAutoRenewFailed() {
        return getMessage("auto-renew-failed");
    }

    public String getAutoRenewDisabledLowFunds() {
        return getMessage("auto-renew-disabled-low-funds");
    }

    // Buy/Sell messages
    public String getSellPriceSet(double price) {
        return getMessage("sell-price-set").replace("{price}", formatPrice(price));
    }

    public String getClaimListed(double price) {
        return getMessage("claim-listed").replace("{price}", formatPrice(price));
    }

    public String getClaimRemovedSale() {
        return getMessage("claim-removed-sale");
    }

    public String getBuySuccess(double price) {
        return getMessage("buy-success").replace("{price}", formatPrice(price));
    }

    public String getBuyFailed(String reason) {
        return getMessage("buy-failed").replace("{reason}", reason);
    }

    public String getNotForSale() {
        return getMessage("not-for-sale");
    }

    public String getCannotBuyOwnClaim() {
        return getMessage("cannot-buy-own-claim");
    }

    // Economy messages
    public String getEconomyNotFound() {
        return getMessage("economy-not-found");
    }

    public String getEconomyError() {
        return getMessage("economy-error");
    }

    // GriefPrevention messages
    public String getGpNotFound() {
        return getMessage("gp-not-found");
    }

    public String getGpError() {
        return getMessage("gp-error");
    }

    // GUI messages
    public String getGuiClaimInfo() {
        return getRawMessage("gui.claim-info");
    }

    public String getGuiOwner(String owner) {
        return getRawMessage("gui.owner").replace("{owner}", owner);
    }

    public String getGuiRenter(String renter) {
        return getRawMessage("gui.renter").replace("{renter}", renter);
    }

    public String getGuiSize(int area) {
        return getRawMessage("gui.size").replace("{area}", String.format("%,d", area));
    }

    public String getGuiRentPrice(double price) {
        return getRawMessage("gui.rent-price").replace("{price}", formatPrice(price));
    }

    public String getGuiSellPrice(double price) {
        return getRawMessage("gui.sell-price").replace("{price}", formatPrice(price));
    }

    public String getGuiRentDuration(String duration) {
        return getRawMessage("gui.rent-duration").replace("{duration}", duration);
    }

    public String getGuiRemainingTime(String time) {
        return getRawMessage("gui.remaining-time").replace("{time}", time);
    }

    public String getGuiStatus(String status) {
        return getRawMessage("gui.status").replace("{status}", status);
    }

    public String getGuiAutoRenew(boolean enabled) {
        String status = getRawMessage(enabled ? "gui.status-available" : "gui.status-private");
        return getRawMessage("gui.auto-renew").replace("{enabled}", status);
    }

    public String getStatusAvailable() {
        return getRawMessage("gui.status-available");
    }

    public String getStatusRented() {
        return getRawMessage("gui.status-rented");
    }

    public String getStatusForSale() {
        return getRawMessage("gui.status-for-sale");
    }

    public String getStatusPrivate() {
        return getRawMessage("gui.status-private");
    }

    public String getButtonRent() {
        return getRawMessage("gui.button-rent");
    }

    public String getButtonBuy() {
        return getRawMessage("gui.button-buy");
    }

    public String getButtonExtend() {
        return getRawMessage("gui.button-extend");
    }

    public String getButtonAutoRenew() {
        return getRawMessage("gui.button-auto-renew");
    }

    public String getButtonClose() {
        return getRawMessage("gui.button-close");
    }

    public String getLoreRent() {
        return getRawMessage("gui.lore-rent");
    }

    public String getLoreBuy() {
        return getRawMessage("gui.lore-buy");
    }

    public String getLoreExtend() {
        return getRawMessage("gui.lore-extend");
    }

    public String getLoreAutoRenewEnabled() {
        return getRawMessage("gui.lore-auto-renew-enabled");
    }

    public String getLoreAutoRenewDisabled() {
        return getRawMessage("gui.lore-auto-renew-disabled");
    }

    // Admin messages
    public String getAdminOnly() {
        return getMessage("admin-only");
    }

    public String getAdminBypass() {
        return getMessage("admin-bypass");
    }

    public String getRentForceEnded() {
        return getMessage("rent-force-ended");
    }

    public String getOwnershipTransferred(String from, String to) {
        return getMessage("ownership-transferred")
                .replace("{from}", from)
                .replace("{to}", to);
    }

    // Error messages
    public String getErrorOccurred() {
        return getMessage("error-occurred");
    }

    public String getDatabaseError() {
        return getMessage("database-error");
    }

    public String getContactAdmin() {
        return getMessage("contact-admin");
    }
}
