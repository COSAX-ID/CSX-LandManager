package dev.cosax.cSXLandManager.model;

import java.util.UUID;

/**
 * Represents a claim's data in the land management system.
 * Contains all information about rent, sale, and ownership status.
 */
public class ClaimData {
    private final long claimId;
    private UUID owner;
    private UUID originalOwner; // Stores original owner when claim is rented
    private UUID renter;
    private double rentPrice;
    private double sellPrice;
    private long rentDuration;
    private boolean autoRenew;
    private long rentStartTime;
    private long rentEndTime;
    private RentStatus status;

    /**
     * Creates a new ClaimData with minimal information.
     *
     * @param claimId The GriefPrevention claim ID
     * @param owner   The owner's UUID
     */
    public ClaimData(long claimId, UUID owner) {
        this.claimId = claimId;
        this.owner = owner;
        this.originalOwner = null; // No original owner when not rented
        this.renter = null;
        this.rentPrice = 0.0;
        this.sellPrice = 0.0;
        this.rentDuration = 0;
        this.autoRenew = false;
        this.rentStartTime = 0;
        this.rentEndTime = 0;
        this.status = RentStatus.PRIVATE;
    }

    /**
     * Full constructor for ClaimData.
     */
    public ClaimData(long claimId, UUID owner, UUID originalOwner, UUID renter, double rentPrice, double sellPrice,
                     long rentDuration, boolean autoRenew, long rentStartTime, long rentEndTime, RentStatus status) {
        this.claimId = claimId;
        this.owner = owner;
        this.originalOwner = originalOwner;
        this.renter = renter;
        this.rentPrice = rentPrice;
        this.sellPrice = sellPrice;
        this.rentDuration = rentDuration;
        this.autoRenew = autoRenew;
        this.rentStartTime = rentStartTime;
        this.rentEndTime = rentEndTime;
        this.status = status;
    }

    // Getters
    public long getClaimId() { return claimId; }
    public UUID getOwner() { return owner; }
    public UUID getOriginalOwner() { return originalOwner; }
    public UUID getRenter() { return renter; }
    public double getRentPrice() { return rentPrice; }
    public double getSellPrice() { return sellPrice; }
    public long getRentDuration() { return rentDuration; }
    public boolean isAutoRenew() { return autoRenew; }
    public long getRentStartTime() { return rentStartTime; }
    public long getRentEndTime() { return rentEndTime; }
    public RentStatus getStatus() { return status; }

    // Setters
    public void setOwner(UUID owner) { this.owner = owner; }
    public void setOriginalOwner(UUID originalOwner) { this.originalOwner = originalOwner; }
    public void setRenter(UUID renter) { this.renter = renter; }
    public void setRentPrice(double rentPrice) { this.rentPrice = rentPrice; }
    public void setSellPrice(double sellPrice) { this.sellPrice = sellPrice; }
    public void setRentDuration(long rentDuration) { this.rentDuration = rentDuration; }
    public void setAutoRenew(boolean autoRenew) { this.autoRenew = autoRenew; }
    public void setRentStartTime(long rentStartTime) { this.rentStartTime = rentStartTime; }
    public void setRentEndTime(long rentEndTime) { this.rentEndTime = rentEndTime; }
    public void setStatus(RentStatus status) { this.status = status; }

    /**
     * Checks if the claim is currently rented.
     */
    public boolean isRented() {
        return status == RentStatus.RENTED && renter != null;
    }

    /**
     * Checks if the claim is for sale.
     */
    public boolean isForSale() {
        return status == RentStatus.FOR_SALE && sellPrice > 0;
    }

    /**
     * Checks if the rent has expired.
     */
    public boolean isRentExpired() {
        return isRented() && System.currentTimeMillis() >= rentEndTime;
    }

    /**
     * Gets the remaining time for the current rent in milliseconds.
     * Returns 0 if not currently rented.
     */
    public long getRemainingRentTime() {
        if (!isRented()) return 0;
        long remaining = rentEndTime - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    /**
     * Checks if a player is the owner of this claim.
     */
    public boolean isOwner(UUID player) {
        return owner != null && owner.equals(player);
    }

    /**
     * Checks if this claim has an owner.
     */
    public boolean hasOwner() {
        return owner != null;
    }

    /**
     * Checks if a player is the current renter of this claim.
     */
    public boolean isRenter(UUID player) {
        return renter != null && renter.equals(player);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClaimData claimData = (ClaimData) o;
        return claimId == claimData.claimId;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(claimId);
    }

    @Override
    public String toString() {
        return "ClaimData{" +
                "claimId=" + claimId +
                ", owner=" + owner +
                ", renter=" + renter +
                ", status=" + status +
                ", rentPrice=" + rentPrice +
                ", sellPrice=" + sellPrice +
                '}';
    }
}
