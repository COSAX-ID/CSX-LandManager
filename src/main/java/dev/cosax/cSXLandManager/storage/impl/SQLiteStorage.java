package dev.cosax.cSXLandManager.storage.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.cosax.cSXLandManager.CSXLandManager;

import java.io.File;
import java.sql.SQLException;

/**
 * SQLite-based storage implementation.
 * Stores data in a SQLite database file in the plugin data folder.
 */
public class SQLiteStorage extends SQLStorage {

    public SQLiteStorage(CSXLandManager plugin) {
        super(plugin);
    }

    @Override
    protected String getJdbcUrl() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File databaseFile = new File(dataFolder, "claims.db");
        return "jdbc:sqlite:" + databaseFile.getAbsolutePath();
    }

    @Override
    protected javax.sql.DataSource createDataSource() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(getJdbcUrl());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1); // SQLite doesn't support multiple connections
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);

        return new HikariDataSource(config);
    }

    @Override
    protected String getStorageTypeName() {
        return "SQLite";
    }

    @Override
    protected java.util.Map<String, String> getColumnTypes() {
        java.util.Map<String, String> types = super.getColumnTypes();
        // SQLite uses INTEGER instead of BOOLEAN
        types.put("autoRenew", "INTEGER");
        return types;
    }
}
