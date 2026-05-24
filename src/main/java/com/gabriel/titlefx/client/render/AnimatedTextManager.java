package com.gabriel.titlefx.client.render;

import com.gabriel.titlefx.client.overlay.AnimatedTextOverlay;
import com.gabriel.titlefx.common.config.TitleFxConfig;
import com.gabriel.titlefx.common.model.AnimatedTextPayload;
import com.gabriel.titlefx.common.model.TextLayerPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AnimatedTextManager {
    private static final AnimatedTextManager INSTANCE = new AnimatedTextManager();

    private final List<AnimatedTextInstance> activeInstances = new ArrayList<>();

    public static AnimatedTextManager getInstance() {
        return INSTANCE;
    }

    private AnimatedTextManager() {}

    public synchronized void showText(AnimatedTextPayload payload) {
        long now = System.currentTimeMillis();
        int maxMessages = TitleFxConfig.CLIENT.maxActiveMessages.get();
        boolean replaceSameType = TitleFxConfig.CLIENT.replaceSameTypeMessages.get();

        // 1. Remove any active layers that belong to the SAME payload ID before processing new ones
        activeInstances.removeIf(instance -> instance.getPayloadId().equals(payload.id()));

        // 2. Loop through the new payload layers
        for (int i = 0; i < payload.layers().size(); i++) {
            TextLayerPayload layer = payload.layers().get(i);
            String instanceId = payload.id() + ":" + i;
            AnimatedTextInstance newInstance = new AnimatedTextInstance(
                payload.id(), // payloadId
                instanceId,   // instanceId
                layer,
                payload.globalDurationMs(),
                now
            );

            // 3. Replace active instances of the same type if they belong to a DIFFERENT payload ID
            if (replaceSameType && !newInstance.getType().equalsIgnoreCase("custom")) {
                activeInstances.removeIf(active -> 
                    active.getType().equalsIgnoreCase(newInstance.getType()) &&
                    !active.getPayloadId().equals(newInstance.getPayloadId())
                );
            }

            activeInstances.add(newInstance);
        }

        // Keep size within limits
        while (activeInstances.size() > maxMessages) {
            activeInstances.remove(0);
        }
    }

    public synchronized void clearText(String clearType) {
        if ("all".equalsIgnoreCase(clearType)) {
            activeInstances.clear();
        } else if ("title".equalsIgnoreCase(clearType) || "subtitle".equalsIgnoreCase(clearType) || "actionbar".equalsIgnoreCase(clearType) || "custom".equalsIgnoreCase(clearType)) {
            activeInstances.removeIf(instance -> instance.getType().equalsIgnoreCase(clearType));
        } else {
            // Clear by specific payload ID or unique layer instance ID
            activeInstances.removeIf(instance -> instance.getPayloadId().equals(clearType) || instance.getId().equals(clearType));
        }
    }

    public synchronized void render(GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        long now = System.currentTimeMillis();

        // Expire old ones
        activeInstances.removeIf(instance -> instance.isExpired(now));

        if (activeInstances.isEmpty()) return;

        // Render debug info if config is active
        if (TitleFxConfig.CLIENT.debugOverlay.get()) {
            Minecraft mc = Minecraft.getInstance();
            int y = 5;
            guiGraphics.drawString(mc.font, "TitleFX Debug - Active: " + activeInstances.size(), 5, y, 0xFFFFFF);
            y += 10;
            for (AnimatedTextInstance inst : activeInstances) {
                String debugText = String.format("[%s] \"%s\" | Scale: %.2f | Anchor: %s | Offset: [%d, %d] | Age: %d/%dms | R:%s, In:%s, Id:%s, Out:%s",
                    inst.getType().toUpperCase(),
                    inst.getText(),
                    inst.getLayer().scale(),
                    inst.getLayer().position().anchor(),
                    inst.getLayer().position().x(),
                    inst.getLayer().position().y(),
                    (now - inst.getCreationTime()),
                    inst.getDuration(),
                    inst.getLayer().reveal().type().name(),
                    inst.getLayer().in().type().name(),
                    inst.getLayer().idle().type().name(),
                    inst.getLayer().out().type().name()
                );
                guiGraphics.drawString(mc.font, debugText, 5, y, 0xAAAAAA);
                y += 10;
            }
        }

        // Draw instances
        for (AnimatedTextInstance instance : activeInstances) {
            instance.render(guiGraphics, partialTick, screenWidth, screenHeight, now);
        }
    }
}
