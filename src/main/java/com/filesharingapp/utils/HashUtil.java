package com.filesharingapp.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * HashUtil
 * --------
 * Baby-English:
 * ✔ We make digital fingerprints (SHA-256) for strings and files.
 * ✔ Used to check file integrity after transfer.
 */
public class HashUtil {

    private HashUtil() {}

    /**
     * Generates a SHA-256 hash for the given file.
     * This is the core method for integrity checks.
     *
     * @param file The File object to hash.
     * @return SHA-256 hash in hexadecimal format.
     */
    public static String sha256Hex(File file) throws IOException {
        try (InputStream is = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            LoggerUtil.error("SHA-256 algorithm not found.", e);
            throw new IOException("SHA-256 algorithm not found.", e);
        }
    }

    /**
     * Generates a hash for the given input string.
     *
     * @param input The string to hash.
     * @param algorithm The hashing algorithm (e.g., "MD5", "SHA-256").
     * @return The hashed value in hexadecimal format.
     */
    public static String generateHash(String input, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hashBytes = digest.digest(input.getBytes());
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Invalid hashing algorithm: " + algorithm, e);
        }
    }

    /**
     * Generates a SHA-256 hash for the given input string.
     */
    public static String sha256(String input) {
        return generateHash(input, "SHA-256");
    }

    /**
     * Converts a byte array to a hexadecimal string.
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}