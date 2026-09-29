package com.mdevstudio.dailystreak.streak;

import com.mdevstudio.dailystreak.DailyStreak;
import com.mdevstudio.dailystreak.config.Messages;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class StreakService implements Listener {

    private final Map<UUID, StreakState> online = new ConcurrentHashMap<>();
    private final DailyStreak plugin;
    private final StreakStorage storage;

    public StreakService(DailyStreak plugin, StreakStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }
        try {
            online.put(event.getUniqueId(), storage.load(event.getUniqueId()));
        } catch (SQLException e) {
            plugin.getSLF4JLogger().error("Could not load the streak of {}", event.getName(), e);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        StreakState state = online.get(event.getPlayer().getUniqueId());
        if (state != null) {
            remind(event.getPlayer(), state);
        } else {
            loadLater(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        online.remove(event.getPlayer().getUniqueId());
    }

    public void loadOnline() {
        plugin.getServer().getOnlinePlayers().forEach(this::loadLater);
    }

    /**
     * Current state of an online player, or {@code null} while it is still being loaded.
     */
    public StreakState state(UUID player) {
        return online.get(player);
    }

    /**
     * Claims today's reward. Must run on the main thread, which also makes a double click claim only once.
     *
     * @return {@code true} if the reward was given
     */
    public boolean claim(Player player) {
        Messages messages = plugin.messages();
        StreakState state = online.get(player.getUniqueId());
        if (state == null) {
            messages.send(player, Messages.NOT_LOADED);
            return false;
        }

        StreakRules rules = plugin.rules();
        long today = plugin.gameDay().today();
        if (rules.claimedToday(state, today)) {
            messages.send(player, Messages.ALREADY_CLAIMED,
                    Placeholder.component("time", messages.duration(plugin.gameDay().untilNextDay())));
            return false;
        }
        if (rules.streakLost(state, today)) {
            messages.send(player, Messages.STREAK_LOST);
        }

        int dayIndex = rules.todayIndex(state, today);
        StreakState claimed = rules.claim(state, today);
        online.put(player.getUniqueId(), claimed);
        storage.save(player.getUniqueId(), claimed);

        boolean dropped = plugin.rewardGiver().give(player, plugin.calendar().get(dayIndex).rewards());
        messages.send(player, Messages.CLAIMED,
                Placeholder.unparsed("day", String.valueOf(dayIndex + 1)),
                Placeholder.unparsed("streak", String.valueOf(claimed.streak())));
        if (dropped) {
            messages.send(player, Messages.INVENTORY_FULL);
        }
        return true;
    }

    public void reset(UUID player) {
        online.computeIfPresent(player, (id, state) -> StreakState.EMPTY);
        storage.delete(player);
    }

    /**
     * Makes the given calendar day, counted from zero, the one the player can claim right now.
     */
    public void setDay(UUID player, int dayIndex) {
        long yesterday = plugin.gameDay().today() - 1;
        StreakState current = online.get(player);
        if (current != null) {
            StreakState updated = new StreakState(dayIndex, yesterday, current.totalClaims());
            online.put(player, updated);
            storage.save(player, updated);
            return;
        }
        storage.loadAsync(player)
                .thenAccept(state -> storage.save(player, new StreakState(dayIndex, yesterday, state.totalClaims())))
                .exceptionally(error -> {
                    plugin.getSLF4JLogger().error("Could not change the streak of {}", player, error);
                    return null;
                });
    }

    private void loadLater(Player player) {
        UUID id = player.getUniqueId();
        storage.loadAsync(id).whenComplete((state, error) -> {
            if (error != null) {
                plugin.getSLF4JLogger().error("Could not load the streak of {}", player.getName(), error);
                return;
            }
            if (plugin.isEnabled()) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        online.put(id, state);
                        remind(player, state);
                    }
                });
            }
        });
    }

    private void remind(Player player, StreakState state) {
        if (plugin.settings().joinReminder() && !plugin.rules().claimedToday(state, plugin.gameDay().today())) {
            plugin.messages().send(player, Messages.JOIN_REMINDER);
        }
    }
}
