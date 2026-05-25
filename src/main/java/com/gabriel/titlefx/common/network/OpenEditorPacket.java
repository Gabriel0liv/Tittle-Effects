package com.gabriel.titlefx.common.network;

import com.gabriel.titlefx.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenEditorPacket {
    public OpenEditorPacket() {}

    public static void encode(OpenEditorPacket msg, FriendlyByteBuf buf) {}

    public static OpenEditorPacket decode(FriendlyByteBuf buf) {
        return new OpenEditorPacket();
    }

    public static void handle(OpenEditorPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleOpenEditor());
        });
        ctx.setPacketHandled(true);
    }
}
