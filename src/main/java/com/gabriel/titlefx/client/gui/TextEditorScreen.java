package com.gabriel.titlefx.client.gui;

import com.gabriel.titlefx.common.animation.*;
import com.gabriel.titlefx.common.animation.RevealSpeed;
import com.gabriel.titlefx.common.model.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TextEditorScreen extends Screen {

    // Inputs Left Column
    private EditBox textEdit;
    private EditBox colorEdit;
    private EditBox scaleEdit;
    private Button typeButton;
    private EditBox xOffsetEdit;
    private EditBox yOffsetEdit;
    private Button alignButton;
    private EditBox durationEdit;

    // Inputs Right Column
    private Button revealTypeButton;
    private Button revealSpeedButton;   // replaces revealDurationEdit in simple mode
    private Button inAnimButton;
    private Button inEasingButton;
    private EditBox inDurationEdit;
    private Button idleAnimButton;
    private EditBox idleIntensityEdit;
    private Button outAnimButton;
    private Button outEasingButton;
    private EditBox outDurationEdit;

    private final String[] types = {"title", "subtitle", "actionbar", "custom"};
    private int typeIndex = 0;

    private final String[] alignments = {"center", "left", "right"};
    private int alignIndex = 0;

    private final String[] revealTypes = {"NONE", "TYPEWRITER", "WORD_BY_WORD", "GLYPH_SCRAMBLE", "OBFUSCATED_DECODE", "CENTER_OUT", "WIPE_LEFT_TO_RIGHT", "FADE_CHARS", "RANDOM_FADE"};
    private int revealTypeIndex = 0;

    // RevealSpeed cycles: INSTANT, FAST, NORMAL, CINEMATIC, SLOW (CUSTOM not in simple mode)
    private static final RevealSpeed[] REVEAL_SPEEDS = {
        RevealSpeed.INSTANT, RevealSpeed.FAST, RevealSpeed.NORMAL, RevealSpeed.CINEMATIC, RevealSpeed.SLOW
    };
    private int revealSpeedIndex = 2; // default = NORMAL

    private final String[] inAnimTypes = {"NONE", "FADE_IN", "CINEMATIC_ZOOM_IN", "SOFT_POP", "SCALE_REVEAL"};
    private int inAnimIndex = 0;

    private final String[] outAnimTypes = {"NONE", "FADE_OUT", "DISSOLVE", "SHRINK_FADE"};
    private int outAnimIndex = 0;

    private final String[] easings = {"LINEAR", "EASE_IN", "EASE_OUT", "EASE_IN_OUT", "BOUNCE", "ELASTIC"};
    private int inEasingIndex = 0;
    private int outEasingIndex = 0;

    private final String[] idleAnimTypes = {"NONE", "SUBTLE_PULSE", "BREATHING", "SUBTLE_SHAKE", "WAVE_SOFT", "FLICKER"};
    private int idleAnimIndex = 0;

    // Status message state
    private String statusMessage = "";
    private int statusTimer = 0;

    public TextEditorScreen() {
        super(Component.literal("TitleFX Editor"));
    }

    @Override
    protected void init() {
        super.init();

        int panelWidth = 420;
        int panelHeight = 220;
        int panelStartX = (this.width - panelWidth) / 2;
        int panelStartY = (this.height - panelHeight) / 2 - 10;

        // --- LEFT COLUMN ---
        int lx = panelStartX;

        // Row 0: Text Input
        textEdit = new EditBox(this.font, lx, panelStartY + 15, 200, 16, Component.literal(""));
        textEdit.setValue("Olá TitleFX!");
        this.addRenderableWidget(textEdit);

        // Row 1: Color and Scale (shifted up from row 2)
        colorEdit = new EditBox(this.font, lx, panelStartY + 43, 95, 16, Component.literal(""));
        colorEdit.setValue("#FFFFFF");
        this.addRenderableWidget(colorEdit);

        scaleEdit = new EditBox(this.font, lx + 105, panelStartY + 43, 95, 16, Component.literal(""));
        scaleEdit.setValue("1.0");
        this.addRenderableWidget(scaleEdit);

        // Row 2: Offsets X and Y (shifted up from row 3)
        xOffsetEdit = new EditBox(this.font, lx, panelStartY + 71, 95, 16, Component.literal(""));
        xOffsetEdit.setValue("0");
        this.addRenderableWidget(xOffsetEdit);

        yOffsetEdit = new EditBox(this.font, lx + 105, panelStartY + 71, 95, 16, Component.literal(""));
        yOffsetEdit.setValue("0");
        this.addRenderableWidget(yOffsetEdit);

        // Row 3: Layer Type and Alignment (shifted up from row 4)
        typeButton = Button.builder(Component.literal("Tipo: " + types[typeIndex]), btn -> {
            typeIndex = (typeIndex + 1) % types.length;
            btn.setMessage(Component.literal("Tipo: " + types[typeIndex]));
        }).bounds(lx, panelStartY + 99, 95, 16).build();
        this.addRenderableWidget(typeButton);

        alignButton = Button.builder(Component.literal("Alinh.: " + alignments[alignIndex]), btn -> {
            alignIndex = (alignIndex + 1) % alignments.length;
            btn.setMessage(Component.literal("Alinh.: " + alignments[alignIndex]));
        }).bounds(lx + 105, panelStartY + 99, 95, 16).build();
        this.addRenderableWidget(alignButton);

        // Row 4: Global Duration (shifted up from row 5)
        durationEdit = new EditBox(this.font, lx, panelStartY + 127, 200, 16, Component.literal(""));
        durationEdit.setValue("3000");
        this.addRenderableWidget(durationEdit);


        // --- RIGHT COLUMN ---
        int rx = panelStartX + 220;

        // Row 0: Reveal Type & Reveal Duration
        revealTypeButton = Button.builder(Component.literal("Rev: " + revealTypes[revealTypeIndex]), btn -> {
            revealTypeIndex = (revealTypeIndex + 1) % revealTypes.length;
            btn.setMessage(Component.literal("Rev: " + revealTypes[revealTypeIndex]));
        }).bounds(rx, panelStartY + 15, 95, 16).build();
        this.addRenderableWidget(revealTypeButton);

        revealSpeedButton = Button.builder(Component.literal("Vel: " + REVEAL_SPEEDS[revealSpeedIndex].getLabel()), btn -> {
            revealSpeedIndex = (revealSpeedIndex + 1) % REVEAL_SPEEDS.length;
            btn.setMessage(Component.literal("Vel: " + REVEAL_SPEEDS[revealSpeedIndex].getLabel()));
        }).bounds(rx + 105, panelStartY + 15, 95, 16).build();
        this.addRenderableWidget(revealSpeedButton);

        // Row 1: In Animation and Easing
        inAnimButton = Button.builder(Component.literal("Entrada: " + inAnimTypes[inAnimIndex]), btn -> {
            inAnimIndex = (inAnimIndex + 1) % inAnimTypes.length;
            btn.setMessage(Component.literal("Entrada: " + inAnimTypes[inAnimIndex]));
        }).bounds(rx, panelStartY + 43, 95, 16).build();
        this.addRenderableWidget(inAnimButton);

        inEasingButton = Button.builder(Component.literal("Ease: " + easings[inEasingIndex]), btn -> {
            inEasingIndex = (inEasingIndex + 1) % easings.length;
            btn.setMessage(Component.literal("Ease: " + easings[inEasingIndex]));
        }).bounds(rx + 105, panelStartY + 43, 95, 16).build();
        this.addRenderableWidget(inEasingButton);

        // Row 2: In Duration
        inDurationEdit = new EditBox(this.font, rx, panelStartY + 71, 200, 16, Component.literal(""));
        inDurationEdit.setValue("500");
        this.addRenderableWidget(inDurationEdit);

        // Row 3: Idle Animation and Intensity
        idleAnimButton = Button.builder(Component.literal("Ocioso: " + idleAnimTypes[idleAnimIndex]), btn -> {
            idleAnimIndex = (idleAnimIndex + 1) % idleAnimTypes.length;
            btn.setMessage(Component.literal("Ocioso: " + idleAnimTypes[idleAnimIndex]));
        }).bounds(rx, panelStartY + 99, 95, 16).build();
        this.addRenderableWidget(idleAnimButton);

        idleIntensityEdit = new EditBox(this.font, rx + 105, panelStartY + 99, 95, 16, Component.literal(""));
        idleIntensityEdit.setValue("1.0");
        this.addRenderableWidget(idleIntensityEdit);

        // Row 4: Out Animation and Easing
        outAnimButton = Button.builder(Component.literal("Saída: " + outAnimTypes[outAnimIndex]), btn -> {
            outAnimIndex = (outAnimIndex + 1) % outAnimTypes.length;
            btn.setMessage(Component.literal("Saída: " + outAnimTypes[outAnimIndex]));
        }).bounds(rx, panelStartY + 127, 95, 16).build();
        this.addRenderableWidget(outAnimButton);

        outEasingButton = Button.builder(Component.literal("Ease: " + easings[outEasingIndex]), btn -> {
            outEasingIndex = (outEasingIndex + 1) % easings.length;
            btn.setMessage(Component.literal("Ease: " + easings[outEasingIndex]));
        }).bounds(rx + 105, panelStartY + 127, 95, 16).build();
        this.addRenderableWidget(outEasingButton);

        // Row 5: Out Duration
        outDurationEdit = new EditBox(this.font, rx, panelStartY + 155, 200, 16, Component.literal(""));
        outDurationEdit.setValue("500");
        this.addRenderableWidget(outDurationEdit);


        // --- BOTTOM ACTION ROW ---
        int by = panelStartY + 190;

        Button previewBtn = Button.builder(Component.literal("Preview Local"), btn -> previewLocally())
                .bounds(panelStartX, by, 100, 20).build();
        this.addRenderableWidget(previewBtn);

        Button copyBtn = Button.builder(Component.literal("Copiar Comando"), btn -> copyCommand())
                .bounds(panelStartX + 106, by, 100, 20).build();
        this.addRenderableWidget(copyBtn);

        Button sendSelfBtn = Button.builder(Component.literal("Enviar (Self)"), btn -> sendEditedText(false))
                .bounds(panelStartX + 212, by, 100, 20).build();
        this.addRenderableWidget(sendSelfBtn);

        Button sendAllBtn = Button.builder(Component.literal("Enviar (Todos)"), btn -> sendEditedText(true))
                .bounds(panelStartX + 318, by, 102, 20).build();
        this.addRenderableWidget(sendAllBtn);
    }

    private AnimatedTextPayload buildPayload() {
        try {
            String text = textEdit.getValue().trim();
            String fontName = "minecraft:default";
            String color = colorEdit.getValue().trim();
            if (color.isEmpty()) color = "#FFFFFF";

            float scale = 1.0f;
            try { scale = Float.parseFloat(scaleEdit.getValue().trim()); } catch (Exception ignored) {}

            int xOffset = 0;
            try { xOffset = Integer.parseInt(xOffsetEdit.getValue().trim()); } catch (Exception ignored) {}

            int yOffset = 0;
            try { yOffset = Integer.parseInt(yOffsetEdit.getValue().trim()); } catch (Exception ignored) {}

            String activeType = types[typeIndex];
            String activeAlign = alignments[alignIndex];

            int duration = 3000;
            try { duration = Integer.parseInt(durationEdit.getValue().trim()); } catch (Exception ignored) {}

            // Reveal
            RevealType revType = RevealType.fromString(revealTypes[revealTypeIndex]);
            RevealSpeed revSpeed = REVEAL_SPEEDS[revealSpeedIndex];

            // In
            InAnimationType inAnimType = InAnimationType.fromString(inAnimTypes[inAnimIndex]);
            int inDur = 500;
            try { inDur = Integer.parseInt(inDurationEdit.getValue().trim()); } catch (Exception ignored) {}
            Easing inE = Easing.fromString(easings[inEasingIndex]);

            // Idle
            IdleAnimationType idleAnimType = IdleAnimationType.fromString(idleAnimTypes[idleAnimIndex]);
            float idleInt = 1.0f;
            try { idleInt = Float.parseFloat(idleIntensityEdit.getValue().trim()); } catch (Exception ignored) {}

            // Out
            OutAnimationType outAnimType = OutAnimationType.fromString(outAnimTypes[outAnimIndex]);
            int outDur = 500;
            try { outDur = Integer.parseInt(outDurationEdit.getValue().trim()); } catch (Exception ignored) {}
            Easing outE = Easing.fromString(easings[outEasingIndex]);

            // Build RevealPayload, InAnimPayload, IdleAnimPayload, OutAnimPayload, PositionPayload, TextLayerPayload
            RevealPayload reveal = new RevealPayload(
                revType,
                revSpeed,
                0,          // durationMs = 0 because speed drives the calculation
                LockMode.LEFT_TO_RIGHT,
                2,
                "safe",
                true,
                true
            );

            InAnimPayload inPayload = new InAnimPayload(inAnimType, inDur, inE);
            IdleAnimPayload idlePayload = new IdleAnimPayload(idleAnimType, idleInt);
            OutAnimPayload outPayload = new OutAnimPayload(outAnimType, outDur, outE);
            PositionPayload posPayload = new PositionPayload(
                activeType, xOffset, yOffset, activeAlign
            );

            TextLayerPayload layer = new TextLayerPayload(
                activeType,
                text,
                fontName,
                color,
                null,
                scale,
                posPayload,
                reveal,
                inPayload,
                idlePayload,
                outPayload,
                duration
            );

            return new AnimatedTextPayload(
                java.util.UUID.randomUUID().toString(),
                java.util.Collections.singletonList(layer),
                duration
            );
        } catch (Exception e) {
            return null;
        }
    }

    private void previewLocally() {
        AnimatedTextPayload payload = buildPayload();
        if (payload != null) {
            com.gabriel.titlefx.client.render.AnimatedTextManager.getInstance().showText(payload);
        }
    }

    private void copyCommand() {
        try {
            String text = textEdit.getValue().trim();
            String color = colorEdit.getValue().trim();
            String scaleStr = scaleEdit.getValue().trim();
            String xStr = xOffsetEdit.getValue().trim();
            String yStr = yOffsetEdit.getValue().trim();
            String activeType = types[typeIndex];
            String activeAlign = alignments[alignIndex];
            String durationStr = durationEdit.getValue().trim();

            String revType = revealTypes[revealTypeIndex].toLowerCase();
            RevealSpeed revSpeed = REVEAL_SPEEDS[revealSpeedIndex];

            String inAnimType = inAnimTypes[inAnimIndex].toLowerCase();
            String inDurStr = inDurationEdit.getValue().trim();
            String inE = easings[inEasingIndex].toLowerCase();

            String idleAnimType = idleAnimTypes[idleAnimIndex].toLowerCase();
            String idleIntStr = idleIntensityEdit.getValue().trim();

            String outAnimType = outAnimTypes[outAnimIndex].toLowerCase();
            String outDurStr = outDurationEdit.getValue().trim();
            String outE = easings[outEasingIndex].toLowerCase();

            StringBuilder cmd = new StringBuilder("/ctitle show @a ");
            cmd.append(activeType).append(" ");
            if (!color.isEmpty()) cmd.append("color:").append(color).append(" ");
            cmd.append("scale:").append(scaleStr).append(" ");
            cmd.append("x:").append(xStr).append(" ");
            cmd.append("y:").append(yStr).append(" ");
            cmd.append("align:").append(activeAlign).append(" ");

            if (!"none".equals(revType)) {
                cmd.append("reveal:").append(revType).append(" ");
                // Emit reveal_speed unless it's CUSTOM, in which case emit reveal_duration
                if (revSpeed == RevealSpeed.CUSTOM) {
                    cmd.append("reveal_duration:").append(0).append(" ");
                } else {
                    cmd.append("reveal_speed:").append(revSpeed.name().toLowerCase()).append(" ");
                }
            }
            if (!"none".equals(inAnimType)) {
                cmd.append("in:").append(inAnimType).append(" ");
                cmd.append("in_duration:").append(inDurStr).append(" ");
                cmd.append("in_easing:").append(inE).append(" ");
            }
            if (!"none".equals(idleAnimType)) {
                cmd.append("idle:").append(idleAnimType).append(" ");
                cmd.append("idle_intensity:").append(idleIntStr).append(" ");
            }
            if (!"none".equals(outAnimType)) {
                cmd.append("out:").append(outAnimType).append(" ");
                cmd.append("out_duration:").append(outDurStr).append(" ");
                cmd.append("out_easing:").append(outE).append(" ");
            }
            cmd.append("duration:").append(durationStr).append(" ");
            cmd.append("\"").append(text).append("\"");

            net.minecraft.client.Minecraft.getInstance().keyboardHandler.setClipboard(cmd.toString());
            this.statusMessage = "Comando copiado para a área de transferência!";
            this.statusTimer = 60;
        } catch (Exception e) {
            this.statusMessage = "Erro ao copiar comando.";
            this.statusTimer = 60;
        }
    }

    private void sendEditedText(boolean targetAll) {
        AnimatedTextPayload payload = buildPayload();
        if (payload != null) {
            com.gabriel.titlefx.common.network.NetworkHandler.CHANNEL.sendToServer(
                new com.gabriel.titlefx.common.network.SendEditedTextPacket(payload, targetAll)
            );
            this.statusMessage = targetAll ? "Título enviado para todos!" : "Título enviado para si mesmo!";
            this.statusTimer = 60;
        } else {
            this.statusMessage = "Erro ao construir dados do título.";
            this.statusTimer = 60;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (statusTimer > 0) {
            statusTimer--;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Draw background
        this.renderBackground(graphics);

        int panelWidth = 420;
        int panelHeight = 220;
        int panelStartX = (this.width - panelWidth) / 2;
        int panelStartY = (this.height - panelHeight) / 2 - 10;

        // Draw outer dark frame panel
        graphics.fill(panelStartX - 10, panelStartY - 10, panelStartX + panelWidth + 10, panelStartY + panelHeight + 10, 0xD0101010);
        graphics.fill(panelStartX - 8, panelStartY - 8, panelStartX + panelWidth + 8, panelStartY + panelHeight + 8, 0xE0202020);

        // Header Title
        graphics.drawCenteredString(this.font, "TitleFX Visual Editor", this.width / 2, panelStartY - 2, 0xFFAA00);

        // Draw Labels - Left Column
        int lx = panelStartX;
        graphics.drawString(this.font, "Texto", lx, panelStartY + 6, 0xA0A0A0);
        graphics.drawString(this.font, "Cor (HEX)", lx, panelStartY + 34, 0xA0A0A0);
        graphics.drawString(this.font, "Escala", lx + 105, panelStartY + 34, 0xA0A0A0);
        graphics.drawString(this.font, "Desloc. X", lx, panelStartY + 62, 0xA0A0A0);
        graphics.drawString(this.font, "Desloc. Y", lx + 105, panelStartY + 62, 0xA0A0A0);
        graphics.drawString(this.font, "Tipo Camada", lx, panelStartY + 90, 0xA0A0A0);
        graphics.drawString(this.font, "Alinhamento", lx + 105, panelStartY + 90, 0xA0A0A0);
        graphics.drawString(this.font, "Duração (ms)", lx, panelStartY + 118, 0xA0A0A0);

        // Draw Labels - Right Column
        int rx = panelStartX + 220;
        graphics.drawString(this.font, "Revelação", rx, panelStartY + 6, 0xA0A0A0);
        graphics.drawString(this.font, "Vel. Revelação", rx + 105, panelStartY + 6, 0xA0A0A0);
        graphics.drawString(this.font, "Anim. Entrada", rx, panelStartY + 34, 0xA0A0A0);
        graphics.drawString(this.font, "Suavização", rx + 105, panelStartY + 34, 0xA0A0A0);
        graphics.drawString(this.font, "Duração Entr.", rx, panelStartY + 62, 0xA0A0A0);
        graphics.drawString(this.font, "Anim. Ociosa", rx, panelStartY + 90, 0xA0A0A0);
        graphics.drawString(this.font, "Intensidade", rx + 105, panelStartY + 90, 0xA0A0A0);
        graphics.drawString(this.font, "Anim. Saída", rx, panelStartY + 118, 0xA0A0A0);
        graphics.drawString(this.font, "Suavização", rx + 105, panelStartY + 118, 0xA0A0A0);
        graphics.drawString(this.font, "Duração Saída", rx, panelStartY + 146, 0xA0A0A0);

        // Draw Status Message if active
        if (statusTimer > 0 && !statusMessage.isEmpty()) {
            graphics.drawCenteredString(this.font, statusMessage, this.width / 2, panelStartY + panelHeight + 4, 0x55FF55);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
