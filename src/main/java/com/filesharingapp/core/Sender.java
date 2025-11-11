package com.filesharingapp.core;

import com.filesharingapp.transfer.HttpTransferHandler;
import com.filesharingapp.transfer.S3TransferHandler;
import com.filesharingapp.transfer.TransferMethod;
import com.filesharingapp.transfer.ZeroTierTransferHandler;
import com.filesharingapp.utils.LoggerUtil;
import com.filesharingapp.utils.NetworkUtil;

import java.io.File;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Sender
 * ------
 * Encapsulates the complete Sender flow.
 *
 * - Uses PromptManager for ALL messages.
 * - Uses existing TransferMethod implementations (HTTP / ZeroTier / S3).
 * - Performs strong validation (file, port, access code).
 * - Designed so MainController / UI can call runInteractive() or step-by-step methods.
 *
 * NOTE:
 *  - No classes/methods from v1.0.5 are removed.
 *  - This class only coordinates; actual network logic stays in your handlers.
 */
public class Sender {

    private final Scanner in = new Scanner(System.in);

    // Keep track of recently sent files for duplicate detection in this session.
    private final Set<String> sentFileFingerprints = new HashSet<>();

    public void runInteractive() {
        try {
            // 1) Session + env checks
            startSessionIntro();

            // 2) Choose transport
            String transport = chooseTransport();

            // 3) File selection + validation
            File file = chooseAndValidateFile();

            // 4) Duplicate check
            handleDuplicateCheck(file);

            // 5) Port binding (HTTP needs it, but we compute anyway for logging)
            int port = findFreePort(8080, 8090);

            // 6) Ask secure access code
            String accessCode = handleAccessCode();

            // 7) Confirm start & human check
            if (!confirmStart()) {
                LoggerUtil.warn("Sender cancelled before start.");
                return;
            }

            // 8) Kick off transfer through proper handler
            // 8) Kick off transfer through proper handler
            Instant start = Instant.now();
            TransferMethod handler = createHandlerFor(transport);

            LoggerUtil.info(PromptManager.SECURITY_NOTE);

// We pass: senderName, file, method label, port, targetHost
            String targetHost = askTargetHostForTransport(transport);

// 🟢 New lines added here — do not remove or move existing logic
            LoggerUtil.info("📡 Using transport method: " + transport);
            NetworkUtil.broadcastModeToReceiver(transport, targetHost, port);

            handler.send(prompt("Your name (for logs only): "), file, transport, port, targetHost);


            // 9) Completion & logs
            Instant end = Instant.now();
            summarizeSuccess(file, Duration.between(start, end));
        } catch (Exception ex) {
            LoggerUtil.error(PromptManager.GENERIC_ERROR, ex);
        }
    }

    // =========================
    // 1) Session / env helpers
    // =========================

    private void startSessionIntro() {
        LoggerUtil.info(PromptManager.WELCOME);
        LoggerUtil.info(PromptManager.ENTER_SESSION_NAME);
        String session = in.nextLine().trim();
        if (session.isEmpty()) {
            session = "default-session";
        }

        LoggerUtil.info(PromptManager.ASK_USER_NAME);
        String name = in.nextLine().trim();
        if (name.isEmpty()) {
            name = "Friend";
        }
        LoggerUtil.info(PromptManager.hiUser(name));

        LoggerUtil.info(PromptManager.QUICK_CHECK);
        LoggerUtil.info(PromptManager.CHECK_JAVA);
        // Minimal runtime check; if not Java 17, we warn (do not hard-exit for CI environments)
        String v = System.getProperty("java.version", "unknown");
        if (v.startsWith("17")) {
            LoggerUtil.info(PromptManager.JAVA_OK);
        } else {
            LoggerUtil.warn(PromptManager.JAVA_BAD + " (Detected: " + v + ")");
        }

        LoggerUtil.info(PromptManager.logSessionStart());
    }

    // =====================
    // 2) Choose transport
    // =====================

