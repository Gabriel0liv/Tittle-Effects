package com.gabriel.titlefx.client.gui;

import com.gabriel.titlefx.common.animation.*;
import com.gabriel.titlefx.common.model.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TitleFxEditorList extends ContainerObjectSelectionList<TitleFxEditorList.Entry> {

    private int listLeft;
    private int listWidth;
    private int contentWidth;

    public TitleFxEditorList(Minecraft minecraft, int width, int height, int top, int bottom, int itemHeight) {
        super(minecraft, width, height, top, bottom, itemHeight);
        this.listWidth = width;
        this.contentWidth = Math.max(160, width - 32);
    }

    public void setListBounds(int x, int width) {
        this.listLeft = x;
        this.listWidth = width;
        this.contentWidth = Math.max(160, width - 32);
        this.setLeftPos(x);
    }

    @Override
    public int getRowWidth() {
        return this.contentWidth;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.listLeft + this.listWidth - 8;
    }

    public static String fitLabel(String text, int maxWidth) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.font.width(text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        int suffixW = mc.font.width(suffix);
        if (maxWidth <= suffixW) {
            return "";
        }
        int low = 0;
        int high = text.length();
        int best = 0;
        while (low <= high) {
            int mid = (low + high) / 2;
            if (mid > 0 && mid < text.length() && text.charAt(mid - 1) == '§') {
                mid--;
            }
            String part = text.substring(0, mid) + suffix;
            if (mc.font.width(part) <= maxWidth) {
                best = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return text.substring(0, best) + suffix;
    }

    public void rebuildMainEntries(EditorDraftState draft, boolean compact, int listW, Runnable onChanged) {
        this.clearEntries();
        
        StyleCard[] allCards = StyleCard.values();
        
        if (listW >= 450) {
            // 3 colunas x 2 linhas
            this.addEntry(new StyleRowEntry(minecraft, draft, "§7Estilos Preset", new StyleCard[]{allCards[0], allCards[1], allCards[2]}, onChanged));
            this.addEntry(new StyleRowEntry(minecraft, draft, "", new StyleCard[]{allCards[3], allCards[4], allCards[5]}, onChanged));
        } else if (listW >= 300) {
            // 2 colunas x 3 linhas
            this.addEntry(new StyleRowEntry(minecraft, draft, "§7Estilos Preset", new StyleCard[]{allCards[0], allCards[1]}, onChanged));
            this.addEntry(new StyleRowEntry(minecraft, draft, "", new StyleCard[]{allCards[2], allCards[3]}, onChanged));
            this.addEntry(new StyleRowEntry(minecraft, draft, "", new StyleCard[]{allCards[4], allCards[5]}, onChanged));
        } else {
            // 1 coluna x 6 linhas
            this.addEntry(new StyleRowEntry(minecraft, draft, "§7Estilos Preset", new StyleCard[]{allCards[0]}, onChanged));
            this.addEntry(new StyleRowEntry(minecraft, draft, "", new StyleCard[]{allCards[1]}, onChanged));
            this.addEntry(new StyleRowEntry(minecraft, draft, "", new StyleCard[]{allCards[2]}, onChanged));
            this.addEntry(new StyleRowEntry(minecraft, draft, "", new StyleCard[]{allCards[3]}, onChanged));
            this.addEntry(new StyleRowEntry(minecraft, draft, "", new StyleCard[]{allCards[4]}, onChanged));
            this.addEntry(new StyleRowEntry(minecraft, draft, "", new StyleCard[]{allCards[5]}, onChanged));
        }

        // 2. Text input
        this.addEntry(new TextEntry(minecraft, draft, onChanged));

        // 3. Reveal Speed
        this.addEntry(new RevealSpeedEntry(minecraft, draft, onChanged));

        // 4. Color input
        this.addEntry(new ColorEntry(minecraft, draft, onChanged));
    }

    public void rebuildAdvancedEntries(EditorDraftState draft, Runnable onChanged) {
        this.clearEntries();

        // 1. Tipo (always visible in advanced, showing Custom option)
        this.addEntry(new TypeEntry(minecraft, draft, true, onChanged));

        // 2. Duração
        this.addEntry(new EditBoxEntry(minecraft, "§7Duração Total (ms)", 
            draft.durationMs <= 0 ? "" : String.valueOf(draft.durationMs),
            val -> {
                try {
                    draft.durationMs = val.trim().isEmpty() ? -1 : Integer.parseInt(val.trim());
                    onChanged.run();
                } catch (Exception ignored) {}
            }
        ));

        // 3. Cor
        this.addEntry(new ColorEntry(minecraft, draft, onChanged));

        // 4. Escala
        this.addEntry(new EditBoxEntry(minecraft, "§7Escala",
            draft.scale <= 0 ? "" : String.format(java.util.Locale.US, "%.1f", draft.scale),
            val -> {
                try {
                    draft.scale = val.trim().isEmpty() ? -1f : Float.parseFloat(val.trim());
                    onChanged.run();
                } catch (Exception ignored) {}
            }
        ));

        // 5. Posição X / Y
        this.addEntry(new PositionEntry(minecraft, draft, onChanged));

        // 6. Alinhamento
        this.addEntry(new AlignmentEntry(minecraft, draft, onChanged));

        // 7. Modo Revelação
        RevealType[] revealTypes = {
            RevealType.NONE, RevealType.TYPEWRITER, RevealType.WORD_BY_WORD,
            RevealType.GLYPH_SCRAMBLE, RevealType.OBFUSCATED_DECODE, RevealType.CENTER_OUT,
            RevealType.WIPE_LEFT_TO_RIGHT, RevealType.FADE_CHARS, RevealType.RANDOM_FADE
        };
        this.addEntry(new CycleButtonEntry<>(minecraft, "§7Modo Revelação", revealTypes,
            this::getRevealLabel, () -> draft.revealType, val -> {
                draft.revealType = val;
                onChanged.run();
            }
        ));

        // 8. Entrada Tipo
        InAnimationType[] inTypes = {
            InAnimationType.NONE, InAnimationType.FADE_IN, InAnimationType.CINEMATIC_ZOOM_IN,
            InAnimationType.SOFT_POP, InAnimationType.SCALE_REVEAL
        };
        this.addEntry(new CycleButtonEntry<>(minecraft, "§7Entrada", inTypes,
            AnimationNames::of, () -> draft.inAnimation, val -> {
                draft.inAnimation = val;
                onChanged.run();
            }
        ));

        // 9. Entrada Duração
        this.addEntry(new EditBoxEntry(minecraft, "§7Duração da Entrada (ms)",
            String.valueOf(draft.inDuration),
            val -> {
                try {
                    draft.inDuration = Integer.parseInt(val.trim());
                    onChanged.run();
                } catch (Exception ignored) {}
            }
        ));

        // 10. Entrada Easing
        Easing[] easings = Easing.values();
        this.addEntry(new CycleButtonEntry<>(minecraft, "§7Suavização Entrada", easings,
            Enum::name, () -> draft.inEasing, val -> {
                draft.inEasing = val;
                onChanged.run();
            }
        ));

        // 11. Ocioso Tipo
        IdleAnimationType[] idleTypes = {
            IdleAnimationType.NONE, IdleAnimationType.SUBTLE_PULSE, IdleAnimationType.BREATHING,
            IdleAnimationType.SUBTLE_SHAKE, IdleAnimationType.WAVE_SOFT, IdleAnimationType.FLICKER
        };
        this.addEntry(new CycleButtonEntry<>(minecraft, "§7Animação Ociosa", idleTypes,
            AnimationNames::of, () -> draft.idleAnimation, val -> {
                draft.idleAnimation = val;
                onChanged.run();
            }
        ));

        // 12. Ocioso Intensidade
        this.addEntry(new EditBoxEntry(minecraft, "§7Intensidade Ociosa",
            String.format(java.util.Locale.US, "%.2f", draft.idleIntensity),
            val -> {
                try {
                    draft.idleIntensity = Float.parseFloat(val.trim());
                    onChanged.run();
                } catch (Exception ignored) {}
            }
        ));

        // 13. Saída Tipo
        OutAnimationType[] outTypes = {
            OutAnimationType.NONE, OutAnimationType.FADE_OUT, OutAnimationType.DISSOLVE,
            OutAnimationType.SHRINK_FADE
        };
        this.addEntry(new CycleButtonEntry<>(minecraft, "§7Saída", outTypes,
            AnimationNames::of, () -> draft.outAnimation, val -> {
                draft.outAnimation = val;
                onChanged.run();
            }
        ));

        // 14. Saída Duração
        this.addEntry(new EditBoxEntry(minecraft, "§7Duração da Saída (ms)",
            String.valueOf(draft.outDuration),
            val -> {
                try {
                    draft.outDuration = Integer.parseInt(val.trim());
                    onChanged.run();
                } catch (Exception ignored) {}
            }
        ));

        // 15. Saída Easing
        this.addEntry(new CycleButtonEntry<>(minecraft, "§7Suavização Saída", easings,
            Enum::name, () -> draft.outEasing, val -> {
                draft.outEasing = val;
                onChanged.run();
            }
        ));
    }

    public void syncAllEntries(EditorDraftState draft) {
        for (Entry entry : children()) {
            entry.syncFromDraft(draft);
        }
    }

    public void tick() {
        for (Entry entry : children()) {
            entry.tick();
        }
    }

    @Override
    protected void renderBackground(GuiGraphics g) {
        // no-op: let the Screen's modern dark background show through
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

    public abstract static class Entry extends ContainerObjectSelectionList.Entry<Entry> {
        public void tick() {}
        public void syncFromDraft(EditorDraftState draft) {}

        @Override
        public List<? extends GuiEventListener> children() {
            return Collections.emptyList();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return Collections.emptyList();
        }
    }

    // ==================================================================
    // ENTRIES IMPLEMENTATIONS
    // ==================================================================

    public static class StyleRowEntry extends Entry {
        private final List<Button> buttons = new ArrayList<>();
        private final EditorDraftState draft;
        private final StyleCard[] cards;
        private final String labelText;

        public StyleRowEntry(Minecraft mc, EditorDraftState draft, String labelText, StyleCard[] cards, Runnable onChanged) {
            this.draft = draft;
            this.cards = cards;
            this.labelText = labelText;

            for (StyleCard card : cards) {
                if (card != null) {
                    Button btn = Button.builder(Component.literal(card.getLabel()), b -> {
                        card.apply(draft);
                        draft.selectedStyleCard = card.name();
                        onChanged.run();
                    }).build();
                    btn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(card.getDescription())));
                    buttons.add(btn);
                }
            }
        }

        @Override
        public void render(GuiGraphics g, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            Minecraft mc = Minecraft.getInstance();
            boolean compact = width < 360;

            int labelW = compact ? 0 : Math.min(130, (int) (width * 0.32));
            int ctrlX = compact ? (left + 6) : (left + labelW + 10);
            int ctrlW = compact ? (width - 12) : (width - labelW - 20);

            if (!labelText.isEmpty()) {
                int labelY = compact ? (top + 4) : (top + (height - 9) / 2);
                g.drawString(mc.font, labelText, left + 6, labelY, 0xFFFFFF);
            }

            int count = buttons.size();
            if (count == 0) return;

            int btnW = (ctrlW - (count - 1) * 4) / count;
            int btnY = compact ? (top + 20) : (top + (height - 18) / 2);

            for (int i = 0; i < count; i++) {
                Button btn = buttons.get(i);
                btn.setX(ctrlX + i * (btnW + 4));
                btn.setY(btnY);
                btn.setWidth(btnW);
                btn.setHeight(18);

                StyleCard card = cards[i];
                boolean sel = card.name().equals(draft.selectedStyleCard);
                String cardLabel = getShortCardLabel(card, width < 450);
                String labelText = sel ? "► " + cardLabel : cardLabel;
                btn.setMessage(Component.literal(fitLabel(labelText, btnW)));
                btn.render(g, mouseX, mouseY, partialTick);
            }
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
                }
            }
            return card.getLabel();
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return buttons;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return buttons;
        }
    }

    public static class TextEntry extends Entry {
        private final EditBox editBox;

        public TextEntry(Minecraft mc, EditorDraftState draft, Runnable onChanged) {
            this.editBox = new EditBox(mc.font, 0, 0, 100, 14, Component.literal(""));
            this.editBox.setValue(draft.text != null ? draft.text : "");
            this.editBox.setMaxLength(256);
            this.editBox.setResponder(val -> {
                draft.text = val;
                onChanged.run();
            });
        }

        @Override
        public void tick() {
            this.editBox.tick();
        }

        @Override
        public void render(GuiGraphics g, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            Minecraft mc = Minecraft.getInstance();
            boolean compact = width < 360;

            int labelW = compact ? 0 : Math.min(130, (int) (width * 0.32));
            int ctrlX = compact ? (left + 6) : (left + labelW + 10);
            int ctrlW = compact ? (width - 12) : (width - labelW - 20);

            int labelY = compact ? (top + 4) : (top + (height - 9) / 2);
            g.drawString(mc.font, "§7Texto", left + 6, labelY, 0xFFFFFF);

            int ctrlY = compact ? (top + 20) : (top + (height - 14) / 2);
            this.editBox.setX(ctrlX);
            this.editBox.setY(ctrlY);
            this.editBox.setWidth(ctrlW);
            this.editBox.setHeight(14);
            this.editBox.render(g, mouseX, mouseY, partialTick);
        }

        @Override
        public void syncFromDraft(EditorDraftState draft) {
            if (!this.editBox.getValue().equals(draft.text)) {
                this.editBox.setValue(draft.text != null ? draft.text : "");
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return Collections.singletonList(editBox);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return Collections.singletonList(editBox);
        }
    }

    public static class TypeEntry extends Entry {
        private final List<Button> buttons = new ArrayList<>();
        private final EditorDraftState draft;

        private static final String[] TYPES = { "title", "subtitle", "actionbar", "custom" };
        private static final String[] TYPE_LABELS = { "Título", "Sub", "Barra", "Custom" };

        public TypeEntry(Minecraft mc, EditorDraftState draft, boolean showCustom, Runnable onChanged) {
            this.draft = draft;
            int count = showCustom ? 4 : 3;
            for (int i = 0; i < count; i++) {
                final int ti = i;
                Button btn = Button.builder(Component.literal(TYPE_LABELS[i]), b -> {
                    draft.type = TYPES[ti];
                    draft.yOffset = Integer.MIN_VALUE;
                    onChanged.run();
                }).build();
                buttons.add(btn);
            }
        }

        @Override
        public void render(GuiGraphics g, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            Minecraft mc = Minecraft.getInstance();
            boolean compact = width < 360;

            int labelW = compact ? 0 : Math.min(130, (int) (width * 0.32));
            int ctrlX = compact ? (left + 6) : (left + labelW + 10);
            int ctrlW = compact ? (width - 12) : (width - labelW - 20);

            int labelY = compact ? (top + 4) : (top + (height - 9) / 2);
            g.drawString(mc.font, "§7Tipo", left + 6, labelY, 0xFFFFFF);

            int count = buttons.size();
            int btnW = (ctrlW - (count - 1) * 4) / count;
            int btnY = compact ? (top + 20) : (top + (height - 18) / 2);

            for (int i = 0; i < count; i++) {
                Button btn = buttons.get(i);
                btn.setX(ctrlX + i * (btnW + 4));
                btn.setY(btnY);
                btn.setWidth(btnW);
                btn.setHeight(18);

                boolean sel = TYPES[i].equalsIgnoreCase(draft.type);
                String label = TYPE_LABELS[i];
                String labelText = sel ? "► " + label : label;
                btn.setMessage(Component.literal(fitLabel(labelText, btnW)));
                btn.render(g, mouseX, mouseY, partialTick);
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return buttons;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return buttons;
        }
    }

    public static class RevealSpeedEntry extends Entry {
        private final List<Button> buttons = new ArrayList<>();
        private final EditorDraftState draft;

        private static final RevealSpeed[] SPEEDS = {
            RevealSpeed.INSTANT, RevealSpeed.FAST, RevealSpeed.NORMAL, RevealSpeed.CINEMATIC, RevealSpeed.SLOW
        };
        private static final String[] SPEED_LABELS = { "Inst", "Rápido", "Normal", "Cine", "Lento" };

        public RevealSpeedEntry(Minecraft mc, EditorDraftState draft, Runnable onChanged) {
            this.draft = draft;
            for (int i = 0; i < 5; i++) {
                final int si = i;
                Button btn = Button.builder(Component.literal(SPEED_LABELS[i]), b -> {
                    draft.revealSpeed = SPEEDS[si];
                    onChanged.run();
                }).build();
                buttons.add(btn);
            }
        }

        @Override
        public void render(GuiGraphics g, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            Minecraft mc = Minecraft.getInstance();
            boolean compact = width < 360;

            int labelW = compact ? 0 : Math.min(130, (int) (width * 0.32));
            int ctrlX = compact ? (left + 6) : (left + labelW + 10);
            int ctrlW = compact ? (width - 12) : (width - labelW - 20);

            int labelY = compact ? (top + 4) : (top + (height - 9) / 2);
            g.drawString(mc.font, "§7Velocidade", left + 6, labelY, 0xFFFFFF);

            int btnW = (ctrlW - 4 * 4) / 5;
            int btnY = compact ? (top + 20) : (top + (height - 18) / 2);

            for (int i = 0; i < 5; i++) {
                Button btn = buttons.get(i);
                btn.setX(ctrlX + i * (btnW + 4));
                btn.setY(btnY);
                btn.setWidth(btnW);
                btn.setHeight(18);

                boolean sel = SPEEDS[i] == draft.revealSpeed;
                String label = SPEED_LABELS[i];
                String labelText = sel ? "► " + label : label;
                btn.setMessage(Component.literal(fitLabel(labelText, btnW)));
                btn.render(g, mouseX, mouseY, partialTick);
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return buttons;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return buttons;
        }
    }

    public static class ColorEntry extends Entry {
        private final EditBox editBox;

        public ColorEntry(Minecraft mc, EditorDraftState draft, Runnable onChanged) {
            this.editBox = new EditBox(mc.font, 0, 0, 100, 14, Component.literal(""));
            this.editBox.setValue(draft.color != null ? draft.color : "#FFFFFF");
            this.editBox.setMaxLength(32);
            this.editBox.setResponder(val -> {
                draft.color = val;
                onChanged.run();
            });
        }

        @Override
        public void tick() {
            this.editBox.tick();
        }

        @Override
        public void render(GuiGraphics g, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            Minecraft mc = Minecraft.getInstance();
            boolean compact = width < 360;

            int labelW = compact ? 0 : Math.min(130, (int) (width * 0.32));
            int ctrlX = compact ? (left + 6) : (left + labelW + 10);
            int ctrlW = compact ? (width - 12) : (width - labelW - 20);

            int labelY = compact ? (top + 4) : (top + (height - 9) / 2);
            g.drawString(mc.font, "§7Cor (HEX)", left + 6, labelY, 0xFFFFFF);

            int ctrlY = compact ? (top + 20) : (top + (height - 14) / 2);
            this.editBox.setX(ctrlX);
            this.editBox.setY(ctrlY);
            this.editBox.setWidth(ctrlW);
            this.editBox.setHeight(14);
            this.editBox.render(g, mouseX, mouseY, partialTick);
        }

        @Override
        public void syncFromDraft(EditorDraftState draft) {
            if (!this.editBox.getValue().equals(draft.color)) {
                this.editBox.setValue(draft.color != null ? draft.color : "#FFFFFF");
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return Collections.singletonList(editBox);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return Collections.singletonList(editBox);
        }
    }

    public static class EditBoxEntry extends Entry {
        private final String labelText;
        private final EditBox editBox;

        public EditBoxEntry(Minecraft mc, String labelText, String initialVal, java.util.function.Consumer<String> responder) {
            this.labelText = labelText;
            this.editBox = new EditBox(mc.font, 0, 0, 100, 14, Component.literal(""));
            this.editBox.setValue(initialVal);
            this.editBox.setMaxLength(16);
            this.editBox.setResponder(responder);
        }

        @Override
        public void tick() {
            this.editBox.tick();
        }

        @Override
        public void render(GuiGraphics g, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            Minecraft mc = Minecraft.getInstance();
            boolean compact = width < 360;

            int labelW = compact ? 0 : Math.min(130, (int) (width * 0.32));
            int ctrlX = compact ? (left + 6) : (left + labelW + 10);
            int ctrlW = compact ? (width - 12) : (width - labelW - 20);

            int labelY = compact ? (top + 4) : (top + (height - 9) / 2);
            g.drawString(mc.font, labelText, left + 6, labelY, 0xFFFFFF);

            int ctrlY = compact ? (top + 20) : (top + (height - 14) / 2);
            this.editBox.setX(ctrlX);
            this.editBox.setY(ctrlY);
            this.editBox.setWidth(ctrlW);
            this.editBox.setHeight(14);
            this.editBox.render(g, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return Collections.singletonList(editBox);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return Collections.singletonList(editBox);
        }
    }

    public static class PositionEntry extends Entry {
        private final EditBox xEdit;
        private final EditBox yEdit;

        public PositionEntry(Minecraft mc, EditorDraftState draft, Runnable onChanged) {
            this.xEdit = new EditBox(mc.font, 0, 0, 50, 14, Component.literal(""));
            this.xEdit.setValue(String.valueOf(draft.xOffset));
            this.xEdit.setMaxLength(6);
            this.xEdit.setResponder(val -> {
                try {
                    draft.xOffset = val.trim().isEmpty() ? 0 : Integer.parseInt(val.trim());
                    onChanged.run();
                } catch (Exception ignored) {}
            });

            this.yEdit = new EditBox(mc.font, 0, 0, 50, 14, Component.literal(""));
            this.yEdit.setValue(draft.yOffset == Integer.MIN_VALUE ? "" : String.valueOf(draft.yOffset));
            this.yEdit.setMaxLength(6);
            this.yEdit.setResponder(val -> {
                try {
                    if (val.trim().isEmpty()) {
                        draft.yOffset = Integer.MIN_VALUE;
                    } else {
                        draft.yOffset = Integer.parseInt(val.trim());
                    }
                    onChanged.run();
                } catch (Exception ignored) {}
            });
        }

        @Override
        public void tick() {
            this.xEdit.tick();
            this.yEdit.tick();
        }

        @Override
        public void render(GuiGraphics g, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            Minecraft mc = Minecraft.getInstance();
            boolean compact = width < 360;

            int labelW = compact ? 0 : Math.min(130, (int) (width * 0.32));
            int ctrlX = compact ? (left + 6) : (left + labelW + 10);
            int ctrlW = compact ? (width - 12) : (width - labelW - 20);
            int inputW = (ctrlW - 12) / 2;

            int labelY = compact ? (top + 4) : (top + (height - 9) / 2);
            g.drawString(mc.font, "§7Posição X / Y", left + 6, labelY, 0xFFFFFF);

            int ctrlY = compact ? (top + 20) : (top + (height - 14) / 2);
            this.xEdit.setX(ctrlX);
            this.xEdit.setY(ctrlY);
            this.xEdit.setWidth(inputW);
            this.xEdit.setHeight(14);
            this.xEdit.render(g, mouseX, mouseY, partialTick);

            int commaY = compact ? (top + 22) : (top + (height - 9) / 2);
            g.drawString(mc.font, ",", ctrlX + inputW + 4, commaY, 0x80FFFFFF);

            this.yEdit.setX(ctrlX + inputW + 8);
            this.yEdit.setY(ctrlY);
            this.yEdit.setWidth(inputW);
            this.yEdit.setHeight(14);
            this.yEdit.render(g, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(xEdit, yEdit);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(xEdit, yEdit);
        }
    }

    public static class AlignmentEntry extends Entry {
        private final List<Button> buttons = new ArrayList<>();
        private final EditorDraftState draft;

        private static final String[] ALIGNMENTS = { "center", "left", "right" };
        private static final String[] ALIGN_LABELS = { "Centro", "Esq", "Dir" };

        public AlignmentEntry(Minecraft mc, EditorDraftState draft, Runnable onChanged) {
            this.draft = draft;
            for (int i = 0; i < 3; i++) {
                final int ai = i;
                Button btn = Button.builder(Component.literal(ALIGN_LABELS[i]), b -> {
                    draft.alignment = ALIGNMENTS[ai];
                    onChanged.run();
                }).build();
                buttons.add(btn);
            }
        }

        @Override
        public void render(GuiGraphics g, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            Minecraft mc = Minecraft.getInstance();
            boolean compact = width < 360;

            int labelW = compact ? 0 : Math.min(130, (int) (width * 0.32));
            int ctrlX = compact ? (left + 6) : (left + labelW + 10);
            int ctrlW = compact ? (width - 12) : (width - labelW - 20);

            int labelY = compact ? (top + 4) : (top + (height - 9) / 2);
            g.drawString(mc.font, "§7Alinhamento", left + 6, labelY, 0xFFFFFF);

            int btnW = (ctrlW - 2 * 4) / 3;
            int btnY = compact ? (top + 20) : (top + (height - 18) / 2);

            for (int i = 0; i < 3; i++) {
                Button btn = buttons.get(i);
                btn.setX(ctrlX + i * (btnW + 4));
                btn.setY(btnY);
                btn.setWidth(btnW);
                btn.setHeight(18);

                boolean sel = ALIGNMENTS[i].equalsIgnoreCase(draft.alignment);
                String label = ALIGN_LABELS[i];
                String labelText = sel ? "► " + label : label;
                btn.setMessage(Component.literal(fitLabel(labelText, btnW)));
                btn.render(g, mouseX, mouseY, partialTick);
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return buttons;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return buttons;
        }
    }

    public static class CycleButtonEntry<T> extends Entry {
        private final String labelText;
        private final Button button;
        private final T[] values;
        private final java.util.function.Function<T, String> nameMapper;
        private final java.util.function.Consumer<T> valSetter;
        private final java.util.function.Supplier<T> valGetter;

        public CycleButtonEntry(Minecraft mc, String labelText, T[] values, java.util.function.Function<T, String> nameMapper, java.util.function.Supplier<T> valGetter, java.util.function.Consumer<T> valSetter) {
            this.labelText = labelText;
            this.values = values;
            this.nameMapper = nameMapper;
            this.valGetter = valGetter;
            this.valSetter = valSetter;

            this.button = Button.builder(Component.literal(""), b -> {
                T current = valGetter.get();
                int idx = 0;
                for (int i = 0; i < values.length; i++) {
                    if (values[i] == current) {
                        idx = i;
                        break;
                    }
                }
                T next = values[(idx + 1) % values.length];
                valSetter.accept(next);
            }).build();
        }

        @Override
        public void render(GuiGraphics g, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            Minecraft mc = Minecraft.getInstance();
            boolean compact = width < 360;

            int labelW = compact ? 0 : Math.min(130, (int) (width * 0.32));
            int ctrlX = compact ? (left + 6) : (left + labelW + 10);
            int ctrlW = compact ? (width - 12) : (width - labelW - 20);

            int labelY = compact ? (top + 4) : (top + (height - 9) / 2);
            g.drawString(mc.font, labelText, left + 6, labelY, 0xFFFFFF);

            int btnY = compact ? (top + 20) : (top + (height - 18) / 2);
            this.button.setX(ctrlX);
            this.button.setY(btnY);
            this.button.setWidth(ctrlW);
            this.button.setHeight(18);

            T current = valGetter.get();
            String name = nameMapper.apply(current);
            this.button.setMessage(Component.literal(fitLabel(name, ctrlW)));

            this.button.render(g, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return Collections.singletonList(button);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return Collections.singletonList(button);
        }
    }


}
