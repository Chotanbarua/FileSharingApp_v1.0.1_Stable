package com.filesharingapp.ui;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * LogViewerFrame
 * --------------
 * Simple GUI to display the rotating log file contents.
 */
public class LogViewerFrame extends JFrame {

    private final JTextArea area;

    public LogViewerFrame() {
        super("File Sharing App - Logs");
        setSize(700, 500);
        setLocationRelativeTo(null);

        area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        add(new JScrollPane(area), BorderLayout.CENTER);

        loadLogs();
    }

    private void loadLogs() {
        try {
            Path logPath = Path.of("logs", "filesharingapp.log");
            if (Files.exists(logPath)) {
                area.setText(Files.readString(logPath));
            } else {
                area.setText("No log file found yet at " + logPath.toAbsolutePath());
            }
        } catch (IOException e) {
            area.setText("Failed to load logs: " + e.getMessage());
        }
    }
}
