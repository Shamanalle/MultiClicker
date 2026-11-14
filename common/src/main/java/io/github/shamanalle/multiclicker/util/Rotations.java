package io.github.shamanalle.multiclicker.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/** Smooth, sensitivity-quantized view steps so automated rotations match real mouse input. */
public final class Rotations {
    private Rotations() {
    }

    /**
     * Moves {@code current} toward {@code target} by at most {@code maxStep} degrees, quantized to
     * the player's current mouse sensitivity step (GCD) so server anti-cheats do not see impossible
     * sub-pixel or single-tick 90-degree snaps.
     */
    public static float stepToward(Minecraft mc, float current, float target, float maxStep) {
        float diff = target - current;
        if (Math.abs(diff) <= 1.0E-4F) {
            return current;
        }
        float delta = Mth.clamp(diff, -maxStep, maxStep);
        float gcd = gcdStep(mc);
        if (gcd > 0.0F) {
            if (Math.abs(diff) < gcd * 0.5F) {
                return current;
            }
            delta = Math.round(delta / gcd) * gcd;
            if (delta == 0.0F) {
                delta = Math.copySign(gcd, diff);
            }
            return current + delta;
        }
        if (Math.abs(diff) <= 0.01F) {
            return target;
        }
        float next = current + delta;
        return (diff > 0.0F ? next > target : next < target) ? target : next;
    }

    public static float clampPitch(float pitch) {
        return Mth.clamp(pitch, -90.0F, 90.0F);
    }

    /** Degrees per mouse pixel from vanilla's {@code MouseHandler.turnPlayer} formula. */
    public static float gcdStep(Minecraft mc) {
        if (mc == null || mc.options == null) {
            return 0.15F;
        }
        double sens = mc.options.sensitivity().get() * 0.6D + 0.2D;
        return (float) (sens * sens * sens * 8.0D * 0.15D);
    }
}
