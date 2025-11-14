package com.filesharingapp.core;

import com.filesharingapp.transfer.TargetConfig;
import com.filesharingapp.transfer.TransferFactory;
import com.filesharingapp.transfer.TransferMethod;
import com.filesharingapp.utils.LoggerUtil;
import com.filesharingapp.utils.ValidationUtil;
import com.filesharingapp.utils.ValidationMessages; // FIX: Added missing import

import java.io.File;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Scanner;

/**
 * Receiver
 * --------
 * Baby-English:
 * - This class helps the person who is getting the file.
 * - It orchestrates the receive process, including the crucial resume check.
 */
public class Receiver {

    /** The active transfer handler (HTTP / ZeroTier / S3). */
    private TransferMethod currentTransferHandler;

    // Default save path
    private static final String DEFAULT_SAVE_PATH = System.getProperty("user.home") + File.separator + "Downloads";


    /**
     * Start the interactive receiver flow (console or UI-driven).
     *
     * @param userName    name of the person using the app
     * @param config      TargetConfig from UI/Console (only mode is relevant for Receiver)
     */
    public void runInteractive(String userName, TargetConfig config) {
        // Use a single Scanner for interactive console input (e.g., resume prompt)
        Scanner in = new Scanner(System.in);

        try {
            // -------------------------------
            // 1) Say hello and log who we are
            // -------------------------------
            LoggerUtil.success(PromptManager.RECEIVER_INIT_OK, null);
            LoggerUtil.info("[Receiver] User: " + userName + " | Mode: " + config.getMode(), null);
            TransferContext.setActiveMethod(config.getMode());

            // -------------------------------
            // 2) Create transfer handler
            // -------------------------------
            currentTransferHandler = TransferFactory.create(config.getMode());
            if (currentTransferHandler == null) {
                LoggerUtil.error("❌ No transfer handler available for mode: " + config.getMode(), null, null);
                return;
            }

            // -------------------------------
            // 3) Ask where to save file
            // -------------------------------
            String saveFolder = askSaveFolder(in);
            if (saveFolder == null) return;

            // -------------------------------
            // 4) Perform handshake (get metadata: name, size, checksum)
            // -------------------------------
            // Note: Handshake implementation is currently a stub in services.
            currentTransferHandler.handshake();
            LoggerUtil.info("Handshake complete. Incoming file: " + TransferContext.getIncomingName(), null);

            // -------------------------------
            // 5) Resume check (critical BRD requirement)
            // -------------------------------
            checkAndSetResumeOffset(in, saveFolder);

            // -------------------------------
            // 6) Call transfer engine
            // -------------------------------
            LoggerUtil.info(PromptManager.RECEIVER_READY, null);
            currentTransferHandler.receive(saveFolder);

            // -------------------------------
            // 7) Goodbye message
            // -------------------------------
            LoggerUtil.success(PromptManager.UI_TRANSFER_DONE, null);

        } catch (Exception e) {
            LoggerUtil.error("Receiver flow failed", e, null);
        }
    }

    // ============================================
    // Check for partial file and set offset
    // ============================================
    private void checkAndSetResumeOffset(Scanner in, String saveFolder) {
        String fileName = TransferContext.getIncomingName();
        if (fileName == null || fileName.isBlank()) return;

        Path partialPath = Path.of(saveFolder, fileName);
        File partialFile = partialPath.toFile();

        if (partialFile.exists() && partialFile.isFile() && partialFile.length() > 0) {
            long currentSize = partialFile.length();
            LoggerUtil.warn("Partial file found: " + fileName + " (" + currentSize + " bytes)", null);

            LoggerUtil.info(PromptManager.RESUME_FOUND, null);

            // Prompt user for console input (or assume 'Y' for UI automation)
            String answer = in.nextLine().trim().toUpperCase(Locale.ROOT);

            if ("Y".equals(answer)) {
                TransferContext.setResumeOffsetBytes(currentSize);
                LoggerUtil.success("Resuming download from offset: " + currentSize, null);
            } else {
                TransferContext.deletePartialFile(partialPath.toString());
                TransferContext.setResumeOffsetBytes(0);
                LoggerUtil.info("Partial file deleted. Starting new transfer.", null);
            }
        }
    }


    // ============================================
    // Ask save folder and validate
    // ============================================
    private String askSaveFolder(Scanner in) {
        // Use console prompt if running interactively
        LoggerUtil.info(PromptManager.RECEIVER_CHOOSE_FOLDER, null);
        String path = in.nextLine().trim();

        if (path.isEmpty()) {
            path = DEFAULT_SAVE_PATH;
        }

        String validationError = ValidationUtil.validateFolder(path);
        if (validationError != null) {
            LoggerUtil.warn(validationError, null);
            return null;
        }

        LoggerUtil.info("Save folder OK: " + path, null);
        return path;
    }
}