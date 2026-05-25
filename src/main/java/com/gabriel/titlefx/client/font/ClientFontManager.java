package com.gabriel.titlefx.client.font;

import net.minecraft.resources.ResourceLocation;

public class ClientFontManager {
    private static final ResourceLocation DEFAULT_FONT = new ResourceLocation("minecraft", "default");

    private static final java.util.Set<String> loggedFallbacks = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static ResourceLocation getFont(String fontId) {
        if (fontId == null || fontId.trim().isEmpty() || "minecraft:default".equals(fontId)) {
            return DEFAULT_FONT;
        }
        if (!ClientFontCache.isFontLoaded(fontId)) {
            if (loggedFallbacks.add(fontId)) {
                com.gabriel.titlefx.TitleFxMod.LOGGER.info("Font not loaded yet, falling back to minecraft:default: " + fontId);
            }
            return DEFAULT_FONT;
        }
        loggedFallbacks.remove(fontId);
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
