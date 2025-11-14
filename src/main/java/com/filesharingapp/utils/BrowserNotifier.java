package com.filesharingapp.utils;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * BrowserNotifier
 * ---------------
 * Baby-English:
 *   ✔ This helper sends tiny messages to the Web UI at /prompt.
 *   ✔ Why? So the browser can show hints like:
 *       - "Upload started"
 *       - "Resume detected"
 *       - "Transfer complete"
 *
 * Features:
 *   ✔ Dynamic port from AppConfig
 *   ✔ Message length safety (max 500 chars)
 *   ✔ Basic throttling (avoid spamming browser)
 *   ✔ Fire-and-forget (never block main flow)
 */
public final class BrowserNotifier {

    /** Last time we sent a message (ms). */
    private static long lastPushTime = 0;

    /** Minimum gap between pushes (ms). */
    private static final long MIN_INTERVAL_MS = 1000; // 1 second

    private BrowserNotifier() {
        // Utility class – do not create objects.
    }

    /**
     * push
     * ----
     * Baby-English:
     *   ✔ Send one small message to the Web UI.
     *   ✔ If browser is offline, we just log a warning.
     *
     * @param msg → The message to show in browser.
     */
    public static void push(String msg) {
        if (msg == null || msg.isBlank()) {
            return; // Nothing to send
        }

        // Throttle: avoid sending too many messages too fast
        long now = System.currentTimeMillis();
        if (now - lastPushTime < MIN_INTERVAL_MS) {
            LoggerUtil.warn("[BrowserNotifier] Skipping push (too frequent).");
            return;
        }
        lastPushTime = now;

        // Limit message size for safety
        String trimmed = msg.length() > 500 ? msg.substring(0, 500) + "…" : msg;

        try {
            // Encode message for URL
            String encoded = URLEncoder.encode(trimmed, StandardCharsets.UTF_8);

            // Dynamic port from AppConfig
            int port = AppConfig.getInt("app.http.port", 8080);
            String urlStr = "http://localhost:" + port + "/prompt?msg=" + encoded;

            // Fire-and-forget HTTP GET
            URL url = new URL(urlStr);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setConnectTimeout(500);
            con.setReadTimeout(500);
            con.getResponseCode(); // Trigger request
            con.disconnect();

            LoggerUtil.info("[BrowserNotifier] Sent message to browser: " + trimmed);

        } catch (Exception e) {
            LoggerUtil.warn("[BrowserNotifier] Failed to notify browser: " + e.getMessage());
        }
    }
}