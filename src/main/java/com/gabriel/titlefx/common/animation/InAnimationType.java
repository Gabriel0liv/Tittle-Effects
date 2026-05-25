package com.gabriel.titlefx.common.animation;

import java.util.Locale;

public enum InAnimationType {
    NONE,
    FADE_IN,
    CINEMATIC_ZOOM_IN,
    SOFT_POP,
    SCALE_REVEAL,
    
    @Deprecated
    SLIDE_UP,
    @Deprecated
    SLIDE_DOWN,
    @Deprecated
    SLIDE_LEFT,
    @Deprecated
    SLIDE_RIGHT,
    @Deprecated
    ZOOM_IN,
    @Deprecated
    ZOOM_OUT,
    @Deprecated
    POP_IN;

    public static InAnimationType fromString(String name) {
        try {
            return InAnimationType.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
