package com.gabriel.titlefx.client.font;

import com.gabriel.titlefx.TitleFxMod;
import com.gabriel.titlefx.common.font.FontInfo;
import com.gabriel.titlefx.common.network.FontRequestPacket;
import com.gabriel.titlefx.common.network.NetworkHandler;
import com.gabriel.titlefx.common.network.FontChunkPacket;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientFontCache {
    private static final File CACHE_DIR = new File(net.minecraft.client.Minecraft.getInstance().gameDirectory, "titlefx/font_cache");
    private static final File ACTIVE_PACK_DIR = new File(CACHE_DIR, "active/generated_pack");
    
    private static String currentRegistryHash = "";
    private static String currentServerHash = "";
    
    // Queue of FontInfo remaining to download
    private static final Queue<FontInfo> downloadQueue = new ConcurrentLinkedQueue<>();
    private static FontInfo activeDownload = null;
    private static String activeTransferId = "";
    private static ByteArrayOutputStream activeDownloadBuffer = null;
    private static int activeDownloadExpectedChunks = 0;
    private static int activeDownloadReceivedChunks = 0;

    private static final Set<String> activeFonts = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static int retryCount = 0;

    static {
        rebuildActiveFontsList();
    }

    public static void rebuildActiveFontsList() {
        activeFonts.clear();
        File fontDir = new File(ACTIVE_PACK_DIR, "assets/titlefx/font");
        if (fontDir.exists() && fontDir.isDirectory()) {
            File[] files = fontDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File f : files) {
                    String name = f.getName();
                    String baseName = name.substring(0, name.lastIndexOf('.'));
                    activeFonts.add("titlefx:" + baseName);
                }
            }
        }
    }

    public static boolean isFontAvailable(String fontId) {
        if (fontId == null || fontId.trim().isEmpty() || "minecraft:default".equals(fontId)) {
            return true;
        }
        return activeFonts.contains(fontId);
    }

    private static void retryActiveFont() {
        if (activeDownload == null) return;
        if (retryCount >= 3) {
            TitleFxMod.LOGGER.error("Failed to download font " + activeDownload.fontId() + " after 3 attempts. Skipping.");
            requestNextFont();
            return;
        }
        retryCount++;
        activeTransferId = UUID.randomUUID().toString();
        activeDownloadBuffer = new ByteArrayOutputStream();
        activeDownloadExpectedChunks = 0;
        activeDownloadReceivedChunks = 0;

        TitleFxMod.LOGGER.info("Retrying font download: " + activeDownload.fontId() + " (" + activeDownload.originalName() + "). Attempt: " + retryCount);
        FontRequestPacket req = new FontRequestPacket(activeDownload.fontId(), activeTransferId, activeDownload.sha256());
        NetworkHandler.CHANNEL.sendToServer(req);
    }

    public static synchronized void handleRegistrySync(String registryHash, String serverHash, List<FontInfo> fonts) {
        TitleFxMod.LOGGER.info("Received FontRegistrySyncPacket. Registry Hash: " + registryHash + ", Server Hash: " + serverHash);
        
        currentServerHash = serverHash;
        File serverPackDir = new File(CACHE_DIR, serverHash + "/generated_pack");
        
        // 1. Check if registry hash matches and active pack is already complete
        if (currentRegistryHash.equals(registryHash) && isPackComplete(fonts)) {
            TitleFxMod.LOGGER.info("Registry hash matches and active pack is complete. Skipping font download.");
            return;
        }

        // 2. Stale cleanup
        cleanupStaleFonts(serverHash, fonts);

        // 3. Populate download queue
        downloadQueue.clear();
        activeDownload = null;
        activeTransferId = "";
        activeDownloadBuffer = null;

        for (FontInfo info : fonts) {
            if (!isFontCached(serverHash, info)) {
                downloadQueue.add(info);
            }
        }

        if (downloadQueue.isEmpty()) {
            TitleFxMod.LOGGER.info("All fonts are already cached. Ensuring pack is active.");
            try {
                ensureDirectories(serverHash);
                generatePackMetadata(serverHash);
                generateProviderJsons(serverHash, fonts);
                mirrorToActive(serverHash);
                triggerResourceReload();
            } catch (Exception e) {
                TitleFxMod.LOGGER.error("Failed to update resource pack", e);
            }
            currentRegistryHash = registryHash;
        } else {
            currentRegistryHash = registryHash;
            TitleFxMod.LOGGER.info("Need to download " + downloadQueue.size() + " fonts. Starting queue...");
            requestNextFont();
        }
    }

    private static boolean isPackComplete(List<FontInfo> expectedFonts) {
        if (!ACTIVE_PACK_DIR.exists()) return false;
        File metadata = new File(ACTIVE_PACK_DIR, "pack.mcmeta");
        if (!metadata.exists()) return false;

        File activeFontDir = new File(ACTIVE_PACK_DIR, "assets/titlefx/font");
        if (!activeFontDir.exists()) return false;

        for (FontInfo info : expectedFonts) {
            String fontName = getFontFileName(info);
            File fontFile = new File(activeFontDir, fontName);
            File jsonFile = new File(activeFontDir, info.fontId().replace("titlefx:", "") + ".json");
            if (!fontFile.exists() || !jsonFile.exists()) {
                return false;
            }
        }
        return true;
    }

    private static void cleanupStaleFonts(String serverHash, List<FontInfo> activeFonts) {
        Set<String> activeIds = new HashSet<>();
        for (FontInfo f : activeFonts) {
            activeIds.add(f.fontId());
        }

        File serverFontDir = new File(CACHE_DIR, serverHash + "/generated_pack/assets/titlefx/font");
        File activeFontDir = new File(ACTIVE_PACK_DIR, "assets/titlefx/font");

        cleanupFolder(serverFontDir, activeIds);
        cleanupFolder(activeFontDir, activeIds);
    }

    private static void cleanupFolder(File folder, Set<String> activeIds) {
        if (!folder.exists() || !folder.isDirectory()) return;
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            String name = file.getName();
            int dotIdx = name.lastIndexOf('.');
            if (dotIdx == -1) continue;
            String baseName = name.substring(0, dotIdx);
            String logicalId = "titlefx:" + baseName;

            if (!activeIds.contains(logicalId)) {
                TitleFxMod.LOGGER.info("Cleaning up stale font file: " + file.getAbsolutePath());
                file.delete();
            }
        }
    }

    private static boolean isFontCached(String serverHash, FontInfo info) {
        File fontDir = new File(CACHE_DIR, serverHash + "/generated_pack/assets/titlefx/font");
        String fileName = getFontFileName(info);
        File fontFile = new File(fontDir, fileName);
        if (!fontFile.exists()) return false;
        
        // Verify size
        if (fontFile.length() != info.sizeBytes()) return false;
        
        // Verify checksum
        String sha = computeFileSHA256(fontFile);
        return sha.equals(info.sha256());
    }

    private static void requestNextFont() {
        activeDownload = downloadQueue.poll();
        if (activeDownload == null) {
            TitleFxMod.LOGGER.info("Font downloads completed successfully!");
            try {
                generatePackMetadata(currentServerHash);
                mirrorToActive(currentServerHash);
                triggerResourceReload();
            } catch (Exception e) {
                TitleFxMod.LOGGER.error("Error finalizing resource pack mirroring", e);
            }
            return;
        }

        activeTransferId = UUID.randomUUID().toString();
        activeDownloadBuffer = new ByteArrayOutputStream();
        activeDownloadExpectedChunks = 0;
        activeDownloadReceivedChunks = 0;
        retryCount = 0;

        TitleFxMod.LOGGER.info("Requesting font: " + activeDownload.fontId() + " (" + activeDownload.originalName() + ")");
        FontRequestPacket req = new FontRequestPacket(activeDownload.fontId(), activeTransferId, activeDownload.sha256());
        NetworkHandler.CHANNEL.sendToServer(req);
    }

    public static synchronized void handleFontChunk(String transferId, String fontId, String sha256, int chunkIndex, int totalChunks, byte[] data) {
        if (activeDownload == null || !activeTransferId.equals(transferId)) {
            // Ignore mismatched or stale transfer chunks
            return;
        }

        if (chunkIndex != activeDownloadReceivedChunks) {
            TitleFxMod.LOGGER.warn("Received out-of-order chunk " + chunkIndex + ", expected: " + activeDownloadReceivedChunks + ". Restarting download.");
            retryActiveFont();
            return;
        }

        // Limit validation: max allowed file size (from config)
        long currentBytes = activeDownloadBuffer.size();
        long maxLimitBytes = (long) com.gabriel.titlefx.common.config.TitleFxConfig.COMMON.maxTotalFontSyncMb.get() * 1024 * 1024;
        if (currentBytes + data.length > maxLimitBytes) {
            TitleFxMod.LOGGER.error("Font transfer exceeded client sync size limit of " + (maxLimitBytes / 1024 / 1024) + "MB.");
            activeDownload = null;
            return;
        }

        try {
            activeDownloadBuffer.write(data);
            activeDownloadReceivedChunks++;
            activeDownloadExpectedChunks = totalChunks;

            if (activeDownloadReceivedChunks >= totalChunks) {
                // Completed!
                byte[] assembledBytes = activeDownloadBuffer.toByteArray();
                String assembledSha = computeBytesSHA256(assembledBytes);

                if (!assembledSha.equals(activeDownload.sha256())) {
                    TitleFxMod.LOGGER.error("Checksum mismatch for downloaded font: " + fontId + ". Retrying...");
                    retryActiveFont();
                    return;
                }

                saveDownloadedFont(assembledBytes);
                requestNextFont();
            }
        } catch (IOException e) {
            TitleFxMod.LOGGER.error("Error writing chunk bytes", e);
            retryActiveFont();
        }
    }

    private static void saveDownloadedFont(byte[] bytes) {
        try {
            ensureDirectories(currentServerHash);
            
            File fontDir = new File(CACHE_DIR, currentServerHash + "/generated_pack/assets/titlefx/font");
            String fileName = getFontFileName(activeDownload);
            File dest = new File(fontDir, fileName);

            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(bytes);
            }
            TitleFxMod.LOGGER.info("Saved font file: " + dest.getAbsolutePath());

            // Generate mapping JSON provider file
            generateProviderJson(currentServerHash, activeDownload);

        } catch (Exception e) {
            TitleFxMod.LOGGER.error("Failed to save downloaded font", e);
        }
    }

    private static void ensureDirectories(String serverHash) {
        File serverFontDir = new File(CACHE_DIR, serverHash + "/generated_pack/assets/titlefx/font");
        if (!serverFontDir.exists()) {
            serverFontDir.mkdirs();
        }
    }

    private static void generatePackMetadata(String serverHash) throws IOException {
        File serverPackDir = new File(CACHE_DIR, serverHash + "/generated_pack");
        File metadataFile = new File(serverPackDir, "pack.mcmeta");
        
        String metaJson = "{\n" +
                "  \"pack\": {\n" +
                "    \"pack_format\": 15,\n" +
                "    \"description\": \"TitleFX Server Fonts\"\n" +
                "  }\n" +
                "}";
                
        try (FileOutputStream fos = new FileOutputStream(metadataFile)) {
            fos.write(metaJson.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void generateProviderJsons(String serverHash, List<FontInfo> fonts) throws IOException {
        for (FontInfo info : fonts) {
            generateProviderJson(serverHash, info);
        }
    }

    private static void generateProviderJson(String serverHash, FontInfo info) throws IOException {
        File fontDir = new File(CACHE_DIR, serverHash + "/generated_pack/assets/titlefx/font");
        String nameWithoutPrefix = info.fontId().replace("titlefx:", "");
        File providerFile = new File(fontDir, nameWithoutPrefix + ".json");

        String fileName = getFontFileName(info);
        String json = "{\n" +
                "  \"providers\": [\n" +
                "    {\n" +
                "      \"type\": \"ttf\",\n" +
                "      \"file\": \"titlefx:font/" + fileName + "\",\n" +
                "      \"shift\": [0, 0],\n" +
                "      \"size\": 11.0,\n" +
                "      \"oversample\": 1.5\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        try (FileOutputStream fos = new FileOutputStream(providerFile)) {
            fos.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void mirrorToActive(String serverHash) throws IOException {
        File serverPackDir = new File(CACHE_DIR, serverHash + "/generated_pack");
        if (!serverPackDir.exists()) return;

        if (ACTIVE_PACK_DIR.exists()) {
            deleteDirectory(ACTIVE_PACK_DIR);
        }
        ACTIVE_PACK_DIR.mkdirs();
        copyDirectory(serverPackDir, ACTIVE_PACK_DIR);
        TitleFxMod.LOGGER.info("Mirrored resource pack to active directory.");
        rebuildActiveFontsList();
    }

    private static void triggerResourceReload() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            TitleFxMod.LOGGER.info("Triggering resource pack reload...");
            mc.reloadResourcePacks();
        });
    }

    private static String getFontFileName(FontInfo info) {
        String lowerName = info.originalName().toLowerCase(Locale.ROOT);
        String ext = lowerName.endsWith(".otf") ? ".otf" : ".ttf";
        return info.fontId().replace("titlefx:", "") + ext;
    }

    private static void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                deleteDirectory(f);
            }
        }
        dir.delete();
    }

    private static void copyDirectory(File source, File destination) throws IOException {
        if (source.isDirectory()) {
            if (!destination.exists()) {
                destination.mkdirs();
            }
            String[] files = source.list();
            if (files != null) {
                for (String file : files) {
                    File srcFile = new File(source, file);
                    File destFile = new File(destination, file);
                    copyDirectory(srcFile, destFile);
                }
            }
        } else {
            try (InputStream in = new FileInputStream(source);
                 OutputStream out = new FileOutputStream(destination)) {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            }
        }
    }

    private static String computeFileSHA256(File file) {
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

    private static String computeBytesSHA256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
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
