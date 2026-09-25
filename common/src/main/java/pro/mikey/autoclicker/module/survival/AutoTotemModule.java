package pro.mikey.autoclicker.module.survival;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Items;
import pro.mikey.autoclicker.module.Category;
import pro.mikey.autoclicker.module.Module;
import pro.mikey.autoclicker.setting.IntSetting;
import pro.mikey.autoclicker.setting.Unit;
import pro.mikey.autoclicker.util.Inventories;

/** Keeps a Totem of Undying in the offhand. */
public class AutoTotemModule extends Module {
    /** Ticks to wait for the server to confirm a swap before trying again. */
    private static final int SWAP_COOLDOWN = 10;

    public final IntSetting healthThreshold = add(new IntSetting("health", 100, 5, 100, Unit.PERCENT));

    private int cooldown;

    public AutoTotemModule() {
        super("auto_totem", Category.SURVIVAL, true, false);
    }

    @Override
    public void stop(Minecraft mc) {
        cooldown = 0;
    }

    @Override
    public void tick(Minecraft mc) {
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        LocalPlayer player = mc.player;
        if (player.getOffhandItem().is(Items.TOTEM_OF_UNDYING) || !Inventories.canClickInventory(mc)) {
            return;
        }
        float healthPercent = player.getHealth() / player.getMaxHealth() * 100.0F;
        if (healthPercent > healthThreshold.get()) {
            return;
        }
        for (int slot = 0; slot < Inventories.MAIN_SIZE; slot++) {
            if (player.getInventory().getItem(slot).is(Items.TOTEM_OF_UNDYING)) {
                Inventories.swapWithOffhand(mc, slot);
                cooldown = SWAP_COOLDOWN;
                return;
            }
        }
    }

    @Override
    public String hudInfo() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return null;
        }
        int count = mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING) ? mc.player.getOffhandItem().getCount() : 0;
        for (int slot = 0; slot < Inventories.MAIN_SIZE; slot++) {
            if (mc.player.getInventory().getItem(slot).is(Items.TOTEM_OF_UNDYING)) {
                count += mc.player.getInventory().getItem(slot).getCount();
            }
        }
        return Integer.toString(count);
    }
}
