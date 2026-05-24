package com.gabriel.titlefx.client.render;

import com.gabriel.titlefx.client.font.ClientFontManager;
import com.gabriel.titlefx.common.animation.IdleAnimationType;
import com.gabriel.titlefx.common.animation.RevealType;
import com.gabriel.titlefx.common.model.TextLayerPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TextRendererEngine {

    public static void render(GuiGraphics guiGraphics, AnimatedTextInstance instance, int screenWidth, int screenHeight) {
        TextLayerPayload layer = instance.getLayer();
        String text = instance.getText();
        if (text == null || text.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        ResourceLocation fontLoc = ClientFontManager.getFont(layer.fontId());
        Font font = mc.font;
        Style fontStyle = Style.EMPTY.withFont(fontLoc);

        // Determine lines of text (separated by \n)
        String[] lines = text.split("\n");
        List<List<RenderableGlyph>> glyphLines = new ArrayList<>();
        int charIndex = 0;
        for (String line : lines) {
            List<RenderableGlyph> lineGlyphs = new ArrayList<>();
            for (int i = 0; i < line.length(); i++) {
                if (charIndex < instance.getGlyphs().size()) {
                    lineGlyphs.add(instance.getGlyphs().get(charIndex++));
                }
            }
            glyphLines.add(lineGlyphs);
            // Skip the '\n' character in instance glyphs if present
            charIndex++; 
        }

        // Calculate total scaled height
        float scale = layer.scale();
        int lineHeight = 10; // 8 pixels text + 2 pixels gap
        float totalHeight = lines.length * lineHeight * scale;

        // Calculate base anchor coordinate
        float anchorX = 0.0f;
        float anchorY = 0.0f;
        String anchor = layer.position().anchor().toLowerCase(Locale.ROOT);

        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        switch (anchor) {
            case "center":
                anchorX = centerX;
                anchorY = centerY;
                break;
            case "top":
                anchorX = centerX;
                anchorY = 0.0f;
                break;
            case "bottom":
                anchorX = centerX;
                anchorY = screenHeight;
                break;
            case "left":
                anchorX = 0.0f;
                anchorY = centerY;
                break;
            case "right":
                anchorX = screenWidth;
                anchorY = centerY;
                break;
            case "top_left":
            case "topleft":
                anchorX = 0.0f;
                anchorY = 0.0f;
                break;
            case "top_right":
            case "topright":
                anchorX = screenWidth;
                anchorY = 0.0f;
                break;
            case "bottom_left":
            case "bottomleft":
                anchorX = 0.0f;
                anchorY = screenHeight;
                break;
            case "bottom_right":
            case "bottomright":
                anchorX = screenWidth;
                anchorY = screenHeight;
                break;
            default: // custom or default to center
                anchorX = centerX;
                anchorY = centerY;
                break;
        }

        // Vertical adjustment for center-aligned anchors
        float startY = anchorY + layer.position().y();
        if (anchor.contains("center") || anchor.equals("left") || anchor.equals("right")) {
            startY -= (totalHeight / 2.0f);
        } else if (anchor.contains("bottom")) {
            startY -= totalHeight;
        }

        // Check if we can optimize and render whole strings
        boolean needsGlyphRender = false;
        if (layer.reveal().type() != RevealType.NONE && layer.reveal().type() != RevealType.LINE_BY_LINE) {
            needsGlyphRender = true;
        }
        if (layer.idle().type() == IdleAnimationType.SHAKE || layer.idle().type() == IdleAnimationType.WAVE || layer.idle().type() == IdleAnimationType.FLICKER) {
            needsGlyphRender = true;
        }
        if (layer.gradient() != null && !layer.gradient().isEmpty()) {
            needsGlyphRender = true;
        }

        // Draw line by line
        PoseStack poseStack = guiGraphics.pose();
        
        for (int l = 0; l < lines.length; l++) {
            String lineText = lines[l];
            List<RenderableGlyph> lineGlyphs = glyphLines.get(l);

            float lineY = startY + (l * lineHeight * scale);

            // Calculate width using target character widths to keep width stable during scrambles
            float targetLineWidth = 0.0f;
            for (RenderableGlyph g : lineGlyphs) {
                targetLineWidth += font.width(Component.literal(String.valueOf(g.targetChar)).withStyle(fontStyle));
            }
            float scaledLineWidth = targetLineWidth * scale;

            // Calculate start X based on alignment
            float startX = anchorX + layer.position().x();
            String alignment = layer.position().alignment().toLowerCase(Locale.ROOT);
            if (alignment.equals("center")) {
                startX -= (scaledLineWidth / 2.0f);
            } else if (alignment.equals("right")) {
                startX -= scaledLineWidth;
            }

            if (!needsGlyphRender) {
                // Optimized standard drawString path
                // Determine color
                float overallAlpha = 1.0f;
                if (!lineGlyphs.isEmpty()) {
                    overallAlpha = lineGlyphs.get(0).alpha;
                }
                int intColor = parseColor(layer.color(), overallAlpha);

                poseStack.pushPose();
                poseStack.translate(startX, lineY, 0.0f);
                poseStack.scale(scale, scale, 1.0f);
                
                // Draw normal string
                if (!lineGlyphs.isEmpty() && lineGlyphs.get(0).visible) {
                    FormattedCharSequence charSeq = Component.literal(lineText).withStyle(fontStyle).getVisualOrderText();
                    guiGraphics.drawString(font, charSeq, 0, 0, intColor, true); // default shadow true
                }
                poseStack.popPose();
            } else {
                // Detailed Glyph-by-Glyph path
                float currentX = startX;
                for (int i = 0; i < lineGlyphs.size(); i++) {
                    RenderableGlyph g = lineGlyphs.get(i);
                    if (!g.visible) {
                        currentX += font.width(Component.literal(String.valueOf(g.targetChar)).withStyle(fontStyle)) * scale;
                        continue;
                    }

                    int targetWidth = font.width(Component.literal(String.valueOf(g.targetChar)).withStyle(fontStyle));
                    int curWidth = font.width(Component.literal(String.valueOf(g.currentChar)).withStyle(fontStyle));
                    float centeringOffset = (targetWidth - curWidth) / 2.0f;

                    poseStack.pushPose();
                    // Translate local character matrix
                    poseStack.translate(
                        currentX + (g.xOffset * scale) + (centeringOffset * g.scale * scale),
                        lineY + (g.yOffset * scale),
                        0.0f
                    );
                    poseStack.scale(g.scale * scale, g.scale * scale, 1.0f);

                    // Compute Color (check gradients)
                    int finalColor;
                    if (layer.gradient() != null && !layer.gradient().isEmpty()) {
                        float factor = lineGlyphs.size() > 1 ? (float) i / (lineGlyphs.size() - 1) : 0.0f;
                        finalColor = getGradientColor(layer.gradient(), factor, g.alpha);
                    } else {
                        finalColor = parseColor(layer.color(), g.alpha);
                    }

                    FormattedCharSequence charSeq = Component.literal(String.valueOf(g.currentChar)).withStyle(fontStyle).getVisualOrderText();
                    guiGraphics.drawString(font, charSeq, 0, 0, finalColor, true);
                    poseStack.popPose();

                    currentX += targetWidth * scale * g.scale;
                }
            }
        }
    }

    public static int parseColor(String hex, float alpha) {
        if (hex == null) return 0xFFFFFF | (((int) (alpha * 255) & 0xFF) << 24);
        String clean = hex.replace("#", "").trim();
        try {
            if (clean.length() == 6) {
                int rgb = Integer.parseInt(clean, 16);
                int a = (int) (alpha * 255) & 0xFF;
                return (a << 24) | (rgb & 0xFFFFFF);
            } else if (clean.length() == 8) {
                long argb = Long.parseLong(clean, 16);
                int a = (int) (((argb >> 24) & 0xFF) * alpha) & 0xFF;
                int rgb = (int) (argb & 0xFFFFFF);
                return (a << 24) | rgb;
            }
        } catch (NumberFormatException ignored) {}
        return 0xFFFFFF | (((int) (alpha * 255) & 0xFF) << 24);
    }

    public static int getGradientColor(List<String> colors, float factor, float alpha) {
        if (colors == null || colors.isEmpty()) return 0xFFFFFF | (((int) (alpha * 255) & 0xFF) << 24);
        if (colors.size() == 1) return parseColor(colors.get(0), alpha);

        float segment = 1.0f / (colors.size() - 1);
        int index = (int) (factor / segment);
        if (index >= colors.size() - 1) {
            return parseColor(colors.get(colors.size() - 1), alpha);
        }
        float localT = (factor - (index * segment)) / segment;

        int c1 = parseColor(colors.get(index), 1.0f);
        int c2 = parseColor(colors.get(index + 1), 1.0f);

        int a1 = (c1 >> 24) & 0xFF;
        int r1 = (c1 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = c1 & 0xFF;

        int a2 = (c2 >> 24) & 0xFF;
        int r2 = (c2 >> 16) & 0xFF;
        int g2 = (c2 >> 8) & 0xFF;
        int b2 = c2 & 0xFF;

        int a = (int) ((a1 + (a2 - a1) * localT) * alpha) & 0xFF;
        int r = (int) (r1 + (r2 - r1) * localT) & 0xFF;
        int g = (int) (g1 + (g2 - g1) * localT) & 0xFF;
        int b = (int) (b1 + (b2 - b1) * localT) & 0xFF;

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
