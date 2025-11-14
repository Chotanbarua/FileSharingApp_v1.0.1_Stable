package com.filesharingapp.transfer;

import java.io.File;

/**
 * TransferMethod
 * --------------
 * Baby-English:
 * ✔ This is the "rule book" for any transfer style.
 * ✔ Every transfer method (HTTP, ZeroTier, S3) MUST follow these rules:
 * 1) send(...)    → Sender side: push file out.
 * 2) receive(...) → Receiver side: pull file in.
 * 3) handshake()  → Exchange small info before big transfer.
 * 4) getResumeOffset() → Ask where to resume if interrupted.
 * 5) computeChecksum() → Make SHA-256 for integrity check.
 *
 * Why?
 * ✔ So console flows, web flows, and future GUIs can share the same contract.
 */
public interface TransferMethod {

    /**
     * send
     * ----
     * Baby-English:
     * ✔ Called on the SENDER side.
     * ✔ Uses TargetConfig to know where to send.
     *
     * @param senderName    Who is sending (for logs/audit only).
     * @param file          The actual file to send.
     * @param targetConfig  Encapsulates target details (host, port, mode, AES key).
     *
     * @throws Exception if something serious goes wrong.
     */
    void send(String senderName,
              File file,
              TargetConfig targetConfig) throws Exception;

    /**
     * receive
     * -------
     * Baby-English:
     * ✔ Called on the RECEIVER side.
     * ✔ It listens or fetches the file using the right protocol.
     *
     * @param savePath Folder where the receiver should store the final file.
     *
     * @throws Exception if something serious goes wrong.
     */
    void receive(String savePath) throws Exception;

    /**
     * handshake
     * ---------
     * Baby-English:
     * ✔ Before sending big data, we exchange small info: file name, size, checksum, AES flag.
     *
     * @throws Exception if handshake fails.
     */
    void handshake() throws Exception;

    /**
     * getResumeOffset
     * ---------------
     * Baby-English:
     * ✔ Ask the other side: "How many bytes do you already have?"
     *
     * @return number of bytes already received (0 if none).
     * @throws Exception if negotiation fails.
     */
    long getResumeOffset() throws Exception;

    /**
     * computeChecksum
     * ---------------
     * Baby-English:
     * ✔ Make SHA-256 checksum for the given file.
     *
     * @param file The file to compute checksum for.
     * @return SHA-256 hex string.
     * @throws Exception if file cannot be read.
     */
    String computeChecksum(File file) throws Exception;
}