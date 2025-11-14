package com.filesharingapp.core;

import com.filesharingapp.utils.LoggerUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TransferContext
 * ---------------
 * Baby-English:
 * - This is a tiny "memory box" inside the JVM.
 */
public final class TransferContext {

    // ================================
    // 🔑 Core transfer identity
    // ================================
    private static volatile String currentTransferId;
    private static volatile String incomingName;
    private static volatile String expectedChecksum;
    private static volatile String activeMethod;
    private static volatile String lastSenderIp;

    // ================================
    // 🔁 Resume / path / state info
    // ================================
    private static volatile long resumeOffsetBytes;
    private static volatile String tempFolderPath;
    private static volatile int totalChunks;
    private static volatile int completedChunks;
    private static volatile String finalOutputPath;
    private static volatile String zeroTierPeerIp;
    private static volatile String zeroTierNetworkId;
    private static volatile String s3DownloadLocation;
    private static volatile boolean encryptionEnabled;
    private static volatile String aesKeyFingerprint;

    // ================================
    // 📊 Progress tracking
    // ================================
    public static final class Progress {
        public final long totalBytes;
        public volatile long receivedBytes;
        public volatile int totalChunks;
        public volatile int completedChunks;

        public Progress(long totalBytes) {
            this.totalBytes = totalBytes;
        }
    }

    private static final Map<String, Progress> PROGRESS_MAP = new ConcurrentHashMap<>();

    // ================================
    // 🧩 Chunk state for safe merge
    // ================================
    private static final class ChunkState {
        final boolean[] received;
        final Object mergeLock = new Object();

        ChunkState(int totalChunks) {
            this.received = new boolean[totalChunks];
        }
    }

    private static final Map<String, ChunkState> CHUNK_STATE_MAP = new ConcurrentHashMap<>();

    private TransferContext() {}

    // ============================================================
    // 🆔 Transfer ID helpers
    // ============================================================
    public static synchronized String generateNewTransferId(String fileName, String checksum) {
        String base = (fileName != null ? fileName : "file");
        String shortHash = (checksum != null && checksum.length() >= 8)
                ? checksum.substring(0, 8)
                : "nohash";
        long now = Instant.now().toEpochMilli();
        String id = base.replaceAll("[^A-Za-z0-9._-]", "_") + "-" + shortHash + "-" + now;

        currentTransferId = id;
        LoggerUtil.info("[Context] Generated transferId: " + id);
        return id;
    }

    public static String getCurrentTransferId() {
        return currentTransferId;
    }

    // ============================================================
    // 🧾 Metadata setters/getters
    // ============================================================
    public static void setIncomingName(String name) { incomingName = name; }
    public static String getIncomingName() { return incomingName; }

    public static void setExpectedChecksum(String checksum) { expectedChecksum = checksum; }
    public static String getExpectedChecksum() { return expectedChecksum; }

    public static void setActiveMethod(String method) { activeMethod = method; }
    public static String getActiveMethod() { return activeMethod; }

    public static void setLastSenderIp(String ip) { lastSenderIp = ip; }
    public static String getLastSenderIp() { return lastSenderIp; }

    public static void setResumeOffsetBytes(long offset) { resumeOffsetBytes = Math.max(offset, 0); }
    public static long getResumeOffsetBytes() { return resumeOffsetBytes; }

    public static void setTempFolderPath(String path) { tempFolderPath = path; }
    public static String getTempFolderPath() { return tempFolderPath; }

    public static void setTotalChunks(int count) { totalChunks = Math.max(count, 0); }
    public static int getTotalChunks() { return totalChunks; }

    public static void setCompletedChunks(int count) { completedChunks = Math.max(count, 0); }
    public static int getCompletedChunks() { return completedChunks; }

    public static void setFinalOutputPath(String path) { finalOutputPath = path; }
    public static String getFinalOutputPath() { return finalOutputPath; }

    public static void setZeroTierPeerIp(String ip) { zeroTierPeerIp = ip; }
    public static String getZeroTierPeerIp() { return zeroTierPeerIp; }

