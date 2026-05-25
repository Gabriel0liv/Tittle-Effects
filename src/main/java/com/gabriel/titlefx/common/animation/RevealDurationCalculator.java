package com.gabriel.titlefx.common.animation;

/**
 * Calculates effective reveal duration based on RevealType, RevealSpeed, and text content.
 * Called by RevealPayload.effectiveDurationMs() for all speeds except CUSTOM.
 */
public class RevealDurationCalculator {

    /**
     * Returns the effective reveal duration in milliseconds.
     *
     * @param type  the reveal animation type
     * @param speed the chosen speed preset (must not be CUSTOM)
     * @param text  the full text string being revealed (used for length-based types)
     * @return effective duration in ms, clamped to the type's [min, max] range
     */
    public static int calculate(RevealType type, RevealSpeed speed, String text) {
        if (type == null || type == RevealType.NONE) return 0;
        if (speed == null || speed == RevealSpeed.INSTANT) return 0;

        float multiplier = speed.getMultiplier();

        switch (type) {
            case TYPEWRITER: {
                int chars = countNonSpace(text);
                float base = chars * 45f;
                return clamp(Math.round(base * multiplier), 600, 3500);
            }
            case WORD_BY_WORD: {
                int words = countWords(text);
                float base = words * 220f;
                return clamp(Math.round(base * multiplier), 700, 3500);
            }
            case GLYPH_SCRAMBLE:
                return clamp(Math.round(1500f * multiplier), 700, 3500);
            case OBFUSCATED_DECODE:
                return clamp(Math.round(1900f * multiplier), 900, 4200);
            case CENTER_OUT:
                return clamp(Math.round(1500f * multiplier), 700, 3200);
            case FADE_CHARS:
                return clamp(Math.round(1400f * multiplier), 700, 3200);
            case RANDOM_FADE:
                return clamp(Math.round(1600f * multiplier), 700, 3500);
            case WIPE_LEFT_TO_RIGHT:
                return clamp(Math.round(1300f * multiplier), 600, 3000);
            default:
                return 0;
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /** Counts non-whitespace characters (spaces and newlines excluded). */
    private static int countNonSpace(String text) {
        if (text == null || text.isEmpty()) return 1;
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != ' ' && c != '\n') count++;
        }
        return Math.max(1, count);
    }

    /** Counts words separated by whitespace. */
    private static int countWords(String text) {
        if (text == null || text.isEmpty()) return 1;
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return 1;
        return trimmed.split("\\s+").length;
    }
}
