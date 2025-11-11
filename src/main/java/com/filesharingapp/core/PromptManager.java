package com.filesharingapp.core;

import java.time.LocalDateTime;

/**
 * PromptManager
 * -------------
 * Central repository for ALL user-facing messages and templates.
 * No UI / business logic here: just strings + tiny helpers.
 *
 * All other classes (Sender, Receiver, UI, tests) should fetch
 * prompts from here instead of hardcoding.
 */
public final class PromptManager {

    private PromptManager() {
        // Utility class: prevent instantiation.
    }

    // =========================
    // 🟩 Session Initialization
    // =========================

    public static final String WELCOME = "Welcome to the File Sharing App! 🎉";
    public static final String ENTER_SESSION_NAME = "Please enter a session name (any name you like):";
    public static final String ASK_USER_NAME = "What’s your name?";
    public static String hiUser(String name) {
        return "Hi " + name + "! Let’s get you started.";
    }
    public static final String QUICK_CHECK = "Quick check time 🔍 Making sure everything is ready…";
    public static final String CHECK_JAVA = "Checking Java version…";
    public static final String JAVA_OK = "✅ Java 17 detected.";
    public static final String JAVA_BAD = "❌ Java 17 not found. Please install Java 17 and restart this tool.";

    // ===============
    // 👥 Role Selection
    // ===============

    public static final String ASK_ROLE = "Are you a Sender (S) or a Receiver (R)?";
    public static final String ROLE_INVALID = "Oops! Please type S for Sender or R for Receiver.";

    // ==================
    // 🌐 Network Readiness
    // ==================

    public static final String CHECK_PORT_RANGE = "Checking server ports (8080–8090)…";
    public static final String CHECK_NETWORK = "Checking internet / network connection…";
    public static final String NETWORK_WARN =
            "⚠️ Network looks offline or restricted. Please verify your connection.";

    // ===============
    // 📤 Sender - Common
    // ===============

    public static final String ASK_TRANSPORT =
            "Choose how you’d like to send your file — HTTP, ZeroTier, or AWS S3?";
    public static final String CHOSE_HTTP =
            "You chose HTTP 🌐 — we’ll host a local server for uploads.";
    public static final String CHOSE_ZEROTIER =
            "You chose ZeroTier 🛰️ — please enter your Network ID:";
    public static final String CHOSE_S3 =
            "You chose AWS S3 ☁️ — enter your Bucket Name:";
    public static final String ASK_REGION = "Enter your Region Code (e.g., us-east-1):";

    // File preparation
    public static final String PROMPT_BROWSE =
            "Click Browse or drag-and-drop your file here 📁";
    public static final String NO_FILE_SELECTED =
            "You did not pick any file. Please select at least one file.";
    public static final String FILE_NOT_FOUND =
            "The selected file does not exist or cannot be opened.";
    public static final String FILE_EMPTY =
            "The file size is 0 bytes. Please choose a file that is not empty.";
    public static final String FILE_TYPE_BLOCKED =
            "This file type is not allowed for security reasons.";
    public static final String MULTI_FILE_INFO =
            "You selected multiple files. We will send them one by one in this session.";
    public static String totalSizeMb(double mb) {
        return "Total size selected: " + String.format("%.2f", mb) + " MB. This might take some time ⏳";
    }
    public static String confirmFile(String name, String sizeLabel) {
        return "You picked: " + name + " (" + sizeLabel + ") — Is that correct? (Y/N)";
    }

    // Duplicate & validation
    public static final String DUP_CHECK_OK =
            "Checking for duplicates … ✅ No duplicates found!";
    public static final String DUP_FOUND =
            "⚠️ Duplicate detected. Do you want to continue? (Y/N)";

    // Port binding
    public static String usingPort(int port) {
        return "Scanning open ports (8080-8090)… ✅ Using " + port;
    }
    public static final String PORT_BIND_FAIL =
            "❌ Could not start server on any port. Please close other apps and try again.";

