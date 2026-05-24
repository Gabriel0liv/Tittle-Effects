package com.gabriel.titlefx.common.model;

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
