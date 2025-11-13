package com.filesharingapp.tests.tests;

import com.filesharingapp.tests.base.TestBase;
import com.filesharingapp.tests.pages.IndexPage;
import com.filesharingapp.tests.utils.ConfigReader;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * SmokeUITest
 * -----------
 * Simple sanity test using the Page Object.
 *
 * This class is used by FactoryRunner to run on multiple browsers.
 *
 * It reads test data from config.properties so you don't hardcode
 * values inside Java.
 */
public class SmokeUITest extends TestBase {

    /** Constructor accepts browser from FactoryRunner. */
    public SmokeUITest(String browser) {
        super(browser);
    }

    @Test
    public void fillSenderAndValidateStatus() {
        // setUp() is called automatically by TestBase via @BeforeMethod.

        IndexPage page = new IndexPage();

        // Read data from config (with defaults that match your feature file).
        String name   = ConfigReader.get("senderName", "Syed");
        String role   = ConfigReader.get("senderRole", "Sender");
        String method = ConfigReader.get("senderMethod", "HTTP (Simple / LAN)");
        String target = ConfigReader.get("senderTarget", "127.0.0.1");
        String port   = ConfigReader.get("senderPort", "8080");

        page.setName(name);
        page.chooseRole(role);
        page.chooseMethod(method);
        page.setTarget(target);
        page.setPort(port);
        page.clickStart();

        String status = page.status(); // expects friendly message from your JS
        Assert.assertTrue(status != null && !status.isBlank(), "Status should not be empty");
    }
}
