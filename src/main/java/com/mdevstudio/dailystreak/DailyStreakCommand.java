package com.mdevstudio.dailystreak;

import com.mdevstudio.dailystreak.config.Messages;
import com.mdevstudio.dailystreak.menu.CalendarMenu;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
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
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    String typed = builder.getRemainingLowerCase();
                                    plugin.getServer().getOnlinePlayers().stream()
                                            .map(Player::getName)
                                            .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(typed))
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> reset(plugin, context.getSource(),
                                        StringArgumentType.getString(context, "player")))));

        registrar.register(root.build(), "Daily reward calendar", List.of());
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
        OfflinePlayer target = plugin.getServer().getOfflinePlayerIfCached(name);
        if (target == null) {
            plugin.messages().send(source.getSender(), Messages.PLAYER_NOT_FOUND, Placeholder.unparsed("player", name));
            return 0;
        }
        plugin.streaks().reset(target.getUniqueId());
        plugin.messages().send(source.getSender(), Messages.RESET,
                Placeholder.unparsed("player", target.getName() != null ? target.getName() : name));
        return Command.SINGLE_SUCCESS;
    }
}
