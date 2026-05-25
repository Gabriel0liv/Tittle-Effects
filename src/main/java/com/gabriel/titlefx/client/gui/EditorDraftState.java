package com.gabriel.titlefx.client.gui;

import com.gabriel.titlefx.common.animation.*;
import com.gabriel.titlefx.common.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.UUID;

/**
 * Full state of the TitleFX visual editor — persisted across screen opens/closes.
 * All enum fields are serialized by Gson as their name() string.
 * Negative / sentinel values mean "use type default":
 *   scale = -1f           → TextDefaults.getDefaultScale(type)
 *   yOffset = MIN_VALUE   → TextDefaults.getDefaultPosition(type).y()
 *   durationMs = -1       → TextDefaults.getDefaultDuration(type)
 */
public class EditorDraftState {

    // ------------------------------------------------------------------ content
    public String text  = "Olá TitleFX!";
    public String type  = "title";
    public String color = "#FFFFFF";

    // ------------------------------------------------------------------ reveal
    public RevealType  revealType  = RevealType.NONE;
    public RevealSpeed revealSpeed = RevealSpeed.NORMAL;

    // ------------------------------------------------------------------ in
    public InAnimationType inAnimation = InAnimationType.NONE;
    public Easing          inEasing    = Easing.LINEAR;
    public int             inDuration  = 500;

    // ------------------------------------------------------------------ idle
    public IdleAnimationType idleAnimation = IdleAnimationType.NONE;
    public float             idleIntensity = 1.0f;

    // ------------------------------------------------------------------ out
    public OutAnimationType outAnimation = OutAnimationType.NONE;
    public Easing           outEasing    = Easing.LINEAR;
    public int              outDuration  = 500;

    // ------------------------------------------------------------------ advanced overrides
    /** -1 = use type default (TextDefaults.getDefaultScale). */
    public float scale     = -1f;
    public int   xOffset   = 0;
    /** Integer.MIN_VALUE = use type default y from TextDefaults. */
    public int   yOffset   = Integer.MIN_VALUE;
    public String alignment = "center";
    /** -1 = use type default duration. */
    public int   durationMs = -1;

    // ------------------------------------------------------------------ UI state
    public boolean advancedMode      = false;
    public String  selectedStyleCard = null;

    // ==================================================================
    // Singleton / persistence
    // ==================================================================
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static EditorDraftState singleton;

    /** Returns the in-memory draft, loading from disk if not yet initialized. */
    public static EditorDraftState getInstance() {
        if (singleton == null) singleton = loadFromDisk();
        return singleton;
    }

    public static EditorDraftState loadFromDisk() {
        Path p = draftPath();
        if (p != null && p.toFile().exists()) {
            try (InputStreamReader r = new InputStreamReader(
                    new FileInputStream(p.toFile()), StandardCharsets.UTF_8)) {
                EditorDraftState loaded = GSON.fromJson(r, EditorDraftState.class);
                if (loaded != null) {
                    // null-safety for fields that Gson may skip
                    if (loaded.text         == null) loaded.text         = "Olá TitleFX!";
                    if (loaded.type         == null) loaded.type         = "title";
                    if (loaded.color        == null) loaded.color        = "#FFFFFF";
                    if (loaded.revealType   == null) loaded.revealType   = RevealType.NONE;
                    if (loaded.revealSpeed  == null) loaded.revealSpeed  = RevealSpeed.NORMAL;
                    if (loaded.inAnimation  == null) loaded.inAnimation  = InAnimationType.NONE;
                    if (loaded.inEasing     == null) loaded.inEasing     = Easing.LINEAR;
                    if (loaded.idleAnimation== null) loaded.idleAnimation= IdleAnimationType.NONE;
                    if (loaded.outAnimation == null) loaded.outAnimation = OutAnimationType.NONE;
                    if (loaded.outEasing    == null) loaded.outEasing    = Easing.LINEAR;
                    if (loaded.alignment    == null) loaded.alignment    = "center";
                    singleton = loaded;
                    return loaded;
                }
            } catch (Exception ignored) {}
        }
        return new EditorDraftState();
    }

    /** Saves current state to disk and updates the in-memory singleton. */
    public void save() {
        singleton = this;
        Path p = draftPath();
        if (p == null) return;
        try {
            p.getParent().toFile().mkdirs();
            try (OutputStreamWriter w = new OutputStreamWriter(
                    new FileOutputStream(p.toFile()), StandardCharsets.UTF_8)) {
                GSON.toJson(this, w);
            }
        } catch (Exception ignored) {}
    }

    /** Resets animation fields to defaults, preserving type, text and UI mode. */
    public void reset() {
        color        = "#FFFFFF";
        revealType   = RevealType.NONE;
        revealSpeed  = RevealSpeed.NORMAL;
        inAnimation  = InAnimationType.NONE;
        inEasing     = Easing.LINEAR;
        inDuration   = 500;
        idleAnimation = IdleAnimationType.NONE;
        idleIntensity = 1.0f;
        outAnimation = OutAnimationType.NONE;
        outEasing    = Easing.LINEAR;
        outDuration  = 500;
        scale        = -1f;
        xOffset      = 0;
        yOffset      = Integer.MIN_VALUE;
        alignment    = "center";
        durationMs   = -1;
        selectedStyleCard = null;
    }

    // ==================================================================
    // Effective value helpers
    // ==================================================================

    public float effectiveScale() {
        return scale > 0 ? scale : TextDefaults.getDefaultScale(type);
    }

