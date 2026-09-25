package pro.mikey.autoclicker.modules.automation;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;
import pro.mikey.autoclicker.core.Module;
import pro.mikey.autoclicker.core.Setting;
import pro.mikey.autoclicker.core.Setting.*;
import pro.mikey.autoclicker.util.InventoryUtils;

import java.util.List;

/**
 * Auto-Totem: places a Totem of Undying in offhand when HP is low.
 */
public class AutoTotemModule implements Module {

    public final BooleanSetting enabled = new BooleanSetting("auto_totem.enabled", "Авто-Тотем", false)
            .withDescription("Автоматически перемещает Тотем Бессмертия в оффхенд при низком HP");
    public final IntSetting hpThreshold = new IntSetting("auto_totem.hp_threshold", "Порог HP %", 30, 1, 99)
            .withDescription(
                    "При HP ниже этого % перемещает Тотем Бессмертия в оффхенд. Также срабатывает при использовании");

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(enabled, hpThreshold);
    }

    private enum State {
        IDLE, PLACE_OFFHAND, DONE
    }

    private State state = State.IDLE;
    private int foundSlot = -1;
    private net.minecraft.world.item.Item lastOffhand = null;

    @Override
    public String getId() {
        return "auto_totem";
    }

    @Override
    public String getDisplayName() {
        return "Авто-Тотем";
    }

    @Override
    public Category getCategory() {
        return Category.AUTOMATION;
    }

    @Override
    public int tickPriority() {
        return 25;
    }

    @Override
    public void onDisable() {
        state = State.IDLE;
        foundSlot = -1;
        lastOffhand = null;
    }

    @Override
    public boolean onTick(Minecraft mc) {
        if (!enabled.get() || mc.player == null)
            return false;

        var offhand = mc.player.getOffhandItem();
        boolean totemInOffhand = offhand.getItem() == Items.TOTEM_OF_UNDYING;
        boolean totemConsumed = (lastOffhand == Items.TOTEM_OF_UNDYING && !totemInOffhand);
        lastOffhand = offhand.isEmpty() ? null : offhand.getItem();

        switch (state) {
            case IDLE -> {
                if (totemInOffhand)
                    return false;
                float hp = mc.player.getHealth() / mc.player.getMaxHealth() * 100f;
                if (hp <= hpThreshold.get() || totemConsumed) {
                    int slot = InventoryUtils.findItem(mc.player, Items.TOTEM_OF_UNDYING);
                    if (slot == -1)
                        return false;
                    foundSlot = slot;
                    state = State.PLACE_OFFHAND;
                }
            }
            case PLACE_OFFHAND -> {
                InventoryUtils.swapToOffhand(mc, foundSlot);
                foundSlot = -1;
                state = State.DONE;
            }
            case DONE -> state = State.IDLE;
        }
        return false;
    }
}
