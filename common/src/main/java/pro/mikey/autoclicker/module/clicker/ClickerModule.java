package pro.mikey.autoclicker.module.clicker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import pro.mikey.autoclicker.MultiClicker;
import pro.mikey.autoclicker.module.Category;
import pro.mikey.autoclicker.module.Module;
import pro.mikey.autoclicker.setting.BoolSetting;
import pro.mikey.autoclicker.setting.EnumSetting;
import pro.mikey.autoclicker.setting.IntSetting;
import pro.mikey.autoclicker.setting.Setting;
import pro.mikey.autoclicker.setting.Unit;

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

    public final ClickChannel use;
    public final ClickChannel jump;

    public final IntSetting startDelay;
    public final BoolSetting background;
    public final IntSetting attackLimit;
    public final IntSetting timeLimit;

    private int startDelayLeft;
    private int attackCount;
    /** The player's own "pause on lost focus" option, restored when the clicker stops. */
    private Boolean savedPauseOnLostFocus;

    public ClickerModule() {
        super("clicker", Category.CLICKER, false, true);
        attack = new ClickChannel(this, "attack", true, ClickChannel.Mode.CLICK, 1);
        attackCooldown = register(new BoolSetting("attack_cooldown", true).visibleWhen(attack.enabled::get));
        attackTarget = register(new EnumSetting<>("attack_target", AttackTarget.ENTITIES).visibleWhen(attack.enabled::get));
        pauseWhileUsing = register(new BoolSetting("pause_while_using", true).visibleWhen(attack.enabled::get));

        use = new ClickChannel(this, "use", false, ClickChannel.Mode.CLICK, 4);
        jump = new ClickChannel(this, "jump", false, ClickChannel.Mode.CLICK, 10);

        startDelay = register(new IntSetting("start_delay", 0, 0, 200, Unit.TICKS).zeroMeans("options.off"));
        background = register(new BoolSetting("background", true));
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

    /** True while the attack key is held for continuous block breaking. */
    public boolean isHoldingAttack() {
        return isRunning() && attack.isHolding();
    }

    @Override
    public void start(Minecraft mc) {
        startDelayLeft = startDelay.get();
        attackCount = 0;
        resetChannels(mc);
        if (background.get() && savedPauseOnLostFocus == null) {
            // Vanilla opens the pause menu when the window loses focus, which would stop every AFK task.
            savedPauseOnLostFocus = mc.options.pauseOnLostFocus;
            mc.options.pauseOnLostFocus = false;
        }
    }

    @Override
    public void stop(Minecraft mc) {
        resetChannels(mc);
        restorePauseOnLostFocus(mc);
    }

    public void restorePauseOnLostFocus(Minecraft mc) {
        if (savedPauseOnLostFocus != null) {
            mc.options.pauseOnLostFocus = savedPauseOnLostFocus;
            savedPauseOnLostFocus = null;
        }
    }

    private void resetChannels(Minecraft mc) {
        attack.reset(mc.options.keyAttack);
        use.reset(mc.options.keyUse);
        jump.reset(mc.options.keyJump);
    }

    @Override
    public void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        MultiClicker mod = MultiClicker.get();
        if (mc.screen != null || player.isDeadOrDying() || player.isSpectator()) {
            resetChannels(mc);
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

        if (attack.enabled.get()) {
            tickAttack(mc, player, mod, eating);
        }
        if (use.enabled.get()) {
            boolean allowed = !eating && !mod.autoFish().controlsUseKey();
            if (use.tick(mc.options.keyUse, allowed, true)) {
                mod.stats().recordClick();
            }
        }
        if (jump.enabled.get()) {
            jump.tick(mc.options.keyJump, true, true);
        }
    }

    private void tickAttack(Minecraft mc, LocalPlayer player, MultiClicker mod, boolean eating) {
        HitResult hit = mc.hitResult;
        Entity target = hit instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;
        boolean validEntity = target != null && mod.targetFilter().isValidTarget(mc, target);
        boolean charged = player.getAttackStrengthScale(0.5F) >= 1.0F;

        boolean allowed = !eating && !(pauseWhileUsing.get() && player.isUsingItem());
        if (allowed) {
            if (target != null) {
                allowed = validEntity && (!attackCooldown.get() || charged);
            } else if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
                allowed = attackTarget.get() != AttackTarget.ENTITIES
                        && mod.mining().allowsBreaking(mc, blockHit.getBlockPos());
            } else {
                allowed = attackTarget.get() == AttackTarget.ANYTHING;
            }
        }
        // With cooldown sync, the random delay starts counting once the weapon is charged.
        boolean countDown = !attackCooldown.get() || target == null || charged;

        if (attack.tick(mc.options.keyAttack, allowed, countDown)) {
            attackCount++;
            mod.stats().recordClick();
            if (validEntity) {
                mod.stats().recordAttack(target);
            }
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
