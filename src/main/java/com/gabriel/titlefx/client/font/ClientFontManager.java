package com.gabriel.titlefx.client.font;

import net.minecraft.resources.ResourceLocation;

public class ClientFontManager {
    private static final ResourceLocation DEFAULT_FONT = new ResourceLocation("minecraft", "default");

    public static ResourceLocation getFont(String fontId) {
        if (fontId == null || fontId.trim().isEmpty() || "minecraft:default".equals(fontId)) {
            return DEFAULT_FONT;
        }
        if (!ClientFontCache.isFontAvailable(fontId)) {
            return DEFAULT_FONT;
        }
        try {
            if (!fontId.contains(":")) {
                return new ResourceLocation("titlefx", fontId);
            }
            return new ResourceLocation(fontId);
        } catch (Exception e) {
            return DEFAULT_FONT;
        }
    }
}
