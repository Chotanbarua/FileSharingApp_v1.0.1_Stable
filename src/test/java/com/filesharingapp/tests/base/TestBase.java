package com.filesharingapp.tests.base;

import com.filesharingapp.tests.utils.ConfigReader;
import com.filesharingapp.tests.utils.DriverFactory;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;

/**
 * TestBase
 * --------
 * Think of this class as the "browser boss".
 *
 * - It creates the browser (Chrome / Edge).
 * - It keeps ONE WebDriver per test thread using ThreadLocal.
 * - It opens the base URL before each test.
 *
 * ThreadLocal<WebDriver>:
 * - Imagine each parallel test has its own little box.
 * - Inside the box is its own WebDriver.
 * - No test touches another test's box.
 */
public abstract class TestBase {

    /**
     * TL_DRIVER (ThreadLocal driver)
     * ------------------------------
     * One WebDriver per thread.
     * This keeps parallel tests from fighting over the same browser.
     */
    protected static final ThreadLocal<WebDriver> TL_DRIVER = new ThreadLocal<>();

    /** Which browser this test should use (chrome, edge, etc.). */
    protected String browser;

    /** Base URL of your app (comes from config.properties by default). */
    protected String baseUrl = ConfigReader.get("baseUrl", "http://localhost:8080");

    /**
     * Default constructor.
     * If nobody tells us the browser, we read it from config.properties.
     */
    public TestBase() {
        this.browser = ConfigReader.get("browser", "chrome");
    }

    /**
     * Constructor that accepts a browser name.
     * Used by @Factory to create tests for many browsers.
     */
    public TestBase(String browser) {
        this.browser = (browser == null || browser.isBlank())
                ? ConfigReader.get("browser", "chrome")
                : browser.trim().toLowerCase();
    }

    /**
     * setUp
     * -----
     * This method runs BEFORE each @Test.
     *
     * Steps:
     * 1. Read baseUrl from TestNG parameters if provided.
     * 2. If this thread has no driver yet, create one using DriverFactory.
     * 3. Open the base URL in the browser.
     */
    @Parameters({"baseUrl"})
    @BeforeMethod(alwaysRun = true)
    public void setUp(String... baseUrlFromXml) {
        // If TestNG XML gives us a baseUrl, override the default.
        if (baseUrlFromXml != null && baseUrlFromXml.length > 0 && baseUrlFromXml[0] != null) {
            this.baseUrl = baseUrlFromXml[0];
        }

        // Only create a driver if this thread doesn't have one yet.
        if (TL_DRIVER.get() == null) {
            WebDriver driver = DriverFactory.create(browser);
            TL_DRIVER.set(driver);
        }

        // Open the app URL.
        TL_DRIVER.get().get(this.baseUrl);
    }

    /**
     * tearDown
     * --------
     * This method runs AFTER each @Test.
     * It closes the browser and removes the driver from ThreadLocal.
     */
    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        clearDriver();
    }

    /**
     * getDriver
     * ---------
     * Static helper so ANY class can ask:
     * "What is my WebDriver for this thread?"
     */
    public static WebDriver getDriver() {
        return TL_DRIVER.get();
    }

    /**
     * clearDriver
     * -----------
     * Safely quits the browser and clears the ThreadLocal.
     * Called from tearDown and listeners.
     */
    public static void clearDriver() {
        WebDriver d = TL_DRIVER.get();
        if (d != null) {
            d.quit();
            TL_DRIVER.remove();
        }
    }
}
