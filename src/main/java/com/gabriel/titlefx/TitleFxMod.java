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

    // Client-only event subscriber to register overlay
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void registerOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("animated_text", AnimatedTextOverlay.INSTANCE);
            LOGGER.info("TitleFX registered animated text HUD overlay.");
        }
    }
}
