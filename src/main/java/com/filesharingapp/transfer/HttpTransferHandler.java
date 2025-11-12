package com.filesharingapp.transfer;

import com.filesharingapp.utils.HashUtil;
import com.filesharingapp.utils.LoggerUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * HttpTransferHandler
 * -------------------
 * Handles upload/download over HTTP.
 */
public class HttpTransferHandler implements TransferMethod {

    @Override
    public void send(String senderName, File file, String method, int port, String targetHost) throws Exception {
        LoggerUtil.info("📤 Starting HTTP upload to " + targetHost + ":" + port);

        URL url = new URL("http://" + targetHost + ":" + port + "/upload");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");

        // --- Attach file metadata for Receiver ---
        conn.setRequestProperty("X-File-Name", file.getName());
        conn.setRequestProperty("X-File-Checksum", HashUtil.sha256Hex(file));

        try (OutputStream out = conn.getOutputStream();
             FileInputStream in = new FileInputStream(file)) {

            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }

        int responseCode = conn.getResponseCode();
        LoggerUtil.info("Server response: " + responseCode);
    }

    @Override
    public void receive(String savePath) {
        // existing receive logic unchanged
    }
}
