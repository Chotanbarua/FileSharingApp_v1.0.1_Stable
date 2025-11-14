package com.filesharingapp.core;

import com.filesharingapp.utils.LoggerUtil;

/**
 * PromptManager
 * -------------
 * Baby-English class that only stores “things we tell the user”.
 * All prompts used by MainController, Sender, Receiver, and Web UI match these strings.
 */
public final class PromptManager {

    private PromptManager() { }

    // ================================
    // 🌟 Session / Basic Setup
    // ================================
    public static final String WELCOME =
            "Welcome to FileSharingApp! (Console)";
    public static final String HELP_HINT =
            "Type carefully. If stuck, type 'help'.";
    public static final String ENTER_SESSION_NAME =
            "What is your session name? (any simple word is OK)";
    public static final String ASK_USER_NAME =
            "What is your name?";
    public static final String INVALID_INPUT =
            "Input was not valid.";
    public static final String QUICK_CHECK =
            "Quick environment check…";
    public static final String CHECK_JAVA =
            "Checking Java version…";
    public static final String JAVA_OK =
            "Java looks good ✅";
    public static final String JAVA_BAD =
            "Java version may be old ❌";
    public static final String ASK_ROLE =
            "Are you a Sender or Receiver? (S/R)";
    public static final String ROLE_INVALID =
            "Please type S for Sender or R for Receiver.";
    public static final String SENDER_INIT_OK =
            "Sender mode selected.";
    public static final String RECEIVER_INIT_OK =
            "Receiver mode selected.";
    public static final String SESSION_END_OK =
            "Session ended.";
    public static final String THANK_YOU =
            "Thank you for using FileSharingApp!";
    public static String hiUser(String name) {
        return "Hello, " + name + "!";
    }
    public static String logSessionStart() {
        return "Session started.";
    }

    // ================================
    // 🚀 Sender Prompts
    // ================================
    public static final String SENDER_PICK_FILE =
            "Please pick your file (full path).";
    public static final String SENDER_FILE_NOT_FOUND =
            "We cannot see that file. Try again.";
    public static final String SENDER_ZIP_MAKING =
            "Making ZIP if needed… please wait.";
    public static final String READY_TO_SEND =
            "File ready. Type 'y' to send or 'n' to cancel.";


    // ================================
    // 📥 Receiver Prompts
    // ================================
    public static final String RECEIVER_CHOOSE_FOLDER =
            "Where should we save the incoming file? (enter folder path, or blank for Downloads)";
    public static final String RECEIVER_FOLDER_BAD =
            "We cannot save there. Pick another folder.";
    public static final String RECEIVER_READY =
            "Receiver ready. Waiting for sender to connect...";

    // ================================
    // 🌐 Transfer Method Selection
    // ================================
    public static final String METHOD_QUESTION =
            "How do you want to send the file? (HTTP / ZeroTier / S3)";
    public static final String METHOD_INVALID =
            "Please type HTTP, ZeroTier, or S3.";

    // ================================
    // 🔐 AES Encryption Prompts
    // ================================
    public static final String ASK_ENABLE_AES =
            "Do you want to enable AES-256 encryption? (y/n)";
    public static final String ASK_AES_PASSWORD_ONE =
            "Please enter your secret password (AES-256):";
    public static final String ASK_AES_PASSWORD_TWO =
            "Re-enter password to confirm:";
    public static final String AES_PASSWORD_EMPTY =
            "Password cannot be empty.";
    public static final String AES_MISMATCH =
            "Passwords did not match. Try again.";

    // ================================
    // 🧩 CAPTCHA
    // ================================
    public static final String CAPTCHA_SHOW =
            "Type this small code to continue:";
    public static final String CAPTCHA_FAIL =
            "Code was wrong. Try again.";

    // ================================
    // 🌍 HTTP Prompts (FIX: Added missing symbols)
    // ================================
    public static final String HTTP_TARGET_IP =
            "What is Receiver IP or URL? (example: 192.168.1.10)"; // FIX: HTTP_TARGET_IP
    public static final String HTTP_PORT =
            "Which port should we use? (default 8080)"; // FIX: HTTP_PORT
    public static final String HTTP_PORT_BAD =
            "Port must be between 1 and 65535.";

    // ================================
    // 🛰️ ZeroTier Prompts
    // ================================
    public static final String ZT_NETWORK_ID =
            "Enter ZeroTier Network ID (16 hex digits).";
    public static final String ZT_PEER_IP =
            "Enter your partner's ZeroTier IP (example: 10.x.x.x).";

    // ================================
    // ☁️ AWS S3 Prompts
    // ================================
    public static final String S3_BUCKET =
            "Enter S3 bucket name:";
    public static final String S3_REGION =
            "Enter AWS region (example: us-east-1):";
    // FIX: Added missing S3 credential prompts used by S3TransferHandler/AwsS3TransferService
    public static final String S3_ACCESS_KEY =
            "Enter AWS Access Key:";
    public static final String S3_SECRET_KEY =
            "Enter AWS Secret Key:";
    public static final String S3_PRESIGNED_URL =
            "Paste the S3 presigned URL for download (Receiver):";
    public static final String S3_UPLOAD =
            "Uploading file to S3…";
    public static final String S3_DOWNLOAD =
            "Downloading file from S3…";
    public static final String S3_PRESIGNED_SHARE =
            "Share this presigned URL with your partner:";


    // ================================
    // 🔁 Resume + Retry
    // ================================
    public static final String RESUME_FOUND =
            "We found a partial file. Resume download? (y/n)";
    public static final String RETRYING =
            "Trying again… please wait.";

    // ================================
    // 🎯 UI Hints (for log forwarding)
    // ================================
    public static final String UI_TRANSFER_DONE =
            "Transfer finished 🎉";
    public static final String UI_TRANSFER_FAIL =
            "Transfer failed. Try again.";

    // ================================
    // Utility Prompts (used by AuthUtil, RetryUtil, etc.)
    // ================================
    public static String askAccessCode() {
        return "Please enter the shared access code:";
    }

    public static String askEnableAES() {
        return ASK_ENABLE_AES;
    }

    public static String askAesPasswordOne() {
        return ASK_AES_PASSWORD_ONE;
    }

    public static String askAesPasswordTwo() {
        return ASK_AES_PASSWORD_TWO;
    }

    public static void showInfo(String message) {
        LoggerUtil.uiPrompt("INFO: " + message, null);
    }

    public static void showWarning(String message) {
        LoggerUtil.uiPrompt("WARNING: " + message, null);
    }

    public static void showError(String message) {
        LoggerUtil.uiPrompt("ERROR: " + message, null);
    }

    public static void showRetryPrompt(int attempt, int max) {
        LoggerUtil.uiPrompt(String.format("Attempt %d of %d failed. Retrying in a moment...", attempt, max), null);
    }

    // FIX: Added missing S3 prompt methods used by old S3TransferHandler logic
    public static String askUserBucket() { return S3_BUCKET; }
    public static String askUserRegion() { return S3_REGION; }
    public static String askUserAccessKey() { return S3_ACCESS_KEY; }
    public static String askUserSecretKey() { return S3_SECRET_KEY; }
    public static String askUserObjectKey() { return "Enter S3 object key (file name):"; }

}