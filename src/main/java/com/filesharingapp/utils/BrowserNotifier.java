package com.filesharingapp.utils;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Sends small messages to the Web UI via /prompt.
 * Non-fatal: failures are logged but never break core flow.
 */
public final class BrowserNotifier {

    private BrowserNotifier() {
    }

    public static void push(String msg) {
        try {
            java.net.URL url = new java.net.URL("http://localhost:8080/prompt?msg=" +
                    java.net.URLEncoder.encode(msg, "UTF-8"));
            java.net.HttpURLConnection con = (java.net.HttpURLConnection) url.openConnection();
            con.setConnectTimeout(500);
            con.setReadTimeout(500);
            con.getResponseCode(); // fire-and-forget
        } catch (Exception e) {
            // Don't spam, just warn if needed
            LoggerUtil.warn("BrowserNotifier failed: " + e.getMessage());
        }
    }
}
