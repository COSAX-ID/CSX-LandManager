package dev.cosax.cSXLandManager.storage.impl;

import dev.cosax.cSXLandManager.CSXLandManager;
import dev.cosax.cSXLandManager.model.ClaimData;
import dev.cosax.cSXLandManager.model.RentStatus;
import dev.cosax.cSXLandManager.storage.StorageProvider;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * Abstract SQL-based storage implementation.
 * Provides common SQL operations for database implementations.
 */
public abstract class SQLStorage implements StorageProvider {

    protected final CSXLandManager plugin;
    protected DataSource dataSource;
    protected ExecutorService executorService;
    protected boolean ready = false;

    protected SQLStorage(CSXLandManager plugin) {
        this.plugin = plugin;
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "CSXLandManager-SQL");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Gets the JDBC URL for the database connection.
     */
    protected abstract String getJdbcUrl();

    /**
     * Gets the DataSource for database connections.
     */
    protected abstract DataSource createDataSource() throws Exception;

    /**
     * Gets the database-specific data types for SQL CREATE TABLE.
     */
    protected Map<String, String> getColumnTypes() {
        Map<String, String> types = new LinkedHashMap<>();
        types.put("claimId", "BIGINT PRIMARY KEY");
        types.put("owner", "CHAR(36)");
        types.put("originalOwner", "CHAR(36)");
        types.put("renter", "CHAR(36)");
        types.put("rentPrice", "DOUBLE");
        types.put("sellPrice", "DOUBLE");
        types.put("rentDuration", "BIGINT");
        types.put("autoRenew", "BOOLEAN");
        types.put("rentStartTime", "BIGINT");
        types.put("rentEndTime", "BIGINT");
        types.put("status", "VARCHAR(20)");
        return types;
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try {
                dataSource = createDataSource();
                createTables();
                ready = true;
                plugin.getLogger().info(getStorageTypeName() + " storage initialized successfully.");
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to initialize " + getStorageTypeName() + " storage", e);
                ready = false;
            }
        }, executorService);
    }

    @Override
    public CompletableFuture<Void> close() {
        return CompletableFuture.runAsync(() -> {
            ready = false;
            if (dataSource instanceof com.zaxxer.hikari.HikariDataSource) {
                ((com.zaxxer.hikari.HikariDataSource) dataSource).close();
            }
            executorService.shutdown();
            plugin.getLogger().info(getStorageTypeName() + " storage closed.");
        }, executorService);
    }

    @Override
    public CompletableFuture<Optional<ClaimData>> loadClaimData(long claimId) {
        return CompletableFuture.supplyAsync(() -> {
            if (!ready) return Optional.empty();

            String sql = "SELECT * FROM claims WHERE claimId = ?";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setLong(1, claimId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(resultSetToClaimData(rs));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load claim data for ID: " + claimId, e);
            }
            return Optional.empty();
        }, executorService);
    }

    @Override
    public CompletableFuture<Void> saveClaimData(ClaimData claimData) {
        return CompletableFuture.runAsync(() -> {
            if (!ready) return;

            try (Connection conn = dataSource.getConnection()) {
                // Check if originalOwner column exists
                boolean hasOriginalOwner = hasColumn(conn, "originalOwner");

                String sql;
                if (hasOriginalOwner) {
                    // New schema with originalOwner
                    sql = """
                        INSERT INTO claims (claimId, owner, originalOwner, renter, rentPrice, sellPrice, rentDuration,
                                           autoRenew, rentStartTime, rentEndTime, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            owner = VALUES(owner),
                            originalOwner = VALUES(originalOwner),
                            renter = VALUES(renter),
                            rentPrice = VALUES(rentPrice),
                            sellPrice = VALUES(sellPrice),
                            rentDuration = VALUES(rentDuration),
                            autoRenew = VALUES(autoRenew),
                            rentStartTime = VALUES(rentStartTime),
                            rentEndTime = VALUES(rentEndTime),
                            status = VALUES(status)
                        """;

                    // Use UPSERT syntax for SQLite
                    if (this instanceof SQLiteStorage) {
                        sql = """
                            INSERT INTO claims (claimId, owner, originalOwner, renter, rentPrice, sellPrice, rentDuration,
                                               autoRenew, rentStartTime, rentEndTime, status)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            ON CONFLICT(claimId) DO UPDATE SET
                                owner = excluded.owner,
                                originalOwner = excluded.originalOwner,
                                renter = excluded.renter,
                                rentPrice = excluded.rentPrice,
                                sellPrice = excluded.sellPrice,
                                rentDuration = excluded.rentDuration,
                                autoRenew = excluded.autoRenew,
                                rentStartTime = excluded.rentStartTime,
                                rentEndTime = excluded.rentEndTime,
                                status = excluded.status
                            """;
                    }
                } else {
                    // Old schema without originalOwner
                    sql = """
                        INSERT INTO claims (claimId, owner, renter, rentPrice, sellPrice, rentDuration,
                                           autoRenew, rentStartTime, rentEndTime, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            owner = VALUES(owner),
                            renter = VALUES(renter),
                            rentPrice = VALUES(rentPrice),
                            sellPrice = VALUES(sellPrice),
                            rentDuration = VALUES(rentDuration),
                            autoRenew = VALUES(autoRenew),
                            rentStartTime = VALUES(rentStartTime),
                            rentEndTime = VALUES(rentEndTime),
                            status = VALUES(status)
                        """;

                    // Use UPSERT syntax for SQLite
                    if (this instanceof SQLiteStorage) {
                        sql = """
                            INSERT INTO claims (claimId, owner, renter, rentPrice, sellPrice, rentDuration,
                                               autoRenew, rentStartTime, rentEndTime, status)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            ON CONFLICT(claimId) DO UPDATE SET
                                owner = excluded.owner,
                                renter = excluded.renter,
                                rentPrice = excluded.rentPrice,
                                sellPrice = excluded.sellPrice,
                                rentDuration = excluded.rentDuration,
                                autoRenew = excluded.autoRenew,
                                rentStartTime = excluded.rentStartTime,
                                rentEndTime = excluded.rentEndTime,
                                status = excluded.status
                            """;
                    }
                }

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    if (hasOriginalOwner) {
                        setClaimDataParameters(stmt, claimData);
                    } else {
                        setClaimDataParametersWithoutOriginalOwner(stmt, claimData);
                    }
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to save claim data for ID: " + claimData.getClaimId(), e);
            }
        }, executorService);
    }

    /**
     * Checks if a column exists in the claims table.
     */
    private boolean hasColumn(Connection conn, String columnName) {
        try {
            ResultSet rs = conn.getMetaData().getColumns(null, null, "CLAIMS", columnName.toUpperCase());
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().fine("Error checking for column " + columnName + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Sets PreparedStatement parameters from ClaimData (without originalOwner).
     * Used for backward compatibility with old databases.
     */
    protected void setClaimDataParametersWithoutOriginalOwner(PreparedStatement stmt, ClaimData data) throws SQLException {
        stmt.setLong(1, data.getClaimId());
        stmt.setString(2, data.getOwner() != null ? data.getOwner().toString() : null);
        stmt.setString(3, data.getRenter() != null ? data.getRenter().toString() : null);
        stmt.setDouble(4, data.getRentPrice());
        stmt.setDouble(5, data.getSellPrice());
        stmt.setLong(6, data.getRentDuration());
        stmt.setBoolean(7, data.isAutoRenew());
        stmt.setLong(8, data.getRentStartTime());
        stmt.setLong(9, data.getRentEndTime());
        stmt.setString(10, data.getStatus().name());
    }

    @Override
    public CompletableFuture<Void> deleteClaimData(long claimId) {
        return CompletableFuture.runAsync(() -> {
            if (!ready) return;

            String sql = "DELETE FROM claims WHERE claimId = ?";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setLong(1, claimId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to delete claim data for ID: " + claimId, e);
            }
        }, executorService);
    }

    @Override
    public CompletableFuture<List<ClaimData>> getAllClaims() {
        return CompletableFuture.supplyAsync(() -> {
            if (!ready) return Collections.emptyList();

            List<ClaimData> claims = new ArrayList<>();
            String sql = "SELECT * FROM claims";

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    claims.add(resultSetToClaimData(rs));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load all claims", e);
            }
            return claims;
        }, executorService);
    }

    @Override
    public CompletableFuture<List<ClaimData>> getClaimsByOwner(UUID owner) {
        return CompletableFuture.supplyAsync(() -> {
            if (!ready) return Collections.emptyList();

            List<ClaimData> claims = new ArrayList<>();
            String sql = "SELECT * FROM claims WHERE owner = ?";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, owner.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        claims.add(resultSetToClaimData(rs));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load claims for owner: " + owner, e);
            }
            return claims;
        }, executorService);
    }

    @Override
    public CompletableFuture<List<ClaimData>> getClaimsByRenter(UUID renter) {
        return CompletableFuture.supplyAsync(() -> {
            if (!ready) return Collections.emptyList();

            List<ClaimData> claims = new ArrayList<>();
            String sql = "SELECT * FROM claims WHERE renter = ?";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, renter.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        claims.add(resultSetToClaimData(rs));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load claims for renter: " + renter, e);
            }
            return claims;
        }, executorService);
    }

    @Override
    public CompletableFuture<List<ClaimData>> getRentedClaims() {
        return CompletableFuture.supplyAsync(() -> {
            if (!ready) return Collections.emptyList();

            List<ClaimData> claims = new ArrayList<>();
            String sql = "SELECT * FROM claims WHERE status = 'RENTED' AND renter IS NOT NULL";

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    claims.add(resultSetToClaimData(rs));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load rented claims", e);
            }
            return claims;
        }, executorService);
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    /**
     * Creates the database tables if they don't exist.
     */
    protected void createTables() throws SQLException {
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS claims (");

        Map<String, String> columnTypes = getColumnTypes();
        boolean first = true;
        for (Map.Entry<String, String> entry : columnTypes.entrySet()) {
            if (!first) {
                sql.append(", ");
            }
            sql.append(entry.getKey()).append(" ").append(entry.getValue());
            first = false;
        }
        sql.append(")");

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql.toString());
        }

        // Run database migrations to add new columns
        migrateDatabase();
    }

    /**
     * Migrates the database to add new columns if they don't exist.
     * This ensures backward compatibility with existing databases.
     */
    protected void migrateDatabase() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            // Check if originalOwner column exists
            boolean columnExists = false;
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, "CLAIMS", "ORIGINALOWNER")) {
                columnExists = rs.next();
            } catch (Exception e) {
                // Some databases might throw exception instead of returning empty result
                plugin.getLogger().fine("Error checking for originalOwner column: " + e.getMessage());
            }

            if (!columnExists) {
                plugin.getLogger().info("Migrating database: Adding originalOwner column...");
                try (Statement stmt = conn.createStatement()) {
                    // Add originalOwner column
                    String alterSql = "ALTER TABLE claims ADD COLUMN originalOwner CHAR(36)";
                    stmt.execute(alterSql);
                    plugin.getLogger().info("Successfully added originalOwner column to claims table");
                } catch (SQLException e) {
                    // Column might already exist (concurrent modification) or other error
                    if (!e.getMessage().contains("already exists") && !e.getMessage().contains("duplicate")) {
                        plugin.getLogger().warning("Failed to add originalOwner column: " + e.getMessage());
                        // Don't throw - allow plugin to continue
                    }
                }
            }
        }
    }

    /**
     * Sets PreparedStatement parameters from ClaimData.
     */
    protected void setClaimDataParameters(PreparedStatement stmt, ClaimData data) throws SQLException {
        stmt.setLong(1, data.getClaimId());
        stmt.setString(2, data.getOwner() != null ? data.getOwner().toString() : null);
        stmt.setString(3, data.getOriginalOwner() != null ? data.getOriginalOwner().toString() : null);
        stmt.setString(4, data.getRenter() != null ? data.getRenter().toString() : null);
        stmt.setDouble(5, data.getRentPrice());
        stmt.setDouble(6, data.getSellPrice());
        stmt.setLong(7, data.getRentDuration());
        stmt.setBoolean(8, data.isAutoRenew());
        stmt.setLong(9, data.getRentStartTime());
        stmt.setLong(10, data.getRentEndTime());
        stmt.setString(11, data.getStatus().name());
    }

    /**
     * Converts a ResultSet row to ClaimData.
     */
    protected ClaimData resultSetToClaimData(ResultSet rs) throws SQLException {
        long claimId = rs.getLong("claimId");
        UUID owner = rs.getString("owner") != null ? UUID.fromString(rs.getString("owner")) : null;
        UUID originalOwner = null;
        try {
            originalOwner = rs.getString("originalOwner") != null ? UUID.fromString(rs.getString("originalOwner")) : null;
        } catch (SQLException e) {
            // Column might not exist in older databases
            plugin.getLogger().fine("originalOwner column not found, using null");
        }
        UUID renter = rs.getString("renter") != null ? UUID.fromString(rs.getString("renter")) : null;
        double rentPrice = rs.getDouble("rentPrice");
        double sellPrice = rs.getDouble("sellPrice");
        long rentDuration = rs.getLong("rentDuration");
        boolean autoRenew = rs.getBoolean("autoRenew");
        long rentStartTime = rs.getLong("rentStartTime");
        long rentEndTime = rs.getLong("rentEndTime");
        RentStatus status = RentStatus.valueOf(rs.getString("status"));

        return new ClaimData(claimId, owner, originalOwner, renter, rentPrice, sellPrice,
                rentDuration, autoRenew, rentStartTime, rentEndTime, status);
    }

    /**
     * Gets the storage type name for logging.
     */
    protected abstract String getStorageTypeName();
}
