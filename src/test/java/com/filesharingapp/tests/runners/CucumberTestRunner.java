package com.filesharingapp.tests.runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

/**
 * CucumberTestRunner
 * ------------------
 * This class tells Cucumber + TestNG:
 * - Where feature files live.
 * - Where step definitions live.
 * - How to run tests (in parallel).
 */
@CucumberOptions(
        features = "src/test/resources/features",
        glue = {"com.filesharingapp.tests.hooks", "com.filesharingapp.tests.steps"},
        plugin = {
                "pretty",
                "summary",
                "html:target/cucumber-report.html"
                // You can also add your custom Extent plugin here if desired
                // "com.filesharingapp.tests.listeners.ExtentCucumberAdapter"
        }
)
public class CucumberTestRunner extends AbstractTestNGCucumberTests {

        @Override
        @DataProvider(parallel = true) // run scenarios in parallel
        public Object[][] scenarios() {
                return super.scenarios();
        }
}
