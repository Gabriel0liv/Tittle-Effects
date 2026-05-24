package com.gabriel.titlefx.client.animation;

import com.gabriel.titlefx.client.render.AnimatedTextInstance;
import com.gabriel.titlefx.client.render.RenderableGlyph;
import com.gabriel.titlefx.common.animation.*;
import com.gabriel.titlefx.common.model.TextLayerPayload;

import java.util.List;
import java.util.Random;

public class AnimationEngine {

    public static void update(AnimatedTextInstance instance, long elapsedMs) {
        TextLayerPayload layer = instance.getLayer();
        List<RenderableGlyph> glyphs = instance.getGlyphs();
        int totalDuration = instance.getDuration();

        // 1. Timings and Phases
        int inDuration = layer.in().durationMs();
        int outDuration = layer.out().durationMs();

        float inProgress = 1.0f;
        if (inDuration > 0) {
            inProgress = Math.max(0.0f, Math.min(1.0f, (float) elapsedMs / inDuration));
        }

        float outProgress = 0.0f;
        if (outDuration > 0 && elapsedMs > (totalDuration - outDuration)) {
            outProgress = Math.max(0.0f, Math.min(1.0f, (float) (elapsedMs - (totalDuration - outDuration)) / outDuration));
        }

        // 2. Compute Transitions
        float inT = layer.in().easing().ease(inProgress);
        float outT = layer.out().easing().ease(outProgress);

        float baseAlpha = 1.0f;
        float baseScale = 1.0f;
        float baseTranslateX = 0.0f;
        float baseTranslateY = 0.0f;

        // IN ANIMATION
        InAnimationType inType = layer.in().type();
        if (inProgress < 1.0f && inType != InAnimationType.NONE) {
            switch (inType) {
                case FADE_IN:
                    baseAlpha *= inT;
                    break;
                case SLIDE_UP:
                    baseTranslateY += (1.0f - inT) * 30.0f;
                    break;
                case SLIDE_DOWN:
                    baseTranslateY += (1.0f - inT) * -30.0f;
                    break;
                case SLIDE_LEFT:
                    baseTranslateX += (1.0f - inT) * 50.0f;
                    break;
                case SLIDE_RIGHT:
                    baseTranslateX += (1.0f - inT) * -50.0f;
                    break;
                case ZOOM_IN:
                    baseScale *= inT;
                    break;
                case ZOOM_OUT:
                    baseScale *= (2.0f - inT);
                    break;
                case POP_IN:
                    baseScale *= Easing.EASE_OUT_BACK.ease(inProgress);
                    break;
            }
        }

        // OUT ANIMATION
        OutAnimationType outType = layer.out().type();
        if (outProgress > 0.0f && outType != OutAnimationType.NONE) {
            switch (outType) {
                case FADE_OUT:
                    baseAlpha *= (1.0f - outT);
                    break;
                case SLIDE_UP_OUT:
                    baseTranslateY += outT * -30.0f;
                    break;
                case SLIDE_DOWN_OUT:
                    baseTranslateY += outT * 30.0f;
                    break;
                case SLIDE_LEFT_OUT:
                    baseTranslateX += outT * -50.0f;
                    break;
                case SLIDE_RIGHT_OUT:
                    baseTranslateX += outT * 50.0f;
                    break;
                case ZOOM_OUT:
                case POP_OUT:
                    baseScale *= (1.0f - outT);
                    break;
            }
        }

        // IDLE ANIMATION (BLOCK LEVEL)
        IdleAnimationType idleType = layer.idle().type();
        float intensity = layer.idle().intensity();
        float timeSec = elapsedMs / 1000.0f;

        if (idleType != IdleAnimationType.NONE) {
            switch (idleType) {
                case PULSE:
                    baseScale *= 1.0f + (float) Math.sin(timeSec * 4.0f) * 0.06f * intensity;
                    break;
                case FLOAT:
                    baseTranslateY += (float) Math.sin(timeSec * 2.0f) * 6.0f * intensity;
                    break;
            }
        }

        // 3. Apply Transformations to Glyphs
        Random rand = new Random();
        for (int i = 0; i < glyphs.size(); i++) {
            RenderableGlyph glyph = glyphs.get(i);
            glyph.alpha *= baseAlpha;
            glyph.scale *= baseScale;
            glyph.xOffset += baseTranslateX;
            glyph.yOffset += baseTranslateY;

            // IDLE ANIMATION (GLYPH LEVEL)
            if (idleType != IdleAnimationType.NONE) {
                switch (idleType) {
                    case SHAKE:
                        // Shake offset is random and jittery per letter
                        glyph.xOffset += (rand.nextFloat() * 2.0f - 1.0f) * intensity * 1.5f;
                        glyph.yOffset += (rand.nextFloat() * 2.0f - 1.0f) * intensity * 1.5f;
                        break;
                    case WAVE:
                        // Sine wave offset per character index
                        glyph.yOffset += (float) Math.sin(timeSec * 6.0f + i * 0.4f) * 4.0f * intensity;
                        break;
                    case FLICKER:
                        // Flicker alpha occasionally
                        long seed = (elapsedMs / 60) + i;
                        rand.setSeed(seed);
                        if (rand.nextFloat() < 0.15f * intensity) {
                            glyph.alpha *= 0.2f + rand.nextFloat() * 0.3f;
                        }
                        break;
                }
            }
        }
    }
}
