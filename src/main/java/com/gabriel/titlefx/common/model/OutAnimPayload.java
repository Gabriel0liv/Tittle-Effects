package com.gabriel.titlefx.common.model;

import com.gabriel.titlefx.common.animation.Easing;
import com.gabriel.titlefx.common.animation.OutAnimationType;
import net.minecraft.network.FriendlyByteBuf;

public record OutAnimPayload(
    OutAnimationType type,
    int durationMs,
    Easing easing
) {
    public static OutAnimPayload defaultEmpty() {
        return new OutAnimPayload(OutAnimationType.NONE, 0, Easing.LINEAR);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(type);
        buf.writeInt(durationMs);
        buf.writeEnum(easing);
    }

    public static OutAnimPayload read(FriendlyByteBuf buf) {
        return new OutAnimPayload(
            buf.readEnum(OutAnimationType.class),
            buf.readInt(),
            buf.readEnum(Easing.class)
        );
    }
}
