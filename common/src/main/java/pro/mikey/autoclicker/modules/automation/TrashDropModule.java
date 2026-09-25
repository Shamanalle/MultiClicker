package pro.mikey.autoclicker.modules.automation;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.ClickType;
import pro.mikey.autoclicker.core.Module;
import pro.mikey.autoclicker.core.Setting;
import pro.mikey.autoclicker.core.Setting.*;

import java.util.List;

/**
 * Trash Auto-Drop: automatically drops items from a configured list.
 * Uses vanilla container click protocol (THROW) for safe server sync.
 */
public class TrashDropModule implements Module {

    public enum DropMode {
        BLACKLIST("Blacklist"), WHITELIST("Whitelist");

        public final String label;

        DropMode(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public final BooleanSetting enabled = new BooleanSetting("trash_drop.enabled", "Авто-Дроп", false)
            .withDescription("Автоматически выбрасывает мусор из инвентаря раз в секунду");
    public final EnumSetting<DropMode> mode = new EnumSetting<>("trash_drop.mode", "Режим", DropMode.BLACKLIST,
            DropMode.class)
            .withDescription("Blacklist = выбрасывает из списка. Whitelist = выбрасывает всё, кроме списка");
    public final IntSetting maxDropsPerCycle = new IntSetting("trash_drop.max_drops", "Макс. дропов", 1, 1, 5)
            .withDescription("Макс. предметов за цикл. Больше = быстрее, но больше пакетов серверу");
    public final StringListSetting items = new StringListSetting("trash_drop.items", "Список предметов",
            new java.util.ArrayList<>())
            .withDescription("Идентификаторы предметов (cobblestone, dirt, gravel). Точное совпадение");
    public final IntSetting keepMinStack = new IntSetting("trash_drop.keep_min", "Минимум стака", 0, 0, 64)
            .withDescription("Не выбрасывать предмет если в стаке ≤ N штук. 0 = выбрасывать всё");

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(enabled, mode, maxDropsPerCycle, items, keepMinStack);
    }

    private int timer = 0;

    @Override
    public String getId() {
        return "trash_drop";
    }

    @Override
    public String getDisplayName() {
        return "Авто-Дроп";
    }

    @Override
    public Category getCategory() {
        return Category.AUTOMATION;
    }

    @Override
    public int tickPriority() {
        return 80;
    }

    @Override
    public void onDisable() {
        timer = 0;
    }

    @Override
    public boolean onTick(Minecraft mc) {
        if (!enabled.get() || mc.player == null || mc.gameMode == null)
            return false;
        if (items.get().isEmpty())
            return false;

        timer++;
        if (timer < 20)
            return false; // once per second
        timer = 0;

        int dropped = 0;
        int maxDrops = maxDropsPerCycle.get();

        for (int i = 0; i < 36 && dropped < maxDrops; i++) {
            var stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty())
                continue;
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            boolean inList = items.get().stream().anyMatch(entry -> itemId.equals(entry.trim()));
            boolean shouldDrop = (mode.get() == DropMode.BLACKLIST) ? inList : !inList;
            if (shouldDrop) {
                // #11 keepMinStack: skip if stack count <= minimum
                if (keepMinStack.get() > 0 && stack.getCount() <= keepMinStack.get()) {
                    continue;
                }
                // Convert inventory slot to container slot for player inventory menu
                int containerSlot = (i < 9) ? (36 + i) : i;
                // Use Q-key throw via container click (vanilla protocol, server-safe)
                mc.gameMode.handleInventoryMouseClick(
                        mc.player.inventoryMenu.containerId,
                        containerSlot,
                        1, // button=1 for full stack throw
                        ClickType.THROW,
                        mc.player);
                dropped++;
            }
        }
        return false;
    }
}
