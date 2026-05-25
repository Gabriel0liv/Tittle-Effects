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

/**
 * TitleFX Visual Editor — redesigned UX.
 *
 * Layout (top→bottom inside panel):
 *  ┌─ Header (title bar) ────────────────────────────────────────┐
 *  │ [Style Cards col]   [Preview zone]  [▶ Reproduzir][↩ Aplicar]│
 *  ├─ Separator ─────────────────────────────────────────────────┤
 *  │ Texto: [___________________________________________]         │
 *  │ Tipo:  [Title] [Subtitle] [Actionbar] [Custom]              │
 *  │ [Revelação…]           [Velocidade…]                        │
 *  │ [Entrada…]                                                   │
 *  │ [Ocioso…]              [Saída…]                              │
 *  │ Cor: [___]  Duração: [____]                                  │
 *  │               [Mostrar avançado ▼]                          │
 *  │ // Advanced rows appear here when toggled                   │
 *  ├─────────────────────────────────────────────────────────────┤
 *  │ [Salvar draft] [Copiar comando] [Enviar Todos] [Resetar] [✕]│
 *  └─────────────────────────────────────────────────────────────┘
 *
 * State is persisted to config/titlefx/editor_draft.json on close.
 * Preview and Apply do NOT close or reset the editor.
 */
public class TextEditorScreen extends Screen {

    // ===================================================================
    // Layout constants
    // ===================================================================
    private static final int W       = 580; // panel width
    private static final int PAD     = 12;  // outer padding
    private static final int GAP     = 8;   // inter-widget gap
    private static final int HDR_H   = 20;  // header height
    private static final int TOP_H   = 230; // style-cards + preview section height
    private static final int ROW_H   = 32;  // height of one form row (8-label + 22-widget + 2 spacing)
    private static final int WID_H   = 22;  // height of interactive widgets
    private static final int FORM_W  = W - 2 * PAD;        // 556
    private static final int HALF_W  = (FORM_W - GAP) / 2; // 274
    private static final int CARD_W  = 128;
    private static final int CARD_H  = 28;
    private static final int CARD_GAP = 4;
    // X offset where preview zone starts (relative to panel-left+PAD)
    private static final int PREV_X_OFF = CARD_W + GAP; // 136

