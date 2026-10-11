package io.github.shamanalle.multiclicker.module.clicker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.compat.Entities;
import io.github.shamanalle.multiclicker.compat.Screens;
import io.github.shamanalle.multiclicker.mixin.MinecraftAccessor;
import io.github.shamanalle.multiclicker.module.Category;
import io.github.shamanalle.multiclicker.module.Module;
import io.github.shamanalle.multiclicker.setting.BoolSetting;
import io.github.shamanalle.multiclicker.setting.EnumSetting;
import io.github.shamanalle.multiclicker.setting.IntSetting;
import io.github.shamanalle.multiclicker.setting.Setting;
import io.github.shamanalle.multiclicker.setting.Unit;
import io.github.shamanalle.multiclicker.util.Input;

import java.util.ArrayList;
import java.util.List;

/** The auto clicker itself: attack, use and jump channels plus session limits. */
public class ClickerModule extends Module {
    public enum AttackTarget {
        ENTITIES, ENTITIES_AND_BLOCKS, ANYTHING
    }

    public final ClickChannel attack;
    public final BoolSetting attackCooldown;
    public final EnumSetting<AttackTarget> attackTarget;
    public final BoolSetting pauseWhileUsing;
    public final BoolSetting lootingSwap;

    public final ClickChannel use;
    public final ClickChannel jump;

    public final IntSetting startDelay;
    public final BoolSetting background;
    public final BoolSetting lockCamera;
    public final IntSetting attackLimit;
    public final IntSetting timeLimit;

    private int startDelayLeft;
    private int attackCount;
    /** The player's own "pause on lost focus" option, restored when the clicker stops. */
    private Boolean savedPauseOnLostFocus;
    private final LootingSwap swap = new LootingSwap();
    /** The use key was let go for one tick so the hit lands past the shield; it goes back next tick. */
    private boolean shieldLowered;
    /** Set by the attack channel this tick: the use key must stay up. */
    private boolean lowerShieldNow;

    public ClickerModule() {
        super("clicker", Category.CLICKER, false, true);
        attack = new ClickChannel(this, "attack", true, ClickChannel.Mode.CLICK, 1, 10);
        attackCooldown = register(new BoolSetting("attack_cooldown", true).visibleWhen(attack.enabled::get));
        attackTarget = register(new EnumSetting<>("attack_target", AttackTarget.ENTITIES).visibleWhen(attack.enabled::get));
        pauseWhileUsing = register(new BoolSetting("pause_while_using", true).visibleWhen(attack.enabled::get));
        lootingSwap = register(new BoolSetting("looting_swap", false).visibleWhen(attack.enabled::get));

        use = new ClickChannel(this, "use", false, ClickChannel.Mode.CLICK, 4, 5);
        jump = new ClickChannel(this, "jump", false, ClickChannel.Mode.CLICK, 10, 2);

        startDelay = register(new IntSetting("start_delay", 0, 0, 200, Unit.TICKS).zeroMeans("options.off"));
        background = register(new BoolSetting("background", true));
        lockCamera = register(new BoolSetting("lock_camera", false));
        attackLimit = register(new IntSetting("attack_limit", 0, 0, 5000, Unit.NONE).zeroMeans("multiclicker.value.unlimited"));
        timeLimit = register(new IntSetting("time_limit", 0, 0, 600, Unit.MINUTES).zeroMeans("multiclicker.value.unlimited"));
    }

    <S extends Setting<?>> S register(S setting) {
        return add(setting);
    }

    /** Settings grouped under a sub-header in the UI; the key is the first setting of each group. */
    public List<Setting<?>> groupStarts() {
        List<Setting<?>> starts = new ArrayList<>();
        starts.add(attack.enabled);
        starts.add(use.enabled);
        starts.add(jump.enabled);
        starts.add(startDelay);
        return starts;
    }

    /** True while a Looting weapon is selected for a finishing blow (other slot changes must wait). */
    public boolean isSwappingWeapon() {
        return swap.isActive();
    }

    /** Mouse movement does not turn the camera while the mod works, so the aim cannot drift. */
    public boolean isCameraLocked() {
        return lockCamera.get() && isRunning() && Screens.current(Minecraft.getInstance()) == null;
    }

    /** True while the attack key is held for continuous block breaking. */
    public boolean isHoldingAttack() {
        return isRunning() && attack.isHolding();
    }

    /** True while the use key is held in hold mode (e.g. a shield raised for good). */
    public boolean isHoldingUse() {
        return isRunning() && use.isHolding();
    }

