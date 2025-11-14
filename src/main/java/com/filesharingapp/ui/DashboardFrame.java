package com.filesharingapp.ui;

import com.filesharingapp.core.PromptManager;
import com.filesharingapp.core.Sender;
import com.filesharingapp.core.Receiver;
import com.filesharingapp.transfer.TargetConfig;
import com.filesharingapp.utils.LoggerUtil;
import com.filesharingapp.utils.ValidationUtil;
import com.filesharingapp.utils.ValidationMessages;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Locale;

/**
 * DashboardFrame
 * --------------
 * Baby English:
 * - This is the main app window.
 * - User picks role (Sender or Receiver).
 * - User enters IP, Port, Method.
 * - User can start transfer and see logs.
 */
public class DashboardFrame extends JFrame {

    private final JComboBox<String> roleBox;
    private final JComboBox<String> methodBox;
    private final JTextField nameField; // Added field for user name
    private final JTextField ipField;
    private final JTextField portField;
    private final JTextField aesKeyField;
    private final JFileChooser fileChooser; // Added file chooser
    private final JButton startButton;
    private final JTextArea logArea;
    private final JProgressBar progressBar;

    private boolean darkMode = false;

    private final Sender sender = new Sender();
    private final Receiver receiver = new Receiver();

    private File fileToTransfer;

    public DashboardFrame() {
        super("File Sharing App - Dashboard");

        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(700, 550); // Increased size slightly
        setLocationRelativeTo(null);

        fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        // Top panel for controls
        JPanel top = new JPanel(new GridLayout(4, 2, 8, 8));
        top.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Input Fields ---
        top.add(new JLabel("Your Name:"));
        nameField = new JTextField("User_");
        top.add(nameField);

        top.add(new JLabel("Role:"));
        roleBox = new JComboBox<>(new String[]{"Sender", "Receiver"});
        top.add(roleBox);

        top.add(new JLabel("Method:"));
        methodBox = new JComboBox<>(new String[]{"HTTP", "ZeroTier", "S3"});
        top.add(methodBox);

        top.add(new JLabel("Target IP / Bucket:"));
        ipField = new JTextField();
        top.add(ipField);

        top.add(new JLabel("Port (HTTP/ZT):"));
        portField = new JTextField("8080");
        top.add(portField);

        top.add(new JLabel("AES Key (optional):"));
        aesKeyField = new JTextField();
        top.add(aesKeyField);

        JButton filePicker = new JButton("Select File (Sender)");
        filePicker.addActionListener(e -> pickFile());
        top.add(filePicker);

        add(top, BorderLayout.NORTH);

        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        startButton = new JButton("Start Transfer");
        startButton.addActionListener(e -> onStart());
        buttonPanel.add(startButton);

        JButton logButton = new JButton("Open Log Viewer");
        logButton.addActionListener(e -> new LogViewerFrame().setVisible(true));
        buttonPanel.add(logButton);

        JButton darkModeBtn = new JButton("Dark Mode");
        darkModeBtn.addActionListener(this::toggleDarkMode);
        buttonPanel.add(darkModeBtn);

        add(buttonPanel, BorderLayout.SOUTH);

        // Log area + progress bar
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(logArea);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        centerPanel.add(progressBar, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        appendLog(PromptManager.WELCOME);
        appendLog(PromptManager.HELP_HINT);
    }

    private void pickFile() {
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            fileToTransfer = fileChooser.getSelectedFile();
            if (ValidationUtil.validateFile(fileToTransfer) != null) {
                appendLog("❌ Invalid File: " + ValidationMessages.FILE_REQUIRED);
                fileToTransfer = null;
            } else {
                appendLog("✅ File selected: " + fileToTransfer.getName());
            }
        }
    }

    /**
     * Baby English:
     * - When user clicks Start:
     * - We check inputs.
     * - We run Sender or Receiver in a new thread.
     */
    private void onStart() {
        String role = (String) roleBox.getSelectedItem();
        String name = nameField.getText().trim();

        if (ValidationUtil.validateName(name) != null) {
            appendLog("⚠️ Please enter a valid name.");
            return;
        }

        if ("Sender".equalsIgnoreCase(role) && fileToTransfer == null) {
            appendLog("⚠️ Sender must select a file.");
            return;
        }

        TargetConfig config = buildTargetConfig();
        if (!config.isValid()) {
            appendLog("❌ Invalid configuration. Check IP/Port/Bucket.");
            return;
        }

        appendLog("[UI] Starting flow as " + role + " via " + config.getMode() + "...");
        startButton.setEnabled(false);
        progressBar.setValue(0);

        new Thread(() -> {
            try {
                if ("Sender".equalsIgnoreCase(role)) {
                    // FIX: Changed signature to match updated Sender class
                    sender.runInteractive(name, config, fileToTransfer);
                } else {
                    // FIX: Changed signature to match updated Receiver class
                    receiver.runInteractive(name, config);
                }
                SwingUtilities.invokeLater(() -> startButton.setEnabled(true));
            } catch (Exception ex) {
                LoggerUtil.error("Error in transfer flow", ex);
                SwingUtilities.invokeLater(() -> {
                    appendLog("❌ Error: " + ex.getMessage());
                    startButton.setEnabled(true);
                });
            }
        }).start();
    }

    /**
     * Gathers and validates UI input into a TargetConfig object.
     */
    private TargetConfig buildTargetConfig() {
        String mode = (String) methodBox.getSelectedItem();
        String host = ipField.getText().trim();
        String portRaw = portField.getText().trim();
        String aesKey = aesKeyField.getText().trim();

        int port = 0;
        try {
            port = portRaw.isEmpty() ? 0 : Integer.parseInt(portRaw);
        } catch (NumberFormatException ignored) {}

        String validationError = null;

        if ("HTTP".equalsIgnoreCase(mode) || "ZEROTIER".equalsIgnoreCase(mode)) {
            validationError = ValidationUtil.validateHost(host);
            if (validationError == null) validationError = ValidationUtil.validatePort(port);
        } else if ("S3".equalsIgnoreCase(mode)) {
            validationError = ValidationUtil.validateS3Bucket(host);
        }

        if (validationError != null) return TargetConfig.createInvalid();

        return TargetConfig.createValid(host, port, mode, aesKey);
    }

    /**
     * Baby English:
     * - We add text to log area safely.
     */
    private void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    /**
     * Baby English:
     * - Switch colors for dark mode.
     */
    private void toggleDarkMode(ActionEvent e) {
        darkMode = !darkMode;
        Color bg = darkMode ? Color.BLACK : Color.WHITE;
        Color fg = darkMode ? Color.GREEN : Color.BLACK;

        logArea.setBackground(bg);
        logArea.setForeground(fg);
    }

    /**
     * Baby English:
     * - Update progress bar from other threads.
     */
    public void updateProgress(int percent) {
        SwingUtilities.invokeLater(() -> progressBar.setValue(percent));
    }
}