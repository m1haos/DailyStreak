package com.mdevstudio.dailystreak.config;

import com.mdevstudio.dailystreak.reward.CalendarDay;
import com.mdevstudio.dailystreak.reward.Reward;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

public final class CalendarConfig {

    public static final int MAX_DAYS = 28;

    private static final String FILE = "rewards.yml";

    private final Logger logger;

    private CalendarConfig(Logger logger) {
        this.logger = logger;
    }

    public static List<CalendarDay> load(JavaPlugin plugin) {
        if (!new File(plugin.getDataFolder(), FILE).exists()) {
            plugin.saveResource(FILE, false);
        }
        CalendarConfig reader = new CalendarConfig(plugin.getSLF4JLogger());
        File file = new File(plugin.getDataFolder(), FILE);
        List<CalendarDay> days = reader.read(YamlConfiguration.loadConfiguration(file));
        if (!days.isEmpty()) {
            return days;
        }

        reader.logger.error("{} has no days, falling back to the default calendar until the file is fixed.", FILE);
        try (Reader bundled = new InputStreamReader(
                Objects.requireNonNull(plugin.getResource(FILE)), StandardCharsets.UTF_8)) {
            return reader.read(YamlConfiguration.loadConfiguration(bundled));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<CalendarDay> read(YamlConfiguration yaml) {
        List<Map<?, ?>> entries = yaml.getMapList("days");
        if (entries.size() > MAX_DAYS) {
            logger.warn("{}: {} days found, only the first {} are used.", FILE, entries.size(), MAX_DAYS);
            entries = entries.subList(0, MAX_DAYS);
        }

        List<CalendarDay> days = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            days.add(day(i + 1, entries.get(i)));
        }
        return days;
    }

    private CalendarDay day(int number, Map<?, ?> entry) {
        String where = "day " + number;
        Material icon = material(entry.get("icon"), where + ", icon");
        List<Reward> rewards = new ArrayList<>();

        if (entry.get("rewards") instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) {
                String rewardWhere = where + ", reward " + (i + 1);
                Reward reward = list.get(i) instanceof Map<?, ?> map ? reward(map, rewardWhere) : null;
                if (reward != null) {
                    rewards.add(reward);
                }
            }
        }
        if (rewards.isEmpty()) {
            logger.warn("{}: {} has no rewards, players will get nothing that day.", FILE, where);
        }
        return new CalendarDay(icon != null ? icon : Material.CHEST, List.copyOf(rewards));
    }

    private Reward reward(Map<?, ?> entry, String where) {
        String type = String.valueOf(entry.get("type"));
        return switch (type) {
            case "item" -> {
                Material material = material(entry.get("material"), where);
                int amount = positive(number(entry.get("amount"), 1, where).intValue(), where);
                yield material != null && amount > 0 ? new Reward.Item(material, amount) : null;
            }
            case "money" -> {
                double amount = number(entry.get("amount"), 0, where).doubleValue();
                if (amount <= 0) {
                    logger.warn("{}: {} has money amount {}, it should be above zero. Skipped.", FILE, where, amount);
                    yield null;
                }
                yield new Reward.Money(amount);
            }
            case "command" -> {
                Object command = entry.get("command");
                if (command == null) {
                    logger.warn("{}: {} has no command, skipped.", FILE, where);
                    yield null;
                }
                Object description = entry.get("description");
                yield new Reward.Command(command.toString(), description != null ? description.toString() : "");
            }
            default -> {
                logger.warn("{}: {} has type '{}', expected item, money or command. Skipped.", FILE, where, type);
                yield null;
            }
        };
    }

    private Material material(Object value, String where) {
        Material material = value != null ? Material.matchMaterial(value.toString()) : null;
        if (material == null || !material.isItem() || material.isAir()) {
            logger.warn("{}: {} has '{}', which is not an item.", FILE, where, value);
            return null;
        }
        return material;
    }

    private int positive(int amount, String where) {
        if (amount <= 0) {
            logger.warn("{}: {} has amount {}, it should be at least 1. Skipped.", FILE, where, amount);
        }
        return amount;
    }

    private Number number(Object value, Number fallback, String where) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number;
        }
        logger.warn("{}: {} has amount '{}', which is not a number. Using {}.", FILE, where, value, fallback);
        return fallback;
    }
}
