package pro.mikey.autoclicker.modules.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Npc;
import pro.mikey.autoclicker.core.Module;
import pro.mikey.autoclicker.core.Setting;
import pro.mikey.autoclicker.core.Setting.*;

import java.util.List;

/**
 * Mob Filter module: configurable entity targeting rules.
 */
public class MobFilterModule implements Module {

    public final BooleanSetting filterBabies = new BooleanSetting("mob_filter.filter_babies", "Игнор. детёнышей",
            false).withDescription("Пропускает детёнышей мобов (isBaby). Полезно для ферм");
    public final BooleanSetting filterAdults = new BooleanSetting("mob_filter.filter_adults", "Игнор. взрослых", false)
            .withDescription("Пропускает взрослых мобов. Полезно при разведении");
    public final BooleanSetting allowHostile = new BooleanSetting("mob_filter.allow_hostile", "Враждебные", true)
            .withDescription("Зомби, скелеты, криперы, эндермены и т.д.");
    public final BooleanSetting allowPassive = new BooleanSetting("mob_filter.allow_passive", "Пассивные", true)
            .withDescription("Коровы, свиньи, курицы, NPC. Отключите для PvP");
    public final BooleanSetting allowNeutral = new BooleanSetting("mob_filter.allow_neutral", "Нейтральные", true)
            .withDescription("Волки, железные големы, пиглины. Атакуют только при провокации");

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(filterBabies, filterAdults, allowHostile, allowPassive, allowNeutral);
    }

    @Override
    public String getId() {
        return "mob_filter";
    }

    @Override
    public String getDisplayName() {
        return "Фильтр мобов";
    }

    @Override
    public Category getCategory() {
        return Category.COMBAT;
    }

    @Override
    public int tickPriority() {
        return 200;
    }

    public boolean passes(LivingEntity e) {
        if (filterBabies.get() && e.isBaby())
            return false;
        if (filterAdults.get() && !e.isBaby())
            return false;

        boolean hostile = e instanceof Monster;
        boolean passive = e instanceof Animal || e instanceof Npc;
        boolean neutral = !hostile && !passive;

        if (hostile && !allowHostile.get())
            return false;
        if (passive && !allowPassive.get())
            return false;
        if (neutral && !allowNeutral.get())
            return false;

        return true;
    }
}
