package pro.mikey.autoclicker.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import pro.mikey.autoclicker.core.EventBus;
import pro.mikey.autoclicker.core.Module;
import pro.mikey.autoclicker.core.Setting;
import pro.mikey.autoclicker.core.Setting.*;

import java.util.List;

/**
 * Combat Feedback module: plays configurable sounds on hit and kill events.
 */
public class CombatFeedbackModule implements Module {

    // ── Enums ──
    public enum SoundType {
        XP_ORB("Опыт"), PLING("Плинг"), ANVIL("Наковальня");

        public final String label;

        SoundType(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }

        public SoundEvent toSoundEvent() {
            return switch (this) {
                case XP_ORB -> SoundEvents.EXPERIENCE_ORB_PICKUP;
                case PLING -> SoundEvents.NOTE_BLOCK_PLING.value();
                case ANVIL -> SoundEvents.ANVIL_USE;
            };
        }
    }

    public enum KillSoundType {
        LEVEL_UP("Уровень"), FIREWORK("Фейерверк"), TOTEM("Тотем");

        public final String label;

        KillSoundType(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }

        public SoundEvent toSoundEvent() {
            return switch (this) {
                case LEVEL_UP -> SoundEvents.PLAYER_LEVELUP;
                case FIREWORK -> SoundEvents.FIREWORK_ROCKET_LAUNCH;
                case TOTEM -> SoundEvents.TOTEM_USE;
            };
        }
    }

    public final BooleanSetting hitSoundEnabled = new BooleanSetting("combat_fb.hit_sound", "Звук удара", false)
            .withGroup("🔊 Звук удара").withDescription("Проигрывает звук при каждом попадании. Слышите только вы");
    public final FloatSetting hitSoundVolume = new FloatSetting("combat_fb.hit_volume", "Громкость удара", 0.8, 0.0,
            1.0).withGroup("🔊 Звук удара").withDescription("Громкость от 0 (тихо) до 1 (максимум). Слышите только вы");
    public final EnumSetting<SoundType> hitSoundType = new EnumSetting<>("combat_fb.hit_type", "Тип звука",
            SoundType.XP_ORB, SoundType.class).withGroup("🔊 Звук удара")
            .withDescription("Опыт = короткий звон, Плинг = нотный блок, Наковальня = тяжёлый удар");
    public final BooleanSetting killSoundEnabled = new BooleanSetting("combat_fb.kill_sound", "Звук убийства", false)
            .withGroup("💀 Звук убийства").withDescription("Проигрывает звук при убийстве моба/игрока");
    public final FloatSetting killSoundVolume = new FloatSetting("combat_fb.kill_volume", "Громкость убийства", 1.0,
            0.0, 1.0).withGroup("💀 Звук убийства").withDescription("Громкость звука убийства. 0 = беззвучно");
    public final EnumSetting<KillSoundType> killSoundType = new EnumSetting<>("combat_fb.kill_type", "Тип звука",
            KillSoundType.LEVEL_UP, KillSoundType.class).withGroup("💀 Звук убийства")
            .withDescription("Уровень = левелап, Фейерверк = запуск ракеты, Тотем = эпичный звук");

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(hitSoundEnabled, hitSoundVolume, hitSoundType,
                killSoundEnabled, killSoundVolume, killSoundType);
    }

    @Override
    public String getId() {
        return "combat_feedback";
    }

    @Override
    public String getDisplayName() {
        return "Combat Feedback";
    }

    @Override
    public Category getCategory() {
        return Category.COMBAT;
    }

    @Override
    public int tickPriority() {
        return 200;
    }

    @Override
    public void onInit(Minecraft mc) {
        EventBus.get().subscribe(EventBus.AttackEvent.class, e -> playHitSound());
        EventBus.get().subscribe(EventBus.EntityDeathEvent.class, e -> playKillSound());
    }

    public void playHitSound() {
        if (!hitSoundEnabled.get())
            return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
            return;
        mc.player.playSound(hitSoundType.get().toSoundEvent(), hitSoundVolume.get().floatValue(), 1.2f);
    }

    public void playKillSound() {
        if (!killSoundEnabled.get())
            return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
            return;
        mc.player.playSound(killSoundType.get().toSoundEvent(), killSoundVolume.get().floatValue(), 1.0f);
    }
}
