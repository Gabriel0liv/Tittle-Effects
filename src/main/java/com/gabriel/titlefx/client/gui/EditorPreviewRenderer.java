package com.gabriel.titlefx.client.gui;

import com.gabriel.titlefx.client.render.AnimatedTextInstance;
import com.gabriel.titlefx.common.model.AnimatedTextPayload;
import com.gabriel.titlefx.common.model.TextLayerPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;

import java.util.UUID;

public class EditorPreviewRenderer {
    private AnimatedTextInstance instance;
    private boolean playing = false;

    /** Cria nova instância e inicia animação a partir de agora. */
    public void play(EditorDraftState draft, int pvW) {
        if (draft == null) return;
        AnimatedTextPayload payload = draft.toPayload();
        if (payload.layers().isEmpty()) return;
        TextLayerPayload layer = payload.layers().get(0);

        float textWidth = Minecraft.getInstance().font.width(layer.text());
        if (textWidth <= 0) textWidth = 1.0f;

        float maxScaleByWidth = (pvW * 0.75f) / textWidth;
        float originalScale = draft.effectiveScale();
        float previewScale = Math.min(originalScale, maxScaleByWidth);
        previewScale = Math.max(0.6f, Math.min(3.0f, previewScale));

        TextLayerPayload previewLayer = new TextLayerPayload(
            layer.type(),
            layer.text(),
            layer.fontId(),
            layer.color(),
            layer.gradient(),
            previewScale,
            layer.position(),
            layer.reveal(),
            layer.in(),
            layer.idle(),
            layer.out(),
            layer.durationMs()
        );

        this.instance = new AnimatedTextInstance(
            payload.id(),
            UUID.randomUUID().toString(),
            previewLayer,
            payload.globalDurationMs(),
            System.currentTimeMillis()
        );
        this.playing = true;
    }

    /** Para a animação (mantém frame atual). */
    public void stop() {
        playing = false;
    }

    public boolean isPlaying() {
        return playing;
    }

    /**
     * Renderiza dentro do retângulo (x, y, w, h).
     */
    public void render(GuiGraphics g, int x, int y, int w, int h, float partialTick) {
        if (!playing || instance == null) {
            renderIdle(g, x, y, w, h);
            return;
        }

        long now = System.currentTimeMillis();
        if (instance.isExpired(now)) {
            playing = false;
            renderIdle(g, x, y, w, h);
            return;
        }

        // Enable scissor to clip text inside the preview panel bounds
        g.enableScissor(x, y, x + w, y + h);
        g.pose().pushPose();
        
        // Translate pose so that 0,0 is the top-left of the preview panel
        g.pose().translate(x, y, 0);
        
        // Render the text instance using w, h as screen dimensions
        instance.render(g, 0f, w, h, now);
        
        g.pose().popPose();
        g.disableScissor();
    }

    private void renderIdle(GuiGraphics g, int px, int py, int pw, int ph) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        
        // Draw a play icon and instructions
        String line1 = "▶ REPRODUZIR PREVIEW";
        String line2 = "As alterações são atualizadas automaticamente";
        
        int textW1 = font.width(line1);
        int textW2 = font.width(line2);
        
        int x1 = px + (pw - textW1) / 2;
        int y1 = py + (ph - 18) / 2;
        
        int x2 = px + (pw - textW2) / 2;
        int y2 = y1 + 12;
        
        g.drawString(font, line1, x1, y1, 0xFF55FF55, false);
        g.drawString(font, line2, x2, y2, 0x80FFFFFF, false);
    }
}
