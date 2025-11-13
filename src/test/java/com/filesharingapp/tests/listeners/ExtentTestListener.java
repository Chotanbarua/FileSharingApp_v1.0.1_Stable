package com.filesharingapp.tests.listeners;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.filesharingapp.tests.base.TestBase;
import com.filesharingapp.tests.utils.ScreenshotUtil;
import org.testng.*;

/**
 * ExtentTestListener
 * ------------------
 * This listens to TestNG test events (@Test methods).
 *
 * For each test:
 *  - onTestStart: create Extent test node.
 *  - onTestSuccess: mark PASS.
 *  - onTestFailure: mark FAIL + screenshot.
 */
public class ExtentTestListener implements ITestListener {

    /** Store ExtentTest per test method using ThreadLocal. */
    private static final ThreadLocal<ExtentTest> CURRENT = new ThreadLocal<>();

    @Override
    public void onTestStart(ITestResult result) {
        ExtentTest t = ExtentReportManager.getReporter()
                .createTest(result.getMethod().getMethodName());
        CURRENT.set(t);
        t.log(Status.INFO, "Starting: " + result.getMethod().getMethodName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        CURRENT.get().log(Status.PASS, "Passed");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        CURRENT.get().log(Status.FAIL, result.getThrowable());
        CURRENT.get().addScreenCaptureFromBase64String(
                java.util.Base64.getEncoder().encodeToString(ScreenshotUtil.asBytes()),
                "Failure screenshot");
    }

    @Override
    public void onFinish(ITestContext context) {
        ExtentReportManager.flush();
        // Safely clear driver using helper in TestBase (no direct TL_DRIVER access).
        TestBase.clearDriver();
    }
}
