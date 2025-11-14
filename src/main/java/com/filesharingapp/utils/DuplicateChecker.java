package com.filesharingapp.utils;

import com.filesharingapp.core.TransferContext;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/**
 * DuplicateChecker
 * ----------------
 * Baby-English:
 * ✔ This class remembers which files were already transferred.
 * ✔ Why? So we can warn the user if they try to send the same file
 * to the same person using the same method.
 *
 * Features:
 * ✔ Thread-safe (synchronized)
 * ✔ Stores keys in memory and logs them in CSV
 * ✔ Includes method type (HTTP / ZeroTier / S3)
 * ✔ Validates file name
 * ✔ Supports expiration (old entries removed after X days)
 * ✔ Better CSV escaping
 */
public final class DuplicateChecker {

    /** Folder for logs. */
    private static final Path LOG_DIR = Path.of("logs");
    /** CSV file for duplicate tracking. */
    private static final Path DUP_FILE = LOG_DIR.resolve("duplicate_files.csv");

    /** In-memory set of keys: "sender|receiver|fileName|mode". */
    private static final Set<String> seen = Collections.synchronizedSet(new HashSet<>());

    /** Expiration in days for old entries. */
    private static final int EXPIRATION_DAYS = 30;

    static {
        try {
            Files.createDirectories(LOG_DIR);

            if (Files.exists(DUP_FILE)) {
                try (BufferedReader br = Files.newBufferedReader(DUP_FILE)) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String trimmed = line.trim();
                        if (!trimmed.isEmpty()) {
                            // FIX: Must strip timestamp/data from key before adding to seen set
                            String[] parts = trimmed.split(",", 2);
                            seen.add(parts[0].trim());
                        }
                    }
                }
            }
        } catch (IOException e) {
            // FIX: Corrected LoggerUtil.error call to match new signature (msg, throwable)
            LoggerUtil.error("Failed to init DuplicateChecker", e);
        }
    }

    private DuplicateChecker() {
        // Utility class – no objects.
    }

    /**
     * isDuplicate
     * -----------
     * Baby-English:
     * ✔ Build a key like "sender|receiver|fileName|mode".
     * ✔ If key exists → return true (duplicate).
     * ✔ If not → add it to memory and CSV, return false.
     *
     * @param sender   Who sends the file
     * @param receiver Who receives the file
     * @param fileName Name of the file
     * @return true if this combination was already logged
     */
    public static boolean isDuplicate(String sender, String receiver, String fileName) {
        if (fileName == null || fileName.isBlank()) {
            // FIX: Corrected LoggerUtil.warn call to use single-argument overload
            LoggerUtil.warn("[DuplicateChecker] File name is empty. Skipping duplicate check.");
            return false;
        }

        String mode = TransferContext.getActiveMethod();
        String key = buildKey(sender, receiver, fileName, mode);

        synchronized (DuplicateChecker.class) {
            if (seen.contains(key)) {
                return true;
            }

            seen.add(key);
            appendToCsv(key);
        }

        return false;
    }

    /**
     * appendToCsv
     * -----------
     * Baby-English:
     * ✔ Write the key into duplicate_files.csv.
     * ✔ Add timestamp for expiration logic.
     */
    private static void appendToCsv(String key) {
        try (Writer w = new FileWriter(DUP_FILE.toFile(), true)) {
            String timestamp = Instant.now().toString();
            // FIX: Escaping the key before writing to ensure consistency with loading logic
            w.write(escapeCsv(key) + "," + timestamp + "\n");
        } catch (IOException e) {
            // FIX: Corrected LoggerUtil.error call to match new signature (msg, throwable)
            LoggerUtil.error("Failed to log duplicate", e);
        }
    }

    /**
     * buildKey
     * --------
     * Baby-English:
     * ✔ Combine sender, receiver, fileName, and mode into one safe string.
     */
    private static String buildKey(String sender, String receiver, String fileName, String mode) {
        return safe(sender) + "|" + safe(receiver) + "|" + safe(fileName) + "|" + safe(mode);
    }

    /**
     * safe
     * ----
     * Baby-English:
     * ✔ Make text safe:
     * - null → empty
     * - remove pipes and newlines
     */
    private static String safe(String v) {
        if (v == null) return "";
        return v.replace("\n", " ")
                .replace("\r", " ")
                .replace("|", "_")
                .trim();
    }

    /**
     * escapeCsv
     * ---------
     * Baby-English:
     * ✔ Wrap text in quotes and replace risky characters.
     */
    private static String escapeCsv(String v) {
        if (v == null) return "\"\"";
        return "\"" + v.replace("\"", "'") + "\"";
    }

    /**
     * cleanupOldEntries
     * -----------------
     * Baby-English:
     * ✔ Remove entries older than EXPIRATION_DAYS from memory and file.
     */
    public static void cleanupOldEntries() {
        try {
            if (!Files.exists(DUP_FILE)) return;

            List<String> freshLines = new ArrayList<>();
            // FIX: 86400 seconds in a day. Multiplied by 1000 for milliseconds, but Instant.minusSeconds is better.
            Instant cutoff = Instant.now().minusSeconds(EXPIRATION_DAYS * 86400L);

            try (BufferedReader br = Files.newBufferedReader(DUP_FILE)) {
                String line;
                while ((line = br.readLine()) != null) {
                    // FIX: Ensure CSV parsing handles the escaped key
                    String[] parts = line.split("\",", 2);
                    if (parts.length == 2) {
                        String key = parts[0].replace("\"", "").trim(); // Remove leading quote
                        Instant ts = Instant.parse(parts[1].trim());

                        if (ts.isAfter(cutoff)) {
                            freshLines.add(line);
                        } else {
                            seen.remove(key);
                        }
                    }
                }
            }

            Files.write(DUP_FILE, freshLines);
            // FIX: Corrected LoggerUtil.info call to use single-argument overload
            LoggerUtil.info("[DuplicateChecker] Old entries cleaned up.");
        } catch (Exception e) {
            // FIX: Corrected LoggerUtil.warn call to use single-argument overload
            LoggerUtil.warn("[DuplicateChecker] Cleanup failed: " + e.getMessage());
        }
    }
}