    @Override
    public void start(Minecraft mc) {
        startDelayLeft = startDelay.get();
        attackCount = 0;
        resetChannels(mc);
        applyBackground(mc);
    }

    @Override
    public void resume(Minecraft mc) {
        int delayLeft = startDelayLeft;
        int count = attackCount;
        start(mc);
        startDelayLeft = delayLeft;
        attackCount = count;
    }

    /** Follows the "work in background" switch, also when it is flipped while the mod runs. */
    private void applyBackground(Minecraft mc) {
        if (!background.get()) {
            restorePauseOnLostFocus(mc);
        } else if (savedPauseOnLostFocus == null) {
            // Vanilla opens the pause menu when the window loses focus, which would stop every AFK task.
            savedPauseOnLostFocus = mc.options.pauseOnLostFocus;
            mc.options.pauseOnLostFocus = false;
        }
    }

    @Override
    public void stop(Minecraft mc) {
        resetChannels(mc);
        swap.restore(mc, mc.player);
        restorePauseOnLostFocus(mc);
    }

    public void restorePauseOnLostFocus(Minecraft mc) {
        if (savedPauseOnLostFocus != null) {
            mc.options.pauseOnLostFocus = savedPauseOnLostFocus;
            savedPauseOnLostFocus = null;
        }
    }

    private void resetChannels(Minecraft mc) {
        raiseShield(mc);
        attack.reset(mc.options.keyAttack);
        use.reset(mc.options.keyUse);
        jump.reset(mc.options.keyJump);
    }

    @Override
    public void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        MultiClicker mod = MultiClicker.get();
        applyBackground(mc);
        if (Screens.current(mc) != null || player.isDeadOrDying() || player.isSpectator()) {
            resetChannels(mc);
            if (swap.isActive()) {
                swap.restore(mc, player);
            }
            return;
        }
        if (startDelayLeft > 0) {
            startDelayLeft--;
            return;
        }
        if (checkLimits(mod)) {
            return;
        }

        boolean eating = mod.autoEat().isBusy();
        raiseShield(mc);
        lowerShieldNow = false;

