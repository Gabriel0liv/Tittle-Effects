package com.gabriel.titlefx.client.render;

import com.gabriel.titlefx.client.animation.AnimationEngine;
import com.gabriel.titlefx.client.animation.TextRevealEngine;
import com.gabriel.titlefx.common.animation.LockMode;
import com.gabriel.titlefx.common.model.TextLayerPayload;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AnimatedTextInstance {
    private final String payloadId;
    private final String instanceId;
    private final TextLayerPayload layer;
    private final int duration;
    private final long creationTime;
    
    private final String text;
    private final List<RenderableGlyph> glyphs = new ArrayList<>();
    private final List<Integer> revealOrder = new ArrayList<>();

    public AnimatedTextInstance(String payloadId, String instanceId, TextLayerPayload layer, int globalDuration, long creationTime) {
        this.payloadId = payloadId;
        this.instanceId = instanceId;
        this.layer = layer;
        this.duration = layer.durationMs() != null ? layer.durationMs() : globalDuration;
        this.creationTime = creationTime;
        this.text = layer.text();

        for (int i = 0; i < text.length(); i++) {
            glyphs.add(new RenderableGlyph(text.charAt(i)));
        }

        initRevealOrder();
    }

    private void initRevealOrder() {
        int length = text.length();
        for (int i = 0; i < length; i++) {
            revealOrder.add(i);
        }

        LockMode mode = layer.reveal().lockMode();
        if (mode == LockMode.RANDOM) {
            Collections.shuffle(revealOrder);
        } else if (mode == LockMode.RIGHT_TO_LEFT) {
            Collections.reverse(revealOrder);
        } else if (mode == LockMode.CENTER_OUT) {
            final double center = (length - 1) / 2.0;
            revealOrder.sort(Comparator.comparingDouble(i -> Math.abs(i - center)));
        } else if (mode == LockMode.EDGES_IN) {
            final double center = (length - 1) / 2.0;
            revealOrder.sort((a, b) -> Double.compare(Math.abs(b - center), Math.abs(a - center)));
        }
    }

    public boolean isExpired(long now) {
        return now - creationTime >= duration;
    }

    public void render(GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight, long now) {
        long elapsedMs = now - creationTime;

        // Reset glyphs state
        for (RenderableGlyph glyph : glyphs) {
            glyph.currentChar = glyph.targetChar;
            glyph.xOffset = 0.0f;
            glyph.yOffset = 0.0f;
            glyph.scale = 1.0f;
            glyph.alpha = 1.0f;
            glyph.visible = true;
        }

        // Process reveal
        TextRevealEngine.update(this, elapsedMs);

        // Process animations
        AnimationEngine.update(this, elapsedMs);

        // Render line/glyphs
        TextRendererEngine.render(guiGraphics, this, screenWidth, screenHeight);
    }

    // Getters
    public String getPayloadId() { return payloadId; }
    public String getId() { return instanceId; }
    public String getInstanceId() { return instanceId; }
    public TextLayerPayload getLayer() { return layer; }
    public int getDuration() { return duration; }
    public long getCreationTime() { return creationTime; }
    public String getText() { return text; }
    public List<RenderableGlyph> getGlyphs() { return glyphs; }
    public List<Integer> getRevealOrder() { return revealOrder; }
    public String getType() { return layer.type(); }
}
