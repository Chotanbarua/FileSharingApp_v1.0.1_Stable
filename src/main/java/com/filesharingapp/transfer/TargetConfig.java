package com.filesharingapp.transfer;

/**
 * TargetConfig
 * ------------
 * Baby-English:
 * ✔ This is a small box to hold all the target settings (where the file goes).
 * ✔ Used by Sender to collect input and pass it to the transfer handler.
 */
public final class TargetConfig {

    /** Target address (IP, hostname, or S3 bucket name). */
    private final String targetHost;

    /** Target port (e.g., 8080 for HTTP, 9993 for ZeroTier). */
    private final int port;

    /** Transfer mode: HTTP, ZEROTIER, or S3. */
    private final String mode;

    /** Optional AES password for encryption. */
    private final String aesPassword;

    /** Simple validation flag. */
    private final boolean valid;

    public TargetConfig(String targetHost, int port, String mode, String aesPassword, boolean valid) {
        this.targetHost = targetHost != null ? targetHost.trim() : "";
        this.port = port;
        this.mode = mode != null ? mode.trim().toUpperCase() : "";
        this.aesPassword = aesPassword;
        this.valid = valid;
    }

    // --- Getters ---

    public String getTargetHost() {
        return targetHost;
    }

    public int getPort() {
        return port;
    }

    public String getMode() {
        return mode;
    }

    public String getAesPassword() {
        return aesPassword;
    }

    public boolean isValid() {
        return valid;
    }

    // --- Builder for easier use ---

    public static TargetConfig createInvalid() {
        return new TargetConfig(null, 0, null, null, false);
    }

    public static TargetConfig createValid(String host, int port, String mode, String aesPassword) {
        return new TargetConfig(host, port, mode, aesPassword, true);
    }
}