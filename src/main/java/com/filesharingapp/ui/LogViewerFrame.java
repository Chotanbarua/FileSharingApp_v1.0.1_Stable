package com.filesharingapp.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * LogViewerFrame
 * --------------
 * Baby English:
 *   - This window shows the app logs.
 *   - We can refresh logs.
 *   - We can search logs.
 *   - We can clear view.
 *   - We make it nice for user eyes.
 */
public class LogViewerFrame extends JFrame {

    private final JTextArea area;
    private final JTextField searchField;
    private final JButton refreshBtn;
    private final JButton clearBtn;
    private final JButton darkModeBtn;
    private boolean darkMode = false;

    public LogViewerFrame() {
        super("File Sharing App - Logs");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Main text area
        area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        JScrollPane scrollPane = new JScrollPane(area);

        // Top panel with controls
        JPanel topPanel = new JPanel(new BorderLayout());
        searchField = new JTextField();
        searchField.setToolTipText("Type text to search in logs");
        topPanel.add(searchField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        refreshBtn = new JButton("Refresh");
        clearBtn = new JButton("Clear");
        darkModeBtn = new JButton("Dark Mode");
        buttonPanel.add(refreshBtn);
        buttonPanel.add(clearBtn);
        buttonPanel.add(darkModeBtn);
        topPanel.add(buttonPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Load logs initially
        loadLogs();

        // Add listeners
        refreshBtn.addActionListener(e -> loadLogs());
        clearBtn.addActionListener(e -> area.setText(""));
        darkModeBtn.addActionListener(this::toggleDarkMode);
        searchField.addActionListener(e -> searchLogs());

        // Auto-refresh every 10 seconds
        Timer timer = new Timer(10000, e -> loadLogs());
        timer.start();
    }

    /**
     * Baby English:
     *   - We read log file line by line.
     *   - We show it in the text area.
     *   - If file missing, we tell user.
     */
    private void loadLogs() {
        SwingUtilities.invokeLater(() -> {
            try {
                Path logPath = Path.of("logs", "filesharingapp.log"); // TODO: use AppConfig for dynamic path
                if (Files.exists(logPath)) {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader reader = Files.newBufferedReader(logPath)) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
                    }
                    area.setText(sb.toString());
                } else {
                    area.setText("No log file found yet at " + logPath.toAbsolutePath());
                }
            } catch (IOException e) {
                area.setText("Failed to load logs: " + e.getMessage());
            }
        });
    }

    /**
     * Baby English:
     *   - We find text in logs.
     *   - We highlight it by selecting.
     */
    private void searchLogs() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) return;

        String content = area.getText();
        int index = content.indexOf(query);
        if (index >= 0) {
            area.requestFocus();
            area.select(index, index + query.length());
        } else {
            JOptionPane.showMessageDialog(this, "Text not found: " + query);
        }
    }

    /**
     * Baby English:
     *   - We switch colors for dark mode.
     */
    private void toggleDarkMode(ActionEvent e) {
        darkMode = !darkMode;
        if (darkMode) {
            area.setBackground(Color.BLACK);
            area.setForeground(Color.GREEN);
        } else {
            area.setBackground(Color.WHITE);
            area.setForeground(Color.BLACK);
        }
    }
}