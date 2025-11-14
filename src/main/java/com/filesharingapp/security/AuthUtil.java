package com.filesharingapp.security;

import com.filesharingapp.core.PromptManager;
import com.filesharingapp.utils.LoggerUtil;

import java.io.Console;
import java.io.IOException; // FIX: Added missing import
import java.util.Random;
import java.util.Scanner;

/**
 * AuthUtil
 * --------
 * Baby-English:
 * ✔ This class asks the user small secret questions before transfer.
 * ✔ Three main jobs:
 * 1) ACCESS CODE
 * 2) CAPTCHA
 * 3) AES PASSWORD
 */
public final class AuthUtil {

    private AuthUtil() {
        // Utility class, do not create objects.
    }

    // ============================================================
    // 1️⃣ ACCESS CODE CHECK
    // ============================================================

    public static boolean checkAccessCode(String expectedCode) {
        LoggerUtil.info("[Auth] Asking user to enter access code.");

        int maxTries = 3;
        for (int i = 1; i <= maxTries; i++) {
            // FIX: Uses static method for prompt string
            String input = readMasked(PromptManager.askAccessCode());
            if (input != null && input.trim().equals(expectedCode)) {
                LoggerUtil.success("[Auth] Access code accepted.");
                return true;
            }
            LoggerUtil.warn("[Auth] Wrong access code. Attempt " + i + "/" + maxTries);
        }

        LoggerUtil.error("[Auth] Access code failed too many times.");
        return false;
    }

    // ============================================================
    // 2️⃣ CAPTCHA CHECK (Placeholder)
    // ============================================================

    public static boolean runCaptcha() {
        LoggerUtil.info("[Auth] Running simple CAPTCHA.");

        Random r = new Random();
        int a = 2 + r.nextInt(8);
        int b = 2 + r.nextInt(8);
        int correct = a + b;

        LoggerUtil.info(PromptManager.CAPTCHA_SHOW + " " + a + " + " + b + " = ?");
        String ans = read("Your answer:");

        try {
            int v = Integer.parseInt(ans.trim());
            if (v == correct) {
                LoggerUtil.success("[Auth] CAPTCHA correct.");
                return true;
            }
        } catch (Exception ignore) {
            // handled below
        }

        LoggerUtil.error("[Auth] CAPTCHA failed.");
        return false;
    }

    // ============================================================
    // 3️⃣ AES PASSWORD HANDLING
    // ============================================================

    /**
     * @return AES password or null if disabled.
     */
    public static String askAesPassword() {
        // FIX: Uses static method for prompt string
        String choice = read(PromptManager.askEnableAES());

        if (!"y".equalsIgnoreCase(choice.trim())) {
            LoggerUtil.info("[AES] Encryption disabled by user.");
            return null;
        }

        LoggerUtil.info("[AES] Encryption enabled.");
        while (true) {
            // FIX: Uses static method for prompt string
            String p1 = readMasked(PromptManager.askAesPasswordOne());
            // FIX: Uses static method for prompt string
            String p2 = readMasked(PromptManager.askAesPasswordTwo());

            if (p1 == null || p2 == null || p1.isBlank() || p2.isBlank()) {
                LoggerUtil.warn("[AES] " + PromptManager.AES_PASSWORD_EMPTY);
                continue;
            }

            if (!p1.equals(p2)) {
                LoggerUtil.warn("[AES] " + PromptManager.AES_MISMATCH);
                continue;
            }

            LoggerUtil.success("[AES] AES password accepted.");
            return p1;
        }
    }

    // ============================================================
    // SMALL INPUT HELPERS
    // ============================================================

    /**
     * Plain read (not masked).
     */
    private static String read(String promptText) {
        System.out.print(promptText + " ");
        // FIX: Use try-with-resources to avoid resource leak, and use a fresh Scanner
        try (Scanner in = new Scanner(System.in)) {
            return in.nextLine();
        } catch (Exception e) {
            LoggerUtil.error("[Auth] Error reading input.", e);
            return "";
        }
    }

    /**
     * Helper for custom prompts (not in PromptManager)
     */
    public static String readWithPrompt(String promptText, Scanner in) {
        System.out.print(promptText + " ");
        return in.nextLine();
    }


    /**
     * Masked input:
     * - Uses Console.readPassword() when available.
     */
    private static String readMasked(String promptText) {
        Console console = System.console();
        if (console != null) {
            char[] chars = console.readPassword(promptText + " ");
            if (chars == null) return "";
            return new String(chars);
        }
        // Fallback to plain read if console is null
        return read(promptText);
    }
}