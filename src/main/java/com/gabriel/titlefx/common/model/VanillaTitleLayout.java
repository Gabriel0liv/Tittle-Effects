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
}
