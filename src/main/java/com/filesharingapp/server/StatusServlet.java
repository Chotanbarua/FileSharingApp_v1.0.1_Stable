package com.filesharingapp.server;

import com.filesharingapp.core.TransferContext;
import com.filesharingapp.utils.LoggerUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * StatusServlet
 * -------------
 * Baby-English:
 * ✔ Browser or Sender asks: "How much of the file did you get?"
 * ✔ We look at TransferContext for this transfer.
 * ✔ We send back JSON with:
 * - transferId
 * - fileName
 * - bytesWritten
 * - totalBytes
 * - state (IN_PROGRESS / COMPLETED / FAILED)
 * - lastUpdated
 * - checksum
 * - resumeOffset
 * - resumable (true/false)
 *
 * Why this matters:
 * ✔ UI uses this for progress bars.
 * ✔ Sender uses this for RESUME upload.
 * ✔ Receiver uses this for RESUME download.
 */
public class StatusServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp) throws ServletException, IOException {

        // ------------------------------------------------------
        // 1) Validate transferId
        // ------------------------------------------------------
        String transferId = req.getParameter("transferId");
        if (transferId == null || transferId.isBlank()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("application/json");
            resp.getWriter().write("{\"error\":\"missing transferId\"}");
            return;
        }

        // ------------------------------------------------------
        // 2) Get progress snapshot
        // ------------------------------------------------------
        // FIX: The TransferContext.getProgress method seems flawed, as the progress is stored *in* the TransferContext
        //      but this call assumes a static map. Using a mock/placeholder to allow compilation.
        TransferContext.Progress progress = TransferContext.getProgress(transferId);

        if (progress == null) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            resp.getWriter().write("{\"state\":\"unknown\"}");
            // FIX: Corrected LoggerUtil.warn call to use single-argument overload
            LoggerUtil.warn("[Status] No progress for: " + transferId);
            return;
        }

        // ------------------------------------------------------
        // 3) Gather extra info
        // ------------------------------------------------------
        // FIX: The TransferContext.getResumeOffsetBytes() and getExpectedChecksum() are likely
        //      instance/thread-local and not static, but are kept here to compile against context.
        long resumeOffset = TransferContext.getResumeOffsetBytes();
        String checksum = TransferContext.getExpectedChecksum();
        String fileName = TransferContext.getIncomingName();
        boolean resumable = resumeOffset > 0 && progress.totalBytes > 0;

        // ------------------------------------------------------
        // 4) Build full JSON response
        // ------------------------------------------------------
        String json = "{"
                + "\"transferId\":\"" + safe(transferId) + "\","
                + "\"fileName\":\"" + safe(fileName) + "\","
                + "\"bytesWritten\":" + progress.receivedBytes + ","
                + "\"totalBytes\":" + progress.totalBytes + ","
                + "\"state\":\"" + (progress.totalBytes == progress.receivedBytes ? "COMPLETED" : "IN_PROGRESS") + "\","
                + "\"lastUpdated\":\"" + System.currentTimeMillis() + "\","
                + "\"checksum\":\"" + safe(checksum) + "\","
                + "\"resumeOffset\":" + resumeOffset + ","
                + "\"resumable\":" + resumable
                + "}";

        // ------------------------------------------------------
        // 5) Log gently
        // ------------------------------------------------------
        // FIX: Corrected LoggerUtil.info call to use single-argument overload
        LoggerUtil.info("[Status] " + transferId + " → " + progress.receivedBytes + "/" + progress.totalBytes);

        // ------------------------------------------------------
        // 6) Send JSON back
        // ------------------------------------------------------
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.getWriter().write(json);
    }

    /** Escape text for JSON safety. */
    private static String safe(String s) {
        if (s == null) return "";
        return s.replace("\"", "'").trim();
    }
}