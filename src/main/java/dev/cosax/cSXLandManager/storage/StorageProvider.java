package dev.cosax.cSXLandManager.storage;

import dev.cosax.cSXLandManager.model.ClaimData;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for storage providers.
 * Implementations can store claim data in various formats (YAML, SQL databases, etc.)
 */
public interface StorageProvider {

    /**
     * Initializes the storage provider.
     * Creates necessary tables, files, or connections.
     *
     * @return CompletableFuture that completes when initialization is done
     */
    CompletableFuture<Void> initialize();

    /**
     * Closes the storage provider.
     * Saves pending data and closes connections.
     *
     * @return CompletableFuture that completes when cleanup is done
     */
    CompletableFuture<Void> close();

    /**
     * Loads claim data by claim ID.
     *
     * @param claimId The GriefPrevention claim ID
     * @return CompletableFuture containing Optional with ClaimData, or empty if not found
     */
    CompletableFuture<Optional<ClaimData>> loadClaimData(long claimId);

    /**
     * Saves claim data.
     *
     * @param claimData The claim data to save
     * @return CompletableFuture that completes when save is done
     */
    CompletableFuture<Void> saveClaimData(ClaimData claimData);

    /**
     * Deletes claim data.
     *
     * @param claimId The claim ID to delete
     * @return CompletableFuture that completes when deletion is done
     */
    CompletableFuture<Void> deleteClaimData(long claimId);

    /**
     * Gets all stored claims.
     *
     * @return CompletableFuture containing list of all ClaimData
     */
    CompletableFuture<List<ClaimData>> getAllClaims();

    /**
     * Gets claims by owner UUID.
     *
     * @param owner The owner's UUID
     * @return CompletableFuture containing list of owner's claims
     */
    CompletableFuture<List<ClaimData>> getClaimsByOwner(UUID owner);

    /**
     * Gets claims by renter UUID.
     *
     * @param renter The renter's UUID
     * @return CompletableFuture containing list of claims rented by player
     */
    CompletableFuture<List<ClaimData>> getClaimsByRenter(UUID renter);

    /**
     * Gets all claims with active rents.
     *
     * @return CompletableFuture containing list of rented claims
     */
    CompletableFuture<List<ClaimData>> getRentedClaims();

    /**
     * Checks if storage is ready.
     *
     * @return true if storage is initialized and ready
     */
    boolean isReady();
}
