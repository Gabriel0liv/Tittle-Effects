package com.gabriel.titlefx.client.gui;

import com.gabriel.titlefx.common.animation.*;
import com.gabriel.titlefx.common.model.AnimatedTextPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TextEditorScreen extends Screen {

    private EditorDraftState draft;
    private String statusMsg = "";
    private int statusTimer = 0;

    private final EditorPreviewRenderer previewRenderer = new EditorPreviewRenderer();
    private TitleFxEditorList list;

    public TextEditorScreen() {
        super(Component.literal("TitleFX Visual Editor"));
    }

    @Override
    protected void init() {
        this.clearWidgets();
        super.init();

        if (draft == null) {
            draft = EditorDraftState.getInstance();
        }

        EditorLayout l = EditorLayout.calculate(this.width, this.height);

        // Preview buttons
        int btnW = (l.previewW() - 2 * 8) / 3;
        Button reproBtn = Button.builder(Component.literal("▶ Reproduzir"), btn -> {
            previewRenderer.play(draft, l.previewW());
            setStatus("Preview reproduzido!");
        }).bounds(l.previewX(), l.previewButtonsY(), btnW, 20).build();
        this.addRenderableWidget(reproBtn);

        Button stopBtn = Button.builder(Component.literal("■ Parar"), btn -> {
            previewRenderer.stop();
            setStatus("Preview parado.");
        }).bounds(l.previewX() + btnW + 8, l.previewButtonsY(), btnW, 20).build();
        this.addRenderableWidget(stopBtn);

        Button applyBtn = Button.builder(Component.literal("↩ Aplicar"), btn -> sendEditedText(false))
            .bounds(l.previewX() + 2 * (btnW + 8), l.previewButtonsY(), l.previewW() - 2 * btnW - 2 * 8, 20).build();
        this.addRenderableWidget(applyBtn);

        // Scrollable option list
        int itemHeight = l.listW() < 420 ? 36 : 24;
        list = new TitleFxEditorList(this.minecraft, l.listW(), l.listH(), l.listY(), l.listY() + l.listH(), itemHeight);
        list.setLeftPos(l.listX());
        list.rebuildMainEntries(draft, l.compact(), l.listW(), this::onDraftChanged);
        this.addRenderableWidget(list);

        // Footer buttons
        int fBtnW = (l.previewW() - 2 * 8) / 3;
        int footerCenterY = l.footerY() + (l.footerH() - 20) / 2;

        Button copyBtn = Button.builder(Component.literal("Copiar comando"), btn -> {
            Minecraft.getInstance().keyboardHandler.setClipboard(draft.toCommand());
            setStatus("Comando copiado!");
        }).bounds(l.previewX(), footerCenterY, fBtnW, 20).build();
        this.addRenderableWidget(copyBtn);

        Button moreBtn = Button.builder(Component.literal("Mais opções"), btn -> {
            Minecraft.getInstance().setScreen(new AdvancedEditorScreen(this));
        }).bounds(l.previewX() + fBtnW + 8, footerCenterY, fBtnW, 20).build();
        this.addRenderableWidget(moreBtn);

        Button closeBtn = Button.builder(Component.literal("✕ Fechar"), btn -> this.onClose())
            .bounds(l.previewX() + 2 * (fBtnW + 8), footerCenterY, l.previewW() - 2 * fBtnW - 2 * 8, 20).build();
        this.addRenderableWidget(closeBtn);
    }

    public void onDraftChanged() {
        if (list != null) {
            list.syncAllEntries(draft);
        }
        // Auto-preview
        EditorLayout l = EditorLayout.calculate(this.width, this.height);
        previewRenderer.play(draft, l.previewW());
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
        statusTimer = 100;
    }

    @Override
    public void tick() {
        super.tick();
        if (statusTimer > 0) statusTimer--;
        if (list != null) {
            list.tick();
        }
    }

    @Override
    public void onClose() {
        draft.save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        EditorLayout l = EditorLayout.calculate(this.width, this.height);

        // Header Background
        g.fill(0, 0, this.width, l.previewY() - 4, 0xFF0C0C1A);
        g.drawCenteredString(this.font, "§bTitleFX §7Visual Editor", this.width / 2, l.headerY(), 0xFFFFFF);

        // Preview Background
        g.fill(l.previewX(), l.previewY(), l.previewX() + l.previewW(), l.previewY() + l.previewH(), 0xFF0A0A18);
        g.fill(l.previewX() + 1, l.previewY() + 1, l.previewX() + l.previewW() - 1, l.previewY() + l.previewH() - 1, 0xFF0D0D22);
        
        // Render preview
        previewRenderer.render(g, l.previewX(), l.previewY(), l.previewW(), l.previewH(), partialTick);

        // Footer Background
        g.fill(0, l.footerY(), this.width, this.height, 0xFF0C0C1A);

        // Status message
        if (statusTimer > 0 && !statusMsg.isEmpty()) {
            float alpha = Math.min(1.0f, statusTimer / 20.0f);
            int color = ((int) (alpha * 0xFF) << 24) | 0x55FF55;
            g.drawCenteredString(this.font, statusMsg, this.width / 2, l.footerY() - 10, color);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }
}
