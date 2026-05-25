package com.gabriel.titlefx.common.network;

import com.gabriel.titlefx.TitleFxMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(TitleFxMod.MOD_ID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        CHANNEL.messageBuilder(ShowAnimatedTextPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(ShowAnimatedTextPacket::encode)
            .decoder(ShowAnimatedTextPacket::decode)
            .consumerMainThread(ShowAnimatedTextPacket::handle)
            .add();

        CHANNEL.messageBuilder(ClearAnimatedTextPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(ClearAnimatedTextPacket::encode)
            .decoder(ClearAnimatedTextPacket::decode)
            .consumerMainThread(ClearAnimatedTextPacket::handle)
            .add();



        CHANNEL.messageBuilder(OpenEditorPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(OpenEditorPacket::encode)
            .decoder(OpenEditorPacket::decode)
            .consumerMainThread(OpenEditorPacket::handle)
            .add();

        CHANNEL.messageBuilder(SendEditedTextPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(SendEditedTextPacket::encode)
            .decoder(SendEditedTextPacket::decode)
            .consumerMainThread(SendEditedTextPacket::handle)
            .add();
    }

    public static void sendToPlayer(ServerPlayer player, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static void sendToAll(Object msg) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), msg);
    }
}
