package com.filesharingapp.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AppConfig
 * ---------
 * Baby-English:
 *   ✔ This class loads values from application.properties.
 *   ✔ Everyone else uses this helper to read settings.
 *   ✔ Features:
 *       - Read String, int, boolean, long, double
 *       - Validate required keys
 *       - Scan keys by prefix (for dynamic configs)
 *       - Reload file if changed
 */
public final class AppConfig {

    /** Where we keep all properties in memory. */
    private static final Properties PROPS = new Properties();

    /** Path to the properties file in resources. */
    private static final String CONFIG_FILE = "application.properties";

    static {
        // Baby-English:
        // ✔ Load properties when class starts.
        loadProperties();
    }

    private AppConfig() {
        // Utility class – do not create objects.
    }

    /**
     * loadProperties
     * --------------
     * Baby-English:
     *   ✔ Reads application.properties from classpath.
     *   ✔ Logs success or failure.
     */
    private static void loadProperties() {
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (in != null) {
                PROPS.clear();
                PROPS.load(in);
                LoggerUtil.info("✅ Loaded " + CONFIG_FILE);
            } else {
                LoggerUtil.warn("⚠️ " + CONFIG_FILE + " not found. Using defaults.");
            }
        } catch (IOException e) {
            LoggerUtil.error("❌ Failed to load " + CONFIG_FILE, e);
        }
    }

    /**
     * reload
     * ------
     * Baby-English:
     *   ✔ Call this if you edit application.properties while app is running.
     */
    public static void reload() {
        LoggerUtil.info("🔄 Reloading application.properties...");
        loadProperties();
    }

    /**
     * get
     * ---
     * Baby-English:
     *   ✔ Read a text value or return default if missing.
     */
    public static String get(String key, String def) {
        String v = PROPS.getProperty(key);
        return (v == null || v.isBlank()) ? def : v.trim();
    }

    /**
     * getInt
     * ------
     * Baby-English:
     *   ✔ Read an integer or return default if invalid.
     */
    public static int getInt(String key, int def) {
        try {
            return Integer.parseInt(get(key, String.valueOf(def)));
        } catch (NumberFormatException e) {
            LoggerUtil.warn("⚠️ Invalid int for key " + key + ", using default " + def);
            return def;
        }
    }

    /**
     * getLong
     * -------
     * Baby-English:
     *   ✔ Read a long number or return default if invalid.
     */
    public static long getLong(String key, long def) {
        try {
            return Long.parseLong(get(key, String.valueOf(def)));
        } catch (NumberFormatException e) {
            LoggerUtil.warn("⚠️ Invalid long for key " + key + ", using default " + def);
            return def;
        }
    }

    /**
     * getDouble
     * ---------
     * Baby-English:
     *   ✔ Read a decimal number or return default if invalid.
     */
    public static double getDouble(String key, double def) {
        try {
            return Double.parseDouble(get(key, String.valueOf(def)));
        } catch (NumberFormatException e) {
            LoggerUtil.warn("⚠️ Invalid double for key " + key + ", using default " + def);
            return def;
        }
    }

    /**
     * getBoolean
     * ----------
     * Baby-English:
     *   ✔ Read true/false or return default if invalid.
     */
    public static boolean getBoolean(String key, boolean def) {
        String v = get(key, String.valueOf(def));
        return "true".equalsIgnoreCase(v) || ("false".equalsIgnoreCase(v) ? false : def);
    }

    /**
     * validateRequired
     * ----------------
     * Baby-English:
     *   ✔ Check if a key exists and is not blank.
     *   ✔ If missing, throw error because app cannot run without it.
     */
    public static void validateRequired(String key) {
        String v = PROPS.getProperty(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("❌ Missing required config key: " + key);
        }
    }

    /**
     * scanByPrefix
     * ------------
     * Baby-English:
     *   ✔ Find all keys that start with a given prefix.
     *   ✔ Useful for dynamic configs like aws.s3.*
     */
    public static Set<String> scanByPrefix(String prefix) {
        return PROPS.stringPropertyNames().stream()
                .filter(k -> k.startsWith(prefix))
                .collect(Collectors.toSet());
    }
}