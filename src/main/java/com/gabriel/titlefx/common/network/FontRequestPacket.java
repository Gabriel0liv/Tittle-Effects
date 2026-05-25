package com.gabriel.titlefx.common.network;

import com.gabriel.titlefx.TitleFxMod;
import com.gabriel.titlefx.common.config.TitleFxConfig;
import com.gabriel.titlefx.common.font.FontInfo;
import com.gabriel.titlefx.common.font.ServerFontManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.io.File;
import java.util.function.Supplier;

public class FontRequestPacket {
    private final String fontId;
    private final String transferId;
    private final String sha256;

    public FontRequestPacket(String fontId, String transferId, String sha256) {
        this.fontId = fontId;
        this.transferId = transferId;
        this.sha256 = sha256;
    }

    public String getFontId() {
        return fontId;
    }

    public String getTransferId() {
        return transferId;
    }

    public String getSha256() {
        return sha256;
    }

    public static void encode(FontRequestPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.fontId);
        buf.writeUtf(msg.transferId);
        buf.writeUtf(msg.sha256);
    }

    public static FontRequestPacket decode(FriendlyByteBuf buf) {
        return new FontRequestPacket(buf.readUtf(), buf.readUtf(), buf.readUtf());
    }

    public static void handle(FontRequestPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            String fontId = msg.getFontId();
            String transferId = msg.getTransferId();
            String expectedSha = msg.getSha256();

            FontInfo info = ServerFontManager.getFontInfo(fontId);
            if (info == null) {
                TitleFxMod.LOGGER.warn("Client requested non-registered font: " + fontId);
                return;
            }
            if (!info.sha256().equals(expectedSha)) {
                TitleFxMod.LOGGER.warn("Client requested font " + fontId + " with mismatched SHA-256. Server has: " + info.sha256() + ", requested: " + expectedSha);
                return;
            }

            File file = ServerFontManager.getFontFile(fontId);
            if (file == null || !file.exists()) {
                TitleFxMod.LOGGER.warn("Font file for " + fontId + " is missing on server disk.");
                return;
            }

            long maxAllowedSize = (long) TitleFxConfig.COMMON.maxFontFileSizeMb.get() * 1024 * 1024;
            if (file.length() > maxAllowedSize) {
                TitleFxMod.LOGGER.warn("Font file " + fontId + " exceeds server size limit: " + file.length() + " > " + maxAllowedSize);
                return;
            }

            int chunkSize = 32768; // 32KB
            long fileSize = file.length();
            int totalChunks = (int) Math.ceil((double) fileSize / chunkSize);
            if (totalChunks <= 0) {
                totalChunks = 1;
            }

            for (int i = 0; i < totalChunks; i++) {
                byte[] chunkData = ServerFontManager.readChunk(fontId, i, chunkSize);
                FontChunkPacket chunkPacket = new FontChunkPacket(
                    transferId,
                    fontId,
                    expectedSha,
                    i,
                    totalChunks,
                    chunkData
                );
                NetworkHandler.sendToPlayer(player, chunkPacket);
            }
        });
        ctx.setPacketHandled(true);
    }
}
