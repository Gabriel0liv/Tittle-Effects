package com.gabriel.titlefx.common.model;

import com.gabriel.titlefx.common.animation.*;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public record TextLayerPayload(
    String type,
    String text,
    String fontId,
    String color,
    List<String> gradient,
    float scale,
    PositionPayload position,
    RevealPayload reveal,
    InAnimPayload in,
    IdleAnimPayload idle,
    OutAnimPayload out,
    Integer durationMs
) {
    public TextLayerPayload {
        fontId = "minecraft:default";

        // Normalize In Animation
        if (in != null) {
            InAnimationType animType = in.type();
            if (animType == InAnimationType.SLIDE_UP || animType == InAnimationType.SLIDE_DOWN ||
                animType == InAnimationType.SLIDE_LEFT || animType == InAnimationType.SLIDE_RIGHT) {
                in = new InAnimPayload(InAnimationType.FADE_IN, in.durationMs(), in.easing());
            } else if (animType == InAnimationType.ZOOM_IN) {
                in = new InAnimPayload(InAnimationType.CINEMATIC_ZOOM_IN, in.durationMs(), in.easing());
            } else if (animType == InAnimationType.POP_IN) {
                in = new InAnimPayload(InAnimationType.SOFT_POP, in.durationMs(), in.easing());
            }
        }

        // Normalize Out Animation
        if (out != null) {
            OutAnimationType animType = out.type();
            if (animType == OutAnimationType.SLIDE_UP_OUT || animType == OutAnimationType.SLIDE_DOWN_OUT ||
                animType == OutAnimationType.SLIDE_LEFT_OUT || animType == OutAnimationType.SLIDE_RIGHT_OUT) {
                out = new OutAnimPayload(OutAnimationType.FADE_OUT, out.durationMs(), out.easing());
            } else if (animType == OutAnimationType.ZOOM_OUT || animType == OutAnimationType.POP_OUT) {
                out = new OutAnimPayload(OutAnimationType.SHRINK_FADE, out.durationMs(), out.easing());
            }
        }

        // Normalize Idle Animation
        if (idle != null) {
            IdleAnimationType animType = idle.type();
            if (animType == IdleAnimationType.PULSE) {
                idle = new IdleAnimPayload(IdleAnimationType.SUBTLE_PULSE, idle.intensity());
            } else if (animType == IdleAnimationType.SHAKE) {
                idle = new IdleAnimPayload(IdleAnimationType.SUBTLE_SHAKE, idle.intensity());
            } else if (animType == IdleAnimationType.WAVE) {
                idle = new IdleAnimPayload(IdleAnimationType.WAVE_SOFT, idle.intensity());
            } else if (animType == IdleAnimationType.FLOAT) {
                idle = new IdleAnimPayload(IdleAnimationType.BREATHING, idle.intensity());
            }
        }
    }
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(type);
        buf.writeUtf(text);
        buf.writeUtf(fontId);
        
        // Color
        if (color != null) {
            buf.writeBoolean(true);
            buf.writeUtf(color);
        } else {
            buf.writeBoolean(false);
        }

        // Gradient
        if (gradient != null) {
            buf.writeBoolean(true);
            buf.writeInt(gradient.size());
            for (String gradCol : gradient) {
                buf.writeUtf(gradCol);
            }
        } else {
            buf.writeBoolean(false);
        }

        buf.writeFloat(scale);
        position.write(buf);
        reveal.write(buf);
        in.write(buf);
        idle.write(buf);
        out.write(buf);

        // Optional durationMs
        if (durationMs != null) {
            buf.writeBoolean(true);
            buf.writeInt(durationMs);
        } else {
            buf.writeBoolean(false);
        }
    }

    public static TextLayerPayload read(FriendlyByteBuf buf) {
        String type = buf.readUtf();
        String text = buf.readUtf();
        String fontId = buf.readUtf();

        String color = null;
        if (buf.readBoolean()) {
            color = buf.readUtf();
        }

        List<String> gradient = null;
        if (buf.readBoolean()) {
            int size = buf.readInt();
            gradient = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                gradient.add(buf.readUtf());
            }
        }

        float scale = buf.readFloat();
        PositionPayload position = PositionPayload.read(buf);
        RevealPayload reveal = RevealPayload.read(buf);
        InAnimPayload in = InAnimPayload.read(buf);
        IdleAnimPayload idle = IdleAnimPayload.read(buf);
        OutAnimPayload out = OutAnimPayload.read(buf);

        Integer durationMs = null;
        if (buf.readBoolean()) {
            durationMs = buf.readInt();
        }

        return new TextLayerPayload(
            type, text, fontId, color, gradient, scale, position, reveal, in, idle, out, durationMs
        );
    }
}
