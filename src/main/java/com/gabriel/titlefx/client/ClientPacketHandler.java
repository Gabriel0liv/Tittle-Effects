package com.gabriel.titlefx.client;

import com.gabriel.titlefx.client.render.AnimatedTextManager;
import com.gabriel.titlefx.common.model.AnimatedTextPayload;

public class ClientPacketHandler {
    public static void handleShow(AnimatedTextPayload payload) {
        AnimatedTextManager.getInstance().showText(payload);
    }

    public static void handleClear(String clearType) {
        AnimatedTextManager.getInstance().clearText(clearType);
    }
}
