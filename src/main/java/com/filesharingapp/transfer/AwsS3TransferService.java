package com.filesharingapp.transfer;

import com.filesharingapp.core.TransferContext;
import com.filesharingapp.security.AesUtil;
import com.filesharingapp.utils.AppConfig;
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
import java.time.Duration;

/**
 * AwsS3TransferService
 * --------------------
 * Baby-English:
 * ✔ Sender side: Upload file, compute checksum, generate presigned URL.
 * ✔ Receiver side: Download file with resume, verify checksum.
 */
public class AwsS3TransferService implements TransferMethod {

    private static final int BUFFER_SIZE = 8192;
    // NOTE: DEFAULT_REGION initialization is simplified here to avoid configuration complexity.
    // private static final Region DEFAULT_REGION = Region.of(AppConfig.get("aws.s3.region", "us-east-1"));

    // =========================================================================
    // ⬇️ TRANSFERMETHOD IMPLEMENTATION ⬇️
    // =========================================================================

    @Override
    public String computeChecksum(File file) throws Exception {
        return HashUtil.sha256Hex(file);
    }

    @Override
    public long getResumeOffset() throws Exception {
        // S3 resume is handled by checking local file size, which is done in the receive logic.
        return 0;
    }

    @Override
    public void handshake() throws Exception {
        // S3 Handshake: Simply ensures that AWS credentials are available and valid.

        String accessKey = AppConfig.get("aws.accessKey", "").trim();
        String secretKey = AppConfig.get("aws.secretKey", "").trim();

        if (accessKey.isEmpty() || secretKey.isEmpty()) {
            LoggerUtil.error("❌ AWS credentials missing in AppConfig.");
            throw new IllegalStateException("AWS credentials missing. Cannot perform S3 Handshake.");
        }

        LoggerUtil.success("✅ AWS credentials found in AppConfig. Handshake OK.");
    }

    @Override
    public void send(String senderName, File file, TargetConfig config) throws Exception {

        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("File missing or invalid");
        }

        handshake(); // Check credentials

        // Get bucket from TargetConfig
        String bucket = config.getTargetHost();
        if (ValidationUtil.validateS3Bucket(bucket) != null) {
            throw new IllegalArgumentException("Invalid S3 bucket name.");
        }

        String regionCode = AppConfig.get("aws.s3.region", "us-east-1").trim();
        Region region = Region.of(regionCode);

        String accessKey = AppConfig.get("aws.accessKey", "").trim();
        String secretKey = AppConfig.get("aws.secretKey", "").trim();
        AwsBasicCredentials creds = AwsBasicCredentials.create(accessKey, secretKey);

        LoggerUtil.info("🪣 [S3] Using bucket: " + bucket + " in region: " + regionCode);

