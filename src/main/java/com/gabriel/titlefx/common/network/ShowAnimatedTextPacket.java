package com.gabriel.titlefx.common.network;

import com.gabriel.titlefx.client.ClientPacketHandler;
import com.gabriel.titlefx.common.model.AnimatedTextPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ShowAnimatedTextPacket {
    private final AnimatedTextPayload payload;

    public ShowAnimatedTextPacket(AnimatedTextPayload payload) {
        this.payload = payload;
    }

    public AnimatedTextPayload getPayload() {
        return payload;
    }

    public static void encode(ShowAnimatedTextPacket msg, FriendlyByteBuf buf) {
        msg.payload.write(buf);
    }

    public static ShowAnimatedTextPacket decode(FriendlyByteBuf buf) {
        return new ShowAnimatedTextPacket(AnimatedTextPayload.read(buf));
    }

    public static void handle(ShowAnimatedTextPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleShow(msg.getPayload()));
        });
        ctx.setPacketHandled(true);
    }
}
