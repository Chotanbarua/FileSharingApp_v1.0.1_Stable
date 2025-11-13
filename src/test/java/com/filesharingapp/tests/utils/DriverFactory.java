package com.filesharingapp.tests.utils;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;

/**
 * DriverFactory
 * -------------
 * This class knows how to create WebDriver for different browsers.
 *
 * It uses WebDriverManager to:
 * - auto-download drivers
 * - auto-update them
 *
 * So the tester does NOT need to manually install chromedriver.exe or msedgedriver.exe.
 */
public final class DriverFactory {

    private DriverFactory() {}

    /** Create a WebDriver based on browser name. */
    public static WebDriver create(String browser) {
        String b = (browser == null ? "chrome" : browser).toLowerCase();

        switch (b) {
            case "edge": {
                WebDriverManager.edgedriver()
                        .avoidResolutionCache()   // always check latest
                        .setup();
                EdgeOptions opt = new EdgeOptions();
                opt.setPageLoadStrategy(PageLoadStrategy.NORMAL);
                opt.addArguments("--remote-allow-origins=*");
                return new EdgeDriver(opt);
            }
            case "chrome":
            default: {
                WebDriverManager.chromedriver()
                        .avoidResolutionCache()   // always check latest
                        .setup();
                ChromeOptions opt = new ChromeOptions();
                opt.setPageLoadStrategy(PageLoadStrategy.NORMAL);
                opt.addArguments("--disable-gpu");
                opt.addArguments("--no-sandbox");
                opt.addArguments("--remote-allow-origins=*");
                return new ChromeDriver(opt);
            }
        }
    }
}
