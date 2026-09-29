package com.mdevstudio.dailystreak.streak;

public record StreakRules(int calendarLength, int graceDays, AfterLastDay afterLastDay) {

    public enum AfterLastDay {
        RESTART,
        REPEAT_LAST
    }

    public boolean claimedToday(StreakState state, long today) {
        return state.lastClaimDay() == today;
    }

    public int activeStreak(StreakState state, long today) {
        if (state.lastClaimDay() == StreakState.NEVER) {
            return 0;
        }
        return today - state.lastClaimDay() > graceDays + 1L ? 0 : state.streak();
    }

    public boolean streakLost(StreakState state, long today) {
        return state.streak() > 0 && activeStreak(state, today) == 0;
    }

    /**
     * Calendar day, counted from zero, that today's claim gives, or has already given.
     */
    public int todayIndex(StreakState state, long today) {
        int streak = activeStreak(state, today);
        return position(claimedToday(state, today) ? streak - 1 : streak);
    }

    public StreakState claim(StreakState state, long today) {
        return new StreakState(activeStreak(state, today) + 1, today, state.totalClaims() + 1);
    }

    public DayStatus status(int dayIndex, StreakState state, long today) {
        int current = todayIndex(state, today);
        boolean claimed = claimedToday(state, today);
        if (dayIndex < current || dayIndex == current && claimed) {
            return DayStatus.CLAIMED;
        }
        if (dayIndex == current) {
            return DayStatus.AVAILABLE;
        }
        return dayIndex == current + 1 && claimed ? DayStatus.TOMORROW : DayStatus.LOCKED;
    }

    private int position(int streak) {
        return switch (afterLastDay) {
            case RESTART -> streak % calendarLength;
            case REPEAT_LAST -> Math.min(streak, calendarLength - 1);
        };
    }
}
