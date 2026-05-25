package com.gabriel.titlefx.client.command;

import com.gabriel.titlefx.TitleFxMod;
import com.gabriel.titlefx.client.font.ClientFontCache;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = TitleFxMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientCommands {

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(
            Commands.literal("ctitleclient")
                .then(Commands.literal("fonts")
                    .then(Commands.literal("status")
                        .executes(context -> executeStatus(context))
                    )
                )
                .then(Commands.literal("status")
                    .executes(context -> executeStatus(context))
                )
        );

        dispatcher.register(
            Commands.literal("ctitle")
                .then(Commands.literal("fonts")
                    .then(Commands.literal("clientstatus")
                        .executes(context -> executeStatus(context))
                    )
                )
                .then(Commands.literal("clientstatus")
                    .executes(context -> executeStatus(context))
                )
        );
    }

    private static int executeStatus(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        List<ClientFontCache.ClientFontStatus> statuses = ClientFontCache.getClientFontStatuses();
        
        context.getSource().sendSuccess(() -> Component.literal("§6=== TitleFX Client Font Status ==="), false);
        if (statuses.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("§7Nenhuma fonte registrada no servidor para sincronização."), false);
            return 1;
        }

        for (ClientFontCache.ClientFontStatus status : statuses) {
            context.getSource().sendSuccess(() -> Component.literal("§eFonte: §f" + status.fontId), false);
            context.getSource().sendSuccess(() -> Component.literal("  - Existe no disco: " + (status.existsOnDisk ? "§asim" : "§cnão")), false);
            context.getSource().sendSuccess(() -> Component.literal("    - JSON da fonte: " + (status.jsonExists ? "§asim" : "§cnão")), false);
            context.getSource().sendSuccess(() -> Component.literal("    - Arquivo da fonte: " + (status.fontFileExists ? "§asim" : "§cnão")), false);
            context.getSource().sendSuccess(() -> Component.literal("  - Sincronizada no disco: " + (status.syncedOnDisk ? "§asim" : "§cnão")), false);
            context.getSource().sendSuccess(() -> Component.literal("  - Carregada no Minecraft: " + (status.loaded ? "§asim" : "§cnão")), false);
        }
        return 1;
    }
}
