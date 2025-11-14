package com.filesharingapp.transfer;

import com.filesharingapp.security.AesUtil;
import com.filesharingapp.utils.HashUtil;
import com.filesharingapp.utils.LoggerUtil;
import com.filesharingapp.utils.RetryUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * HttpTransferHandler
 * -------------------
 * Baby-English:
 *   ✔ This class does the "hard work" of sending bytes over HTTP.
 *   ✔ Supports:
 *       - Resume (ask server how many bytes it has)
 *       - Chunked mode (optional)
 *       - AES encryption before upload
 *       - Retry with backoff
 *       - Checksum validation after upload
 */
public final class HttpTransferHandler {

    private static final int CHUNK_SIZE = 256 * 1024; // 256 KB
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 15000;

    private HttpTransferHandler() {}

    /**
     * uploadWithResume
     * Baby-English:
     *   ✔ Ask /status for resume offset.
     *   ✔ Encrypt file if AES enabled.
     *   ✔ Send remaining bytes to /upload.
     *   ✔ Verify checksum after upload.
     *
     * @param file       File to upload
     * @param transferId Unique transfer ID
     * @param checksum   SHA-256 checksum
     * @param host       HTTP host
     * @param port       HTTP port
     * @param aesPassword Optional AES password (null if disabled)
     */
    public static void uploadWithResume(File file,
                                        String transferId,
                                        String checksum,
                                        String host,
                                        int port,
                                        String aesPassword) throws Exception {

        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("File missing or invalid");
        }
        if (transferId == null || transferId.isBlank()) {
            throw new IllegalArgumentException("transferId cannot be empty");
        }

        boolean ok = RetryUtil.runWithRetry(() -> {
            try {
                doUploadOnce(file, transferId, checksum, host, port, aesPassword);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, 3, 2000);

        if (!ok) throw new IllegalStateException("HTTP upload failed after retries");
    }

    private static void doUploadOnce(File file,
                                     String transferId,
                                     String checksum,
                                     String host,
                                     int port,
                                     String aesPassword) throws Exception {

        long fileSize = file.length();
        long resumeFrom = queryResumeOffset(transferId, host, port);
        if (resumeFrom < 0 || resumeFrom > fileSize) resumeFrom = 0;

        LoggerUtil.info("🌐 [HTTP] File size=" + fileSize + ", resumeFrom=" + resumeFrom);

        String uploadUrl = "http://" + host + ":" + port + "/upload";
        HttpURLConnection conn = (HttpURLConnection) new URL(uploadUrl).openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");

        // Headers
        conn.setRequestProperty("X-Transfer-Id", transferId);
        conn.setRequestProperty("X-File-Name", file.getName());
        conn.setRequestProperty("X-Total-Bytes", String.valueOf(fileSize));
        conn.setRequestProperty("X-Resume-Offset", String.valueOf(resumeFrom));
        if (checksum != null && !checksum.isBlank()) conn.setRequestProperty("X-Checksum", checksum);
        if (aesPassword != null) conn.setRequestProperty("X-AES-Password", "true");

        conn.setChunkedStreamingMode(CHUNK_SIZE);

        long sent = resumeFrom;

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream rawOut = conn.getOutputStream();
             BufferedOutputStream out = new BufferedOutputStream(rawOut)) {

            // Skip already uploaded part
            long skipped = fis.skip(resumeFrom);
            if (skipped < resumeFrom) {
                LoggerUtil.warn("⚠️ Could not skip resume offset. Restarting from 0.");
                fis.getChannel().position(0);
                sent = 0;
            }

            byte[] buffer = new byte[CHUNK_SIZE];
            int read;
            long lastLogTime = System.currentTimeMillis();

            while ((read = fis.read(buffer)) != -1) {
                byte[] toSend = buffer;
                int len = read;

                // AES encrypt if enabled
                if (aesPassword != null) {
                    byte[] chunk = new byte[read];
                    System.arraycopy(buffer, 0, chunk, 0, read);
                    toSend = AesUtil.encryptChunk(chunk, aesPassword); // Implement encryptChunk in AesUtil
                    len = toSend.length;
                }

                out.write(toSend, 0, len);
                sent += read;

                long now = System.currentTimeMillis();
                if (now - lastLogTime > 1000) {
                    int percent = (int) ((sent * 100) / fileSize);
                    LoggerUtil.info("📤 [HTTP] Progress: " + percent + "% (" + sent + "/" + fileSize + ")");
                    lastLogTime = now;
                }
            }
            out.flush();
        }

        int code = conn.getResponseCode();
        String responseText;
        try (InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream()) {
            responseText = (is != null) ? new String(is.readAllBytes(), StandardCharsets.UTF_8) : "";
        } finally {
            conn.disconnect();
        }

        if (code >= 200 && code < 300) {
            LoggerUtil.success("✅ Upload finished. Server replied: " + responseText);
            verifyChecksumAfterUpload(transferId, host, port, checksum);
        } else {
            LoggerUtil.error("❌ Upload failed: HTTP " + code + " → " + responseText);
            throw new IllegalStateException("Upload failed with status " + code);
        }
    }

    private static long queryResumeOffset(String transferId, String host, int port) {
        try {
            String statusUrl = "http://" + host + ":" + port + "/status?transferId=" +
                    URLEncoder.encode(transferId, StandardCharsets.UTF_8);
            HttpURLConnection conn = (HttpURLConnection) new URL(statusUrl).openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() != 200) return 0L;
            String json;
            try (InputStream is = conn.getInputStream()) {
                json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            } finally {
                conn.disconnect();
            }

            long result = extractLongField(json, "bytesWritten");
            if (result < 0) result = extractLongField(json, "receivedBytes");
            return Math.max(result, 0);
        } catch (Exception e) {
            LoggerUtil.warn("⚠️ Could not query resume offset: " + e.getMessage());
            return 0L;
        }
    }

    private static void verifyChecksumAfterUpload(String transferId, String host, int port, String expectedChecksum) {
        if (expectedChecksum == null || expectedChecksum.isBlank()) return;
        try {
            String statusUrl = "http://" + host + ":" + port + "/status?transferId=" +
                    URLEncoder.encode(transferId, StandardCharsets.UTF_8);
            HttpURLConnection conn = (HttpURLConnection) new URL(statusUrl).openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                String json;
                try (InputStream is = conn.getInputStream()) {
                    json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
                if (json.contains(expectedChecksum)) {
                    LoggerUtil.success("🔒 Checksum verified successfully.");
                } else {
                    LoggerUtil.error("❌ Checksum mismatch after upload.");
                }
            }
        } catch (Exception e) {
            LoggerUtil.warn("⚠️ Could not verify checksum: " + e.getMessage());
        }
    }

    private static long extractLongField(String json, String fieldName) {
        if (json == null || json.isBlank()) return -1L;
        try {
            String key = "\"" + fieldName + "\":";
            int idx = json.indexOf(key);
            if (idx < 0) return -1L;
            int start = idx + key.length();
            int end = start;
            while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
            return Long.parseLong(json.substring(start, end));
        } catch (Exception e) {
            return -1L;
        }
    }
}