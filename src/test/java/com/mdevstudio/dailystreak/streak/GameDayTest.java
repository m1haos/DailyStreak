package com.mdevstudio.dailystreak.streak;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class GameDayTest {

    private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");

    @Test
    void dayChangesAtMidnightInTheConfiguredZone() {
        // 20:59 UTC is 23:59 in Moscow, one minute later it is the next day there.
        GameDay before = at("2026-03-10T20:59:00Z", MOSCOW, LocalTime.MIDNIGHT);
        GameDay after = at("2026-03-10T21:00:00Z", MOSCOW, LocalTime.MIDNIGHT);

        assertEquals(epochDay("2026-03-10"), before.today());
        assertEquals(epochDay("2026-03-11"), after.today());
    }

    @Test
    void dayChangesAtTheResetTime() {
        LocalTime sixAm = LocalTime.of(6, 0);

        assertEquals(epochDay("2026-03-09"), at("2026-03-10T05:59:00Z", ZoneOffset.UTC, sixAm).today());
        assertEquals(epochDay("2026-03-10"), at("2026-03-10T06:00:00Z", ZoneOffset.UTC, sixAm).today());
    }

    @Test
    void untilNextDayCountsToTheResetTime() {
        GameDay evening = at("2026-03-10T18:30:00Z", ZoneOffset.UTC, LocalTime.MIDNIGHT);
        GameDay earlyMorning = at("2026-03-10T05:00:00Z", ZoneOffset.UTC, LocalTime.of(6, 0));

        assertEquals(Duration.ofMinutes(5 * 60 + 30), evening.untilNextDay());
        assertEquals(Duration.ofHours(1), earlyMorning.untilNextDay());
    }

    @Test
    void daylightSavingDayIsShorter() {
        // Berlin moves clocks forward on 2026-03-29, so that day has 23 hours.
        GameDay night = at("2026-03-28T23:00:00Z", ZoneId.of("Europe/Berlin"), LocalTime.MIDNIGHT);

        assertEquals(Duration.ofHours(23), night.untilNextDay());
    }

    private static GameDay at(String instant, ZoneId zone, LocalTime resetTime) {
        return new GameDay(Clock.fixed(Instant.parse(instant), ZoneOffset.UTC), zone, resetTime);
    }

    private static long epochDay(String date) {
        return LocalDate.parse(date).toEpochDay();
    }
}
