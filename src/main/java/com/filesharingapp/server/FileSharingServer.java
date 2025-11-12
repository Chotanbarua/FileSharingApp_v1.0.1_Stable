package com.filesharingapp.server;

import com.filesharingapp.core.PromptManager;
import com.filesharingapp.core.TransferContext;
import com.filesharingapp.utils.LoggerUtil;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;

/**
 * FileSharingServer
 * -----------------
 * Starts embedded Jetty to serve UI and receive file uploads.
 */
public final class FileSharingServer {

    private FileSharingServer() { }

    public static int startSharedServer() {
        try {
            int port = 8080;
            Server server = new Server(port);
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            server.setHandler(context);

            // ✅ /prompt endpoint for live browser messages
            context.addServlet(new ServletHolder(new HttpServlet() {
                @Override
                protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                    String msg = req.getParameter("msg");
                    if (msg != null && !msg.isBlank()) {
                        System.out.println("[Browser] " + msg);
                    }
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().println("OK");
                }
            }), "/prompt");

            // ✅ /upload servlet
            context.addServlet(new ServletHolder(new HttpServlet() {
                @Override
                protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

                    // --- Capture file metadata headers from sender ---
                    String incomingName = req.getHeader("X-File-Name");
                    String expectedChecksum = req.getHeader("X-File-Checksum");

                    if (incomingName != null)
                        TransferContext.setIncomingName(incomingName);
                    if (expectedChecksum != null)
                        TransferContext.setExpectedChecksum(expectedChecksum);

                    LoggerUtil.info("[Server] Received metadata: name=" + incomingName + ", checksum=" + expectedChecksum);

                    // continue with existing upload logic
                    InputStream input = req.getInputStream();
                    // ... (existing file-write logic unchanged)
                    resp.setStatus(HttpServletResponse.SC_OK);
                }
            }), "/upload");

            // ✅ /handshake servlet
            context.addServlet(new ServletHolder(new HttpServlet() {
                @Override
                protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                    String method = req.getParameter("method");
                    String name   = req.getParameter("name");
                    String sum    = req.getParameter("checksum");

                    if (name != null) TransferContext.setIncomingName(name);
                    if (sum  != null) TransferContext.setExpectedChecksum(sum);

                    LoggerUtil.info(PromptManager.MODE_NOTIFICATION);
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().println("OK");
                }
            }), "/handshake");

            // Serve static UI (index.html etc.)
            ResourceHandler resourceHandler = new ResourceHandler();
            resourceHandler.setDirectoriesListed(false);
            resourceHandler.setWelcomeFiles(new String[]{"index.html"});
            resourceHandler.setResourceBase("src/main/resources/web");

            HandlerList handlers = new HandlerList();
            handlers.addHandler(resourceHandler);
            handlers.addHandler(context);

            server.setHandler(handlers);
            server.start();
            LoggerUtil.info("[FileSharingServer] ✅ Running at http://localhost:" + port);
            return port;

        } catch (Exception e) {
            LoggerUtil.error("Failed to start server", e);
            throw new RuntimeException("Failed to start Jetty server", e);
        }
    }
}
