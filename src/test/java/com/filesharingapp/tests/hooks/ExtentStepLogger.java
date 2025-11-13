package com.filesharingapp.tests.hooks;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.gherkin.model.Scenario;
import com.filesharingapp.tests.listeners.ExtentReportManager;
import io.cucumber.java.*;

import java.util.HashMap;
import java.util.Map;

/**
 * ExtentStepLogger
 * ----------------
 * This class listens to Cucumber steps and logs them into ExtentReport.
 *
 * For each Scenario:
 *  - Creates a node.
 *  - Logs each step as PASS or FAIL with time taken.
 */
public class ExtentStepLogger {

    /** One global ExtentReports instance (shared). */
    private static final ExtentReports extent = ExtentReportManager.getReporter();

    /** Store ExtentTest per scenario ID. */
    private static final Map<String, ExtentTest> scenarioMap = new HashMap<>();

    /** Store start time of each step to measure duration. */
    private static final Map<String, Long> stepStartTimes = new HashMap<>();

    /** Runs before each scenario. */
    @Before
    public void beforeScenario(io.cucumber.java.Scenario scenario) {
        // Create a "feature" test node using the feature file path.
        ExtentTest feature = extent.createTest(scenario.getUri().getPath());
        // Create a "scenario" node under that feature.
        ExtentTest test = feature.createNode(Scenario.class, scenario.getName());

        scenarioMap.put(scenario.getId(), test);
        test.log(Status.INFO, "🟢 Starting Scenario: " + scenario.getName());
    }

    /** Runs before each step to record start time. */
    @BeforeStep
    public void beforeStep(io.cucumber.java.Scenario scenario) {
        stepStartTimes.put(scenario.getId(), System.currentTimeMillis());
    }

    /** Runs after each step to log result and duration. */
    @AfterStep
    public void afterStep(io.cucumber.java.Scenario scenario) {
        ExtentTest test = scenarioMap.get(scenario.getId());
        if (test == null) return;

        long end = System.currentTimeMillis();
        long start = stepStartTimes.getOrDefault(scenario.getId(), end);
        long duration = end - start;

        Status status = scenario.isFailed() ? Status.FAIL : Status.PASS;
        String icon = scenario.isFailed() ? "❌" : "✅";

        test.log(status, icon + " Step completed in " + duration + " ms");

        if (scenario.isFailed()) {
            test.fail("Step failed in scenario: " + scenario.getName());
        }
    }

    /** Runs after each scenario to mark it passed/failed and flush Extent. */
    @After
    public void afterScenario(io.cucumber.java.Scenario scenario) {
        ExtentTest test = scenarioMap.get(scenario.getId());
        if (test == null) return;

        if (scenario.isFailed()) {
            test.log(Status.FAIL, "❌ Scenario Failed: " + scenario.getName());
        } else {
            test.log(Status.PASS, "✅ Scenario Passed: " + scenario.getName());
        }

        extent.flush();
        stepStartTimes.remove(scenario.getId());
    }
}
