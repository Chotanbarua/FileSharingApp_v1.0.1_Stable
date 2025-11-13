package com.filesharingapp.tests.tests;

import com.filesharingapp.tests.base.TestBase;
import com.filesharingapp.tests.pages.IndexPage;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * SmokeUITest
 * -----------
 * Simple sanity test using the Page Object.
 * This class EXTENDS TestBase because it is a normal TestNG test.
 *
 * It does NOT create its own IndexPage.
 * It reuses the per-thread IndexPage from TestBase.indexPage().
 */
public class SmokeUITest extends TestBase {

    /** FactoryRunner passes browser name into this constructor. */
    public SmokeUITest(String browser) {
        super(browser);
    }

    @Test
    public void fillSenderAndValidateStatus() {
        // setUp() in TestBase already opened the app.

        IndexPage page = TestBase.indexPage();

        // For smoke test we keep simple example values.
        page.setName("Syed");
        page.chooseRole("Sender");
        page.chooseMethod("HTTP (Simple / LAN)");
        page.setTarget("127.0.0.1");
        page.setPort("8080");
        page.clickStart();

        String status = page.status();
        Assert.assertTrue(status != null && !status.isBlank(), "Status should not be empty");
    }
}
