package com.filesharingapp.transfer;

import com.filesharingapp.core.PromptManager;
import com.filesharingapp.core.TransferContext;
import com.filesharingapp.security.AesUtil;
import com.filesharingapp.utils.HashUtil;
import com.filesharingapp.utils.LoggerUtil;
import com.filesharingapp.utils.ValidationUtil;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * S3TransferHandler (REDUNDANT HANDLER - FIXED FOR COMPILATION)
 * -----------------
 * Baby-English:
 * ✔ Contains interactive prompts and S3 core functions.
 */
public class S3TransferHandler {

    private static final int BUFFER_SIZE = 8192;

    // ============================
    // SENDER SIDE: Upload to S3
    // ============================
    public static void uploadWithPrompts(File file) throws Exception {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("S3 upload: file missing.");
        }

        // Ask user for S3 details
        String bucket = PromptManager.askUserBucket();
        String regionCode = PromptManager.askUserRegion();
        String accessKey = PromptManager.askUserAccessKey();
        String secretKey = PromptManager.askUserSecretKey();

        bucket = sanitizeBucket(bucket);
        String key = sanitizeKey(file.getName());

        LoggerUtil.info("🪣 Bucket: " + bucket);
        LoggerUtil.info("🌍 Region: " + regionCode);

        Region region = Region.of(regionCode);
        StaticCredentialsProvider provider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
        );

        try (S3Client s3 = S3Client.builder()
                .region(region)
                .credentialsProvider(provider)
                .build()) {

            ensureBucketExists(s3, bucket);

            // Compute checksum
            String checksum = HashUtil.sha256Hex(file);
            LoggerUtil.info("🧮 Local checksum: " + checksum);

            // AES encrypt if enabled
            File finalFile = file;
            if (TransferContext.isEncryptionEnabled()) {
                LoggerUtil.info("🔐 Encrypting file before upload...");
                // FIX: Use correct AesUtil signature (requires SecretKey)
                File encryptedFile = new File(file.getAbsolutePath() + ".enc");
                AesUtil.encryptFile(file, encryptedFile, AesUtil.buildKeyFromPassword(TransferContext.getAesPassword()));
                finalFile = encryptedFile;
            }

            // Upload with metadata
            PutObjectRequest putReq = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .metadata(java.util.Map.of(
                            "sha256", checksum,
                            "encrypted", String.valueOf(TransferContext.isEncryptionEnabled())))
                    .build();

            s3.putObject(putReq, RequestBody.fromFile(finalFile));
            LoggerUtil.success("✅ Uploaded to s3://" + bucket + "/" + key);

            // Generate presigned URL
            try (S3Presigner presigner = S3Presigner.builder()
                    .credentialsProvider(provider)
                    .region(region)
                    .build()) {

                GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofHours(12))
                        .getObjectRequest(GetObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .build())
                        .build();

                String url = presigner.presignGetObject(presignReq).url().toString();
                LoggerUtil.success("🔗 SHARE THIS PRESIGNED URL WITH RECEIVER:");
                LoggerUtil.success(url);

                TransferContext.setIncomingName(key);
                TransferContext.setExpectedChecksum(checksum);
                TransferContext.setActiveMethod("S3");
            }
        }
    }

    // ============================
    // RECEIVER SIDE: Download from S3
    // ============================
    public static void downloadWithPrompts(String saveFolder) throws Exception {

        // FIX 2: Replaced non-existent ensureFolder with correct validation logic
        if (ValidationUtil.validateFolder(saveFolder) != null) {
            throw new IllegalStateException("Cannot use save folder");
        }

        String bucket = PromptManager.askUserBucket();
        String regionCode = PromptManager.askUserRegion();
        String accessKey = PromptManager.askUserAccessKey();
        String secretKey = PromptManager.askUserSecretKey();
        String key = PromptManager.askUserObjectKey();

        bucket = sanitizeBucket(bucket);
        key = sanitizeKey(key);

        Region region = Region.of(regionCode);
        StaticCredentialsProvider provider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
        );

        try (S3Client s3 = S3Client.builder()
                .credentialsProvider(provider)
                .region(region)
                .build()) {

            HeadObjectResponse head = s3.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());

            long remoteSize = head.contentLength();
            String remoteSha = head.metadata().get("sha256");
            boolean encrypted = Boolean.parseBoolean(head.metadata().get("encrypted"));

            LoggerUtil.info("ℹ️ Remote size: " + remoteSize);
            LoggerUtil.info("ℹ️ Remote checksum: " + remoteSha);

            Path localFile = Path.of(saveFolder, key);
            long localSize = Files.exists(localFile) ? localFile.toFile().length() : 0;
            boolean resume = localSize > 0 && localSize < remoteSize;

            LoggerUtil.info(resume ? "🔁 Resuming download from " + localSize : "⬇️ Starting fresh download");

            GetObjectRequest.Builder getReqBuilder = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key);
            if (resume) getReqBuilder.range("bytes=" + localSize + "-");

            try (ResponseInputStream<GetObjectResponse> s3Stream = s3.getObject(getReqBuilder.build());
                 FileOutputStream fos = new FileOutputStream(localFile.toFile(), resume)) {

                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                long downloaded = localSize;

                while ((read = s3Stream.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                    downloaded += read;

                    int percent = (int) ((downloaded * 100) / remoteSize);
                    if (percent % 10 == 0) LoggerUtil.info("📥 Download progress: " + percent + "%");
                }
            }

            LoggerUtil.success("✅ Download complete: " + localFile);

            File finalFile = localFile.toFile(); // Start with the local file

            if (encrypted) {
                LoggerUtil.info("🔓 Decrypting file...");

                // 1. Define the decrypted output file
                File decryptedFile = new File(finalFile.getAbsolutePath().replace(".enc", ""));

                // 2. Perform the decryption (void return)
                AesUtil.decryptFile(
                        finalFile, // inputFile (encrypted)
                        decryptedFile, // outputFile (decrypted)
                        AesUtil.buildKeyFromPassword(TransferContext.getAesPassword()) // Key
                );

                // 3. Update 'finalFile' to point to the newly created decrypted file
                finalFile = decryptedFile;

                LoggerUtil.success("✅ File decrypted: " + finalFile.getAbsolutePath());
            }

            if (remoteSha != null) {
                String localSha = HashUtil.sha256Hex(finalFile);
                if (remoteSha.equalsIgnoreCase(localSha)) {
                    LoggerUtil.success("🔒 Checksum OK!");
                } else {
                    LoggerUtil.error("❌ Checksum mismatch! Remote=" + remoteSha + ", Local=" + localSha);
                }
            }
        }
    }

    // ============================
    // HELPER METHODS
    // ============================
    private static String sanitizeBucket(String bucket) {
        return bucket.trim().replace(" ", "").toLowerCase();
    }

    private static String sanitizeKey(String key) {
        key = key.trim().replace("\\", "/");
        if (key.contains("..")) key = key.replace("..", "");
        while (key.startsWith("/")) key = key.substring(1);
        return key;
    }

    private static void ensureBucketExists(S3Client s3, String bucket) {
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException nsb) {
            LoggerUtil.warn("Bucket does not exist… creating: " + bucket);
            s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            LoggerUtil.success("Bucket created: " + bucket);
        }
    }
}