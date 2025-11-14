package com.filesharingapp.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LoggerUtil
 * ----------
 * Baby English:
 * - This class is our one-stop shop for logging.
 * - **FIXED:** Simplified error overloads to remove ambiguity.
 */
public final class LoggerUtil {

    private static final Logger logger = LogManager.getLogger("FileSharingApp");
    private static final int MAX_LOGS_PER_SECOND = 50;
    private static final AtomicInteger logCounter = new AtomicInteger(0);
    private static long currentSecond = Instant.now().getEpochSecond();

    private LoggerUtil() {}

    // ============================================================
    // 1️⃣ Simple Overloads (Used when transferId is unknown/optional)
    // ============================================================

    public static void info(String msg) { info(msg, null); }
    public static void success(String msg) { success(msg, null); }
    public static void warn(String msg) { warn(msg, null); }

    // Primary error methods
    public static void error(String msg) { error(msg, null, null); }
    public static void error(String msg, Throwable t) { error(msg, t, null); }


    // ============================================================
    // 2️⃣ Primary Methods (With transferId context)
    // ============================================================

    public static void info(String msg, String transferId) {
        if (!canLog() || msg == null) return;
        logger.info(buildJson("INFO", msg, transferId));
    }

    public static void success(String msg, String transferId) {
        if (!canLog() || msg == null) return;
        logger.info(buildJson("SUCCESS", msg, transferId));
    }

    public static void warn(String msg, String transferId) {
        if (!canLog() || msg == null) return;
        logger.warn(buildJson("WARN", msg, transferId));
    }

    /**
     * error with stack trace
     */
    public static void error(String msg, Throwable t, String transferId) {
        if (!canLog()) return;
        if (msg == null) msg = "Unexpected error";
        logger.error(buildJson("ERROR", msg, transferId), t);
    }

    // Helper to log without stack trace
    public static void error(String msg, String transferId) {
        error(msg, null, transferId);
    }

    /**
     * uiPrompt
     */
    public static void uiPrompt(String message, String transferId) {
        if (!canLog() || message == null || message.isBlank()) return;

        String safeMessage = maskSensitive(message);
        logger.info(buildJson("UI_PROMPT", safeMessage, transferId));

        try {
            String encoded = URLEncoder.encode(safeMessage, StandardCharsets.UTF_8.name());
            int port = AppConfig.getInt("app.http.port", 8080);
            String urlStr = "http://localhost:" + port + "/prompt?msg=" + encoded;

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(500);
            conn.setReadTimeout(500);
            conn.getResponseCode();
            conn.disconnect();
        } catch (Exception e) {
            logger.warn(buildJson("WARN", "uiPrompt HTTP forward failed: " + e.getMessage(), transferId));
        }
    }

    /**
     * buildJson
     */
    private static String buildJson(String level, String msg, String transferId) {
        JSONObject json = new JSONObject();
        json.put("level", level);
        json.put("message", maskSensitive(msg));
        json.put("transferId", transferId == null ? "N/A" : transferId);
        json.put("timestamp", Instant.now().toString());
        return json.toString();
    }

    /**
     * maskSensitive
     */
    private static String maskSensitive(String msg) {
        if (msg == null) return null;
        String masked = msg;
        // ... (Masking regex remains the same)
        masked = masked.replaceAll("(?i)(secret\\s*[:=]\\s*)([^\\s]+)", "$1****MASKED****");
        masked = masked.replaceAll("(?i)(password\\s*[:=]\\s*)([^\\s]+)", "$1****MASKED****");
        masked = masked.replaceAll("(?i)(aws[_-]?secret[_-]?access[_-]?key\\s*[:=]\\s*)([^\\s]+)", "$1****MASKED****");
        return masked;
    }

    /**
     * canLog
     */
    private static boolean canLog() {
        long now = Instant.now().getEpochSecond();
        if (now != currentSecond) {
            currentSecond = now;
            logCounter.set(0);
        }
        return logCounter.incrementAndGet() <= MAX_LOGS_PER_SECOND;
    }
}