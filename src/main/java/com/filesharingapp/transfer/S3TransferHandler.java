package com.filesharingapp.transfer;

import com.filesharingapp.core.PromptManager;
import com.filesharingapp.utils.AppConfig;
import com.filesharingapp.utils.LoggerUtil;
import com.filesharingapp.utils.NetworkUtil;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.nio.file.Paths;

/** Real AWS S3 integration. */
public class S3TransferHandler implements TransferMethod {
    private final String bucket = AppConfig.get("aws.s3.bucket", "");
    private final Region region = Region.of(AppConfig.get("aws.s3.region", "us-east-1"));

    @Override
    public void send(String sender, File file, String method, int port, String host) throws Exception {
        // 🟢 Verify receiver reachability before upload (simple network check)
        if (!NetworkUtil.pingReceiver(host, port)) {
            LoggerUtil.warn(PromptManager.CONNECTION_RETRY);
            return;
        }

        try (S3Client s3 = S3Client.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build()) {

            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(file.getName())
                            .build(),
                    RequestBody.fromFile(Paths.get(file.getPath()))
            );

            LoggerUtil.success("[S3] ✅ Uploaded to s3://" + bucket + "/" + file.getName());
        }
    }

    @Override
    public void receive(String path) throws Exception {
        String key = AppConfig.get("aws.s3.download.key", "");
        if (key.isEmpty()) return;
        try (S3Client s3 = S3Client.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build()) {
            s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build(),
                    Paths.get(path, key));
            LoggerUtil.success("[S3] Downloaded " + key);
        }
    }
}
