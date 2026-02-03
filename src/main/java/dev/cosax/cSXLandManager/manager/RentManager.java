package dev.cosax.cSXLandManager.manager;

import dev.cosax.cSXLandManager.CSXLandManager;
import dev.cosax.cSXLandManager.config.Config;
import dev.cosax.cSXLandManager.config.Messages;
import dev.cosax.cSXLandManager.model.ClaimData;
import dev.cosax.cSXLandManager.model.RentStatus;
import dev.cosax.cSXLandManager.storage.StorageManager;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Manages rent operations for claims.
 */
public class RentManager {

    private final CSXLandManager plugin;
    private final Config config;
    private final Messages messages;
    private final ClaimManager claimManager;
    private final EconomyManager economyManager;
    private final StorageManager storageManager;
    private final GUIManager guiManager;

    public RentManager(CSXLandManager plugin, Config config, Messages messages,
                       ClaimManager claimManager, EconomyManager economyManager,
                       StorageManager storageManager) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.claimManager = claimManager;
        this.economyManager = economyManager;
        this.storageManager = storageManager;
        this.guiManager = plugin.getGUIManager();
    }

    /**
     * Starts a rent for a claim.
     */
    public CompletableFuture<RentResult> startRent(Player player, ClaimData claimData, long durationMillis) {
        // Validate duration
        if (durationMillis < config.getMinRentDuration()) {
            return CompletableFuture.completedFuture(
                new RentResult(false, "Rent duration too short", null)
            );
        }

        if (config.getMaxRentDuration() > 0 && durationMillis > config.getMaxRentDuration()) {
            return CompletableFuture.completedFuture(
                new RentResult(false, "Rent duration too long", null)
            );
        }

        // Check if claim is available
        if (claimData.getStatus() != RentStatus.AVAILABLE && claimData.getStatus() != RentStatus.PRIVATE) {
            return CompletableFuture.completedFuture(
                new RentResult(false, "Claim not available for rent", null)
            );
        }

        // Check price
        double price = claimData.getRentPrice();
        if (economyManager.isAvailable() && !player.hasPermission("landmgmt.bypass")) {
            double balance = economyManager.getBalance(player);
            if (balance < price) {
                return CompletableFuture.completedFuture(
                    new RentResult(false, messages.getInsufficientFunds(price, balance), null)
                );
            }
        }

        Claim claim = claimManager.getClaimById(claimData.getClaimId());
        if (claim == null) {
            return CompletableFuture.completedFuture(new RentResult(false, messages.getClaimNotFound(), null));
        }

        // Process payment ATOMICALLY using processPayment
        if (economyManager.isAvailable() && price > 0 && !player.hasPermission("landmgmt.bypass")) {
            if (claimData.getOwner() == null) {
                return CompletableFuture.completedFuture(new RentResult(false, "Cannot rent: No owner found", null));
            }

            // Use processPayment for ATOMIC transaction (withdraw + deposit)
            return economyManager.processPayment(
                    player.getUniqueId(),
                    claimData.getOwner(),
                    price,
                    "Rent payment for claim " + claimData.getClaimId()
                ).thenCompose(paymentResult -> {
                    if (!paymentResult.success()) {
                        plugin.getLogger().warning("Payment failed: " + paymentResult.message());
                        return CompletableFuture.completedFuture(new RentResult(false, messages.getRentFailed(paymentResult.message()), null));
                    }

                    plugin.getLogger().info("Payment successful! Processing rent...");

                    // Calculate timestamps
                    long startTime = System.currentTimeMillis();
                    long endTime = startTime + durationMillis;

                    // Store original owner before transfer
                    UUID originalOwner = claimData.getOwner();
                    claimData.setOriginalOwner(originalOwner);

                    // Transfer ownership to renter
                    return claimManager.transferOwnership(claim, player.getUniqueId())
                        .thenCompose(transferSuccess -> {
                            if (!transferSuccess) {
                                plugin.getLogger().severe("Failed to transfer ownership to renter!");
                                // Refund the payment
                                return economyManager.depositPlayer(player.getUniqueId(), price)
                                    .thenApply(v -> new RentResult(false, "Failed to transfer ownership, payment refunded", null));
                            }

                            plugin.getLogger().info("Ownership transferred to renter: " + player.getUniqueId());

                            // Update claim data
                            claimData.setOwner(player.getUniqueId());
                            claimData.setRenter(player.getUniqueId());
                            claimData.setRentDuration(durationMillis);
                            claimData.setRentStartTime(startTime);
                            claimData.setRentEndTime(endTime);
                            claimData.setStatus(RentStatus.RENTED);
                            claimData.setAutoRenew(config.isAutoRenewEnabled());

                            return storageManager.saveClaimData(claimData)
                                .thenApply(v -> {
                                    plugin.getLogger().info("Rent started successfully for claim " + claimData.getClaimId());

                                    // Notify the original owner that their claim has been rented
                                    if (originalOwner != null) {
                                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                                            org.bukkit.OfflinePlayer owner = Bukkit.getOfflinePlayer(originalOwner);
                                            if (owner.isOnline()) {
                                                Player ownerPlayer = owner.getPlayer();
                                                if (ownerPlayer != null && ownerPlayer.isOnline()) {
                                                    ownerPlayer.sendMessage("");
                                                    ownerPlayer.sendMessage("§b§l=== LAND RENTED ===");
                                                    ownerPlayer.sendMessage("§aYour claim has been rented by a player!");
                                                    ownerPlayer.sendMessage("§7Location: Claim #" + claimData.getClaimId());
                                                    ownerPlayer.sendMessage("§7Renter: §e" + player.getName());
                                                    ownerPlayer.sendMessage("§7Duration: §e" + messages.formatDuration(durationMillis));
                                                    ownerPlayer.sendMessage("§7You earned: §e" + economyManager.formatAmount(price));
                                                    ownerPlayer.sendMessage("§7Auto-renew: " + (config.isAutoRenewEnabled() ? "§aEnabled" : "§cDisabled"));
                                                    ownerPlayer.sendMessage("§b§l===================");
                                                    guiManager.playRentSuccessSound(ownerPlayer);
                                                }
                                            }
                                        });
                                    }

                                    return new RentResult(true, messages.getRentSuccess(messages.formatDuration(durationMillis)), claimData);
                                });
                        });
                })
                .exceptionally(e -> {
                    plugin.getLogger().log(Level.SEVERE, "Error starting rent with payment", e);
                    return new RentResult(false, messages.getErrorOccurred(), null);
                });
        }

        // No payment needed (admin or price = 0)
        plugin.getLogger().info("No payment needed (admin or price=0), starting rent...");

        // Calculate timestamps
        long startTime = System.currentTimeMillis();
        long endTime = startTime + durationMillis;

        // Store original owner before transfer
        UUID originalOwner = claimData.getOwner();
        claimData.setOriginalOwner(originalOwner);

        // Transfer ownership to renter
        return claimManager.transferOwnership(claim, player.getUniqueId())
            .thenCompose(transferSuccess -> {
                if (!transferSuccess) {
                    plugin.getLogger().severe("Failed to transfer ownership to renter!");
                    return CompletableFuture.completedFuture(new RentResult(false, "Failed to transfer ownership", null));
                }

                plugin.getLogger().info("Ownership transferred to renter: " + player.getUniqueId());

                // Update claim data
                claimData.setOwner(player.getUniqueId());
                claimData.setRenter(player.getUniqueId());
                claimData.setRentDuration(durationMillis);
                claimData.setRentStartTime(startTime);
                claimData.setRentEndTime(endTime);
                claimData.setStatus(RentStatus.RENTED);
                claimData.setAutoRenew(config.isAutoRenewEnabled());

                return storageManager.saveClaimData(claimData)
                    .thenApply(v -> {
                        plugin.getLogger().info("Rent started successfully for claim (no payment) " + claimData.getClaimId());

                        // Notify the original owner that their claim has been rented
                        if (originalOwner != null) {
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                org.bukkit.OfflinePlayer owner = Bukkit.getOfflinePlayer(originalOwner);
                                if (owner.isOnline()) {
                                    Player ownerPlayer = owner.getPlayer();
                                    if (ownerPlayer != null && ownerPlayer.isOnline()) {
                                        ownerPlayer.sendMessage("");
                                        ownerPlayer.sendMessage("§b§l=== LAND RENTED ===");
                                        ownerPlayer.sendMessage("§aYour claim has been rented!");
                                        ownerPlayer.sendMessage("§7Location: Claim #" + claimData.getClaimId());
                                        ownerPlayer.sendMessage("§7Renter: §e" + player.getName());
                                        ownerPlayer.sendMessage("§7Duration: §e" + messages.formatDuration(durationMillis));
                                        ownerPlayer.sendMessage("§7Price: §cFREE (Admin/Bypass)");
                                        ownerPlayer.sendMessage("§b§l===================");
                                        guiManager.playRentSuccessSound(ownerPlayer);
                                    }
                                }
                            });
                        }

                        return new RentResult(true, messages.getRentSuccess(messages.formatDuration(durationMillis)), claimData);
                    });
            })
            .exceptionally(e -> {
                plugin.getLogger().log(Level.SEVERE, "Error starting rent without payment", e);
                return new RentResult(false, messages.getErrorOccurred(), null);
            });
    }

    /**
     * Extends an existing rent.
     */
    public CompletableFuture<RentResult> extendRent(Player player, ClaimData claimData, long additionalDuration) {
        // Check if player is the renter
        if (!claimData.isRenter(player.getUniqueId())) {
            return CompletableFuture.completedFuture(
                new RentResult(false, messages.getNoActiveRent(), null)
            );
        }

        // Check if rent has expired
        if (claimData.isRentExpired()) {
            return CompletableFuture.completedFuture(
                new RentResult(false, messages.getRentExpired(), null)
            );
        }

        // Calculate price
        double price = claimData.getRentPrice();
        if (economyManager.isAvailable() && !player.hasPermission("landmgmt.bypass")) {
            double balance = economyManager.getBalance(player);
            if (balance < price) {
                return CompletableFuture.completedFuture(
                    new RentResult(false, messages.getInsufficientFunds(price, balance), null)
                );
            }
        }

        Claim claim = claimManager.getClaimById(claimData.getClaimId());
        if (claim == null) {
            return CompletableFuture.completedFuture(new RentResult(false, messages.getClaimNotFound(), null));
        }

        // Process payment ATOMICALLY using processPayment
        if (economyManager.isAvailable() && price > 0 && !player.hasPermission("landmgmt.bypass")) {
            if (claimData.getOwner() == null) {
                return CompletableFuture.completedFuture(new RentResult(false, "Cannot extend: No owner found", null));
            }

            // Use processPayment for ATOMIC transaction (withdraw + deposit)
            return economyManager.processPayment(
                    player.getUniqueId(),
                    claimData.getOwner(),
                    price,
                    "Rent extension for claim " + claimData.getClaimId()
                ).thenCompose(paymentResult -> {
                    if (!paymentResult.success()) {
                        plugin.getLogger().warning("Extension payment failed: " + paymentResult.message());
                        return CompletableFuture.completedFuture(new RentResult(false, messages.getRentExtendFailed(paymentResult.message()), null));
                    }

                    plugin.getLogger().info("Extension payment successful! Processing...");

                    // Extend rent time
                    long newEndTime = claimData.getRentEndTime() + additionalDuration;
                    claimData.setRentEndTime(newEndTime);

                    return storageManager.saveClaimData(claimData)
                        .thenApply(v -> {
                            plugin.getLogger().info("Rent extended successfully for claim " + claimData.getClaimId());
                            return new RentResult(true, messages.getRentExtended(messages.formatDuration(additionalDuration)), claimData);
                        });
                })
                .exceptionally(e -> {
                    plugin.getLogger().log(Level.SEVERE, "Error extending rent with payment", e);
                    return new RentResult(false, messages.getErrorOccurred(), null);
                });
        }

        // No payment needed (admin or price = 0)
        plugin.getLogger().info("No payment needed (admin or price=0), extending rent...");

        // Extend rent time
        long newEndTime = claimData.getRentEndTime() + additionalDuration;
        claimData.setRentEndTime(newEndTime);

        return storageManager.saveClaimData(claimData)
            .thenApply(v -> new RentResult(true, messages.getRentExtended(messages.formatDuration(additionalDuration)), claimData))
            .exceptionally(e -> {
                plugin.getLogger().log(Level.SEVERE, "Error extending rent without payment", e);
                return new RentResult(false, messages.getErrorOccurred(), null);
            });
    }

    /**
     * Ends a rent (admin or owner).
     */
    public CompletableFuture<RentResult> endRent(Player admin, ClaimData claimData, boolean isAdmin) {
        return CompletableFuture.supplyAsync(() -> {
            if (!claimData.isRented()) {
                return CompletableFuture.completedFuture(new RentResult(false, messages.getNoActiveRent(), null));
            }

            Claim claim = claimManager.getClaimById(claimData.getClaimId());
            if (claim == null) {
                return CompletableFuture.completedFuture(new RentResult(false, messages.getClaimNotFound(), null));
            }

            UUID renterUuid = claimData.getRenter();
            Player renter = Bukkit.getPlayer(renterUuid);
            UUID originalOwner = claimData.getOriginalOwner();

            // Transfer ownership back to original owner
            if (originalOwner != null) {
                return claimManager.transferOwnership(claim, originalOwner)
                    .thenCompose(transferSuccess -> {
                        if (!transferSuccess) {
                            plugin.getLogger().warning("Failed to transfer ownership back to original owner: " + originalOwner);
                        }

                        plugin.getLogger().info("Ownership transferred back to original owner: " + originalOwner);

                        // Reset claim data
                        claimData.setOwner(originalOwner);
                        claimData.setOriginalOwner(null);
                        claimData.setRenter(null);
                        claimData.setRentDuration(0);
                        claimData.setRentStartTime(0);
                        claimData.setRentEndTime(0);
                        claimData.setAutoRenew(false);
                        claimData.setStatus(RentStatus.AVAILABLE);

                        // Save to storage
                        return storageManager.saveClaimData(claimData);
                    })
                    .thenApply(saved -> {
                        // Notify renter if online
                        if (renter != null && renter.isOnline()) {
                            renter.sendMessage(messages.getRentCancelled());
                        }

                        String message = isAdmin ? messages.getRentCancelledAdmin() : messages.getRentCancelledOwner();
                        return new RentResult(true, message, claimData);
                    })
                    .exceptionally(e -> {
                        plugin.getLogger().log(Level.SEVERE, "Error ending rent", e);
                        return new RentResult(false, messages.getErrorOccurred(), null);
                    });
            } else {
                // No original owner stored, just reset data
                claimData.setRenter(null);
                claimData.setRentDuration(0);
                claimData.setRentStartTime(0);
                claimData.setRentEndTime(0);
                claimData.setAutoRenew(false);
                claimData.setStatus(RentStatus.AVAILABLE);

                return storageManager.saveClaimData(claimData)
                    .thenApply(saved -> {
                        if (renter != null && renter.isOnline()) {
                            renter.sendMessage(messages.getRentCancelled());
                        }

                        String message = isAdmin ? messages.getRentCancelledAdmin() : messages.getRentCancelledOwner();
                        return new RentResult(true, message, claimData);
                    })
                    .exceptionally(e -> {
                        plugin.getLogger().log(Level.SEVERE, "Error ending rent", e);
                        return new RentResult(false, messages.getErrorOccurred(), null);
                    });
            }
        }).thenCompose(future -> future); // Flatten the nested CompletableFuture
    }

    /**
     * Processes rent expiration for a single claim.
     */
    public CompletableFuture<Boolean> processRentExpiration(ClaimData claimData) {
        return CompletableFuture.supplyAsync(() -> {
            if (!claimData.isRented()) {
                return CompletableFuture.completedFuture(false);
            }

            if (!claimData.isRentExpired()) {
                return CompletableFuture.completedFuture(false);
            }

            Claim claim = claimManager.getClaimById(claimData.getClaimId());
            if (claim == null) {
                return CompletableFuture.completedFuture(false);
            }

            // Attempt auto-renew if enabled
            if (claimData.isAutoRenew() && config.isAutoRenewEnabled()) {
                UUID renterUuid = claimData.getRenter();
                UUID ownerUuid = claimData.getOriginalOwner();
                double price = claimData.getRentPrice();

                // Check if owner exists (originalOwner)
                if (ownerUuid == null) {
                    plugin.getLogger().warning("Cannot auto-renew: No original owner found for claim " + claimData.getClaimId());
                    claimData.setAutoRenew(false);
                    // Continue to expiration handling
                } else {
                    // Process auto-renewal using processPayment to ensure money goes to owner
                    return economyManager.processPayment(
                            renterUuid,
                            ownerUuid,
                            price,
                            "Auto-renew rent for claim " + claimData.getClaimId()
                        ).thenCompose(paymentResult -> {
                            if (paymentResult.success()) {
                                // Payment successful, extend rent
                                long newEndTime = System.currentTimeMillis() + claimData.getRentDuration();
                                claimData.setRentEndTime(newEndTime);

                                plugin.getLogger().info("Auto-renew payment successful for claim " + claimData.getClaimId() +
                                    " - " + price + " from " + renterUuid + " to " + ownerUuid);

                                return storageManager.saveClaimData(claimData)
                                    .thenApply(v -> {
                                        // Notify renter if online
                                        Player renter = Bukkit.getPlayer(renterUuid);
                                        if (renter != null && renter.isOnline()) {
                                            renter.sendMessage(messages.getRentExtended(messages.formatDuration(claimData.getRentDuration())));
                                        }

                                        plugin.getLogger().info("Auto-renewed rent for claim " + claimData.getClaimId() + " for player " + renterUuid);
                                        return true;
                                    });
                            } else {
                                // Payment failed, disable auto-renew
                                plugin.getLogger().info("Auto-renew payment failed for claim " + claimData.getClaimId() + ": " + paymentResult.message());
                                claimData.setAutoRenew(false);
                                return storageManager.saveClaimData(claimData)
                                    .thenApply(v -> {
                                        Player renter = Bukkit.getPlayer(renterUuid);
                                        if (renter != null && renter.isOnline()) {
                                            renter.sendMessage(messages.getAutoRenewDisabledLowFunds());
                                        }
                                        return false; // Continue to expiration
                                    });
                            }
                        })
                        .thenCompose(autoRenewed -> {
                            if (autoRenewed) {
                                return CompletableFuture.completedFuture(true);
                            }

                            // Rent has expired and wasn't auto-renewed
                            UUID originalOwner = claimData.getOriginalOwner();

                            // Transfer ownership back to original owner
                            if (originalOwner != null) {
                                return claimManager.transferOwnership(claim, originalOwner)
                                    .thenCompose(transferSuccess -> {
                                        if (!transferSuccess) {
                                            plugin.getLogger().warning("Failed to transfer ownership back to original owner on expiration: " + originalOwner);
                                        } else {
                                            plugin.getLogger().info("Ownership transferred back to original owner on expiration: " + originalOwner);
                                        }

                                        // Reset claim data
                                        claimData.setOwner(originalOwner);
                                        claimData.setOriginalOwner(null);
                                        claimData.setRenter(null);
                                        claimData.setRentDuration(0);
                                        claimData.setRentStartTime(0);
                                        claimData.setRentEndTime(0);
                                        claimData.setAutoRenew(false);
                                        claimData.setStatus(RentStatus.AVAILABLE);

                                        return storageManager.saveClaimData(claimData);
                                    })
                                    .thenApply(v -> {
                                        // Notify ex-renter if online
                                        Player renter = Bukkit.getPlayer(renterUuid);
                                        if (renter != null && renter.isOnline()) {
                                            renter.sendMessage(messages.getRentExpired());
                                        }

                                        plugin.getLogger().info("Rent expired for claim " + claimData.getClaimId());
                                        return true;
                                    });
                            } else {
                                // No original owner stored, just reset data
                                claimData.setRenter(null);
                                claimData.setRentDuration(0);
                                claimData.setRentStartTime(0);
                                claimData.setRentEndTime(0);
                                claimData.setAutoRenew(false);
                                claimData.setStatus(RentStatus.AVAILABLE);

                                return storageManager.saveClaimData(claimData)
                                    .thenApply(v -> {
                                        // Notify ex-renter if online
                                        Player renter = Bukkit.getPlayer(renterUuid);
                                        if (renter != null && renter.isOnline()) {
                                            renter.sendMessage(messages.getRentExpired());
                                        }

                                        plugin.getLogger().info("Rent expired for claim " + claimData.getClaimId());
                                        return true;
                                    });
                            }
                        })
                        .exceptionally(e -> {
                            plugin.getLogger().log(Level.SEVERE, "Error processing auto-renew for claim " + claimData.getClaimId(), e);
                            return false;
                        });
                }
            }

            // Rent has expired and auto-renew is not enabled
            UUID renterUuid = claimData.getRenter();
            UUID originalOwner = claimData.getOriginalOwner();

            // Transfer ownership back to original owner
            if (originalOwner != null) {
                return claimManager.transferOwnership(claim, originalOwner)
                    .thenCompose(transferSuccess -> {
                        if (!transferSuccess) {
                            plugin.getLogger().warning("Failed to transfer ownership back to original owner on expiration: " + originalOwner);
                        } else {
                            plugin.getLogger().info("Ownership transferred back to original owner on expiration: " + originalOwner);
                        }

                        // Reset claim data
                        claimData.setOwner(originalOwner);
                        claimData.setOriginalOwner(null);
                        claimData.setRenter(null);
                        claimData.setRentDuration(0);
                        claimData.setRentStartTime(0);
                        claimData.setRentEndTime(0);
                        claimData.setAutoRenew(false);
                        claimData.setStatus(RentStatus.AVAILABLE);

                        return storageManager.saveClaimData(claimData);
                    })
                    .thenApply(v -> {
                        // Notify ex-renter if online
                        Player renter = Bukkit.getPlayer(renterUuid);
                        if (renter != null && renter.isOnline()) {
                            renter.sendMessage(messages.getRentExpired());
                        }

                        plugin.getLogger().info("Rent expired for claim " + claimData.getClaimId());
                        return true;
                    })
                    .exceptionally(e -> {
                        plugin.getLogger().log(Level.SEVERE, "Error processing rent expiration for claim " + claimData.getClaimId(), e);
                        return false;
                    });
            } else {
                // No original owner stored, just reset data
                claimData.setRenter(null);
                claimData.setRentDuration(0);
                claimData.setRentStartTime(0);
                claimData.setRentEndTime(0);
                claimData.setAutoRenew(false);
                claimData.setStatus(RentStatus.AVAILABLE);

                return storageManager.saveClaimData(claimData)
                    .thenApply(v -> {
                        // Notify ex-renter if online
                        Player renter = Bukkit.getPlayer(renterUuid);
                        if (renter != null && renter.isOnline()) {
                            renter.sendMessage(messages.getRentExpired());
                        }

                        plugin.getLogger().info("Rent expired for claim " + claimData.getClaimId());
                        return true;
                    })
                    .exceptionally(e -> {
                        plugin.getLogger().log(Level.SEVERE, "Error processing rent expiration for claim " + claimData.getClaimId(), e);
                        return false;
                    });
            }
        }).thenCompose(future -> future); // Flatten the nested CompletableFuture
    }

    /**
     * Processes all expired rents.
     */
    public CompletableFuture<Integer> processAllExpiredRents() {
        return storageManager.getRentedClaims().thenCompose(rentedClaims -> {
            if (rentedClaims.isEmpty()) {
                return CompletableFuture.completedFuture(0);
            }

            List<CompletableFuture<Boolean>> futures = rentedClaims.stream()
                    .filter(ClaimData::isRentExpired)
                    .map(this::processRentExpiration)
                    .toList();

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> (int) futures.stream().mapToLong(f -> f.join() ? 1 : 0).sum());
        });
    }

    /**
     * Toggles auto-renew for a claim.
     */
    public CompletableFuture<Boolean> toggleAutoRenew(Player player, ClaimData claimData) {
        if (!claimData.isRenter(player.getUniqueId())) {
            return CompletableFuture.completedFuture(false);
        }

        boolean newState = !claimData.isAutoRenew();
        claimData.setAutoRenew(newState);
        return storageManager.saveClaimData(claimData)
            .thenApply(v -> newState);
    }

    /**
     * Result of a rent operation.
     */
    public record RentResult(
            boolean success,
            String message,
            ClaimData claimData
    ) {}
}
