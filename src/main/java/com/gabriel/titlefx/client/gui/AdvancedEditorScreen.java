package com.gabriel.titlefx.client.gui;

import com.gabriel.titlefx.common.animation.*;
import com.gabriel.titlefx.common.model.AnimatedTextPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AdvancedEditorScreen extends Screen {

    private final TextEditorScreen parent;
    private final EditorDraftState draft;
    private TitleFxEditorList list;

    // Status state
    private String statusMsg = "";
    private int statusTimer = 0;

    public AdvancedEditorScreen(TextEditorScreen parent) {
        super(Component.literal("Configurações Avançadas"));
        this.parent = parent;
        this.draft = EditorDraftState.getInstance();
    }

    @Override
    protected void init() {
        this.clearWidgets();
        super.init();



        int margin = 12;
        int headerH = 24;
        int footerH = 28;

        int listX = margin;
        int listY = headerH + margin;
        int listW = this.width - margin * 2;
        int listH = this.height - listY - footerH - 8;

        int footerY = this.height - footerH;
        int footerCenterY = footerY + (footerH - 20) / 2;

        // Scrollable list
        int itemHeight = 44;
        list = new TitleFxEditorList(this.minecraft, listW, listH, listY, listY + listH, itemHeight);
        list.setListBounds(listX, listW);
        list.rebuildAdvancedEntries(draft, this::onDraftChanged);
        this.addRenderableWidget(list);

        // Footer buttons
        int fBtnW = (listW - 3 * 8) / 4;

        String backLabel = "Voltar";
        Button backBtn = Button.builder(Component.literal(TitleFxEditorList.fitLabel(backLabel, fBtnW)), btn -> {
            parent.onDraftChanged();
            Minecraft.getInstance().setScreen(parent);
        }).bounds(listX, footerCenterY, fBtnW, 20).build();
        backBtn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Volta para o editor simples.")));
        this.addRenderableWidget(backBtn);

        String copySLabel = "Copiar para mim";
        Button copySBtn = Button.builder(Component.literal(TitleFxEditorList.fitLabel(copySLabel, fBtnW)), btn -> {
            Minecraft.getInstance().keyboardHandler.setClipboard(draft.toCommand("@s", true));
            setStatus("Comando avançado @s copiado.");
        }).bounds(listX + fBtnW + 8, footerCenterY, fBtnW, 20).build();
        copySBtn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Copia o comando show completo (com opções avançadas) com alvo @s.")));
        this.addRenderableWidget(copySBtn);

        String copyALabel = "Copiar para todos";
        Button copyABtn = Button.builder(Component.literal(TitleFxEditorList.fitLabel(copyALabel, fBtnW)), btn -> {
            Minecraft.getInstance().keyboardHandler.setClipboard(draft.toCommand("@a", true));
            setStatus("Comando avançado @a copiado.");
        }).bounds(listX + 2 * (fBtnW + 8), footerCenterY, fBtnW, 20).build();
        copyABtn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Copia o comando show completo (com opções avançadas) com alvo @a.")));
        this.addRenderableWidget(copyABtn);

        String resetLabel = "Resetar";
        int lastBtnW = listW - 3 * fBtnW - 3 * 8;
        Button resetBtn = Button.builder(Component.literal(TitleFxEditorList.fitLabel(resetLabel, lastBtnW)), btn -> {
            draft.reset();
            onDraftChanged();
            setStatus("Configurações redefinidas.");
        }).bounds(listX + 3 * (fBtnW + 8), footerCenterY, lastBtnW, 20).build();
        resetBtn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Redefine todas as opções avançadas e cores para o padrão.")));
        this.addRenderableWidget(resetBtn);
    }

    private void onDraftChanged() {
        if (list != null) {
            list.syncAllEntries(draft);
        }
    }

    private void setStatus(String msg) {
        statusMsg = msg;
        statusTimer = 80;
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

        int margin = 12;
        int headerH = 24;
        int footerH = 28;
        
        int listX = margin;
        int listY = headerH + margin;
        int listW = this.width - margin * 2;
        int listH = this.height - listY - footerH - 8;
        int footerY = this.height - footerH;

        // Header Background
        g.fill(0, 0, this.width, listY - 4, 0xEE0C0C1A);
        g.drawCenteredString(this.font, "§bTitleFX §7Configurações Avançadas", this.width / 2, 6, 0xFFFFFF);

        // Scrollable List Panel Background
        g.fill(listX, listY, listX + listW, listY + listH, 0xAA070711);
        g.fill(listX, listY, listX + listW, listY + 1, 0x44FFFFFF);

        // Footer Background
        g.fill(0, footerY, this.width, this.height, 0xEE0C0C1A);

        // Dica / Status message
        if (statusTimer > 0 && !statusMsg.isEmpty()) {
            float alpha = Math.min(1.0f, statusTimer / 20.0f);
            int color = ((int) (alpha * 0xFF) << 24) | 0x55FF55;
            g.drawCenteredString(this.font, statusMsg, this.width / 2, footerY - 12, color);
        } else {
            g.drawCenteredString(this.font, "§eDica: §7Ajustes avançados geram comandos mais detalhados.", this.width / 2, footerY - 12, 0x80FFFFFF);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }
}
