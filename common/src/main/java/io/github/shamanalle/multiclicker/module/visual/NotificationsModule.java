package io.github.shamanalle.multiclicker.module.visual;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import io.github.shamanalle.multiclicker.compat.Entities;
import io.github.shamanalle.multiclicker.compat.Messages;
import io.github.shamanalle.multiclicker.compat.Slots;
import io.github.shamanalle.multiclicker.compat.Sounds;
import io.github.shamanalle.multiclicker.module.Category;
import io.github.shamanalle.multiclicker.module.Module;
import io.github.shamanalle.multiclicker.setting.BoolSetting;
import io.github.shamanalle.multiclicker.setting.EnumSetting;
import io.github.shamanalle.multiclicker.setting.IntSetting;
import io.github.shamanalle.multiclicker.setting.StringSetting;
import io.github.shamanalle.multiclicker.setting.Unit;
import io.github.shamanalle.multiclicker.stats.Counters;
import io.github.shamanalle.multiclicker.stats.SessionRecord;
import io.github.shamanalle.multiclicker.stats.Stat;
import io.github.shamanalle.multiclicker.stats.StatFormat;
import io.github.shamanalle.multiclicker.stats.Statistics;
import io.github.shamanalle.multiclicker.util.Inventories;
import io.github.shamanalle.multiclicker.util.ServerStats;
import io.github.shamanalle.multiclicker.util.Webhook;
import net.minecraft.client.resources.language.I18n;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Tells the player when something needs attention while the mod works: a full inventory, a tool
 * about to break, the last item of a stack used up, no food, a player nearby, the mod stopping by
 * itself. Other modules report their events through the public methods. The important events can
 * also go to a Discord webhook, to keep an eye on the game from the phone while away.
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
    /** Belongs to the player, not to a setup: profiles neither store nor change it. */
    public final StringSetting webhook = add(new StringSetting("webhook", "", 300).secret()
            .validator(Webhook::isValidUrl).tester(this::testWebhook).global());
    public final BoolSetting webhookStopped = add(webhookEvent("webhook_stopped", true));
    public final BoolSetting webhookDisconnect = add(webhookEvent("webhook_disconnect", true));
    public final BoolSetting webhookDeath = add(webhookEvent("webhook_death", true));
    public final BoolSetting webhookPlayer = add(webhookEvent("webhook_player", true));
    public final BoolSetting webhookInventory = add(webhookEvent("webhook_inventory", false));
    public final BoolSetting webhookGoal = add(webhookEvent("webhook_goal", true));
    public final BoolSetting webhookSummary = add(webhookEvent("webhook_summary", true));

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
    private boolean wasDead;
    /** Tick of the last death sent to the webhook, so the stop that follows it is not sent twice. */
    private int deathPostedAt = Integer.MIN_VALUE / 2;

    public NotificationsModule() {
        super("notifications", Category.SESSION, true, true);
    }

    private BoolSetting webhookEvent(String key, boolean defaultValue) {
        return new BoolSetting(key, defaultValue).visibleWhen(() -> !webhook.isEmpty());
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
        wasDead = mc.player != null && mc.player.isDeadOrDying();
    }

    @Override
    public void tick(Minecraft mc) {
        ticks++;
        LocalPlayer player = mc.player;
        boolean dead = player.isDeadOrDying();
        if (dead && !wasDead) {
            died(mc, player);
        }
        wasDead = dead;
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
            if (send(mc, "inventory_full", REPEAT_TICKS, Component.translatable("multiclicker.notify.inventory_full"))) {
                post(mc, webhookInventory, Component.translatable("multiclicker.webhook.inventory_full"), Component.empty(),
                        Webhook.WARNING);
            }
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
                Component message = Component.translatable("multiclicker.notify.player_nearby",
                        other.getName(), Math.round(other.distanceTo(player)));
                if (send(mc, "player:" + other.getUUID(), REPEAT_LASTING_TICKS, message)) {
                    post(mc, webhookPlayer, Component.translatable("multiclicker.webhook.player_nearby"), message,
                            Webhook.WARNING);
                }
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

    /** The crosshair rests on a player, but players are off in the target filter; once per start. */
    public void playersSkipped(Minecraft mc) {
        send(mc, "players_skipped", Integer.MAX_VALUE, Component.translatable("multiclicker.notify.players_skipped"));
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
        if (ticks - deathPostedAt > SETTLE_TICKS) {
            post(mc, webhookStopped, Component.translatable("multiclicker.webhook.stopped"), reason, Webhook.DANGER);
        }
        if (!isEnabled() || !stopped.get() || mc.player == null) {
            return false;
        }
        show(mc, reason, false);
        return true;
    }

    /** The mod was turned off: a line in the chat with what the session did. */
    public void sessionSummary(Minecraft mc, SessionRecord session) {
        if (!isEnabled() || session.duration() < SUMMARY_MIN_MS) {
            return;
        }
        Counters counters = session.counters();
        postSummary(mc, session, counters);
        if (!sessionSummary.get() || mc.player == null) {
            return;
        }
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

    private void postSummary(Minecraft mc, SessionRecord session, Counters counters) {
        StringBuilder text = new StringBuilder();
        for (Stat stat : SUMMARY_STATS) {
            if (counters.get(stat) > 0) {
                text.append(I18n.get(stat.translationKey())).append(": **").append(StatFormat.value(counters, stat))
                        .append("**\n");
            }
        }
        post(mc, webhookSummary, Component.translatable("multiclicker.webhook.summary",
                Statistics.formatDuration(session.duration())), Component.literal(text.toString().strip()), Webhook.INFO);
    }

    /** The player died while the mod was working. */
    private void died(Minecraft mc, LocalPlayer player) {
        deathPostedAt = ticks;
        post(mc, webhookDeath, Component.translatable("multiclicker.webhook.death"), deathPlace(player), Webhook.DANGER);
    }

    /** "x y z in dimension", for the chat and the webhook. */
    public static Component deathPlace(LocalPlayer player) {
        return Component.translatable("multiclicker.notify.died_at", player.getBlockX(), player.getBlockY(),
                player.getBlockZ(), Entities.dimension(player));
    }

    /** The connection to the server was lost; auto reconnect may try again. */
    public void disconnected(Minecraft mc, Component reason, boolean reconnecting) {
        Component text = reconnecting
                ? reason.copy().append("\n").append(Component.translatable("multiclicker.webhook.reconnecting"))
                : reason;
        post(mc, webhookDisconnect, Component.translatable("multiclicker.webhook.disconnected"), text, Webhook.WARNING);
    }

    /** Auto reconnect joined the server again. */
    public void reconnected(Minecraft mc) {
        post(mc, webhookDisconnect, Component.translatable("multiclicker.webhook.reconnected"), Component.empty(),
                Webhook.SUCCESS);
    }

    /**
     * A session goal was reached. Shown even while this module is off, since the player asked for
     * it; when the goal stops the mod, the stop message says it instead.
     */
    public void goalReached(Minecraft mc, Component goal, boolean stopping) {
        post(mc, webhookGoal, Component.translatable("multiclicker.webhook.goal"), goal, Webhook.SUCCESS);
        if (stopping || mc.player == null) {
            return;
        }
        Component message = Component.translatable("multiclicker.notify.goal_reached", goal);
        if (isEnabled()) {
            show(mc, message, true);
        } else {
            Messages.overlay(mc.player, Component.literal("MultiClicker: ").withStyle(ChatFormatting.GOLD)
                    .append(message.copy().withStyle(ChatFormatting.YELLOW)));
            mc.getSoundManager().play(Sounds.notice());
        }
    }

    /** Sends an event to the webhook, if one is set and the event is chosen. */
    private void post(Minecraft mc, BoolSetting event, Component title, Component text, int color) {
        if (isEnabled() && !webhook.isEmpty() && event.get()) {
            Webhook.send(webhook.get(), title.getString(), text.getString(), color, footer(mc));
        }
    }

    private static String footer(Minecraft mc) {
        String name = mc.player != null ? mc.player.getName().getString() : mc.getUser().getName();
        String server = ServerStats.address(mc);
        return server == null ? name : name + " · " + server;
    }

    private CompletableFuture<Component> testWebhook(String url) {
        Minecraft mc = Minecraft.getInstance();
        return Webhook.send(url, I18n.get("multiclicker.webhook.test"), I18n.get("multiclicker.webhook.test.desc"),
                Webhook.SUCCESS, footer(mc)).thenApply(status -> status >= 200 && status < 300
                ? Component.translatable("multiclicker.webhook.test.ok")
                : Component.translatable("multiclicker.webhook.test.failed", status < 0 ? "—" : String.valueOf(status)));
    }

    /**
     * Shows a notification unless the one with the same key was shown less than {@code repeatTicks} ago.
     *
     * @return whether it was shown
     */
    private boolean send(Minecraft mc, String key, int repeatTicks, Component message) {
        Integer last = lastShown.get(key);
        if (last != null && ticks - last < repeatTicks) {
            return false;
        }
        lastShown.put(key, ticks);
        show(mc, message, sound.get());
        return true;
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
