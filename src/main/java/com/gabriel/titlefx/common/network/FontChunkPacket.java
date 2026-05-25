package com.gabriel.titlefx.common.network;

import com.gabriel.titlefx.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FontChunkPacket {
    private final String transferId;
    private final String fontId;
    private final String sha256;
    private final int chunkIndex;
    private final int totalChunks;
    private final byte[] data;

    public FontChunkPacket(String transferId, String fontId, String sha256, int chunkIndex, int totalChunks, byte[] data) {
        this.transferId = transferId;
        this.fontId = fontId;
        this.sha256 = sha256;
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
        this.data = data;
    }

    public String getTransferId() {
        return transferId;
    }

    public String getFontId() {
        return fontId;
    }

    public String getSha256() {
        return sha256;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public byte[] getData() {
        return data;
    }

    public static void encode(FontChunkPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.transferId);
        buf.writeUtf(msg.fontId);
        buf.writeUtf(msg.sha256);
        buf.writeInt(msg.chunkIndex);
        buf.writeInt(msg.totalChunks);
        buf.writeByteArray(msg.data);
    }

    public static FontChunkPacket decode(FriendlyByteBuf buf) {
        return new FontChunkPacket(
            buf.readUtf(),
            buf.readUtf(),
            buf.readUtf(),
            buf.readInt(),
            buf.readInt(),
            buf.readByteArray(65536)
        );
    }

    public static void handle(FontChunkPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleFontChunk(msg));
        });
        ctx.setPacketHandled(true);
    }
}
