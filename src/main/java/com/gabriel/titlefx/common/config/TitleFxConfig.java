package com.gabriel.titlefx.common.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class TitleFxConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;
    
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        final Pair<Common, ForgeConfigSpec> specPairCommon = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON_SPEC = specPairCommon.getRight();
        COMMON = specPairCommon.getLeft();

        final Pair<Client, ForgeConfigSpec> specPairClient = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT_SPEC = specPairClient.getRight();
        CLIENT = specPairClient.getLeft();
    }

    public static class Common {
        public final ForgeConfigSpec.IntValue permissionLevel;
        public final ForgeConfigSpec.BooleanValue allowPreviewCommand;
        public final ForgeConfigSpec.IntValue maxMessageLength;
        public final ForgeConfigSpec.IntValue maxFontFileSizeMb;
        public final ForgeConfigSpec.IntValue fontChunkSizeKb;
        public final ForgeConfigSpec.IntValue maxTotalFontSyncMb;

        public Common(ForgeConfigSpec.Builder builder) {
            builder.push("general");
            
            permissionLevel = builder
                    .comment("Default permission level required to run /ctitle administrative commands.")
                    .defineInRange("permissionLevel", 2, 0, 4);

            allowPreviewCommand = builder
                    .comment("Whether non-admin players are allowed to preview presets using /ctitle preview.")
                    .define("allowPreviewCommand", true);

            maxMessageLength = builder
                    .comment("Maximum character length for incoming texts.")
                    .defineInRange("maxMessageLength", 512, 1, 4096);

            maxFontFileSizeMb = builder
                    .comment("Maximum allowed font file size in Megabytes for sync.")
                    .defineInRange("maxFontFileSizeMb", 16, 1, 128);

            fontChunkSizeKb = builder
                    .comment("Size of font sync chunks in Kilobytes.")
                    .defineInRange("fontChunkSizeKb", 32, 4, 256);

            maxTotalFontSyncMb = builder
                    .comment("Maximum allowed accumulated fonts sync size in Megabytes.")
                    .defineInRange("maxTotalFontSyncMb", 64, 4, 1024);

            builder.pop();
        }
    }

    public static class Client {
        public final ForgeConfigSpec.BooleanValue enableAnimations;
        public final ForgeConfigSpec.BooleanValue enableExternalFonts;
        public final ForgeConfigSpec.BooleanValue defaultShadow;
        public final ForgeConfigSpec.DoubleValue defaultScale;
        public final ForgeConfigSpec.BooleanValue reducedMotionMode;
        public final ForgeConfigSpec.BooleanValue debugOverlay;
        public final ForgeConfigSpec.IntValue maxActiveMessages;
        public final ForgeConfigSpec.BooleanValue replaceSameTypeMessages;

        public Client(ForgeConfigSpec.Builder builder) {
            builder.push("rendering");

            enableAnimations = builder
                    .comment("Enable text animations.")
                    .define("enableAnimations", true);

            enableExternalFonts = builder
                    .comment("Enable loading external fonts (deferred to post-MVP).")
                    .define("enableExternalFonts", false);

            defaultShadow = builder
                    .comment("Whether texts should have shadow by default.")
                    .define("defaultShadow", true);

            defaultScale = builder
                    .comment("Default text scaling factor.")
                    .defineInRange("defaultScale", 1.0, 0.1, 10.0);

            reducedMotionMode = builder
                    .comment("If true, disables aggressive screen-shaking, fast flashing, and glitch animations.")
                    .define("reducedMotionMode", false);

            debugOverlay = builder
                    .comment("Show debug information about active animated texts on the HUD.")
                    .define("debugOverlay", false);

            maxActiveMessages = builder
                    .comment("Maximum number of concurrent animated texts on screen.")
                    .defineInRange("maxActiveMessages", 8, 1, 32);

            replaceSameTypeMessages = builder
                    .comment("If true, a new TITLE replaces the active TITLE, same for SUBTITLE and ACTIONBAR.")
                    .define("replaceSameTypeMessages", true);

            builder.pop();
        }
    }
}
