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
        int btnW = (l.previewW() - 8) / 2;
        Button reproBtn = Button.builder(Component.literal("Prévia local"), btn -> {
            previewRenderer.play(draft, l.previewW());
            setStatus("Prévia aproximada carregada.");
        }).bounds(l.previewX(), l.previewButtonsY(), btnW, 20).build();
        reproBtn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Mostra apenas uma prévia aproximada dentro desta tela. Não envia para jogadores.")));
        this.addRenderableWidget(reproBtn);

        Button stopBtn = Button.builder(Component.literal("Parar prévia"), btn -> {
            previewRenderer.stop();
            setStatus("Preview parado.");
        }).bounds(l.previewX() + btnW + 8, l.previewButtonsY(), btnW, 20).build();
        stopBtn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Para a reprodução da visualização local.")));
        this.addRenderableWidget(stopBtn);

        // Scrollable option list
        int itemHeight = 42;
        list = new TitleFxEditorList(this.minecraft, l.listW(), l.listH(), l.listY(), l.listY() + l.listH(), itemHeight);
        list.setListBounds(l.listX(), l.listW());
        list.rebuildMainEntries(draft, l.compact(), l.listW(), this::onDraftChanged);
        this.addRenderableWidget(list);

        // Footer buttons
        int fBtnW = (l.previewW() - 3 * 8) / 4;
        int footerCenterY = l.footerY() + (l.footerH() - 20) / 2;

        String copySLabel = "Copiar para mim";
        Button copySBtn = Button.builder(Component.literal(TitleFxEditorList.fitLabel(copySLabel, fBtnW)), btn -> {
            Minecraft.getInstance().keyboardHandler.setClipboard(draft.toCommand("@s", false));
            setStatus("Comando copiado. Cole no chat para testar.");
        }).bounds(l.previewX(), footerCenterY, fBtnW, 20).build();
        copySBtn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Copia o comando /ctitle show com alvo @s para colar no chat.")));
        this.addRenderableWidget(copySBtn);

        String copyALabel = "Copiar para todos";
        Button copyABtn = Button.builder(Component.literal(TitleFxEditorList.fitLabel(copyALabel, fBtnW)), btn -> {
            Minecraft.getInstance().keyboardHandler.setClipboard(draft.toCommand("@a", false));
            setStatus("Comando copiado. Use no Command Block.");
        }).bounds(l.previewX() + fBtnW + 8, footerCenterY, fBtnW, 20).build();
        copyABtn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Copia o comando /ctitle show com alvo @a para todos os jogadores.")));
        this.addRenderableWidget(copyABtn);

        String moreLabel = fBtnW < 60 ? "Opções" : "Mais opções";
        Button moreBtn = Button.builder(Component.literal(TitleFxEditorList.fitLabel(moreLabel, fBtnW)), btn -> {
            Minecraft.getInstance().setScreen(new AdvancedEditorScreen(this));
        }).bounds(l.previewX() + 2 * (fBtnW + 8), footerCenterY, fBtnW, 20).build();
        moreBtn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Abre configurações adicionais como escala, timings e easings.")));
        this.addRenderableWidget(moreBtn);

        String closeLabel = "✕ Fechar";
        int lastBtnW = l.previewW() - 3 * fBtnW - 3 * 8;
        Button closeBtn = Button.builder(Component.literal(TitleFxEditorList.fitLabel(closeLabel, lastBtnW)), btn -> this.onClose())
            .bounds(l.previewX() + 3 * (fBtnW + 8), footerCenterY, lastBtnW, 20).build();
        closeBtn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Salva o rascunho e fecha o editor.")));
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
    public void renderBackground(GuiGraphics g) {
        // No-op to override vanilla background
    }

    private void renderEditorBackground(GuiGraphics g) {
        g.fill(0, 0, this.width, this.height, 0xDD05050A);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderEditorBackground(g);

        EditorLayout l = EditorLayout.calculate(this.width, this.height);

        // Header Background
        g.fill(0, 0, this.width, l.previewY() - 4, 0xEE0C0C1A);
        g.drawCenteredString(this.font, "§bTitleFX §7Visual Editor", this.width / 2, l.headerY(), 0xFFFFFF);

        // Preview Background with neon border
        g.fill(l.previewX(), l.previewY(), l.previewX() + l.previewW(), l.previewY() + l.previewH(), 0x3344FFFF);
        g.fill(l.previewX() + 1, l.previewY() + 1, l.previewX() + l.previewW() - 1, l.previewY() + l.previewH() - 1, 0xEE090918);
        
        // Render preview
        previewRenderer.render(g, l.previewX(), l.previewY(), l.previewW(), l.previewH(), partialTick);

        // Scrollable List Panel Background
        g.fill(l.listX(), l.listY(), l.listX() + l.listW(), l.listY() + l.listH(), 0xAA070711);
        g.fill(l.listX(), l.listY(), l.listX() + l.listW(), l.listY() + 1, 0x44FFFFFF);

        // Footer Background
        g.fill(0, l.footerY(), this.width, this.height, 0xEE0C0C1A);

        // Dica / Status message
        if (statusTimer > 0 && !statusMsg.isEmpty()) {
            float alpha = Math.min(1.0f, statusTimer / 20.0f);
            int color = ((int) (alpha * 0xFF) << 24) | 0x55FF55;
            g.drawCenteredString(this.font, statusMsg, this.width / 2, l.footerY() - 12, color);
        } else {
            g.drawCenteredString(this.font, "§eDica: §7Escolha um estilo, escreva o texto e copie o comando.", this.width / 2, l.footerY() - 12, 0x80FFFFFF);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }
}
