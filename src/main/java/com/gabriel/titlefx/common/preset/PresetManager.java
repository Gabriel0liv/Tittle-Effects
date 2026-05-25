package com.gabriel.titlefx.common.preset;

import com.gabriel.titlefx.TitleFxMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PresetManager {
    private static final Map<String, TitlePreset> PRESETS = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void init() {
        PRESETS.clear();
        File presetsDir = new File("config/titlefx/presets");
        if (!presetsDir.exists()) {
            presetsDir.mkdirs();
        }

        File defaultFile = new File(presetsDir, "default_presets_v4.json");
        if (!defaultFile.exists()) {
            writeDefaultPresets(defaultFile);
        }

        loadPresetsFromDir(presetsDir);
    }

    public static Set<String> getPresetIds() {
        return PRESETS.keySet();
    }

    public static TitlePreset getPreset(String id) {
        return PRESETS.get(id);
    }

    private static void loadPresetsFromDir(File dir) {
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) return;

        // Sort: v4 loads last and wins over v3
        java.util.Arrays.sort(files, (f1, f2) -> {
            if ("default_presets_v4.json".equals(f1.getName())) return 1;
            if ("default_presets_v4.json".equals(f2.getName())) return -1;
            return f1.getName().compareTo(f2.getName());
        });

        for (File file : files) {
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                Map<String, TitlePreset> loaded = GSON.fromJson(reader, new TypeToken<Map<String, TitlePreset>>(){}.getType());
                if (loaded != null) {
                    PRESETS.putAll(loaded);
                    TitleFxMod.LOGGER.info("Loaded " + loaded.size() + " presets from " + file.getName());
                }
            } catch (Exception e) {
                TitleFxMod.LOGGER.error("Failed to load preset file: " + file.getName() + " - " + e.getMessage());
            }
        }
    }

    private static void writeDefaultPresets(File file) {
        String defaultJson = "{\n" +
            "  \"boss_intro\": {\n" +
            "    \"type\": \"title\",\n" +
            "    \"color\": \"#AA0000\",\n" +
            "    \"gradient\": [\"#FF0000\", \"#550000\"],\n" +
            "    \"duration\": 5000,\n" +
            "    \"reveal\": {\n" +
            "      \"type\": \"obfuscated_decode\",\n" +
            "      \"speed\": \"cinematic\"\n" +
            "    },\n" +
            "    \"in\": {\n" +
            "      \"type\": \"cinematic_zoom_in\",\n" +
            "      \"duration\": 600\n" +
            "    },\n" +
            "    \"idle\": {\n" +
            "      \"type\": \"subtle_shake\",\n" +
            "      \"intensity\": 0.8\n" +
            "    },\n" +
            "    \"out\": {\n" +
            "      \"type\": \"shrink_fade\",\n" +
            "      \"duration\": 600\n" +
            "    },\n" +
            "    \"subtitle\": {\n" +
            "      \"type\": \"subtitle\",\n" +
            "      \"text\": \"Prepare-se!\",\n" +
            "      \"color\": \"#FFFFFF\",\n" +
            "      \"reveal\": {\n" +
            "        \"type\": \"typewriter\",\n" +
            "        \"speed\": \"normal\"\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "\n" +
            "  \"quest_start\": {\n" +
            "    \"type\": \"title\",\n" +
            "    \"color\": \"#FFD56A\",\n" +
            "    \"duration\": 3500,\n" +
            "    \"reveal\": {\n" +
            "      \"type\": \"word_by_word\",\n" +
            "      \"speed\": \"normal\"\n" +
            "    },\n" +
            "    \"in\": {\n" +
            "      \"type\": \"fade_in\",\n" +
            "      \"duration\": 400\n" +
            "    },\n" +
            "    \"idle\": {\n" +
            "      \"type\": \"breathing\",\n" +
            "      \"intensity\": 1.0\n" +
            "    },\n" +
            "    \"out\": {\n" +
            "      \"type\": \"fade_out\",\n" +
            "      \"duration\": 400\n" +
            "    }\n" +
            "  },\n" +
            "\n" +
            "  \"location_intro\": {\n" +
            "    \"type\": \"title\",\n" +
            "    \"color\": \"#FFFFFF\",\n" +
            "    \"duration\": 3000,\n" +
            "    \"reveal\": {\n" +
            "      \"type\": \"fade_chars\",\n" +
            "      \"speed\": \"cinematic\"\n" +
            "    },\n" +
            "    \"in\": {\n" +
            "      \"type\": \"soft_pop\",\n" +
            "      \"duration\": 500\n" +
            "    },\n" +
            "    \"out\": {\n" +
            "      \"type\": \"dissolve\",\n" +
            "      \"duration\": 500\n" +
            "    }\n" +
            "  },\n" +
            "\n" +
            "  \"warning\": {\n" +
            "    \"type\": \"title\",\n" +
            "    \"color\": \"#FFAA00\",\n" +
            "    \"duration\": 3000,\n" +
            "    \"reveal\": {\n" +
            "      \"type\": \"glyph_scramble\",\n" +
            "      \"speed\": \"fast\"\n" +
            "    },\n" +
            "    \"in\": {\n" +
            "      \"type\": \"soft_pop\",\n" +
            "      \"duration\": 400\n" +
            "    },\n" +
            "    \"idle\": {\n" +
            "      \"type\": \"flicker\",\n" +
            "      \"intensity\": 1.0\n" +
            "    },\n" +
            "    \"out\": {\n" +
            "      \"type\": \"fade_out\",\n" +
            "      \"duration\": 400\n" +
            "    }\n" +
            "  },\n" +
            "\n" +
            "  \"system\": {\n" +
            "    \"type\": \"actionbar\",\n" +
            "    \"color\": \"#55AAFF\",\n" +
            "    \"duration\": 2500,\n" +
            "    \"reveal\": {\n" +
            "      \"type\": \"typewriter\",\n" +
            "      \"speed\": \"fast\"\n" +
            "    },\n" +
            "    \"in\": {\n" +
            "      \"type\": \"fade_in\",\n" +
            "      \"duration\": 200\n" +
            "    },\n" +
            "    \"out\": {\n" +
            "      \"type\": \"fade_out\",\n" +
            "      \"duration\": 300\n" +
            "    }\n" +
            "  },\n" +
            "\n" +
            "  \"achievement\": {\n" +
            "    \"type\": \"title\",\n" +
            "    \"color\": \"#FFFF55\",\n" +
            "    \"gradient\": [\"#FFFF55\", \"#FF55FF\"],\n" +
            "    \"duration\": 3500,\n" +
            "    \"reveal\": {\n" +
            "      \"type\": \"center_out\",\n" +
            "      \"speed\": \"cinematic\"\n" +
            "    },\n" +
            "    \"in\": {\n" +
            "      \"type\": \"soft_pop\",\n" +
            "      \"duration\": 500\n" +
            "    },\n" +
            "    \"idle\": {\n" +
            "      \"type\": \"subtle_pulse\",\n" +
            "      \"intensity\": 1.0\n" +
            "    },\n" +
            "    \"out\": {\n" +
            "      \"type\": \"fade_out\",\n" +
            "      \"duration\": 400\n" +
            "    }\n" +
            "  }\n" +
            "}";

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(defaultJson);
            TitleFxMod.LOGGER.info("Created default_presets_v4.json file.");
        } catch (IOException e) {
            TitleFxMod.LOGGER.error("Failed to write default presets", e);
        }
    }
}
