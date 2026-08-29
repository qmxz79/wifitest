package com.wifitest.wifikeyviewer.core.tester;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.wifitest.wifikeyviewer.core.model.WifiApInfo;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class RootWifiTester implements IWifiTester {

    private final AtomicBoolean isTesting = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private final AtomicBoolean skipCurrent = new AtomicBoolean(false);
    private Thread workerThread;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void startTest(Context context, WifiApInfo apInfo, List<String> passwords, TestCallback callback) {
        if (isTesting.get()) return;

        isTesting.set(true);
        isPaused.set(false);
        skipCurrent.set(false);

        workerThread = new Thread(() -> {
            int total = passwords.size();
            postCallback(() -> callback.onStart(apInfo, total));
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < total; i++) {
                if (!isTesting.get()) {
                    postCallback(callback::onCancelled);
                    return;
                }

                while (isPaused.get() && isTesting.get()) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        postCallback(callback::onCancelled);
                        return;
                    }
                }

                String key = passwords.get(i);
                int index = i + 1;
                long elapsed = System.currentTimeMillis() - startTime;
                double speed = elapsed > 0 ? (index * 1000.0 / elapsed) : 0.0;

                postCallback(() -> {
                    callback.onProgress(key, index, total, speed);
                    callback.onLog("⚡ [Root] 测试密码 [" + index + "/" + total + "]: " + key);
                });

                skipCurrent.set(false);
                boolean connected = attemptRootConnect(apInfo.getSsid(), key);

                if (connected) {
                    long duration = System.currentTimeMillis() - startTime;
                    postCallback(() -> {
                        callback.onLog("🎉 [Root] 匹配成功！密码是: " + key);
                        callback.onSuccess(key, duration);
                    });
                    isTesting.set(false);
                    return;
                }
            }

            if (isTesting.get()) {
                postCallback(() -> {
                    callback.onLog("❌ 未找到匹配的弱密码");
                    callback.onFailure("已完成全部 " + total + " 个弱密码测试，无匹配项");
                });
                isTesting.set(false);
            }
        });

        workerThread.start();
    }

    private boolean attemptRootConnect(String ssid, String key) {
        // 先尝试现代 cmd wifi 命令 (Android 11+)
        try {
            String cmd = String.format("cmd wifi connect-network \"%s\" wpa2 \"%s\"\n", ssid, key);
            execSuCommand(cmd);

            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 3500) {
                if (!isTesting.get() || skipCurrent.get()) {
                    execSuCommand(String.format("cmd wifi forget-network \"%s\"\n", ssid));
                    return false;
                }
                Thread.sleep(500);
                if (checkCurrentConnectedSsid(ssid)) {
                    return true;
                }
            }
            execSuCommand(String.format("cmd wifi forget-network \"%s\"\n", ssid));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 备用方案：wpa_cli (经典底层指令)
        try {
            String netId = execSuCommand("wpa_cli add_network\n").trim();
            if (netId.matches("\\d+")) {
                execSuCommand(String.format("wpa_cli set_network %s ssid '\"%s\"'\n", netId, ssid));
                execSuCommand(String.format("wpa_cli set_network %s psk '\"%s\"'\n", netId, key));
                execSuCommand(String.format("wpa_cli select_network %s\n", netId));

                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() - start < 3000) {
                    if (!isTesting.get() || skipCurrent.get()) {
                        execSuCommand(String.format("wpa_cli remove_network %s\n", netId));
                        return false;
                    }
                    Thread.sleep(400);
                    String status = execSuCommand("wpa_cli status\n");
                    if (status.contains("wpa_state=COMPLETED") && status.contains(ssid)) {
                        return true;
                    }
                }
                execSuCommand(String.format("wpa_cli remove_network %s\n", netId));
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    private boolean checkCurrentConnectedSsid(String targetSsid) {
        try {
            String dumpsys = execSuCommand("dumpsys wifi | grep 'mWifiInfo'\n");
            return dumpsys.contains("\"" + targetSsid + "\"") || dumpsys.contains(targetSsid);
        } catch (Exception e) {
            return false;
        }
    }

    private String execSuCommand(String command) {
        StringBuilder output = new StringBuilder();
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            os.writeBytes(command);
            os.writeBytes("exit\n");
            os.flush();

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            process.waitFor();
        } catch (Exception e) {
            output.append(e.getMessage());
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return output.toString();
    }

    private void postCallback(Runnable r) {
        mainHandler.post(r);
    }

    @Override
    public void pause() {
        isPaused.set(true);
    }

    @Override
    public void resume() {
        isPaused.set(false);
    }

    @Override
    public void skipCurrent() {
        skipCurrent.set(true);
    }

    @Override
    public void cancel() {
        isTesting.set(false);
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }

    @Override
    public boolean isTesting() {
        return isTesting.get();
    }

    @Override
    public boolean isPaused() {
        return isPaused.get();
    }

    @Override
    public String getEngineName() {
        return "Root 极速静默模式 (wpa_cli / cmd wifi)";
    }
}
