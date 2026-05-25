package com.gabriel.titlefx;

import com.gabriel.titlefx.client.overlay.AnimatedTextOverlay;
import com.gabriel.titlefx.common.command.CTitleCommand;
import com.gabriel.titlefx.common.config.TitleFxConfig;
import com.gabriel.titlefx.common.network.NetworkHandler;
import com.gabriel.titlefx.common.preset.PresetManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(TitleFxMod.MOD_ID)
public class TitleFxMod {
    public static final String MOD_ID = "titlefx";
    public static final Logger LOGGER = LogManager.getLogger("TitleFX");

    public TitleFxMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register Configs
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, TitleFxConfig.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, TitleFxConfig.CLIENT_SPEC);

        // Mod bus events
        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::clientSetup);

        // Forge bus events
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            NetworkHandler.register();
            PresetManager.init();
            com.gabriel.titlefx.common.font.ServerFontManager.init();
        });
        LOGGER.info("TitleFX Common Setup completed.");
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("TitleFX Client Setup completed.");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CTitleCommand.register(event.getDispatcher());
        LOGGER.info("TitleFX registered server-side commands.");
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            com.gabriel.titlefx.common.font.ServerFontManager.onPlayerLoggedIn(player);
        }
    }

    // Client-only event subscriber to register overlay
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void registerOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("animated_text", AnimatedTextOverlay.INSTANCE);
            LOGGER.info("TitleFX registered animated text HUD overlay.");
        }

        @SubscribeEvent
        public static void onAddPackFinders(net.minecraftforge.event.AddPackFindersEvent event) {
            if (event.getPackType() == net.minecraft.server.packs.PackType.CLIENT_RESOURCES) {
                java.nio.file.Path resourcePath = net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().resolve("titlefx/font_cache/active/generated_pack");
                
                java.io.File packDir = resourcePath.toFile();
                if (!packDir.exists()) {
                    packDir.mkdirs();
                }
                java.io.File mcmeta = new java.io.File(packDir, "pack.mcmeta");
                if (!mcmeta.exists()) {
                    try {
                        String metaJson = "{\n" +
                                "  \"pack\": {\n" +
                                "    \"pack_format\": 15,\n" +
                                "    \"description\": \"TitleFX Server Fonts\"\n" +
                                "  }\n" +
                                "}";
                        java.nio.file.Files.writeString(mcmeta.toPath(), metaJson, java.nio.charset.StandardCharsets.UTF_8);
                    } catch (Exception ignored) {}
                }

                event.addRepositorySource((consumer) -> {
                    net.minecraft.server.packs.repository.Pack pack = net.minecraft.server.packs.repository.Pack.readMetaAndCreate(
                        "titlefx:generated_pack",
                        net.minecraft.network.chat.Component.literal("TitleFX Server Fonts"),
                        true, // always active
                        (id) -> new net.minecraft.server.packs.PathPackResources(id, resourcePath, false),
                        net.minecraft.server.packs.PackType.CLIENT_RESOURCES,
                        net.minecraft.server.packs.repository.Pack.Position.TOP,
                        net.minecraft.server.packs.repository.PackSource.BUILT_IN
                    );
                    if (pack != null) {
                        consumer.accept(pack);
                    }
                });
            }
        }
    }
}
