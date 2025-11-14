package com.filesharingapp.server;

import com.filesharingapp.utils.LoggerUtil;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * TransferStatusRegistry
 * ----------------------
 * Baby English:
 *   - This class remembers "how is my transfer doing right now?"
 *   - Server updates this during /upload.
 *   - /status servlet reads from here and sends JSON to the browser.
 *
 * Important notes:
 *   - We track ONE active transfer at a time (the latest one).
 *   - This is enough for your current app and UI.
 *   - If you ever want many transfers at once, you can wrap
 *     this logic in a Map<transferId, TransferStatusRegistry>.
 */
public final class TransferStatusRegistry {

    /**
     * State of the transfer.
     * Baby English:
     *   - IDLE       → nothing is happening.
     *   - IN_PROGRESS→ upload or download is running.
     *   - COMPLETED  → file finished OK.
     *   - FAILED     → something bad happened.
     */
    public enum State {
        IDLE,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }

    // ------------------------------
    // Core transfer fields (existing)
    // ------------------------------
    private static String transferId;
    private static String fileName;
    private static long   totalBytes;
    private static long   bytesWritten;
    private static State  state        = State.IDLE;
    private static String errorMessage;
    private static LocalDateTime lastUpdated;

    // ------------------------------
    // New meta fields (as per analysis)
    // ------------------------------
    /** HTTP / S3 / ZEROTIER */
    private static String protocol;

    /** Friendly user name, if known. */
    private static String userName;

    /** Expected checksum (SHA-256) for integrity. */
    private static String checksum;

    /** Where to resume from (byte offset). */
    private static long resumeOffset;

    /** Full path of final saved file (optional). */
    private static String filePath;

    /** True if AES encryption is enabled for this transfer. */
    private static boolean aesEnabled;

    /** When transfer started (for speed/ETA). */
    private static long startedAtMs;

    /** Last update timestamp in milliseconds. */
    private static long lastUpdatedMs;

    private TransferStatusRegistry() {
        // Utility class — no objects.
    }

    // ============================
    // Start / Progress / Complete
    // ============================

    /**
     * Begin a new transfer.
     * Baby English:
     *   - We reset everything.
     *   - We remember id, file name, and total size.
     */
    public static synchronized void begin(String id, String name, long total) {
        transferId   = safe(id);
        fileName     = safe(name);
        totalBytes   = Math.max(total, 0);
        bytesWritten = 0L;
        state        = State.IN_PROGRESS;
        errorMessage = null;
        protocol     = null;
        checksum     = null;
        resumeOffset = 0L;
        filePath     = null;
        aesEnabled   = false;
        startedAtMs  = System.currentTimeMillis();
        lastUpdatedMs = startedAtMs;
        lastUpdated   = LocalDateTime.now();
        LoggerUtil.info("[Status] Begin transfer: " + transferId
                + " (" + fileName + "), totalBytes=" + totalBytes);
    }

    /**
     * Set which protocol we are using.
     * Example: "HTTP", "S3", "ZEROTIER".
     */
    public static synchronized void setProtocol(String proto) {
        protocol = safe(proto);
    }

    /**
     * Set display name of user (sender or receiver).
     */
    public static synchronized void setUserName(String name) {
        userName = safe(name);
    }

    /**
     * Set expected checksum string.
     */
    public static synchronized void setChecksum(String cs) {
        checksum = safe(cs);
    }

    /**
     * Set resume offset in bytes.
     * This is where next upload chunk should start.
     */
    public static synchronized void setResumeOffset(long offset) {
        resumeOffset = Math.max(offset, 0L);
    }

    /**
     * Save final file path for reference.
     */
    public static synchronized void setFilePath(String path) {
        filePath = safe(path);
    }

    /**
     * Mark whether AES encryption is enabled.
     */
    public static synchronized void setAesEnabled(boolean enabled) {
        aesEnabled = enabled;
    }

    /**
     * Update bytesWritten to a new absolute value.
     * Baby English:
     *   - newBytesWritten is "how many bytes total we have now".
     */
    public static synchronized void progress(long newBytesWritten) {
        bytesWritten = Math.max(newBytesWritten, 0L);
        lastUpdated  = LocalDateTime.now();
        lastUpdatedMs = System.currentTimeMillis();
    }

    /**
     * Add a delta of bytes to current bytesWritten.
     * Baby English:
     *   - We use this when we only know "we wrote X more bytes".
     */
    public static synchronized void addBytes(long delta) {
        if (delta < 0) {
            return;
        }
        bytesWritten += delta;
        if (bytesWritten < 0) {
            bytesWritten = 0;
        }
        lastUpdated   = LocalDateTime.now();
        lastUpdatedMs = System.currentTimeMillis();

        // If we know total size and reached it, we can auto-complete.
        if (totalBytes > 0 && bytesWritten >= totalBytes && state == State.IN_PROGRESS) {
            state = State.COMPLETED;
            LoggerUtil.success("[Status] Transfer reached total bytes. Marking as COMPLETED.");
        }
    }

