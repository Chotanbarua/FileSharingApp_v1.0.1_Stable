package com.filesharingapp.transfer;

import com.filesharingapp.core.TransferContext;
import com.filesharingapp.security.AesUtil;
import com.filesharingapp.utils.HashUtil;
import com.filesharingapp.utils.LoggerUtil;
import com.filesharingapp.utils.NetworkUtil;
import com.filesharingapp.utils.ValidationUtil;
import com.filesharingapp.security.AuthUtil; // FIX: Added missing AuthUtil import

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

/**
 * ZeroTierTransferService
 * -----------------------
 * Baby-English:
 * ✔ This class handles file transfers over ZeroTier (using HTTP over ZT).
 */
public class ZeroTierTransferService implements TransferMethod {

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
        return TransferContext.getResumeOffsetBytes();
    }

    @Override
    public void handshake() throws Exception {
        // ZT Handshake: Ensure network is joined and peer is reachable.
        LoggerUtil.info("[ZeroTier] Handshake: Initiating ZeroTier network checks...");

        String networkId = TransferContext.getZeroTierNetworkId();
        String peerIp = TransferContext.getZeroTierPeerIp();
        // FIX: The port is stored in the TargetConfig and passed during send. For Handshake,
        // we use the default HTTP port or assume it's in context if set by the UI/Controller flow.
        // Assuming controller sets the intended HTTP port into context.
        int port = 8080;

        if (networkId == null || peerIp == null) {
            throw new IllegalStateException("ZeroTier Network ID and Peer IP must be set in context.");
        }

        // FIX: Use the public static method instead of the private method.
        if (!NetworkUtil.isZeroTierInstalled()) {
            throw new IllegalStateException("ZeroTier CLI not found. Install ZeroTier first.");
        }

        if (!joinZeroTierNetwork(networkId)) {
            throw new IllegalStateException("Failed to join ZeroTier network: " + networkId);
        }

        LoggerUtil.info("✅ Joined ZeroTier network: " + networkId);

        if (!NetworkUtil.pingReceiver(peerIp, port)) {
            LoggerUtil.warn("⚠️ Peer not reachable at " + peerIp + ":" + port + ". Transfer may fail.");
        } else {
            LoggerUtil.success("✅ Peer is reachable.");
        }
    }

    @Override
    public void send(String senderName, File file, TargetConfig config) throws Exception {

        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("[ZeroTier] File missing.");
        }

        String peerIp = config.getTargetHost();
        int port = config.getPort();
        String aesPassword = config.getAesPassword();

        // 1) ZeroTier Network Check & Join (Prompts for Network ID are in Controller/UI flow)
        // We rely on the console/UI collecting the network ID and setting it via TransferContext
        // or passing it through TargetConfig (which we'll use for simplicity here).
        // For console flow:
        Scanner in = new Scanner(System.in);
        String networkId = AuthUtil.readWithPrompt("Enter ZeroTier Network ID:", in);
        if (ValidationUtil.validateZeroTierId(networkId) != null) {
            throw new IllegalArgumentException("Invalid ZeroTier Network ID.");
        }
        TransferContext.setZeroTierNetworkId(networkId); // Set for Handshake/Audit

        // 2) Handshake & Pre-checks
        handshake(); // This handles joining and pinging

        // 3) Final file check (file is already zipped/encrypted by Sender.java)
        File finalFile = file;

        // 4) Compute checksum
        String checksum = HashUtil.sha256Hex(finalFile);

        // 5) Update TransferContext
        TransferContext.setIncomingName(finalFile.getName());
        TransferContext.setExpectedChecksum(checksum);
        TransferContext.setActiveMethod("ZEROTIER");
        TransferContext.setEncryptionEnabled(aesPassword != null);
        TransferContext.setZeroTierPeerIp(peerIp);

        // 6) Delegate actual transfer using HTTP handler (ZeroTier routes TCP)
        String transferId = "zt-" + System.currentTimeMillis();
        HttpTransferHandler.uploadWithResume(finalFile, transferId, checksum, peerIp, port, aesPassword);

        LoggerUtil.success("🎉 [ZeroTier] File sent successfully.");
    }

    @Override
    public void receive(String savePath) throws Exception {
        LoggerUtil.info("📥 [ZeroTier] Preparing to receive file...");

        if (ValidationUtil.validateFolder(savePath) != null) {
            throw new IllegalStateException("[ZeroTier] Cannot create save folder: " + savePath);
        }

        // 1) Ensure Handshake was successful (i.e., network is joined)
        handshake();

        String incomingName = TransferContext.getIncomingName();
        if (incomingName == null || incomingName.isBlank()) {
            LoggerUtil.warn("⚠️ No incoming file name found in TransferContext. Did the sender start?");
            return;
        }

        // We assume the file was received via HTTP POST on the local server running over the ZT network
        Path receivedFile = RECEIVED_DIR.resolve(incomingName);
        Path targetFile = Path.of(savePath, incomingName);

        if (!Files.exists(receivedFile)) {
            LoggerUtil.warn("⚠️ File not found in 'received/' folder. Did the sender finish uploading?");
            return;
        }

        Files.copy(receivedFile, targetFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        LoggerUtil.success("✅ File saved to: " + targetFile);
        Files.deleteIfExists(receivedFile); // Cleanup server copy

        // Verification and Decryption logic
        verifyAndDecryptFile(targetFile);
    }

    /**
     * Common verification and decryption logic for receiver side.
     */
    private void verifyAndDecryptFile(Path targetFile) throws Exception {
        // 1. AES decrypt if enabled
        String aesPassword = TransferContext.getAesPassword();
        if (TransferContext.isEncryptionEnabled() && aesPassword != null) {
            LoggerUtil.info("🔐 [ZeroTier] Decrypting file...");
            File decryptedFile = new File(targetFile.toString().replace(".enc", ""));
            // FIX: Use correct AesUtil signature (requires SecretKey)
            AesUtil.decryptFile(targetFile.toFile(), decryptedFile, AesUtil.buildKeyFromPassword(aesPassword));
            Files.deleteIfExists(targetFile);
            targetFile = decryptedFile.toPath();
            LoggerUtil.success("✅ [ZeroTier] File decrypted: " + targetFile.getFileName());
        }

        // 2. Verify checksum
        String expectedChecksum = TransferContext.getExpectedChecksum();
        if (expectedChecksum != null && !expectedChecksum.isBlank()) {
            String actual = HashUtil.sha256Hex(targetFile.toFile());
            if (expectedChecksum.equalsIgnoreCase(actual)) {
                LoggerUtil.success("🔒 [ZeroTier] Checksum OK");
            } else {
                LoggerUtil.error("❌ [ZeroTier] Checksum mismatch! Expected=" + expectedChecksum + " Actual=" + actual);
            }
        }
    }


    /**
     * joinZeroTierNetwork
     * -------------------
     * Baby-English:
     * ✔ Runs "zerotier-cli join <networkId>".
     */
    private boolean joinZeroTierNetwork(String networkId) {
        try {
            LoggerUtil.info("[ZeroTier] Joining network: " + networkId);
            // FIX: Use the public static path getter
            ProcessBuilder pb = new ProcessBuilder(NetworkUtil.getZeroTierCliPath(), "join", networkId);
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            int exit = proc.waitFor();

            if (exit == 0) {
                LoggerUtil.success("[ZeroTier] Joined network successfully.");
                return true;
            } else {
                LoggerUtil.warn("[ZeroTier] Join command failed with exit code: " + exit);
                return false;
            }
        } catch (Exception e) {
            LoggerUtil.warn("[ZeroTier] Could not join network: " + e.getMessage());
            return false;
        }
    }
}