    // ===================================================================
    // Enum cycling arrays  (non-deprecated values only)
    // ===================================================================
    private static final RevealType[] REVEAL_TYPES = {
        RevealType.NONE, RevealType.TYPEWRITER, RevealType.WORD_BY_WORD,
        RevealType.GLYPH_SCRAMBLE, RevealType.OBFUSCATED_DECODE, RevealType.CENTER_OUT,
        RevealType.WIPE_LEFT_TO_RIGHT, RevealType.FADE_CHARS, RevealType.RANDOM_FADE
    };
    private static final RevealSpeed[] REVEAL_SPEEDS = {
        RevealSpeed.INSTANT, RevealSpeed.FAST, RevealSpeed.NORMAL,
        RevealSpeed.CINEMATIC, RevealSpeed.SLOW
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
    private static final String[]  TYPES      = { "title", "subtitle", "actionbar", "custom" };
    private static final String[]  TYPE_LABEL = { "Title", "Subtitle", "Actionbar", "Custom" };
    private static final String[]  ALIGNMENTS = { "center", "left", "right" };
    private static final Easing[]  EASINGS    = Easing.values();

    // ===================================================================
    // Runtime state
    // ===================================================================
    private EditorDraftState draft;
    private boolean          advancedMode = false;
    private String           statusMsg    = "";
    private int              statusTimer  = 0;

    private final EditorPreviewRenderer previewRenderer = new EditorPreviewRenderer();
    private long lastEditMs = 0;
    private boolean previewDirty = false;

    private String lastTextVal = "";
    private String lastColorVal = "";
    private String lastDurationVal = "";
    private String lastScaleVal = "";
    private String lastXVal = "";
    private String lastYVal = "";
    private String lastInDurVal = "";
    private String lastOutDurVal = "";
    private String lastIdleIntVal = "";

    // ===================================================================
    // Widgets — simple mode
    // ===================================================================
    private EditBox  textEdit;
    private EditBox  colorEdit;
    private EditBox  durationEdit;
    private Button[] typeButtons = new Button[4];
    private Button   revealBtn;
    private Button   revealSpeedBtn;
    private Button   inAnimBtn;
    private Button   idleBtn;
    private Button   outBtn;
    private Button   advToggleBtn;
    private Button[] cardBtns = new Button[StyleCard.values().length];

    // ===================================================================
    // Widgets — advanced mode (toggled via advancedWidgets list)
    // ===================================================================
    private EditBox scaleEdit;
    private EditBox xEdit;
    private EditBox yEdit;
    private Button  alignBtn;
    private EditBox inDurEdit;
    private Button  inEasingBtn;
    private EditBox outDurEdit;
    private Button  outEasingBtn;
    private EditBox idleIntEdit;

    private final List<AbstractWidget> advancedWidgets = new ArrayList<>();

    // ===================================================================
    // Constructor
    // ===================================================================
    public TextEditorScreen() {
        super(Component.literal("TitleFX Editor"));
    }

    // ===================================================================
    // Layout helper — single source of truth for all Y coordinates
    // ===================================================================
    private Layout buildLayout() {
        return new Layout(this.width, this.height, advancedMode);
    }

    /** Holds all computed pixel positions for a given screen size + mode. */
    private static final class Layout {
        final int px, py, panelH;
        // form
        final int formTopY, toggleY, advTopY, footerY;

        Layout(int sw, int sh, boolean adv) {
            int advExtra = adv ? (5 * ROW_H + 4) : 0;
            panelH = HDR_H + TOP_H + 2 + 4    // header + top-section + sep + gap
                   + 4 * ROW_H                  // 4 simple form rows (0 to 3)
                   + 4 + WID_H + 4               // gap + toggle btn + gap
                   + advExtra                    // optional advanced rows
                   + 4 + 20 + 4;                 // gap + footer (16 btn + 4 pad)
            px = (sw - W) / 2;
            py = (sh - panelH) / 2;
            formTopY = py + HDR_H + TOP_H + 2 + 4;
            toggleY  = formTopY + 4 * ROW_H + 4;
            advTopY  = toggleY + WID_H + 4;
            footerY  = advTopY + advExtra + 4;
        }

        // Label Y for simple-form row i (8 px above widget)
        int lY(int row) { return formTopY + row * ROW_H; }
        // Widget Y for simple-form row i
        int wY(int row) { return formTopY + row * ROW_H + 8; }
        // Label Y for advanced row i
        int alY(int row) { return advTopY + row * ROW_H; }
        // Widget Y for advanced row i
        int awY(int row) { return advTopY + row * ROW_H + 8; }
    }

    // ===================================================================
    // Screen init
    // ===================================================================
    @Override
    protected void init() {
        super.init();
        advancedWidgets.clear();

        // Load draft only on first open; re-init from same draft on every rebuild
        if (draft == null) {
            draft = EditorDraftState.getInstance();
            advancedMode = draft.advancedMode;
        }

        Layout l  = buildLayout();
        int fx    = l.px + PAD; // left edge of form content

        // ---------------------------------------------------------------
        // STYLE CARD BUTTONS (left column of top section)
        // ---------------------------------------------------------------
        StyleCard[] cards = StyleCard.values();
        int cardColStartY = l.py + HDR_H + 6;
        for (int i = 0; i < cards.length; i++) {
            final StyleCard card = cards[i];
            int cardY = cardColStartY + i * (CARD_H + CARD_GAP);
            boolean sel = card.name().equals(draft.selectedStyleCard);
            cardBtns[i] = Button.builder(
                Component.literal((sel ? "► " : "  ") + card.getLabel()),
                btn -> {
                    syncEditboxesToDraft();
                    card.apply(draft);
                    draft.selectedStyleCard = card.name();
                    markDirtyImmediate();
                    this.init();
                }
            ).bounds(l.px + PAD, cardY, CARD_W, CARD_H)
             .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(card.getDescription())))
             .build();
            this.addRenderableWidget(cardBtns[i]);
        }

