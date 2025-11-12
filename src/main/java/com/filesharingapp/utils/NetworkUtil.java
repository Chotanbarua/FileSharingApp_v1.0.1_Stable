package com.filesharingapp.utils;

import com.filesharingapp.core.PromptManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;

/**
 * NetworkUtil
 * -----------
 * Small helpers for:
 * - finding local IP,
 * - checking if a port is free,
 * - checking if a host:port is reachable,
 * - broadcasting sender handshake info,
 * - verifying ZeroTier network status,
 * - pushing live /prompt updates for browser UI.
 */
public final class NetworkUtil {

    private NetworkUtil() { }

    // ============================
    // 🧭 Local IP Discovery
    // ============================
    public static String findLocalIp() {
        try {
            InetAddress local = InetAddress.getLocalHost();
            if (!local.isLoopbackAddress()) {
                return local.getHostAddress();
            }
        } catch (Exception ignore) { }

        try {
            for (NetworkInterface nif : java.util.Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nif.isUp() || nif.isLoopback()) continue;
                for (InetAddress addr : java.util.Collections.list(nif.getInetAddresses())) {
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            LoggerUtil.warn("Unable to detect local IP: " + e.getMessage());
        }
        return "127.0.0.1";
    }

    // ============================
    // 🔌 Port and Connectivity Tests
    // ============================
    public static boolean isPortFree(int port) {
        try (ServerSocket ignored = new ServerSocket(port)) {
            ignored.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean canConnect(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // ============================
    // 📡 Handshake Broadcast (Sender → Receiver)
    // ============================
    public static void broadcastModeToReceiver(String method, String host, int port) {
        try {
            URL url = new URL("http://" + host + ":" + port + "/handshake?method=" + method);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2000);
            conn.getResponseCode();
            LoggerUtil.info("🔄 Notified receiver of transport mode: " + method);
        } catch (Exception e) {
            LoggerUtil.warn("⚠️ Could not notify receiver about transport mode.");
        }
    }

    /** ✅ Enhanced: notify with filename + checksum for metadata sync */
    public static void broadcastModeToReceiver(String method, String host, int port,
                                               String fileName, String checksum) {
        try {
            String query = String.format(
                    "method=%s&name=%s&checksum=%s",
                    URLEncoder.encode(method, "UTF-8"),
                    URLEncoder.encode(fileName, "UTF-8"),
                    URLEncoder.encode(checksum, "UTF-8")
            );
            URL url = new URL("http://" + host + ":" + port + "/handshake?" + query);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2000);
            conn.getResponseCode();
            LoggerUtil.info("[Handshake] Sent metadata → " + method + " / " + fileName);
        } catch (Exception e) {
            LoggerUtil.warn("⚠️ Could not notify receiver about transport mode / metadata.");
        }
    }

    // ============================
    // 🌍 Ping / Status Checks
    // ============================
    public static boolean pingReceiver(String host, int port) {
        try {
            if (host == null || host.isBlank()) {
                LoggerUtil.warn("❌ No target host provided.");
                uiPrompt("⚠️ No target host provided.");
                return false;
            }

            if (isZeroTierId(host)) {
                uiPrompt("🛰️ Checking ZeroTier connection …");
                boolean ztOk = checkZeroTierStatus(host);
                uiPrompt(ztOk ? "✅ ZeroTier network online" : "❌ ZeroTier network unreachable");
                return ztOk;
            }

            uiPrompt("🌐 Pinging receiver at " + host + ":" + port + " …");
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 2000);
                uiPrompt("✅ Receiver reachable at " + host + ":" + port);
                return true;
            } catch (IOException io) {
                uiPrompt("❌ Receiver not reachable at " + host + ":" + port);
                return false;
            }
        } catch (Exception e) {
            LoggerUtil.error("Ping failed: " + e.getMessage());
            uiPrompt("❌ Ping error: " + e.getMessage());
            return false;
        }
    }

    private static boolean isZeroTierId(String target) {
        return target != null && target.matches("^[0-9a-fA-F]{16}$");
    }

    // ============================
    // 🛰️ ZeroTier CLI Status
    // ============================
    private static boolean checkZeroTierStatus(String networkId) {
        try {
            ProcessBuilder pb = new ProcessBuilder("zerotier-cli", "status");
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) output.append(line).append("\n");
            int exitCode = proc.waitFor();
            if (exitCode == 0 && output.toString().toLowerCase().contains("online")) {
                LoggerUtil.info("🛰️ ZeroTier is online — network reachable.");
                return true;
            } else {
                LoggerUtil.warn("⚠️ ZeroTier network check failed:\n" + output);
                return false;
            }
        } catch (Exception ex) {
            LoggerUtil.error("❌ Error running zerotier-cli: " + ex.getMessage());
            return false;
        }
    }

    /** Optional: used when debugging general ZeroTier info */
    public static void logZeroTierStatusIfConfigured() {
        try {
            ProcessBuilder pb = new ProcessBuilder("zerotier-cli", "info");
            Process proc = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line = reader.readLine();
                if (line != null) {
                    LoggerUtil.info("[ZeroTier] " + line);
                } else {
                    LoggerUtil.info("[ZeroTier] CLI available but returned no info.");
                }
            }
        } catch (IOException e) {
            LoggerUtil.warn("[ZeroTier] CLI not found or not accessible.");
        }
    }

    // ============================
    // 🖥️ Live UI prompt (for web frontend)
    // ============================
    private static void uiPrompt(String msg) {
        try {
            String encoded = URLEncoder.encode(msg, "UTF-8");
            URL url = new URL("http://localhost:8080/prompt?msg=" + encoded);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(1000);
            conn.getResponseCode();
        } catch (Exception ignored) { }
    }
}
