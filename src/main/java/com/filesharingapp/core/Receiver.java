package com.filesharingapp.core;

import com.filesharingapp.transfer.TransferFactory;
import com.filesharingapp.transfer.TransferMethod;
import com.filesharingapp.utils.HashUtil;
import com.filesharingapp.utils.LoggerUtil;
import com.filesharingapp.utils.NetworkUtil;

import java.io.File;
import java.util.Locale;
import java.util.Scanner;

/**
 * Receiver
 * --------
 * Handles incoming transfer operations and integrity verification.
 */
public class Receiver {

    private TransferMethod currentTransferHandler;

    public void runInteractive() {
        try (Scanner in = new Scanner(System.in)) {
            LoggerUtil.info("✅ Receiver mode initialized.");

            LoggerUtil.info("Sender is using which transport? (HTTP / ZeroTier / S3)");
            String method = in.nextLine().trim();
            currentTransferHandler = TransferFactory.create(method);

            if (currentTransferHandler == null) {
                LoggerUtil.error("❌ Invalid or unsupported method: " + method);
                return;
            }

            LoggerUtil.info("Please enter the Sender’s IP or download URL:");
            String senderHost = in.nextLine().trim();

            LoggerUtil.info("Enter port number (default 8080):");
            int senderPort = Integer.parseInt(in.nextLine().trim());

            // --- Connectivity check & retry loop ---
            LoggerUtil.info(PromptManager.TRYING_REACH);
            int max = 3;
            boolean ok = false;
            for (int i = 1; i <= max; i++) {
                if (NetworkUtil.pingReceiver(senderHost, senderPort)) {
                    LoggerUtil.info(PromptManager.NETWORK_PING_SUCCESS);
                    ok = true;
                    break;
                }
                LoggerUtil.warn(PromptManager.retrying(i, max));
                if (i == max) {
                    LoggerUtil.error(PromptManager.RETRY_GIVEUP);
                    return;
                }
                Thread.sleep(1000L);
            }
            if (!ok) return;

            // --- Ask where to save the incoming file(s) ---
            LoggerUtil.info(PromptManager.ASK_SAVE_FOLDER);
            String path = in.nextLine().trim();
            if (path.isEmpty()) path = System.getProperty("user.home") + File.separator + "Downloads";
            File saveDir = new File(path);
            if (!saveDir.exists() && !saveDir.mkdirs()) {
                LoggerUtil.warn(PromptManager.PERMISSION_DENIED);
                return;
            }
            if (!saveDir.canWrite()) {
                LoggerUtil.warn(PromptManager.PERMISSION_DENIED);
                return;
            }
            LoggerUtil.info(PromptManager.FREE_SPACE_OK);

            // --- Log incoming metadata ---
            String incomingName = TransferContext.getIncomingName();
            String expectedChecksum = TransferContext.getExpectedChecksum();
            LoggerUtil.info("Incoming file: " + incomingName + " | Expected checksum: " + expectedChecksum);

            // --- Perform actual receive ---
            currentTransferHandler.receive(saveDir.getAbsolutePath());

            // --- After file download completes ---
            File downloaded = new File(saveDir, incomingName != null ? incomingName : "received_file.tmp");
            try {
                verifyChecksumWithRetry(downloaded, expectedChecksum);
            } catch (Exception ex) {
                LoggerUtil.error(PromptManager.VERIFY_FAIL, ex);
                return;
            }
            LoggerUtil.info(PromptManager.VERIFY_OK);
            LoggerUtil.success("🎉 File received successfully at " + saveDir);

        } catch (Exception e) {
            LoggerUtil.error("Receiver failed", e);
        }
    }

    private static void verifyChecksumWithRetry(File file, String expected) throws Exception {
        if (expected == null || expected.isBlank()) {
            LoggerUtil.warn("⚠️ No checksum provided by sender, skipping verification.");
            return;
        }
        String actual = HashUtil.sha256Hex(file);
        if (!actual.equalsIgnoreCase(expected))
            throw new Exception("Checksum mismatch: " + actual + " ≠ " + expected);
    }
}
