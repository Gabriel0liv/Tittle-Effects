package com.gabriel.titlefx.common.model;

import java.util.Locale;

public class TextDefaults {
    public static float getDefaultScale(String type) {
        String t = type.toLowerCase(Locale.ROOT);
        if ("title".equals(t)) {
            return 4.0f;
        } else if ("subtitle".equals(t)) {
            return 2.0f;
        } else if ("actionbar".equals(t)) {
            return 1.0f;
        }
        return 1.0f;
    }

    public static PositionPayload getDefaultPosition(String type) {
        String t = type.toLowerCase(Locale.ROOT);
        if ("subtitle".equals(t)) {
            return new PositionPayload("center", 0, 10, "center");
        } else if ("actionbar".equals(t)) {
            return new PositionPayload("bottom", 0, -59, "center");
        } else {
            // title and custom default
            return new PositionPayload("center", 0, -40, "center");
        }
    }

    public static int getDefaultDuration(String type) {
        String t = type.toLowerCase(Locale.ROOT);
        if ("actionbar".equals(t)) {
            return 2000;
        }
        return 3500;
    }
}
