package com.mdevstudio.dailystreak.streak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mdevstudio.dailystreak.streak.StreakRules.AfterLastDay;
import org.junit.jupiter.api.Test;

class StreakRulesTest {

    private static final long MONDAY = 20_000;

    private final StreakRules week = new StreakRules(7, 0, AfterLastDay.RESTART);

    @Test
    void firstClaimGivesDayOne() {
        assertEquals(0, week.todayIndex(StreakState.EMPTY, MONDAY));
        assertEquals(DayStatus.AVAILABLE, week.status(0, StreakState.EMPTY, MONDAY));
        assertEquals(DayStatus.LOCKED, week.status(1, StreakState.EMPTY, MONDAY));

        StreakState claimed = week.claim(StreakState.EMPTY, MONDAY);
        assertEquals(new StreakState(1, MONDAY, 1), claimed);
        assertTrue(week.claimedToday(claimed, MONDAY));
    }

    @Test
    void claimedDayStaysOnTheSameIndexUntilTomorrow() {
        StreakState claimed = week.claim(StreakState.EMPTY, MONDAY);

        assertEquals(0, week.todayIndex(claimed, MONDAY));
        assertEquals(DayStatus.CLAIMED, week.status(0, claimed, MONDAY));
        assertEquals(DayStatus.TOMORROW, week.status(1, claimed, MONDAY));
        assertEquals(DayStatus.LOCKED, week.status(2, claimed, MONDAY));
    }

    @Test
    void nextDayContinuesTheStreak() {
        StreakState state = week.claim(StreakState.EMPTY, MONDAY);

        assertEquals(1, week.todayIndex(state, MONDAY + 1));
        assertFalse(week.streakLost(state, MONDAY + 1));
        assertEquals(2, week.claim(state, MONDAY + 1).streak());
    }

    @Test
    void missedDayStartsOver() {
        StreakState state = new StreakState(4, MONDAY, 4);

        assertTrue(week.streakLost(state, MONDAY + 2));
        assertEquals(0, week.todayIndex(state, MONDAY + 2));
        assertEquals(new StreakState(1, MONDAY + 2, 5), week.claim(state, MONDAY + 2));
    }

    @Test
    void graceDaysForgiveShortBreaks() {
        StreakRules lenient = new StreakRules(7, 1, AfterLastDay.RESTART);
        StreakState state = new StreakState(4, MONDAY, 4);

        assertEquals(4, lenient.activeStreak(state, MONDAY + 2));
        assertEquals(0, lenient.activeStreak(state, MONDAY + 3));
    }

    @Test
    void restartGoesBackToDayOneAfterTheLastDay() {
        StreakState fullWeek = new StreakState(7, MONDAY, 7);

        assertEquals(6, week.todayIndex(fullWeek, MONDAY));
        assertEquals(0, week.todayIndex(fullWeek, MONDAY + 1));
        assertEquals(DayStatus.AVAILABLE, week.status(0, fullWeek, MONDAY + 1));
        assertEquals(DayStatus.LOCKED, week.status(6, fullWeek, MONDAY + 1));
    }

    @Test
    void repeatLastKeepsGivingTheLastDay() {
        StreakRules repeat = new StreakRules(7, 0, AfterLastDay.REPEAT_LAST);
        StreakState longStreak = new StreakState(30, MONDAY, 30);

        assertEquals(6, repeat.todayIndex(longStreak, MONDAY + 1));
        assertEquals(DayStatus.CLAIMED, repeat.status(0, longStreak, MONDAY + 1));
        assertEquals(DayStatus.AVAILABLE, repeat.status(6, longStreak, MONDAY + 1));
    }

    @Test
    void resetStateBehavesLikeANewPlayer() {
        assertEquals(0, week.activeStreak(StreakState.EMPTY, MONDAY));
        assertFalse(week.streakLost(StreakState.EMPTY, MONDAY));
        assertFalse(week.claimedToday(StreakState.EMPTY, MONDAY));
    }
}
