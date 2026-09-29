package com.mdevstudio.dailystreak.menu;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public final class MenuListener implements Listener {

    // Cancelling on the top inventory also covers shift-clicks, number keys and double clicks from below.
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder(false) instanceof CalendarMenu menu) {
            event.setCancelled(true);
            if (event.getClickedInventory() == top) {
                menu.click(event.getSlot());
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof CalendarMenu) {
            event.setCancelled(true);
        }
    }
}
