package com.jmbross.payroll.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

public final class DatabaseManager implements AutoCloseable {
    private final HikariDataSource dataSource;

    public DatabaseManager(DatabaseSettings settings) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(settings.jdbcUrl());
        config.setUsername(settings.user());
        config.setPassword(settings.password());
        config.setMaximumPoolSize(8);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10_000);
        config.setPoolName("payroll-pool");
        this.dataSource = new HikariDataSource(config);
    }

    public void migrate() {
        Flyway.configure().dataSource(dataSource).cleanDisabled(true).load().migrate();
    }

    public DataSource dataSource() {
        return dataSource;
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
