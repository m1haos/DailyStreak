package com.mdevstudio.dailystreak;

import com.mdevstudio.dailystreak.config.CalendarConfig;
import com.mdevstudio.dailystreak.config.Messages;
import com.mdevstudio.dailystreak.config.PluginConfig;
import com.mdevstudio.dailystreak.menu.CalendarMenu;
import com.mdevstudio.dailystreak.menu.MenuListener;
import com.mdevstudio.dailystreak.reward.CalendarDay;
import com.mdevstudio.dailystreak.reward.Reward;
import com.mdevstudio.dailystreak.reward.RewardGiver;
import com.mdevstudio.dailystreak.streak.GameDay;
import com.mdevstudio.dailystreak.streak.StreakRules;
import com.mdevstudio.dailystreak.streak.StreakService;
import com.mdevstudio.dailystreak.streak.StreakStorage;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.sql.SQLException;
import java.time.Clock;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.java.JavaPlugin;

public class DailyStreak extends JavaPlugin {

    private PluginConfig settings;
    private Messages messages;
    private List<CalendarDay> calendar;
    private GameDay gameDay;
    private RewardGiver rewardGiver;
    private StreakStorage storage;
    private StreakService streaks;

    @Override
    public void onEnable() {
        reload();
        rewardGiver = new RewardGiver(getServer(), getSLF4JLogger());

        try {
            storage = StreakStorage.open(settings.storage(), getDataFolder().toPath(), getSLF4JLogger());
        } catch (SQLException e) {
            getSLF4JLogger().error("Could not open the {} database, DailyStreak stays disabled: {}",
                    settings.storage().mysql() ? "MySQL" : "SQLite", e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        streaks = new StreakService(this, storage);
        getServer().getPluginManager().registerEvents(streaks, this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        streaks.loadOnline();

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS,
                event -> DailyStreakCommand.register(event.registrar(), this));
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new DailyStreakExpansion(this).register();
        }
        // Economy plugins may register with Vault after this plugin, so check once the server is running.
        getServer().getScheduler().runTask(this, this::warnAboutMoney);
    }

    @Override
    public void onDisable() {
        if (storage != null) {
            storage.close();
        }
    }

    public void reload() {
        settings = PluginConfig.load(this);
        messages = Messages.load(this, settings.language());
        calendar = CalendarConfig.load(this);
        gameDay = new GameDay(Clock.systemUTC(), settings.timeZone(), settings.resetTime());

        // An open menu would keep showing the old calendar.
        for (Player player : getServer().getOnlinePlayers()) {
            InventoryView view = player.getOpenInventory();
            if (view.getType() == InventoryType.CHEST
                    && view.getTopInventory().getHolder(false) instanceof CalendarMenu) {
                player.closeInventory();
            }
        }
        if (rewardGiver != null) {
            warnAboutMoney();
        }
    }

    public PluginConfig settings() {
        return settings;
    }

    public Messages messages() {
        return messages;
    }

    public List<CalendarDay> calendar() {
        return calendar;
    }

    public GameDay gameDay() {
        return gameDay;
    }

    public StreakRules rules() {
        return new StreakRules(calendar.size(), settings.graceDays(), settings.afterLastDay());
    }

    public RewardGiver rewardGiver() {
        return rewardGiver;
    }

    public StreakService streaks() {
        return streaks;
    }

    private void warnAboutMoney() {
        boolean hasMoney = calendar.stream()
                .flatMap(day -> day.rewards().stream())
                .anyMatch(Reward.Money.class::isInstance);
        if (hasMoney && !rewardGiver.economyAvailable()) {
            getSLF4JLogger().warn("rewards.yml has money rewards, but there is no economy plugin behind Vault. "
                    + "Those rewards will be skipped.");
        }
    }
}
