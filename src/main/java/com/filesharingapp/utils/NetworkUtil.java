package com.filesharingapp.utils;

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
 * - checking if a host:port is reachable.
 */
public final class NetworkUtil {

    private NetworkUtil() {
    }

    /**
     * Find a non-loopback local IP address to show to user.
     */
    public static String findLocalIp() {
        try {
            // Best effort: use local host.
            InetAddress local = InetAddress.getLocalHost();
            if (!local.isLoopbackAddress()) {
                return local.getHostAddress();
            }
        } catch (Exception ignore) {
        }

        // Fallback: try all interfaces.
        try {
            for (NetworkInterface nif : java.util.Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nif.isUp() || nif.isLoopback()) {
                    continue;
                }
                for (InetAddress addr : java.util.Collections.list(nif.getInetAddresses())) {
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            LoggerUtil.warn("Unable to detect local IP: " + e.getMessage());
        }

        // Last resort.
        return "127.0.0.1";
    }

    /**
     * Check if a port is free on this machine.
     */
    public static boolean isPortFree(int port) {
        try (ServerSocket ignored = new ServerSocket(port)) {
            ignored.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Check if host:port is reachable within timeout.
     */
    public static boolean canConnect(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

//    public static boolean pingReceiver(String targetHost, int port) {
//        try (Socket socket = new Socket()) {
//            socket.connect(new InetSocketAddress(targetHost, port), 2000);
//            LoggerUtil.info("✅ Receiver reachable at " + targetHost + ":" + port);
//            return true;
//        } catch (Exception e) {
//            LoggerUtil.warn("❌ Receiver not reachable at " + targetHost + ":" + port);
//            return false;
//        }
//    }

    public static void broadcastModeToReceiver(String method, String host, int port) {
        try {
            java.net.URL url = new java.net.URL("http://" + host + ":" + port + "/handshake?method=" + method);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2000);
            conn.getResponseCode();
            LoggerUtil.info("🔄 Notified receiver of transport mode: " + method);
        } catch (Exception e) {
            LoggerUtil.warn("⚠️ Could not notify receiver about transport mode.");
        }
    }


    public static boolean pingReceiver(String host, int port) {
        try {
            if (host == null || host.isBlank()) {
                LoggerUtil.warn("❌ No target host provided.");
                uiPrompt("⚠️ No target host provided.");
                return false;
            }

            // ✅ ZeroTier check
            if (isZeroTierId(host)) {
                uiPrompt("🛰️ Checking ZeroTier connection …");
                boolean ztOk = checkZeroTierStatus(host);
                uiPrompt(ztOk ? "✅ ZeroTier network online" : "❌ ZeroTier network unreachable");
                return ztOk;
            }

            // ✅ Standard HTTP / TCP ping check
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

    // ✅ Runs `zerotier-cli status` to confirm connectivity
    private static boolean checkZeroTierStatus(String networkId) {
        try {
            ProcessBuilder pb = new ProcessBuilder("zerotier-cli", "status");
            pb.redirectErrorStream(true);
            Process proc = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

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

    // ✅ Send live status updates to /prompt endpoint (used by browser UI)
    private static void uiPrompt(String msg) {
        try {
            String encoded = java.net.URLEncoder.encode(msg, "UTF-8");
            URL url = new URL("http://localhost:8080/prompt?msg=" + encoded);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(1000);
            conn.getResponseCode(); // trigger
        } catch (Exception ignored) {
            // silent fallback
        }
    }
}







