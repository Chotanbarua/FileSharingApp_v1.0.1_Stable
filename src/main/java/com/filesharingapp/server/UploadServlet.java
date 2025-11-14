package com.filesharingapp.server;

import com.filesharingapp.core.TransferContext;
import com.filesharingapp.security.AesUtil;
import com.filesharingapp.utils.LoggerUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * UploadServlet
 * -------------
 * Baby-English:
 *   ✔ Browser or Sender POSTs file data to /upload.
 *   ✔ Supports:
 *       - Chunk mode (many small pieces)
 *       - Stream mode (one big upload)
 *       - Resume using Range or X-Resume-Offset
 *       - AES decrypt if enabled
 *       - JSON response for UI
 */
public class UploadServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req,
                          HttpServletResponse resp) throws ServletException, IOException {

        resp.setContentType("application/json");

        // ------------------------------------------------------
        // 1) Read metadata from query params or headers
        // ------------------------------------------------------
        String transferId = firstNonBlank(req.getParameter("transferId"), req.getHeader("X-Transfer-Id"));
        String fileName = firstNonBlank(req.getParameter("fileName"), req.getHeader("X-File-Name"));
        String chunkIndexStr = firstNonBlank(req.getParameter("chunkIndex"), req.getHeader("X-Chunk-Index"));
        String totalBytesStr = firstNonBlank(req.getParameter("totalBytes"), req.getHeader("X-Total-Bytes"));
        String checksum = firstNonBlank(req.getParameter("checksum"), req.getHeader("X-Checksum"));
        String resumeOffsetStr = req.getHeader("X-Resume-Offset");
        String aesPassword = req.getHeader("X-AES-Password"); // optional AES key

        // ------------------------------------------------------
        // 2) Validate required fields
        // ------------------------------------------------------
        if (isBlank(transferId) || isBlank(fileName) || isBlank(totalBytesStr)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"status\":\"error\",\"message\":\"Missing transferId, fileName, or totalBytes\"}");
            return;
        }

        long totalBytes;
        try {
            totalBytes = Long.parseLong(totalBytesStr);
            if (totalBytes < 0) throw new NumberFormatException("negative");
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"status\":\"error\",\"message\":\"Invalid totalBytes\"}");
            return;
        }

        // ------------------------------------------------------
        // 3) Sanitize file name
        // ------------------------------------------------------
        String safeFileName = sanitizeFileName(fileName);
        if (safeFileName == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"status\":\"error\",\"message\":\"Invalid file name\"}");
            return;
        }

        // ------------------------------------------------------
        // 4) Parse resume offset
        // ------------------------------------------------------
        long resumeOffset = 0L;
        if (!isBlank(resumeOffsetStr)) {
            try {
                resumeOffset = Long.parseLong(resumeOffsetStr);
                if (resumeOffset < 0) resumeOffset = 0;
            } catch (NumberFormatException ignored) {
                resumeOffset = 0;
            }
        }

        boolean isChunkMode = (chunkIndexStr != null);
        int chunkIndex = 0;
        if (isChunkMode) {
            try {
                chunkIndex = Integer.parseInt(chunkIndexStr);
                if (chunkIndex < 0) throw new NumberFormatException("negative chunk index");
            } catch (NumberFormatException ex) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"status\":\"error\",\"message\":\"Invalid chunkIndex\"}");
                return;
            }
        }

        // ------------------------------------------------------
        // 5) Initialize TransferStatusRegistry
        // ------------------------------------------------------
        if (!isChunkMode || chunkIndex == 0) {
            TransferStatusRegistry.begin(transferId, safeFileName, totalBytes);
        }
        TransferStatusRegistry.setProtocol("HTTP");
        TransferStatusRegistry.setResumeOffset(resumeOffset);
        TransferStatusRegistry.setAesEnabled(aesPassword != null);
        if (!isBlank(checksum)) {
            TransferStatusRegistry.setChecksum(checksum);
            TransferContext.setExpectedChecksum(checksum);
        }
        TransferContext.setIncomingName(safeFileName);
        TransferContext.setActiveMethod("HTTP");
        TransferContext.setLastSenderIp(req.getRemoteAddr());

        // ------------------------------------------------------
        // 6) Handle upload body
        // ------------------------------------------------------
        try {
            if (!isChunkMode) {
                // ======================
                // STREAM MODE
                // ======================
                Path saved = ChunkUploadService.handleStream(
                        transferId,
                        safeFileName,
                        totalBytes,
                        req.getInputStream(),
                        aesPassword // decrypt if AES enabled
                );

                TransferStatusRegistry.complete(saved.toAbsolutePath().toString());

                String json = "{"
                        + "\"status\":\"ok\","
                        + "\"merged\":true,"
                        + "\"received\":" + totalBytes + ","
                        + "\"resumeFrom\":0,"
                        + "\"transferId\":\"" + escapeJson(transferId) + "\""
                        + "}";
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(json);
                return;

            } else {
                // ======================
                // CHUNK MODE
                // ======================
                byte[] body;
                try (InputStream in = req.getInputStream();
                     ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = in.read(buf)) != -1) {
                        bos.write(buf, 0, len);
                    }
                    body = bos.toByteArray();
                }

                // AES decrypt if enabled
                if (aesPassword != null) {
                    try {
                        body = AesUtil.decrypt(body, aesPassword);
                    } catch (Exception e) {
                        LoggerUtil.error("[AES] Chunk decryption failed", e);
                        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        resp.getWriter().write("{\"status\":\"error\",\"message\":\"AES decryption failed\"}");
                        return;
                    }
                }

                String result = ChunkUploadService.handleChunk(
                        transferId,
                        safeFileName,
                        chunkIndex,
                        totalBytes,
                        body,
                        aesPassword
                );

                TransferStatusRegistry.addBytes(body.length);
                boolean merged = "MERGED".equalsIgnoreCase(result);
                if (merged) {
                    TransferStatusRegistry.complete(Path.of("received", safeFileName).toAbsolutePath().toString());
                }

                long receivedNow = TransferStatusRegistry.getBytesWritten();
                String json = "{"
                        + "\"status\":\"ok\","
                        + "\"merged\":" + merged + ","
                        + "\"received\":" + receivedNow + ","
                        + "\"resumeFrom\":" + resumeOffset + ","
                        + "\"transferId\":\"" + escapeJson(transferId) + "\""
                        + "}";
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(json);
            }

        } catch (Exception ex) {
            LoggerUtil.error("Upload failed", ex);
            TransferStatusRegistry.fail(ex.getMessage());
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"status\":\"error\",\"message\":\"Upload failed on server\"}");
        }
    }

    // ====================
    // Helper methods
    // ====================
    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.trim().isEmpty()) return a.trim();
        if (b != null && !b.trim().isEmpty()) return b.trim();
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String sanitizeFileName(String fileName) {
        if (fileName == null) return null;
        String trimmed = fileName.trim().replace("\\", "/");
        int lastSlash = trimmed.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < trimmed.length() - 1) {
            trimmed = trimmed.substring(lastSlash + 1);
        }
        if (trimmed.contains("..") || trimmed.contains("/") || trimmed.contains("\\")) return null;
        return trimmed;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").trim();
    }
}
