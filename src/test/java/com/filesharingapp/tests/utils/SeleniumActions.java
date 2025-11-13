package com.filesharingapp.tests.utils;

import com.filesharingapp.tests.base.TestBase;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * SeleniumActions
 * ---------------
 * Reusable, beginner-friendly wrappers around Selenium.
 * Every method uses TestBase.getDriver() (ThreadLocal-safe).
 */
public final class SeleniumActions {

    private SeleniumActions() {}

    /** Get the WebDriver for the current thread. */
    private static WebDriver d() {
        return TestBase.getDriver();
    }

    /** Build a WebDriverWait with a 10-second timeout. */
    private static WebDriverWait waitUntil() {
        return new WebDriverWait(d(), Duration.ofSeconds(10));
    }

    /** Clicks an element when it is clickable. */
    public static void click(By locator) {
        waitUntil().until(ExpectedConditions.elementToBeClickable(locator)).click();
    }

    /** Types text into an input after clearing it. */
    public static void sendKeys(By locator, String text) {
        WebElement el = waitUntil().until(ExpectedConditions.visibilityOfElementLocated(locator));
        el.clear();
        el.sendKeys(text == null ? "" : text);
    }

    /** Selects a drop-down option by its visible text. */
    public static void selectByText(By locator, String visibleText) {
        WebElement el = waitUntil().until(ExpectedConditions.visibilityOfElementLocated(locator));
        new Select(el).selectByVisibleText(visibleText);
    }

    /** Returns the element text (trimmed). */
    public static String text(By locator) {
        return waitUntil().until(ExpectedConditions.visibilityOfElementLocated(locator))
                .getText()
                .trim();
    }

    /** Small helper to upload a file with <input type="file">. */
    public static void upload(By locator, String absolutePath) {
        WebElement el = waitUntil().until(ExpectedConditions.presenceOfElementLocated(locator));
        el.sendKeys(absolutePath);
    }

    /** Waits until the page is fully loaded (document.readyState == complete). */
    public static void waitForPageReady() {
        new WebDriverWait(d(), Duration.ofSeconds(15)).until(
                wd -> ((JavascriptExecutor) wd)
                        .executeScript("return document.readyState").equals("complete"));
    }
}
