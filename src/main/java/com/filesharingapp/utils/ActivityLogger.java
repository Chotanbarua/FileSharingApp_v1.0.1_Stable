package com.filesharingapp.utils;

import com.filesharingapp.core.TransferContext;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * ActivityLogger
 * --------------
 * Baby-English:
 *   ✔ This class writes one CSV line for every transfer event.
 *   ✔ Why? So we can check history later: "What happened last time?"
 *   ✔ Each line includes:
 *       - timestamp
 *       - mode (HTTP / ZeroTier / S3)
 *       - sender IP
 *       - receiver IP
 *       - file name
 *       - bytes transferred
 *       - duration (ms)
 *       - status (SUCCESS / FAIL)
 *       - message (error or note)
 *       - resume offset
 *       - AES enabled (true/false)
 *       - checksum
 *       - extra metadata (bucket/key for S3, networkId/peerIp for ZeroTier)
 */
public final class ActivityLogger {

    /** Folder where we keep all log files. */
    private static final Path LOG_DIR = Path.of("logs");
    /** The CSV file that stores transfer history. */
    private static final Path AUDIT_FILE = LOG_DIR.resolve("transfer_audit.csv");

    /** Lock object for thread-safe writes. */
    private static final Object LOCK = new Object();

    static {
        try {
            // Baby-English:
            // ✔ Make sure logs folder exists.
            Files.createDirectories(LOG_DIR);

            // ✔ If CSV file does not exist, create it and write header line.
            if (Files.notExists(AUDIT_FILE)) {
                try (Writer w = new FileWriter(AUDIT_FILE.toFile(), true)) {
                    w.write("timestamp,mode,senderIP,receiverIP,fileName,bytes,durationMs,status,message,resumeOffset,aesEnabled,checksum,extraMeta\n");
                }
            }
        } catch (IOException e) {
            LoggerUtil.error("Failed to init ActivityLogger", e.getMessage());
        }
    }

    private ActivityLogger() {
        // Utility class – do not create objects.
    }

    /**
     * logTransfer
     * -----------
     * Baby-English:
     *   ✔ Take all details (mode, IPs, file, size, status).
     *   ✔ Add TransferContext info (resume offset, AES, checksum).
     *   ✔ Append one CSV line to transfer_audit.csv.
     *
     * @param mode        → HTTP / ZeroTier / S3
     * @param senderIp    → IP or host of sender
     * @param receiverIp  → IP or host of receiver
     * @param fileName    → file name being transferred
     * @param bytes       → how many bytes were sent/received
     * @param durationMs  → how long it took in milliseconds
     * @param status      → "SUCCESS", "FAILED", etc.
     * @param message     → any extra note (error, resume, etc.)
     */
    public static void logTransfer(
            String mode,
            String senderIp,
            String receiverIp,
            String fileName,
            long bytes,
            long durationMs,
            String status,
            String message) {

        // Validate inputs
        if (mode == null || mode.isBlank()) mode = "UNKNOWN";
        if (fileName == null) fileName = "";
        if (senderIp == null) senderIp = "";
        if (receiverIp == null) receiverIp = "";
        if (status == null) status = "UNKNOWN";

        // Pull extra info from TransferContext
        long resumeOffset = TransferContext.getResumeOffsetBytes();
        boolean aesEnabled = TransferContext.isEncryptionEnabled();
        String checksum = safe(TransferContext.getExpectedChecksum());

        // Extra metadata for S3 or ZeroTier
        String extraMeta = "";
        if ("S3".equalsIgnoreCase(mode)) {
            extraMeta = "bucket=" + safe(System.getProperty("aws.bucket", "")) +
                    ";key=" + safe(fileName);
        } else if ("ZEROTIER".equalsIgnoreCase(mode)) {
            extraMeta = "networkId=" + safe(TransferContext.getZeroTierNetworkId()) +
                    ";peerIp=" + safe(TransferContext.getZeroTierPeerIp());
        }

        String timestamp = LocalDateTime.now().toString();

        String line = String.join(",",
                safe(timestamp),
                safe(mode),
                safe(senderIp),
                safe(receiverIp),
                safe(fileName),
                String.valueOf(bytes),
                String.valueOf(durationMs),
                safe(status),
                safe(message),
                String.valueOf(resumeOffset),
                String.valueOf(aesEnabled),
                safe(checksum),
                safe(extraMeta)
        );

        synchronized (LOCK) {
            try (Writer w = new FileWriter(AUDIT_FILE.toFile(), true)) {
                w.write(line);
                w.write("\n");
            } catch (IOException e) {
                // FIX: Convert exception to String
                LoggerUtil.error("Failed to write transfer audit", e.getMessage());
            }
        }
    }


    /**
     * safe
     * ----
     * Baby-English:
     *   ✔ Make text safe for CSV:
     *       - If null → empty
     *       - Replace commas with underscores
     *       - Remove newlines
     */
    private static String safe(String v) {
        if (v == null) return "";
        return v.replace(",", "_")
                .replace("\n", " ")
                .replace("\r", " ")
                .trim();
    }
}