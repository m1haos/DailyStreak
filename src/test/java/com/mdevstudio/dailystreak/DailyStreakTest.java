package com.mdevstudio.dailystreak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mdevstudio.dailystreak.menu.CalendarMenu;
import com.mdevstudio.dailystreak.streak.StreakState;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class DailyStreakTest {

    private static final int FIRST_DAY_SLOT = 10;

    private ServerMock server;
    private DailyStreak plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock(new PaperServerMock());
        plugin = MockBukkit.load(DailyStreak.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void joiningPlayerIsRemindedAboutTheReward() throws InterruptedException {
        PlayerMock player = join();

        assertTrue(allMessages(player).contains("/daily"));
    }

    @Test
    void firstDayIsGivenOnceEvenOnDoubleClick() throws InterruptedException {
        PlayerMock player = join();
        InventoryView menu = openMenu(player);

        InventoryClickEvent first = leftClick(menu, FIRST_DAY_SLOT);
        leftClick(menu, FIRST_DAY_SLOT);

        assertTrue(first.isCancelled());
        assertEquals(16, count(player, Material.BREAD));
        StreakState state = plugin.streaks().state(player.getUniqueId());
        assertEquals(1, state.streak());
        assertEquals(1, state.totalClaims());
        assertTrue(allMessages(player).contains("already claimed"));
    }

    @Test
    void rewardsThatDoNotFitAreDroppedAtTheFeet() throws InterruptedException {
        PlayerMock player = join();
        // Every slot, not just the 36 main ones: MockBukkit also fills armor slots when adding items.
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            player.getInventory().setItem(slot, ItemStack.of(Material.STONE, 64));
        }

        leftClick(openMenu(player), FIRST_DAY_SLOT);

        int onGround = player.getWorld().getEntitiesByClass(Item.class).stream()
                .filter(item -> item.getItemStack().getType() == Material.BREAD)
                .mapToInt(item -> item.getItemStack().getAmount())
                .sum();
        assertEquals(16, onGround);
        assertTrue(allMessages(player).contains("on the ground"));
    }

    @Test
    void lockedDaysGiveNothing() throws InterruptedException {
        PlayerMock player = join();
        InventoryView menu = openMenu(player);

        leftClick(menu, FIRST_DAY_SLOT + 1);

        assertEquals(0, count(player, Material.IRON_INGOT));
        assertEquals(0, plugin.streaks().state(player.getUniqueId()).totalClaims());
    }

    @Test
    void itemsCannotBeMovedInOrOutOfTheMenu() throws InterruptedException {
        PlayerMock player = join();
        player.getInventory().setItem(0, ItemStack.of(Material.DIRT, 10));
        InventoryView menu = openMenu(player);
        int bottomFirstSlot = menu.getTopInventory().getSize() + 27;

        InventoryClickEvent shiftFromBelow = call(new InventoryClickEvent(menu, SlotType.CONTAINER, bottomFirstSlot,
                ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY));
        InventoryClickEvent numberKey = call(new InventoryClickEvent(menu, SlotType.CONTAINER, FIRST_DAY_SLOT + 1,
                ClickType.NUMBER_KEY, InventoryAction.HOTBAR_SWAP, 0));
        InventoryClickEvent takeIcon = call(new InventoryClickEvent(menu, SlotType.CONTAINER, FIRST_DAY_SLOT + 1,
                ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY));

        assertTrue(shiftFromBelow.isCancelled());
        assertTrue(numberKey.isCancelled());
        assertTrue(takeIcon.isCancelled());
        assertFalse(menu.getTopInventory().contains(Material.DIRT));
    }

    @Test
    void resetCommandStartsTheStreakOver() throws InterruptedException {
        PlayerMock player = join();
        leftClick(openMenu(player), FIRST_DAY_SLOT);

        server.dispatchCommand(server.getConsoleSender(), "daily reset " + player.getName());

        assertEquals(StreakState.EMPTY, plugin.streaks().state(player.getUniqueId()));
    }

    @Test
    void setCommandMakesTheChosenDayAvailable() throws InterruptedException {
        PlayerMock player = join();

        server.dispatchCommand(server.getConsoleSender(), "daily set " + player.getName() + " 5");
        leftClick(openMenu(player), FIRST_DAY_SLOT + 4);

        assertEquals(2, count(player, Material.GOLDEN_APPLE));
        assertEquals(5, plugin.streaks().state(player.getUniqueId()).streak());
    }

    @Test
    void setCommandRefusesDaysPastTheCalendar() throws InterruptedException {
        PlayerMock player = join();
        player.setOp(true);
        allMessages(player);

        server.dispatchCommand(player, "daily set " + player.getName() + " 9");

        assertEquals(StreakState.EMPTY, plugin.streaks().state(player.getUniqueId()));
        assertTrue(allMessages(player).contains("pick one from 1 to 7"));
    }

    @Test
    void linesMissingFromAnOldLanguageFileComeFromTheBundledOne() throws IOException, InterruptedException {
        Files.writeString(plugin.getDataFolder().toPath().resolve("lang/en.yml"), "prefix: \"<gray>Old\"\n");
        plugin.reload();
        PlayerMock player = join();
        player.setOp(true);
        allMessages(player);

        server.dispatchCommand(player, "daily set " + player.getName() + " 9");

        String messages = allMessages(player);
        assertTrue(messages.contains("Old"));
        assertTrue(messages.contains("pick one from 1 to 7"));
    }

    @Test
    void reloadClosesOpenMenus() throws InterruptedException {
        PlayerMock player = join();
        openMenu(player);

        plugin.reload();

        assertEquals(InventoryType.CRAFTING, player.getOpenInventory().getType());
    }

    private PlayerMock join() throws InterruptedException {
        PlayerMock player = server.addPlayer();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        // Loading runs on the storage thread and lands on the main thread with the next tick.
        while (plugin.streaks().state(player.getUniqueId()) == null) {
            assertTrue(System.nanoTime() < deadline, "streak was not loaded within 5 seconds");
            Thread.sleep(10);
            server.getScheduler().performOneTick();
        }
        return player;
    }

    private InventoryView openMenu(PlayerMock player) {
        CalendarMenu.open(plugin, player);
        InventoryView view = player.getOpenInventory();
        assertNotNull(view);
        assertTrue(view.getTopInventory().getHolder(false) instanceof CalendarMenu);
        return view;
    }

    private InventoryClickEvent leftClick(InventoryView view, int slot) {
        return call(new InventoryClickEvent(view, SlotType.CONTAINER, slot,
                ClickType.LEFT, InventoryAction.PICKUP_ALL));
    }

    private InventoryClickEvent call(InventoryClickEvent event) {
        server.getPluginManager().callEvent(event);
        return event;
    }

    private static int count(PlayerMock player, Material material) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }
        return total;
    }

    private static String allMessages(PlayerMock player) {
        StringBuilder text = new StringBuilder();
        for (Component message = player.nextComponentMessage(); message != null;
                message = player.nextComponentMessage()) {
            text.append(PlainTextComponentSerializer.plainText().serialize(message)).append('\n');
        }
        return text.toString();
    }
}
