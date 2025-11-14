package com.filesharingapp.server;

import com.filesharingapp.core.TransferContext;
import com.filesharingapp.security.AesUtil;
import com.filesharingapp.utils.LoggerUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * ChunkUploadService
 * ------------------
 * Baby-English:
 *   ✔ This class decides where and how we save uploaded bytes.
 *   ✔ Two main styles:
 *       1) handleStream(...) → ONE big HTTP stream (supports resume).
 *       2) handleChunk(...) → Many small pieces (chunks).
 *
 *   ✔ Folders:
 *       - "received" → final files ready for download.
 *       - "tmp/uploads" → temporary chunks before merge.
 *
 *   ✔ AES:
 *       - If sender encrypted → decrypt before writing (optional).
 *       - If AES disabled → raw copy.
 */
public final class ChunkUploadService {

    private static final Path TMP_DIR = Path.of("tmp", "uploads");
    private static final Path RECEIVED_DIR = Path.of("received");

    static {
        try {
            Files.createDirectories(TMP_DIR);
            Files.createDirectories(RECEIVED_DIR);
        } catch (IOException e) {
            LoggerUtil.error("Failed to init upload directories", e);
        }
    }

    private ChunkUploadService() {}

    // ============================================================
    // 1️⃣ STREAM-BASED UPLOAD (supports resume)
    // ============================================================

    /**
     * handleStream
     * ------------
     * Baby-English:
     *   ✔ We get ONE long stream of bytes from HTTP.
     *   ✔ If file exists → resume from offset.
     *   ✔ If AES enabled → decrypt before writing.
     *
     * @param transferId unique transfer ID
     * @param fileName   final file name
     * @param totalBytes total size (or -1 if unknown)
     * @param input      HTTP InputStream
     * @param aesPassword optional AES password (null if disabled)
     * @return path to saved file
     */
    public static Path handleStream(String transferId,
                                    String fileName,
                                    long totalBytes,
                                    InputStream input,
                                    String aesPassword) throws IOException {

        if (transferId == null || transferId.isBlank()) throw new IOException("Missing transferId");
        if (fileName == null || fileName.isBlank()) throw new IOException("Missing fileName");

        Path outPath = RECEIVED_DIR.resolve(fileName);
        long already = Files.exists(outPath) ? Files.size(outPath) : 0L;

        LoggerUtil.info("[UploadStream] Saving to: " + outPath + " (resume offset=" + already + ")");

        TransferStatusRegistry.begin(transferId, fileName, totalBytes);
        TransferContext.setFinalOutputPath(outPath.toString());
        TransferContext.setResumeOffsetBytes(already);

        try (OutputStream out = new BufferedOutputStream(
                Files.newOutputStream(outPath,
                        java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.APPEND))) {

            byte[] buffer = new byte[8192];
            long written = already;
            int len;

            while ((len = input.read(buffer)) != -1) {
                byte[] toWrite = buffer;
                int writeLen = len;

                // AES decrypt if enabled
                if (aesPassword != null) {
                    try {
                        byte[] chunk = Arrays.copyOf(buffer, len);
                        toWrite = AesUtil.decrypt(chunk, aesPassword);
                        writeLen = toWrite.length;
                    } catch (Exception e) {
                        LoggerUtil.error("[AES] Decryption failed during stream upload.", e);
                        throw new IOException("AES decryption error");
                    }
                }

                out.write(toWrite, 0, writeLen);
                written += writeLen;

                TransferStatusRegistry.progress(written);
                TransferContext.addReceivedBytes(transferId, writeLen);

                if (written % (512 * 1024) < buffer.length) {
                    LoggerUtil.info("[UploadStream] Written " + written + " bytes so far.");
                }
            }
            out.flush();
        } catch (IOException e) {
            TransferStatusRegistry.fail("Stream upload error: " + e.getMessage());
            throw e;
        }

        TransferStatusRegistry.complete(outPath.toAbsolutePath().toString());
        LoggerUtil.success("[UploadStream] Finished. Total bytes now on disk=" + Files.size(outPath));
        TransferContext.setIncomingName(fileName);

        return outPath;
    }