    public static void setZeroTierNetworkId(String id) { zeroTierNetworkId = id; }
    public static String getZeroTierNetworkId() { return zeroTierNetworkId; }

    public static void setS3DownloadLocation(String path) { s3DownloadLocation = path; }
    public static String getS3DownloadLocation() { return s3DownloadLocation; }

    // FIX: Added missing getter for AES Password (used by handlers)
    public static String getAesPassword() { return aesKeyFingerprint; }

    public static void setEncryptionEnabled(boolean enabled) { encryptionEnabled = enabled; }
    public static boolean isEncryptionEnabled() { return encryptionEnabled; }

    public static void setAesKeyFingerprint(String fingerprint) { aesKeyFingerprint = fingerprint; }
    public static String getAesKeyFingerprint() { return aesKeyFingerprint; }

    // ============================================================
    // 📊 Progress helpers
    // ============================================================
    public static Progress getOrCreateProgress(String transferId, long totalBytes) {
        return PROGRESS_MAP.computeIfAbsent(transferId, k -> {
            Progress p = new Progress(totalBytes);
            p.totalChunks = totalChunks;
            return p;
        });
    }

    public static void addReceivedBytes(String transferId, long delta) {
        Progress p = PROGRESS_MAP.get(transferId);
        if (p != null) p.receivedBytes = Math.max(p.receivedBytes + delta, 0);
    }

    public static Progress getProgress(String transferId) { return PROGRESS_MAP.get(transferId); }
    public static void clearProgress(String transferId) { PROGRESS_MAP.remove(transferId); }

    // ============================================================
    // 🧩 Chunk tracking
    // ============================================================
    public static synchronized void initChunkState(String transferId, int chunkCount) {
        if (transferId == null || chunkCount <= 0) return;
        CHUNK_STATE_MAP.put(transferId, new ChunkState(chunkCount));
        setTotalChunks(chunkCount);
        setCompletedChunks(0);
    }

    public static void markChunkReceived(String transferId, int index) {
        ChunkState state = CHUNK_STATE_MAP.get(transferId);
        if (state == null || index < 0 || index >= state.received.length) return;
        synchronized (state) {
            if (!state.received[index]) {
                state.received[index] = true;
                completedChunks++;
                Progress p = PROGRESS_MAP.get(transferId);
                if (p != null) p.completedChunks = completedChunks;
            }
        }
    }

    public static boolean areAllChunksReceived(String transferId) {
        ChunkState state = CHUNK_STATE_MAP.get(transferId);
        if (state == null) return false;
        synchronized (state) {
            for (boolean b : state.received) if (!b) return false;
            return true;
        }
    }

    public static Object getMergeLock(String transferId) {
        ChunkState state = CHUNK_STATE_MAP.get(transferId);
        return (state != null) ? state.mergeLock : null;
    }

    // ============================================================
    // 🧹 Cleanup helpers
    // ============================================================
    public static void deletePartialFile(String pathString) {
        if (pathString == null || pathString.isBlank()) return;
        try {
            Path p = Path.of(pathString);
            if (Files.exists(p)) Files.delete(p);
        } catch (IOException e) {
            LoggerUtil.warn("[Context] Could not delete partial file: " + pathString);
        }
    }

    public static void deleteKnownFinalOutput() {
        if (finalOutputPath != null) deletePartialFile(finalOutputPath);
    }

    public static synchronized void clearTransfer(String transferId) {
        PROGRESS_MAP.remove(transferId);
        CHUNK_STATE_MAP.remove(transferId);
    }

    public static synchronized void clearAll() {
        currentTransferId = null;
        incomingName = null;
        expectedChecksum = null;
        activeMethod = null;
        lastSenderIp = null;
        resumeOffsetBytes = 0;
        tempFolderPath = null;
        totalChunks = 0;
        completedChunks = 0;
        finalOutputPath = null;
        zeroTierPeerIp = null;
        zeroTierNetworkId = null;
        s3DownloadLocation = null;
        encryptionEnabled = false;
        aesKeyFingerprint = null;
        PROGRESS_MAP.clear();
        CHUNK_STATE_MAP.clear();
    }
}