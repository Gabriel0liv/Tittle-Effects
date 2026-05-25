package com.gabriel.titlefx.client;

import com.gabriel.titlefx.client.font.ClientFontCache;
import com.gabriel.titlefx.client.render.AnimatedTextManager;
import com.gabriel.titlefx.common.model.AnimatedTextPayload;
import com.gabriel.titlefx.common.network.FontRegistrySyncPacket;
import com.gabriel.titlefx.common.network.FontChunkPacket;

public class ClientPacketHandler {
    public static void handleShow(AnimatedTextPayload payload) {
        AnimatedTextManager.getInstance().showText(payload);
    }

    public static void handleClear(String clearType) {
        AnimatedTextManager.getInstance().clearText(clearType);
    }

    public static void handleRegistrySync(FontRegistrySyncPacket packet) {
        ClientFontCache.handleRegistrySync(packet.getRegistryHash(), packet.getServerHash(), packet.getFonts());
    }

    public static void handleFontChunk(FontChunkPacket packet) {
        ClientFontCache.handleFontChunk(
            packet.getTransferId(),
            packet.getFontId(),
            packet.getSha256(),
            packet.getChunkIndex(),
            packet.getTotalChunks(),
            packet.getData()
        );
    }
}
