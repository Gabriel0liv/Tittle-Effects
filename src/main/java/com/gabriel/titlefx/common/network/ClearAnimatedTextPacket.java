package com.gabriel.titlefx.common.network;

import com.gabriel.titlefx.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClearAnimatedTextPacket {
    private final String clearType; // "all", "title", "subtitle", "actionbar", "custom", or an ID

    public ClearAnimatedTextPacket(String clearType) {
        this.clearType = clearType;
    }

    public String getClearType() {
        return clearType;
    }

    public static void encode(ClearAnimatedTextPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.clearType);
    }

    public static ClearAnimatedTextPacket decode(FriendlyByteBuf buf) {
        return new ClearAnimatedTextPacket(buf.readUtf());
    }

    public static void handle(ClearAnimatedTextPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleClear(msg.getClearType()));
        });
        ctx.setPacketHandled(true);
    }
}
