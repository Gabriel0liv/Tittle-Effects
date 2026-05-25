package com.gabriel.titlefx.common.model;

import java.util.Locale;

public class VanillaTitleLayout {
    public static final float TITLE_SCALE = 4.0f;
    public static final String TITLE_ANCHOR = "center";
    public static final int TITLE_X = 0;
    public static final int TITLE_Y = -40;
    public static final String TITLE_ALIGN = "center";

    public static final float SUBTITLE_SCALE = 2.0f;
    public static final String SUBTITLE_ANCHOR = "center";
    public static final int SUBTITLE_X = 0;
    public static final int SUBTITLE_Y = 10;
    public static final String SUBTITLE_ALIGN = "center";

    public static final float ACTIONBAR_SCALE = 1.0f;
    public static final String ACTIONBAR_ANCHOR = "bottom";
    public static final int ACTIONBAR_X = 0;
    public static final int ACTIONBAR_Y = -59;
    public static final String ACTIONBAR_ALIGN = "center";

    public static float getDefaultScale(String type) {
        String t = type.toLowerCase(Locale.ROOT);
        if ("title".equals(t)) return TITLE_SCALE;
        if ("subtitle".equals(t)) return SUBTITLE_SCALE;
        if ("actionbar".equals(t)) return ACTIONBAR_SCALE;
        return 1.0f;
    }

    public static PositionPayload getDefaultPosition(String type) {
        String t = type.toLowerCase(Locale.ROOT);
        if ("subtitle".equals(t)) {
            return new PositionPayload(SUBTITLE_ANCHOR, SUBTITLE_X, SUBTITLE_Y, SUBTITLE_ALIGN);
        } else if ("actionbar".equals(t)) {
            return new PositionPayload(ACTIONBAR_ANCHOR, ACTIONBAR_X, ACTIONBAR_Y, ACTIONBAR_ALIGN);
        } else {
            return new PositionPayload(TITLE_ANCHOR, TITLE_X, TITLE_Y, TITLE_ALIGN);
        }
    }

    public static boolean isDefaultVanillaLayout(String type, float scale, PositionPayload pos) {
        if (pos == null) return true;
        String t = type.toLowerCase(Locale.ROOT);
        if ("title".equals(t)) {
            return Math.abs(scale - TITLE_SCALE) < 0.01f && 
                   TITLE_ANCHOR.equalsIgnoreCase(pos.anchor()) && 
                   pos.x() == TITLE_X && 
                   pos.y() == TITLE_Y && 
                   TITLE_ALIGN.equalsIgnoreCase(pos.alignment());
        } else if ("subtitle".equals(t)) {
            return Math.abs(scale - SUBTITLE_SCALE) < 0.01f && 
                   SUBTITLE_ANCHOR.equalsIgnoreCase(pos.anchor()) && 
                   pos.x() == SUBTITLE_X && 
                   pos.y() == SUBTITLE_Y && 
                   SUBTITLE_ALIGN.equalsIgnoreCase(pos.alignment());
        } else if ("actionbar".equals(t)) {
            return Math.abs(scale - ACTIONBAR_SCALE) < 0.01f && 
                   ACTIONBAR_ANCHOR.equalsIgnoreCase(pos.anchor()) && 
                   pos.x() == ACTIONBAR_X && 
                   pos.y() == ACTIONBAR_Y && 
                   ACTIONBAR_ALIGN.equalsIgnoreCase(pos.alignment());
        }
        return false;
    }

    public static boolean isVanillaLikeLayout(String type, PositionPayload pos) {
        if (pos == null) return true;
        String t = type.toLowerCase(Locale.ROOT);
        if ("title".equals(t) || "subtitle".equals(t)) {
            return "center".equalsIgnoreCase(pos.anchor()) && "center".equalsIgnoreCase(pos.alignment());
        } else if ("actionbar".equals(t)) {
            return "bottom".equalsIgnoreCase(pos.anchor()) && "center".equalsIgnoreCase(pos.alignment());
        }
        return false;
    }

    public static float[] resolveVanillaLikeCoordinates(String type, int screenWidth, int screenHeight, float scale, float lineWidth, int offsetX, int offsetY) {
        float x = (screenWidth - (lineWidth * scale)) / 2.0f + offsetX;
        float y = 0.0f;
        String t = type.toLowerCase(Locale.ROOT);
        if ("title".equals(t)) {
            y = (screenHeight / 2.0f) - 40.0f + (offsetY - TITLE_Y);
        } else if ("subtitle".equals(t)) {
            y = (screenHeight / 2.0f) + 10.0f + (offsetY - SUBTITLE_Y);
        } else if ("actionbar".equals(t)) {
            y = screenHeight - 59.0f + (offsetY - ACTIONBAR_Y);
        }
        return new float[]{x, y};
    }

    public static float[] resolveVanillaLikeCoordinates(String type, int screenWidth, int screenHeight, float scale, float lineWidth) {
        String t = type.toLowerCase(Locale.ROOT);
        int defaultY = "subtitle".equals(t) ? 10 : ("actionbar".equals(t) ? -59 : -40);
        return resolveVanillaLikeCoordinates(type, screenWidth, screenHeight, scale, lineWidth, 0, defaultY);
    }
}
