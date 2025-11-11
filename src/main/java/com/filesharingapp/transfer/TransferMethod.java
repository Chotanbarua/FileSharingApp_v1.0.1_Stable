package com.filesharingapp.transfer;

import java.io.File;

/** Shared contract for all transfer types. */
public interface TransferMethod {
    void send(String senderName, File file, String method, int port, String targetHost) throws Exception;
    void receive(String savePath) throws Exception;
}
