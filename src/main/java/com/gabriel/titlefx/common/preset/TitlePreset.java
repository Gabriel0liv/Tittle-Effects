package com.gabriel.titlefx.common.preset;

import com.gabriel.titlefx.common.animation.*;
import com.gabriel.titlefx.common.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TitlePreset {
    public String type = "title";
    public String text = "";
    public String font = "minecraft:default";
    public String color = null;
    public List<String> gradient = null;
    public double scale = -1.0;
    public int duration = -1;

    public PositionPreset position;
    public RevealPreset reveal;
    public InPreset in;
    public IdlePreset idle;
    public OutPreset out;

    // Optional nested subtitle
    public TitlePreset subtitle;

    public static class PositionPreset {
        public String anchor = null;
        public Integer x = null;
        public Integer y = null;
        public String alignment = "center";
    }

    public static class RevealPreset {
        public String type = "none";
        public int duration = 1000;
        public String lockMode = "left_to_right";
        public int flickerSpeed = 2;
        public String charset = "safe";
        public boolean preserveSpaces = true;
        public boolean preserveCase = true;
    }

    public static class InPreset {
        public String type = "none";
        public int duration = 500;
        public String easing = "linear";
    }

    public static class IdlePreset {
        public String type = "none";
        public double intensity = 1.0;
    }

    public static class OutPreset {
        public String type = "none";
        public int duration = 500;
        public String easing = "linear";
    }

    public AnimatedTextPayload toPayload(String textOverride) {
        List<TextLayerPayload> layers = new ArrayList<>();

        // Determine global duration
        int globalDuration = duration > 0 ? duration : TextDefaults.getDefaultDuration(type);

        // 1. Process Main Layer
        String mainText = text != null ? text : "";
        if (textOverride != null) {
            if (mainText.contains("{text}")) {
                mainText = mainText.replace("{text}", textOverride);
            } else {
                mainText = textOverride;
            }
        }
        layers.add(createLayerPayload(this, mainText, type, globalDuration));

        // 2. Process Nested Subtitle Layer
        if (subtitle != null) {
            String subText = subtitle.text != null ? subtitle.text : "";
            if (textOverride != null && subText.contains("{text}")) {
                subText = subText.replace("{text}", textOverride);
            }
            layers.add(createLayerPayload(subtitle, subText, "subtitle", globalDuration));
        }

        return new AnimatedTextPayload(UUID.randomUUID().toString(), layers, globalDuration);
    }

    private TextLayerPayload createLayerPayload(TitlePreset preset, String resolvedText, String defaultType, int fallbackDuration) {
        String layerType = preset.type != null ? preset.type : defaultType;

        // Position
        PositionPayload posPayload;
        if (preset.position != null) {
            PositionPayload defaultPos = TextDefaults.getDefaultPosition(layerType);
            String anchor = preset.position.anchor != null ? preset.position.anchor : defaultPos.anchor();
            int xVal = preset.position.x != null ? preset.position.x : defaultPos.x();
            int yVal = preset.position.y != null ? preset.position.y : defaultPos.y();
            posPayload = new PositionPayload(
                anchor,
                xVal,
                yVal,
                preset.position.alignment != null ? preset.position.alignment : defaultPos.alignment()
            );
        } else {
            posPayload = TextDefaults.getDefaultPosition(layerType);
        }

        // Reveal
        RevealPayload revPayload;
        if (preset.reveal != null) {
            revPayload = new RevealPayload(
                RevealType.fromString(preset.reveal.type),
                preset.reveal.duration,
                LockMode.fromString(preset.reveal.lockMode),
                preset.reveal.flickerSpeed,
                preset.reveal.charset != null ? preset.reveal.charset : "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789",
                preset.reveal.preserveSpaces,
                preset.reveal.preserveCase
            );
        } else {
            revPayload = RevealPayload.defaultEmpty();
        }

        // In Anim
        InAnimPayload inPayload;
        if (preset.in != null) {
            inPayload = new InAnimPayload(
                InAnimationType.fromString(preset.in.type),
                preset.in.duration,
                Easing.fromString(preset.in.easing)
            );
        } else {
            inPayload = InAnimPayload.defaultEmpty();
        }

        // Idle Anim
        IdleAnimPayload idlePayload;
        if (preset.idle != null) {
            idlePayload = new IdleAnimPayload(
                IdleAnimationType.fromString(preset.idle.type),
                (float) preset.idle.intensity
            );
        } else {
            idlePayload = IdleAnimPayload.defaultEmpty();
        }

        // Out Anim
        OutAnimPayload outPayload;
        if (preset.out != null) {
            outPayload = new OutAnimPayload(
                OutAnimationType.fromString(preset.out.type),
                preset.out.duration,
                Easing.fromString(preset.out.easing)
            );
        } else {
            outPayload = OutAnimPayload.defaultEmpty();
        }

        int layerDuration = preset.duration > 0 ? preset.duration : fallbackDuration;
        float layerScale = preset.scale > 0 ? (float) preset.scale : TextDefaults.getDefaultScale(layerType);

        return new TextLayerPayload(
            layerType,
            resolvedText,
            preset.font != null ? preset.font : "minecraft:default",
            preset.color,
            preset.gradient,
            layerScale,
            posPayload,
            revPayload,
            inPayload,
            idlePayload,
            outPayload,
            layerDuration
        );
    }
}
