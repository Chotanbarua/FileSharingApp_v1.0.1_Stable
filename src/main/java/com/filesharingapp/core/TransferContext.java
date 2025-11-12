package com.filesharingapp.core;

import com.filesharingapp.utils.LoggerUtil;

/**
 * Minimal process-wide context to pass filename/checksum
 * and active transfer metadata between Sender, Server, and Receiver.
 */
public final class TransferContext {
    private static volatile String incomingName;
    private static volatile String expectedChecksum;

    // 🆕 Optional runtime metadata (non-breaking additions)
    private static volatile String activeMethod;
    private static volatile String lastSenderIp;

    private TransferContext() {}

    // ======================
    // ✅ Core Getters/Setters
    // ======================
    public static void setIncomingName(String name) {
        incomingName = name;
        LoggerUtil.info("[Context] Incoming file name set to: " + name);
    }

    public static void setExpectedChecksum(String checksum) {
        expectedChecksum = checksum;
        LoggerUtil.info("[Context] Expected checksum set to: " + checksum);
    }

    public static String getIncomingName()        { return incomingName; }
    public static String getExpectedChecksum()    { return expectedChecksum; }

    // ======================
    // 🆕 Extra Metadata (used by handshake)
    // ======================
    public static void setActiveMethod(String method) {
        activeMethod = method;
        LoggerUtil.info("[Context] Transfer method set to: " + method);
    }

    public static void setLastSenderIp(String ip) {
        lastSenderIp = ip;
        LoggerUtil.info("[Context] Last sender IP recorded as: " + ip);
    }

    public static String getActiveMethod() { return activeMethod; }
    public static String getLastSenderIp() { return lastSenderIp; }

    // ======================
    // 🧹 Clear Context
    // ======================
    public static void clear() {
        incomingName = null;
        expectedChecksum = null;
        activeMethod = null;
        lastSenderIp = null;
        LoggerUtil.info("[Context] TransferContext cleared.");
    }
}
