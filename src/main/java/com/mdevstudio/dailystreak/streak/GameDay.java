package com.mdevstudio.dailystreak.streak;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class GameDay {

    private final Clock clock;
    private final ZoneId zone;
    private final LocalTime resetTime;

    public GameDay(Clock clock, ZoneId zone, LocalTime resetTime) {
        this.clock = clock;
        this.zone = zone;
        this.resetTime = resetTime;
    }

    public long today() {
        return shifted(now()).toLocalDate().toEpochDay();
    }

    public Duration untilNextDay() {
        ZonedDateTime now = now();
        ZonedDateTime next = shifted(now).toLocalDate().plusDays(1).atTime(resetTime).atZone(zone);
        return Duration.between(now, next);
    }

    private ZonedDateTime now() {
        return ZonedDateTime.now(clock).withZoneSameInstant(zone);
    }

    // Moving the clock back by the reset time makes the date change exactly at the reset moment.
    private ZonedDateTime shifted(ZonedDateTime time) {
        return time.minusSeconds(resetTime.toSecondOfDay());
    }
}
