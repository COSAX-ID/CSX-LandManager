package dev.cosax.cSXLandManager.storage.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.cosax.cSXLandManager.config.Config;

/**
 * MySQL database-based storage implementation.
 * Connects to a remote MySQL database.
 */
public class MySQLStorage extends SQLStorage {

    private final Config config;

    public MySQLStorage(dev.cosax.cSXLandManager.CSXLandManager plugin, Config config) {
        super(plugin);
        this.config = config;
    }

    @Override
    protected String getJdbcUrl() {
        return String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true",
                config.getDbHost(),
                config.getDbPort(),
                config.getDbDatabase());
    }

    @Override
    protected javax.sql.DataSource createDataSource() throws Exception {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(getJdbcUrl());
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setUsername(config.getDbUsername());
        hikariConfig.setPassword(config.getDbPassword());
        hikariConfig.setMaximumPoolSize(config.getDbPoolSize());
        hikariConfig.setConnectionTimeout(config.getDbConnectionTimeout());
        hikariConfig.setMaxLifetime(config.getDbMaxLifetime());
        hikariConfig.setIdleTimeout(600000);

        // MySQL-specific optimizations
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
        hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
        hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
        hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
        hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
        hikariConfig.addDataSourceProperty("maintainTimeStats", "false");

        return new HikariDataSource(hikariConfig);
    }

    @Override
    protected String getStorageTypeName() {
        return "MySQL";
    }
}
