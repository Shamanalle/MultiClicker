package pro.mikey.autoclicker.module.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import pro.mikey.autoclicker.module.Category;
import pro.mikey.autoclicker.module.Module;
import pro.mikey.autoclicker.setting.BoolSetting;

/** Decides which entities the attack clicker (and the target highlight) may target. */
public class TargetFilterModule extends Module {
    public enum Kind {
        PLAYER, HOSTILE, NEUTRAL, PASSIVE, OTHER
    }

    public final BoolSetting players = add(new BoolSetting("players", false));
    public final BoolSetting hostile = add(new BoolSetting("hostile", true));
    public final BoolSetting neutral = add(new BoolSetting("neutral", true));
    public final BoolSetting passive = add(new BoolSetting("passive", true));
    public final BoolSetting other = add(new BoolSetting("other", false));
    public final BoolSetting ignoreBabies = add(new BoolSetting("ignore_babies", false));
    public final BoolSetting ignoreNamed = add(new BoolSetting("ignore_named", true));
    public final BoolSetting ignorePets = add(new BoolSetting("ignore_pets", true));
    public final BoolSetting ignoreInvisible = add(new BoolSetting("ignore_invisible", false));

    public TargetFilterModule() {
        super("target_filter", Category.COMBAT, true, true);
    }

    public static Kind classify(Entity entity) {
        if (entity instanceof Player) {
            return Kind.PLAYER;
        }
        if (!(entity instanceof Mob)) {
            // Armor stands, boats, minecarts, end crystals, item frames...
            return Kind.OTHER;
        }
        if (entity instanceof NeutralMob) {
            // Endermen, zombified piglins, wolves, bees, iron golems...
            return Kind.NEUTRAL;
        }
        if (entity instanceof Enemy) {
            return Kind.HOSTILE;
        }
        return Kind.PASSIVE;
    }

    /** Whether the entity is something the player is allowed to auto-attack. */
    public boolean isValidTarget(Minecraft mc, Entity entity) {
        if (entity == null || entity == mc.player || !entity.isAlive() || !entity.isAttackable()) {
            return false;
        }
        if (entity instanceof LivingEntity living && living.isDeadOrDying()) {
            return false;
        }
        if (!isEnabled()) {
            // Without the filter, attack every living creature but never decorations.
            return entity instanceof Mob || entity instanceof Player;
        }
        boolean allowed = switch (classify(entity)) {
            case PLAYER -> players.get();
            case HOSTILE -> hostile.get();
            case NEUTRAL -> neutral.get();
            case PASSIVE -> passive.get();
            case OTHER -> other.get();
        };
        if (!allowed) {
            return false;
        }
        if (ignoreBabies.get() && entity instanceof LivingEntity living && living.isBaby()) {
            return false;
        }
        if (ignoreNamed.get() && entity.hasCustomName() && !(entity instanceof Player)) {
            return false;
        }
        if (ignorePets.get() && entity instanceof OwnableEntity ownable && ownable.getOwnerReference() != null) {
            return false;
        }
        return !(ignoreInvisible.get() && entity.isInvisible());
    }
}
