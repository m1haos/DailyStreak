package com.mdevstudio.dailystreak.streak;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mdevstudio.dailystreak.config.PluginConfig.StorageSettings;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StreakStorageTest {

    private static final StorageSettings SQLITE =
            new StorageSettings(false, "", 0, "", "", "", "dailystreak_");
    private static final Logger LOGGER = LoggerFactory.getLogger(StreakStorageTest.class);

    @TempDir
    Path folder;

    @Test
    void unknownPlayerStartsEmpty() throws SQLException {
        StreakStorage storage = StreakStorage.open(SQLITE, folder, LOGGER);

        assertEquals(StreakState.EMPTY, storage.load(UUID.randomUUID()));
        storage.close();
    }

    @Test
    void savedStateSurvivesReopening() throws SQLException {
        UUID player = UUID.randomUUID();
        StreakStorage storage = StreakStorage.open(SQLITE, folder, LOGGER);
        storage.save(player, new StreakState(3, 20_000, 3));
        storage.save(player, new StreakState(4, 20_001, 4));
        storage.close();

        StreakStorage reopened = StreakStorage.open(SQLITE, folder, LOGGER);
        assertEquals(new StreakState(4, 20_001, 4), reopened.load(player));
        reopened.close();
    }

    @Test
    void deleteForgetsThePlayer() throws SQLException {
        UUID player = UUID.randomUUID();
        StreakStorage storage = StreakStorage.open(SQLITE, folder, LOGGER);
        storage.save(player, new StreakState(2, 20_000, 2));
        storage.delete(player);
        storage.close();

        StreakStorage reopened = StreakStorage.open(SQLITE, folder, LOGGER);
        assertEquals(StreakState.EMPTY, reopened.load(player));
        reopened.close();
    }
}
