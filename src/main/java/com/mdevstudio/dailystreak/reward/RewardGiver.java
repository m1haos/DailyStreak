package com.mdevstudio.dailystreak.reward;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.slf4j.Logger;

public final class RewardGiver {

    private final Server server;
    private final Logger logger;

    public RewardGiver(Server server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    /**
     * Gives all rewards of a day. Items that do not fit are dropped at the player's feet.
     *
     * @return {@code true} if some items ended up on the ground
     */
    public boolean give(Player player, List<Reward> rewards) {
        List<ItemStack> items = new ArrayList<>();
        for (Reward reward : rewards) {
            switch (reward) {
                case Reward.Item item -> items.addAll(stacks(item));
                case Reward.Money money -> deposit(player, money.amount());
                case Reward.Command command -> server.dispatchCommand(server.getConsoleSender(),
                        command.command().replace("<player>", player.getName()));
            }
        }
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(items.toArray(ItemStack[]::new));
        leftovers.values().forEach(item -> player.getWorld().dropItem(player.getLocation(), item));
        return !leftovers.isEmpty();
    }

    public boolean economyAvailable() {
        return server.getPluginManager().isPluginEnabled("Vault") && VaultBridge.hasEconomy(server);
    }

    public String formatMoney(double amount) {
        String formatted = economyAvailable() ? VaultBridge.format(server, amount) : null;
        return formatted != null ? formatted : BigDecimal.valueOf(amount).stripTrailingZeros().toPlainString();
    }

    private void deposit(Player player, double amount) {
        if (!economyAvailable() || !VaultBridge.deposit(server, player, amount)) {
            logger.warn("Could not give {} money to {}: no working economy plugin behind Vault", amount,
                    player.getName());
        }
    }

    private static List<ItemStack> stacks(Reward.Item item) {
        List<ItemStack> stacks = new ArrayList<>();
        int maxStack = item.material().getMaxStackSize();
        for (int left = item.amount(); left > 0; left -= maxStack) {
            stacks.add(ItemStack.of(item.material(), Math.min(left, maxStack)));
        }
        return stacks;
    }
}
