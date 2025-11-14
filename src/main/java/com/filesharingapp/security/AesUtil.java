package com.filesharingapp.security;

import com.filesharingapp.utils.LoggerUtil;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * AesUtil
 * -------
 * Baby-English:
 *   ✔ We take a password from the user.
 *   ✔ We stretch it into a 32-byte key (AES-256).
 *   ✔ When encrypting:
 *       1) Make a random 16-byte IV.
 *       2) Write IV at the top of the file.
 *       3) Encrypt the rest and write it.
 *   ✔ When decrypting:
 *       1) Read first 16 bytes (IV).
 *       2) Decrypt the remaining bytes.
 *
 * Used by:
 *   - Sender (before sending)
 *   - Receiver (after download)
 *   - ChunkUploadService (for chunk-level AES)
 */
public final class AesUtil {

    /** AES block size for IV. */
   // private static final int IV_LENGTH = 16;
    public static final int IV_LENGTH = 16;

    /** AES key size (32 bytes = 256 bits). */
    private static final int KEY_LENGTH = 32;

    private AesUtil() {}

    // ============================================================
    // 🔐 Password → AES Key
    // ============================================================

    /**
     * Baby-English:
     *   - User types a password.
     *   - We turn it into a 32-byte array.
     *   - If short → repeat it.
     *   - If long → cut it.
     *   - Wrap into AES key.
     */
    public static SecretKey buildKeyFromPassword(String password) {
        if (password == null) password = "";
        byte[] pwdBytes = password.getBytes();
        byte[] keyBytes = new byte[KEY_LENGTH];

        for (int i = 0; i < KEY_LENGTH; i++) {
            keyBytes[i] = pwdBytes[i % pwdBytes.length];
        }

        return new SecretKeySpec(keyBytes, "AES");
    }

    // ============================================================
    // 🔒 Encrypt file
    // ============================================================

    /**
     * Steps:
     *   1) Make random IV.
     *   2) Write IV at top of output file.
     *   3) Encrypt input file and write encrypted bytes.
     */
    public static void encryptFile(File inputFile, File outputFile, SecretKey key) throws Exception {
        LoggerUtil.info("[AES] Encrypting file: " + inputFile.getName());

        byte[] ivBytes = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(ivBytes);
        IvParameterSpec iv = new IvParameterSpec(ivBytes);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);

        try (FileOutputStream fos = new FileOutputStream(outputFile);
             FileInputStream fis = new FileInputStream(inputFile)) {

            fos.write(ivBytes); // Write IV first

            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                byte[] enc = cipher.update(buffer, 0, read);
                if (enc != null) fos.write(enc);
            }

