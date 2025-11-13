package com.filesharingapp.tests.listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;

/**
 * ExtentReportManager
 * -------------------
 * This class creates ONE ExtentReports object for the whole run.
 *
 * Everyone else calls getReporter() to log results.
 */
public final class ExtentReportManager {

    private static ExtentReports extent;

    private ExtentReportManager() {}

    /** Get or create the single ExtentReports instance. */
    public synchronized static ExtentReports getReporter() {
        if (extent == null) {
            ExtentSparkReporter spark = new ExtentSparkReporter("target/extent-report.html");
            spark.config().setReportName("FileSharingApp UI & API Tests");
            extent = new ExtentReports();
            extent.attachReporter(spark);
            extent.setSystemInfo("Project", "FileSharingApp");
        }
        return extent;
    }

    /** Flush the report content to disk. */
    public synchronized static void flush() {
        if (extent != null) {
            extent.flush();
        }
    }
}
