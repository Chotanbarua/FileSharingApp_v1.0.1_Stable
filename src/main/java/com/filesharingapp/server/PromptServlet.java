package com.filesharingapp.server;

import com.filesharingapp.utils.LoggerUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * PromptServlet
 * -------------
 * Baby-English:
 *   ✔ Browser calls this API at /prompt with ?msg=hello.
 *   ✔ We clean the text, log it, and send a friendly response.
 *   ✔ Used for:
 *       - UI notifications
 *       - Transfer start messages
 *       - Resume hints
 *       - ZeroTier/S3 status
 *
 *   ✔ What it does NOT do:
 *       - No file transfer
 *       - No encryption
 */
public class PromptServlet extends HttpServlet {

    /**
     * GET /prompt?msg=hello
     * Baby-English:
     *   ✔ Read message
     *   ✔ Clean message
     *   ✔ Log message
     *   ✔ Reply with JSON
     */
    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp) throws ServletException, IOException {

        // ------------------------------------------------------
        // 1) Read message from browser
        // ------------------------------------------------------
        String msg = req.getParameter("msg");

        // ------------------------------------------------------
        // 2) Clean message:
        //    - Null → empty
        //    - Trim spaces
        //    - Remove risky quotes
        // ------------------------------------------------------
        if (msg == null) msg = "";
        msg = msg.trim().replace("\"", "'");

        // ------------------------------------------------------
        // 3) Log message if not empty
        // ------------------------------------------------------
        if (!msg.isBlank()) {
            LoggerUtil.info("[Browser Prompt] " + msg);
        }

        // ------------------------------------------------------
        // 4) Allow cross-origin (UI may run from file:// or another port)
        // ------------------------------------------------------
        resp.setHeader("Access-Control-Allow-Origin", "*");

        // ------------------------------------------------------
        // 5) Respond with JSON for better UI integration
        // ------------------------------------------------------
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.getWriter().write("{\"status\":\"ok\",\"message\":\"" + escapeJson(msg) + "\"}");
    }

    /**
     * Escape JSON special characters in message.
     */
    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}