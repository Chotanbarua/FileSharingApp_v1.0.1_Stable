package com.filesharingapp.server;

import com.filesharingapp.utils.AppConfig;
import com.filesharingapp.utils.LoggerUtil;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 * FileSharingServer
 * -----------------
 * Baby-English:
 *   ✔ This class starts a small Jetty web server for the app.
 *   ✔ What it does:
 *       - Serves upload API → /upload
 *       - Serves download API → /download
 *       - Serves progress API → /status
 *       - Serves prompt API → /prompt
 *       - Serves static files (index.html, JS, CSS) from /web folder
 *
 *   ✔ What it does NOT do:
 *       - No business logic here
 *       - No encryption or chunk merging here
 *
 *   ✔ Why Jetty?
 *       - Lightweight
 *       - Easy to embed
 */
public final class FileSharingServer {

    /** Shared Jetty server instance for this JVM. */
    private static Server server;

    private FileSharingServer() {
        // Utility class – do not create objects.
    }

    /**
     * Start the shared HTTP server if not already running.
     *
     * Baby-English:
     *   ✔ If server is already running → return port.
     *   ✔ If not → read port from config and start Jetty.
     *
     * @return port number where Jetty is listening
     */
    public static synchronized int startSharedServer() {
        if (server != null && server.isStarted()) {
            LoggerUtil.info("[FileSharingServer] Already running.");
            return AppConfig.getInt("app.http.port", 8080);
        }

        final int port = AppConfig.getInt("app.http.port", 8080);

        try {
            // ------------------------------------------------------
            // 1) Configure Jetty thread pool for better performance
            // ------------------------------------------------------
            QueuedThreadPool threadPool = new QueuedThreadPool(50, 10);
            server = new Server(threadPool);
            server.setStopAtShutdown(true);
            server.setConnectors(null); // Jetty will create default connector
            server.setHandler(null);
            server.setStopTimeout(5000);
            server.setAttribute("org.eclipse.jetty.server.Request.maxFormContentSize", 50 * 1024 * 1024); // 50MB

            // Create Jetty server on this port
            server = new Server(port);

            // ------------------------------------------------------
            // 2) Servlet context for APIs
            // ------------------------------------------------------
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");

            // Register servlets
            context.addServlet(new ServletHolder(new UploadServlet()), "/upload");
            context.addServlet(new ServletHolder(new DownloadServlet()), "/download");
            context.addServlet(new ServletHolder(new StatusServlet()), "/status");
            context.addServlet(new ServletHolder(new PromptServlet()), "/prompt");

            // ------------------------------------------------------
            // 3) Static resource handler for index.html + JS/CSS
            // ------------------------------------------------------
            ResourceHandler resourceHandler = new ResourceHandler();
            resourceHandler.setDirectoriesListed(false);
            resourceHandler.setWelcomeFiles(new String[]{"index.html"});
            resourceHandler.setResourceBase("src/main/resources/web");

            // ------------------------------------------------------
            // 4) Combine handlers
            // ------------------------------------------------------
            HandlerList handlers = new HandlerList();
            handlers.addHandler(resourceHandler);
            handlers.addHandler(context);

            server.setHandler(handlers);

            // ------------------------------------------------------
            // 5) Start Jetty
            // ------------------------------------------------------
            server.start();
            LoggerUtil.success("[FileSharingServer] Running at http://localhost:" + port);
            return port;

        } catch (Exception e) {
            LoggerUtil.error("Failed to start Jetty server", e);
            throw new RuntimeException("Failed to start Jetty server", e);
        }
    }

    /**
     * Stop the Jetty server gracefully.
     *
     * Baby-English:
     *   ✔ If running → stop it.
     *   ✔ If not → do nothing.
     */
    public static synchronized void stopServer() {
        if (server != null) {
            try {
                server.stop();
                LoggerUtil.info("[FileSharingServer] Stopped.");
            } catch (Exception e) {
                LoggerUtil.error("Error while stopping Jetty server", e);
            }
        }
    }
}