package com.filesharingapp.tests.utils;

import com.filesharingapp.tests.base.TestBase;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

/**
 * ScreenshotUtil
 * --------------
 * Tiny helper to capture screenshots as bytes.
 */
public final class ScreenshotUtil {

    private ScreenshotUtil() {}

    public static byte[] asBytes() {
        return ((TakesScreenshot) TestBase.getDriver())
                .getScreenshotAs(OutputType.BYTES);
    }
}
