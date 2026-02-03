package dev.cosax.cSXLandManager.model;

/**
 * Enum representing the current status of a claim in the land management system.
 */
public enum RentStatus {
    /**
     * Claim is available for rent, not currently rented or for sale.
     */
    AVAILABLE,

    /**
     * Claim is currently being rented by a player.
     */
    RENTED,

    /**
     * Claim is listed for sale (permanent ownership transfer).
     */
    FOR_SALE,

    /**
     * Claim is private - not available for rent or sale.
     * This is the default state for claims not managed by the plugin.
     */
    PRIVATE
}
