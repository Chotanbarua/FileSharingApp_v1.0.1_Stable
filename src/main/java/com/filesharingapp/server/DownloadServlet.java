package com.filesharingapp.server;

import com.filesharingapp.core.TransferContext;
import com.filesharingapp.security.AesUtil;
import com.filesharingapp.utils.LoggerUtil;
import com.filesharingapp.utils.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * DownloadServlet
 * ---------------
 * Baby-English:
 * ✔ This servlet is the "door" for downloading a file.
 */
public class DownloadServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp) throws ServletException, IOException {

        // ------------------------------------------------------
        // 1) Validate "name" and transfer ID
        // ------------------------------------------------------
        String name = req.getParameter("name");
        String transferId = req.getParameter("transferId");

        if (name == null || name.isBlank() || ValidationUtil.validateName(name) != null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Missing or invalid 'name' parameter");
            return;
        }
        if (transferId == null || transferId.isBlank()) {
            transferId = "download-" + name;
        }

        // ------------------------------------------------------
        // 2) Resolve file path
        // ------------------------------------------------------
        Path file = ChunkUploadService.getReceivedFile(name);
        if (file == null || !Files.exists(file) || !Files.isRegularFile(file)) {
            LoggerUtil.warn("Download requested but file not found: " + name, transferId);
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("File not found");
            return;
        }

        // ------------------------------------------------------
        // 3) Handle AES decryption status
        // ------------------------------------------------------
        String aesPassword = req.getParameter("aesPassword");
        boolean decryptOnServer = aesPassword != null && !aesPassword.isBlank();
        SecretKey aesKey = null;

        if (decryptOnServer) {
            try {
                aesKey = AesUtil.buildKeyFromPassword(aesPassword);
                LoggerUtil.info("[AES] Decrypting on server.", transferId);
            } catch (Exception e) {
                LoggerUtil.error("[AES] Invalid password for server-side decryption.", e, transferId);
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid AES password for server decryption");
                return;
            }
        }

        // ------------------------------------------------------
        // 4) Handle Range header for resume (Only for unencrypted files)
        // ------------------------------------------------------
        String rangeHeader = req.getHeader("Range");
        long startByte = 0;
        long fileSize = Files.size(file);

        if (!decryptOnServer && rangeHeader != null) {
            // FIX: The method is now available in DownloadService
            startByte = DownloadService.parseRange(rangeHeader, fileSize);
        }

        // ------------------------------------------------------
        // 5) Set HTTP headers (simplified, letting DownloadService handle range)
        // ------------------------------------------------------
        String contentType = Files.probeContentType(file);
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        resp.setContentType(contentType);
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + name + "\"");

        // Set checksum header for integrity verification
        String expectedChecksum = TransferContext.getExpectedChecksum();
        if (expectedChecksum != null && !expectedChecksum.isBlank()) {
            resp.setHeader("X-File-Checksum-SHA256", expectedChecksum);
        }

        // ------------------------------------------------------
        // 6) Stream file with resume or streaming AES decrypt
        // ------------------------------------------------------
        try (OutputStream out = resp.getOutputStream()) {
            if (decryptOnServer) {
                // **FIXED LOGIC: Use streaming decryption**
                resp.setStatus(HttpServletResponse.SC_OK);
                // NOTE: AesUtil.IV_LENGTH must be publicly accessible (public static final)
                resp.setContentLengthLong(fileSize - AesUtil.IV_LENGTH);
                try (InputStream fileIn = Files.newInputStream(file)) {
                    // This method reads the IV and decrypts the rest directly to 'out'
                    AesUtil.decryptStream(fileIn, out, aesKey);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                // Unencrypted download with full resume support handled by DownloadService
                DownloadService.streamDownload(transferId, file, rangeHeader, out);
            }
        } catch (IOException e) {
            // If the connection is cut during streaming, this catches it.
            LoggerUtil.error("Download stream failed for file: " + name, e, transferId);
        }
    }
}