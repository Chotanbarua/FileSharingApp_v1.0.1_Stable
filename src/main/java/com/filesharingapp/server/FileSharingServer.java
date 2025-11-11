package com.filesharingapp.server;

import com.filesharingapp.utils.LoggerUtil;
import jakarta.servlet.http.HttpServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.ContextHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * FileSharingServer
 * -----------------
 * Lightweight Jetty server used by tests and runtime flows.
 * Provides /health, /handshake, and /api/send-http endpoints.
 *
 * Fully compatible with v1.0.15 (Jetty 11 + Jakarta Servlet API)
 */
public class FileSharingServer {

    private static Server server;
    private static int port = 8080;

    /**
     * Start the Jetty test server on the given port.
     * Called by StepDefinitions and integration tests.
     */
// Inside your startSharedServer() or equivalent method
    // Inside your startSharedServer() or equivalent method
    public static int startSharedServer() {
        try {
            int port = 8080;
            Server server = new Server(port);

            // --- Serve static resources (index.html etc.) ---
            ResourceHandler resourceHandler = new ResourceHandler();
            resourceHandler.setDirectoriesListed(false);
            resourceHandler.setWelcomeFiles(new String[]{"index.html"});
            resourceHandler.setResourceBase("src/main/resources/web");  // path to your web UI folder

            // --- Existing API handler ---
            FileSharingHandler apiHandler = new FileSharingHandler();

            // ✅ Create the context *before* adding prompt servlet
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");

            // ✅ Add this new /prompt endpoint here
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

            // ✅ Combine everything in the correct order
            HandlerList handlers = new HandlerList();
            handlers.addHandler(resourceHandler);
            handlers.addHandler(apiHandler);
            handlers.addHandler(context); // <-- move context LAST so /prompt works

            server.setHandler(handlers);
            server.start();

            LoggerUtil.info("[FileSharingServer] ✅ Running at http://localhost:" + port);
            return port;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to start Jetty server", e);
        }
    }


    public FileSharingServer(int port){
        try{
            server = new Server(port);
            server.setHandler(new FileSharingHandler());
        }catch (Exception e){
            throw new RuntimeException("Failed to initialize FileSharingServer" , e);
        }
    }

    /**
     * Stop the Jetty test server.
     */
    public static void stopServer() {
        try {
            if (server != null && server.isStarted()) {
                server.stop();
                LoggerUtil.info("[FileSharingServer] Server stopped.");

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Minimal embedded handler that serves 3 endpoints:
     *  - /health → "OK"
     *  - /handshake → "READY:<timestamp>"
     *  - /api/send-http → simple placeholder upload confirmation
     */
    private static class FileSharingHandler extends AbstractHandler {
        @Override
        public void handle(String target,
                           Request baseRequest,
                           HttpServletRequest request,
                           HttpServletResponse response) throws IOException {

            response.setContentType("text/plain; charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);

            try (PrintWriter out = response.getWriter()) {
                switch (target) {
                    case "/health":
                        LoggerUtil.info("OK");
                        break;
                    case "/handshake":
                        LoggerUtil.info("Ready: " + System.currentTimeMillis());
                        break;
                    case "/api/send-http":
                        LoggerUtil.info("UPLOAD_OK");
                        break;
                    default:
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        LoggerUtil.info("404 Not Found");

                }
            }

            baseRequest.setHandled(true);
        }
    }


}
