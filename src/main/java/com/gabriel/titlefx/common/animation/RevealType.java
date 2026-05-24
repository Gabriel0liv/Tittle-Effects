package com.gabriel.titlefx.common.animation;

import java.util.Locale;

public enum RevealType {
    NONE,
    TYPEWRITER,
    WORD_BY_WORD,
    LINE_BY_LINE,
    OBFUSCATED_DECODE,
    GLYPH_SCRAMBLE;

    public static RevealType fromString(String name) {
        try {
            return RevealType.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
