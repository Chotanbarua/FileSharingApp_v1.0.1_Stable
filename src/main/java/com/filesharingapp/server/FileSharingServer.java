package com.filesharingapp.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.ContextHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

            // Combine both into a handler list
            HandlerList handlers = new HandlerList();
            handlers.addHandler(resourceHandler);
            handlers.addHandler(apiHandler);

            server.setHandler(handlers);
            server.start();

            System.out.println("[FileSharingServer] ✅ Running at http://localhost:" + port);
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
                System.out.println("[FileSharingServer] Server stopped.");
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
                        out.println("OK");
                        break;
                    case "/handshake":
                        out.println("READY:" + System.currentTimeMillis());
                        break;
                    case "/api/send-http":
                        out.println("UPLOAD_OK");
                        break;
                    default:
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        out.println("404 Not Found");
                }
            }

            baseRequest.setHandled(true);
        }
    }
}
