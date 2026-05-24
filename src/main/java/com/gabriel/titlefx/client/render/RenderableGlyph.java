package com.gabriel.titlefx.client.render;

public class RenderableGlyph {
    public char targetChar;
    public char currentChar;
    public float xOffset = 0.0f;
    public float yOffset = 0.0f;
    public float scale = 1.0f;
    public int color = 0xFFFFFF;
    public float alpha = 1.0f;
    public boolean visible = true;

    public RenderableGlyph(char targetChar) {
        this.targetChar = targetChar;
        this.currentChar = targetChar;
    }
}
