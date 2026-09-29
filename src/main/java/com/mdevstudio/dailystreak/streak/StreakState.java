package com.mdevstudio.dailystreak.streak;

public record StreakState(int streak, long lastClaimDay, int totalClaims) {

    public static final long NEVER = -1;
    public static final StreakState EMPTY = new StreakState(0, NEVER, 0);
}
