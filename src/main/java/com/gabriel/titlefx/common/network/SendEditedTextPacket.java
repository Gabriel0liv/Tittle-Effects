package com.gabriel.titlefx.common.network;

import com.gabriel.titlefx.common.config.TitleFxConfig;
import com.gabriel.titlefx.common.model.AnimatedTextPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SendEditedTextPacket {
    private final AnimatedTextPayload payload;
    private final boolean targetAll;

    public SendEditedTextPacket(AnimatedTextPayload payload, boolean targetAll) {
        this.payload = payload;
        this.targetAll = targetAll;
    }

    public AnimatedTextPayload getPayload() {
        return payload;
    }

    public boolean isTargetAll() {
        return targetAll;
    }

    public static void encode(SendEditedTextPacket msg, FriendlyByteBuf buf) {
        msg.payload.write(buf);
        buf.writeBoolean(msg.targetAll);
    }

    public static SendEditedTextPacket decode(FriendlyByteBuf buf) {
        return new SendEditedTextPacket(AnimatedTextPayload.read(buf), buf.readBoolean());
    }

    public static void handle(SendEditedTextPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            int permLevel = TitleFxConfig.COMMON.permissionLevel.get();
            if (!player.hasPermissions(permLevel)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cVocê não tem permissão para enviar títulos personalizados."));
                return;
            }
            if (msg.isTargetAll()) {
                NetworkHandler.sendToAll(new ShowAnimatedTextPacket(msg.getPayload()));
            } else {
                NetworkHandler.sendToPlayer(player, new ShowAnimatedTextPacket(msg.getPayload()));
            }
        });
        ctx.setPacketHandled(true);
    }
}
