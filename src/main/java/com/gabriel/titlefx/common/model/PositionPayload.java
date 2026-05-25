package com.gabriel.titlefx.common.model;

import net.minecraft.network.FriendlyByteBuf;

public record PositionPayload(
    String anchor,
    int x,
    int y,
    String alignment
) {
    public static PositionPayload defaultForType(String type) {
        return TextDefaults.getDefaultPosition(type);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(anchor);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeUtf(alignment);
    }

    public static PositionPayload read(FriendlyByteBuf buf) {
        return new PositionPayload(
            buf.readUtf(),
            buf.readInt(),
            buf.readInt(),
            buf.readUtf()
        );
    }
}
