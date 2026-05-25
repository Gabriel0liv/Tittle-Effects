package com.gabriel.titlefx.common.model;

import java.util.Locale;

public class TextDefaults {
    public static float getDefaultScale(String type) {
        return VanillaTitleLayout.getDefaultScale(type);
    }

    public static PositionPayload getDefaultPosition(String type) {
        return VanillaTitleLayout.getDefaultPosition(type);
    }

    public static int getDefaultDuration(String type) {
        String t = type.toLowerCase(Locale.ROOT);
        if ("actionbar".equals(t)) {
            return 2000;
        }
        return 3500;
    }
}
