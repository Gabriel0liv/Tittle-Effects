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



        // /ctitle send <presetId> <targets> [text]
        base.then(Commands.literal("send")
            .requires(src -> src.hasPermission(permLevel))
            .then(Commands.argument("presetId", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(PresetManager.getPresetIds(), builder))
                .then(Commands.argument("targets", EntityArgument.players())
                    .executes(ctx -> executeSend(ctx, null))
                    .then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(ctx -> executeSend(ctx, StringArgumentType.getString(ctx, "text")))
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

        // /ctitle preview <presetId> [text]
        base.then(Commands.literal("preview")
            .requires(src -> {
                boolean allowPreview = TitleFxConfig.COMMON.allowPreviewCommand.get();
                return allowPreview || src.hasPermission(permLevel);
            })
            .then(Commands.argument("presetId", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(PresetManager.getPresetIds(), builder))
                .executes(CTitleCommand::executePreview)
                .then(Commands.argument("text", StringArgumentType.greedyString())
                    .executes(ctx -> executePreviewWithText(ctx, StringArgumentType.getString(ctx, "text")))
                )
            )
        );

        // /ctitle fonts list/reload/path
        base.then(Commands.literal("fonts")
            .then(Commands.literal("list")
                .requires(src -> {
                    boolean allowPreview = TitleFxConfig.COMMON.allowPreviewCommand.get();
                    return allowPreview || src.hasPermission(permLevel);
                })
                .executes(CTitleCommand::executeFontsList)
            )
            .then(Commands.literal("reload")
                .requires(src -> src.hasPermission(permLevel))
                .executes(CTitleCommand::executeFontsReload)
            )
            .then(Commands.literal("path")
                .requires(src -> src.hasPermission(permLevel))
                .executes(CTitleCommand::executeFontsPath)
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

        // /ctitle editor
        base.then(Commands.literal("editor")
            .requires(src -> {
                boolean allowPreview = TitleFxConfig.COMMON.allowPreviewCommand.get();
                return allowPreview || src.hasPermission(permLevel);
            })
            .executes(CTitleCommand::executeEditor)
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

            int globalDuration = data.durationMs != null ? data.durationMs : TextDefaults.getDefaultDuration(type);
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

    private static int executeSend(CommandContext<CommandSourceStack> context, String textOverride) {
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
        return executePreviewInternal(context, null);
    }

    private static int executePreviewWithText(CommandContext<CommandSourceStack> context, String text) {
        return executePreviewInternal(context, text);
    }

    private static int executePreviewInternal(CommandContext<CommandSourceStack> context, String textOverride) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String presetId = StringArgumentType.getString(context, "presetId");

            TitlePreset preset = PresetManager.getPreset(presetId);
            if (preset == null) {
                context.getSource().sendFailure(Component.literal("Preset '" + presetId + "' não encontrado."));
                return 0;
            }

            AnimatedTextPayload payload = preset.toPayload(textOverride);
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
        context.getSource().sendSuccess(() -> Component.literal("§6Fontes disponíveis no servidor:"), false);
        context.getSource().sendSuccess(() -> Component.literal(" - §aminecraft:default§7 (Padrão)"), false);
        for (String fontId : com.gabriel.titlefx.common.font.ServerFontManager.getRegisteredFontIds()) {
            context.getSource().sendSuccess(() -> Component.literal(" - §a" + fontId), false);
        }
        return 1;
    }

    private static int executeFontsPath(CommandContext<CommandSourceStack> context) {
        String path = com.gabriel.titlefx.common.font.ServerFontManager.getFontsDirAbsolutePath();
        context.getSource().sendSuccess(() -> Component.literal("§6[TitleFX] Caminho de Fontes do Servidor:"), false);
        context.getSource().sendSuccess(() -> Component.literal("§f" + path), false);
        return 1;
    }

    private static int executeFontsReload(CommandContext<CommandSourceStack> context) {
        com.gabriel.titlefx.common.font.ServerFontManager.ScanResult result = com.gabriel.titlefx.common.font.ServerFontManager.rescan();
        com.gabriel.titlefx.common.font.ServerFontManager.broadcastSync();
        
        // Em singleplayer/integrated server, garante sincronia local enviando diretamente ao executor se for um player
        try {
            net.minecraft.server.level.ServerPlayer player = context.getSource().getPlayerOrException();
            com.gabriel.titlefx.common.font.ServerFontManager.syncFontsToPlayer(player);
        } catch (Exception ignored) {}

        // Imprime diagnóstico detalhado no chat do jogo
        context.getSource().sendSuccess(() -> Component.literal("§6=== TitleFX Font Scan ==="), false);
        context.getSource().sendSuccess(() -> Component.literal("§7Pasta: §f" + result.absolutePath), false);
        context.getSource().sendSuccess(() -> Component.literal("§7Pasta existe: §f" + (result.directoryExists ? "sim" : "não")), false);
        context.getSource().sendSuccess(() -> Component.literal("§7Pasta legível: §f" + (result.directoryReadable ? "sim" : "não")), false);
        context.getSource().sendSuccess(() -> Component.literal("§7Total bruto de itens encontrados: §f" + result.rawFileCount), false);
        
        context.getSource().sendSuccess(() -> Component.literal("§7Arquivos encontrados:"), false);
        if (result.allFilesFound.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("  - Nenhum"), false);
        } else {
            for (String file : result.allFilesFound) {
                context.getSource().sendSuccess(() -> Component.literal("  - " + file), false);
            }
        }

        context.getSource().sendSuccess(() -> Component.literal("§7Candidatos .ttf/.otf:"), false);
        if (result.candidateFontFiles.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("  - Nenhum"), false);
        } else {
            for (String file : result.candidateFontFiles) {
                context.getSource().sendSuccess(() -> Component.literal("  - " + file), false);
            }
        }

        context.getSource().sendSuccess(() -> Component.literal("§7Registradas:"), false);
        if (result.registeredFonts.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("  - Nenhum"), false);
        } else {
            for (String fontMap : result.registeredFonts) {
                context.getSource().sendSuccess(() -> Component.literal("  - " + fontMap), false);
            }
        }

        if (!result.rejectedFiles.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("§cRejeitadas com motivo:"), false);
            for (Map.Entry<String, String> entry : result.rejectedFiles.entrySet()) {
                context.getSource().sendSuccess(() -> Component.literal("  - §c" + entry.getKey() + "§7: " + entry.getValue()), false);
            }
        } else {
            context.getSource().sendSuccess(() -> Component.literal("§7Rejeitadas com motivo: §aNenhuma"), false);
        }

        context.getSource().sendSuccess(() -> Component.literal("§aFontes do servidor recarregadas e sincronizadas com todos os jogadores!"), true);
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

    private static int executeEditor(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            NetworkHandler.sendToPlayer(player, new com.gabriel.titlefx.common.network.OpenEditorPacket());
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cApenas jogadores podem abrir a interface do editor."));
            return 0;
        }
    }

    public static class ParsedCommandData {
        public String text = "";
        public String fontId = "minecraft:default";
        public String color = null;
        public List<String> gradient = null;
        public float scale = -1.0f; // negative means unset
        public PositionPayload position;
        
        public RevealType revealType = RevealType.NONE;
        public int revealDuration = 1000;
        public LockMode lockMode = LockMode.LEFT_TO_RIGHT;
        public int flickerSpeed = 2;
        public String charset = "safe";
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
        data.position = null; // will resolve later if not set

        String optionsString = "";
        String textPart = "";

        int lastQuoteIdx = input.lastIndexOf('"');
        if (lastQuoteIdx != -1) {
            int secondToLastQuoteIdx = input.lastIndexOf('"', lastQuoteIdx - 1);
            if (secondToLastQuoteIdx != -1) {
                textPart = input.substring(secondToLastQuoteIdx + 1, lastQuoteIdx);
                optionsString = input.substring(0, secondToLastQuoteIdx).trim();
            } else {
                textPart = input.substring(lastQuoteIdx + 1).trim();
                optionsString = input.substring(0, lastQuoteIdx).trim();
            }
        } else {
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
        PositionPayload customPos = null;
        String anchor = null;
        Integer posX = null;
        Integer posY = null;
        String alignment = null;

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
                case "anchor":
                    anchor = val;
                    break;
                case "x":
                    try { posX = Integer.parseInt(val); } catch (Exception ignored) {}
                    break;
                case "y":
                    try { posY = Integer.parseInt(val); } catch (Exception ignored) {}
                    break;
                case "align":
                case "alignment":
                    alignment = val;
                    break;
            }
        }

        // Apply scale default if not set
        if (data.scale <= 0.0f) {
            data.scale = TextDefaults.getDefaultScale(type);
        }

        // Apply duration default if not set
        if (data.durationMs == null) {
            data.durationMs = TextDefaults.getDefaultDuration(type);
        }

        // Apply position default if custom parameters are missing, or construct custom
        PositionPayload defaultPos = TextDefaults.getDefaultPosition(type);
        if (anchor != null || posX != null || posY != null || alignment != null) {
            data.position = new PositionPayload(
                anchor != null ? anchor : defaultPos.anchor(),
                posX != null ? posX : defaultPos.x(),
                posY != null ? posY : defaultPos.y(),
                alignment != null ? alignment : defaultPos.alignment()
            );
        } else {
            data.position = defaultPos;
        }

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
            "out:", "out_duration:", "out_easing:",
            "anchor:", "x:", "y:", "alignment:"
        );

        if (!currentToken.contains(":")) {
            String prefix = lastSpace == -1 ? "" : input.substring(0, lastSpace + 1);
            for (String key : optionKeys) {
                if (key.startsWith(currentToken)) {
                    builder.suggest(prefix + key);
                }
            }
            if (currentToken.startsWith("\"") || currentToken.isEmpty()) {
                builder.suggest(prefix + "\"Texto aqui\"");
            }
        } else {
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
                    suggestions.add("minecraft:default");
                    suggestions.addAll(com.gabriel.titlefx.common.font.ServerFontManager.getRegisteredFontIds());
                    break;
                case "color":
                    suggestions.addAll(Arrays.asList("#FFFFFF", "#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#FF00FF", "#00FFFF"));
                    break;
                case "shadow":
                    suggestions.addAll(Arrays.asList("true", "false"));
                    break;
                case "anchor":
                    suggestions.addAll(Arrays.asList("center", "top", "bottom", "left", "right", "top_left", "top_right", "bottom_left", "bottom_right"));
                    break;
                case "alignment":
                    suggestions.addAll(Arrays.asList("center", "left", "right"));
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
