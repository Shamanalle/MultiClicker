package io.github.shamanalle.multiclicker.module.visual;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import io.github.shamanalle.multiclicker.compat.Messages;
import io.github.shamanalle.multiclicker.compat.Slots;
import io.github.shamanalle.multiclicker.compat.Sounds;
import io.github.shamanalle.multiclicker.module.Category;
import io.github.shamanalle.multiclicker.module.Module;
import io.github.shamanalle.multiclicker.setting.BoolSetting;
import io.github.shamanalle.multiclicker.setting.EnumSetting;
import io.github.shamanalle.multiclicker.setting.IntSetting;
import io.github.shamanalle.multiclicker.setting.Unit;
import io.github.shamanalle.multiclicker.stats.Counters;
import io.github.shamanalle.multiclicker.stats.SessionRecord;
import io.github.shamanalle.multiclicker.stats.Stat;
import io.github.shamanalle.multiclicker.stats.StatFormat;
import io.github.shamanalle.multiclicker.stats.Statistics;
import io.github.shamanalle.multiclicker.util.Inventories;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tells the player when something needs attention while the mod works: a full inventory, a tool
 * about to break, the last item of a stack used up, no food, a player nearby, the mod stopping by
 * itself. Other modules report their events through the public methods.
 */
public class NotificationsModule extends Module {
    public enum Where {
        HOTBAR, CHAT, BOTH
    }

    /** The same notification is not repeated sooner than this (10 seconds)... */
    private static final int REPEAT_TICKS = 200;
    /** ...or this (a minute) for a state that lasts, like being hungry without food or a player standing nearby. */
    private static final int REPEAT_LASTING_TICKS = 1200;
    /** A held item must stay gone or worn this long, so hotbar refill gets the chance to replace it first. */
    private static final int SETTLE_TICKS = 10;
    /** Shorter sessions (a quick toggle) get no summary. */
    private static final long SUMMARY_MIN_MS = 30_000;
    private static final List<Stat> SUMMARY_STATS = List.of(Stat.CLICKS, Stat.KILLS, Stat.DAMAGE, Stat.CATCHES,
            Stat.CROPS, Stat.BLOCKS, Stat.FOOD, Stat.XP, Stat.DEATHS);

    public final EnumSetting<Where> where = add(new EnumSetting<>("where", Where.HOTBAR));
    public final BoolSetting sound = add(new BoolSetting("sound", true));
    public final BoolSetting stopped = add(new BoolSetting("stopped", true));
    public final BoolSetting inventoryFull = add(new BoolSetting("inventory_full", true));
    public final IntSetting toolDurability = add(new IntSetting("tool_durability", 10, 0, 100, Unit.NONE)
            .zeroMeans("options.off"));
    public final BoolSetting ranOut = add(new BoolSetting("ran_out", true));
    public final BoolSetting noFood = add(new BoolSetting("no_food", true));
    public final BoolSetting fishCaught = add(new BoolSetting("fish_caught", false));
    public final BoolSetting sessionSummary = add(new BoolSetting("session_summary", true));
    public final IntSetting playerRadius = add(new IntSetting("player_radius", 0, 0, 64, Unit.BLOCKS)
            .zeroMeans("options.off"));

    /** Last tick each notification was shown, to keep repeated events quiet. */
    private final Map<String, Integer> lastShown = new HashMap<>();
    private int ticks;
    private boolean wasFull;
    private int wornTicks;
    private boolean toolWarned;
    /** The stack held in the selected slot last tick, to notice it being used up or broken. */
    private ItemStack lastHeld = ItemStack.EMPTY;
    private int lastSlot = -1;
    private ItemStack goneStack = ItemStack.EMPTY;
    private int goneSlot = -1;
    private int goneTicks;

    public NotificationsModule() {
        super("notifications", Category.VISUAL, true, true);
    }

    @Override
    public void start(Minecraft mc) {
        lastShown.clear();
        wasFull = mc.player != null && isFull(mc.player.getInventory());
        wornTicks = 0;
        toolWarned = false;
        lastHeld = ItemStack.EMPTY;
        lastSlot = -1;
        goneSlot = -1;
    }

    @Override
    public void tick(Minecraft mc) {
        ticks++;
        LocalPlayer player = mc.player;
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        Inventory inventory = player.getInventory();
        checkFull(mc, inventory);
        checkWornTool(mc, player);
        checkUsedUp(mc, inventory);
        checkPlayers(mc, player);
    }

    private void checkFull(Minecraft mc, Inventory inventory) {
        boolean full = isFull(inventory);
        if (full && !wasFull && inventoryFull.get()) {
            send(mc, "inventory_full", REPEAT_TICKS, Component.translatable("multiclicker.notify.inventory_full"));
        }
        wasFull = full;
    }

