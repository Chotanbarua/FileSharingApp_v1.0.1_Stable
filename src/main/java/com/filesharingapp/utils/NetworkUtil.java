package com.filesharingapp.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean; // FIX: Added missing import
import java.util.stream.Collectors;

/**
 * NetworkUtil
 * -----------
 * Baby English:
 * - This class helps with network stuff.
 */
public final class NetworkUtil {

    private NetworkUtil() {
        // Utility class – do not create objects.
    }

    // ============================
    // 🧭 Local IP Discovery
    // ============================
    public static String findLocalIp() {
        try {
            InetAddress local = InetAddress.getLocalHost();
            if (!local.isLoopbackAddress()) {
                return local.getHostAddress();
            }
        } catch (Exception ignore) {
            // fallback below
        }

        try {
            for (NetworkInterface nif : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nif.isUp() || nif.isLoopback()) continue;
                for (InetAddress addr : Collections.list(nif.getInetAddresses())) {
                    if ((addr instanceof Inet4Address || addr instanceof Inet6Address) && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            // FIX: Explicitly pass null transferId to resolve ambiguity (Line 170)
            LoggerUtil.warn("Unable to detect local IP: " + e.getMessage(), null);
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
    public static void broadcastModeToReceiver(String method, String host, String fileName, String checksum) {
        if (method == null || method.isBlank() || fileName == null || fileName.isBlank()) {
            LoggerUtil.warn("Invalid handshake parameters", null);
            return;
        }

        int port = AppConfig.getInt("app.http.port", 8080);
        int timeout = AppConfig.getInt("network.timeout.ms", 2000);

        String query = String.format("method=%s&name=%s&checksum=%s",
                URLEncoder.encode(method, StandardCharsets.UTF_8),
                URLEncoder.encode(fileName, StandardCharsets.UTF_8),
                URLEncoder.encode(checksum, StandardCharsets.UTF_8));

        String urlStr = "http://" + host + ":" + port + "/handshake?" + query;

        // FIX: runWithRetry must use a Callable<Boolean> or runWithRetry(Runnable)
        // Since we need to return true/false, we wrap the action in a Runnable and return its boolean result.
        // Ambiguity fixed by explicitly passing the cancel token (new AtomicBoolean(false)).
        RetryUtil.runWithRetry(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setConnectTimeout(timeout);
                conn.setReadTimeout(timeout);
                int code = conn.getResponseCode();
                if (code == 200) {
                    LoggerUtil.info("[Handshake] OK → " + method + " / " + fileName, null);
                } else {
                    LoggerUtil.warn("[Handshake] Failed with HTTP " + code, null);
                    throw new IOException("Handshake failed with code: " + code);
                }
            } catch (Exception e) {
                LoggerUtil.warn("[Handshake] Error: " + e.getMessage(), null);
                throw new RuntimeException(e); // Rethrow to trigger retry
            }
        }, 3, AppConfig.getLong("retry.delay.ms", 1000L), new AtomicBoolean(false));
    }

    // ============================
    // 🌍 Ping / Status Checks
    // ============================
    public static boolean pingReceiver(String host, int port) {
        int timeout = AppConfig.getInt("network.timeout.ms", 2000);

        if (host == null || host.isBlank()) {
            LoggerUtil.uiPrompt("⚠️ No target host provided", null);
            return false;
        }

        if (isZeroTierId(host)) {
            LoggerUtil.uiPrompt("🛰️ Checking ZeroTier connection …", null);
            boolean ztOk = checkZeroTierStatus(AppConfig.get("zerotier.network.id", host));
            LoggerUtil.uiPrompt(ztOk ? "✅ ZeroTier network online" : "❌ ZeroTier network unreachable", null);
            return ztOk;
        }

        LoggerUtil.uiPrompt("🌐 Pinging receiver at " + host + ":" + port + " …", null);
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            LoggerUtil.uiPrompt("✅ Receiver reachable at " + host + ":" + port, null);
            return true;
        } catch (IOException io) {
            LoggerUtil.uiPrompt("❌ Receiver not reachable at " + host + ":" + port, null);
            return false;
        }
    }

    private static boolean isZeroTierId(String target) {
        return target != null && target.matches("^[0-9a-fA-F]{16}$");
    }

    // ============================
    // 🛰️ ZeroTier CLI Status + Peer Discovery
    // ============================
    private static boolean checkZeroTierStatus(String networkId) {
        if (!isCliAvailable()) {
            LoggerUtil.warn("ZeroTier CLI not found", null);
            return false;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(getZeroTierCliPath(), "status");
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            String output = new BufferedReader(new InputStreamReader(proc.getInputStream()))
                    .lines().collect(Collectors.joining("\n"));
            int exitCode = proc.waitFor();
            return exitCode == 0 && output.toLowerCase().contains("online");
        } catch (Exception ex) {
            LoggerUtil.error("ZeroTier status check failed: " + ex.getMessage(), null, null);
            return false;
        }
    }

    public static List<String> discoverZeroTierPeers() {
        if (!isCliAvailable()) return Collections.emptyList();
        try {
            ProcessBuilder pb = new ProcessBuilder(getZeroTierCliPath(), "listpeers");
            Process proc = pb.start();
            return new BufferedReader(new InputStreamReader(proc.getInputStream()))
                    .lines()
                    .filter(line -> line.contains("ONLINE"))
                    .map(line -> line.split("\\s+")[2]) // IP column
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LoggerUtil.warn("Failed to list ZeroTier peers: " + e.getMessage(), null);
            return Collections.emptyList();
        }
    }

    public static boolean isCliAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(getZeroTierCliPath(), "--version");
            return pb.start().waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getZeroTierCliPath() {
        return AppConfig.get("zerotier.cli.path", "zerotier-cli");
    }

    // ============================
    // 🗄️ S3 Network Test Stub
    // ============================
    public static boolean testS3Connectivity(String endpoint) {
        LoggerUtil.info("Testing S3 connectivity to " + endpoint, null);
        // TODO: Implement real S3 ping using AWS SDK
        return true;
    }

    /**
     * Baby English: Checks if the ZeroTier CLI is installed and executable.
     */
    public static boolean isZeroTierInstalled() {
        try {
            // FIX: Use the public path getter
            ProcessBuilder pb = new ProcessBuilder(getZeroTierCliPath(), "--version");
            return pb.start().waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}