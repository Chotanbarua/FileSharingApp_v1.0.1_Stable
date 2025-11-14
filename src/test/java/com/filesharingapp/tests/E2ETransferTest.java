package com.filesharingapp.tests;

import com.filesharingapp.core.Sender;
import com.filesharingapp.core.Receiver;
import com.filesharingapp.transfer.TargetConfig;
import com.filesharingapp.utils.HashUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class E2ETransferTest extends BaseTest {

    // --- Configuration ---
    private static final String USER_NAME = "TestUser";
    private static final int HTTP_PORT = 8080;
    private static final String TARGET_HOST_LOCAL = "127.0.0.1";
    private static final String TARGET_BUCKET = "test-file-sharing-bucket"; // Requires S3 setup

    @Test(dataProvider = "transferMethods", description = "Verify successful end-to-end file transfer and checksum")
    public void testFileTransferSuccess(String method) throws Exception {

        // 1. Setup paths and mock file
        Path tempFilePath = Files.createTempFile("test_file", ".txt");
        Files.writeString(tempFilePath, "This is a test file for " + method + " transfer.");
        File originalFile = tempFilePath.toFile();

        // 2. Determine target configuration
        String host = method.equals("S3") ? TARGET_BUCKET : TARGET_HOST_LOCAL;

        TargetConfig senderConfig = TargetConfig.createValid(
                host,
                HTTP_PORT,
                method,
                null // No AES encryption for simplicity
        );

        // 3. Setup Sender (Upload)
        Sender sender = new Sender();
        // This simulates the sender preparing and initiating the upload to the server (HTTP/ZT) or S3.
        // NOTE: For S3, this requires the target bucket to exist and credentials to be configured.
        sender.runInteractive(USER_NAME, senderConfig, originalFile);

        // 4. Setup Receiver (Download/Verification)
        // Since the server side (UploadServlet) handles the file receipt,
        // the receiver flow typically verifies the file has landed.

        // For E2E, we need a way to mock the receiver's download and verification step.
        // As a simplified check, we verify the integrity of the sent file.
        String expectedChecksum = HashUtil.sha256Hex(originalFile);

        // ***************************************************************
        // REAL RECEIVER SIMULATION:
        // This part needs to be run on the Receiver's machine (or a mock environment)
        // For a true E2E, we must mock the local storage check.
        // ***************************************************************

        // Mock Receiver saving to a temporary folder
        Path receiverSaveFolder = Files.createTempDirectory("receiver_temp");

        Receiver receiver = new Receiver();

        // NOTE: The receiver needs the transfer metadata (name, checksum) set by the sender.
        // This often happens in a separate handshake step or relies on context persistence.

        // Running the receiver flow here simulates the client pulling the file from the source (S3/HTTP).
        // This relies on the file being available in the 'received' directory or S3 bucket.
        receiver.runInteractive(USER_NAME, senderConfig);

        // Assume the transfer name is the original file name (for simplicity, ignoring .zip/.enc extension)
        File receivedFile = receiverSaveFolder.resolve(originalFile.getName()).toFile();

        if (method.equals("HTTP") || method.equals("ZEROTIER")) {
            // In a real HTTP/ZT test, the file lands in the server's 'received' folder first,
            // then Receiver.receive() moves/decrypts it to receiverSaveFolder.
        } else if (method.equals("S3")) {
            // In an S3 test, the file is downloaded directly from S3 to receiverSaveFolder.
        }

        // 5. Assert Integrity
        Assert.assertTrue(receivedFile.exists(), "File should be successfully received by the receiver.");
        String actualChecksum = HashUtil.sha256Hex(receivedFile);

        Assert.assertEquals(actualChecksum, expectedChecksum, "Checksum mismatch! File corruption detected.");

        // 6. Cleanup
        Files.deleteIfExists(tempFilePath);
        Files.deleteIfExists(receivedFile.toPath());
        Files.deleteIfExists(receiverSaveFolder);
    }
}