    private String chooseTransport() {
        while (true) {
            LoggerUtil.info(PromptManager.ASK_TRANSPORT);
            String choice = in.nextLine().trim().toUpperCase(Locale.ROOT);

            switch (choice) {
                case "HTTP":
                    LoggerUtil.info(PromptManager.CHOSE_HTTP);
                    return "HTTP";
                case "ZEROTIER":
                case "ZT":
                    LoggerUtil.info(PromptManager.CHOSE_ZEROTIER);
                    return "ZeroTier";
                case "S3":
                case "AWS":
                    LoggerUtil.info(PromptManager.CHOSE_S3);
                    return "S3";
                default:
                    LoggerUtil.warn(PromptManager.INVALID_INPUT);
            }
        }
    }

    // ==========================
    // 3) File selection & checks
    // ==========================

    private File chooseAndValidateFile() throws Exception {
        LoggerUtil.info(PromptManager.PROMPT_BROWSE);

        while (true) {
            String path = in.nextLine().trim();
            if (path.isEmpty()) {
                LoggerUtil.warn(PromptManager.NO_FILE_SELECTED);
                continue;
            }

            File file = new File(path);
            if (!file.exists() || !file.isFile()) {
                LoggerUtil.error(PromptManager.FILE_NOT_FOUND);
                continue;
            }

            long size = file.length();
            if (size == 0) {
                LoggerUtil.warn(PromptManager.FILE_EMPTY);
                continue;
            }

            String lower = file.getName().toLowerCase(Locale.ROOT);
            if (!(lower.endsWith(".zip") || lower.endsWith(".7z") || lower.endsWith(".tar")
                    || lower.endsWith(".gz") || lower.endsWith(".rar") || lower.endsWith(".pdf")
                    || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png"))) {
                LoggerUtil.warn(PromptManager.FILE_TYPE_BLOCKED);
                continue;
            }

            double mb = size / (1024.0 * 1024.0);
            LoggerUtil.info(PromptManager.totalSizeMb(mb));

            if (mb > 512) {
                LoggerUtil.info(PromptManager.LARGE_FILE_WARN);
                LoggerUtil.info(PromptManager.CHUNK_INFO);
            }

            LoggerUtil.info(PromptManager.confirmFile(file.getName(),
                    String.format("%.2f MB", mb)));

            String confirm = in.nextLine().trim().toUpperCase(Locale.ROOT);
            if ("Y".equals(confirm)) {
                return file;
            }
        }
    }

    // ==========================
    // 4) Duplicate handling
    // ==========================

    private void handleDuplicateCheck(File file) {
        String fingerprint = file.getName() + ":" + file.length();

        if (sentFileFingerprints.contains(fingerprint)) {
            LoggerUtil.warn(PromptManager.DUP_FOUND);
            String ans = in.nextLine().trim().toUpperCase(Locale.ROOT);
            if (!"Y".equals(ans)) {
                throw new IllegalStateException("User aborted due to duplicate file.");
            }
        } else {
            LoggerUtil.info(PromptManager.DUP_CHECK_OK);
            sentFileFingerprints.add(fingerprint);
        }
    }

    // =====================
    // 5) Port selection
    // =====================

    private int findFreePort(int from, int to) throws Exception {
        for (int p = from; p <= to; p++) {
            try (ServerSocket socket = new ServerSocket(p)) {
                socket.setReuseAddress(true);
                LoggerUtil.info(PromptManager.usingPort(p));
                logIpAndPort(p);
                return p;
            } catch (Exception ignored) {
                // Port in use; keep scanning.
            }
        }
        LoggerUtil.error(PromptManager.PORT_BIND_FAIL);
        throw new IllegalStateException(PromptManager.PORT_BIND_FAIL);
    }

    private void logIpAndPort(int port) throws Exception {
        String ip = InetAddress.getLocalHost().getHostAddress();
        LoggerUtil.info(PromptManager.logUsingPort(ip, port));
    }

    // ======================
    // 6) Access code / token
    // ======================

    private String handleAccessCode() {
        LoggerUtil.info(PromptManager.ASK_ACCESS_CODE);
        String code = in.nextLine().trim();

        // Simple demo rule: "1234" is accepted if provided.
        if (!code.isEmpty() && !"1234".equals(code)) {
            LoggerUtil.warn(PromptManager.ACCESS_CODE_BAD);
            throw new IllegalArgumentException("Invalid access code.");
        }

        LoggerUtil.info(PromptManager.ACCESS_CODE_OK);
        return code;
    }

    // =======================
    // 7) Start confirmation
    // =======================

    private boolean confirmStart() {
        LoggerUtil.info(PromptManager.READY_TO_SEND);
        String ready = in.nextLine().trim();
        if (!"READY".equalsIgnoreCase(ready)) {
            return false;
        }

        // Very tiny "human" check: random 3 digits
        int code = new Random().nextInt(900) + 100;
        LoggerUtil.info(PromptManager.HUMAN_CHECK + " " + code);
        String entered = in.nextLine().trim();
        return String.valueOf(code).equals(entered);
    }

    // =====================
    // 8) Handler selection
    // =====================

    private TransferMethod createHandlerFor(String transport) {
        switch (transport) {
            case "HTTP":
                return new HttpTransferHandler();
            case "ZeroTier":
                return new ZeroTierTransferHandler();
            case "S3":
                return new S3TransferHandler();
            default:
                throw new IllegalArgumentException("Unsupported transport: " + transport);
        }
    }

    private String askTargetHostForTransport(String transport) {
        if ("HTTP".equals(transport)) {
            LoggerUtil.info(PromptManager.ASK_SENDER_IP);
            LoggerUtil.info("Tip: If you are the Sender, this is the Receiver's IP.");
            return in.nextLine().trim();
        }
        if ("ZeroTier".equals(transport)) {
            LoggerUtil.info(PromptManager.ZT_NOTE);
            LoggerUtil.info("Enter ZeroTier peer IP / Node ID:");
            return in.nextLine().trim();
        }
        if ("S3".equals(transport)) {
            LoggerUtil.info(PromptManager.S3_NOTE);
            LoggerUtil.info("Enter pre-configured download endpoint or leave blank:");
            return in.nextLine().trim();
        }
        return "";
    }

    // ==================
    // 9) Completion logs
    // ==================

    private void summarizeSuccess(File file, Duration duration) throws Exception {
        String downloadUrl = "(receiver-specific /api endpoint)";
        LoggerUtil.info(PromptManager.uploadDone(downloadUrl));

        double mb = Files.size(Path.of(file.getPath())) / (1024.0 * 1024.0);
        LoggerUtil.info(PromptManager.transferSummary(
                1,
                file.getName(),
                duration.toSeconds() + " seconds"));

        LoggerUtil.info(PromptManager.STOPPING_SERVER);
        // Actual HTTP server stop is managed by the handler; nothing to do here.
        LoggerUtil.info(PromptManager.SERVER_STOPPED);
        LoggerUtil.info(PromptManager.SESSION_END_OK);
    }

    // Utility to show inline prompt + read; can be reused by UI adapter.
    private String prompt(String message) {
        LoggerUtil.info(message);
        return in.nextLine().trim();
    }

    private boolean pingReceiver(String targetHost, int port) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(targetHost, port), 2000);
            socket.close();
            LoggerUtil.info("✅ Receiver reachable at " + targetHost + ":" + port);
            return true;
        } catch (Exception e) {
            LoggerUtil.warn("❌ Receiver not reachable at " + targetHost + ":" + port);
            return false;
        }
    }

//    public class NetworkUtil {
//        public static void broadcastModeToReceiver(String method, String host, int port) {
//            try {
//                // Optional: send small JSON ping announcing the method
//                URL url = new URL("http://" + host + ":" + port + "/handshake?method=" + method);
//                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//                conn.setConnectTimeout(2000);
//                conn.getResponseCode(); // trigger
//                LoggerUtil.info("🔄 Notified receiver of transport mode: " + method);
//            } catch (Exception e) {
//                LoggerUtil.warn("⚠️ Could not notify receiver about transport mode.");
//            }
//        }
    }