    // Security
    public static final String ASK_ACCESS_CODE =
            "Enter your secure access code (this keeps your transfer private):";
    public static final String ACCESS_CODE_BAD =
            "Access code is incorrect. Please try again.";
    public static final String ACCESS_CODE_OK =
            "✅ Access code accepted. You’re good to go.";
    public static final String SECURITY_NOTE =
            "🔒 This transfer uses a secure token and checksum to protect your file.";

    // Start confirmation
    public static final String READY_TO_SEND =
            "Ready to send your file? Type ‘READY’ to begin 🚀";
    public static final String HUMAN_CHECK =
            "Type the numbers you see to confirm you’re human 🤖:";

    // During transfer
    public static String uploading(String name, int percent, String speed, String eta) {
        return "📤 Uploading " + name + " — " + percent +
                "% complete — Speed: " + speed + "/s ⏳ ETA: " + eta;
    }
    public static final String LARGE_FILE_WARN =
            "Heads up 📢 This file is large. It may take longer than usual.";
    public static final String CHUNK_INFO =
            "We’ll send it in safe chunks so your connection doesn’t break.";

    // Completion
    public static String uploadDone(String url) {
        return "✅ Upload complete! File available at " + url;
    }
    public static String transferSummary(int files, String namesCsv, String duration) {
        return "Transferred " + files + " files (" + namesCsv +
                ") in " + duration + ". Logs saved to /logs.";
    }
    public static final String STOPPING_SERVER =
            "Stopping the upload server…";
    public static final String SERVER_STOPPED =
            "✅ Server stopped. Thank you for using the File Sharing App 💚";

    // ===============
    // 📥 Receiver Flow
    // ===============

    public static String senderUsing(String method) {
        return "Sender is using " + method + ". Connecting…";
    }
    public static final String ASK_SENDER_IP =
            "Please enter the Sender’s IP or download URL:";
    public static final String ASK_PORT =
            "Enter port number (if different from default 8080):";
    public static String pinging(String host) {
        return "Pinging Sender at " + host + ": 🔍";
    }
    public static final String CONNECT_OK = "✅ Connection OK";
    public static final String CONNECT_FAIL = "❌ Couldn’t reach Sender — try again.";
    public static final String ASK_SAVE_FOLDER =
            "Where should we save the file? 📂 (Leave blank for Downloads folder)";
    public static final String FREE_SPACE_OK =
            "Checking free space … ✅ Enough space available.";
    public static final String FILE_EXISTS =
            "A file with the same name already exists. Overwrite? (Y/N)";
    public static final String PERMISSION_DENIED =
            "You do not have permission to save here. Choose another location.";
    public static String resumePartial(int percent) {
        return "We found a partial download. Resume from " + percent + "% ? (Y/N)";
    }
    public static String downloading(String name, int percent, String speed, String eta) {
        return "📥 Downloading " + name + " — " + percent +
                "% — Speed: " + speed + "/s ⏳ ETA: " + eta;
    }
    public static final String VERIFYING_SHA =
            "🔒 Verifying file integrity (SHA-256)…";
    public static final String VERIFY_OK =
            "✅ Match!";
    public static final String VERIFY_FAIL =
            "❌ Checksum failed, retrying.";
    public static String doneSavedTo(String path) {
        return "🎉 All done! File saved to " + path + ". Enjoy your transfer 😊";
    }

    // =================
    // 🛰️ ZeroTier Prompts
    // =================

    public static final String ZT_CHECK =
            "Checking for ZeroTier CLI installation 🛰️…";
    public static final String ZT_NOT_FOUND =
            "⚠️ ZeroTier not found. Please install from https://www.zerotier.com.";
    public static final String ZT_JOINING = "Joining network …";
    public static final String ZT_OK = "✅ Connected";
    public static final String ZT_FAIL = "❌ Failed.";
    public static String ztNodeStatus(String nodeId, String status) {
        return "Node ID: " + nodeId + "   Status: " + status;
    }
    public static final String ZT_NOTE =
            "Note: ZeroTier mode here is a guided helper, not a full file tunnel.";

    // ==============
    // ☁️ AWS S3 Prompts
    // ==============

