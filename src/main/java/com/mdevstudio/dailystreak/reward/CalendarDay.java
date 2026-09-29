package com.mdevstudio.dailystreak.reward;

import java.util.List;
import org.bukkit.Material;

public record CalendarDay(Material icon, List<Reward> rewards) {
}
