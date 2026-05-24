package com.gabriel.titlefx.common.animation;

import java.util.Locale;

public enum InAnimationType {
    NONE,
    FADE_IN,
    SLIDE_UP,
    SLIDE_DOWN,
    SLIDE_LEFT,
    SLIDE_RIGHT,
    ZOOM_IN,
    ZOOM_OUT,
    POP_IN;

    public static InAnimationType fromString(String name) {
        try {
            return InAnimationType.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
