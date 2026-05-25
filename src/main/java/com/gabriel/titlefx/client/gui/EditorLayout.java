package com.gabriel.titlefx.client.gui;

public record EditorLayout(
    int margin,
    int headerY,
    int previewX,
    int previewY,
    int previewW,
    int previewH,
    int previewButtonsY,
    int listX,
    int listY,
    int listW,
    int listH,
    int footerY,
    int footerH,
    boolean compact
) {
    public static EditorLayout calculate(int width, int height) {
        int margin = 12;
        int headerH = 24;
        int footerH = 28;
        int previewH = Math.max(80, Math.min(150, (int) (height * 0.30)));

        int previewX = margin;
        int previewY = headerH + margin;
        int previewW = width - margin * 2;

        int previewButtonsY = previewY + previewH + 4;

        int listY = previewButtonsY + 20 + 8; // 20 is button height
        int listH = height - listY - footerH - 8;

        int footerY = height - footerH;

        boolean compact = width < 680 || height < 460;

        return new EditorLayout(
            margin,
            6, // headerY
            previewX,
            previewY,
            previewW,
            previewH,
            previewButtonsY,
            margin, // listX
            listY,
            width - margin * 2, // listW
            listH,
            footerY,
            footerH,
            compact
        );
    }
}
