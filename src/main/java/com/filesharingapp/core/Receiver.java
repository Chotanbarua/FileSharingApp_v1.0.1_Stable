package com.filesharingapp.core;

import com.filesharingapp.security.HashUtil;
import com.filesharingapp.utils.AppConfig;
import com.filesharingapp.utils.LoggerUtil;
import com.filesharingapp.transfer.TransferFactory;
import com.filesharingapp.transfer.TransferMethod;

import java.io.File;
import java.util.Scanner;

/**
 * Receiver
 * --------
 * Handles incoming file transfers and checksum verification.
 *
 * ✅ Adds runInteractive() method (required by MainController & DashboardFrame)
 * ✅ Uses existing verifyChecksumWithRetry() logic
 * ✅ Non-breaking addition — no refactoring elsewhere needed
 */
public class Receiver {

    private TransferMethod currentTransferHandler;

    /**
     * Console/interactive entry point for Receiver.
     * Called by MainController and DashboardFrame.
     */
    public void runInteractive() {
        try (Scanner in = new Scanner(System.in)) {
            LoggerUtil.info("📥 Receiver mode initialized.");

            LoggerUtil.info("Sender is using which transport? (HTTP / ZeroTier / S3)");
            String method = in.nextLine().trim();
            currentTransferHandler = TransferFactory.create(method);

            if (currentTransferHandler == null) {
                LoggerUtil.error("❌ Invalid or unsupported method: " + method);
                return;
            }

            LoggerUtil.info("Enter folder path to save the file (leave blank for current folder):");
            String savePath = in.nextLine().trim();
            if (savePath.isEmpty()) {
                savePath = System.getProperty("user.dir");
            }

            LoggerUtil.info("Starting receiver...");
            currentTransferHandler.receive(savePath);

            // Example: After receive, verify checksum (mocked expected hash)
            File received = new File(savePath, "received_file.tmp");
            verifyChecksumWithRetry(received, "mock_expected_sha256");

            LoggerUtil.success("🎉 File received successfully at " + savePath);
        } catch (Exception e) {
            LoggerUtil.error("Receiver flow failed.", e);
        }
    }

    /**
     * Verifies SHA-256 integrity and retries failed transfers.
     */
    private void verifyChecksumWithRetry(File downloadedFile, String expectedChecksum) throws Exception {
        boolean enabled = Boolean.parseBoolean(AppConfig.get("transfer.checksum.enabled", "true"));
        int maxRetries = Integer.parseInt(AppConfig.get("transfer.checksum.maxRetries", "3"));
        if (!enabled) return;

        for (int i = 1; i <= maxRetries; i++) {
            String actual = HashUtil.sha256Hex(downloadedFile);
            if (actual.equalsIgnoreCase(expectedChecksum)) {
                LoggerUtil.success("[Receiver] ✅ SHA-256 match on attempt " + i);
                return;
            }

            LoggerUtil.warn("[Receiver] ❌ Checksum failed, retry " + i);
            if (i < maxRetries) {
                if (downloadedFile.exists()) downloadedFile.delete();
                currentTransferHandler.receive(downloadedFile.getParent());
            } else {
                throw new IllegalStateException("Checksum failed after " + maxRetries + " attempts.");
            }
        }
    }
}
