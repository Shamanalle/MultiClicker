package pro.mikey.autoclicker.modules.automation;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import pro.mikey.autoclicker.core.Module;
import pro.mikey.autoclicker.core.Setting;
import pro.mikey.autoclicker.core.Setting.*;
import pro.mikey.autoclicker.util.InventoryUtils;

import java.util.List;

/**
 * Smart Offhand: priority-based offhand item management.
 */
public class SmartOffhandModule implements Module {

    public final BooleanSetting enabled = new BooleanSetting("smart_offhand.enabled", "Smart Offhand", false)
            .withDescription("Умное управление оффхендом с приоритетами: Тотем > Еда > Факел > Щит");
    public final IntSetting totemHp = new IntSetting("smart_offhand.totem_hp", "Тотем HP %", 30, 1, 99)
            .withDescription("При HP ниже этого % в оффхенд идёт Тотем. Высший приоритет");
    public final IntSetting foodHunger = new IntSetting("smart_offhand.food_hunger", "Еда: порог голода", 14, 1, 20)
            .withDescription("При голоде ≤ порога помещает еду в оффхенд. Второй приоритет после тотема");
    public final BooleanSetting shieldDefault = new BooleanSetting("smart_offhand.shield", "Щит по умолч.", true)
            .withDescription("Если ничего более приоритетного — щит в оффхенд");
    public final BooleanSetting torchOnPickaxe = new BooleanSetting("smart_offhand.torch", "Факел + Кирка", true)
            .withDescription("Авто-факел в оффхенд, когда кирка в основной руке. Удобно при копке");

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(enabled, totemHp, foodHunger, shieldDefault, torchOnPickaxe);
    }

    private int cooldown = 0;

    @Override
    public String getId() {
        return "smart_offhand";
    }

    @Override
    public String getDisplayName() {
        return "Smart Offhand";
    }

    @Override
    public Category getCategory() {
        return Category.AUTOMATION;
    }

    @Override
    public int tickPriority() {
        return 20;
    }

    @Override
    public void onDisable() {
        cooldown = 0;
    }

    @Override
    public boolean onTick(Minecraft mc) {
        if (!enabled.get() || mc.player == null)
            return false;
        if (cooldown > 0) {
            cooldown--;
            return false;
        }

        // Yield if AutoTotem is currently handling the offhand
        var autoTotem = pro.mikey.autoclicker.AutoClicker.getInstance()
                .getModuleManager().<AutoTotemModule>get("auto_totem").orElse(null);
        if (autoTotem != null && autoTotem.isBusy()) {
            return false;
        }

        var player = mc.player;
        float hp = player.getHealth() / player.getMaxHealth() * 100f;
        int hunger = player.getFoodData().getFoodLevel();

        net.minecraft.world.item.Item desired = null;
        int desiredSlot = -1;

        if (hp <= totemHp.get()) {
            int slot = InventoryUtils.findItem(player, Items.TOTEM_OF_UNDYING);
            if (slot != -1) {
                desired = Items.TOTEM_OF_UNDYING;
                desiredSlot = slot;
            }
        }
        if (desired == null && hunger <= foodHunger.get()) {
            for (int i = 0; i < 36; i++) {
                ItemStack s = player.getInventory().getItem(i);
                if (!s.isEmpty() && AutoEatModule.isFoodItem(s)) {
                    desired = s.getItem();
                    desiredSlot = i;
                    break;
                }
            }
        }
        if (desired == null && torchOnPickaxe.get() && isPickaxe(player.getMainHandItem().getItem())) {
            int slot = InventoryUtils.findItem(player, Items.TORCH);
            if (slot != -1) {
                desired = Items.TORCH;
                desiredSlot = slot;
            }
        }
        if (desired == null && shieldDefault.get()) {
            int slot = InventoryUtils.findItem(player, Items.SHIELD);
            if (slot != -1) {
                desired = Items.SHIELD;
                desiredSlot = slot;
            }
        }

        if (desired == null)
            return false;
        ItemStack current = player.getOffhandItem();
        if (!current.isEmpty() && current.getItem() == desired)
            return false;

        InventoryUtils.swapToOffhand(mc, desiredSlot);
        cooldown = 20;
        return false;
    }

    private static boolean isPickaxe(net.minecraft.world.item.Item item) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath().contains("pickaxe");
    }
}
