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

        for (TextLayerPayload layer : payload.layers()) {
            AnimatedTextInstance newInstance = new AnimatedTextInstance(payload.id(), layer, payload.globalDurationMs(), now);

            // Replacement rules
            Iterator<AnimatedTextInstance> iterator = activeInstances.iterator();
            while (iterator.hasNext()) {
                AnimatedTextInstance active = iterator.next();
                
                // Same ID replaces immediately
                if (active.getId().equals(newInstance.getId())) {
                    iterator.remove();
                    continue;
                }

                // If replaceSameType is active, same standard type (title, subtitle, actionbar) replaces
                if (replaceSameType && !active.getType().equals("custom") && active.getType().equalsIgnoreCase(newInstance.getType())) {
                    iterator.remove();
                }
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
            // Clear by specific ID
            activeInstances.removeIf(instance -> instance.getId().equals(clearType));
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
                guiGraphics.drawString(mc.font, " -> [" + inst.getType().toUpperCase() + "] \"" + inst.getText() + "\" (Age: " + (now - inst.getCreationTime()) + "ms / " + inst.getDuration() + "ms)", 5, y, 0xAAAAAA);
                y += 10;
            }
        }

        // Draw instances
        for (AnimatedTextInstance instance : activeInstances) {
            instance.render(guiGraphics, partialTick, screenWidth, screenHeight, now);
        }
    }
}
