package com.mdevstudio.dailystreak.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class Messages {

    public static final String RELOADED = "reloaded";
    public static final String PLAYERS_ONLY = "players-only";
    public static final String NOT_LOADED = "not-loaded";
    public static final String PLAYER_NOT_FOUND = "player-not-found";
    public static final String RESET = "reset";
    public static final String SET = "set";
    public static final String DAY_OUT_OF_RANGE = "day-out-of-range";
    public static final String JOIN_REMINDER = "join-reminder";
    public static final String CLAIMED = "claimed";
    public static final String ALREADY_CLAIMED = "already-claimed";
    public static final String STREAK_LOST = "streak-lost";
    public static final String INVENTORY_FULL = "inventory-full";

    public static final String MENU_TITLE = "menu.title";
    public static final String MENU_DAY = "menu.day";
    public static final String MENU_AVAILABLE = "menu.status-available";
    public static final String MENU_CLAIMED = "menu.status-claimed";
    public static final String MENU_TOMORROW = "menu.status-tomorrow";
    public static final String MENU_LOCKED = "menu.status-locked";
    public static final String MENU_REWARDS = "menu.rewards-header";
    public static final String MENU_REWARD_ITEM = "menu.reward-item";
    public static final String MENU_REWARD_MONEY = "menu.reward-money";
    public static final String MENU_REWARD_COMMAND = "menu.reward-command";
    public static final String MENU_INFO_NAME = "menu.info-name";
    public static final String MENU_INFO_STREAK = "menu.info-streak";
    public static final String MENU_INFO_TOTAL = "menu.info-total";
    public static final String MENU_INFO_NEXT = "menu.info-next";

    static final Set<String> LANGUAGES = Set.of("ru", "en");

    private static final String TIME = "time";
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final YamlConfiguration lines;
    private final TagResolver prefix;

    private Messages(YamlConfiguration lines) {
        this.lines = lines;
        this.prefix = Placeholder.parsed("prefix", line("prefix"));
    }

    public static Messages load(JavaPlugin plugin, String language) {
        for (String available : LANGUAGES) {
            String path = path(available);
            if (!new File(plugin.getDataFolder(), path).exists()) {
                plugin.saveResource(path, false);
            }
        }

        YamlConfiguration lines = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), path(language)));
        // Keys an admin deleted, or that appeared in an update, fall back to the bundled text.
        try (Reader bundled = new InputStreamReader(
                Objects.requireNonNull(plugin.getResource(path(language))), StandardCharsets.UTF_8)) {
            lines.setDefaults(YamlConfiguration.loadConfiguration(bundled));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new Messages(lines);
    }

    public void send(CommandSender recipient, String key, TagResolver... placeholders) {
        recipient.sendMessage(component(key, placeholders));
    }

    public Component component(String key, TagResolver... placeholders) {
        return MINI_MESSAGE.deserialize(line(key), prefix, TagResolver.resolver(placeholders));
    }

    /**
     * Same as {@link #component}, without the italic style that item names and lore get by default.
     */
    public Component itemText(String key, TagResolver... placeholders) {
        return component(key, placeholders).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public Component duration(Duration duration) {
        // Round up so that "0m" is never shown while there are still seconds left.
        long minutes = Math.ceilDiv(duration.toSeconds(), 60);
        return component(TIME,
                Placeholder.unparsed("hours", String.valueOf(minutes / 60)),
                Placeholder.unparsed("minutes", String.valueOf(minutes % 60)));
    }

    public String plainDuration(Duration duration) {
        return PlainTextComponentSerializer.plainText().serialize(duration(duration));
    }

    // getString(key, fallback) would skip the bundled defaults, so they are checked before falling back to the key.
    private String line(String key) {
        String line = lines.getString(key);
        return line != null ? line : key;
    }

    private static String path(String language) {
        return "lang/" + language + ".yml";
    }
}
