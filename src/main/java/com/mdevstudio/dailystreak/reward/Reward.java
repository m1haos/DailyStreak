package com.mdevstudio.dailystreak.reward;

import org.bukkit.Material;

public sealed interface Reward {

    record Item(Material material, int amount) implements Reward {
    }

    record Money(double amount) implements Reward {
    }

    record Command(String command, String description) implements Reward {
    }
}
