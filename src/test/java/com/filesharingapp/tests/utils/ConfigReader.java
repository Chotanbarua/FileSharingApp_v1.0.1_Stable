package com.filesharingapp.tests.utils;

import java.io.InputStream;
import java.util.Properties;

/**
 * ConfigReader
 * ------------
 * This class reads key=value pairs from:
 *   src/test/resources/config.properties
 *
 * Example:
 *   baseUrl=http://localhost:8080
 *   browser=chrome
 */
public final class ConfigReader {

    private static final Properties P = new Properties();

    static {
        try (InputStream in = ConfigReader.class
                .getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (in != null) {
                P.load(in);
            }
        } catch (Exception ignored) {
        }
    }

    private ConfigReader() {}

    /** Get a value with a default if key is missing. */
    public static String get(String key, String def) {
        return P.getProperty(key, def);
    }

    /** Get a value that may be null if key is missing. */
    public static String get(String key) {
        return P.getProperty(key);
    }
}
