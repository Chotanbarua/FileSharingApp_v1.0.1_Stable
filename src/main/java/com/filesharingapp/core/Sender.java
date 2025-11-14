package com.filesharingapp.core;

import com.filesharingapp.security.AesUtil;
import com.filesharingapp.security.AuthUtil; // FIX: Added missing import
import com.filesharingapp.transfer.TargetConfig;
import com.filesharingapp.transfer.TransferFactory;
import com.filesharingapp.transfer.TransferMethod;
import com.filesharingapp.utils.ActivityLogger;
import com.filesharingapp.utils.HashUtil;
import com.filesharingapp.utils.LoggerUtil;
import com.filesharingapp.utils.NetworkUtil;
import com.filesharingapp.utils.ValidationMessages; // FIX: Added missing import
import com.filesharingapp.utils.ValidationUtil;
import com.filesharingapp.utils.ZipUtil; // FIX: Added missing import

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Scanner;

/**
 * Sender
 * ------
 * Baby-English:
 * - This class talks to the human on the Sender side.
 * - It orchestrates file preparation and the transfer process.
 */
public class Sender {

    /** The current transfer handler (HTTP / ZeroTier / S3). */
    private TransferMethod currentTransferHandler;

    // ============================================
    // CONSOLE/INTERACTIVE FLOW ENTRY POINT
    // ============================================

    /**
     * Run the Sender flow in interactive console mode.
     *
     * @param userName    friendly name of the user (already validated)
     * @param sessionName name of the session (for logs only)
     */
    public void runInteractive(String userName, String sessionName) {
        Scanner in = new Scanner(System.in);

        try {
            // -------------------------------
            // 1) Say hello and log who we are
            // -------------------------------
            LoggerUtil.success(PromptManager.SENDER_INIT_OK);
            LoggerUtil.info("[Sender] Session: " + sessionName + " | User: " + userName);

            // -------------------------------
            // 2) Ask which file to send
            // -------------------------------
            File originalFile = askForFile(in);
            if (originalFile == null) return;

            // -------------------------------
            // 3) Pick transport method
            // -------------------------------
            String mode = askTransportMode(in);
            if (mode == null) return;

            // -------------------------------
            // 4) Ask for target details & build TargetConfig
            // -------------------------------
            TargetConfig targetConfig = askTargetConfig(in, mode);
            if (!targetConfig.isValid()) return;

            // -------------------------------
            // 5) Build the handler
            // -------------------------------
            currentTransferHandler = TransferFactory.create(mode);
            if (currentTransferHandler == null) {
                LoggerUtil.error("❌ No handler for method: " + mode);
                return;
            }

            // -------------------------------
            // 6) Prepare file (Zip/Encrypt)
            // -------------------------------
            File fileToSend = prepareFileForTransfer(originalFile, targetConfig);
            if (fileToSend == null) return;

            // -------------------------------
            // 7) Compute checksum & Update Context
            // -------------------------------
            String checksum = computeChecksum(fileToSend);
            if (checksum == null) return;
            TransferContext.setIncomingName(fileToSend.getName());
            TransferContext.setExpectedChecksum(checksum);
            TransferContext.setActiveMethod(mode.toUpperCase(Locale.ROOT));
            TransferContext.setLastSenderIp(targetConfig.getTargetHost());
            TransferContext.setEncryptionEnabled(targetConfig.getAesPassword() != null);

            // -------------------------------
            // 8) Confirm READY
            // -------------------------------
            if (!confirmReady(in, fileToSend.getName())) return;

            // -------------------------------
            // 9) Start transfer
            // -------------------------------
            LoggerUtil.info("🚀 Starting transfer using " + mode.toUpperCase(Locale.ROOT) + " …");
            long start = System.currentTimeMillis();
            boolean success = false;

            try {
                currentTransferHandler.send(userName, fileToSend, targetConfig);
                success = true;
                LoggerUtil.success("🎉 File sent successfully.");
            } catch (Exception e) {
                LoggerUtil.error("Sender flow failed", e);
            }

            long durationMs = System.currentTimeMillis() - start;
            logAuditTrail(mode, targetConfig.getTargetHost(), fileToSend, durationMs, success);

        } catch (Exception e) {
            LoggerUtil.error("Sender interactive flow crashed", e);
        }
    }

