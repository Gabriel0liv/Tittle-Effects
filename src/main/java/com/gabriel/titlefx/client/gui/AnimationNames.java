package com.gabriel.titlefx.client.gui;

import com.gabriel.titlefx.common.animation.*;

/**
 * Human-readable PT-BR display names for animation enum values.
 * Used in the editor UI only — the command system always uses technical enum names.
 */
public final class AnimationNames {
    private AnimationNames() {}

    public static String of(RevealType t) {
        if (t == null) return "Nenhuma";
        switch (t) {
            case NONE:               return "Nenhuma";
            case TYPEWRITER:         return "Digitando";
            case WORD_BY_WORD:       return "Palavra por palavra";
            case GLYPH_SCRAMBLE:     return "Letras embaralhadas";
            case OBFUSCATED_DECODE:  return "Decodificar mágico";
            case CENTER_OUT:         return "Aparecer do centro";
            case WIPE_LEFT_TO_RIGHT: return "Varrer da esquerda";
            case FADE_CHARS:         return "Fade por letras";
            case RANDOM_FADE:        return "Aleatório suave";
            default:                 return t.name();
        }
    }

    public static String of(RevealSpeed s) {
        return s != null ? s.getLabel() : "Normal";
    }

    public static String of(InAnimationType t) {
        if (t == null) return "Nenhuma";
        switch (t) {
            case NONE:               return "Nenhuma";
            case FADE_IN:            return "Aparecer suave";
            case CINEMATIC_ZOOM_IN:  return "Zoom cinematográfico";
            case SOFT_POP:           return "Pop suave";
            case SCALE_REVEAL:       return "Crescer do centro";
            default:                 return t.name();
        }
    }

    public static String of(IdleAnimationType t) {
        if (t == null) return "Nenhuma";
        switch (t) {
            case NONE:          return "Nenhuma";
            case SUBTLE_PULSE:  return "Pulso sutil";
            case BREATHING:     return "Respiração leve";
            case SUBTLE_SHAKE:  return "Tremor leve";
            case WAVE_SOFT:     return "Onda suave";
            case FLICKER:       return "Flicker mágico";
            default:            return t.name();
        }
    }

    public static String of(OutAnimationType t) {
        if (t == null) return "Nenhuma";
        switch (t) {
            case NONE:        return "Nenhuma";
            case FADE_OUT:    return "Sumir suave";
            case DISSOLVE:    return "Dissolver";
            case SHRINK_FADE: return "Encolher e sumir";
            default:          return t.name();
        }
    }
}
