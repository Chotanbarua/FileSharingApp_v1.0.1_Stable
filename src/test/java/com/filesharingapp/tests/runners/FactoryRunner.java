package com.filesharingapp.tests.runners;

import com.filesharingapp.tests.tests.SmokeUITest;
import com.filesharingapp.tests.utils.ConfigReader;
import org.testng.annotations.Factory;

/**
 * FactoryRunner
 * -------------
 * Uses TestNG @Factory to create the same test
 * for multiple browsers.
 *
 * Browsers are read from config.properties:
 *   browsers=chrome,edge
 */
public class FactoryRunner {

    @Factory
    public Object[] factoryData() {
        // Read "browsers" from config, default to chrome
        String csv = ConfigReader.get("browsers", "chrome");
        String[] parts = csv.split(",");
        Object[] tests = new Object[parts.length];

        for (int i = 0; i < parts.length; i++) {
            String browser = parts[i].trim();
            tests[i] = new SmokeUITest(browser);
        }
        return tests;
    }
}
