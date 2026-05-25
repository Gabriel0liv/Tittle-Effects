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
        list = new TitleFxEditorList(this.minecraft, listW, listH, listY, listY + listH, 24);
        list.setLeftPos(listX);
        list.rebuildAdvancedEntries(draft, this::onDraftChanged);
        this.addRenderableWidget(list);

        // Footer buttons
        int fBtnW = (listW - 2 * 8) / 3;

        Button backBtn = Button.builder(Component.literal("Voltar"), btn -> {
            parent.onDraftChanged();
            Minecraft.getInstance().setScreen(parent);
        }).bounds(listX, footerCenterY, fBtnW, 20).build();
        this.addRenderableWidget(backBtn);

        Button applyBtn = Button.builder(Component.literal("Aplicar"), btn -> {
            sendEditedText(false);
        }).bounds(listX + fBtnW + 8, footerCenterY, fBtnW, 20).build();
        this.addRenderableWidget(applyBtn);

        sendAllBtn = Button.builder(Component.literal("Enviar (Todos)"), btn -> {
            if (!confirmSendAll) {
                confirmSendAll = true;
                confirmTimer = 60; // 3 seconds
                btn.setMessage(Component.literal("Confirmar?"));
            } else {
                sendEditedText(true);
                confirmSendAll = false;
                confirmTimer = 0;
                btn.setMessage(Component.literal("Enviar (Todos)"));
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
                    sendAllBtn.setMessage(Component.literal("Enviar (Todos)"));
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
        this.renderBackground(g);

        int margin = 12;
        int headerH = 24;
        int footerH = 28;
        int footerY = this.height - footerH;

        // Header Background
        g.fill(0, 0, this.width, headerH + margin - 4, 0xFF0C0C1A);
        g.drawCenteredString(this.font, "§bTitleFX §7Configurações Avançadas", this.width / 2, 6, 0xFFFFFF);

        // Footer Background
        g.fill(0, footerY, this.width, this.height, 0xFF0C0C1A);

        // Status message
        if (statusTimer > 0 && !statusMsg.isEmpty()) {
            float alpha = Math.min(1.0f, statusTimer / 20.0f);
            int color = ((int) (alpha * 0xFF) << 24) | 0x55FF55;
            g.drawCenteredString(this.font, statusMsg, this.width / 2, footerY - 10, color);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }
}