    public int effectiveDuration() {
        return durationMs > 0 ? durationMs : TextDefaults.getDefaultDuration(type);
    }

    public int effectiveY() {
        return yOffset != Integer.MIN_VALUE ? yOffset : TextDefaults.getDefaultPosition(type).y();
    }

    // ==================================================================
    // Payload builder
    // ==================================================================

    /** Builds an AnimatedTextPayload from the current draft state. */
    public AnimatedTextPayload toPayload() {
        PositionPayload defaultPos = TextDefaults.getDefaultPosition(type);
        int resolvedY = yOffset != Integer.MIN_VALUE ? yOffset : defaultPos.y();

        PositionPayload pos = new PositionPayload(
            defaultPos.anchor(), xOffset, resolvedY,
            alignment != null ? alignment : "center"
        );

        RevealPayload reveal = new RevealPayload(
            revealType  != null ? revealType  : RevealType.NONE,
            revealSpeed != null ? revealSpeed : RevealSpeed.NORMAL,
            0, LockMode.LEFT_TO_RIGHT, 2, "safe", true, true
        );

        InAnimPayload  inPay   = new InAnimPayload(
            inAnimation != null ? inAnimation : InAnimationType.NONE,
            inDuration, inEasing != null ? inEasing : Easing.LINEAR
        );
        IdleAnimPayload idlePay = new IdleAnimPayload(
            idleAnimation != null ? idleAnimation : IdleAnimationType.NONE, idleIntensity
        );
        OutAnimPayload outPay  = new OutAnimPayload(
            outAnimation != null ? outAnimation : OutAnimationType.NONE,
            outDuration, outEasing != null ? outEasing : Easing.LINEAR
        );

        int   dur  = effectiveDuration();
        float sc   = effectiveScale();

        TextLayerPayload layer = new TextLayerPayload(
            type, text != null ? text : "",
            "minecraft:default",
            color, null, sc,
            pos, reveal, inPay, idlePay, outPay, dur
        );

        return new AnimatedTextPayload(
            UUID.randomUUID().toString(),
            Collections.singletonList(layer), dur
        );
    }

    // ==================================================================
    // Minimal command builder
    // ==================================================================

    /**
     * Produces the shortest valid /ctitle show command representing this state.
     * Omits scale, x, y, duration, alignment, speed if they match type defaults.
     */
    public String toCommand() {
        StringBuilder cmd = new StringBuilder("/ctitle show @a ").append(type).append(" ");

        // Color — only if differs from default #FFFFFF
        if (color != null && !color.isEmpty() && !"#FFFFFF".equalsIgnoreCase(color)) {
            cmd.append("color:").append(color).append(" ");
        }

        // Scale — only if differs from type default
        float defScale = TextDefaults.getDefaultScale(type);
        if (scale > 0 && Math.abs(scale - defScale) > 0.01f) {
            cmd.append("scale:").append(String.format("%.1f", scale)).append(" ");
        }

        // X — only if non-zero
        if (xOffset != 0) cmd.append("x:").append(xOffset).append(" ");

        // Y — only if differs from type default
        int defY      = TextDefaults.getDefaultPosition(type).y();
        int resolvedY = yOffset != Integer.MIN_VALUE ? yOffset : defY;
        if (resolvedY != defY) cmd.append("y:").append(resolvedY).append(" ");

        // Alignment — only if not center
        if (!"center".equalsIgnoreCase(alignment)) {
            cmd.append("align:").append(alignment).append(" ");
        }

        // Duration — only if differs from type default
        int defDur = TextDefaults.getDefaultDuration(type);
        if (durationMs > 0 && durationMs != defDur) {
            cmd.append("duration:").append(durationMs).append(" ");
        }

        // Reveal
        if (revealType != null && revealType != RevealType.NONE) {
            cmd.append("reveal:").append(revealType.name().toLowerCase()).append(" ");
            if (revealSpeed != null && revealSpeed != RevealSpeed.NORMAL) {
                cmd.append("reveal_speed:").append(revealSpeed.name().toLowerCase()).append(" ");
            }
        }

        // In animation
        if (inAnimation != null && inAnimation != InAnimationType.NONE) {
            cmd.append("in:").append(inAnimation.name().toLowerCase()).append(" ");
            if (inDuration != 500) cmd.append("in_duration:").append(inDuration).append(" ");
            if (inEasing != null && inEasing != Easing.LINEAR) {
                cmd.append("in_easing:").append(inEasing.name().toLowerCase()).append(" ");
            }
        }

        // Idle animation
        if (idleAnimation != null && idleAnimation != IdleAnimationType.NONE) {
            cmd.append("idle:").append(idleAnimation.name().toLowerCase()).append(" ");
            if (Math.abs(idleIntensity - 1.0f) > 0.01f) {
                cmd.append("idle_intensity:").append(idleIntensity).append(" ");
            }
        }

        // Out animation
        if (outAnimation != null && outAnimation != OutAnimationType.NONE) {
            cmd.append("out:").append(outAnimation.name().toLowerCase()).append(" ");
            if (outDuration != 500) cmd.append("out_duration:").append(outDuration).append(" ");
            if (outEasing != null && outEasing != Easing.LINEAR) {
                cmd.append("out_easing:").append(outEasing.name().toLowerCase()).append(" ");
            }
        }

        cmd.append("\"").append(text != null ? text : "").append("\"");
        return cmd.toString();
    }

    // ==================================================================
    private static Path draftPath() {
        try {
            return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve("titlefx").resolve("editor_draft.json");
        } catch (Exception e) {
            return null;
        }
    }
}
