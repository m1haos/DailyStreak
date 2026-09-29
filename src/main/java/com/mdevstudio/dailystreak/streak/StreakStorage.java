package com.mdevstudio.dailystreak.streak;

import com.mdevstudio.dailystreak.config.PluginConfig.StorageSettings;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

public final class StreakStorage {

    private final HikariDataSource source;
    private final Logger logger;
    private final String table;
    private final String upsert;
    // A single thread keeps writes for one player in the order they were made.
    private final ExecutorService worker = Executors.newSingleThreadExecutor(
            task -> new Thread(task, "DailyStreak Storage"));

    private StreakStorage(HikariDataSource source, Logger logger, String table, boolean mysql) {
        this.source = source;
        this.logger = logger;
        this.table = table;
        String insert = "INSERT INTO " + table + " (uuid, streak, last_claim_day, total_claims) VALUES (?, ?, ?, ?) ";
        this.upsert = insert + (mysql
                ? "ON DUPLICATE KEY UPDATE streak = VALUES(streak), last_claim_day = VALUES(last_claim_day), "
                        + "total_claims = VALUES(total_claims)"
                : "ON CONFLICT(uuid) DO UPDATE SET streak = excluded.streak, "
                        + "last_claim_day = excluded.last_claim_day, total_claims = excluded.total_claims");
    }

    public static StreakStorage open(StorageSettings settings, Path dataFolder, Logger logger) throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setPoolName("DailyStreak");
        if (settings.mysql()) {
            config.setDriverClassName("org.mariadb.jdbc.Driver");
            config.setJdbcUrl("jdbc:mariadb://" + settings.host() + ":" + settings.port() + "/" + settings.database());
            config.setUsername(settings.username());
            config.setPassword(settings.password());
            config.setMaximumPoolSize(4);
        } else {
            config.setDriverClassName("org.sqlite.JDBC");
            config.setJdbcUrl("jdbc:sqlite:" + dataFolder.resolve("data.db"));
            // SQLite allows one writer at a time, more connections would only wait on each other.
            config.setMaximumPoolSize(1);
        }

        HikariDataSource source;
        try {
            source = new HikariDataSource(config);
        } catch (RuntimeException e) {
            throw new SQLException(e.getMessage(), e);
        }

        StreakStorage storage = new StreakStorage(source, logger, settings.tablePrefix() + "players", settings.mysql());
        try {
            storage.createTable();
        } catch (SQLException e) {
            source.close();
            throw e;
        }
        return storage;
    }

    /**
     * Reads a player's state, blocking the calling thread. Never call it on the main thread.
     */
    public StreakState load(UUID player) throws SQLException {
        try (Connection connection = source.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT streak, last_claim_day, total_claims FROM " + table + " WHERE uuid = ?")) {
            statement.setString(1, player.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return StreakState.EMPTY;
                }
                return new StreakState(result.getInt(1), result.getLong(2), result.getInt(3));
            }
        }
    }

    public CompletableFuture<StreakState> loadAsync(UUID player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return load(player);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, worker);
    }

    public void save(UUID player, StreakState state) {
        worker.execute(() -> {
            try (Connection connection = source.getConnection();
                 PreparedStatement statement = connection.prepareStatement(upsert)) {
                statement.setString(1, player.toString());
                statement.setInt(2, state.streak());
                statement.setLong(3, state.lastClaimDay());
                statement.setInt(4, state.totalClaims());
                statement.executeUpdate();
            } catch (SQLException e) {
                logger.error("Could not save the streak of {}", player, e);
            }
        });
    }

    public void delete(UUID player) {
        worker.execute(() -> {
            try (Connection connection = source.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "DELETE FROM " + table + " WHERE uuid = ?")) {
                statement.setString(1, player.toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                logger.error("Could not reset the streak of {}", player, e);
            }
        });
    }

    /**
     * Finishes pending writes and closes the connection pool.
     */
    public void close() {
        worker.shutdown();
        try {
            if (!worker.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warn("Some streaks were not saved within 10 seconds of shutdown");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        source.close();
    }

    private void createTable() throws SQLException {
        try (Connection connection = source.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + table + " ("
                    + "uuid CHAR(36) NOT NULL PRIMARY KEY, "
                    + "streak INT NOT NULL, "
                    + "last_claim_day BIGINT NOT NULL, "
                    + "total_claims INT NOT NULL)");
        }
    }
}
