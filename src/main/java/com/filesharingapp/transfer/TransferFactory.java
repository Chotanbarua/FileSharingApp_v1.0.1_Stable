package com.filesharingapp.transfer;

import com.filesharingapp.utils.LoggerUtil;

/**
 * TransferFactory
 * ---------------
 * Baby-English:
 *   ✔ This class decides which transfer helper to use.
 *   ✔ It looks at the mode: "HTTP", "ZeroTier", or "S3".
 *   ✔ Then it creates the correct TransferMethod object.
 *
 * Why?
 *   ✔ So Sender and Receiver do not need to write "new" everywhere.
 *   ✔ If we change the implementation for one mode, we only fix it here.
 */
public final class TransferFactory {

    private TransferFactory() {
        // Utility class – do not create objects.
    }

    /**
     * create
     * ------
     * Baby-English:
     *   ✔ Check the mode string.
     *   ✔ Return the correct TransferMethod implementation.
     *   ✔ Never return null.
     *
     * @param mode The transfer mode: "HTTP", "ZeroTier", or "S3" (case-insensitive).
     * @return A TransferMethod implementation ready to send/receive.
     * @throws IllegalArgumentException If mode is unknown or blank.
     */
    public static TransferMethod create(String mode) {
        if (mode == null || mode.isBlank()) {
            throw new IllegalArgumentException("Transfer mode cannot be null or empty");
        }

        String normalized = mode.trim().toUpperCase();

        switch (normalized) {
            case "HTTP":
                LoggerUtil.info("[Factory] Using HTTP transfer service.");
                return new HttpTransferService(); // Unified service for HTTP

            case "ZEROTIER":
                LoggerUtil.info("[Factory] Using ZeroTier transfer service.");
                return new ZeroTierTransferService(); // Unified service for ZeroTier

            case "S3":
                LoggerUtil.info("[Factory] Using AWS S3 transfer service.");
                return new AwsS3TransferService(); // Unified service for S3

            default:
                LoggerUtil.error("[Factory] Unsupported transfer mode: " + mode);
                throw new IllegalArgumentException("Unsupported transfer mode: " + mode);
        }
    }
}