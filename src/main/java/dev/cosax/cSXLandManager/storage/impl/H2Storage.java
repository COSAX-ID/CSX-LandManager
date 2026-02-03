package dev.cosax.cSXLandManager.storage.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.cosax.cSXLandManager.CSXLandManager;

import java.io.File;

/**
 * H2 database-based storage implementation.
 * Stores data in an H2 database file in the plugin data folder.
 */
public class H2Storage extends SQLStorage {

    public H2Storage(CSXLandManager plugin) {
        super(plugin);
    }

    @Override
    protected String getJdbcUrl() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File databaseFile = new File(dataFolder, "claims");
        return "jdbc:h2:file:" + databaseFile.getAbsolutePath() + ";MODE=MySQL";
    }

    @Override
    protected javax.sql.DataSource createDataSource() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(getJdbcUrl());
        config.setDriverClassName("org.h2.Driver");
        config.setMaximumPoolSize(5);
        config.setConnectionTimeout(30000);
        config.setMaxLifetime(1800000);
        config.setIdleTimeout(600000);

        return new HikariDataSource(config);
    }

    @Override
    protected String getStorageTypeName() {
        return "H2";
    }
}