    /**
     * Mark transfer as completed.
     * Optionally set final file path.
     */
    public static synchronized void complete(String finalPath) {
        state         = State.COMPLETED;
        lastUpdated   = LocalDateTime.now();
        lastUpdatedMs = System.currentTimeMillis();
        if (finalPath != null) {
            filePath = safe(finalPath);
        }
        LoggerUtil.success("[Status] Transfer completed: " + fileName);
    }

    /**
     * Mark transfer as failed with error message.
     */
    public static synchronized void fail(String msg) {
        state         = State.FAILED;
        errorMessage  = safe(msg);
        lastUpdated   = LocalDateTime.now();
        lastUpdatedMs = System.currentTimeMillis();
        LoggerUtil.error("[Status] Transfer failed: " + msg);
    }

    /**
     * Reset everything back to IDLE.
     */
    public static synchronized void reset() {
        transferId    = null;
        fileName      = null;
        totalBytes    = 0L;
        bytesWritten  = 0L;
        state         = State.IDLE;
        errorMessage  = null;
        protocol      = null;
        userName      = null;
        checksum      = null;
        resumeOffset  = 0L;
        filePath      = null;
        aesEnabled    = false;
        startedAtMs   = 0L;
        lastUpdatedMs = 0L;
        lastUpdated   = LocalDateTime.now();
        LoggerUtil.info("[Status] Reset transfer status registry.");
    }

    // =============
    // Public getters
    // =============

    public static synchronized long getBytesWritten() {
        return bytesWritten;
    }

    public static synchronized long getTotalBytes() {
        return totalBytes;
    }

    public static synchronized long getResumeOffset() {
        return resumeOffset;
    }

    public static synchronized String getTransferId() {
        return transferId;
    }

    public static synchronized String getFileName() {
        return fileName;
    }

    // ============================
    // JSON builder for /status API
    // ============================

    /**
     * Build small JSON string for /status response.
     * Baby English:
     *   - We do not use any JSON library.
     *   - We just build a text string carefully.
     */
    public static synchronized String toJson() {
        // Compute progressPercent, speed, and ETA safely.
        double percent = 0.0;
        if (totalBytes > 0) {
            percent = (bytesWritten * 100.0) / totalBytes;
        }

        long   now     = System.currentTimeMillis();
        long   elapsed = (startedAtMs > 0) ? Math.max(now - startedAtMs, 1L) : 1L; // ms
        double speedBps = (bytesWritten * 1000.0) / elapsed; // bytes per second

        long remainingBytes = (totalBytes > 0 && bytesWritten <= totalBytes)
                ? (totalBytes - bytesWritten)
                : 0L;

        long etaSeconds = (speedBps > 1.0 && remainingBytes > 0)
                ? (long) (remainingBytes / speedBps)
                : 0L;

        String lastUpdatedStr = (lastUpdated != null)
                ? lastUpdated.toString()
                : "";

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"transferId\":\"").append(safe(transferId)).append("\",");
        sb.append("\"fileName\":\"").append(safe(fileName)).append("\",");
        sb.append("\"protocol\":\"").append(safe(protocol)).append("\",");
        sb.append("\"userName\":\"").append(safe(userName)).append("\",");
        sb.append("\"totalBytes\":").append(totalBytes).append(",");
        sb.append("\"bytesWritten\":").append(bytesWritten).append(",");
        sb.append("\"progressPercent\":").append(String.format("%.2f", percent)).append(",");
        sb.append("\"resumeOffset\":").append(resumeOffset).append(",");
        sb.append("\"state\":\"").append(state.name()).append("\",");
        sb.append("\"error\":\"").append(safe(errorMessage)).append("\",");
        sb.append("\"checksum\":\"").append(safe(checksum)).append("\",");
        sb.append("\"aesEnabled\":").append(aesEnabled).append(",");
        sb.append("\"filePath\":\"").append(safe(filePath)).append("\",");
        sb.append("\"speedBytesPerSecond\":").append(String.format("%.2f", speedBps)).append(",");
        sb.append("\"estimatedEtaSeconds\":").append(etaSeconds).append(",");
        sb.append("\"lastUpdated\":\"").append(lastUpdatedStr).append("\"");
        sb.append("}");
        return sb.toString();
    }

    // =============
    // Helper methods
    // =============

    /** Simple helper: avoid nulls and remove quotes. */
    private static String safe(String s) {
        if (s == null) return "";
        return s.replace("\"", "'").trim();
    }

    /** Optional helper to log a human-readable timestamp from millis. */
    @SuppressWarnings("unused")
    private static String toLocalDateTime(long epochMs) {
        if (epochMs <= 0) return "";
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(epochMs),
                ZoneId.systemDefault()
        ).toString();
    }
}
