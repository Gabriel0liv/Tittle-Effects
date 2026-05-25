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

        File defaultFile = new File(presetsDir, "default_presets_v2.json");
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

        // Sort files to ensure default_presets_v2.json loads last and overrides older presets
        java.util.Arrays.sort(files, (f1, f2) -> {
            if ("default_presets_v2.json".equals(f1.getName())) return 1;
            if ("default_presets_v2.json".equals(f2.getName())) return -1;
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
            "    \"font\": \"minecraft:default\",\n" +
            "    \"color\": \"#AA0000\",\n" +
            "    \"gradient\": [\"#FF0000\", \"#550000\"],\n" +
            "    \"duration\": 5000,\n" +
            "    \"reveal\": {\n" +
            "      \"type\": \"obfuscated_decode\",\n" +
            "      \"duration\": 1200,\n" +
            "      \"lockMode\": \"left_to_right\",\n" +
            "      \"flickerSpeed\": 2\n" +
            "    },\n" +
            "    \"in\": {\n" +
            "      \"type\": \"zoom_in\",\n" +
            "      \"duration\": 500,\n" +
            "      \"easing\": \"ease_out_back\"\n" +
            "    },\n" +
            "    \"idle\": {\n" +
            "      \"type\": \"shake\",\n" +
            "      \"intensity\": 1.5\n" +
            "    },\n" +
            "    \"out\": {\n" +
            "      \"type\": \"fade_out\",\n" +
            "      \"duration\": 500\n" +
            "    },\n" +
            "    \"subtitle\": {\n" +
            "      \"type\": \"subtitle\",\n" +
            "      \"text\": \"Prepare-se!\",\n" +
            "      \"font\": \"minecraft:default\",\n" +
            "      \"color\": \"#FFFFFF\",\n" +
            "      \"reveal\": {\n" +
            "        \"type\": \"typewriter\",\n" +
            "        \"duration\": 1000\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "\n" +
            "  \"quest_start\": {\n" +
            "    \"type\": \"title\",\n" +
            "    \"font\": \"minecraft:default\",\n" +
            "    \"color\": \"#FFD56A\",\n" +
            "    \"duration\": 4000,\n" +
            "    \"reveal\": {\n" +
            "      \"type\": \"word_by_word\",\n" +
            "      \"duration\": 1000\n" +
            "    },\n" +
            "    \"in\": {\n" +
            "      \"type\": \"fade_in\",\n" +
            "      \"duration\": 400\n" +
            "    },\n" +
            "    \"idle\": {\n" +
            "      \"type\": \"pulse\",\n" +
            "      \"intensity\": 0.06\n" +
            "    },\n" +
            "    \"out\": {\n" +
            "      \"type\": \"fade_out\",\n" +
            "      \"duration\": 400\n" +
            "    }\n" +
            "  },\n" +
            "\n" +
            "  \"warning\": {\n" +
            "    \"type\": \"title\",\n" +
            "    \"font\": \"minecraft:default\",\n" +
            "    \"color\": \"#FFAA00\",\n" +
            "    \"duration\": 3000,\n" +
            "    \"reveal\": {\n" +
            "      \"type\": \"typewriter\",\n" +
            "      \"duration\": 800\n" +
            "    },\n" +
            "    \"in\": {\n" +
            "      \"type\": \"zoom_in\",\n" +
            "      \"duration\": 400\n" +
            "    },\n" +
            "    \"idle\": {\n" +
            "      \"type\": \"pulse\",\n" +
            "      \"intensity\": 0.1\n" +
            "    },\n" +
            "    \"out\": {\n" +
            "      \"type\": \"fade_out\",\n" +
            "      \"duration\": 400\n" +
            "    }\n" +
            "  },\n" +
            "\n" +
            "  \"system\": {\n" +
            "    \"type\": \"actionbar\",\n" +
            "    \"font\": \"minecraft:default\",\n" +
            "    \"color\": \"#55AAFF\",\n" +
            "    \"duration\": 2500,\n" +
            "    \"reveal\": {\n" +
            "      \"type\": \"glyph_scramble\",\n" +
            "      \"duration\": 900,\n" +
            "      \"lockMode\": \"random\",\n" +
            "      \"charset\": \"safe\",\n" +
            "      \"preserveSpaces\": true,\n" +
            "      \"preserveCase\": true\n" +
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
            "    \"font\": \"minecraft:default\",\n" +
            "    \"color\": \"#FFFF55\",\n" +
            "    \"gradient\": [\"#FFFF55\", \"#FF55FF\"],\n" +
            "    \"duration\": 4500,\n" +
            "    \"in\": {\n" +
            "      \"type\": \"zoom_in\",\n" +
            "      \"duration\": 600,\n" +
            "      \"easing\": \"ease_out_back\"\n" +
            "    },\n" +
            "    \"idle\": {\n" +
            "      \"type\": \"pulse\",\n" +
            "      \"intensity\": 0.05\n" +
            "    },\n" +
            "    \"out\": {\n" +
            "      \"type\": \"fade_out\",\n" +
            "      \"duration\": 500\n" +
            "    },\n" +
            "    \"subtitle\": {\n" +
            "      \"type\": \"subtitle\",\n" +
            "      \"font\": \"minecraft:default\",\n" +
            "      \"color\": \"#55FF55\"\n" +
            "    }\n" +
            "  },\n" +
            "\n" +
            "  \"location_intro\": {\n" +
            "    \"type\": \"title\",\n" +
            "    \"font\": \"minecraft:default\",\n" +
            "    \"color\": \"#55FF55\",\n" +
            "    \"duration\": 5000,\n" +
            "    \"in\": {\n" +
            "      \"type\": \"fade_in\",\n" +
            "      \"duration\": 1000\n" +
            "    },\n" +
            "    \"out\": {\n" +
            "      \"type\": \"fade_out\",\n" +
            "      \"duration\": 1000\n" +
            "    },\n" +
            "    \"subtitle\": {\n" +
            "      \"type\": \"subtitle\",\n" +
            "      \"font\": \"minecraft:default\",\n" +
            "      \"color\": \"#AAAAAA\"\n" +
            "    }\n" +
            "  },\n" +
            "\n" +
            "  \"danger\": {\n" +
            "    \"type\": \"title\",\n" +
            "    \"font\": \"minecraft:default\",\n" +
            "    \"color\": \"#FF5555\",\n" +
            "    \"duration\": 3000,\n" +
            "    \"reveal\": {\n" +
            "      \"type\": \"glyph_scramble\",\n" +
            "      \"duration\": 500\n" +
            "    },\n" +
            "    \"in\": {\n" +
            "      \"type\": \"zoom_in\",\n" +
            "      \"duration\": 300\n" +
            "    },\n" +
            "    \"idle\": {\n" +
            "      \"type\": \"shake\",\n" +
            "      \"intensity\": 2.0\n" +
            "    },\n" +
            "    \"out\": {\n" +
            "      \"type\": \"fade_out\",\n" +
            "      \"duration\": 300\n" +
            "    }\n" +
            "  },\n" +
            "\n" +
            "  \"success\": {\n" +
            "    \"type\": \"title\",\n" +
            "    \"font\": \"minecraft:default\",\n" +
            "    \"color\": \"#55FF55\",\n" +
            "    \"duration\": 3500,\n" +
            "    \"in\": {\n" +
            "      \"type\": \"zoom_in\",\n" +
            "      \"duration\": 400\n" +
            "    },\n" +
            "    \"idle\": {\n" +
            "      \"type\": \"pulse\",\n" +
            "      \"intensity\": 0.08\n" +
            "    },\n" +
            "    \"out\": {\n" +
            "      \"type\": \"fade_out\",\n" +
            "      \"duration\": 400\n" +
            "    }\n" +
            "  },\n" +
            "\n" +
            "  \"error\": {\n" +
            "    \"type\": \"title\",\n" +
            "    \"font\": \"minecraft:default\",\n" +
            "    \"color\": \"#FF5555\",\n" +
            "    \"duration\": 3500,\n" +
            "    \"in\": {\n" +
            "      \"type\": \"zoom_in\",\n" +
            "      \"duration\": 300\n" +
            "    },\n" +
            "    \"idle\": {\n" +
            "      \"type\": \"shake\",\n" +
            "      \"intensity\": 1.2\n" +
            "    },\n" +
            "    \"out\": {\n" +
            "      \"type\": \"fade_out\",\n" +
            "      \"duration\": 300\n" +
            "    }\n" +
            "  },\n" +
            "\n" +
            "  \"info\": {\n" +
            "    \"type\": \"actionbar\",\n" +
            "    \"font\": \"minecraft:default\",\n" +
            "    \"color\": \"#FFFFCC\",\n" +
            "    \"duration\": 3000,\n" +
            "    \"reveal\": {\n" +
            "      \"type\": \"typewriter\",\n" +
            "      \"duration\": 500\n" +
            "    },\n" +
            "    \"in\": {\n" +
            "      \"type\": \"fade_in\",\n" +
            "      \"duration\": 200\n" +
            "    },\n" +
            "    \"out\": {\n" +
            "      \"type\": \"fade_out\",\n" +
            "      \"duration\": 300\n" +
            "    }\n" +
            "  }\n" +
            "}";

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(defaultJson);
            TitleFxMod.LOGGER.info("Created default_presets_v2.json file.");
        } catch (IOException e) {
            TitleFxMod.LOGGER.error("Failed to write default presets", e);
        }
    }
}
