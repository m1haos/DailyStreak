package com.mdevstudio.dailystreak.menu;

import com.mdevstudio.dailystreak.DailyStreak;
import com.mdevstudio.dailystreak.config.Messages;
import com.mdevstudio.dailystreak.reward.CalendarDay;
import com.mdevstudio.dailystreak.reward.Reward;
import com.mdevstudio.dailystreak.streak.DayStatus;
import com.mdevstudio.dailystreak.streak.StreakRules;
import com.mdevstudio.dailystreak.streak.StreakState;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public final class CalendarMenu implements InventoryHolder {

    private static final int COLUMNS = 7;

    private final DailyStreak plugin;
    private final Player viewer;
    private final Inventory inventory;

    private CalendarMenu(DailyStreak plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
        int rows = 2 + Math.ceilDiv(plugin.calendar().size(), COLUMNS);
        this.inventory = plugin.getServer().createInventory(this, rows * 9,
                plugin.messages().component(Messages.MENU_TITLE));
    }

    public static void open(DailyStreak plugin, Player player) {
        StreakState state = plugin.streaks().state(player.getUniqueId());
        if (state == null) {
            plugin.messages().send(player, Messages.NOT_LOADED);
            return;
        }
        CalendarMenu menu = new CalendarMenu(plugin, player);
        menu.render(state);
        player.openInventory(menu.inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    void click(int slot) {
        StreakState state = plugin.streaks().state(viewer.getUniqueId());
        int dayIndex = dayAt(slot);
        if (state == null || dayIndex < 0
                || dayIndex != plugin.rules().todayIndex(state, plugin.gameDay().today())) {
            return;
        }
        if (plugin.streaks().claim(viewer)) {
            render(plugin.streaks().state(viewer.getUniqueId()));
        }
    }

    private void render(StreakState state) {
        ItemStack filler = ItemStack.of(Material.GRAY_STAINED_GLASS_PANE);
        filler.editMeta(meta -> meta.setHideTooltip(true));
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }

        StreakRules rules = plugin.rules();
        long today = plugin.gameDay().today();
        List<CalendarDay> calendar = plugin.calendar();
        for (int i = 0; i < calendar.size(); i++) {
            inventory.setItem(slotOf(i), dayIcon(i, calendar.get(i), rules.status(i, state, today)));
        }
        inventory.setItem(inventory.getSize() - 5, info(rules.activeStreak(state, today), state.totalClaims()));
    }

    private ItemStack dayIcon(int index, CalendarDay day, DayStatus status) {
        Messages messages = plugin.messages();
        List<Component> lore = new ArrayList<>();
        lore.add(messages.itemText(switch (status) {
            case CLAIMED -> Messages.MENU_CLAIMED;
            case AVAILABLE -> Messages.MENU_AVAILABLE;
            case TOMORROW -> Messages.MENU_TOMORROW;
            case LOCKED -> Messages.MENU_LOCKED;
        }));
        lore.add(Component.empty());
        lore.add(messages.itemText(Messages.MENU_REWARDS));
        for (Reward reward : day.rewards()) {
            Component line = rewardLine(reward);
            if (line != null) {
                lore.add(line);
            }
        }

        Material material = status == DayStatus.CLAIMED ? plugin.settings().claimedIcon() : day.icon();
        // The stack size doubles as the day number, as long as the item can stack that high.
        ItemStack icon = ItemStack.of(material, Math.min(index + 1, material.getMaxStackSize()));
        icon.editMeta(meta -> {
            meta.displayName(messages.itemText(Messages.MENU_DAY,
                    Placeholder.unparsed("day", String.valueOf(index + 1))));
            meta.lore(lore);
            meta.setEnchantmentGlintOverride(status == DayStatus.AVAILABLE);
        });
        return icon;
    }

    private Component rewardLine(Reward reward) {
        Messages messages = plugin.messages();
        return switch (reward) {
            case Reward.Item item -> messages.itemText(Messages.MENU_REWARD_ITEM,
                    Placeholder.unparsed("amount", String.valueOf(item.amount())),
                    Placeholder.component("item", Component.translatable(item.material())));
            case Reward.Money money -> messages.itemText(Messages.MENU_REWARD_MONEY,
                    Placeholder.unparsed("amount", plugin.rewardGiver().formatMoney(money.amount())));
            case Reward.Command command -> command.description().isEmpty() ? null
                    : messages.itemText(Messages.MENU_REWARD_COMMAND,
                            Placeholder.parsed("description", command.description()));
        };
    }

    private ItemStack info(int streak, int total) {
        Messages messages = plugin.messages();
        ItemStack info = ItemStack.of(Material.CLOCK);
        info.editMeta(meta -> {
            meta.displayName(messages.itemText(Messages.MENU_INFO_NAME));
            meta.lore(List.of(
                    messages.itemText(Messages.MENU_INFO_STREAK,
                            Placeholder.unparsed("streak", String.valueOf(streak))),
                    messages.itemText(Messages.MENU_INFO_TOTAL,
                            Placeholder.unparsed("total", String.valueOf(total))),
                    messages.itemText(Messages.MENU_INFO_NEXT,
                            Placeholder.component("time", messages.duration(plugin.gameDay().untilNextDay())))));
        });
        return info;
    }

    private static int slotOf(int dayIndex) {
        return (1 + dayIndex / COLUMNS) * 9 + 1 + dayIndex % COLUMNS;
    }

    private int dayAt(int slot) {
        int row = slot / 9;
        int column = slot % 9;
        if (row < 1 || column < 1 || column > COLUMNS) {
            return -1;
        }
        int index = (row - 1) * COLUMNS + column - 1;
        return index < plugin.calendar().size() ? index : -1;
    }
}
