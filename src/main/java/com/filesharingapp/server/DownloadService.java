package com.filesharingapp.server;

import com.filesharingapp.core.TransferContext;
import com.filesharingapp.utils.HashUtil;
import com.filesharingapp.utils.LoggerUtil;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.io.File;
import java.util.regex.Pattern; // FIX: Added missing import for Pattern

/**
 * DownloadService
 * ---------------
 * Baby-English:
 * ✔ This helper sends a file from the server to the receiver.
 * ✔ Supports resume using HTTP Range header:
 * Range: bytes=start-
 * ✔ Sends correct headers for partial content.
 */
public final class DownloadService {

    private static final int BUFFER = 8192;
    // FIX: Added Pattern declaration for range parsing
    private static final Pattern RANGE_PATTERN = Pattern.compile("bytes=(\\d+)-");

    private DownloadService() {}

    // ============================================================
    // 1️⃣ Main entry: streamDownload with resume
    // ============================================================

    /**
     * streamDownload
     * Baby-English:
     * ✔ Open file from "received/{name}".
     * ✔ If Range header exists → resume from offset.
     * ✔ Send bytes in small pieces.
     * ✔ Update progress trackers.
     *
     * @param transferId unique transfer ID
     * @param filePath   path to file in "received"
     * @param rangeHeader HTTP Range header (or null)
     * @param clientOut  output stream to client
     */
    public static void streamDownload(
            String transferId,
            Path filePath,
            String rangeHeader,
            OutputStream clientOut) throws IOException {

        if (filePath == null || !Files.exists(filePath)) {
            LoggerUtil.error("File missing → " + filePath);
            throw new IOException("File not found on server.");
        }

        long fileSize = Files.size(filePath);
        long start = parseRange(rangeHeader, fileSize); // Calls helper within this class

        // Mark transfer start if full download
        if (start == 0) {
            TransferContext.getOrCreateProgress(transferId, fileSize);
        }

        TransferContext.Progress prog = TransferContext.getOrCreateProgress(transferId, fileSize);

        try (InputStream in = new BufferedInputStream(Files.newInputStream(filePath))) {
            if (start > 0) {
                long skipped = in.skip(start);
                LoggerUtil.info("[Resume] Skipped " + skipped + " bytes.");
            }

            byte[] buf = new byte[BUFFER];
            long sent = start;
            int len;

            while ((len = in.read(buf)) != -1) {
                clientOut.write(buf, 0, len);
                sent += len;

                TransferContext.addReceivedBytes(transferId, len);

                if (sent % (512 * 1024) < BUFFER) {
                    LoggerUtil.info("[Download] Sent " + sent + " of " + fileSize);
                }
            }

            clientOut.flush();

            if (sent == fileSize) {
                LoggerUtil.success("[Download] File fully delivered (" + fileSize + " bytes)");
            }

        } catch (IOException e) {
            LoggerUtil.error("Error while sending bytes: " + e.getMessage(), e);
            throw e;
        }
    }

    // ============================================================
    // 2️⃣ Helper method to parse HTTP Range header (FIX: Missing method added)
    // ============================================================

    /**
     * parseRange
     * Baby-English: Checks "Range: bytes=X-" header and returns start offset X.
     * * @param rangeHeader The HTTP Range header string (e.g., "bytes=100-").
     * @param totalSize The total size of the file.
     * @return The starting byte offset (0 if full download is requested).
     */
    public static long parseRange(String rangeHeader, long totalSize) {
        if (rangeHeader == null || !rangeHeader.startsWith("bytes=")) {
            return 0; // Full download
        }

        String range = rangeHeader.substring("bytes=".length()).trim();

        // Simple case: bytes=X- (start from X to the end)
        if (range.endsWith("-")) {
            try {
                long start = Long.parseLong(range.substring(0, range.length() - 1));
                return Math.max(0, Math.min(start, totalSize));
            } catch (NumberFormatException ignore) {
                // Malformed range, fallback to full download
                return 0;
            }
        }

        // Complex ranges (e.g., bytes=X-Y or bytes=-Y) are ignored.
        return 0;
    }


    // ============================================================
    // 3️⃣ Verify checksum after full download
    // ============================================================

    /**
     * verifyChecksum
     * Baby-English:
     * ✔ Compute SHA-256 and compare with expected.
     */
    public static boolean verifyChecksum(Path file, String expected) {
        if (expected == null || expected.isBlank()) {
            LoggerUtil.warn("[Checksum] No expected checksum provided.");
            return true;
        }

        try {
            String actual = HashUtil.sha256Hex(file.toFile());
            boolean ok = actual.equalsIgnoreCase(expected);

            if (ok) {
                LoggerUtil.success("[Checksum] Match: " + actual);
            } else {
                LoggerUtil.error("[Checksum] MISMATCH. Expected: " + expected + ", actual: " + actual);
            }
            return ok;

        } catch (Exception e) {
            LoggerUtil.error("[Checksum] Error computing hash", e);
            return false;
        }
    }
}