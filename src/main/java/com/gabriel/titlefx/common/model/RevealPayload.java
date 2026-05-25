package com.gabriel.titlefx.common.model;

import com.gabriel.titlefx.common.animation.LockMode;
import com.gabriel.titlefx.common.animation.RevealType;
import net.minecraft.network.FriendlyByteBuf;

public record RevealPayload(
    RevealType type,
    int durationMs,
    LockMode lockMode,
    int flickerSpeed,
    String charset,
    boolean preserveSpaces,
    boolean preserveCase
) {
    public static RevealPayload defaultEmpty() {
        return new RevealPayload(
            RevealType.NONE,
            0,
            LockMode.LEFT_TO_RIGHT,
            2,
            "safe",
            true,
            true
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(type);
        buf.writeInt(durationMs);
        buf.writeEnum(lockMode);
        buf.writeInt(flickerSpeed);
        buf.writeUtf(charset);
        buf.writeBoolean(preserveSpaces);
        buf.writeBoolean(preserveCase);
    }

    public static RevealPayload read(FriendlyByteBuf buf) {
        return new RevealPayload(
            buf.readEnum(RevealType.class),
            buf.readInt(),
            buf.readEnum(LockMode.class),
            buf.readInt(),
            buf.readUtf(),
            buf.readBoolean(),
            buf.readBoolean()
        );
    }
}
