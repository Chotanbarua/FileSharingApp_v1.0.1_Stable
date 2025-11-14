package com.filesharingapp.utils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.zip.ZipFile;

/**
 * ValidationUtil
 * --------------
 * Baby English:
 * - We check user input.
 */
public final class ValidationUtil {

    private static final long MAX_FILE_BYTES = 5L * 1024 * 1024 * 1024; // 5 GB

    private ValidationUtil() {
        // Utility class – no objects.
    }

    // ============================
    // ✅ Name Validation
    // ============================
    public static String validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return ValidationMessages.NAME_REQUIRED;
        }
        boolean ok = name.matches("[A-Za-z0-9 _.-]{1,50}");
        return ok ? null : "Name can only have letters, numbers, space, dash, underscore, dot.";
    }

    // ============================
    // ✅ Mode Validation
    // ============================
    public static String validateMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return ValidationMessages.MODE_INVALID;
        }
        boolean ok = "HTTP".equalsIgnoreCase(mode)
                || "ZEROTIER".equalsIgnoreCase(mode)
                || "S3".equalsIgnoreCase(mode);
        return ok ? null : ValidationMessages.MODE_INVALID;
    }

    // ============================
    // ✅ File Validation (FIX for Sender.java)
    // ============================
    public static String validateFile(File f) {
        if (f == null) {
            return ValidationMessages.FILE_REQUIRED;
        }
        if (!f.exists() || !f.isFile()) {
            return ValidationMessages.FILE_NOT_FOUND;
        }
        if (f.length() <= 0) {
            return ValidationMessages.FILE_EMPTY;
        }
        if (f.length() > MAX_FILE_BYTES) {
            return ValidationMessages.FILE_TOO_LARGE;
        }
        return null;
    }

    // ============================
    // ✅ ZIP File Validation
    // ============================
    public static String validateZipFile(String path) {
        // ... (existing logic)
        if (path == null || path.isBlank()) {
            return ValidationMessages.FILE_REQUIRED;
        }
        File f = new File(path.trim());
        if (!f.exists() || !f.isFile()) {
            return ValidationMessages.FILE_NOT_FOUND;
        }
        if (!f.getName().toLowerCase().endsWith(".zip")) {
            return "File must be a .zip archive.";
        }
        if (f.length() <= 0) {
            return ValidationMessages.FILE_EMPTY;
        }
        if (f.length() > MAX_FILE_BYTES) {
            return ValidationMessages.FILE_TOO_LARGE;
        }
        try (ZipFile zip = new ZipFile(f)) {
            if (zip.size() == 0) {
                return "ZIP file is empty or corrupted.";
            }
        } catch (IOException e) {
            return "ZIP file appears corrupted.";
        }
        return null;
    }

    // ============================
    // ✅ Folder Validation
    // ============================
    public static String validateFolder(String path) {
        if (path == null || path.isBlank()) {
            return ValidationMessages.FOLDER_REQUIRED;
        }
        try {
            Path p = Path.of(path);
            if (Files.exists(p)) {
                return Files.isDirectory(p) ? null : ValidationMessages.FOLDER_DENIED;
            }
            Files.createDirectories(p);
            return null;
        } catch (Exception e) {
            return ValidationMessages.FOLDER_DENIED;
        }
    }

    // ============================
    // ✅ Host Validation
    // ============================
    public static String validateHost(String host) {
        if (host == null || host.isBlank()) {
            return ValidationMessages.HOST_REQUIRED;
        }
        boolean ok = host.matches("[A-Za-z0-9._-]{1,253}");
        return ok ? null : "Host name looks invalid.";
    }

    // ============================
    // ✅ Port Validation
    // ============================
    public static String validatePort(int port) {
        return (port > 0 && port <= 65535) ? null : ValidationMessages.PORT_INVALID;
    }

    // ============================
    // ✅ HTTP URL Validation
    // ============================
    public static String validateHttpUrl(String url) {
        if (url == null || url.isBlank()) {
            return ValidationMessages.HTTP_PATH_REQUIRED;
        }
        try {
            new URL(url);
            return null;
        } catch (MalformedURLException e) {
            return "Invalid HTTP URL format.";
        }
    }

    // ============================
    // ✅ ZeroTier ID Validation
    // ============================
    public static String validateZeroTierId(String id) {
        if (id == null || id.isBlank()) {
            return ValidationMessages.ZT_ID_INVALID;
        }
        return id.matches("^[0-9a-fA-F]{16}$") ? null : ValidationMessages.ZT_ID_INVALID;
    }

    // ============================
    // ✅ S3 Bucket Validation
    // ============================
    public static String validateS3Bucket(String bucket) {
        if (bucket == null || bucket.isBlank()) {
            return ValidationMessages.S3_BUCKET_REQUIRED;
        }
        boolean ok = bucket.matches("^[a-z0-9.-]{3,63}$") && !bucket.startsWith("-") && !bucket.endsWith("-");
        return ok ? null : "Bucket name must be 3-63 chars, lowercase letters, numbers, dots, or dashes.";
    }

    // ============================
    // ✅ AWS Region Validation
    // ============================
    public static String validateAwsRegion(String region) {
        if (region == null || region.isBlank()) {
            return ValidationMessages.S3_REGION_REQUIRED;
        }
        boolean ok = region.matches("^[a-z]{2}-[a-z]+-\\d+$");
        return ok ? null : "Region format looks wrong (example: us-east-1).";
    }

    // ============================
    // ✅ Resume File Validation
    // ============================
    public static String validateResumeFile(String tempFilePath) {
        if (tempFilePath == null || tempFilePath.isBlank()) {
            return null; // No resume file provided
        }
        File f = new File(tempFilePath);
        return (f.exists() && f.isFile() && f.length() > 0) ? null : "Resume file is missing or empty.";
    }

    // ============================
    // ✅ application.properties Validation
    // ============================
    public static String validateAppProperties(Properties props) {
        if (props == null) {
            return "Configuration file is missing.";
        }
        if (props.getProperty("app.http.port") == null) {
            return "Missing app.http.port in configuration.";
        }
        if (props.getProperty("app.mode") == null) {
            return "Missing app.mode in configuration.";
        }
        return null;
    }
}