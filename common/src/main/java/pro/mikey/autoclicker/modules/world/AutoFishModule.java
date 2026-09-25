package pro.mikey.autoclicker.modules.world;

import net.minecraft.client.Minecraft;
import pro.mikey.autoclicker.core.EventBus;
import pro.mikey.autoclicker.core.Module;
import pro.mikey.autoclicker.core.Setting;
import pro.mikey.autoclicker.core.Setting.*;

import java.util.List;
import java.util.Random;

/**
 * Auto-Fish: detects bite (via EventBus) and reels/recasts automatically.
 */
public class AutoFishModule implements Module {

    public final BooleanSetting enabled = new BooleanSetting("auto_fish.enabled", "Авто-Рыбалка", false)
            .withDescription("Автоматически подсекает и перезабрасывает удочку при поклёвке");
    public final IntSetting reactDelay = new IntSetting("auto_fish.react_delay", "Задержка реакции", 4, 1, 20)
            .withDescription("Тики между поклёвкой и подсечкой. Слишком быстро = подозрительно");
    public final BooleanSetting randomize = new BooleanSetting("auto_fish.randomize", "Рандом задержки", false)
            .withDescription("Добавляет случайность к времени реакции. Рекомендуется для обхода античита");
    public final IntSetting randomRange = new IntSetting("auto_fish.random_range", "Диапазон рандома", 6, 0, 40)
            .withDescription("Макс. дополнительная задержка в тиках поверх базовой");
    public final IntSetting fishLimit = new IntSetting("auto_fish.fish_limit", "Лимит рыбы", 0, 0, 1000)
            .withDescription("Поймать N рыб и выключиться. 0 = бесконечно");
    public final BooleanSetting catchSound = new BooleanSetting("auto_fish.catch_sound", "Звук улова", false)
            .withDescription("Проигрывает звук XP при каждом улове. Слышно когда отвлёкся");

    private int catchCount = 0;

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(enabled, reactDelay, randomize, randomRange, fishLimit, catchSound);
    }

    private static final Random RANDOM = new Random();

    private enum State {
        IDLE, WAITING_BITE, BITE_DETECTED, REELING, RECASTING
    }

    private State state = State.IDLE;
    private int timer = 0;
    private boolean biteDetected = false;

    @Override
    public String getId() {
        return "auto_fish";
    }

    @Override
    public String getDisplayName() {
        return "Авто-Рыбалка";
    }

    @Override
    public Category getCategory() {
        return Category.WORLD;
    }

    @Override
    public int tickPriority() {
        return 40;
    }

    @Override
    public void onInit(Minecraft mc) {
        EventBus.get().subscribe(EventBus.FishBiteEvent.class, e -> biteDetected = true);
    }

    @Override
    public void onDisable() {
        state = State.IDLE;
        timer = 0;
        biteDetected = false;
        catchCount = 0;
    }

    @Override
    public boolean onTick(Minecraft mc) {
        if (!enabled.get() || mc.player == null)
            return false;

        if (mc.gui.screen() != null)
            return false;

        boolean hasRod = mc.player.getMainHandItem().getItem() instanceof net.minecraft.world.item.FishingRodItem
                || mc.player.getOffhandItem().getItem() instanceof net.minecraft.world.item.FishingRodItem;
        if (!hasRod) {
            state = State.IDLE;
            return false;
        }

        switch (state) {
            case IDLE -> {
                if (mc.player.fishing != null) {
                    state = State.WAITING_BITE;
                }
            }
            case WAITING_BITE -> {
                if (mc.player.fishing == null) {
                    state = State.IDLE;
                    return false;
                }
                if (biteDetected) {
                    biteDetected = false;
                    state = State.BITE_DETECTED;
                    int delay = reactDelay.get();
                    if (randomize.get())
                        delay += RANDOM.nextInt(randomRange.get() + 1);
                    timer = delay;
                }
            }
            case BITE_DETECTED -> {
                timer--;
                if (timer <= 0) {
                    mc.options.keyUse.setDown(true);
                    state = State.REELING;
                    timer = 2;
                }
                return true;
            }
            case REELING -> {
                timer--;
                if (timer <= 0) {
                    mc.options.keyUse.setDown(false);
                    catchCount++;
                    if (catchSound.get() && mc.player != null) {
                        mc.player.playSound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
                    }
                    if (fishLimit.get() > 0 && catchCount >= fishLimit.get()) {
                        enabled.set(false);
                        state = State.IDLE;
                        catchCount = 0;
                        return true;
                    }
                    state = State.RECASTING;
                    timer = 15;
                }
                return true;
            }
            case RECASTING -> {
                timer--;
                if (timer == 5)
                    mc.options.keyUse.setDown(true);
                if (timer == 3)
                    mc.options.keyUse.setDown(false);
                if (timer <= 0) {
                    state = State.WAITING_BITE;
                }
                return true;
            }
        }
        return false;
    }
}
