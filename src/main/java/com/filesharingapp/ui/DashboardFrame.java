package com.filesharingapp.ui;

import com.filesharingapp.core.PromptManager;
import com.filesharingapp.core.Sender;
import com.filesharingapp.core.Receiver;
import com.filesharingapp.utils.LoggerUtil;

import javax.swing.*;
import java.awt.*;

/**
 * DashboardFrame
 * --------------
 * Simple Swing GUI wrapper that lets user:
 *  - choose Sender / Receiver
 *  - click Start
 *  - see logs in a text area
 *
 * Uses existing Sender and Receiver logic.
 */
public class DashboardFrame extends JFrame {

    private final JComboBox<String> roleBox;
    private final JButton startButton;
    private final JButton logButton;
    private final JTextArea logArea;

    private final Sender sender = new Sender();
    private final Receiver receiver = new Receiver();

    public DashboardFrame() {
        super("File Sharing App - Dashboard");

        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);

        // Top panel
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Who are you today?"));
        roleBox = new JComboBox<>(new String[]{"Sender", "Receiver"});
        top.add(roleBox);

        startButton = new JButton("Start Flow");
        startButton.addActionListener(e -> onStart());
        top.add(startButton);

        logButton = new JButton("Open Log Viewer");
        logButton.addActionListener(e -> new LogViewerFrame().setVisible(true));
        top.add(logButton);

        add(top, BorderLayout.NORTH);

        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        // Accessibility: allow keyboard focus
        startButton.setMnemonic('S');
        roleBox.setFocusable(true);

        appendLog(PromptManager.WELCOME);
        appendLog(PromptManager.HELP_HINT);
    }

    private void onStart() {
        String role = (String) roleBox.getSelectedItem();
        appendLog("[UI] Selected role: " + role);

        new Thread(() -> {
            try {
                if ("Sender".equalsIgnoreCase(role)) {
                    sender.runInteractive();
                } else {
                    receiver.runInteractive();
                }
            } catch (Exception ex) {
                LoggerUtil.error("Error in flow", ex);
            }
        }).start();
    }

    private void appendLog(String msg) {
        logArea.append(msg + "\n");
    }

    /**
     * Simple launcher for IntelliJ run config:
     * Main class: com.filesharingapp.ui.DashboardFrame
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DashboardFrame().setVisible(true));
    }
}
