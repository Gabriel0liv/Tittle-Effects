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

public class TextEditorScreen extends Screen {

    // Layout constants
    private static final int PAD = 12;
    private static final int GAP = 8;
    private static final int HDR_H = 20;

    // Segment arrays
    private static final String[] TYPES = { "title", "subtitle", "actionbar" };
    private static final String[] TYPE_LABELS = { "Título", "Sub", "Barra" };

    private static final RevealSpeed[] REVEAL_SPEEDS = {
        RevealSpeed.INSTANT, RevealSpeed.FAST, RevealSpeed.NORMAL, RevealSpeed.CINEMATIC, RevealSpeed.SLOW
    };
    private static final String[] SPEED_LABELS = { "Inst", "Rápido", "Normal", "Cine", "Lento" };

    // Runtime state
    private EditorDraftState draft;
    private String statusMsg = "";
    private int statusTimer = 0;

    private final EditorPreviewRenderer previewRenderer = new EditorPreviewRenderer();
    private long lastEditMs = 0;
    private boolean previewDirty = false;

    private String lastTextVal = "";
    private String lastColorVal = "";

    // Widgets
    private EditBox textEdit;
    private EditBox colorEdit;
    private Button[] typeButtons = new Button[3];
    private Button[] speedButtons = new Button[5];
    private Button[] cardBtns = new Button[StyleCard.values().length];

    public TextEditorScreen() {
        super(Component.literal("TitleFX Editor"));
    }

    private Layout buildLayout() {
        return new Layout(this.width, this.height);
    }

    private static final class Layout {
        final int px, py, panelW, panelH;
        final int previewH;
        final int cardCols, cardRows, cardsH;
        final int formTopY;
        final int rowH, widH;
        final int footerY;
        final boolean compact;

        Layout(int sw, int sh) {
            this.compact = sw < 680 || sh < 460;
            int availableW = Math.max(260, sw - 24);
            int preferredW = compact ? 520 : 620;
            this.panelW = Math.min(preferredW, availableW);

            int availableH = Math.max(220, sh - 16);
            int preferredH = compact ? 330 : 390;
            this.panelH = Math.min(preferredH, availableH);

            this.px = (sw - panelW) / 2;
            this.py = (sh - panelH) / 2;

            this.rowH = panelH >= 380 ? 28 : 22;
            this.widH = panelH >= 380 ? 18 : 14;

            this.previewH = (int) Math.max(80.0, Math.min(130.0, (panelH - 240) * 0.5));
            this.cardCols = panelW >= 560 ? 3 : 2;
            this.cardRows = panelW >= 560 ? 2 : 3;
            this.cardsH = cardRows * 22 + (cardRows - 1) * 4;

            int previewActionAreaH = widH + 8;
            int cardsTopY = py + HDR_H + 4 + previewH + 4 + previewActionAreaH + 4;
            this.formTopY = cardsTopY + cardsH + 6;
            this.footerY = py + panelH - 20 - PAD;
        }

        int wY(int row) {
            return formTopY + row * rowH + (rowH - widH) / 2;
        }

        int lY(int row) {
            return formTopY + row * rowH - 2;
        }
    }

