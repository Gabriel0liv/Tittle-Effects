package com.gabriel.titlefx.common.model;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public record AnimatedTextPayload(
    String id,
    List<TextLayerPayload> layers,
    int globalDurationMs
) {
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(id);
        buf.writeInt(layers.size());
        for (TextLayerPayload layer : layers) {
            layer.write(buf);
        }
        buf.writeInt(globalDurationMs);
    }

    public static AnimatedTextPayload read(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        int layerSize = buf.readInt();
        List<TextLayerPayload> layers = new ArrayList<>(layerSize);
        for (int i = 0; i < layerSize; i++) {
            layers.add(TextLayerPayload.read(buf));
        }
        int globalDurationMs = buf.readInt();

        return new AnimatedTextPayload(id, layers, globalDurationMs);
    }
}
