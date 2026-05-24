package com.gabriel.titlefx.common.command;

import com.gabriel.titlefx.TitleFxMod;
import com.gabriel.titlefx.common.animation.*;
import com.gabriel.titlefx.common.config.TitleFxConfig;
import com.gabriel.titlefx.common.model.*;
import com.gabriel.titlefx.common.network.ClearAnimatedTextPacket;
import com.gabriel.titlefx.common.network.NetworkHandler;
import com.gabriel.titlefx.common.network.ShowAnimatedTextPacket;
import com.gabriel.titlefx.common.preset.PresetManager;
import com.gabriel.titlefx.common.preset.TitlePreset;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class CTitleCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        int permLevel = TitleFxConfig.COMMON.permissionLevel.get();

        LiteralArgumentBuilder<CommandSourceStack> base = Commands.literal("ctitle");

        // /ctitle show <targets> <type> [options...] <text>
        base.then(Commands.literal("show")
            .requires(src -> src.hasPermission(permLevel))
            .then(Commands.argument("targets", EntityArgument.players())
                .then(Commands.argument("type", StringArgumentType.word())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(Arrays.asList("title", "subtitle", "actionbar", "custom"), builder))
                    .then(Commands.argument("options_and_text", StringArgumentType.greedyString())
                        .suggests(CTitleCommand::suggestOptionsAndText)
                        .executes(CTitleCommand::executeShow)
                    )
                )
            )
        );

        // /ctitle preset <targets> <presetId> [text_override]
        base.then(Commands.literal("preset")
            .requires(src -> src.hasPermission(permLevel))
            .then(Commands.argument("targets", EntityArgument.players())
                .then(Commands.argument("presetId", StringArgumentType.word())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(PresetManager.getPresetIds(), builder))
                    .executes(ctx -> executePreset(ctx, null))
                    .then(Commands.argument("text_override", StringArgumentType.string())
                        .executes(ctx -> executePreset(ctx, StringArgumentType.getString(ctx, "text_override")))
                    )
                )
            )
        );

        // /ctitle clear <targets> [type]
        base.then(Commands.literal("clear")
            .requires(src -> src.hasPermission(permLevel))
            .then(Commands.argument("targets", EntityArgument.players())
                .executes(ctx -> executeClear(ctx, "all"))
                .then(Commands.argument("clear_type", StringArgumentType.word())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(Arrays.asList("all", "title", "subtitle", "actionbar", "custom"), builder))
                    .executes(ctx -> executeClear(ctx, StringArgumentType.getString(ctx, "clear_type")))
                )
            )
        );

        // /ctitle preview <presetId>
        base.then(Commands.literal("preview")
            .requires(src -> {
                boolean allowPreview = TitleFxConfig.COMMON.allowPreviewCommand.get();
                return allowPreview || src.hasPermission(permLevel);
            })
            .then(Commands.argument("presetId", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(PresetManager.getPresetIds(), builder))
                .executes(CTitleCommand::executePreview)
            )
        );

        // /ctitle fonts list
        base.then(Commands.literal("fonts")
            .then(Commands.literal("list")
                .requires(src -> {
                    boolean allowPreview = TitleFxConfig.COMMON.allowPreviewCommand.get();
                    return allowPreview || src.hasPermission(permLevel);
                })
                .executes(CTitleCommand::executeFontsList)
            )
        );

        // /ctitle animations list
        base.then(Commands.literal("animations")
            .then(Commands.literal("list")
                .requires(src -> {
                    boolean allowPreview = TitleFxConfig.COMMON.allowPreviewCommand.get();
                    return allowPreview || src.hasPermission(permLevel);
                })
                .executes(CTitleCommand::executeAnimationsList)
            )
        );

        // /ctitle presets list
        base.then(Commands.literal("presets")
            .then(Commands.literal("list")
                .requires(src -> {
                    boolean allowPreview = TitleFxConfig.COMMON.allowPreviewCommand.get();
                    return allowPreview || src.hasPermission(permLevel);
                })
                .executes(CTitleCommand::executePresetsList)
            )
        );

        // /ctitle reload
        base.then(Commands.literal("reload")
            .requires(src -> src.hasPermission(permLevel))
            .executes(CTitleCommand::executeReload)
        );

        dispatcher.register(base);
    }

    private static int executeShow(CommandContext<CommandSourceStack> context) {
        try {
            Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
            String type = StringArgumentType.getString(context, "type");
            String optionsAndText = StringArgumentType.getString(context, "options_and_text");

            ParsedCommandData data = parseOptionsAndText(optionsAndText, type);

            // Construct payload
            TextLayerPayload layer = new TextLayerPayload(
                type,
                data.text,
                data.fontId,
                data.color,
                data.gradient,
                data.scale,
                data.position,
                data.reveal,
                data.in,
                data.idle,
                data.out,
                data.durationMs
            );

            int globalDuration = data.durationMs != null ? data.durationMs : 3000;
            AnimatedTextPayload payload = new AnimatedTextPayload(
                UUID.randomUUID().toString(),
                Collections.singletonList(layer),
                globalDuration
            );

            ShowAnimatedTextPacket packet = new ShowAnimatedTextPacket(payload);
            for (ServerPlayer player : players) {
                NetworkHandler.sendToPlayer(player, packet);
            }

            context.getSource().sendSuccess(() -> Component.literal("Enviado texto animado para " + players.size() + " jogadores."), true);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Erro ao executar comando: " + e.getMessage()));
            TitleFxMod.LOGGER.error("Error executing show command", e);
            return 0;
        }
    }

    private static int executePreset(CommandContext<CommandSourceStack> context, String textOverride) {
        try {
            Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
            String presetId = StringArgumentType.getString(context, "presetId");

            TitlePreset preset = PresetManager.getPreset(presetId);
            if (preset == null) {
                context.getSource().sendFailure(Component.literal("Preset '" + presetId + "' não encontrado."));
                return 0;
            }

            AnimatedTextPayload payload = preset.toPayload(textOverride);
            ShowAnimatedTextPacket packet = new ShowAnimatedTextPacket(payload);

            for (ServerPlayer player : players) {
                NetworkHandler.sendToPlayer(player, packet);
            }

            context.getSource().sendSuccess(() -> Component.literal("Enviado preset '" + presetId + "' para " + players.size() + " jogadores."), true);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Erro ao processar preset: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeClear(CommandContext<CommandSourceStack> context, String clearType) {
        try {
            Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
            ClearAnimatedTextPacket packet = new ClearAnimatedTextPacket(clearType);

            for (ServerPlayer player : players) {
                NetworkHandler.sendToPlayer(player, packet);
            }

            context.getSource().sendSuccess(() -> Component.literal("Enviado comando clear (" + clearType + ") para " + players.size() + " jogadores."), true);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Erro: " + e.getMessage()));
            return 0;
        }
    }

    private static int executePreview(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String presetId = StringArgumentType.getString(context, "presetId");

            TitlePreset preset = PresetManager.getPreset(presetId);
            if (preset == null) {
                context.getSource().sendFailure(Component.literal("Preset '" + presetId + "' não encontrado."));
                return 0;
            }

            AnimatedTextPayload payload = preset.toPayload(null);
            ShowAnimatedTextPacket packet = new ShowAnimatedTextPacket(payload);

            NetworkHandler.sendToPlayer(player, packet);
            context.getSource().sendSuccess(() -> Component.literal("Mostrando preview do preset '" + presetId + "'."), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Erro ao executar preview: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeFontsList(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("§6Fontes disponíveis no MVP:"), false);
        context.getSource().sendSuccess(() -> Component.literal(" - §aminecraft:default§7 (Padrão)"), false);
        context.getSource().sendSuccess(() -> Component.literal(" - §atitlefx:medieval§7 (Stub/Fallback no MVP)"), false);
        context.getSource().sendSuccess(() -> Component.literal(" - §atitlefx:horror§7 (Stub/Fallback no MVP)"), false);
        context.getSource().sendSuccess(() -> Component.literal(" - §atitlefx:pixel§7 (Stub/Fallback no MVP)"), false);
        return 1;
    }

    private static int executeAnimationsList(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("§6Tipos de Reveal:"), false);
        for (RevealType t : RevealType.values()) {
            context.getSource().sendSuccess(() -> Component.literal(" - " + t.name().toLowerCase()), false);
        }
        context.getSource().sendSuccess(() -> Component.literal("§6Tipos de In Animation:"), false);
        for (InAnimationType t : InAnimationType.values()) {
            context.getSource().sendSuccess(() -> Component.literal(" - " + t.name().toLowerCase()), false);
        }
        context.getSource().sendSuccess(() -> Component.literal("§6Tipos de Idle Animation:"), false);
        for (IdleAnimationType t : IdleAnimationType.values()) {
            context.getSource().sendSuccess(() -> Component.literal(" - " + t.name().toLowerCase()), false);
        }
        context.getSource().sendSuccess(() -> Component.literal("§6Tipos de Out Animation:"), false);
        for (OutAnimationType t : OutAnimationType.values()) {
            context.getSource().sendSuccess(() -> Component.literal(" - " + t.name().toLowerCase()), false);
        }
        return 1;
    }

    private static int executePresetsList(CommandContext<CommandSourceStack> context) {
        Set<String> ids = PresetManager.getPresetIds();
        context.getSource().sendSuccess(() -> Component.literal("§6Presets carregados: " + String.join(", ", ids)), false);
        return 1;
    }

    private static int executeReload(CommandContext<CommandSourceStack> context) {
        PresetManager.init();
        context.getSource().sendSuccess(() -> Component.literal("§aConfiguração e presets do TitleFX recarregados com sucesso!"), true);
        return 1;
    }

    // Helper classes and parsing
    public static class ParsedCommandData {
        public String text = "";
        public String fontId = "minecraft:default";
        public String color = null;
        public List<String> gradient = null;
        public float scale = 1.0f;
        public PositionPayload position;
        
        public RevealType revealType = RevealType.NONE;
        public int revealDuration = 1000;
        public LockMode lockMode = LockMode.LEFT_TO_RIGHT;
        public int flickerSpeed = 2;
        public String charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        public boolean preserveSpaces = true;
        public boolean preserveCase = true;

        public InAnimationType inAnim = InAnimationType.NONE;
        public int inDuration = 500;
        public Easing inEasing = Easing.LINEAR;

        public IdleAnimationType idleAnim = IdleAnimationType.NONE;
        public float idleIntensity = 1.0f;

        public OutAnimationType outAnim = OutAnimationType.NONE;
        public int outDuration = 500;
        public Easing outEasing = Easing.LINEAR;

        public Integer durationMs = null;

        public RevealPayload reveal;
        public InAnimPayload in;
        public IdleAnimPayload idle;
        public OutAnimPayload out;
    }

    public static ParsedCommandData parseOptionsAndText(String input, String type) {
        ParsedCommandData data = new ParsedCommandData();
        data.position = PositionPayload.defaultForType(type);

        String optionsString = "";
        String textPart = "";

        // Check if there are quotes. The text should ideally be at the end, in quotes.
        int lastQuoteIdx = input.lastIndexOf('"');
        if (lastQuoteIdx != -1) {
            int secondToLastQuoteIdx = input.lastIndexOf('"', lastQuoteIdx - 1);
            if (secondToLastQuoteIdx != -1) {
                textPart = input.substring(secondToLastQuoteIdx + 1, lastQuoteIdx);
                optionsString = input.substring(0, secondToLastQuoteIdx).trim();
            } else {
                // Only one quote: fallback
                textPart = input.substring(lastQuoteIdx + 1).trim();
                optionsString = input.substring(0, lastQuoteIdx).trim();
            }
        } else {
            // No quotes at all.
            // Check if there are colon indicators. If so, parse everything before the last word as options?
            // Safer: split by space, tokens with ':' are options. The remaining tokens are concatenated as text.
            String[] tokens = input.split("\\s+");
            StringBuilder optionsBuilder = new StringBuilder();
            StringBuilder textBuilder = new StringBuilder();
            for (String t : tokens) {
                if (t.contains(":")) {
                    optionsBuilder.append(t).append(" ");
                } else {
                    textBuilder.append(t).append(" ");
                }
            }
            optionsString = optionsBuilder.toString().trim();
            textPart = textBuilder.toString().trim();
        }

        data.text = textPart;

        // Parse options
        String[] optTokens = optionsString.split("\\s+");
        for (String token : optTokens) {
            if (!token.contains(":")) continue;
            int colon = token.indexOf(':');
            String key = token.substring(0, colon).toLowerCase(Locale.ROOT);
            String val = token.substring(colon + 1);

            switch (key) {
                case "font":
                    data.fontId = val;
                    break;
                case "color":
                    data.color = val;
                    break;
                case "gradient":
                    data.gradient = Arrays.asList(val.split(","));
                    break;
                case "scale":
                    try { data.scale = Float.parseFloat(val); } catch (Exception ignored) {}
                    break;
                case "reveal":
                    data.revealType = RevealType.fromString(val);
                    break;
                case "reveal_duration":
                case "revealduration":
                    try { data.revealDuration = Integer.parseInt(val); } catch (Exception ignored) {}
                    break;
                case "lock_mode":
                case "lockmode":
                    data.lockMode = LockMode.fromString(val);
                    break;
                case "flicker_speed":
                case "flickerspeed":
                    try { data.flickerSpeed = Integer.parseInt(val); } catch (Exception ignored) {}
                    break;
                case "charset":
                    data.charset = val;
                    break;
                case "in":
                    data.inAnim = InAnimationType.fromString(val);
                    break;
                case "in_duration":
                case "induration":
                    try { data.inDuration = Integer.parseInt(val); } catch (Exception ignored) {}
                    break;
                case "easing":
                    data.inEasing = Easing.fromString(val);
                    data.outEasing = Easing.fromString(val);
                    break;
                case "in_easing":
                case "ineasing":
                    data.inEasing = Easing.fromString(val);
                    break;
                case "idle":
                    data.idleAnim = IdleAnimationType.fromString(val);
                    break;
                case "intensity":
                case "idle_intensity":
                case "idleintensity":
                    try { data.idleIntensity = Float.parseFloat(val); } catch (Exception ignored) {}
                    break;
                case "out":
                    data.outAnim = OutAnimationType.fromString(val);
                    break;
                case "out_duration":
                case "outduration":
                    try { data.outDuration = Integer.parseInt(val); } catch (Exception ignored) {}
                    break;
                case "out_easing":
                case "outeasing":
                    data.outEasing = Easing.fromString(val);
                    break;
                case "duration":
                    try { data.durationMs = Integer.parseInt(val); } catch (Exception ignored) {}
                    break;
            }
        }

        // Validate options and construct payloads
        data.reveal = new RevealPayload(
            data.revealType,
            data.revealDuration,
            data.lockMode,
            data.flickerSpeed,
            data.charset,
            data.preserveSpaces,
            data.preserveCase
        );
        data.in = new InAnimPayload(data.inAnim, data.inDuration, data.inEasing);
        data.idle = new IdleAnimPayload(data.idleAnim, data.idleIntensity);
        data.out = new OutAnimPayload(data.outAnim, data.outDuration, data.outEasing);

        return data;
    }

    private static CompletableFuture<Suggestions> suggestOptionsAndText(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String input = builder.getRemaining();
        int lastSpace = input.lastIndexOf(' ');
        String currentToken = lastSpace == -1 ? input : input.substring(lastSpace + 1);

        List<String> optionKeys = Arrays.asList(
            "font:", "color:", "gradient:", "scale:", "duration:",
            "reveal:", "reveal_duration:", "lock_mode:", "flicker_speed:", "charset:",
            "in:", "in_duration:", "in_easing:",
            "idle:", "idle_intensity:",
            "out:", "out_duration:", "out_easing:"
        );

        if (!currentToken.contains(":")) {
            // Suggest option keys
            String prefix = lastSpace == -1 ? "" : input.substring(0, lastSpace + 1);
            for (String key : optionKeys) {
                if (key.startsWith(currentToken)) {
                    builder.suggest(prefix + key);
                }
            }
            // Also suggest starting quotes for text
            if (currentToken.startsWith("\"") || currentToken.isEmpty()) {
                builder.suggest(prefix + "\"Texto aqui\"");
            }
        } else {
            // Suggest values for the key
            int colonIdx = currentToken.indexOf(':');
            String key = currentToken.substring(0, colonIdx).toLowerCase(Locale.ROOT);
            String valPrefix = currentToken.substring(colonIdx + 1);
            String prefix = lastSpace == -1 ? "" : input.substring(0, lastSpace + 1);

            List<String> suggestions = new ArrayList<>();
            switch (key) {
                case "reveal":
                    for (RevealType t : RevealType.values()) suggestions.add(t.name().toLowerCase());
                    break;
                case "in":
                    for (InAnimationType t : InAnimationType.values()) suggestions.add(t.name().toLowerCase());
                    break;
                case "idle":
                    for (IdleAnimationType t : IdleAnimationType.values()) suggestions.add(t.name().toLowerCase());
                    break;
                case "out":
                    for (OutAnimationType t : OutAnimationType.values()) suggestions.add(t.name().toLowerCase());
                    break;
                case "easing":
                case "in_easing":
                case "out_easing":
                    for (Easing e : Easing.values()) suggestions.add(e.name().toLowerCase());
                    break;
                case "lock_mode":
                    for (LockMode m : LockMode.values()) suggestions.add(m.name().toLowerCase());
                    break;
                case "font":
                    suggestions.addAll(Arrays.asList("minecraft:default", "titlefx:medieval", "titlefx:horror", "titlefx:pixel"));
                    break;
                case "color":
                    suggestions.addAll(Arrays.asList("#FFFFFF", "#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#FF00FF", "#00FFFF"));
                    break;
                case "shadow":
                    suggestions.addAll(Arrays.asList("true", "false"));
                    break;
            }

            for (String sug : suggestions) {
                if (sug.startsWith(valPrefix)) {
                    builder.suggest(prefix + key + ":" + sug);
                }
            }
        }

        return builder.buildFuture();
    }
}
