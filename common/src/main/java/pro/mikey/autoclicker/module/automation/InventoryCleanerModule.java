package pro.mikey.autoclicker.module.automation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import pro.mikey.autoclicker.module.Category;
import pro.mikey.autoclicker.module.Module;
import pro.mikey.autoclicker.setting.BoolSetting;
import pro.mikey.autoclicker.setting.IntSetting;
import pro.mikey.autoclicker.setting.ListSetting;
import pro.mikey.autoclicker.setting.Unit;
import pro.mikey.autoclicker.util.Inventories;

import java.util.List;

/** Throws away listed junk items (one stack at a time) to keep the inventory from filling up. */
public class InventoryCleanerModule extends Module {
    public final ListSetting items = add(new ListSetting("items", ListSetting.Kind.ITEM,
            List.of("minecraft:rotten_flesh", "minecraft:poisonous_potato")));
    public final BoolSetting keepHotbar = add(new BoolSetting("keep_hotbar", true));
    public final IntSetting delay = add(new IntSetting("delay", 10, 1, 100, Unit.TICKS));

    private int cooldown;

    public InventoryCleanerModule() {
        super("inventory_cleaner", Category.AUTOMATION, true, false);
    }

    @Override
    public void tick(Minecraft mc) {
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        LocalPlayer player = mc.player;
        if (player.isCreative() || items.get().isEmpty() || !Inventories.canClickInventory(mc)) {
            return;
        }
        int first = keepHotbar.get() ? Inventories.HOTBAR_SIZE : 0;
        for (int slot = first; slot < Inventories.MAIN_SIZE; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && items.contains(stack.getItem())) {
                Inventories.dropStack(mc, slot);
                cooldown = delay.get();
                return;
            }
        }
    }
}
