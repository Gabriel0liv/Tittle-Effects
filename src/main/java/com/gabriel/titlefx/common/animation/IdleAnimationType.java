package com.gabriel.titlefx.common.animation;

import java.util.Locale;

public enum IdleAnimationType {
    NONE,
    PULSE,
    SHAKE,
    WAVE,
    FLOAT,
    GRADIENT_SHIFT,
    FLICKER;

    public static IdleAnimationType fromString(String name) {
        try {
            return IdleAnimationType.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
