package com.filesharingapp.transfer;

import com.filesharingapp.core.TransferContext;
import com.filesharingapp.security.AesUtil;
import com.filesharingapp.utils.HashUtil;
import com.filesharingapp.utils.LoggerUtil;
import com.filesharingapp.utils.NetworkUtil;
import com.filesharingapp.utils.ValidationUtil;
import com.filesharingapp.utils.ZipUtil; // Import ZipUtil

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * HttpTransferService
 * -------------------
 * Baby-English:
 * ✔ High-level HTTP helper for Sender + Receiver.
 */
public class HttpTransferService implements TransferMethod {

    private static final Path RECEIVED_DIR = Path.of("received");

    // =========================================================================
    // ⬇️ TRANSFERMETHOD IMPLEMENTATION ⬇️
    // =========================================================================

    @Override
    public String computeChecksum(File file) throws Exception {
        return HashUtil.sha256Hex(file);
    }

    @Override
    public long getResumeOffset() throws Exception {
        // HTTP resume relies on the receiver asking the server via /status
        // We can't determine the offset without a TargetConfig on the receiver side.
        // The implementation relies on the receiver checking the local file size and setting TransferContext.
        return TransferContext.getResumeOffsetBytes();
    }

    @Override
    public void handshake() throws Exception {
        // HTTP Handshake: This is often implicit or done via a simple ping/status check.
        // Full handshake logic is complex and will be deferred to the Handler/Controller flow.
        LoggerUtil.info("[HTTP] Handshake: Check for receiver reachability...");
        // This is where a more complex initial data exchange would happen.
    }

    @Override
    public void send(String senderName, File file, TargetConfig config) throws Exception {

        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("[HTTP] send: file is missing or invalid");
        }

        // This file is already zipped/encrypted by Sender.java
        File finalFile = file;

        String host = config.getTargetHost();
        int effectivePort = config.getPort();
        String aesPassword = config.getAesPassword();

        if (ValidationUtil.validateHost(host) != null) {
            throw new IllegalArgumentException("[HTTP] Invalid host/IP: " + host);
        }
        if (ValidationUtil.validatePort(effectivePort) != null) {
            throw new IllegalArgumentException("[HTTP] Invalid port: " + effectivePort);
        }

        LoggerUtil.info("🌐 [HTTP] Preparing upload to " + host + ":" + effectivePort);

        // Compute checksum
        String checksum = HashUtil.sha256Hex(finalFile);
        LoggerUtil.info("🧮 [HTTP] SHA-256 checksum: " + checksum);

        // Build transferId
        String prefix = checksum.length() >= 12 ? checksum.substring(0, 12) : checksum;
        String transferId = "http-" + prefix + "-" + System.currentTimeMillis();

        // Update TransferContext
        TransferContext.setIncomingName(finalFile.getName());
        TransferContext.setExpectedChecksum(checksum);
        TransferContext.setActiveMethod("HTTP");
        TransferContext.setEncryptionEnabled(aesPassword != null);

        // Upload with resume logic handled by HttpTransferHandler
        HttpTransferHandler.uploadWithResume(finalFile, transferId, checksum, host, effectivePort, aesPassword);

        LoggerUtil.success("🎉 [HTTP] Sender finished HTTP transfer for file: " + finalFile.getName());
    }

    @Override
    public void receive(String savePath) throws Exception {
        if (savePath == null || savePath.isBlank()) {
            throw new IllegalArgumentException("[HTTP] receive: savePath cannot be empty");
        }

        if (ValidationUtil.validateFolder(savePath) != null) {
            LoggerUtil.warn("❌ [HTTP] Cannot use save folder: " + savePath);
            return;
        }

        String incomingName = TransferContext.getIncomingName();
        if (incomingName == null || incomingName.isBlank()) {
            LoggerUtil.warn("⚠️ [HTTP] No incoming file name in TransferContext.");
            return;
        }

        // Final file path on the server
        Path serverFile = RECEIVED_DIR.resolve(incomingName);

        // Final file path for the user
        Path targetFile = Path.of(savePath).resolve(incomingName);

        if (!Files.exists(serverFile)) {
            LoggerUtil.warn("⚠️ [HTTP] Expected file not found in 'received/' folder: " + serverFile);
            // This happens if the receiver started the server but the sender hasn't finished uploading.
            return;
        }

        LoggerUtil.info("📁 [HTTP] Moving file from server temp to user folder: " + targetFile);

        // Move the file and handle decryption/checksum on the received file.
        // Since DownloadServlet handled the transfer, this method just performs cleanup and verification.
        Files.copy(serverFile, targetFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        LoggerUtil.success("✅ [HTTP] File placed at: " + targetFile);
        Files.deleteIfExists(serverFile); // Clean up server received file

        // Verification and Decryption logic
        verifyAndDecryptFile(targetFile);
    }

    /**
     * Common verification and decryption logic for receiver side.
     */
    private void verifyAndDecryptFile(Path targetFile) throws Exception {

        // 1. AES decrypt if enabled
        String aesPassword = TransferContext.getAesKeyFingerprint(); // Assuming AES fingerprint is the password itself
        if (TransferContext.isEncryptionEnabled() && aesPassword != null) {
            LoggerUtil.info("🔐 [HTTP] Decrypting file...");
            // Decrypts in place or creates a new file based on AesUtil implementation
            File decryptedFile = new File(targetFile.toString().replace(".enc", ""));
            AesUtil.decryptFile(targetFile.toFile(), decryptedFile, AesUtil.buildKeyFromPassword(aesPassword));
            Files.deleteIfExists(targetFile); // Delete encrypted file
            targetFile = decryptedFile.toPath(); // Use decrypted file for checksum
            LoggerUtil.success("✅ [HTTP] File decrypted: " + targetFile.getFileName());
        }

        // 2. Verify checksum
        String expectedChecksum = TransferContext.getExpectedChecksum();
        if (expectedChecksum != null && !expectedChecksum.isBlank()) {
            String actual = HashUtil.sha256Hex(targetFile.toFile());
            if (expectedChecksum.equalsIgnoreCase(actual)) {
                LoggerUtil.success("🔒 [HTTP] Checksum OK");
            } else {
                LoggerUtil.error("❌ [HTTP] Checksum mismatch! Expected=" + expectedChecksum + " Actual=" + actual);
            }
        }
    }
}