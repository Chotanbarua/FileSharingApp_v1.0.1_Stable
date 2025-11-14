package com.filesharingapp.ui;

import com.filesharingapp.server.FileSharingServer;
import com.filesharingapp.utils.AppConfig;
import com.filesharingapp.utils.LoggerUtil;
import com.filesharingapp.core.PromptManager;

import java.awt.Desktop;
import java.net.URI;

/**
 * WebLauncher
 * -----------
 * Baby English:
 * - We start the web server.
 * - We open the browser for user.
 */
public final class WebLauncher {

    /**
     * FIX: Adds the required public static void main(String[]) method to enable
     * launch via the standard IntelliJ "Application" Run Configuration.
     */
    public static void main(String[] args) {
        launchWebUI();
    }

    private WebLauncher() {
        // Utility class – no objects.
    }

    /**
     * Baby English:
     * - Start Jetty server.
     * - Open browser to index.html.
     */
    public static void launchWebUI() {


        int port = FileSharingServer.startSharedServer();
        if (port <= 0) {
            // FIX: Ensure correct LoggerUtil error signature is used (msg, throwable, transferId)
            LoggerUtil.error("❌ Could not start server. Port invalid.", null, null);
            PromptManager.showError("Server failed to start. Please check logs.");
            return;
        }

        String protocol = AppConfig.getBoolean("app.use.https", false) ? "https" : "http";
        String url = protocol + "://localhost:" + port + "/";

        LoggerUtil.info("🌐 Application running at: " + url, null);
        PromptManager.showInfo("Web UI available at: " + url);

        openBrowser(url);
    }

    /**
     * Baby English:
     * - Try to open browser.
     */
    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
                LoggerUtil.success("🔎 Browser opened: " + url, null);
            } else {
                Runtime.getRuntime().exec("xdg-open " + url);
                LoggerUtil.success("🔎 Browser opened via xdg-open: " + url, null);
            }
        } catch (Exception e) {
            LoggerUtil.warn("⚠ Could not open browser automatically: " + e.getMessage(), null);
            PromptManager.showWarning("Please open manually: " + url);
        }
    }
}