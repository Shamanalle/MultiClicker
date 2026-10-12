package io.github.shamanalle.multiclicker.boot;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Each variant has a mixin config of its own with an empty mixin list and this plugin; the plugin adds the
 * variant's mixins only when the variant is the one for the running loader and Minecraft version, so the configs
 * of the other variants apply nothing.
 */
public final class VariantMixinPlugin implements IMixinConfigPlugin {
    private List<String> mixins = Collections.emptyList();

    @Override
    public void onLoad(String mixinPackage) {
        String variant = Variants.variantOf(mixinPackage);
        if (variant != null && variant.equals(Variants.current()) && Variants.client()) {
            mixins = Arrays.asList(Variants.property(variant, "mixins").split(","));
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return mixins;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
