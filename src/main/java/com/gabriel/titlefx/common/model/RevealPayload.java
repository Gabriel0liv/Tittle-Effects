package com.gabriel.titlefx.common.model;

import com.gabriel.titlefx.common.animation.LockMode;
import com.gabriel.titlefx.common.animation.RevealDurationCalculator;
import com.gabriel.titlefx.common.animation.RevealSpeed;
import com.gabriel.titlefx.common.animation.RevealType;
import net.minecraft.network.FriendlyByteBuf;

public record RevealPayload(
    RevealType type,
    RevealSpeed speed,
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
            RevealSpeed.NORMAL,
            0,
            LockMode.LEFT_TO_RIGHT,
            2,
            "safe",
            true,
            true
        );
    }

    /**
     * Returns the effective reveal duration for a given text.
     * <ul>
     *   <li>CUSTOM → returns {@link #durationMs} directly.</li>
     *   <li>INSTANT → 0 ms (immediate reveal).</li>
     *   <li>All others → delegated to {@link RevealDurationCalculator}.</li>
     * </ul>
     */
    public int effectiveDurationMs(String text) {
        if (speed == RevealSpeed.CUSTOM) return durationMs;
        return RevealDurationCalculator.calculate(type, speed, text);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(type);
        buf.writeEnum(speed);
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
            buf.readEnum(RevealSpeed.class),
            buf.readInt(),
            buf.readEnum(LockMode.class),
            buf.readInt(),
            buf.readUtf(),
            buf.readBoolean(),
            buf.readBoolean()
        );
    }
}
