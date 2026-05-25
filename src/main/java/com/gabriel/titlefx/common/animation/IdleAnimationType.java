package com.gabriel.titlefx.common.animation;

import java.util.Locale;

public enum IdleAnimationType {
    NONE,
    SUBTLE_PULSE,
    BREATHING,
    SUBTLE_SHAKE,
    WAVE_SOFT,
    FLICKER,
    
    @Deprecated
    PULSE,
    @Deprecated
    SHAKE,
    @Deprecated
    WAVE,
    @Deprecated
    FLOAT,
    @Deprecated
    GRADIENT_SHIFT;

    public static IdleAnimationType fromString(String name) {
        try {
            return IdleAnimationType.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
