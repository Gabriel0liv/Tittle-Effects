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

    // Status & confirmation state
    private String statusMsg = "";
    private int statusTimer = 0;
    private boolean confirmSendAll = false;
    private int confirmTimer = 0;
    private Button sendAllBtn;

    public AdvancedEditorScreen(TextEditorScreen parent) {
        super(Component.literal("Configurações Avançadas"));
        this.parent = parent;
        this.draft = EditorDraftState.getInstance();
    }

    @Override
    protected void init() {
        this.clearWidgets();
        super.init();

        confirmSendAll = false;
        confirmTimer = 0;

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
        int fBtnW = (listW - 2 * 8) / 3;

        String backLabel = "Voltar";
        Button backBtn = Button.builder(Component.literal(TitleFxEditorList.fitLabel(backLabel, fBtnW)), btn -> {
            parent.onDraftChanged();
            Minecraft.getInstance().setScreen(parent);
        }).bounds(listX, footerCenterY, fBtnW, 20).build();
        this.addRenderableWidget(backBtn);

        String applyLabel = "Aplicar";
        Button applyBtn = Button.builder(Component.literal(TitleFxEditorList.fitLabel(applyLabel, fBtnW)), btn -> {
            sendEditedText(false);
        }).bounds(listX + fBtnW + 8, footerCenterY, fBtnW, 20).build();
        this.addRenderableWidget(applyBtn);

        String sendAllLabel = fBtnW < 75 ? "Todos" : "Enviar (Todos)";
        sendAllBtn = Button.builder(Component.literal(TitleFxEditorList.fitLabel(sendAllLabel, listW - 2 * fBtnW - 2 * 8)), btn -> {
            if (!confirmSendAll) {
                confirmSendAll = true;
                confirmTimer = 60; // 3 seconds
                btn.setMessage(Component.literal(TitleFxEditorList.fitLabel("Confirmar?", listW - 2 * fBtnW - 2 * 8)));
            } else {
                sendEditedText(true);
                confirmSendAll = false;
                confirmTimer = 0;
                btn.setMessage(Component.literal(TitleFxEditorList.fitLabel(sendAllLabel, listW - 2 * fBtnW - 2 * 8)));
            }
        }).bounds(listX + 2 * (fBtnW + 8), footerCenterY, listW - 2 * fBtnW - 2 * 8, 20).build();
        this.addRenderableWidget(sendAllBtn);
    }

    private void onDraftChanged() {
        if (list != null) {
            list.syncAllEntries(draft);
        }
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
                    String sendAllLabel = (sendAllBtn.getWidth() < 75) ? "Todos" : "Enviar (Todos)";
                    sendAllBtn.setMessage(Component.literal(TitleFxEditorList.fitLabel(sendAllLabel, sendAllBtn.getWidth())));
                }
            }
        }

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
        // Transparent modern dark overlay
        g.fill(0, 0, this.width, this.height, 0xCC05050A);

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
        g.fill(listX, listY, listX + listW, listY + listH, 0xAA080812);
        g.fill(listX, listY, listX + listW, listY + 1, 0x44FFFFFF);

        // Footer Background
        g.fill(0, footerY, this.width, this.height, 0xEE0C0C1A);

        // Status message
        if (statusTimer > 0 && !statusMsg.isEmpty()) {
            float alpha = Math.min(1.0f, statusTimer / 20.0f);
            int color = ((int) (alpha * 0xFF) << 24) | 0x55FF55;
            g.drawCenteredString(this.font, statusMsg, this.width / 2, footerY - 10, color);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }
}
