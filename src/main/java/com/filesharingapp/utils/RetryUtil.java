package com.filesharingapp.utils;

import com.filesharingapp.core.PromptManager;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RetryUtil
 * ---------
 * Baby English:
 * - We try something again and again.
 */
public final class RetryUtil {

    private RetryUtil() {
        // Utility class – no objects.
    }

    // ============================
    // 🔁 Simple Retry for Runnable (FIXED AMBIGUITY)
    // ============================
    public static boolean runWithRetry(Runnable action, int maxRetries, long delayMs) {
        // FIX: Explicitly call the 4-argument method with null cancelToken
        return runWithRetry(action, maxRetries, delayMs, new AtomicBoolean(false));
    }

    public static boolean runWithRetry(Runnable action, int maxRetries, long delayMs, AtomicBoolean cancelToken) {
        int attempt = 0;

        while (attempt < maxRetries && !cancelToken.get()) {
            try {
                action.run();
                return true;
            } catch (Exception e) {
                attempt++;
                // FIX: Explicitly pass null transferId to LoggerUtil calls
                LoggerUtil.warn("Attempt " + attempt + " failed: " + e.getMessage(), null);
                PromptManager.showRetryPrompt(attempt, maxRetries);

                if (attempt >= maxRetries) {
                    LoggerUtil.error("All retry attempts failed", e, null);
                    return false;
                }

                sleep(delayMs);
            }
        }
        return false;
    }

    // ============================
    // 🔁 Retry for Callable<T>
    // ============================
    public static <T> T runWithRetry(Callable<T> action, int maxRetries, long delayMs) {
        // FIX: Explicitly call the 4-argument method with null cancelToken
        return runWithRetry(action, maxRetries, delayMs, new AtomicBoolean(false));
    }

    public static <T> T runWithRetry(Callable<T> action, int maxRetries, long delayMs, AtomicBoolean cancelToken) {
        int attempt = 0;

        while (attempt < maxRetries && !cancelToken.get()) {
            try {
                return action.call();
            } catch (Exception e) {
                attempt++;
                // FIX: Explicitly pass null transferId to LoggerUtil calls
                LoggerUtil.warn("Attempt " + attempt + " failed: " + e.getMessage(), null);
                PromptManager.showRetryPrompt(attempt, maxRetries);

                if (attempt >= maxRetries) {
                    LoggerUtil.error("All retry attempts failed", e, null);
                    return null;
                }

                sleep(delayMs);
            }
        }
        return null;
    }

    // ============================
    // 🔁 Retry with Exponential Backoff
    // ============================
    public static boolean runWithRetryAndBackoff(Runnable action, int maxRetries, long initialDelayMs, AtomicBoolean cancelToken) {
        int attempt = 0;
        long delay = initialDelayMs;

        while (attempt < maxRetries && !cancelToken.get()) {
            try {
                action.run();
                return true;
            } catch (Exception e) {
                attempt++;
                LoggerUtil.warn("Attempt " + attempt + " failed: " + e.getMessage(), null);
                PromptManager.showRetryPrompt(attempt, maxRetries);

                if (attempt >= maxRetries) {
                    LoggerUtil.error("All retry attempts failed", e, null);
                    return false;
                }

                sleep(delay);
                delay *= 2; // backoff
            }
        }
        return false;
    }

    // ============================
    // 🌐 Retry Network Call with Status Check
    // ============================
    public static boolean runNetworkRetry(Callable<Integer> networkCall, int maxRetries, long delayMs, AtomicBoolean cancelToken) {
        int attempt = 0;

        while (attempt < maxRetries && !cancelToken.get()) {
            try {
                int code = networkCall.call();
                if (code == 200) {
                    LoggerUtil.info("Network call OK (HTTP 200)", null);
                    return true;
                } else {
                    LoggerUtil.warn("Network call failed with code: " + code, null);
                }
            } catch (Exception e) {
                LoggerUtil.warn("Network call error: " + e.getMessage(), null);
            }

            attempt++;
            PromptManager.showRetryPrompt(attempt, maxRetries);

            if (attempt >= maxRetries) {
                LoggerUtil.error("All network retries failed", null, null);
                return false;
            }

            sleep(delayMs);
        }
        return false;
    }

    // ============================
    // 💤 Sleep Helper
    // ============================
    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            LoggerUtil.error("Retry sleep interrupted", ie, null);
        }
    }
}