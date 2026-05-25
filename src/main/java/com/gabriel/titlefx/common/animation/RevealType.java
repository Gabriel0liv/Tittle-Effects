package com.gabriel.titlefx.common.animation;

import java.util.Locale;

public enum RevealType {
    NONE,
    TYPEWRITER,
    WORD_BY_WORD,
    GLYPH_SCRAMBLE,
    OBFUSCATED_DECODE,
    CENTER_OUT,
    WIPE_LEFT_TO_RIGHT,
    FADE_CHARS,
    RANDOM_FADE,
    
    @Deprecated
    LINE_BY_LINE;

    public static RevealType fromString(String name) {
        try {
            return RevealType.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
