package com.gabriel.titlefx.common.font;

import net.minecraft.network.FriendlyByteBuf;

public record FontInfo(
    String fontId,
    String originalName,
    String extension,
    long sizeBytes,
    String sha256
) {
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(fontId);
        buf.writeUtf(originalName);
        buf.writeUtf(extension);
        buf.writeLong(sizeBytes);
        buf.writeUtf(sha256);
    }

    public static FontInfo read(FriendlyByteBuf buf) {
        return new FontInfo(
            buf.readUtf(),
            buf.readUtf(),
            buf.readUtf(),
            buf.readLong(),
            buf.readUtf()
        );
    }
}
