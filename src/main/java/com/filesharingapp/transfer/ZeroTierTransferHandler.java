package com.filesharingapp.transfer;

import com.filesharingapp.core.PromptManager;
import com.filesharingapp.utils.AppConfig;
import com.filesharingapp.utils.LoggerUtil;
import com.filesharingapp.utils.NetworkUtil;
import com.filesharingapp.utils.ZipUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/** Real CLI-based ZeroTier integration. */
public class ZeroTierTransferHandler implements TransferMethod {
    private final String cliPath = AppConfig.get("zerotier.cli.path", "zerotier-cli");
    private final String networkId = AppConfig.get("zerotier.network.id", "");

    @Override
    public void send(String sender, File file, String method, int port, String host) throws Exception {

        NetworkUtil.logZeroTierStatusIfConfigured();
        LoggerUtil.info("[HTTP] Preparing to send file: " + file.getName());

        // ✅ Add this line immediately after validating 'file'
        File toSend = ZipUtil.zipIfNeeded(file);

        // ✅ Use toSend instead of file everywhere below
        long size = toSend.length();
        LoggerUtil.info("[HTTP] Final file for transfer: " + toSend.getName() + " (" + size + " bytes)");


        // 🟢 Verify receiver reachability
        if (!NetworkUtil.pingReceiver(host, port)) {
            LoggerUtil.warn(PromptManager.CONNECTION_RETRY);
            LoggerUtil.info("[ZeroTier] Starting transfer to node " + host);
            return;
        }

        ensureCli();
        joinNetwork();
        LoggerUtil.success("[ZeroTier] Network ready; send file via HTTP over ZeroTier IP.");

    }

    @Override
    public void receive(String path) throws Exception {
        ensureCli();
        joinNetwork();
        LoggerUtil.success("[ZeroTier] Receiver joined ZeroTier network.");
    }

    private void ensureCli() throws Exception {
        Process p = new ProcessBuilder(cliPath, "-v").start();
        if (p.waitFor() != 0) throw new IllegalStateException("ZeroTier CLI missing.");
        LoggerUtil.info("[ZeroTier] CLI detected.");
    }

    private void joinNetwork() throws Exception {
        if (networkId.isEmpty()) {
            LoggerUtil.warn("[ZeroTier] No network id set in properties.");
            return;
        }
        Process list = new ProcessBuilder(cliPath, "listnetworks").start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(list.getInputStream()))) {
            if (br.lines().anyMatch(l -> l.contains(networkId) && l.contains("OK"))) return;
        }
        LoggerUtil.info("[ZeroTier] Joining network " + networkId);
        new ProcessBuilder(cliPath, "join", networkId).start().waitFor();
    }
}
