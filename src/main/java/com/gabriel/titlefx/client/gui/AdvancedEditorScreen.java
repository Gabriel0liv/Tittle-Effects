package com.gabriel.titlefx.client.gui;

import com.gabriel.titlefx.common.animation.*;
import com.gabriel.titlefx.common.model.AnimatedTextPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class AdvancedEditorScreen extends Screen {

    private final TextEditorScreen parent;
    private final EditorDraftState draft;

    // Layout constants
    private static final int PAD = 12;
    private static final int GAP = 8;
    private static final int HDR_H = 20;

    // Enum arrays and metadata
    private static final String[] TYPES = { "title", "subtitle", "actionbar", "custom" };
    private static final String[] TYPE_LABELS = { "Título", "Sub", "Barra", "Custom" };

    private static final RevealType[] REVEAL_TYPES = {
        RevealType.NONE, RevealType.TYPEWRITER, RevealType.WORD_BY_WORD,
        RevealType.GLYPH_SCRAMBLE, RevealType.OBFUSCATED_DECODE, RevealType.CENTER_OUT,
        RevealType.WIPE_LEFT_TO_RIGHT, RevealType.FADE_CHARS, RevealType.RANDOM_FADE
    };

    private static final InAnimationType[] IN_TYPES = {
        InAnimationType.NONE, InAnimationType.FADE_IN, InAnimationType.CINEMATIC_ZOOM_IN,
        InAnimationType.SOFT_POP, InAnimationType.SCALE_REVEAL
    };
    private static final IdleAnimationType[] IDLE_TYPES = {
        IdleAnimationType.NONE, IdleAnimationType.SUBTLE_PULSE, IdleAnimationType.BREATHING,
        IdleAnimationType.SUBTLE_SHAKE, IdleAnimationType.WAVE_SOFT, IdleAnimationType.FLICKER
    };
    private static final OutAnimationType[] OUT_TYPES = {
        OutAnimationType.NONE, OutAnimationType.FADE_OUT, OutAnimationType.DISSOLVE,
        OutAnimationType.SHRINK_FADE
    };
    private static final String[] ALIGNMENTS = { "center", "left", "right" };
    private static final String[] ALIGN_LABELS = { "Centro", "Esq", "Dir" };
    private static final Easing[] EASINGS = Easing.values();

    // Widgets
    private EditBox durationEdit;
    private EditBox colorEdit;
    private Button[] typeBtns = new Button[4];
    private Button revealBtn;
    private Button inAnimBtn;
    private EditBox inDurEdit;
    private Button inEasingBtn;
    private Button idleBtn;
    private EditBox idleIntEdit;
    private Button outBtn;
    private EditBox outDurEdit;
    private Button outEasingBtn;
    private EditBox scaleEdit;
    private EditBox xEdit;
    private EditBox yEdit;
    private Button[] alignBtns = new Button[3];
    private Button sendAllBtn;

    // Status & confirmation state
    private String statusMsg = "";
    private int statusTimer = 0;
    private boolean confirmSendAll = false;
    private int confirmTimer = 0;

    public AdvancedEditorScreen(TextEditorScreen parent) {
        super(Component.literal("Configurações Avançadas"));
        this.parent = parent;
        this.draft = EditorDraftState.getInstance();
    }

    private Layout buildLayout() {
        return new Layout(this.width, this.height);
    }

    private static final class Layout {
        final int px, py, panelW, panelH;
        final int formTopY, footerY;
        final int rowH, widH;
        final boolean compact;

        Layout(int sw, int sh) {
            this.compact = sw < 680 || sh < 460;
            int availableW = Math.max(260, sw - 24);
            int preferredW = compact ? 520 : 620;
            panelW = Math.min(preferredW, availableW);

            rowH = sh >= 380 ? 28 : 22;
            widH = sh >= 380 ? 18 : 14;

            int neededH = HDR_H + PAD + 6 * rowH + GAP + 20 + PAD;
            int availableH = Math.max(200, sh - 16);
            panelH = Math.min(neededH, availableH);

            px = (sw - panelW) / 2;
            py = (sh - panelH) / 2;
            formTopY = py + HDR_H + PAD;
            footerY = py + panelH - 20 - PAD;
        }

        int wY(int row) {
            return formTopY + row * rowH + (rowH - widH) / 2 + 4;
        }

        int lY(int row) {
            return formTopY + row * rowH - 2;
        }
    }

    @Override
    protected void init() {
        this.clearWidgets();
        super.init();

        confirmSendAll = false;
        confirmTimer = 0;

        Layout l = buildLayout();
        int fx = l.px + PAD;
        int formW = l.panelW - 2 * PAD;
        int halfW = (formW - GAP) / 2;
        int qW = (halfW - GAP) / 2;

        // ROW 0 — Duration & Color
        durationEdit = new EditBox(this.font, fx, l.wY(0), halfW, l.widH, Component.literal(""));
        durationEdit.setValue(String.valueOf(draft.effectiveDuration()));
        durationEdit.setMaxLength(10);
        this.addRenderableWidget(durationEdit);

        colorEdit = new EditBox(this.font, fx + halfW + GAP, l.wY(0), halfW, l.widH, Component.literal(""));
        colorEdit.setValue(draft.color != null ? draft.color : "#FFFFFF");
        colorEdit.setMaxLength(32);
        this.addRenderableWidget(colorEdit);

        // ROW 1 — Tipo & Revelação
        int typeW = (halfW - 3 * 2) / 4;
        for (int i = 0; i < 4; i++) {
            final int ti = i;
            boolean sel = TYPES[i].equalsIgnoreCase(draft.type);
            String shown = TYPE_LABELS[i];
            typeBtns[i] = Button.builder(
                fitLabel(getSegmentedLabel(shown, sel, typeW), typeW),
                btn -> {
                    draft.type = TYPES[ti];
                    draft.yOffset = Integer.MIN_VALUE; // reset default Y
                    updateTypeButtons(typeW);
                    onTypeChanged();
                    draft.save();
                }
            ).bounds(fx + i * (typeW + 2), l.wY(1), typeW, l.widH).build();
            this.addRenderableWidget(typeBtns[i]);
        }
        updateTypeButtons(typeW);

        revealBtn = Button.builder(
            fitLabel("Revelar: " + getRevealLabel(draft.revealType), halfW),
            btn -> {
                int idx = (indexOf(REVEAL_TYPES, draft.revealType) + 1) % REVEAL_TYPES.length;
                draft.revealType = REVEAL_TYPES[idx];
                btn.setMessage(fitLabel("Revelar: " + getRevealLabel(draft.revealType), halfW));
                draft.save();
            }
        ).bounds(fx + halfW + GAP, l.wY(1), halfW, l.widH).build();
        this.addRenderableWidget(revealBtn);

        // ROW 2 — In Animation
        inAnimBtn = Button.builder(
            fitLabel("Entrada: " + AnimationNames.of(draft.inAnimation), halfW),
            btn -> {
                int idx = (indexOf(IN_TYPES, draft.inAnimation) + 1) % IN_TYPES.length;
                draft.inAnimation = IN_TYPES[idx];
                btn.setMessage(fitLabel("Entrada: " + AnimationNames.of(draft.inAnimation), halfW));
                draft.save();
            }
        ).bounds(fx, l.wY(2), halfW, l.widH).build();
        this.addRenderableWidget(inAnimBtn);

        inDurEdit = new EditBox(this.font, fx + halfW + GAP, l.wY(2), qW, l.widH, Component.literal(""));
        inDurEdit.setValue(String.valueOf(draft.inDuration));
        inDurEdit.setMaxLength(6);
        this.addRenderableWidget(inDurEdit);

        inEasingBtn = Button.builder(
            fitLabel(draft.inEasing != null ? draft.inEasing.name() : "LINEAR", qW),
            btn -> {
                int idx = (indexOf(EASINGS, draft.inEasing) + 1) % EASINGS.length;
                draft.inEasing = EASINGS[idx];
                btn.setMessage(fitLabel(draft.inEasing.name(), qW));
                draft.save();
            }
        ).bounds(fx + halfW + GAP + qW + GAP, l.wY(2), qW, l.widH).build();
        this.addRenderableWidget(inEasingBtn);

        // ROW 3 — Idle Animation
        idleBtn = Button.builder(
            fitLabel("Ocioso: " + AnimationNames.of(draft.idleAnimation), halfW),
            btn -> {
                int idx = (indexOf(IDLE_TYPES, draft.idleAnimation) + 1) % IDLE_TYPES.length;
                draft.idleAnimation = IDLE_TYPES[idx];
                btn.setMessage(fitLabel("Ocioso: " + AnimationNames.of(draft.idleAnimation), halfW));
                draft.save();
            }
        ).bounds(fx, l.wY(3), halfW, l.widH).build();
        this.addRenderableWidget(idleBtn);

        idleIntEdit = new EditBox(this.font, fx + halfW + GAP, l.wY(3), halfW, l.widH, Component.literal(""));
        idleIntEdit.setValue(String.format(java.util.Locale.US, "%.2f", draft.idleIntensity));
        idleIntEdit.setMaxLength(6);
        this.addRenderableWidget(idleIntEdit);

        // ROW 4 — Out Animation
        outBtn = Button.builder(
            fitLabel("Saída: " + AnimationNames.of(draft.outAnimation), halfW),
            btn -> {
                int idx = (indexOf(OUT_TYPES, draft.outAnimation) + 1) % OUT_TYPES.length;
                draft.outAnimation = OUT_TYPES[idx];
                btn.setMessage(fitLabel("Saída: " + AnimationNames.of(draft.outAnimation), halfW));
                draft.save();
            }
        ).bounds(fx, l.wY(4), halfW, l.widH).build();
        this.addRenderableWidget(outBtn);

        outDurEdit = new EditBox(this.font, fx + halfW + GAP, l.wY(4), qW, l.widH, Component.literal(""));
        outDurEdit.setValue(String.valueOf(draft.outDuration));
        outDurEdit.setMaxLength(6);
        this.addRenderableWidget(outDurEdit);

        outEasingBtn = Button.builder(
            fitLabel(draft.outEasing != null ? draft.outEasing.name() : "LINEAR", qW),
            btn -> {
                int idx = (indexOf(EASINGS, draft.outEasing) + 1) % EASINGS.length;
                draft.outEasing = EASINGS[idx];
                btn.setMessage(fitLabel(draft.outEasing.name(), qW));
                draft.save();
            }
        ).bounds(fx + halfW + GAP + qW + GAP, l.wY(4), qW, l.widH).build();
        this.addRenderableWidget(outEasingBtn);

        // ROW 5 — Transforms (Scale + X + Y + Segmented Align)
        int colW = (formW - 3 * GAP) / 4;
        scaleEdit = new EditBox(this.font, fx, l.wY(5), colW, l.widH, Component.literal(""));
        scaleEdit.setValue(String.format(java.util.Locale.US, "%.1f", draft.effectiveScale()));
        scaleEdit.setMaxLength(6);
        this.addRenderableWidget(scaleEdit);

        xEdit = new EditBox(this.font, fx + colW + GAP, l.wY(5), colW, l.widH, Component.literal(""));
        xEdit.setValue(String.valueOf(draft.xOffset));
        xEdit.setMaxLength(6);
        this.addRenderableWidget(xEdit);

        yEdit = new EditBox(this.font, fx + 2 * (colW + GAP), l.wY(5), colW, l.widH, Component.literal(""));
        yEdit.setValue(String.valueOf(draft.effectiveY()));
        yEdit.setMaxLength(6);
        this.addRenderableWidget(yEdit);

        // Segmented Align
        int alignAreaW = formW - 3 * colW - 3 * GAP;
        int alignBtnW = (alignAreaW - 2 * GAP) / 3;
        for (int i = 0; i < 3; i++) {
            final int ai = i;
            boolean sel = ALIGNMENTS[i].equalsIgnoreCase(draft.alignment);
            alignBtns[i] = Button.builder(
                fitLabel(getSegmentedLabel(ALIGN_LABELS[i], sel, alignBtnW), alignBtnW),
                btn -> {
                    draft.alignment = ALIGNMENTS[ai];
                    updateAlignButtons(alignBtnW);
                    draft.save();
                }
            ).bounds(fx + 3 * (colW + GAP) + i * (alignBtnW + GAP), l.wY(5), alignBtnW, l.widH).build();
            this.addRenderableWidget(alignBtns[i]);
        }

        // FOOTER BUTTONS
        int fBtnW = (formW - 2 * GAP) / 3;
        Button backBtn = Button.builder(fitLabel("Voltar", fBtnW), btn -> {
            syncToDraft();
            parent.markDirtyImmediate();
            parent.syncDraftToWidgets();
            Minecraft.getInstance().setScreen(parent);
        }).bounds(fx, l.footerY, fBtnW, 16).build();
        this.addRenderableWidget(backBtn);

        Button applyBtn = Button.builder(fitLabel("Aplicar", fBtnW), btn -> {
            syncToDraft();
            sendEditedText(false);
        }).bounds(fx + fBtnW + GAP, l.footerY, fBtnW, 16).build();
        this.addRenderableWidget(applyBtn);

        sendAllBtn = Button.builder(fitLabel("Enviar (Todos)", fBtnW), btn -> {
            if (!confirmSendAll) {
                confirmSendAll = true;
                confirmTimer = 60; // 3 seconds
                btn.setMessage(fitLabel("Confirmar?", fBtnW));
            } else {
                syncToDraft();
                sendEditedText(true);
                confirmSendAll = false;
                confirmTimer = 0;
                btn.setMessage(fitLabel("Enviar (Todos)", fBtnW));
            }
        }).bounds(fx + 2 * (fBtnW + GAP), l.footerY, formW - 2 * fBtnW - 2 * GAP, 16).build();
        this.addRenderableWidget(sendAllBtn);
    }

    private void updateTypeButtons(int w) {
        for (int i = 0; i < 4; i++) {
            if (typeBtns[i] != null) {
                boolean sel = TYPES[i].equalsIgnoreCase(draft.type);
                String shown = TYPE_LABELS[i];
                typeBtns[i].setMessage(fitLabel(getSegmentedLabel(shown, sel, w), w));
            }
        }
    }

    private void updateAlignButtons(int w) {
        for (int i = 0; i < 3; i++) {
            if (alignBtns[i] != null) {
                boolean sel = ALIGNMENTS[i].equalsIgnoreCase(draft.alignment);
                String shown = ALIGN_LABELS[i];
                alignBtns[i].setMessage(fitLabel(getSegmentedLabel(shown, sel, w), w));
            }
        }
    }

    private void onTypeChanged() {
        if (scaleEdit != null && draft.scale <= 0) {
            scaleEdit.setValue(String.format(java.util.Locale.US, "%.1f", draft.effectiveScale()));
        }
        if (yEdit != null && draft.yOffset == Integer.MIN_VALUE) {
            yEdit.setValue(String.valueOf(draft.effectiveY()));
        }
        if (durationEdit != null && draft.durationMs <= 0) {
            durationEdit.setValue(String.valueOf(draft.effectiveDuration()));
        }
    }

    private void syncToDraft() {
        if (durationEdit != null) {
            try {
                int v = Integer.parseInt(durationEdit.getValue().trim());
                draft.durationMs = v > 0 ? v : -1;
            } catch (Exception ignored) {}
        }
        if (colorEdit != null) {
            draft.color = colorEdit.getValue().trim();
        }
        if (scaleEdit != null) {
            try { draft.scale = Float.parseFloat(scaleEdit.getValue().trim()); } catch (Exception ignored) {}
        }
        if (xEdit != null) {
            try { draft.xOffset = Integer.parseInt(xEdit.getValue().trim()); } catch (Exception ignored) {}
        }
        if (yEdit != null) {
            try { draft.yOffset = Integer.parseInt(yEdit.getValue().trim()); } catch (Exception ignored) {}
        }
        if (inDurEdit != null) {
            try { draft.inDuration = Integer.parseInt(inDurEdit.getValue().trim()); } catch (Exception ignored) {}
        }
        if (outDurEdit != null) {
            try { draft.outDuration = Integer.parseInt(outDurEdit.getValue().trim()); } catch (Exception ignored) {}
        }
        if (idleIntEdit != null) {
            try { draft.idleIntensity = Float.parseFloat(idleIntEdit.getValue().trim()); } catch (Exception ignored) {}
        }
        draft.save();
    }

    private void sendEditedText(boolean targetAll) {
        AnimatedTextPayload p = draft.toPayload();
        com.gabriel.titlefx.common.network.NetworkHandler.CHANNEL.sendToServer(
            new com.gabriel.titlefx.common.network.SendEditedTextPacket(p, targetAll)
        );
        setStatus(targetAll ? "Enviado para todos!" : "Aplicado!");
    }

    private void setStatus(String msg) {
        statusMsg = msg;
        statusTimer = 80;
    }

    @Override
    public void tick() {
        super.tick();
        if (statusTimer > 0) statusTimer--;

        if (confirmTimer > 0) {
            confirmTimer--;
            if (confirmTimer == 0) {
                confirmSendAll = false;
                if (sendAllBtn != null) {
                    Layout l = buildLayout();
                    int formW = l.panelW - 2 * PAD;
                    int fBtnW = (formW - 2 * GAP) / 3;
                    sendAllBtn.setMessage(fitLabel("Enviar (Todos)", fBtnW));
                }
            }
        }

        if (durationEdit != null) durationEdit.tick();
        if (colorEdit != null) colorEdit.tick();
        if (inDurEdit != null) inDurEdit.tick();
        if (outDurEdit != null) outDurEdit.tick();
        if (scaleEdit != null) scaleEdit.tick();
        if (xEdit != null) xEdit.tick();
        if (yEdit != null) yEdit.tick();
        if (idleIntEdit != null) idleIntEdit.tick();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        Layout l = buildLayout();
        int fx = l.px + PAD;

        // Background
        g.fill(l.px - 1, l.py - 1, l.px + l.panelW + 1, l.py + l.panelH + 1, 0xFF070710);
        g.fill(l.px, l.py, l.px + l.panelW, l.py + l.panelH, 0xFF12121E);

        // Header
        g.fill(l.px, l.py, l.px + l.panelW, l.py + HDR_H, 0xFF0C0C1A);
        g.drawCenteredString(this.font, "§bTitleFX §7Configurações Avançadas", l.px + l.panelW / 2, l.py + 6, 0xFFFFFF);

        // Labels
        g.drawString(this.font, "§8Duração e Cor:", fx, l.lY(0), 0x7070A0);
        g.drawString(this.font, "§8Tipo e Revelação:", fx, l.lY(1), 0x7070A0);
        g.drawString(this.font, "§8Entrada / Duração / Suavização:", fx, l.lY(2), 0x7070A0);
        g.drawString(this.font, "§8Ocioso / Intensidade:", fx, l.lY(3), 0x7070A0);
        g.drawString(this.font, "§8Saída / Duração / Suavização:", fx, l.lY(4), 0x7070A0);
        g.drawString(this.font, "§8Escala / X / Y / Alinhamento:", fx, l.lY(5), 0x7070A0);

        // Status message
        if (statusTimer > 0 && !statusMsg.isEmpty()) {
            float alpha = Math.min(1.0f, statusTimer / 20.0f);
            int color = ((int) (alpha * 0xFF) << 24) | 0x55FF55;
            g.drawCenteredString(this.font, statusMsg, l.px + l.panelW / 2, l.py + l.panelH - 7, color);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    private Component fitLabel(String text, int width) {
        if (text == null) text = "";
        int maxWidth = Math.max(6, width - 10);

        if (this.font.width(text) <= maxWidth) {
            return Component.literal(text);
        }

        String ellipsis = "…";
        String s = text;

        while (s.length() > 1 && this.font.width(s + ellipsis) > maxWidth) {
            s = s.substring(0, s.length() - 1);
        }

        return Component.literal(s + ellipsis);
    }

    private String getSegmentedLabel(String label, boolean selected, int width) {
        if (!selected) return label;
        int maxWidth = Math.max(6, width - 10);
        String withSpace = "► " + label;
        if (this.font.width(withSpace) <= maxWidth) {
            return withSpace;
        }
        String withoutSpace = "►" + label;
        if (this.font.width(withoutSpace) <= maxWidth) {
            return withoutSpace;
        }
        return label;
    }

    private String getRevealLabel(RevealType type) {
        if (type == null) return "Nenhum";
        switch (type) {
            case NONE: return "Nenhum";
            case TYPEWRITER: return "Digitação";
            case WORD_BY_WORD: return "Palavra";
            case GLYPH_SCRAMBLE: return "Embaralhar";
            case OBFUSCATED_DECODE: return "Decodificar";
            case CENTER_OUT: return "Centro-Out";
            case WIPE_LEFT_TO_RIGHT: return "Varredura";
            case FADE_CHARS: return "Letras";
            case RANDOM_FADE: return "Aleatório";
            default: return type.name();
        }
    }

    private static <T> int indexOf(T[] arr, T val) {
        for (int i = 0; i < arr.length; i++) if (arr[i] == val) return i;
        return 0;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
