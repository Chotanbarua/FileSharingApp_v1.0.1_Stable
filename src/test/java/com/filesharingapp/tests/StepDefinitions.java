package com.filesharingapp.tests;

import com.filesharingapp.server.FileSharingServer;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.testng.Assert;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * StepDefinitions
 * ----------------
 * BDD-style smoke test class that verifies the embedded FileSharingServer.
 * Checks that the server starts, responds to /health and /handshake endpoints.
 *
 * Compatible with Jetty 11 + Jakarta Servlet 6.
 */
public class StepDefinitions {

    private int serverPort;

    @Given("the file sharing server is running")
    public void the_server_is_running() throws Exception {
        // Start the Jetty test server on default port 8080
        serverPort = FileSharingServer.startSharedServer();

        System.out.println("[Test] ✅ Server started on port: " + serverPort);
        Assert.assertTrue(serverPort > 0, "Server port must be valid.");

        // Give Jetty a moment to boot
        Thread.sleep(1500);
    }

    @Then("the health endpoint returns OK")
    public void health_returns_ok() throws Exception {
        String body = get("http://localhost:" + serverPort + "/health");
        Assert.assertEquals(body.trim(), "OK", "❌ Health endpoint failed!");
        System.out.println("[Test] 🩺 Health check OK");
    }

    @Then("the handshake endpoint returns READY")
    public void handshake_returns_ready() throws Exception {
        String body = get("http://localhost:" + serverPort + "/handshake");
        Assert.assertTrue(body.startsWith("READY:"), "❌ Handshake endpoint failed!");
        System.out.println("[Test] 🤝 Handshake OK: " + body);
    }

    // ---------------------------------------------------------------
    // Utility: simple GET request helper for health/handshake tests
    // ---------------------------------------------------------------
    private String get(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(3000);

        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
}