        // ---------------------------------------------------------------
        // PREVIEW ACTION BUTTONS (bottom of top section, right column)
        // ---------------------------------------------------------------
        int pvX    = l.px + PAD + PREV_X_OFF;
        int pvW    = W - PAD - PREV_X_OFF - PAD; // ~420
        int pvBtnY = l.py + HDR_H + TOP_H - WID_H - 4;
        int btnW   = (pvW - 2 * GAP) / 3;

        Button reproBtn = Button.builder(
            Component.literal("▶ Reproduzir"), btn -> {
                syncEditboxesToDraft();
                previewRenderer.play(draft);
                setStatus("Preview reproduzido!");
            }
        ).bounds(pvX, pvBtnY, btnW, WID_H).build();
        this.addRenderableWidget(reproBtn);

        Button stopBtn = Button.builder(
            Component.literal("■ Parar"), btn -> {
                previewRenderer.stop();
                setStatus("Preview parado.");
            }
        ).bounds(pvX + btnW + GAP, pvBtnY, btnW, WID_H).build();
        this.addRenderableWidget(stopBtn);

        Button applyBtn = Button.builder(
            Component.literal("↩ Aplicar"), btn -> sendEditedText(false)
        ).bounds(pvX + 2 * (btnW + GAP), pvBtnY, pvW - 2 * btnW - 2 * GAP, WID_H).build();
        this.addRenderableWidget(applyBtn);

        // ---------------------------------------------------------------
        // ROW 0 — Text input
        // ---------------------------------------------------------------
        textEdit = new EditBox(this.font, fx, l.wY(0), FORM_W, WID_H, Component.literal(""));
        textEdit.setValue(draft.text != null ? draft.text : "");
        textEdit.setMaxLength(256);
        this.addRenderableWidget(textEdit);
        lastTextVal = textEdit.getValue();

        // ---------------------------------------------------------------
        // ROW 1 — Type selector (4 toggle buttons)
        // ---------------------------------------------------------------
        int typeW = (FORM_W - 3 * GAP) / 4; // 133 px each
        for (int i = 0; i < 4; i++) {
            final int ti = i;
            boolean sel = TYPES[i].equalsIgnoreCase(draft.type);
            typeButtons[i] = Button.builder(
                Component.literal(sel ? "► " + TYPE_LABEL[i] : TYPE_LABEL[i]),
                btn -> {
                    syncEditboxesToDraft();
                    draft.type    = TYPES[ti];
                    draft.yOffset = Integer.MIN_VALUE; // reset Y to type default
                    markDirtyImmediate();
                    this.init();
                }
            ).bounds(fx + i * (typeW + GAP), l.wY(1), typeW, WID_H).build();
            this.addRenderableWidget(typeButtons[i]);
        }

        // ---------------------------------------------------------------
        // ROW 2 — Reveal type + Reveal speed
        // ---------------------------------------------------------------
        revealBtn = Button.builder(
            Component.literal(AnimationNames.of(draft.revealType)),
            btn -> {
                int idx = (indexOf(REVEAL_TYPES, draft.revealType) + 1) % REVEAL_TYPES.length;
                draft.revealType = REVEAL_TYPES[idx];
                btn.setMessage(Component.literal(AnimationNames.of(draft.revealType)));
                markDirtyImmediate();
            }
        ).bounds(fx, l.wY(2), HALF_W, WID_H).build();
        this.addRenderableWidget(revealBtn);