    private boolean fits(Layout l, int row) {
        return l.wY(row) + l.widH <= l.footerY - 4;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        super.init();

        // Load draft only on first open
        if (draft == null) {
            draft = EditorDraftState.getInstance();
        }

        Layout l = buildLayout();
        int fx = l.px + PAD;
        int formW = l.panelW - 2 * PAD;
        int halfW = (formW - GAP) / 2;

        // ---------------------------------------------------------------
        // PREVIEW ACTION BUTTONS
        // ---------------------------------------------------------------
        int pvX = l.px + PAD;
        int pvW = l.panelW - 2 * PAD;
        int pvBtnY = l.py + HDR_H + 4 + l.previewH + 4;
        int btnW = (pvW - 2 * GAP) / 3;

        if (pvBtnY + l.widH <= l.footerY - 4) {
            Button reproBtn = Button.builder(
                fitLabel("▶ Reproduzir", btnW), btn -> {
                    syncEditboxesToDraft();
                    previewRenderer.play(draft, pvW);
                    setStatus("Preview reproduzido!");
                }
            ).bounds(pvX, pvBtnY, btnW, l.widH).build();
            this.addRenderableWidget(reproBtn);

            Button stopBtn = Button.builder(
                fitLabel("■ Parar", btnW), btn -> {
                    previewRenderer.stop();
                    setStatus("Preview parado.");
                }
            ).bounds(pvX + btnW + GAP, pvBtnY, btnW, l.widH).build();
            this.addRenderableWidget(stopBtn);

            Button applyBtn = Button.builder(
                fitLabel("↩ Aplicar", pvW - 2 * btnW - 2 * GAP), btn -> sendEditedText(false)
            ).bounds(pvX + 2 * (btnW + GAP), pvBtnY, pvW - 2 * btnW - 2 * GAP, l.widH).build();
            this.addRenderableWidget(applyBtn);
        }

        // ---------------------------------------------------------------
        // STYLE CARDS GRID
        // ---------------------------------------------------------------
        StyleCard[] styleCards = StyleCard.values();
        int cardW = (formW - (l.cardCols - 1) * 4) / l.cardCols;
        for (int i = 0; i < styleCards.length; i++) {
            final StyleCard card = styleCards[i];
            int col = i % l.cardCols;
            int row = i / l.cardCols;
            int cardX = fx + col * (cardW + 4);
            int cardY = l.py + HDR_H + 4 + l.previewH + 4 + l.widH + 8 + row * (22 + 4);

            if (cardY + 22 <= l.footerY - 4) {
                boolean sel = card.name().equals(draft.selectedStyleCard);
                String cardLabel = getShortCardLabel(card, l.compact);
                cardBtns[i] = segmentedButton(cardLabel, sel, cardX, cardY, cardW, 22, () -> {
                    syncEditboxesToDraft();
                    card.apply(draft);
                    draft.selectedStyleCard = card.name();
                    markDirtyImmediate();
                    syncDraftToWidgets();
                });
                cardBtns[i].setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(card.getDescription())));
                this.addRenderableWidget(cardBtns[i]);
            } else {
                cardBtns[i] = null;
            }
        }

        int currentRow = 0;

        // ---------------------------------------------------------------
        // ROW 0 — Text input
        // ---------------------------------------------------------------
        if (fits(l, currentRow)) {
            textEdit = new EditBox(this.font, fx, l.wY(currentRow), formW, l.widH, Component.literal(""));
            textEdit.setValue(draft.text != null ? draft.text : "");
            textEdit.setMaxLength(256);
            this.addRenderableWidget(textEdit);
            lastTextVal = textEdit.getValue();
            currentRow++;
        } else {
            textEdit = null;
        }

        // ---------------------------------------------------------------
        // ROW 1 — Type selector (only if NOT compact)
        // ---------------------------------------------------------------
        if (!l.compact && fits(l, currentRow)) {
            int typeW = (formW - 2 * GAP) / 3;
            for (int i = 0; i < 3; i++) {
                final int ti = i;
                boolean sel = TYPES[i].equalsIgnoreCase(draft.type);
                typeButtons[i] = segmentedButton(TYPE_LABELS[i], sel, fx + i * (typeW + GAP), l.wY(currentRow), typeW, l.widH, () -> {
                    syncEditboxesToDraft();
                    draft.type = TYPES[ti];
                    draft.yOffset = Integer.MIN_VALUE; // reset to type default
                    markDirtyImmediate();
                    updateTypeButtons();
                });
                this.addRenderableWidget(typeButtons[i]);
            }
            currentRow++;
        } else {
            for (int i = 0; i < 3; i++) typeButtons[i] = null;
        }

        // ---------------------------------------------------------------
        // ROW 2 — Speed selector
        // ---------------------------------------------------------------
        if (fits(l, currentRow)) {
            int speedW = (formW - 4 * GAP) / 5;
            for (int i = 0; i < 5; i++) {
                final int si = i;
                boolean sel = REVEAL_SPEEDS[i] == draft.revealSpeed;
                speedButtons[i] = segmentedButton(SPEED_LABELS[i], sel, fx + i * (speedW + GAP), l.wY(currentRow), speedW, l.widH, () -> {
                    syncEditboxesToDraft();
                    draft.revealSpeed = REVEAL_SPEEDS[si];
                    markDirtyImmediate();
                    updateSpeedButtons();
                });
                this.addRenderableWidget(speedButtons[i]);
            }
            currentRow++;
        } else {
            for (int i = 0; i < 5; i++) speedButtons[i] = null;
        }

        // ---------------------------------------------------------------
        // ROW 3 — Color input (only if NOT compact)
        // ---------------------------------------------------------------
        if (!l.compact && fits(l, currentRow)) {
            colorEdit = new EditBox(this.font, fx, l.wY(currentRow), halfW, l.widH, Component.literal(""));
            colorEdit.setValue(draft.color != null ? draft.color : "#FFFFFF");
            colorEdit.setMaxLength(32);
            this.addRenderableWidget(colorEdit);
            lastColorVal = colorEdit.getValue();
            currentRow++;
        } else {
            colorEdit = null;
        }

        // ---------------------------------------------------------------
        // FOOTER BUTTONS
        // ---------------------------------------------------------------
        int footerBtnW = (formW - 3 * GAP) / 4;
        int remainingW = formW - 3 * footerBtnW - 3 * GAP;

        Button copyBtn = Button.builder(fitLabel("Copiar", footerBtnW), btn -> {
            syncEditboxesToDraft();
            Minecraft.getInstance().keyboardHandler.setClipboard(draft.toCommand());
            setStatus("Comando copiado!");
        }).bounds(fx, l.footerY, footerBtnW, 16).build();
        this.addRenderableWidget(copyBtn);

        Button resetBtn = Button.builder(fitLabel("Resetar", footerBtnW), btn -> {
            draft.reset();
            syncDraftToWidgets();
            markDirtyImmediate();
            setStatus("Configurações resetadas.");
        }).bounds(fx + footerBtnW + GAP, l.footerY, footerBtnW, 16).build();
        this.addRenderableWidget(resetBtn);

        Button moreBtn = Button.builder(fitLabel("Avançado", footerBtnW), btn -> {
            syncEditboxesToDraft();
            Minecraft.getInstance().setScreen(new AdvancedEditorScreen(this));
        }).bounds(fx + 2 * (footerBtnW + GAP), l.footerY, footerBtnW, 16).build();
        this.addRenderableWidget(moreBtn);

        Button closeBtn = Button.builder(fitLabel("✕ Fechar", remainingW), btn -> this.onClose())
            .bounds(fx + 3 * (footerBtnW + GAP), l.footerY, remainingW, 16).build();
        this.addRenderableWidget(closeBtn);
    }

    private String getShortCardLabel(StyleCard card, boolean compact) {
        if (compact) {
            switch (card) {
                case BOSS_CINEMATICO: return "Boss";
                case MISSAO: return "Missão";
                case LOCALIZACAO: return "Local";
                case AVISO: return "Aviso";
                case SISTEMA: return "Sistema";
                case ACHIEVEMENT: return "Achiev";
                default: return card.getLabel();
            }
        }
        return card.getLabel();
    }

    private Button segmentedButton(String label, boolean selected, int x, int y, int w, int h, Runnable onClick) {
        String shown = getSegmentedLabel(label, selected, w);
        return Button.builder(fitLabel(shown, w), btn -> onClick.run())
            .bounds(x, y, w, h)
            .build();
    }

    public void syncDraftToWidgets() {
        if (textEdit != null) {
            textEdit.setValue(draft.text != null ? draft.text : "");
            lastTextVal = textEdit.getValue();
        }
        if (colorEdit != null) {
            colorEdit.setValue(draft.color != null ? draft.color : "#FFFFFF");
            lastColorVal = colorEdit.getValue();
        }
        updateCardButtons();
        updateTypeButtons();
        updateSpeedButtons();
    }

    private void updateCardButtons() {
        StyleCard[] styleCards = StyleCard.values();
        Layout l = buildLayout();
        int formW = l.panelW - 2 * PAD;
        int cardW = (formW - (l.cardCols - 1) * 4) / l.cardCols;
        for (int i = 0; i < styleCards.length; i++) {
            if (cardBtns[i] != null) {
                StyleCard card = styleCards[i];
                boolean sel = card.name().equals(draft.selectedStyleCard);
                String cardLabel = getShortCardLabel(card, l.compact);
                String shown = getSegmentedLabel(cardLabel, sel, cardW);
                cardBtns[i].setMessage(fitLabel(shown, cardW));
            }
        }
    }

    private void updateTypeButtons() {
        Layout l = buildLayout();
        int formW = l.panelW - 2 * PAD;
        int typeW = (formW - 2 * GAP) / 3;
        for (int i = 0; i < 3; i++) {
            if (typeButtons[i] != null) {
                boolean sel = TYPES[i].equalsIgnoreCase(draft.type);
                String shown = getSegmentedLabel(TYPE_LABELS[i], sel, typeW);
                typeButtons[i].setMessage(fitLabel(shown, typeW));
            }
        }
    }

    private void updateSpeedButtons() {
        Layout l = buildLayout();
        int formW = l.panelW - 2 * PAD;
        int speedW = (formW - 4 * GAP) / 5;
        for (int i = 0; i < 5; i++) {
            if (speedButtons[i] != null) {
                boolean sel = REVEAL_SPEEDS[i] == draft.revealSpeed;
                String shown = getSegmentedLabel(SPEED_LABELS[i], sel, speedW);
                speedButtons[i].setMessage(fitLabel(shown, speedW));
            }
        }
    }

    public void markDirtyImmediate() {
        previewDirty = true;
        lastEditMs = 0;
    }

    private void markDirtyDebounced() {
        previewDirty = true;
        lastEditMs = System.currentTimeMillis();
    }

    private void syncEditboxesToDraft() {
        if (textEdit != null) draft.text = textEdit.getValue().trim();
        if (colorEdit != null) draft.color = colorEdit.getValue().trim();
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
        statusMsg = msg;
        statusTimer = 100;
    }

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

        if (editBoxChanged) {
            markDirtyDebounced();
        }

        // Auto-preview logic
        if (previewDirty && (lastEditMs == 0 || System.currentTimeMillis() - lastEditMs > 300)) {
            syncEditboxesToDraft();
            Layout l = buildLayout();
            int pvW = l.panelW - 2 * PAD;
            previewRenderer.play(draft, pvW);
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

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        Layout l = buildLayout();
        int fx = l.px + PAD;

        // ---- Panel background ----
        g.fill(l.px - 1, l.py - 1, l.px + l.panelW + 1, l.py + l.panelH + 1, 0xFF070710);
        g.fill(l.px, l.py, l.px + l.panelW, l.py + l.panelH, 0xFF12121E);

        // ---- Header bar ----
        g.fill(l.px, l.py, l.px + l.panelW, l.py + HDR_H, 0xFF0C0C1A);
        g.drawCenteredString(this.font, "§bTitleFX §7Visual Editor", l.px + l.panelW / 2, l.py + 6, 0xFFFFFF);

        // ---- Preview zone ----
        int pvX = l.px + PAD;
        int pvW = l.panelW - 2 * PAD;
        int pvY = l.py + HDR_H + 4;
        int pvH = l.previewH;
        g.fill(pvX, pvY, pvX + pvW, pvY + pvH, 0xFF0A0A18);
        g.fill(pvX + 1, pvY + 1, pvX + pvW - 1, pvY + pvH - 1, 0xFF0D0D22);
        
        // Render preview inside panel
        previewRenderer.render(g, pvX, pvY, pvW, pvH);

        // ---- Separator ----
        int previewActionAreaH = l.widH + 8;
        int sepY = l.py + HDR_H + 4 + l.previewH + 4 + previewActionAreaH + 2;
        g.fill(l.px, sepY, l.px + l.panelW, sepY + 1, 0xFF1E1E30);
        g.fill(l.px, sepY + 1, l.px + l.panelW, sepY + 2, 0xFF090912);

        // ---- Form labels ----
        int currentLabelRow = 0;
        if (textEdit != null) {
            g.drawString(this.font, "§8Texto:", fx, l.lY(currentLabelRow), 0x7070A0);
            currentLabelRow++;
        }

        if (typeButtons[0] != null) {
            g.drawString(this.font, "§8Tipo:", fx, l.lY(currentLabelRow), 0x7070A0);
            currentLabelRow++;
        }

        if (speedButtons[0] != null) {
            g.drawString(this.font, "§8Velocidade:", fx, l.lY(currentLabelRow), 0x7070A0);
            currentLabelRow++;
        }

        if (colorEdit != null) {
            g.drawString(this.font, "§8Cor:", fx, l.lY(currentLabelRow), 0x7070A0);
            currentLabelRow++;
        }

        // ---- Status message ----
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
}
