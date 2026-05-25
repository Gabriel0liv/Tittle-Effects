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
        int footerY = height - footerH;

        // Space available between header bottom and footer top (with margins)
        int availableSpace = (height - footerH - 8) - (headerH + margin);

        // Target listH of at least 80px (or 50px if height is very small)
        int minListH = Math.max(50, Math.min(80, height / 4));

        // Target previewH: 22% of height, clamped between 40 and 120
        int targetPreviewH = Math.max(40, Math.min(120, (int) (height * 0.22)));

        int previewH = targetPreviewH;
        int listH = availableSpace - 32 - previewH;

        if (listH < minListH) {
            int deficit = minListH - listH;
            previewH = Math.max(30, previewH - deficit);
            listH = availableSpace - 32 - previewH;
        }

        if (listH < 40) {
            listH = 40;
        }

        int previewX = margin;
        int previewY = headerH + margin;
        int previewW = width - margin * 2;

        int previewButtonsY = previewY + previewH + 4;
        int listY = previewButtonsY + 20 + 8;

        if (listY + listH > footerY - 4) {
            listY = footerY - 4 - listH;
            previewButtonsY = listY - 8 - 20;
            previewY = previewButtonsY - 4 - previewH;

            if (previewY < headerH + 2) {
                int shift = (headerH + 2) - previewY;
                previewY = headerH + 2;
                previewH = Math.max(0, previewH - shift);
                previewButtonsY = previewY + previewH + 4;
                listY = previewButtonsY + 20 + 8;
                listH = Math.max(30, footerY - 4 - listY);
            }
        }

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