        revealSpeedBtn = Button.builder(
            Component.literal(AnimationNames.of(draft.revealSpeed)),
            btn -> {
                int idx = (indexOf(REVEAL_SPEEDS, draft.revealSpeed) + 1) % REVEAL_SPEEDS.length;
                draft.revealSpeed = REVEAL_SPEEDS[idx];
                btn.setMessage(Component.literal(AnimationNames.of(draft.revealSpeed)));
                markDirtyImmediate();
            }
        ).bounds(fx + HALF_W + GAP, l.wY(2), HALF_W, WID_H).build();
        this.addRenderableWidget(revealSpeedBtn);

        // ---------------------------------------------------------------
        // ROW 3 — Color
        // ---------------------------------------------------------------
        colorEdit = new EditBox(this.font, fx, l.wY(3), HALF_W, WID_H, Component.literal(""));
        colorEdit.setValue(draft.color != null ? draft.color : "#FFFFFF");
        colorEdit.setMaxLength(32);
        this.addRenderableWidget(colorEdit);
        lastColorVal = colorEdit.getValue();

        // ---------------------------------------------------------------
        // Advanced toggle
        // ---------------------------------------------------------------
        advToggleBtn = Button.builder(
            Component.literal(advancedMode ? "Ocultar avançado ▲" : "Mostrar avançado ▼"),
            btn -> {
                syncEditboxesToDraft();
                advancedMode       = !advancedMode;
                draft.advancedMode = advancedMode;
                this.init();
            }
        ).bounds(fx + FORM_W / 2 - 90, l.toggleY, 180, 16).build();
        this.addRenderableWidget(advToggleBtn);

        // ---------------------------------------------------------------
        // ADVANCED ROW 4 — Duration total
        // ---------------------------------------------------------------
        durationEdit = new EditBox(this.font, fx, l.awY(0), HALF_W, WID_H, Component.literal(""));
        durationEdit.setValue(String.valueOf(draft.effectiveDuration()));
        durationEdit.setMaxLength(10);
        markAdv(durationEdit);
        lastDurationVal = durationEdit.getValue();

        // ---------------------------------------------------------------
        // ADVANCED ROW 5 — In animation (In + Dur + Easing)
        // ---------------------------------------------------------------
        int qW = (HALF_W - GAP) / 2;
        inAnimBtn = Button.builder(
            Component.literal(AnimationNames.of(draft.inAnimation)),
            btn -> {
                int idx = (indexOf(IN_TYPES, draft.inAnimation) + 1) % IN_TYPES.length;
                draft.inAnimation = IN_TYPES[idx];
                btn.setMessage(Component.literal(AnimationNames.of(draft.inAnimation)));
                markDirtyImmediate();
            }
        ).bounds(fx, l.awY(1), HALF_W, WID_H).build();
        markAdv(inAnimBtn);

        inDurEdit = new EditBox(this.font, fx + HALF_W + GAP, l.awY(1), qW, WID_H, Component.literal(""));
        inDurEdit.setValue(String.valueOf(draft.inDuration));
        inDurEdit.setMaxLength(6);
        markAdv(inDurEdit);
        lastInDurVal = inDurEdit.getValue();

        inEasingBtn = Button.builder(
            Component.literal(draft.inEasing != null ? draft.inEasing.name() : "LINEAR"),
            btn -> {
                int idx = (indexOf(EASINGS, draft.inEasing) + 1) % EASINGS.length;
                draft.inEasing = EASINGS[idx];
                btn.setMessage(Component.literal(draft.inEasing.name()));
                markDirtyImmediate();
            }
        ).bounds(fx + HALF_W + GAP + qW + GAP, l.awY(1), qW, WID_H).build();
        markAdv(inEasingBtn);

        // ---------------------------------------------------------------
        // ADVANCED ROW 6 — Idle (Idle + Intensity)
        // ---------------------------------------------------------------
        idleBtn = Button.builder(
            Component.literal(AnimationNames.of(draft.idleAnimation)),
            btn -> {
                int idx = (indexOf(IDLE_TYPES, draft.idleAnimation) + 1) % IDLE_TYPES.length;
                draft.idleAnimation = IDLE_TYPES[idx];
                btn.setMessage(Component.literal(AnimationNames.of(draft.idleAnimation)));
                markDirtyImmediate();
            }
        ).bounds(fx, l.awY(2), HALF_W, WID_H).build();
        markAdv(idleBtn);

