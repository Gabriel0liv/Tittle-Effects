package com.gabriel.titlefx.common.animation;

import java.util.Locale;

public enum LockMode {
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
    RANDOM,
    CENTER_OUT,
    EDGES_IN,
    WORD_BY_WORD;

    public static LockMode fromString(String name) {
        try {
            return LockMode.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return LEFT_TO_RIGHT;
        }
    }
}
