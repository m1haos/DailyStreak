package com.mdevstudio.dailystreak.reward;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

// Kept apart so that Vault classes are only loaded when Vault is actually installed.
final class VaultBridge {

    private VaultBridge() {
    }

    static boolean hasEconomy(Server server) {
        return economy(server) != null;
    }

    static boolean deposit(Server server, Player player, double amount) {
        Economy economy = economy(server);
        return economy != null && economy.depositPlayer(player, amount).transactionSuccess();
    }

    static String format(Server server, double amount) {
        Economy economy = economy(server);
        return economy != null ? economy.format(amount) : null;
    }

    private static Economy economy(Server server) {
        RegisteredServiceProvider<Economy> provider = server.getServicesManager().getRegistration(Economy.class);
        return provider != null ? provider.getProvider() : null;
    }
}