        idleIntEdit = new EditBox(this.font, fx + HALF_W + GAP, l.awY(2), HALF_W, WID_H, Component.literal(""));
        idleIntEdit.setValue(String.format("%.2f", draft.idleIntensity));
        idleIntEdit.setMaxLength(6);
        markAdv(idleIntEdit);
        lastIdleIntVal = idleIntEdit.getValue();

        // ---------------------------------------------------------------
        // ADVANCED ROW 7 — Out (Out + Dur + Easing)
        // ---------------------------------------------------------------
        outBtn = Button.builder(
            Component.literal(AnimationNames.of(draft.outAnimation)),
            btn -> {
                int idx = (indexOf(OUT_TYPES, draft.outAnimation) + 1) % OUT_TYPES.length;
                draft.outAnimation = OUT_TYPES[idx];
                btn.setMessage(Component.literal(AnimationNames.of(draft.outAnimation)));
                markDirtyImmediate();
            }
        ).bounds(fx, l.awY(3), HALF_W, WID_H).build();
        markAdv(outBtn);

        outDurEdit = new EditBox(this.font, fx + HALF_W + GAP, l.awY(3), qW, WID_H, Component.literal(""));
        outDurEdit.setValue(String.valueOf(draft.outDuration));
        outDurEdit.setMaxLength(6);
        markAdv(outDurEdit);
        lastOutDurVal = outDurEdit.getValue();

        outEasingBtn = Button.builder(
            Component.literal(draft.outEasing != null ? draft.outEasing.name() : "LINEAR"),
            btn -> {
                int idx = (indexOf(EASINGS, draft.outEasing) + 1) % EASINGS.length;
                draft.outEasing = EASINGS[idx];
                btn.setMessage(Component.literal(draft.outEasing.name()));
                markDirtyImmediate();
            }
        ).bounds(fx + HALF_W + GAP + qW + GAP, l.awY(3), qW, WID_H).build();
        markAdv(outEasingBtn);

        // ---------------------------------------------------------------
        // ADVANCED ROW 8 — Transforms (Scale + X + Y + Align)
        // ---------------------------------------------------------------
        int colW = (FORM_W - 3 * GAP) / 4; // 133 px each
        scaleEdit = new EditBox(this.font, fx, l.awY(4), colW, WID_H, Component.literal(""));
        scaleEdit.setValue(String.format("%.1f", draft.effectiveScale()));
        scaleEdit.setMaxLength(6);
        markAdv(scaleEdit);
        lastScaleVal = scaleEdit.getValue();

        xEdit = new EditBox(this.font, fx + 1 * (colW + GAP), l.awY(4), colW, WID_H, Component.literal(""));
        xEdit.setValue(String.valueOf(draft.xOffset));
        xEdit.setMaxLength(6);
        markAdv(xEdit);
        lastXVal = xEdit.getValue();

        yEdit = new EditBox(this.font, fx + 2 * (colW + GAP), l.awY(4), colW, WID_H, Component.literal(""));
        yEdit.setValue(String.valueOf(draft.effectiveY()));
        yEdit.setMaxLength(6);
        markAdv(yEdit);
        lastYVal = yEdit.getValue();

        alignBtn = Button.builder(
            Component.literal("Alinh.: " + draft.alignment),
            btn -> {
                int idx = (indexOf(ALIGNMENTS, draft.alignment) + 1) % ALIGNMENTS.length;
                draft.alignment = ALIGNMENTS[idx];
                btn.setMessage(Component.literal("Alinh.: " + draft.alignment));
                markDirtyImmediate();
            }
        ).bounds(fx + 3 * (colW + GAP), l.awY(4), colW, WID_H).build();
        markAdv(alignBtn);

        // Apply advanced widget visibility
        for (AbstractWidget w : advancedWidgets) w.visible = advancedMode;

