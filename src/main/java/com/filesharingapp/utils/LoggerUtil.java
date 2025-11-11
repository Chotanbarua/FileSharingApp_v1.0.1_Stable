package com.filesharingapp.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Unified Logger wrapper using Log4j2 + console mirror.
 */
public final class LoggerUtil {
    private static final Logger logger = LogManager.getLogger("FileSharingApp");

    private LoggerUtil() {}

    public static void info(String msg) {
        logger.info(msg);
        System.out.println("[INFO] " + msg);
    }

    public static void success(String msg) {
        logger.info("[SUCCESS] " + msg);
        System.out.println("[SUCCESS] " + msg);
    }

    public static void warn(String msg) {
        logger.warn(msg);
        System.out.println("[WARN] " + msg);
    }

    public static void error(String msg) {
        logger.error(msg);
        System.err.println("[ERROR] " + msg);
    }

    public static void error(String msg, Throwable t) {
        logger.error(msg, t);
        System.err.println("[ERROR] " + msg);
        if (t != null) t.printStackTrace(System.err);
    }
}
