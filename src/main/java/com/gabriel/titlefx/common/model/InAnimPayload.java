package com.gabriel.titlefx.common.model;

import com.gabriel.titlefx.common.animation.Easing;
import com.gabriel.titlefx.common.animation.InAnimationType;
import net.minecraft.network.FriendlyByteBuf;

public record InAnimPayload(
    InAnimationType type,
    int durationMs,
    Easing easing
) {
    public static InAnimPayload defaultEmpty() {
        return new InAnimPayload(InAnimationType.NONE, 0, Easing.LINEAR);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(type);
        buf.writeInt(durationMs);
        buf.writeEnum(easing);
    }

    public static InAnimPayload read(FriendlyByteBuf buf) {
        return new InAnimPayload(
            buf.readEnum(InAnimationType.class),
            buf.readInt(),
            buf.readEnum(Easing.class)
        );
    }
}
