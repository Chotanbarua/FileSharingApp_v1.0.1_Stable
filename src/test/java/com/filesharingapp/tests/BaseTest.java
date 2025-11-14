package com.filesharingapp.tests;

import com.filesharingapp.server.FileSharingServer;
import com.filesharingapp.core.TransferContext;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;

import java.io.File;

public class BaseTest {

    // --- Server Management ---

    @BeforeSuite
    public void setupSuite() {
        System.out.println("--- Starting Embedded Jetty Server for testing... ---");
        FileSharingServer.startSharedServer();
    }

    @AfterSuite
    public void teardownSuite() {
        System.out.println("--- Stopping Embedded Jetty Server... ---");
        FileSharingServer.stopServer();
    }

    // --- Test Data Provider ---

    @DataProvider(name = "transferMethods")
    public Object[][] transferMethods() {
        // Data provider to run the same test method across multiple protocols
        return new Object[][] {
                {"HTTP"},
                {"ZEROTIER"}, // Requires ZeroTier CLI to be installed/running
                {"S3"}       // Requires AWS credentials in application.properties/env
        };
    }
}