package com.filesharingapp.security;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

/**
 * HashUtil
 * --------
 * Utility for computing SHA-256 checksum.
 */
public final class HashUtil {

    private HashUtil() {}

    /** Compute SHA-256 of raw byte array */
    public static String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /** Compute SHA-256 of a File (used by Receiver checksum verification) */
    public static String sha256Hex(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute SHA-256 for file: " + file.getAbsolutePath(), e);
        }
    }
}
