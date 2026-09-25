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
        IDLE, WAIT_CONFIRMATION
    }

    private State state = State.IDLE;
    private int cooldown = 0;
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
        return 15;
    }

    public boolean isBusy() {
        return enabled.get() && (state != State.IDLE || cooldown > 0);
    }

    @Override
    public void onDisable() {
        state = State.IDLE;
        cooldown = 0;
        lastOffhand = null;
    }

    @Override
    public boolean onTick(Minecraft mc) {
        if (!enabled.get() || mc.player == null)
            return false;

        if (cooldown > 0) {
            cooldown--;
        }

        var offhand = mc.player.getOffhandItem();
        boolean totemInOffhand = offhand.getItem() == Items.TOTEM_OF_UNDYING;
        boolean totemConsumed = (lastOffhand == Items.TOTEM_OF_UNDYING && !totemInOffhand);
        lastOffhand = offhand.isEmpty() ? null : offhand.getItem();

        if (totemInOffhand) {
            state = State.IDLE;
            return false;
        }

        if (cooldown > 0) {
            return false;
        }

        float hp = mc.player.getHealth() / mc.player.getMaxHealth() * 100f;
        if (hp <= hpThreshold.get() || totemConsumed) {
            int slot = InventoryUtils.findItem(mc.player, Items.TOTEM_OF_UNDYING);
            if (slot != -1) {
                InventoryUtils.swapToOffhand(mc, slot);
                cooldown = 8; // Debounce to allow server round-trip packet sync
                state = State.WAIT_CONFIRMATION;
                return true;
            }
        }
        return false;
    }
}
