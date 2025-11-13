package com.filesharingapp.tests.pages;

import com.filesharingapp.tests.utils.SeleniumActions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * IndexPage (Page Object)
 * -----------------------
 * This class represents your index.html page.
 *
 * Idea:
 * - Each locator = an element on the page.
 * - Each method = one small user action.
 */
public class IndexPage {

    // ======= Locators (IDs from index.html) =======
    private static final By NAME       = By.id("name");
    private static final By ROLE       = By.id("role");
    private static final By METHOD     = By.id("method");
    private static final By FILE       = By.id("file");
    private static final By TARGET     = By.id("target");
    private static final By PORT       = By.id("port");
    private static final By START      = By.id("startBtn");
    private static final By STATUS     = By.id("status");
    private static final By FILE_LABEL = By.id("fileLabel");

    /**
     * Empty constructor.
     * We do NOT store WebDriver here because SeleniumActions
     * pulls driver from TestBase.getDriver().
     */
    public IndexPage() {
        // nothing to store
    }

    /**
     * Constructor that accepts WebDriver (for compatibility with older code).
     * We ignore the argument but keep it so old code still compiles.
     */
    public IndexPage(WebDriver driver) {
        // no-op on purpose
    }

    // ======= Actions =======

    /** Type the user's name into the name box. */
    public void setName(String name) {
        SeleniumActions.sendKeys(NAME, name);
    }

    /** Choose Sender or Receiver from the role dropdown. */
    public void chooseRole(String role) {
        SeleniumActions.selectByText(ROLE, role);
    }

    /** Select the transfer method (HTTP / ZeroTier / S3). */
    public void chooseMethod(String method) {
        SeleniumActions.selectByText(METHOD, method);
    }

    /** Upload a file using the file input. */
    public void chooseFile(String absolutePath) {
        SeleniumActions.upload(FILE, absolutePath);
    }

    /** Enter target IP / URL / bucket. */
    public void setTarget(String target) {
        SeleniumActions.sendKeys(TARGET, target);
    }

    /** Enter port number. */
    public void setPort(String port) {
        SeleniumActions.sendKeys(PORT, port);
    }

    /** Click the Start button. */
    public void clickStart() {
        SeleniumActions.click(START);
    }

    /** Read the status message text. */
    public String statusText() {
        return SeleniumActions.text(STATUS);
    }

    /** Older helper used in some tests; just forwards to statusText(). */
    public String status() {
        return statusText();
    }

    /** Read the label text above the file control. */
    public String fileLabelText() {
        return SeleniumActions.text(FILE_LABEL);
    }
}
