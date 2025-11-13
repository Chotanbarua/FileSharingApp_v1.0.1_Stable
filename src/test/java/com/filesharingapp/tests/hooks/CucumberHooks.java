package com.filesharingapp.tests.hooks;

import com.filesharingapp.tests.base.TestBase;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

/**
 * CucumberHooks
 * -------------
 * These hooks run BEFORE and AFTER each Cucumber Scenario.
 *
 * Here we ONLY manage WebDriver:
 * - @Before: call TestBase.setUp() to open the browser and app.
 * - @After : call TestBase.clearDriver() to close the browser.
 *
 * Extent reporting is handled in separate listener classes.
 */
public class CucumberHooks extends TestBase {

    @Before
    public void beforeScenario(Scenario scenario) {
        // Start browser and open base URL.
        setUp();
    }

    @After
    public void afterScenario(Scenario scenario) {
        // Close browser and clean up.
        clearDriver();
    }
}
