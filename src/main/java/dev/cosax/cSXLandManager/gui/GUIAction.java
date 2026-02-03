package dev.cosax.cSXLandManager.gui;

/**
 * Represents possible actions in the land management GUI.
 */
public enum GUIAction {
    /**
     * Rent the claim (start a new rent)
     */
    RENT,

    /**
     * Buy the claim (permanent ownership transfer)
     */
    BUY,

    /**
     * Extend an existing rent
     */
    EXTEND,

    /**
     * Toggle auto-renew on/off
     */
    TOGGLE_AUTO_RENEW,

    /**
     * Set rent price (claim owner)
     */
    SET_RENT_PRICE,

    /**
     * Set sell price / list for sale (claim owner)
     */
    SET_SELL_PRICE,

    /**
     * Remove from sale (claim owner)
     */
    REMOVE_FROM_SALE,

    /**
     * Cancel current rent (admin)
     */
    CANCEL_RENT,

    /**
     * Toggle rent availability
     */
    TOGGLE_RENT,

    /**
     * Toggle sale availability
     */
    TOGGLE_SALE,

    /**
     * Refresh the GUI
     */
    REFRESH,

    /**
     * Close the GUI
     */
    CLOSE
}