        // ---------------------------------------------------------------
        // FOOTER BUTTONS
        // ---------------------------------------------------------------
        Button saveBtn = Button.builder(Component.literal("Salvar draft"), btn -> {
            syncEditboxesToDraft();
            draft.save();
            setStatus("Draft salvo!");
        }).bounds(fx, l.footerY, 110, 16).build();
        this.addRenderableWidget(saveBtn);

        Button copyBtn = Button.builder(Component.literal("Copiar comando"), btn -> {
            syncEditboxesToDraft();
            Minecraft.getInstance().keyboardHandler.setClipboard(draft.toCommand());
            setStatus("Comando copiado!");
        }).bounds(fx + 110 + GAP, l.footerY, 135, 16).build();
        this.addRenderableWidget(copyBtn);

        Button sendAllBtn = Button.builder(Component.literal("Enviar (Todos)"), btn -> sendEditedText(true))
            .bounds(fx + 110 + GAP + 135 + GAP, l.footerY, 110, 16).build();
        sendAllBtn.visible = advancedMode;
        this.addRenderableWidget(sendAllBtn);

        Button resetBtn = Button.builder(Component.literal("Resetar"), btn -> {
            draft.reset();
            markDirtyImmediate();
            this.init();
        }).bounds(fx + 110 + GAP + 135 + GAP + (advancedMode ? (110 + GAP) : 0), l.footerY, 80, 16).build();
        this.addRenderableWidget(resetBtn);

