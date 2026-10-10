package io.github.shamanalle.multiclicker.stats;

import java.util.Set;

/** What a fishing catch was, after the vanilla loot tables. */
public enum LootKind {
    FISH(Stat.FISH),
    TREASURE(Stat.TREASURE),
    JUNK(Stat.JUNK),
    /** Anything a datapack or a server plugin added. */
    OTHER(Stat.OTHER_LOOT);

    private static final Set<String> FISH_ITEMS = Set.of("minecraft:cod", "minecraft:salmon",
            "minecraft:tropical_fish", "minecraft:pufferfish");
    private static final Set<String> TREASURE_ITEMS = Set.of("minecraft:bow", "minecraft:enchanted_book",
            "minecraft:name_tag", "minecraft:nautilus_shell", "minecraft:saddle");
    private static final Set<String> JUNK_ITEMS = Set.of("minecraft:lily_pad", "minecraft:bowl",
            "minecraft:leather", "minecraft:leather_boots", "minecraft:rotten_flesh", "minecraft:stick",
            "minecraft:string", "minecraft:potion", "minecraft:bone", "minecraft:ink_sac",
            "minecraft:tripwire_hook", "minecraft:bamboo", "minecraft:cocoa_beans");

    private final Stat stat;

    LootKind(Stat stat) {
        this.stat = stat;
    }

    public Stat stat() {
        return stat;
    }

    /**
     * Sorts a caught item. A fishing rod is treasure when enchanted and junk otherwise, as in the
     * vanilla loot tables; a bow or a book is treasure either way.
     */
    public static LootKind classify(String itemId, boolean enchanted) {
        if (itemId.equals("minecraft:fishing_rod")) {
            return enchanted ? TREASURE : JUNK;
        }
        if (FISH_ITEMS.contains(itemId)) {
            return FISH;
        }
        if (TREASURE_ITEMS.contains(itemId)) {
            return TREASURE;
        }
        if (JUNK_ITEMS.contains(itemId)) {
            return JUNK;
        }
        return OTHER;
    }
}
