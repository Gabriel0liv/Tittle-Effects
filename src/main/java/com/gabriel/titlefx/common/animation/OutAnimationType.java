package com.gabriel.titlefx.common.animation;

import java.util.Locale;

public enum OutAnimationType {
    NONE,
    FADE_OUT,
    SLIDE_UP_OUT,
    SLIDE_DOWN_OUT,
    SLIDE_LEFT_OUT,
    SLIDE_RIGHT_OUT,
    ZOOM_OUT,
    POP_OUT;

    public static OutAnimationType fromString(String name) {
        try {
            return OutAnimationType.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
