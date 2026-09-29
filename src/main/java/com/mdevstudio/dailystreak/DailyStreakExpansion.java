package com.mdevstudio.dailystreak;

import com.mdevstudio.dailystreak.streak.StreakRules;
import com.mdevstudio.dailystreak.streak.StreakState;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

final class DailyStreakExpansion extends PlaceholderExpansion {

    private final DailyStreak plugin;

    DailyStreakExpansion(DailyStreak plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "dailystreak";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        StreakState state = player != null ? plugin.streaks().state(player.getUniqueId()) : null;
        if (state == null) {
            return "";
        }
        StreakRules rules = plugin.rules();
        long today = plugin.gameDay().today();
        return switch (params) {
            case "streak" -> String.valueOf(rules.activeStreak(state, today));
            case "total" -> String.valueOf(state.totalClaims());
            case "day" -> String.valueOf(rules.todayIndex(state, today) + 1);
            case "available" -> String.valueOf(!rules.claimedToday(state, today));
            case "next" -> plugin.messages().plainDuration(plugin.gameDay().untilNextDay());
            default -> null;
        };
    }
}
