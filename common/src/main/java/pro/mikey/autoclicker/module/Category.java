package pro.mikey.autoclicker.module;

import net.minecraft.network.chat.Component;

import java.util.Locale;

public enum Category {
    CLICKER("✦"),
    COMBAT("⚔"),
    SURVIVAL("❤"),
    AUTOMATION("⚙"),
    MINING("⛏"),
    VISUAL("◈");

    private final String icon;

    Category(String icon) {
        this.icon = icon;
    }

    public String icon() {
        return icon;
    }

    public Component title() {
        return Component.translatable("multiclicker.category." + name().toLowerCase(Locale.ROOT));
    }
}
