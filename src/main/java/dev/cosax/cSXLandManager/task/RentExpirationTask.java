package dev.cosax.cSXLandManager.task;

import dev.cosax.cSXLandManager.CSXLandManager;
import dev.cosax.cSXLandManager.config.Config;
import dev.cosax.cSXLandManager.manager.RentManager;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Periodic task that checks for expired rents and processes them.
 */
public class RentExpirationTask implements Runnable {

    private final CSXLandManager plugin;
    private final Config config;
    private final RentManager rentManager;
    private BukkitTask task;

    public RentExpirationTask(CSXLandManager plugin, Config config, RentManager rentManager) {
        this.plugin = plugin;
        this.config = config;
        this.rentManager = rentManager;
    }

    /**
     * Starts the periodic task.
     */
    public void start() {
        long intervalTicks = config.getExpirationCheckInterval();
        task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin,
                this,
                intervalTicks,
                intervalTicks
        );
        plugin.getLogger().info("Rent expiration task started (runs every " +
                (intervalTicks / 20) + " seconds)");
    }

    /**
     * Stops the periodic task.
     */
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
            plugin.getLogger().info("Rent expiration task stopped");
        }
    }

    @Override
    public void run() {
        if (config.isDebug()) {
            plugin.getLogger().info("Checking for expired rents...");
        }

        rentManager.processAllExpiredRents().thenAccept(expiredCount -> {
            if (expiredCount > 0) {
                plugin.getLogger().info("Processed " + expiredCount + " expired rent(s)");
            } else if (config.isDebug()) {
                plugin.getLogger().info("No expired rents found");
            }
        }).exceptionally(e -> {
            plugin.getLogger().log(Level.SEVERE, "Error checking for expired rents", e);
            return null;
        });
    }

    /**
     * Checks if the task is currently running.
     */
    public boolean isRunning() {
        return task != null && !task.isCancelled();
    }
}