    // ============================================
    // UI-DRIVEN FLOW ENTRY POINT (DashboardFrame)
    // ============================================

    /**
     * Overload for UI-driven transfer (DashboardFrame)
     */
    public void runInteractive(String userName, TargetConfig config, File file) { // FIX: Removed unused sessionName parameter

        if (!config.isValid() || file == null) {
            LoggerUtil.error("Invalid configuration or file for UI transfer.", null, null);
            return;
        }

        try {
            LoggerUtil.info("[Sender-UI] User: " + userName + " | Mode: " + config.getMode());

            TransferMethod handler = TransferFactory.create(config.getMode());
            if (handler == null) {
                LoggerUtil.error("❌ No handler for method: " + config.getMode());
                return;
            }

            // Prepare File
            File fileToSend = prepareFileForTransfer(file, config);
            if (fileToSend == null) return;

            // Set Context and Checksum
            String checksum = computeChecksum(fileToSend);
            if (checksum == null) return;
            TransferContext.setIncomingName(fileToSend.getName());
            TransferContext.setExpectedChecksum(checksum);
            TransferContext.setActiveMethod(config.getMode());
            TransferContext.setLastSenderIp(config.getTargetHost());
            TransferContext.setEncryptionEnabled(config.getAesPassword() != null);

            // Start transfer
            long start = System.currentTimeMillis();
            boolean success = false;

            try {
                handler.send(userName, fileToSend, config);
                success = true;
                LoggerUtil.success("🎉 File sent successfully (UI).");
            } catch (Exception e) {
                LoggerUtil.error("Sender UI flow failed", e);
            }

            long durationMs = System.currentTimeMillis() - start;
            logAuditTrail(config.getMode(), config.getTargetHost(), fileToSend, durationMs, success);

        } catch (Exception e) {
            LoggerUtil.error("Sender UI flow crashed", e);
        }
    }


    // ============================================
    // File Preparation (Zip/Encrypt)
    // ============================================
    private File prepareFileForTransfer(File originalFile, TargetConfig config) throws IOException {
        File fileToSend = originalFile;

        // Zip the file if needed
        if (!fileToSend.getName().toLowerCase().endsWith(".zip")) {
            LoggerUtil.info("📦 Zipping file before upload...");
            // Use the same folder as the original file for the zip output
            Path tempDir = originalFile.getParentFile().toPath();
            // FIX: Uses correct ZipUtil signature
            fileToSend = ZipUtil.zipIfNeeded(originalFile, tempDir.toFile());
        }

        // AES encryption if enabled
        String aesPassword = config.getAesPassword();
        if (aesPassword != null && !aesPassword.isBlank()) {
            LoggerUtil.info("🔐 Encrypting file before upload...");
            // Temporary encrypted file will be deleted later by cleanup logic
            File encryptedFile = new File(fileToSend.getAbsolutePath() + ".enc");
            try {
                // FIX: Use correct AesUtil signature for File encryption (requires SecretKey)
                AesUtil.encryptFile(fileToSend, encryptedFile, AesUtil.buildKeyFromPassword(aesPassword));
                fileToSend = encryptedFile;
            } catch (Exception e) {
                LoggerUtil.error("Failed to encrypt file.", e);
                return null;
            }
        }

        long sizeBytes = fileToSend.length();
        double sizeMb = sizeBytes / (1024.0 * 1024.0);
        LoggerUtil.info(String.format("File size: %.2f MB", sizeMb));
        if (sizeMb > 500) {
            LoggerUtil.warn("⚠️ Large file detected. This will be sent in chunks.");
        }

        return fileToSend;
    }