    // ============================================================
    // 2️⃣ CHUNK-BASED UPLOAD (supports AES + merge)
    // ============================================================

    /**
     * handleChunk
     * -----------
     * Baby-English:
     *   ✔ Save each chunk in tmp/uploads.
     *   ✔ If AES enabled → decrypt before saving.
     *   ✔ When all chunks arrive → merge into final file.
     *
     * @return "CHUNK-STORED" or "MERGED"
     */
    public static String handleChunk(String transferId,
                                     String fileName,
                                     int chunkIndex,
                                     long totalBytes,
                                     byte[] body,
                                     String aesPassword) throws IOException {

        // NOTE: RECEIVED_DIR and TMP_DIR must be correctly defined class-level static variables
        // as per the rest of your ChunkUploadService.java file.

        if (transferId == null || transferId.isBlank()) throw new IOException("Missing transferId");
        if (fileName == null || fileName.isBlank()) throw new IOException("Missing fileName");
        if (body == null) throw new IOException("Missing chunk body");

        if (chunkIndex == 0) {
            TransferStatusRegistry.begin(transferId, fileName, totalBytes);
            TransferContext.initChunkState(transferId, (int) Math.ceil(totalBytes / (1024 * 1024))); // rough chunk count
            TransferContext.setFinalOutputPath(RECEIVED_DIR.resolve(fileName).toString());
        }

        byte[] plain = body;
        if (aesPassword != null) {
            try {
                plain = AesUtil.decrypt(body, aesPassword);
            } catch (Exception e) {
                LoggerUtil.error("[AES] Chunk decryption failed.", e);
                throw new IOException("AES decryption error");
            }
        }

        Path chunkFile = TMP_DIR.resolve(transferId + "." + chunkIndex + ".chunk");
        Files.write(chunkFile, plain);
        LoggerUtil.info("[UploadChunk] Stored chunk " + chunkIndex + " (" + plain.length + " bytes)");

        TransferContext.markChunkReceived(transferId, chunkIndex);
        TransferContext.addReceivedBytes(transferId, plain.length);

        long sum = Files.list(TMP_DIR)
                .filter(p -> p.getFileName().toString().startsWith(transferId + "."))
                .mapToLong(p -> {
                    try { return Files.size(p); } catch (IOException e) { return 0L; }
                }).sum();

        TransferStatusRegistry.progress(sum);

        if (totalBytes > 0 && sum >= totalBytes && TransferContext.areAllChunksReceived(transferId)) {
            Path out = RECEIVED_DIR.resolve(fileName);
            LoggerUtil.info("[UploadChunk] All chunks received. Merging into " + out);

            try (OutputStream outStream = Files.newOutputStream(out)) {
                int idx = 0;
                while (true) {
                    Path cf = TMP_DIR.resolve(transferId + "." + idx + ".chunk");
                    if (!Files.exists(cf)) break;
                    Files.copy(cf, outStream);
                    idx++;
                }
            }

            Files.list(TMP_DIR)
                    .filter(p -> p.getFileName().toString().startsWith(transferId + "."))
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException e) {
                            LoggerUtil.warn("[UploadChunk] Failed to delete chunk " + p);
                        }
                    });

            // FIX: Replaced undefined 'safeFileName' with the correct input parameter 'fileName'
            TransferStatusRegistry.complete(Path.of("received", fileName).toAbsolutePath().toString());

            TransferContext.setIncomingName(fileName);
            TransferContext.clearTransfer(transferId);

            LoggerUtil.success("[UploadChunk] Merge complete for " + transferId);
            return "MERGED";
        }

        return "CHUNK-STORED";
    }
    // ============================================================
    // 3️⃣ Helper: Resolve received file
    // ============================================================
    public static Path getReceivedFile(String name) {
        return RECEIVED_DIR.resolve(name);
    }
}
