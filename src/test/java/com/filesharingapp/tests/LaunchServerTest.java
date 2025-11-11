package com.filesharingapp.tests;

import com.filesharingapp.server.FileSharingServer;
import org.testng.annotations.Test;

public class LaunchServerTest {

    @Test
    public void startServerAndUi() throws Exception {
        System.out.println("[Test] 🚀 Starting File Sharing Server...");
        int port = FileSharingServer.startSharedServer();

        System.out.println("[Test] ✅ Server started on port: " + port);

        // Give Jetty a moment to boot
        Thread.sleep(1500);

        // 🔥 Launch browser automatically
        try {
            String url = "http://localhost:" + port;
            System.out.println("[Test] 🌐 Opening browser at: " + url);
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Optional: keep test alive for manual browser inspection
        Thread.sleep(5000);
    }
}
