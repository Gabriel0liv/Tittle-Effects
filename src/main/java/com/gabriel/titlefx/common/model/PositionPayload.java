package com.gabriel.titlefx.common.model;

import net.minecraft.network.FriendlyByteBuf;

public record PositionPayload(
    String anchor,
    int x,
    int y,
    String alignment
) {
    public static PositionPayload defaultForType(String type) {
        if ("subtitle".equalsIgnoreCase(type)) {
            return new PositionPayload("center", 0, -10, "center");
        } else if ("actionbar".equalsIgnoreCase(type)) {
            return new PositionPayload("bottom", 0, -60, "center");
        } else {
            // title and default
            return new PositionPayload("center", 0, -40, "center");
        }
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
