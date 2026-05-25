package com.gabriel.titlefx.common.animation;

import java.util.Locale;

public enum OutAnimationType {
    NONE,
    FADE_OUT,
    DISSOLVE,
    SHRINK_FADE,
    
    @Deprecated
    SLIDE_UP_OUT,
    @Deprecated
    SLIDE_DOWN_OUT,
    @Deprecated
    SLIDE_LEFT_OUT,
    @Deprecated
    SLIDE_RIGHT_OUT,
    @Deprecated
    ZOOM_OUT,
    @Deprecated
    POP_OUT;

    public static OutAnimationType fromString(String name) {
        try {
            return OutAnimationType.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
