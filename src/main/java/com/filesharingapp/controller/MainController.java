package com.filesharingapp.controller;

import com.filesharingapp.core.PromptManager;
import com.filesharingapp.core.Receiver;
import com.filesharingapp.core.Sender;
import com.filesharingapp.utils.LoggerUtil;

import java.util.Locale;
import java.util.Scanner;

/**
 * MainController
 * --------------
 * Single orchestration entry for the File Sharing App.
 *
 * - PRESERVES the class & method names so existing integrations don't break.
 * - Internally delegates full logic to Sender and Receiver classes.
 * - Uses PromptManager for all user-facing text.
 *
 * You can:
 *  - Run this from TestNG (LaunchServerTest etc.)
 *  - Call start() from Swing / HTML launcher classes.
 */
public class MainController {

    private final Scanner in = new Scanner(System.in);
    private final Sender sender = new Sender();
    private final Receiver receiver = new Receiver();

    /**
     * Entry point for interactive console / automation.
     * Does NOT remove or break previous MainController contracts:
     * if you had other overloads, keep them.
     */
    public void start() {
        LoggerUtil.info(PromptManager.WELCOME);
        LoggerUtil.info(PromptManager.HELP_HINT);

        while (true) {
            LoggerUtil.info(PromptManager.ASK_ROLE);
            String role = in.nextLine().trim().toUpperCase(Locale.ROOT);

            if ("S".equals(role)) {
                sender.runInteractive();
                break;
            } else if ("R".equals(role)) {
                receiver.runInteractive();
                break;
            } else {
                LoggerUtil.warn(PromptManager.ROLE_INVALID);
            }
        }

        LoggerUtil.info(PromptManager.THANK_YOU);
    }
}
