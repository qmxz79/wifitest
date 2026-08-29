package com.wifitest.wifikeyviewer.core.tester;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.wifitest.wifikeyviewer.core.model.WifiApInfo;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class StandardWifiTester implements IWifiTester {

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
                    callback.onLog("正在测试密码 [" + index + "/" + total + "]: " + key);
                });

                skipCurrent.set(false);
                boolean connected = attemptConnect(context, apInfo, key);

                if (connected) {
                    long duration = System.currentTimeMillis() - startTime;
                    postCallback(() -> {
                        callback.onLog("🎉 匹配成功！密码是: " + key);
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

    private boolean attemptConnect(Context context, WifiApInfo apInfo, String key) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return attemptConnectApi29Plus(context, apInfo, key);
        } else {
            return attemptConnectLegacy(context, apInfo, key);
        }
    }

    private boolean attemptConnectApi29Plus(Context context, WifiApInfo apInfo, String key) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        final AtomicBoolean success = new AtomicBoolean(false);
        final CountDownLatch latch = new CountDownLatch(1);

        try {
            WifiNetworkSpecifier.Builder specifierBuilder = new WifiNetworkSpecifier.Builder()
                    .setSsid(apInfo.getSsid());

            if (apInfo.getSecurityType().contains("WPA3")) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    specifierBuilder.setWpa3Passphrase(key);
                } else {
                    specifierBuilder.setWpa2Passphrase(key);
                }
            } else {
                specifierBuilder.setWpa2Passphrase(key);
            }

            if (apInfo.getBssid() != null && !apInfo.getBssid().isEmpty()) {
                try {
                    specifierBuilder.setBssid(android.net.MacAddress.fromString(apInfo.getBssid()));
                } catch (Exception ignored) {
                }
            }

            NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .setNetworkSpecifier(specifierBuilder.build())
                    .build();

            ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    success.set(true);
                    latch.countDown();
                }

                @Override
                public void onUnavailable() {
                    success.set(false);
                    latch.countDown();
                }
            };

            cm.requestNetwork(request, networkCallback, 10000); // 10秒超时

            // 监听中断或超时
            long waitStart = System.currentTimeMillis();
            while (System.currentTimeMillis() - waitStart < 10000) {
                if (latch.await(300, TimeUnit.MILLISECONDS)) {
                    break;
                }
                if (!isTesting.get() || skipCurrent.get()) {
                    break;
                }
            }

            try {
                cm.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return success.get();
    }

    private boolean attemptConnectLegacy(Context context, WifiApInfo apInfo, String key) {
        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm == null) return false;

        try {
            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = "\"" + apInfo.getSsid() + "\"";
            conf.preSharedKey = "\"" + key + "\"";

            int netId = wm.addNetwork(conf);
            if (netId == -1) return false;

            wm.disconnect();
            wm.enableNetwork(netId, true);
            wm.reconnect();

            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 8000) {
                if (!isTesting.get() || skipCurrent.get()) {
                    wm.removeNetwork(netId);
                    return false;
                }
                if (wm.getConnectionInfo() != null &&
                        apInfo.getSsid().equals(wm.getConnectionInfo().getSSID().replace("\"", ""))) {
                    return true;
                }
                Thread.sleep(500);
            }
            wm.removeNetwork(netId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
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
        return "免 Root 极简模式 (NetworkSpecifier)";
    }
}
