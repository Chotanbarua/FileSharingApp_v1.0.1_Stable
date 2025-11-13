package com.filesharingapp.tests.utils;

import java.io.InputStream;
import java.util.Properties;

/**
 * ConfigReader
 * ------------
 * Reads values from:
 *   - JVM system properties (passed with -Dkey=value), then
 *   - src/test/resources/config.properties
 *
 * Example config.properties:
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

    private ConfigReader() {
    }

    /**
     * Get a value for a key.
     * Order:
     *   1) System.getProperty(key)           (VM option)
     *   2) config.properties value
     *   3) default value (def)
     */
    public static String get(String key, String def) {
        String fromVm = System.getProperty(key);
        if (fromVm != null && !fromVm.isBlank()) {
            return fromVm;
        }
        return P.getProperty(key, def);
    }

    /** Get a value that may be null if not set anywhere. */
    public static String get(String key) {
        return get(key, null);
    }
}