        // A channel switched off by its hotkey while running gives its key back, or it would stay held.
        if (attack.enabled.get()) {
            tickAttack(mc, player, mod, eating);
        } else {
            attack.reset(mc.options.keyAttack);
            if (swap.isActive()) {
                swap.restore(mc, player);
            }
        }
        if (lowerShieldNow) {
            // The shield stays down for this tick; the use channel picks the key up again next tick.
            Input.forceUp(mc.options.keyUse);
            shieldLowered = true;
        } else if (mod.autoEat().holdsUseKey()) {
            // Releasing the key now would stop the meal; auto eat gives it back when done.
            use.handOver();
        } else if (use.enabled.get()) {
            boolean allowed = !eating && !mod.autoFish().controlsUseKey();
            if (use.tick(mc.options.keyUse, allowed, true)) {
                mod.stats().recordClick();
            }
        } else {
            use.reset(mc.options.keyUse);
        }
        if (jump.enabled.get()) {
            jump.tick(mc.options.keyJump, true, true);
        } else {
            jump.reset(mc.options.keyJump);
        }
    }

    private void tickAttack(Minecraft mc, LocalPlayer player, MultiClicker mod, boolean eating) {
        HitResult hit = mc.hitResult;
        // The ender dragon is aimed at through its body parts.
        Entity target = hit instanceof EntityHitResult entityHit ? Entities.whole(entityHit.getEntity()) : null;
        boolean validEntity = target != null && mod.targetFilter().isValidTarget(mc, target);
        boolean charged = player.getAttackStrengthScale(0.5F) >= 1.0F;
        boolean onBlock = target == null && hit instanceof BlockHitResult && hit.getType() == HitResult.Type.BLOCK;
        // With cooldown sync the delay starts counting once the weapon is charged, so it is added on
        // top of the cooldown. Mining does not depend on the charge.
        boolean countDown = !attackCooldown.get() || onBlock || charged;
        // Vanilla drops attack clicks while an item is in use and for half a second after a click
        // into the air. Of the items in use, only a shield may be lowered for the hit.
        boolean inUse = player.isUsingItem();
        boolean shieldInTheWay = inUse && !pauseWhileUsing.get() && player.getUseItem().is(Items.SHIELD);
        boolean blocked = inUse || ((MinecraftAccessor) mc).multiclicker$getMissTime() > 0;
        boolean paused = eating || inUse && !shieldInTheWay;

        if (tickLootingSwap(mc, player, mod, eating, paused, blocked, shieldInTheWay, countDown,
                validEntity ? target : null)) {
            return;
        }

        boolean allowed = !paused;
        if (allowed) {
            if (target != null) {
                allowed = validEntity && (!attackCooldown.get() || charged);
                if (validEntity && mod.autoTool().giveWeaponBack(player)) {
                    // A mining tool is in hand: the weapon comes back first and its charge starts over.
                    allowed = false;
                }
            } else if (onBlock) {
                allowed = attackTarget.get() != AttackTarget.ENTITIES
                        && mod.mining().allowsBreaking(mc, ((BlockHitResult) hit).getBlockPos());
            } else {
                // A swing into the air also uses up the charge, so it waits for it like a hit.
                allowed = attackTarget.get() == AttackTarget.ANYTHING && (!attackCooldown.get() || charged);
            }
        }
        if (allowed && blocked) {
            // The click would be dropped: let the delay run and, past a shield, lower it so the
            // hit lands on the next tick.
            if (attack.waitTick(mc.options.keyAttack, countDown) && shieldInTheWay && validEntity) {
                lowerShieldNow = true;
            }
            return;
        }

        if (attack.tick(mc.options.keyAttack, allowed, countDown)) {
            attackCount++;
            mod.stats().recordClick();
            if (validEntity) {
                mod.stats().recordAttack(target);
            }
        }
    }

    /**
     * Runs the Looting finishing-blow logic. The blow keeps the rhythm of the attack channel: it
     * waits for the same delay, and the next click waits for it again.
     *
     * @return {@code true} if it handled the attack key this tick
     */
    private boolean tickLootingSwap(Minecraft mc, LocalPlayer player, MultiClicker mod, boolean eating,
                                    boolean paused, boolean blocked, boolean shieldInTheWay, boolean countDown,
                                    Entity validTarget) {
        if (!swap.isActive()) {
            boolean canStart = lootingSwap.get() && !paused && validTarget instanceof LivingEntity living
                    && swap.tryStart(mc, player, living);
            if (!canStart) {
                return false;
            }
            attack.release(mc.options.keyAttack);
            return true;
        }
        boolean due = attack.waitTick(mc.options.keyAttack, countDown);
        if (due && blocked && shieldInTheWay && !paused && swap.readyToHit(mc, player)) {
            lowerShieldNow = true;
        }
        LootingSwap.Result result = swap.tick(mc, player, eating, due, blocked);
        if (result == LootingSwap.Result.ATTACKED) {
            attack.clicked();
            attackCount++;
            mod.stats().recordClick();
            mod.stats().recordAttack(validTarget != null ? validTarget
                    : Entities.whole(((EntityHitResult) mc.hitResult).getEntity()));
        }
        return result != LootingSwap.Result.IDLE;
    }

    /** Gives the use key back after it was let go for a hit past the shield. */
    private void raiseShield(Minecraft mc) {
        if (shieldLowered) {
            shieldLowered = false;
            Input.release(mc.options.keyUse);
        }
    }

    /** Stops the mod once a configured limit is reached. */
    private boolean checkLimits(MultiClicker mod) {
        if (attackLimit.get() > 0 && attackCount >= attackLimit.get()) {
            mod.setActive(false, Component.translatable("multiclicker.message.attack_limit", attackLimit.get()));
            return true;
        }
        if (timeLimit.get() > 0 && mod.stats().elapsedMillis() >= timeLimit.get() * 60_000L) {
            mod.setActive(false, Component.translatable("multiclicker.message.time_limit", timeLimit.get()));
            return true;
        }
        return false;
    }

    public List<ClickChannel> channels() {
        return List.of(attack, use, jump);
    }

    /**
     * The clicks per second the settings aim for, for the statistics graph; -1 when no channel
     * clicks at a set rate (holding, or attacking whenever the weapon is charged).
     */
    public double targetCps() {
        if (!isRunning()) {
            return -1;
        }
        double total = 0;
        boolean any = false;
        for (ClickChannel channel : List.of(attack, use)) {
            if (!channel.enabled.get() || channel.mode.get() == ClickChannel.Mode.HOLD) {
                continue;
            }
            if (channel == attack && attackCooldown.get()) {
                return -1;
            }
            total += channel.averageCps();
            any = true;
        }
        return any ? total : -1;
    }

    /** Which channels are on, for the HUD. */
    public List<ClickChannel> enabledChannels() {
        List<ClickChannel> channels = new ArrayList<>(3);
        for (ClickChannel channel : List.of(attack, use, jump)) {
            if (channel.enabled.get()) {
                channels.add(channel);
            }
        }
        return channels;
    }
}
