package com.filesharingapp.core;

import com.filesharingapp.transfer.TransferFactory;
import com.filesharingapp.transfer.TransferMethod;
import com.filesharingapp.utils.HashUtil;
import com.filesharingapp.utils.LoggerUtil;
import com.filesharingapp.utils.NetworkUtil;

import java.io.File;
import java.util.Scanner;

/**
 * Sender
 * ------
 * Handles file selection, metadata broadcast, and upload initiation.
 */
public class Sender {

    private TransferMethod currentTransferHandler;

    public void runInteractive() {
        try (Scanner in = new Scanner(System.in)) {
            LoggerUtil.info("✅ Sender mode initialized.");

            // --- Choose file to send ---
            LoggerUtil.info("Enter full file path to send:");
            String path = in.nextLine().trim();
            File file = new File(path);
            if (!file.exists() || !file.isFile()) {
                LoggerUtil.error("❌ File not found: " + path);
                return;
            }

            LoggerUtil.info("Choose transport method (HTTP / ZeroTier / S3):");
            String method = in.nextLine().trim();
            currentTransferHandler = TransferFactory.create(method);

            if (currentTransferHandler == null) {
                LoggerUtil.error("❌ Invalid or unsupported method: " + method);
                return;
            }

            LoggerUtil.info("📡 Using transport: " + method);

            // ✅ Notify receiver about transfer method and metadata
            String filename = file.getName();
            String checksum = HashUtil.sha256Hex(file);

            LoggerUtil.info("🧾 File name: " + filename + " | SHA256: " + checksum);

            // --- Ask for target host and port ---
            LoggerUtil.info("Enter receiver’s IP or hostname:");
            String targetHost = in.nextLine().trim();

            LoggerUtil.info("Enter receiver port (default 8080):");
            String portInput = in.nextLine().trim();
            int port = portInput.isEmpty() ? 8080 : Integer.parseInt(portInput);

            // ✅ Log ZeroTier status if configured (optional but helpful)
            NetworkUtil.logZeroTierStatusIfConfigured();

            // ✅ Send handshake metadata to receiver
            NetworkUtil.broadcastModeToReceiver(method, targetHost, port, filename, checksum);

            // --- Small connection check before send ---
            if (!NetworkUtil.canConnect(targetHost, port, 2000)) {
                LoggerUtil.warn("⚠️ Receiver not reachable. Check network or ZeroTier connection.");
                return;
            }

            LoggerUtil.info("🚀 Starting transfer to " + targetHost + ":" + port);

            // --- Perform send ---
            currentTransferHandler.send(System.getProperty("user.name"), file, method, port, targetHost);

            LoggerUtil.success("🎉 File sent successfully to " + targetHost);

        } catch (Exception e) {
            LoggerUtil.error("Sender flow failed", e);
        }
    }
}
