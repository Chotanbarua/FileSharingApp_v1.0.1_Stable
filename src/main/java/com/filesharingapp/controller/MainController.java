package com.filesharingapp.controller;

import com.filesharingapp.core.PromptManager;
import com.filesharingapp.core.Sender;
import com.filesharingapp.core.Receiver;
import com.filesharingapp.transfer.TargetConfig;
import com.filesharingapp.utils.ValidationUtil;
import com.filesharingapp.utils.AppConfig;
import com.filesharingapp.utils.LoggerUtil;

import java.util.Locale;
import java.util.Scanner;

/**
 * MainController
 * --------------
 * This is the application's core entry class for the console flow.
 */
public class MainController {

    /**
     * The main entry point required by the JVM for the Application Run Configuration.
     * It simply delegates to the core logic in start().
     */
    public static void main(String[] args) {
        new MainController().start();
    }

    /** Scanner for reading user input. We do NOT close System.in. */
    private final Scanner in = new Scanner(System.in);

    /** Sender wizard (handles all transport logic internally). */
    private final Sender sender = new Sender();

    /** Receiver wizard (handles all transport logic internally). */
    private final Receiver receiver = new Receiver();

    /**
     * start()
     * -------
     * Contains the primary console interaction logic.
     */
    public void start() {

        // -------------------------------
        // 1) Say Hello
        // -------------------------------
        LoggerUtil.info(PromptManager.WELCOME);
        LoggerUtil.info(PromptManager.HELP_HINT);

        // -------------------------------
        // 2) Ask for Session Name
        // -------------------------------
        LoggerUtil.info(PromptManager.ENTER_SESSION_NAME);
        String sessionName = in.nextLine().trim();

        // If blank → fallback to app name + version
        if (sessionName.isEmpty()) {
            sessionName = AppConfig.get("app.name", "FileSharingApp") + " "
                    + AppConfig.get("app.version", "1.0");
        }

        LoggerUtil.info(PromptManager.logSessionStart() + " | Session: " + sessionName);

        // -------------------------------
        // 3) Ask for User Name
        // -------------------------------
        String userName;
        while (true) {
            LoggerUtil.info(PromptManager.ASK_USER_NAME);
            userName = in.nextLine().trim();

            String validationError = ValidationUtil.validateName(userName);

            if (validationError == null) {
                LoggerUtil.info(PromptManager.hiUser(userName));
                break;
            }
            LoggerUtil.warn(validationError);
        }

        // -------------------------------
        // 4) Quick environment checks
        // -------------------------------
        LoggerUtil.info(PromptManager.QUICK_CHECK);

        // Check Java version
        LoggerUtil.info(PromptManager.CHECK_JAVA);
        String javaVersion = System.getProperty("java.version", "unknown");
        if (javaVersion.startsWith("17")) {
            LoggerUtil.info(PromptManager.JAVA_OK + " (Detected: " + javaVersion + ")");
        } else {
            LoggerUtil.warn(PromptManager.JAVA_BAD + " (Detected: " + javaVersion + ")");
        }

        // -------------------------------
        // 5) Ask for Role (Sender or Receiver)
        // -------------------------------
        while (true) {
            LoggerUtil.info(PromptManager.ASK_ROLE);
            String role = in.nextLine().trim().toUpperCase(Locale.ROOT);

            if ("S".equals(role)) {
                LoggerUtil.info(PromptManager.SENDER_INIT_OK);
                sender.runInteractive(userName, sessionName);
                break;
            } else if ("R".equals(role)) {
                LoggerUtil.info(PromptManager.RECEIVER_INIT_OK);
                // Receiver interactive flow will handle mode/save path/resume prompts
                receiver.runInteractive(userName, TargetConfig.createInvalid());
                break;
            } else {
                LoggerUtil.warn(PromptManager.ROLE_INVALID);
            }
        }

        // -------------------------------
        // 6) Goodbye message
        // -------------------------------
        LoggerUtil.info(PromptManager.SESSION_END_OK);
        LoggerUtil.info(PromptManager.THANK_YOU);
    }
}