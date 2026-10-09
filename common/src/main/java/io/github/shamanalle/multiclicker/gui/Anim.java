package io.github.shamanalle.multiclicker.gui;

/** Frame-rate independent exponential smoothing towards a target value. */
public final class Anim {
    private float value;

    public Anim(float initial) {
        this.value = initial;
    }

    public float update(float target, float deltaSeconds, float speed) {
        value += (target - value) * (1.0F - (float) Math.exp(-deltaSeconds * speed));
        if (Math.abs(target - value) < 0.001F) {
            value = target;
        }
        return value;
    }

    public float value() {
        return value;
    }

    /** Jumps to a value without animating, e.g. to start an animation from there. */
    public void set(float value) {
        this.value = value;
    }
}