        try (S3Client s3 = S3Client.builder()
                .region(region)
                .credentialsProvider(StaticCredentialsProvider.create(creds))
                .build()) {

            // Compute checksum
            String sha256 = HashUtil.sha256Hex(file);
            LoggerUtil.info("🧮 [S3] Local SHA-256: " + sha256);

            String key = file.getName();

            // Update TransferContext
            TransferContext.setIncomingName(key);
            TransferContext.setExpectedChecksum(sha256);
            TransferContext.setActiveMethod("S3");
            TransferContext.setEncryptionEnabled(config.getAesPassword() != null);


            // Upload file with checksum metadata
            PutObjectRequest putReq = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .metadata(java.util.Map.of("sha256", sha256, "encrypted", String.valueOf(config.getAesPassword() != null)))
                    .build();

            s3.putObject(putReq, RequestBody.fromFile(file));
            LoggerUtil.success("✅ [S3] Upload completed: s3://" + bucket + "/" + key);

            // Generate presigned URL
            try (S3Presigner presigner = S3Presigner.builder()
                    .region(region)
                    .credentialsProvider(StaticCredentialsProvider.create(creds))
                    .build()) {

                GetObjectRequest getReq = GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build();

                GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
                        .getObjectRequest(getReq)
                        .signatureDuration(Duration.ofHours(12))
                        .build();

                String presignedUrl = presigner.presignGetObject(presignReq).url().toExternalForm();
                LoggerUtil.success("🔗 [S3] Presigned URL (share with receiver): " + presignedUrl);
                TransferContext.setS3DownloadLocation(presignedUrl); // Optional: Store for audit
            }
        }
    }

    @Override
    public void receive(String savePath) throws Exception {
        if (ValidationUtil.validateFolder(savePath) != null) {
            throw new IllegalArgumentException("[S3] Save path validation failed.");
        }

        handshake(); // Check credentials

        String bucket = AppConfig.get("aws.s3.bucket", "").trim();
        String key = TransferContext.getIncomingName(); // Assuming key is set during the Handshake phase
        String regionCode = AppConfig.get("aws.s3.region", "us-east-1").trim();
        String accessKey = AppConfig.get("aws.accessKey", "").trim();
        String secretKey = AppConfig.get("aws.secretKey", "").trim();

        if (bucket.isEmpty() || key.isEmpty()) {
            throw new IllegalStateException("Missing S3 bucket or file key in context.");
        }

        AwsBasicCredentials creds = AwsBasicCredentials.create(accessKey, secretKey);
        Region region = Region.of(regionCode);

        File localFile = new File(savePath, key);

        try (S3Client s3 = S3Client.builder()
                .region(region)
                .credentialsProvider(StaticCredentialsProvider.create(creds))
                .build()) {

            HeadObjectResponse headResp = s3.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());

            long remoteSize = headResp.contentLength();
            String remoteSha = headResp.metadata().get("sha256");
            boolean encrypted = Boolean.parseBoolean(headResp.metadata().get("encrypted"));

            long localSize = localFile.exists() ? localFile.length() : 0L;
            boolean resume = localFile.exists() && localSize > 0 && localSize < remoteSize;

            String rangeHeader = resume ? "bytes=" + localSize + "-" : null;
            LoggerUtil.info(resume ? "🔁 [S3] Resuming download from " + localSize : "⬇️ [S3] Starting full download");

            GetObjectRequest.Builder getReqBuilder = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key);
            if (rangeHeader != null) getReqBuilder.range(rangeHeader);

            try (ResponseInputStream<GetObjectResponse> s3Stream = s3.getObject(getReqBuilder.build());
                 FileOutputStream fos = new FileOutputStream(localFile, resume)) {

                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                long downloaded = localSize;

                while ((read = s3Stream.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                    downloaded += read;
                    // TODO: Update TransferStatusRegistry.addBytes(read);
                    if (remoteSize > 0 && downloaded % (1024 * 1024) < BUFFER_SIZE) {
                        int percent = (int) ((downloaded * 100) / remoteSize);
                        LoggerUtil.info("📥 [S3] Progress: " + percent + "%");
                    }
                }
            }

            LoggerUtil.success("✅ [S3] Download complete: " + localFile.getAbsolutePath());

            // Verification and Decryption logic
            verifyAndDecryptFile(localFile, remoteSha, encrypted);
        }
    }

    /**
     * Common verification and decryption logic for receiver side.
     */
    private void verifyAndDecryptFile(File targetFile, String remoteSha, boolean encrypted) throws Exception {

        // 1. AES decrypt if enabled
        if (encrypted) {
            String aesPassword = TransferContext.getAesKeyFingerprint();
            if (aesPassword == null) {
                LoggerUtil.error("❌ [S3] File is encrypted but no AES password available.");
                return;
            }
            LoggerUtil.info("🔐 [S3] Decrypting file...");
            // Use decrypted file for checksum
            File decryptedFile = new File(targetFile.getAbsolutePath().replace(".enc", ""));
            AesUtil.decryptFile(targetFile, decryptedFile, AesUtil.buildKeyFromPassword(aesPassword));
            Files.deleteIfExists(targetFile.toPath());
            targetFile = decryptedFile;
            LoggerUtil.success("✅ [S3] File decrypted: " + targetFile.getName());
        }

        // 2. Verify checksum
        if (remoteSha != null && !remoteSha.isBlank()) {
            String localSha = HashUtil.sha256Hex(targetFile);
            if (remoteSha.equalsIgnoreCase(localSha)) {
                LoggerUtil.success("🔒 [S3] Checksum OK");
            } else {
                // Fix was likely here, ensuring the string concatenation is correct
                LoggerUtil.error("❌ [S3] Checksum mismatch! Remote=" + remoteSha + ", Local=" + localSha);
            }
        }
    }
}