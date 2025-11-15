package com.filesharingapp.tests.pages;

import com.filesharingapp.tests.utils.SeleniumActions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * IndexPage (Page Object)
 * -----------------------
 * This class represents your index.html screen.
 *
 * Each field below is a locator (an address on the web page).
 * Each method below is a small user action.
 */
public class IndexPage {

    // Locators (match your index.html IDs)
    private static final By NAME       = By.id("name");
    private static final By ROLE       = By.id("role");
    private static final By METHOD     = By.id("method");
    private static final By FILE       = By.id("file");
    private static final By TARGET     = By.id("target");
    private static final By PORT       = By.id("port");
    private static final By START      = By.id("startBtn");
    private static final By STATUS     = By.id("status");
    private static final By FILE_LABEL = By.id("fileLabel"); // label above file input

    /** Empty constructor. We do not keep WebDriver here. */
    public IndexPage() {
    }

    /** Optional constructor, kept only for compatibility. It does nothing. */
    public IndexPage(WebDriver driver) {
        // no-op
    }

    // ===== Actions =====

    /** Type the user's name into the name field. */
    public void setName(String name) {
        SeleniumActions.sendKeys(NAME, name);
    }

    /** Choose Sender or Receiver from the role drop-down. */
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

    /** Enter the target IP or host. */
    public void setTarget(String target) {
        SeleniumActions.sendKeys(TARGET, target);
    }

    /** Enter the port number. */
    public void setPort(String port) {
        SeleniumActions.sendKeys(PORT, port);
    }

    /** Click the Start button. */
    public void clickStart() {
        SeleniumActions.click(START);
    }

    /** Get the status text from the status area. */
    public String statusText() {
        return SeleniumActions.text(STATUS);
    }

    /** Alias used in some tests. */
    public String status() {
        return statusText();
    }

    /** Get the label text above the file input. */
    public String fileLabelText() {
        return SeleniumActions.text(FILE_LABEL);
    }


    /// Syed Test
}
