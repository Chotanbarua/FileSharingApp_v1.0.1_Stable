package com.filesharingapp.tests.listeners;

import com.aventstack.extentreports.Status;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.*;

/**
 * ExtentCucumberAdapter
 * ---------------------
 * Bridges Cucumber events into ExtentReport.
 *
 * Uses Cucumber's own Status and maps it to Extent Status.
 */
public class ExtentCucumberAdapter implements ConcurrentEventListener {

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestCaseStarted.class, this::caseStarted);
        publisher.registerHandlerFor(TestCaseFinished.class, this::caseFinished);
    }

    private void caseStarted(TestCaseStarted e) {
        String name = e.getTestCase().getName();
        ExtentReportManager.getReporter()
                .createTest(name)
                .log(Status.INFO, "Cucumber started: " + name);
    }

    private void caseFinished(TestCaseFinished e) {
        String name = e.getTestCase().getName();
        io.cucumber.plugin.event.Status cucumberStatus = e.getResult().getStatus();

        Status extentStatus;
        switch (cucumberStatus) {
            case PASSED:
                extentStatus = Status.PASS;
                break;
            case FAILED:
                extentStatus = Status.FAIL;
                break;
            default:
                extentStatus = Status.SKIP;
                break;
        }

        ExtentReportManager.getReporter()
                .createTest(name)
                .log(extentStatus, "Cucumber finished: " + name + " -> " + cucumberStatus);

        ExtentReportManager.flush();
    }
}
