package com.gabriel.titlefx.client.overlay;

import com.gabriel.titlefx.client.render.AnimatedTextManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class AnimatedTextOverlay implements IGuiOverlay {
    public static final AnimatedTextOverlay INSTANCE = new AnimatedTextOverlay();

    private AnimatedTextOverlay() {}

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        AnimatedTextManager.getInstance().render(guiGraphics, partialTick, screenWidth, screenHeight);
    }
}