    // ============================================
    // Ask for file and validate
    // ============================================
    private File askForFile(Scanner in) {
        LoggerUtil.info(PromptManager.SENDER_PICK_FILE);
        String path = in.nextLine().trim();
        if (path.isEmpty()) {
            LoggerUtil.warn(ValidationMessages.FILE_REQUIRED);
            return null;
        }
        File f = new File(path);
        // FIX: Use newly implemented validateFile
        String validationError = ValidationUtil.validateFile(f);
        if (validationError != null) {
            LoggerUtil.warn(validationError);
            return null;
        }
        LoggerUtil.info("File chosen: " + f.getName());
        return f;
    }

    // ============================================
    // Ask transport mode
    // ============================================
    private String askTransportMode(Scanner in) {
        LoggerUtil.info(PromptManager.METHOD_QUESTION);
        String modeRaw = in.nextLine().trim().toUpperCase(Locale.ROOT);
        String validationError = ValidationUtil.validateMode(modeRaw);
        if (validationError != null) {
            LoggerUtil.warn(validationError);
            return null;
        }
        return modeRaw;
    }

    // ============================================
    // Ask Target Configuration
    // ============================================
    private TargetConfig askTargetConfig(Scanner in, String mode) {
        String targetHost = null;
        int port = 0;
        // FIX: AuthUtil.askAesPassword() is missing its Scanner dependency in AuthUtil.java, assuming console reads directly from System.in
        String aesPassword = AuthUtil.askAesPassword();

        // 2. Ask Host/Port based on mode
        if ("HTTP".equalsIgnoreCase(mode)) {
            // FIX: Uses correct PromptManager static fields
            LoggerUtil.info(PromptManager.HTTP_TARGET_IP);
            targetHost = in.nextLine().trim();
            if (ValidationUtil.validateHost(targetHost) != null) return TargetConfig.createInvalid();

            // FIX: Uses correct PromptManager static fields
            LoggerUtil.info(PromptManager.HTTP_PORT);
            String portRaw = in.nextLine().trim();
            port = portRaw.isEmpty() ? 8080 : safeParsePort(portRaw);
            if (ValidationUtil.validatePort(port) != null) return TargetConfig.createInvalid();

        } else if ("ZEROTIER".equalsIgnoreCase(mode)) {
            LoggerUtil.info(PromptManager.ZT_PEER_IP);
            targetHost = in.nextLine().trim();
            if (ValidationUtil.validateHost(targetHost) != null) return TargetConfig.createInvalid();

            port = 8080; // ZeroTier will tunnel the HTTP port, 8080 is a safe default

        } else if ("S3".equalsIgnoreCase(mode)) {
            LoggerUtil.info(PromptManager.S3_BUCKET);
            targetHost = in.nextLine().trim();
            if (ValidationUtil.validateS3Bucket(targetHost) != null) return TargetConfig.createInvalid();

            LoggerUtil.info(PromptManager.S3_REGION);
            String region = in.nextLine().trim(); // Collect region but don't stop the flow
            if (ValidationUtil.validateAwsRegion(region) != null) { /* continue flow, validation happens in S3 Service */ }

            port = 0; // Not applicable
        }

        if (targetHost == null) return TargetConfig.createInvalid();

        return TargetConfig.createValid(targetHost, port, mode, aesPassword);
    }

    private int safeParsePort(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ============================================
    // Confirm READY
    // ============================================
    private boolean confirmReady(Scanner in, String fileName) {
        LoggerUtil.info(PromptManager.READY_TO_SEND);
        String answer = in.nextLine().trim();
        return "y".equalsIgnoreCase(answer);
    }

    // ============================================
    // Compute checksum
    // ============================================
    private String computeChecksum(File file) {
        try {
            return HashUtil.sha256Hex(file);
        } catch (Exception e) {
            LoggerUtil.error("Checksum failed.", e);
            return null;
        }
    }

    // ============================================
    // Log audit trail
    // ============================================
    private void logAuditTrail(String mode, String targetHost, File file, long durationMs, boolean success) {
        ActivityLogger.logTransfer(
                mode,
                NetworkUtil.findLocalIp(),
                targetHost,
                file.getName(),
                file.length(),
                durationMs,
                success ? "SUCCESS" : "FAIL",
                success ? "Transfer finished OK" : "Transfer failed or canceled"
        );
    }
}