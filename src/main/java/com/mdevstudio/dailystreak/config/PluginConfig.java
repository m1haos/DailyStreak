package com.mdevstudio.dailystreak.config;

import com.mdevstudio.dailystreak.streak.StreakRules.AfterLastDay;
import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

public record PluginConfig(
        String language,
        ZoneId timeZone,
        LocalTime resetTime,
        int graceDays,
        AfterLastDay afterLastDay,
        boolean joinReminder,
        Material claimedIcon,
        StorageSettings storage
) {

    private static final int VERSION = 1;
    private static final String DEFAULT_LANGUAGE = "en";
    private static final Material DEFAULT_CLAIMED_ICON = Material.LIME_STAINED_GLASS_PANE;

    public record StorageSettings(
            boolean mysql,
            String host,
            int port,
            String database,
            String username,
            String password,
            String tablePrefix
    ) {
    }

    public static PluginConfig load(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration yaml = plugin.getConfig();
        Logger logger = plugin.getSLF4JLogger();

        if (yaml.getInt("config-version") < VERSION) {
            // Admins keep their values; only keys added in newer versions are written in.
            yaml.options().copyDefaults(true);
            yaml.set("config-version", VERSION);
            plugin.saveConfig();
        }

        return new PluginConfig(
                language(yaml, logger),
                timeZone(yaml, logger),
                resetTime(yaml, logger),
                graceDays(yaml, logger),
                afterLastDay(yaml, logger),
                yaml.getBoolean("join-reminder", true),
                claimedIcon(yaml, logger),
                storage(yaml, logger));
    }

    private static String language(FileConfiguration yaml, Logger logger) {
        String language = yaml.getString("language", DEFAULT_LANGUAGE);
        if (!Messages.LANGUAGES.contains(language)) {
            logger.warn("config.yml: language '{}' is not available, expected one of {}. Using '{}'.",
                    language, Messages.LANGUAGES, DEFAULT_LANGUAGE);
            return DEFAULT_LANGUAGE;
        }
        return language;
    }

    private static ZoneId timeZone(FileConfiguration yaml, Logger logger) {
        String value = yaml.getString("reset.time-zone", "UTC");
        try {
            return ZoneId.of(value);
        } catch (DateTimeException e) {
            logger.warn("config.yml: reset.time-zone '{}' is not a time zone, e.g. Europe/Berlin. Using UTC.",
                    value);
            return ZoneId.of("UTC");
        }
    }

    private static LocalTime resetTime(FileConfiguration yaml, Logger logger) {
        String value = yaml.getString("reset.time", "00:00");
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            logger.warn("config.yml: reset.time '{}' should look like 00:00 or 18:30. Using 00:00.", value);
            return LocalTime.MIDNIGHT;
        }
    }

    private static int graceDays(FileConfiguration yaml, Logger logger) {
        int days = yaml.getInt("streak.grace-days", 0);
        if (days < 0) {
            logger.warn("config.yml: streak.grace-days can't be negative, got {}. Using 0.", days);
            return 0;
        }
        return days;
    }

    private static AfterLastDay afterLastDay(FileConfiguration yaml, Logger logger) {
        String value = yaml.getString("streak.after-last-day", "restart");
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "restart" -> AfterLastDay.RESTART;
            case "repeat-last" -> AfterLastDay.REPEAT_LAST;
            default -> {
                logger.warn("config.yml: streak.after-last-day '{}' should be restart or repeat-last. Using restart.",
                        value);
                yield AfterLastDay.RESTART;
            }
        };
    }

    private static Material claimedIcon(FileConfiguration yaml, Logger logger) {
        String value = yaml.getString("menu.claimed-icon", "");
        Material icon = Material.matchMaterial(value);
        if (icon == null || !icon.isItem() || icon.isAir()) {
            logger.warn("config.yml: menu.claimed-icon '{}' is not an item. Using {}.", value,
                    DEFAULT_CLAIMED_ICON.getKey().getKey());
            return DEFAULT_CLAIMED_ICON;
        }
        return icon;
    }

    private static StorageSettings storage(FileConfiguration yaml, Logger logger) {
        String type = yaml.getString("storage.type", "sqlite").toLowerCase(Locale.ROOT);
        if (!type.equals("sqlite") && !type.equals("mysql")) {
            logger.warn("config.yml: storage.type '{}' should be sqlite or mysql. Using sqlite.", type);
        }
        String prefix = yaml.getString("storage.table-prefix", "dailystreak_");
        if (!prefix.matches("[A-Za-z0-9_]*")) {
            logger.warn("config.yml: storage.table-prefix '{}' may only have letters, digits and _. "
                    + "Using dailystreak_.", prefix);
            prefix = "dailystreak_";
        }
        return new StorageSettings(
                type.equals("mysql"),
                yaml.getString("storage.mysql.host", "localhost"),
                yaml.getInt("storage.mysql.port", 3306),
                yaml.getString("storage.mysql.database", "minecraft"),
                yaml.getString("storage.mysql.username", "minecraft"),
                yaml.getString("storage.mysql.password", ""),
                prefix);
    }
}
