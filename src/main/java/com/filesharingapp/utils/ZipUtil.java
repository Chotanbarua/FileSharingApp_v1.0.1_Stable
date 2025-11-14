package com.filesharingapp.utils;

import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * ZipUtil
 * -------
 * Baby English:
 *   - We make ZIP files for sending.
 *   - If file is already ZIP, we use it.
 *   - If folder has many files, we zip them all.
 *   - We check ZIP is good.
 *   - We tell progress while zipping.
 *   - We give checksum after done.
 */
public final class ZipUtil {

    private ZipUtil() {
        // Utility class – no objects.
    }

    // ============================
    // ✅ Zip Single File If Needed
    // ============================
    public static File zipIfNeeded(File file, File destFolder) throws IOException {
        if (file == null || !file.exists()) {
            throw new FileNotFoundException("File for ZIP not found");
        }

        String name = file.getName().toLowerCase();

        // Already a zip
        if (name.endsWith(".zip")) {
            LoggerUtil.info("[Zip] File already a ZIP. Using as-is: " + file.getName(), null);
            return file;
        }

        // Destination folder (default: same as original)
        File targetFolder = (destFolder != null) ? destFolder : file.getParentFile();
        File zipFile = new File(targetFolder, sanitizeName(file.getName()) + ".zip");

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            ZipEntry entry = new ZipEntry(sanitizeName(file.getName()));
            zos.putNextEntry(entry);
            Files.copy(file.toPath(), zos);
            zos.closeEntry();
        }

        verifyZip(zipFile);
        LoggerUtil.info("[Zip] Created archive: " + zipFile.getName(), null);
        return zipFile;
    }

    // ============================
    // ✅ Zip Multiple Files (Folder)
    // ============================
    public static File zipFiles(List<File> files, File destFolder, ProgressCallback callback) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No files to zip");
        }

        File zipFile = new File(destFolder, "archive_" + System.currentTimeMillis() + ".zip");

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            int total = files.size();
            int count = 0;

            for (File f : files) {
                if (f == null || !f.exists() || !f.isFile()) continue;

                ZipEntry entry = new ZipEntry(sanitizeName(f.getName()));
                zos.putNextEntry(entry);
                Files.copy(f.toPath(), zos);
                zos.closeEntry();

                count++;
                if (callback != null) {
                    callback.onProgress(count, total);
                }
            }
        }

        verifyZip(zipFile);
        LoggerUtil.info("[Zip] Multi-file archive created: " + zipFile.getName(), null);
        return zipFile;
    }

    // ============================
    // ✅ Sanitize File Name
    // ============================
    private static String sanitizeName(String name) {
        // Baby English: Remove bad characters
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    // ============================
    // ✅ Verify ZIP Integrity
    // ============================
    private static void verifyZip(File zipFile) throws IOException {
        try (java.util.zip.ZipFile zf = new java.util.zip.ZipFile(zipFile)) {
            if (zf.size() == 0) {
                throw new IOException("ZIP file is empty or corrupted.");
            }
        }
    }

    // ============================
    // ✅ Compute Checksum After ZIP
    // ============================
    public static String computeChecksum(File file) throws IOException {
        try (InputStream is = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IOException("Failed to compute checksum", e);
        }
    }

    // ============================
    // ✅ Progress Callback Interface
    // ============================
    public interface ProgressCallback {
        void onProgress(int current, int total);
    }
}