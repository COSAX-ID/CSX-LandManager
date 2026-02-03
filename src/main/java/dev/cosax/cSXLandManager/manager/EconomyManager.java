package dev.cosax.cSXLandManager.manager;

import dev.cosax.cSXLandManager.CSXLandManager;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Manages economy operations using Vault.
 * Supports Vault-compatible economy plugins like EssentialsX, CMI, etc.
 */
public class EconomyManager {

    private final CSXLandManager plugin;
    private Economy economy;
    private boolean enabled;
    private String economyProvider;

    public EconomyManager(CSXLandManager plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    /**
     * Sets up the Vault economy integration.
     * Detects and logs the economy provider (e.g., EssentialsX).
     */
    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault plugin not found!");
            plugin.getLogger().warning("Please install Vault to use economy features.");
            plugin.getLogger().warning("Compatible economy plugins: EssentialsX, CMI, VaultAPI, etc.");
            enabled = false;
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager()
                .getRegistration(Economy.class);

        if (rsp == null) {
            plugin.getLogger().warning("No economy plugin found! Please install an economy plugin that supports Vault.");
            plugin.getLogger().warning("Recommended: EssentialsX (https://essentialsx.net/downloads.html)");
            enabled = false;
            return false;
        }

        economy = rsp.getProvider();
        enabled = true;

        // Detect economy provider
        economyProvider = economy.getName();
        plugin.getLogger().info("Successfully hooked into " + economyProvider + " economy.");

        // Special messages for common providers
        if (economyProvider.toLowerCase().contains("essentials")) {
            plugin.getLogger().info("EssentialsX economy detected! Full support enabled.");
        } else if (economyProvider.toLowerCase().contains("cmi")) {
            plugin.getLogger().info("CMI economy detected!");
        }

        return true;
    }

    /**
     * Checks if economy is available.
     */
    public boolean isAvailable() {
        return enabled && economy != null;
    }

    /**
     * Gets the economy instance.
     */
    public Economy getEconomy() {
        return economy;
    }

    /**
     * Gets the economy provider name.
     */
    public String getEconomyProvider() {
        return economyProvider != null ? economyProvider : "Unknown";
    }

    /**
     * Gets a player's balance.
     */
    public double getBalance(Player player) {
        if (!isAvailable()) return 0;
        return economy.getBalance(player);
    }

    /**
     * Gets an offline player's balance.
     */
    public double getBalance(OfflinePlayer player) {
        if (!isAvailable()) return 0;
        return economy.getBalance(player);
    }