    private void checkWornTool(Minecraft mc, LocalPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (!Inventories.isNearlyBroken(held, toolDurability.get())) {
            wornTicks = 0;
            toolWarned = false;
            return;
        }
        if (++wornTicks >= SETTLE_TICKS && !toolWarned) {
            toolWarned = true;
            send(mc, "tool_durability", REPEAT_TICKS, Component.translatable("multiclicker.notify.tool_durability",
                    held.getHoverName(), Inventories.remainingDurability(held)));
        }
    }

    private void checkUsedUp(Minecraft mc, Inventory inventory) {
        int slot = Slots.selected(inventory);
        ItemStack held = inventory.getItem(slot);
        if (slot == lastSlot && held.isEmpty() && !lastHeld.isEmpty()
                && (lastHeld.getCount() == 1 || lastHeld.isDamageableItem())) {
            goneStack = lastHeld;
            goneSlot = slot;
            goneTicks = 0;
        }
        if (goneSlot != -1 && ++goneTicks >= SETTLE_TICKS) {
            if (inventory.getItem(goneSlot).isEmpty() && ranOut.get()) {
                String key = goneStack.isDamageableItem() ? "multiclicker.notify.broke" : "multiclicker.notify.ran_out";
                send(mc, "ran_out", REPEAT_TICKS, Component.translatable(key, goneStack.getHoverName()));
            }
            goneSlot = -1;
        }
        lastHeld = held.copy();
        lastSlot = slot;
    }

    private void checkPlayers(Minecraft mc, LocalPlayer player) {
        int radius = playerRadius.get();
        if (radius <= 0) {
            return;
        }
        for (Player other : mc.level.players()) {
            if (other != player && !other.isSpectator() && other.distanceTo(player) <= radius) {
                send(mc, "player:" + other.getUUID(), REPEAT_LASTING_TICKS, Component.translatable("multiclicker.notify.player_nearby",
                        other.getName(), Math.round(other.distanceTo(player))));
            }
        }
    }

    private static boolean isFull(Inventory inventory) {
        for (int slot = 0; slot < Inventories.MAIN_SIZE; slot++) {
            if (inventory.getItem(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Auto eat: hungry, but no food it may eat is in the inventory. */
    public void noFood(Minecraft mc) {
        if (noFood.get()) {
            send(mc, "no_food", REPEAT_LASTING_TICKS, Component.translatable("multiclicker.notify.no_food"));
        }
    }

    /** Auto fish reeled something in. */
    public void fishCaught(Minecraft mc, int catches) {
        if (fishCaught.get()) {
            show(mc, Component.translatable("multiclicker.notify.fish_caught", catches), false);
        }
    }

    /**
     * The mod stopped by itself (safety, a limit, rod protection, an error).
     *
     * @return false when this module is off, so the caller shows the message the usual way
     */
    public boolean stopped(Minecraft mc, Component reason) {
        if (!isEnabled() || !stopped.get() || mc.player == null) {
            return false;
        }
        show(mc, reason, false);
        return true;
    }

    /** The mod was turned off: a line in the chat with what the session did. */
    public void sessionSummary(Minecraft mc, SessionRecord session) {
        if (!isEnabled() || !sessionSummary.get() || mc.player == null || session.duration() < SUMMARY_MIN_MS) {
            return;
        }
        Counters counters = session.counters();
        MutableComponent text = Component.literal("MultiClicker: ").withStyle(ChatFormatting.GOLD)
                .append(Component.translatable("multiclicker.notify.session_summary",
                        Statistics.formatDuration(session.duration())).withStyle(ChatFormatting.YELLOW));
        boolean any = false;
        for (Stat stat : SUMMARY_STATS) {
            if (counters.get(stat) > 0) {
                text.append(Component.literal(any ? " · " : " ").withStyle(ChatFormatting.DARK_GRAY))
                        .append(Component.translatable(stat.translationKey()).withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(" " + StatFormat.value(counters, stat)).withStyle(ChatFormatting.WHITE));
                any = true;
            }
        }
        if (any) {
            Messages.chat(mc.player, text);
        }
    }

    /** Shows a notification unless the one with the same key was shown less than {@code repeatTicks} ago. */
    private void send(Minecraft mc, String key, int repeatTicks, Component message) {
        Integer last = lastShown.get(key);
        if (last != null && ticks - last < repeatTicks) {
            return;
        }
        lastShown.put(key, ticks);
        show(mc, message, sound.get());
    }

    private void show(Minecraft mc, Component message, boolean withSound) {
        if (!isEnabled() || mc.player == null) {
            return;
        }
        Component text = Component.literal("MultiClicker: ").withStyle(ChatFormatting.GOLD)
                .append(message.copy().withStyle(ChatFormatting.YELLOW));
        if (where.get() != Where.CHAT) {
            Messages.overlay(mc.player, text);
        }
        if (where.get() != Where.HOTBAR) {
            Messages.chat(mc.player, text);
        }
        if (withSound) {
            mc.getSoundManager().play(Sounds.notice());
        }
    }
}
