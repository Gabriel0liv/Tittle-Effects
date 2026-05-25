package com.gabriel.titlefx.common.font;

import com.gabriel.titlefx.TitleFxMod;
import com.gabriel.titlefx.common.network.NetworkHandler;
import com.gabriel.titlefx.common.network.FontRegistrySyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerFontManager {
    private static final File FONTS_DIR = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("titlefx/fonts").toFile();
    private static final Map<String, FontInfo> FONTS = new ConcurrentHashMap<>();
    private static final Map<String, File> FONT_FILES = new ConcurrentHashMap<>();
    private static String registryHash = "";
    private static String serverHash = "";

    public static class ScanResult {
        public String absolutePath = "";
        public boolean directoryExists = false;
        public boolean directoryReadable = false;
        public int rawFileCount = 0;
        public int ttfOtfCount = 0;
        public List<String> allFilesFound = new ArrayList<>();
        public List<String> candidateFontFiles = new ArrayList<>();
        public List<String> registeredFonts = new ArrayList<>();
        public Map<String, String> rejectedFiles = new LinkedHashMap<>();
    }

    public static void init() {
        rescan();
    }

    public static String getFontsDirAbsolutePath() {
        return FONTS_DIR.getAbsolutePath();
    }

    public static synchronized ScanResult rescan() {
        ScanResult result = new ScanResult();
        result.absolutePath = FONTS_DIR.getAbsolutePath();

        FONTS.clear();
        FONT_FILES.clear();
        registryHash = "";

        if (!FONTS_DIR.exists()) {
            FONTS_DIR.mkdirs();
        }

        result.directoryExists = FONTS_DIR.exists();
        result.directoryReadable = FONTS_DIR.canRead();

        File[] files = FONTS_DIR.listFiles();
        if (files != null) {
            result.rawFileCount = files.length;
            for (File file : files) {
                if (file.isDirectory()) continue;
                String fileName = file.getName();
                result.allFilesFound.add(fileName);

                String lower = fileName.toLowerCase(Locale.ROOT);
                if (lower.endsWith(".ttf") || lower.endsWith(".otf")) {
                    result.candidateFontFiles.add(fileName);
                    result.ttfOtfCount++;
                    
                    if (!file.canRead()) {
                        result.rejectedFiles.put(fileName, "Arquivo sem permissão de leitura");
                        continue;
                    }

                    try {
                        if (!file.getCanonicalPath().startsWith(FONTS_DIR.getCanonicalPath())) {
                            TitleFxMod.LOGGER.warn("Skipping file due to path traversal attempt: " + file.getName());
                            result.rejectedFiles.put(file.getName(), "Tentativa de Path Traversal");
                            continue;
                        }
                    } catch (IOException e) {
                        result.rejectedFiles.put(file.getName(), "Erro de I/O ao verificar path canonical");
                        continue;
                    }

                    int dotIdx = fileName.lastIndexOf('.');
                    if (dotIdx == -1) continue;

                    String nameWithoutExtension = fileName.substring(0, dotIdx).toLowerCase(Locale.ROOT);
                    String extension = fileName.substring(dotIdx).toLowerCase(Locale.ROOT);
                    // Sanitization
                    nameWithoutExtension = nameWithoutExtension.replaceAll("[\\s]+", "_");
                    nameWithoutExtension = nameWithoutExtension.replaceAll("[^a-z0-9_\\-]", "");
                    
                    String fontId = "titlefx:" + nameWithoutExtension;

                    if (FONTS.containsKey(fontId)) {
                        result.rejectedFiles.put(fileName, "Colisão de ID lógico (" + fontId + ")");
                        continue;
                    }

                    String sha256 = computeSHA256(file);
                    if (sha256.isEmpty()) {
                        TitleFxMod.LOGGER.error("Failed to calculate SHA-256 for: " + fileName);
                        result.rejectedFiles.put(fileName, "Falha ao calcular SHA-256");
                        continue;
                    }

                    long sizeBytes = file.length();
                    long maxAllowedSize = (long) com.gabriel.titlefx.common.config.TitleFxConfig.COMMON.maxFontFileSizeMb.get() * 1024 * 1024;
                    if (sizeBytes > maxAllowedSize) {
                        String errMsg = "Tamanho excede o limite individual de " + (maxAllowedSize / 1024 / 1024) + "MB (" + (sizeBytes / 1024 / 1024) + "MB)";
                        TitleFxMod.LOGGER.warn("Skipping font " + fileName + " because it exceeds the maximum size limit of " + (maxAllowedSize / 1024 / 1024) + "MB.");
                        result.rejectedFiles.put(fileName, errMsg);
                        continue;
                    }

                    FontInfo info = new FontInfo(fontId, fileName, extension, sizeBytes, sha256);
                    FONTS.put(fontId, info);
                    FONT_FILES.put(fontId, file);
                    result.registeredFonts.add(fileName + " -> " + fontId);
                    TitleFxMod.LOGGER.info("Registered server font: " + fontId + " (" + fileName + ")");
                }
            }
            if (files.length == 0) {
                TitleFxMod.LOGGER.info("Server font directory is empty: " + result.absolutePath);
            }
        } else {
            TitleFxMod.LOGGER.warn("Server font listFiles() returned null for: " + result.absolutePath);
        }

        calculateRegistryHash();
        return result;
    }

    private static void calculateRegistryHash() {
        List<FontInfo> sorted = new ArrayList<>(FONTS.values());
        sorted.sort(Comparator.comparing(FontInfo::fontId));
        StringBuilder sb = new StringBuilder();
        for (FontInfo f : sorted) {
            sb.append(f.fontId()).append(f.sha256()).append(f.sizeBytes());
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            registryHash = hexString.toString();
        } catch (Exception e) {
            registryHash = "";
        }
    }

    public static Collection<String> getRegisteredFontIds() {
        return FONTS.keySet();
    }

    public static FontInfo getFontInfo(String fontId) {
        return FONTS.get(fontId);
    }

    public static File getFontFile(String fontId) {
        return FONT_FILES.get(fontId);
    }

    public static List<FontInfo> getFontsList() {
        return new ArrayList<>(FONTS.values());
    }

    public static String getRegistryHash() {
        return registryHash;
    }

    public static synchronized String getServerHash() {
        if (serverHash.isEmpty()) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            String addressPort = "local_server";
            if (server != null) {
                String ip = server.getLocalIp();
                int port = server.getPort();
                addressPort = (ip == null ? "127.0.0.1" : ip) + ":" + port;
            }
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(addressPort.getBytes(StandardCharsets.UTF_8));
                StringBuilder hexString = new StringBuilder();
                for (byte b : hash) {
                    hexString.append(String.format("%02x", b));
                }
                serverHash = hexString.toString();
            } catch (Exception e) {
                serverHash = "default_server";
            }
        }
        return serverHash;
    }

    public static void onPlayerLoggedIn(ServerPlayer player) {
        syncFontsToPlayer(player);
    }

    public static void syncFontsToPlayer(ServerPlayer player) {
        FontRegistrySyncPacket packet = new FontRegistrySyncPacket(
            getRegistryHash(),
            getServerHash(),
            getFontsList()
        );
        NetworkHandler.sendToPlayer(player, packet);
    }

    public static void broadcastSync() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        FontRegistrySyncPacket packet = new FontRegistrySyncPacket(
            getRegistryHash(),
            getServerHash(),
            getFontsList()
        );
        PlayerList playerList = server.getPlayerList();
        if (playerList != null) {
            for (ServerPlayer player : playerList.getPlayers()) {
                NetworkHandler.sendToPlayer(player, packet);
            }
        }
    }

    public static byte[] readChunk(String fontId, int chunkIndex, int chunkSize) {
        File file = FONT_FILES.get(fontId);
        if (file == null || !file.exists()) return new byte[0];
        
        byte[] buffer = new byte[chunkSize];
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek((long) chunkIndex * chunkSize);
            int read = raf.read(buffer);
            if (read <= 0) return new byte[0];
            if (read < chunkSize) {
                byte[] exact = new byte[read];
                System.arraycopy(buffer, 0, exact, 0, read);
                return exact;
            }
            return buffer;
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private static String computeSHA256(File file) {
        try (InputStream fis = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int count;
            while ((count = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, count);
            }
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
