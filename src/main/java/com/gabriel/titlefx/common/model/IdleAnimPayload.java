package com.gabriel.titlefx.common.model;

import com.gabriel.titlefx.common.animation.IdleAnimationType;
import net.minecraft.network.FriendlyByteBuf;

public record IdleAnimPayload(
    IdleAnimationType type,
    float intensity
) {
    public static IdleAnimPayload defaultEmpty() {
        return new IdleAnimPayload(IdleAnimationType.NONE, 1.0f);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(type);
        buf.writeFloat(intensity);
    }

    public static IdleAnimPayload read(FriendlyByteBuf buf) {
        return new IdleAnimPayload(
            buf.readEnum(IdleAnimationType.class),
            buf.readFloat()
        );
    }
}
