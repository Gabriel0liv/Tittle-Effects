package com.gabriel.titlefx.client.gui;

import com.gabriel.titlefx.common.animation.*;

/**
 * Built-in style presets for the TitleFX visual editor.
 * Each card applies a complete, curated animation configuration to an EditorDraftState.
 */
public enum StyleCard {

    BOSS_CINEMATICO("Boss Cinemático", 0xFF5A1010) {
        @Override public void apply(EditorDraftState d) {
            d.type        = "title";
            d.color       = "#AA0000";
            d.revealType  = RevealType.OBFUSCATED_DECODE;
            d.revealSpeed = RevealSpeed.CINEMATIC;
            d.inAnimation = InAnimationType.CINEMATIC_ZOOM_IN;
            d.idleAnimation = IdleAnimationType.SUBTLE_PULSE;
            d.outAnimation  = OutAnimationType.DISSOLVE;
            d.durationMs  = 5500;
            d.inDuration  = 600;
            d.outDuration = 600;
        }
    },

    MISSAO("Missão", 0xFF4A3010) {
        @Override public void apply(EditorDraftState d) {
            d.type        = "title";
            d.color       = "#FFD56A";
            d.revealType  = RevealType.WORD_BY_WORD;
            d.revealSpeed = RevealSpeed.NORMAL;
            d.inAnimation = InAnimationType.FADE_IN;
            d.idleAnimation = IdleAnimationType.BREATHING;
            d.outAnimation  = OutAnimationType.FADE_OUT;
            d.durationMs  = 4000;
            d.inDuration  = 400;
            d.outDuration = 400;
        }
    },

    LOCALIZACAO("Localização", 0xFF1A3A1A) {
        @Override public void apply(EditorDraftState d) {
            d.type        = "title";
            d.color       = "#FFFFFF";
            d.revealType  = RevealType.FADE_CHARS;
            d.revealSpeed = RevealSpeed.CINEMATIC;
            d.inAnimation = InAnimationType.SOFT_POP;
            d.idleAnimation = IdleAnimationType.NONE;
            d.outAnimation  = OutAnimationType.DISSOLVE;
            d.durationMs  = 3800;
            d.inDuration  = 500;
            d.outDuration = 500;
        }
    },

    AVISO("Aviso", 0xFF4A2A00) {
        @Override public void apply(EditorDraftState d) {
            d.type        = "title";
            d.color       = "#FFAA00";
            d.revealType  = RevealType.GLYPH_SCRAMBLE;
            d.revealSpeed = RevealSpeed.FAST;
            d.inAnimation = InAnimationType.SOFT_POP;
            d.idleAnimation = IdleAnimationType.FLICKER;
            d.outAnimation  = OutAnimationType.FADE_OUT;
            d.durationMs  = 3500;
            d.inDuration  = 400;
            d.outDuration = 400;
        }
    },

    SISTEMA("Sistema", 0xFF0A2040) {
        @Override public void apply(EditorDraftState d) {
            d.type        = "actionbar";
            d.color       = "#55AAFF";
            d.revealType  = RevealType.TYPEWRITER;
            d.revealSpeed = RevealSpeed.FAST;
            d.inAnimation = InAnimationType.FADE_IN;
            d.idleAnimation = IdleAnimationType.NONE;
            d.outAnimation  = OutAnimationType.FADE_OUT;
            d.durationMs  = 2500;
            d.inDuration  = 200;
            d.outDuration = 300;
        }
    },

    ACHIEVEMENT("Achievement", 0xFF303010) {
        @Override public void apply(EditorDraftState d) {
            d.type        = "title";
            d.color       = "#FFFF55";
            d.revealType  = RevealType.CENTER_OUT;
            d.revealSpeed = RevealSpeed.CINEMATIC;
            d.inAnimation = InAnimationType.SOFT_POP;
            d.idleAnimation = IdleAnimationType.SUBTLE_PULSE;
            d.outAnimation  = OutAnimationType.DISSOLVE;
            d.durationMs  = 4200;
            d.inDuration  = 500;
            d.outDuration = 400;
        }
    };

    private final String label;
    private final int    accentColor;

    StyleCard(String label, int accentColor) {
        this.label       = label;
        this.accentColor = accentColor;
    }

    public String getLabel()     { return label; }
    public int    getAccentColor() { return accentColor; }

    /** Applies this card's complete configuration to the given draft state. */
    public abstract void apply(EditorDraftState d);
}
