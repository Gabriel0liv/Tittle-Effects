package com.gabriel.titlefx.common.network;

import com.gabriel.titlefx.client.ClientPacketHandler;
import com.gabriel.titlefx.common.font.FontInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class FontRegistrySyncPacket {
    private final String registryHash;
    private final String serverHash;
    private final List<FontInfo> fonts;

    public FontRegistrySyncPacket(String registryHash, String serverHash, List<FontInfo> fonts) {
        this.registryHash = registryHash;
        this.serverHash = serverHash;
        this.fonts = fonts;
    }

    public String getRegistryHash() {
        return registryHash;
    }

    public String getServerHash() {
        return serverHash;
    }

    public List<FontInfo> getFonts() {
        return fonts;
    }

    public static void encode(FontRegistrySyncPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.registryHash);
        buf.writeUtf(msg.serverHash);
        buf.writeInt(msg.fonts.size());
        for (FontInfo info : msg.fonts) {
            info.write(buf);
        }
    }

    public static FontRegistrySyncPacket decode(FriendlyByteBuf buf) {
        String registryHash = buf.readUtf();
        String serverHash = buf.readUtf();
        int size = buf.readInt();
        List<FontInfo> fonts = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            fonts.add(FontInfo.read(buf));
        }
        return new FontRegistrySyncPacket(registryHash, serverHash, fonts);
    }

    public static void handle(FontRegistrySyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleRegistrySync(msg));
        });
        ctx.setPacketHandled(true);
    }
}
