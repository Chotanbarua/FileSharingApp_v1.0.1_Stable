package com.filesharingapp.transfer;

/**
 * TransferFactory
 * ---------------
 * Factory for selecting correct transfer handler at runtime.
 */
public class TransferFactory {

    public static TransferMethod getHandler(String method) {
        if (method == null) return null;
        return switch (method.trim().toUpperCase()) {
            case "HTTP" -> new HttpTransferHandler();
            case "ZEROTIER" -> new ZeroTierTransferHandler();
            case "S3" -> new S3TransferHandler();
            default -> null;
        };
    }

    // ----------------------------------------------------------------------------------------------------
// Added for v1.0.5 compatibility (Receiver / Sender interactive mode support)
// ----------------------------------------------------------------------------------------------------
    public static TransferMethod create(String method) {
        if (method == null) return null;
        method = method.trim().toUpperCase();

        switch (method) {
            case "HTTP":
                return new com.filesharingapp.transfer.HttpTransferHandler();
            case "ZEROTIER":
                return new com.filesharingapp.transfer.ZeroTierTransferHandler();
            case "S3":
            case "AWS":
                return new com.filesharingapp.transfer.S3TransferHandler();
            default:
                return null;
        }
    }

}