            byte[] finalBytes = cipher.doFinal();
            if (finalBytes != null) fos.write(finalBytes);
        }

        LoggerUtil.info("[AES] File encrypted → " + outputFile.getAbsolutePath());
    }

    // ============================================================
    // 🔓 Decrypt file
    // ============================================================

    /**
     * Steps:
     *   1) Read first 16 bytes as IV.
     *   2) Decrypt the rest.
     */
    public static void decryptFile(File inputFile, File outputFile, SecretKey key) throws Exception {
        LoggerUtil.info("[AES] Decrypting file: " + inputFile.getName());

        try (FileInputStream fis = new FileInputStream(inputFile)) {
            byte[] ivBytes = new byte[IV_LENGTH];
            if (fis.read(ivBytes) != IV_LENGTH) throw new IOException("Missing IV in encrypted file");

            IvParameterSpec iv = new IvParameterSpec(ivBytes);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key, iv);

            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    byte[] dec = cipher.update(buffer, 0, read);
                    if (dec != null) fos.write(dec);
                }

                byte[] finalBytes = cipher.doFinal();
                if (finalBytes != null) fos.write(finalBytes);
            }
        }

        LoggerUtil.info("[AES] File decrypted → " + outputFile.getAbsolutePath());
    }

    // ============================================================
    // 🆕 decrypt(byte[]) for ChunkUploadService
    // ============================================================

    /**
     * Baby-English:
     *   - First 16 bytes = IV.
     *   - Rest = encrypted data.
     *   - Return decrypted bytes.
     */
    public static byte[] decrypt(byte[] body, String password) throws Exception {
        if (body == null || body.length < IV_LENGTH) {
            throw new IllegalArgumentException("Encrypted payload too small");
        }

        byte[] ivBytes = Arrays.copyOfRange(body, 0, IV_LENGTH);
        byte[] encBytes = Arrays.copyOfRange(body, IV_LENGTH, body.length);

        SecretKey key = buildKeyFromPassword(password);
        IvParameterSpec iv = new IvParameterSpec(ivBytes);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        try {
            cipher.init(Cipher.DECRYPT_MODE, key, iv);
        } catch (Exception e) {
            LoggerUtil.warn("[AES] Wrong password or invalid IV.");
            throw new IOException("Wrong password");
        }

        return cipher.doFinal(encBytes);
    }

    // ============================================================
    // 🧹 Clear sensitive data
    // ============================================================

    public static void clearBytes(byte[] arr) {
        if (arr != null) Arrays.fill(arr, (byte) 0);
    }

    // ============================================================
    // 🆕 decryptStream(InputStream, OutputStream) for Server Decrypt
    // ============================================================

    /**
     * Steps:
     * 1) Read first 16 bytes as IV.
     * 2) Decrypt the rest directly to the output stream.
     * ✔ Avoids loading the entire file into memory.
     */
    public static void decryptStream(InputStream encryptedStream, OutputStream decryptedStream, SecretKey key) throws Exception {
        LoggerUtil.info("[AES] Starting stream decryption...", null);

        byte[] ivBytes = new byte[IV_LENGTH];
        if (encryptedStream.read(ivBytes) != IV_LENGTH) {
            throw new IOException("Missing IV in encrypted stream");
        }

        IvParameterSpec iv = new IvParameterSpec(ivBytes);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, iv);

        byte[] buffer = new byte[8192];
        int read;

        while ((read = encryptedStream.read(buffer)) != -1) {
            byte[] dec = cipher.update(buffer, 0, read);
            if (dec != null) decryptedStream.write(dec);
        }

        byte[] finalBytes = cipher.doFinal();
        if (finalBytes != null) decryptedStream.write(finalBytes);

        decryptedStream.flush();
        LoggerUtil.info("[AES] Stream decryption finished.", null);
    }

    // ============================================================
// 🆕 encryptChunk(byte[]) for HTTP streaming
// ============================================================

    /**
     * Encrypts a chunk of bytes with IV generation for the first call.
     * This is meant to be called repeatedly on a stream, but HTTP chunking
     * requires the IV to be prepended, so we assume the IV is handled
     * externally (by HttpTransferHandler) or is only applied to the *entire* file before stream.
     * For now, we stub this to use simple byte-array encryption.
     */
    public static byte[] encryptChunk(byte[] chunk, String password) throws Exception {
        SecretKey key = buildKeyFromPassword(password);

        // NOTE: This implementation is simplified for chunking, ideally stateful streaming cipher is used.
        // However, based on the input code, we'll implement a basic one that requires IV to be prepended by caller.
        // For simplicity and compilation, we will assume the full encryption happened externally.
        // Since the main logic uses encryptFile, this method is likely unnecessary but required for compilation.

        // Stub implementation:
        byte[] ivBytes = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(ivBytes);
        IvParameterSpec iv = new IvParameterSpec(ivBytes);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);

        byte[] encrypted = cipher.doFinal(chunk);

        // Prepend IV to the chunk for transport (simplistic)
        byte[] result = new byte[IV_LENGTH + encrypted.length];
        System.arraycopy(ivBytes, 0, result, 0, IV_LENGTH);
        System.arraycopy(encrypted, 0, result, IV_LENGTH, encrypted.length);

        return result;
    }
}