    public static final String S3_ASK_BUCKET = "Enter your S3 bucket name:";
    public static final String S3_ASK_REGION = "Enter AWS region code (e.g., us-east-1):";
    public static final String S3_ASK_KEY = "Enter Access Key ID:";
    public static final String S3_ASK_SECRET = "Enter Secret Access Key:";
    public static final String S3_TESTING =
            "Testing upload to S3 bucket …";
    public static final String S3_OK =
            "✅ OK";
    public static final String S3_BAD =
            "❌ Failed credentials.";
    public static final String S3_NOTE =
            "Note: AWS mode is for demonstration. Keys are not stored.";

    // ===========================
    // ⚠️ Connection / Retry / Resume
    // ===========================

    public static final String TRYING_REACH =
            "We’re trying to reach the other side. Please wait…";
    public static String retrying(int attempt, int max) {
        return "⏳ No response yet, retrying… (Attempt " + attempt + " of " + max + ")";
    }
    public static final String RETRY_GIVEUP =
            "❌ Could not connect after multiple tries.";
    public static final String ASK_RETRY =
            "Do you want to retry connection? (Y/N)";
    public static final String ASK_RESUME_INACTIVE =
            "The transfer paused due to inactivity. Resume? (Y/N)";

    // ===================
    // 🔐 Security & AES
    // ===================

    public static final String ASK_AES_PWD =
            "Enter encryption password (leave blank to skip):";
    public static final String ASK_AES_CONFIRM =
            "Confirm password:";
    public static final String AES_DISABLED =
            "AES-256 encryption is disabled by default for faster transfers.";
    public static final String INVALID_INPUT =
            "⚠️ Invalid input. Please try again.";
    public static final String GENERIC_ERROR =
            "Something went wrong 💥 See logs for details.";

    // =========================
    // 🧾 Logging / Reports / Exit
    // =========================

    public static String logSessionStart() {
        return "[LOG] Session started at " + LocalDateTime.now();
    }
    public static String logUsingPort(String ip, int port) {
        return "[LOG] Using port " + port + " on IP " + ip;
    }
    public static String logFile(String name, double mb, String extra) {
        return "[LOG] " + name + " – " + String.format("%.2f", mb) + " MB – " + extra;
    }
    public static String duplicateLog(String name, LocalDateTime when) {
        return "[DUPLICATE] " + name + " already sent on " + when;
    }
    public static final String EXCEL_CREATED =
            "Excel report created at /logs/TransferReport.xlsx";
    public static final String ASK_OPEN_LOGS =
            "Would you like to open the logs folder now? (Y/N)";
    public static String logsSavedAt(String path) {
        return "Logs saved at: " + path;
    }
    public static final String SESSION_END_OK =
            "[LOG] Session ended successfully ✅";
    public static final String ASK_OPEN_LOGS2 =
            "✨ Transfer complete! Would you like to open the log folder? (Y/N)";
    public static final String CLEANING_TMP =
            "Cleaning temporary files … ✅ Done.";
    public static final String THANK_YOU =
            "Thank you for using File Sharing App 💚 See you next time!";

    // =============
    // 💬 Help Prompts
    // =============

    public static final String HELP_HINT =
            "Not sure what to do? Click ‘Help’ for a simple 1-2-3 step guide.";
    public static final String HELP_SUMMARY =
            "Sender: Choose your file and share the link. Receiver: Paste and download. That’s it 👍";
    public static final String HELP_WIFI_TIP =
            "If you are on the same Wi-Fi, use the local IP shown above. For remote transfer, use ZeroTier.";

    public static final String NETWORK_PING_CHECK = "Pinging receiver to verify connection...";
    public static final String NETWORK_PING_FAIL = "⚠️ Receiver is offline or unreachable.";
    public static final String NETWORK_PING_SUCCESS = "✅ Receiver online and ready to receive.";
    public static final String CONNECTION_RETRY = "Retrying connection...";
    public static final String MODE_NOTIFICATION = "Sharing active transport method with receiver.";
    public static final String MODE_CONFIRMATION = "Receiver confirmed mode synchronization.";

}
