package com.mdevstudio.dailystreak;

import com.mdevstudio.dailystreak.config.CalendarConfig;
import com.mdevstudio.dailystreak.config.Messages;
import com.mdevstudio.dailystreak.menu.CalendarMenu;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

final class DailyStreakCommand {

    private DailyStreakCommand() {
    }

    static void register(Commands registrar, DailyStreak plugin) {
        var root = Commands.literal("daily")
                .requires(source -> source.getSender().hasPermission(Permissions.DAILY))
                .executes(context -> open(plugin, context.getSource()))
                .then(Commands.literal("reload")
                        .requires(source -> source.getSender().hasPermission(Permissions.RELOAD))
                        .executes(context -> reload(plugin, context.getSource())))
                .then(Commands.literal("reset")
                        .requires(source -> source.getSender().hasPermission(Permissions.RESET))
                        .then(playerArgument(plugin)
                                .executes(context -> reset(plugin, context.getSource(),
                                        StringArgumentType.getString(context, "player")))))
                .then(Commands.literal("set")
                        .requires(source -> source.getSender().hasPermission(Permissions.SET))
                        .then(playerArgument(plugin)
                                .then(Commands.argument("day", IntegerArgumentType.integer(1, CalendarConfig.MAX_DAYS))
                                        .executes(context -> set(plugin, context.getSource(),
                                                StringArgumentType.getString(context, "player"),
                                                IntegerArgumentType.getInteger(context, "day"))))));

        registrar.register(root.build(), "Daily reward calendar", List.of());
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> playerArgument(DailyStreak plugin) {
        return Commands.argument("player", StringArgumentType.word())
                .suggests((context, builder) -> {
                    String typed = builder.getRemainingLowerCase();
                    plugin.getServer().getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(typed))
                            .forEach(builder::suggest);
                    return builder.buildFuture();
                });
    }

    private static int open(DailyStreak plugin, CommandSourceStack source) {
        if (source.getSender() instanceof Player player) {
            CalendarMenu.open(plugin, player);
        } else {
            plugin.messages().send(source.getSender(), Messages.PLAYERS_ONLY);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int reload(DailyStreak plugin, CommandSourceStack source) {
        plugin.reload();
        plugin.messages().send(source.getSender(), Messages.RELOADED);
        return Command.SINGLE_SUCCESS;
    }

    private static int reset(DailyStreak plugin, CommandSourceStack source, String name) {
        OfflinePlayer target = find(plugin, source, name);
        if (target == null) {
            return 0;
        }
        plugin.streaks().reset(target.getUniqueId());
        plugin.messages().send(source.getSender(), Messages.RESET,
                Placeholder.unparsed("player", nameOf(target, name)));
        return Command.SINGLE_SUCCESS;
    }

    private static int set(DailyStreak plugin, CommandSourceStack source, String name, int day) {
        int days = plugin.calendar().size();
        if (day > days) {
            plugin.messages().send(source.getSender(), Messages.DAY_OUT_OF_RANGE,
                    Placeholder.unparsed("days", String.valueOf(days)));
            return 0;
        }
        OfflinePlayer target = find(plugin, source, name);
        if (target == null) {
            return 0;
        }
        plugin.streaks().setDay(target.getUniqueId(), day - 1);
        plugin.messages().send(source.getSender(), Messages.SET,
                Placeholder.unparsed("player", nameOf(target, name)),
                Placeholder.unparsed("day", String.valueOf(day)));
        return Command.SINGLE_SUCCESS;
    }

    private static OfflinePlayer find(DailyStreak plugin, CommandSourceStack source, String name) {
        OfflinePlayer target = plugin.getServer().getOfflinePlayerIfCached(name);
        if (target == null) {
            plugin.messages().send(source.getSender(), Messages.PLAYER_NOT_FOUND, Placeholder.unparsed("player", name));
        }
        return target;
    }

    private static String nameOf(OfflinePlayer player, String typed) {
        return player.getName() != null ? player.getName() : typed;
    }
}
