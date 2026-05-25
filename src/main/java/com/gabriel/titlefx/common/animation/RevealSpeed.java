package com.gabriel.titlefx.common.animation;

import java.util.Locale;

public enum RevealSpeed {

    INSTANT("Instantâneo", 0.0f),
    FAST("Rápido", 0.7f),
    NORMAL("Normal", 1.0f),
    CINEMATIC("Cinemático", 1.35f),
    SLOW("Lento", 1.75f),
    CUSTOM("Personalizado", -1.0f);

    private final String label;
    private final float multiplier;

    RevealSpeed(String label, float multiplier) {
        this.label = label;
        this.multiplier = multiplier;
    }

    /** PT-BR display name for UI. */
    public String getLabel() {
        return label;
    }

    /**
     * Speed multiplier applied to the base duration.
     * Returns -1 for CUSTOM (use raw durationMs instead).
     */
    public float getMultiplier() {
        return multiplier;
    }

    /** Case-insensitive parse; falls back to NORMAL on unknown input. */
    public static RevealSpeed fromString(String name) {
        if (name == null || name.isEmpty()) return NORMAL;
        try {
            return RevealSpeed.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return NORMAL;
        }
    }
}
