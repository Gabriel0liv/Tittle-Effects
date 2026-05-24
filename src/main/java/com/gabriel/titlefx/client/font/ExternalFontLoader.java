package com.gabriel.titlefx.client.font;

import com.gabriel.titlefx.TitleFxMod;

public class ExternalFontLoader {
    public static void init() {
        TitleFxMod.LOGGER.info("ExternalFontLoader: Font loading from config/titlefx/fonts/ is deferred to post-MVP.");
    }

    public static void reload() {
        TitleFxMod.LOGGER.info("ExternalFontLoader: Fonts reload is deferred to post-MVP.");
    }
}
