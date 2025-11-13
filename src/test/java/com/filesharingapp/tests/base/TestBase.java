package com.filesharingapp.tests.base;

import com.filesharingapp.tests.pages.IndexPage;
import com.filesharingapp.tests.utils.ConfigReader;
import com.filesharingapp.tests.utils.DriverFactory;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;

/**
 * TestBase
 * --------
 * Purpose:
 *   This class is the "driver boss" for all tests.
 *
 * Main jobs:
 *   1) Create a browser (WebDriver) for each test thread.
 *   2) Remember that browser in a ThreadLocal box.
 *   3) Open the base URL of the web app.
 *   4) Create a Page Object (IndexPage) per thread.
 *
 * Why ThreadLocal?
 *   - Think of each test thread as a student.
 *   - ThreadLocal<WebDriver> is a tiny locker for that student.
 *   - Each student gets their own browser in their own locker.
 *   - No student can touch another student's browser.
 */
public abstract class TestBase {

    /** One WebDriver per thread (for parallel runs). */
    protected static final ThreadLocal<WebDriver> TL_DRIVER = new ThreadLocal<>();

    /** One IndexPage per thread, reusing the same driver. */
    private static final ThreadLocal<IndexPage> TL_INDEX_PAGE = new ThreadLocal<>();

    /** Which browser this test should use (chrome / edge). */
    protected String browser;

    /** Base URL of the FileSharing web app. */
    protected String baseUrl = ConfigReader.get("baseUrl", "http://localhost:8080");

    /**
     * Default constructor.
     * If nobody tells us the browser, read it from config.properties
     * or from a JVM system property "browser".
     */
    public TestBase() {
        this.browser = ConfigReader.get("browser", "chrome");
    }

    /**
     * Constructor that accepts a browser name.
     * Used by FactoryRunner (for chrome + edge).
     */
    public TestBase(String browser) {
        this.browser = (browser == null || browser.isBlank())
                ? ConfigReader.get("browser", "chrome")
                : browser.trim().toLowerCase();
    }

    /**
     * setUp
     * -----
     * Runs BEFORE each TestNG @Test method.
     *
     * Steps:
     *   1) Optionally read baseUrl from TestNG XML parameter.
     *   2) If this thread has no WebDriver yet, create one.
     *   3) If this thread has no IndexPage yet, create one.
     *   4) Open the base URL in the browser.
     */
    @Parameters({"baseUrl"})
    @BeforeMethod(alwaysRun = true)
    public void setUp(String... baseUrlFromXml) {
        // If TestNG XML passed a baseUrl, use that instead of the default.
        if (baseUrlFromXml != null && baseUrlFromXml.length > 0 && baseUrlFromXml[0] != null) {
            this.baseUrl = baseUrlFromXml[0];
        }

        // Create WebDriver if this thread does not have one yet.
        if (TL_DRIVER.get() == null) {
            WebDriver driver = DriverFactory.create(browser);
            TL_DRIVER.set(driver);
        }

        // Create IndexPage if this thread does not have one yet.
        if (TL_INDEX_PAGE.get() == null) {
            TL_INDEX_PAGE.set(new IndexPage());
        }

        // Open the app URL.
        TL_DRIVER.get().get(this.baseUrl);
    }

    /**
     * tearDown
     * --------
     * Runs AFTER each TestNG @Test method.
     * It calls clearDriver(), which closes the browser and
     * cleans both ThreadLocal lockers.
     */
    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        clearDriver();
    }

    /**
     * getDriver
     * ---------
     * Static helper: any class can call TestBase.getDriver()
     * to get "its" WebDriver for the current thread.
     */
    public static WebDriver getDriver() {
        return TL_DRIVER.get();
    }

    /**
     * indexPage
     * ---------
     * Static helper: any class can call TestBase.indexPage()
     * to get the IndexPage for the current thread.
     * StepDefs and tests will use this instead of "new IndexPage()".
     */
    public static IndexPage indexPage() {
        return TL_INDEX_PAGE.get();
    }

    /**
     * clearDriver
     * -----------
     * Safely quits the browser for the current thread and
     * cleans both ThreadLocals.
     *
     * This is used by:
     *   - tearDown()
     *   - CucumberHooks
     *   - ExtentTestListener
     */
    public static void clearDriver() {
        WebDriver d = TL_DRIVER.get();
        if (d != null) {
            d.quit();
        }
        TL_DRIVER.remove();
        TL_INDEX_PAGE.remove();
    }
}