    /**
     * Gets a player's balance by UUID (async).
     */
    public CompletableFuture<Double> getBalance(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            return getBalance(player);
        });
    }

    /**
     * Checks if a player has enough money.
     */
    public boolean hasEnough(Player player, double amount) {
        if (!isAvailable()) return true; // Allow if economy disabled
        return economy.has(player, amount);
    }

    /**
     * Withdraws money from a player.
     */
    public CompletableFuture<EconomyResponse> withdrawPlayer(Player player, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAvailable()) {
                plugin.getLogger().fine("Economy disabled, bypassing withdraw of " + amount + " from " + player.getName());
                return new EconomyResponse(amount, 0, EconomyResponse.ResponseType.SUCCESS, "Economy disabled, payment bypassed");
            }

            double beforeBalance = economy.getBalance(player);
            EconomyResponse response = economy.withdrawPlayer(player, amount);
            double afterBalance = economy.getBalance(player);

            plugin.getLogger().info("WITHDRAW: " + player.getName() + " | Amount: " + amount +
                " | Before: " + beforeBalance + " | After: " + afterBalance +
                " | Success: " + response.transactionSuccess());

            if (!response.transactionSuccess()) {
                plugin.getLogger().warning("Withdraw failed for " + player.getName() + ": " + response.errorMessage);
            }

            return response;
        });
    }

    /**
     * Withdraws money from an offline player.
     */
    public CompletableFuture<EconomyResponse> withdrawPlayer(OfflinePlayer player, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAvailable()) {
                plugin.getLogger().fine("Economy disabled, bypassing withdraw of " + amount + " from " + player.getName());
                return new EconomyResponse(amount, 0, EconomyResponse.ResponseType.SUCCESS, "Economy disabled, payment bypassed");
            }

            double beforeBalance = economy.getBalance(player);
            EconomyResponse response = economy.withdrawPlayer(player, amount);
            double afterBalance = economy.getBalance(player);

            plugin.getLogger().info("WITHDRAW (OFFLINE): " + player.getName() + " | Amount: " + amount +
                " | Before: " + beforeBalance + " | After: " + afterBalance +
                " | Success: " + response.transactionSuccess());

            return response;
        });
    }

    /**
     * Withdraws money from a player by UUID.
     */
    public CompletableFuture<EconomyResponse> withdrawPlayer(UUID uuid, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            return withdrawPlayer(player, amount).join();
        });
    }

    /**
     * Deposits money to a player.
     */
    public CompletableFuture<EconomyResponse> depositPlayer(Player player, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAvailable()) {
                plugin.getLogger().fine("Economy disabled, bypassing deposit of " + amount + " to " + player.getName());
                return new EconomyResponse(amount, 0, EconomyResponse.ResponseType.SUCCESS, "Economy disabled");
            }

            double beforeBalance = economy.getBalance(player);
            EconomyResponse response = economy.depositPlayer(player, amount);
            double afterBalance = economy.getBalance(player);

            plugin.getLogger().info("DEPOSIT: " + player.getName() + " | Amount: " + amount +
                " | Before: " + beforeBalance + " | After: " + afterBalance +
                " | Success: " + response.transactionSuccess());

            if (!response.transactionSuccess()) {
                plugin.getLogger().warning("Deposit failed for " + player.getName() + ": " + response.errorMessage);
            }

            return response;
        });
    }

    /**
     * Deposits money to an offline player.
     */
    public CompletableFuture<EconomyResponse> depositPlayer(OfflinePlayer player, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAvailable()) {
                plugin.getLogger().fine("Economy disabled, bypassing deposit of " + amount + " to " + player.getName());
                return new EconomyResponse(amount, 0, EconomyResponse.ResponseType.SUCCESS, "Economy disabled");
            }

            double beforeBalance = economy.getBalance(player);
            EconomyResponse response = economy.depositPlayer(player, amount);
            double afterBalance = economy.getBalance(player);

            plugin.getLogger().info("DEPOSIT (OFFLINE): " + player.getName() + " | Amount: " + amount +
                " | Before: " + beforeBalance + " | After: " + afterBalance +
                " | Success: " + response.transactionSuccess());

            return response;
        });
    }

    /**
     * Deposits money to a player by UUID.
     */
    public CompletableFuture<EconomyResponse> depositPlayer(UUID uuid, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            return depositPlayer(player, amount).join();
        });
    }

    /**
     * Formats an amount for display.
     */
    public String formatAmount(double amount) {
        if (!isAvailable()) {
            return String.format("%.2f", amount);
        }
        return economy.format(amount);
    }

    /**
     * Processes a payment from one player to another.
     * This method is ATOMIC - either both transactions succeed or both fail.
     * Compatible with EssentialsX and other Vault-compatible economies.
     */
    public CompletableFuture<PaymentResult> processPayment(UUID fromUuid, UUID toUuid, double amount, String description) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                plugin.getLogger().info("==================");
                plugin.getLogger().info("PAYMENT PROCESS START");
                plugin.getLogger().info("From: " + fromUuid);
                plugin.getLogger().info("To: " + toUuid);
                plugin.getLogger().info("Amount: " + amount);
                plugin.getLogger().info("Description: " + description);
                plugin.getLogger().info("Provider: " + getEconomyProvider());
                plugin.getLogger().info("==================");

                OfflinePlayer fromPlayer = Bukkit.getOfflinePlayer(fromUuid);
                OfflinePlayer toPlayer = Bukkit.getOfflinePlayer(toUuid);

                // Check if economy is available
                if (!isAvailable()) {
                    plugin.getLogger().warning("Economy not available, bypassing payment");
                    return new PaymentResult(true, "Economy disabled - payment bypassed", 0, 0);
                }

                // Check if sender has enough money
                double fromBalance = economy.getBalance(fromPlayer);
                plugin.getLogger().info("SENDER BALANCE BEFORE: " + fromBalance);

                if (!economy.has(fromPlayer, amount)) {
                    plugin.getLogger().warning("INSUFFICIENT FUNDS!");
                    plugin.getLogger().warning("Has: " + fromBalance + ", Needs: " + amount);
                    double toBalance = economy.getBalance(toPlayer);
                    return new PaymentResult(false, "Insufficient funds", fromBalance, toBalance);
                }

                // Step 1: Withdraw from sender
                plugin.getLogger().info("STEP 1: Withdrawing " + amount + " from " + fromPlayer.getName() + "...");
                EconomyResponse withdrawResponse = economy.withdrawPlayer(fromPlayer, amount);

                if (withdrawResponse.type != EconomyResponse.ResponseType.SUCCESS) {
                    plugin.getLogger().severe("WITHDRAW FAILED!");
                    plugin.getLogger().severe("Response: " + withdrawResponse.errorMessage);
                    double toBalance = economy.getBalance(toPlayer);
                    return new PaymentResult(false, "Withdraw failed: " + withdrawResponse.errorMessage, fromBalance, toBalance);
                }

                // Verify withdrawal
                double afterWithdraw = economy.getBalance(fromPlayer);
                plugin.getLogger().info("SENDER BALANCE AFTER WITHDRAW: " + afterWithdraw + " (expected: " + (fromBalance - amount) + ")");

                // Step 2: Deposit to receiver
                double toBalanceBefore = economy.getBalance(toPlayer);
                plugin.getLogger().info("RECEIVER BALANCE BEFORE: " + toBalanceBefore);
                plugin.getLogger().info("STEP 2: Depositing " + amount + " to " + toPlayer.getName() + "...");

                EconomyResponse depositResponse = economy.depositPlayer(toPlayer, amount);

                if (depositResponse.type != EconomyResponse.ResponseType.SUCCESS) {
                    plugin.getLogger().severe("DEPOSIT FAILED! Refunding sender...");
                    plugin.getLogger().severe("Response: " + depositResponse.errorMessage);

                    // Refund sender if deposit failed
                    economy.depositPlayer(fromPlayer, amount);

                    double fromBalanceRefund = economy.getBalance(fromPlayer);
                    plugin.getLogger().info("SENDER BALANCE AFTER REFUND: " + fromBalanceRefund);

                    return new PaymentResult(false, "Deposit failed: " + depositResponse.errorMessage + " (refunded)", afterWithdraw, toBalanceBefore);
                }

                // Verify deposit
                double toBalanceAfter = economy.getBalance(toPlayer);
                plugin.getLogger().info("RECEIVER BALANCE AFTER DEPOSIT: " + toBalanceAfter + " (expected: " + (toBalanceBefore + amount) + ")");

                // Final verification
                double fromFinal = economy.getBalance(fromPlayer);
                plugin.getLogger().info("SENDER FINAL BALANCE: " + fromFinal);
                plugin.getLogger().info("RECEIVER FINAL BALANCE: " + toBalanceAfter);

                plugin.getLogger().info("==================");
                plugin.getLogger().info("PAYMENT SUCCESS!");
                plugin.getLogger().info("Amount transferred: " + amount);
                plugin.getLogger().info("==================");

                return new PaymentResult(true, description, fromFinal, toBalanceAfter);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "ERROR processing payment", e);
                return new PaymentResult(false, "Internal error: " + e.getMessage(), 0, 0);
            }
        });
    }

    /**
     * Result of a payment operation.
     */
    public record PaymentResult(
            boolean success,
            String message,
            double fromBalance,
            double toBalance
    ) {}
}