        Button closeBtn = Button.builder(Component.literal("✕ Fechar"), btn -> this.onClose())
            .bounds(l.px + W - PAD - 72, l.footerY, 72, 16).build();
        this.addRenderableWidget(closeBtn);
    }

    // ===================================================================
    // Internal helpers
    // ===================================================================

    /** Registers an advanced-mode widget: adds to list + adds to screen. */
    private void markAdv(AbstractWidget w) {
        advancedWidgets.add(w);
        this.addRenderableWidget(w);
    }

    private void syncEditboxesToDraft() {
        if (textEdit    != null) draft.text  = textEdit.getValue().trim();
        if (colorEdit   != null) draft.color = colorEdit.getValue().trim();
        if (durationEdit != null) {
            try {
                int v = Integer.parseInt(durationEdit.getValue().trim());
                draft.durationMs = v > 0 ? v : -1;
            } catch (Exception ignored) {}
        }
        if (advancedMode) {
            if (scaleEdit  != null) { try { draft.scale    = Float.parseFloat(scaleEdit.getValue().trim()); }  catch (Exception ignored) {} }
            if (xEdit      != null) { try { draft.xOffset  = Integer.parseInt(xEdit.getValue().trim()); }      catch (Exception ignored) {} }
            if (yEdit      != null) { try { draft.yOffset  = Integer.parseInt(yEdit.getValue().trim()); }      catch (Exception ignored) {} }
            if (inDurEdit  != null) { try { draft.inDuration  = Integer.parseInt(inDurEdit.getValue().trim()); }  catch (Exception ignored) {} }
            if (outDurEdit != null) { try { draft.outDuration = Integer.parseInt(outDurEdit.getValue().trim()); } catch (Exception ignored) {} }
            if (idleIntEdit!= null) { try { draft.idleIntensity = Float.parseFloat(idleIntEdit.getValue().trim()); } catch (Exception ignored) {} }
        }
    }

    private void markDirtyImmediate() {
        previewDirty = true;
        lastEditMs = 0;
    }

    private void markDirtyDebounced() {
        previewDirty = true;
        lastEditMs = System.currentTimeMillis();
    }

    private void sendEditedText(boolean targetAll) {
        syncEditboxesToDraft();
        AnimatedTextPayload p = draft.toPayload();
        com.gabriel.titlefx.common.network.NetworkHandler.CHANNEL.sendToServer(
            new com.gabriel.titlefx.common.network.SendEditedTextPacket(p, targetAll)
        );
        setStatus(targetAll ? "Enviado para todos!" : "Aplicado!");
    }

    private void setStatus(String msg) {
        statusMsg   = msg;
        statusTimer = 100;
    }

    // ===================================================================
    // Tick / lifecycle
    // ===================================================================

    @Override
    public void tick() {
        super.tick();
        if (statusTimer > 0) statusTimer--;

        boolean editBoxChanged = false;

        if (textEdit != null) {
            textEdit.tick();
            String current = textEdit.getValue();
            if (!current.equals(lastTextVal)) {
                lastTextVal = current;
                editBoxChanged = true;
            }
        }
        if (colorEdit != null) {
            colorEdit.tick();
            String current = colorEdit.getValue();
            if (!current.equals(lastColorVal)) {
                lastColorVal = current;
                editBoxChanged = true;
            }
        }
        if (durationEdit != null) {
            durationEdit.tick();
            String current = durationEdit.getValue();
            if (!current.equals(lastDurationVal)) {
                lastDurationVal = current;
                editBoxChanged = true;
            }
        }
        if (scaleEdit != null) {
            scaleEdit.tick();
            String current = scaleEdit.getValue();
            if (!current.equals(lastScaleVal)) {
                lastScaleVal = current;
                editBoxChanged = true;
            }
        }
        if (xEdit != null) {
            xEdit.tick();
            String current = xEdit.getValue();
            if (!current.equals(lastXVal)) {
                lastXVal = current;
                editBoxChanged = true;
            }
        }
        if (yEdit != null) {
            yEdit.tick();
            String current = yEdit.getValue();
            if (!current.equals(lastYVal)) {
                lastYVal = current;
                editBoxChanged = true;
            }
        }
        if (inDurEdit != null) {
            inDurEdit.tick();
            String current = inDurEdit.getValue();
            if (!current.equals(lastInDurVal)) {
                lastInDurVal = current;
                editBoxChanged = true;
            }
        }
        if (outDurEdit != null) {
            outDurEdit.tick();
            String current = outDurEdit.getValue();
            if (!current.equals(lastOutDurVal)) {
                lastOutDurVal = current;
                editBoxChanged = true;
            }
        }
        if (idleIntEdit != null) {
            idleIntEdit.tick();
            String current = idleIntEdit.getValue();
            if (!current.equals(lastIdleIntVal)) {
                lastIdleIntVal = current;
                editBoxChanged = true;
            }
        }

        if (editBoxChanged) {
            markDirtyDebounced();
        }

        // Auto-preview logic
        if (previewDirty && (lastEditMs == 0 || System.currentTimeMillis() - lastEditMs > 300)) {
            syncEditboxesToDraft();
            previewRenderer.play(draft);
            previewDirty = false;
        }
    }

    @Override
    public void onClose() {
        syncEditboxesToDraft();
        draft.save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ===================================================================
    // Render
    // ===================================================================

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        Layout l  = buildLayout();
        int fx    = l.px + PAD;

        // ---- Panel background ----
        g.fill(l.px - 1, l.py - 1, l.px + W + 1, l.py + l.panelH + 1, 0xFF070710);
        g.fill(l.px,     l.py,     l.px + W,     l.py + l.panelH,     0xFF12121E);

        // ---- Header bar ----
        g.fill(l.px, l.py, l.px + W, l.py + HDR_H, 0xFF0C0C1A);
        g.drawCenteredString(this.font, "§bTitleFX §7Visual Editor", l.px + W / 2, l.py + 6, 0xFFFFFF);

        // ---- Style card column (left side of top section) ----
        int cardColStartY = l.py + HDR_H + 6;
        StyleCard[] cards = StyleCard.values();
        for (int i = 0; i < cards.length; i++) {
            int cardY = cardColStartY + i * (CARD_H + CARD_GAP);
            boolean sel = cards[i].name().equals(draft.selectedStyleCard);
            int barColor = sel ? 0xFF5599FF : cards[i].getAccentColor();
            g.fill(l.px + PAD - 3, cardY, l.px + PAD - 1, cardY + CARD_H, barColor);
            if (sel) {
                g.fill(l.px + PAD - 1, cardY - 1, l.px + PAD + CARD_W + 1, cardY, 0xFF334466);
                g.fill(l.px + PAD - 1, cardY + CARD_H, l.px + PAD + CARD_W + 1, cardY + CARD_H + 1, 0xFF334466);
            }
        }
        g.drawString(this.font, "§8Estilos prontos:", l.px + PAD, l.py + HDR_H + 1, 0x505065);

        // ---- Preview zone (right side of top section) ----
        int pvX = l.px + PAD + PREV_X_OFF;
        int pvW = W - PAD - PREV_X_OFF - PAD;
        int pvH = TOP_H - WID_H - 12; // height above action buttons
        int pvY = l.py + HDR_H + 4;
        g.fill(pvX, pvY, pvX + pvW, pvY + pvH, 0xFF0A0A18);
        g.fill(pvX + 1, pvY + 1, pvX + pvW - 1, pvY + pvH - 1, 0xFF0D0D22);
        
        // Render preview inside panel
        previewRenderer.render(g, pvX, pvY, pvW, pvH);

        // ---- Separator ----
        int sepY = l.py + HDR_H + TOP_H;
        g.fill(l.px, sepY, l.px + W, sepY + 1, 0xFF1E1E30);
        g.fill(l.px, sepY + 1, l.px + W, sepY + 2, 0xFF090912);

        // ---- Form labels (simple rows) ----
        g.drawString(this.font, "§8Texto:", fx, l.lY(0), 0x7070A0);
        g.drawString(this.font, "§8Tipo:", fx, l.lY(1), 0x7070A0);
        g.drawString(this.font, "§8Revelação / Velocidade:", fx, l.lY(2), 0x7070A0);
        g.drawString(this.font, "§8Cor:", fx, l.lY(3), 0x7070A0);

        // ---- Advanced labels ----
        if (advancedMode) {
            // A0: durationEdit
            g.drawString(this.font, "§8Duração Total (ms):", fx, l.alY(0), 0x7070A0);
            
            // A1: inAnimBtn(fx), inDurEdit(fx+HALF_W+GAP), inEasingBtn(fx+HALF_W+GAP+qW+GAP)
            g.drawString(this.font, "§8Entrada / Duração / Suavização:", fx, l.alY(1), 0x7070A0);
            
            // A2: idleBtn(fx), idleIntEdit(fx+HALF_W+GAP)
            g.drawString(this.font, "§8Ocioso / Intensidade:", fx, l.alY(2), 0x7070A0);
            
            // A3: outBtn(fx), outDurEdit(fx+HALF_W+GAP), outEasingBtn(fx+HALF_W+GAP+qW+GAP)
            g.drawString(this.font, "§8Saída / Duração / Suavização:", fx, l.alY(3), 0x7070A0);
            
            // A4: scaleEdit, xEdit, yEdit, alignBtn
            g.drawString(this.font, "§8Escala / X / Y / Alinhamento:", fx, l.alY(4), 0x7070A0);
        }

        // ---- Status message (fades out) ----
        if (statusTimer > 0 && !statusMsg.isEmpty()) {
            float alpha = Math.min(1.0f, statusTimer / 20.0f);
            int color = ((int)(alpha * 0xFF) << 24) | 0x55FF55;
            g.drawCenteredString(this.font, statusMsg, l.px + W / 2, l.py + l.panelH - 7, color);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    // ===================================================================
    // Utility
    // ===================================================================

    private static <T> int indexOf(T[] arr, T val) {
        for (int i = 0; i < arr.length; i++) if (arr[i] == val) return i;
        return 0;
    }

    private static int indexOf(String[] arr, String val) {
        if (val == null) return 0;
        for (int i = 0; i < arr.length; i++) if (arr[i].equalsIgnoreCase(val)) return i;
        return 0;
    }
}
