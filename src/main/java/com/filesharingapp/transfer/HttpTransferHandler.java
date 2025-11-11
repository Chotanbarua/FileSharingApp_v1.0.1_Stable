package com.filesharingapp.transfer;

import com.filesharingapp.core.PromptManager;
import com.filesharingapp.security.HashUtil;
import com.filesharingapp.utils.LoggerUtil;
import com.filesharingapp.utils.NetworkUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Real HTTP uploader for FileSharingServer /api/send-http.
 * Includes checksum header for integrity verification.
 */
public class HttpTransferHandler implements TransferMethod {

    @Override
    public void send(String senderName, File file, String method, int port, String host) throws Exception {

        // 🟢 Verify receiver reachability before actual send
        if (!NetworkUtil.pingReceiver(host, port)) {
            LoggerUtil.warn(PromptManager.CONNECTION_RETRY);
            return;
        }

        // 🔸 Existing HTTP logic continues here
        LoggerUtil.info("[HTTP] Starting file upload to " + host + ":" + port);

        if (!file.exists()) throw new IllegalArgumentException("File not found: " + file);
        String urlStr = "http://" + host + ":" + port + "/api/send-http";
        String checksum = HashUtil.sha256Hex(file);

        LoggerUtil.info("[HTTP] Sending to " + urlStr);

        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/octet-stream");
        conn.setRequestProperty("X-Checksum-SHA256", checksum);

        try (OutputStream os = conn.getOutputStream(); FileInputStream fis = new FileInputStream(file)) {
            byte[] buf = new byte[8192];
            int read; long sent = 0, total = file.length();
            while ((read = fis.read(buf)) != -1) {
                os.write(buf, 0, read);
                sent += read;
                int percent = (int)((sent * 100) / total);
                LoggerUtil.info("[HTTP] Upload " + percent + "%");
            }
        }

        if (conn.getResponseCode() == 200)
            LoggerUtil.success("[HTTP] Upload OK ✅");
        else
            LoggerUtil.error("[HTTP] Upload failed: " + conn.getResponseCode());
    }

    @Override
    public void receive(String savePath) {
        LoggerUtil.info("[HTTP] Receiver handled by server endpoint.");
    }
}
