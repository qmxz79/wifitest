package com.wifitest.wifikeyviewer.core.scanner;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;

import com.wifitest.wifikeyviewer.core.model.WifiApInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WifiScanner {

    public interface ScanCallback {
        void onScanStarted();
        void onScanSuccess(List<WifiApInfo> apList);
        void onScanFailed(String reason);
    }

    private final Context context;
    private final WifiManager wifiManager;
    private final Handler mainHandler;
    private ScanCallback currentCallback;
    private boolean isScanning = false;

    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
            handleScanCompleted(success);
        }
    };

    public WifiScanner(Context context) {
        this.context = context.getApplicationContext();
        this.wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public boolean hasPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isWifiEnabled() {
        return wifiManager != null && wifiManager.isWifiEnabled();
    }

    public synchronized void startScan(ScanCallback callback) {
        this.currentCallback = callback;

        if (wifiManager == null) {
            if (callback != null) callback.onScanFailed("当前设备不支持 WiFi 管理");
            return;
        }

        if (!isWifiEnabled()) {
            if (callback != null) callback.onScanFailed("WiFi 未开启，请先开启 WiFi");
            return;
        }

        if (!hasPermissions()) {
            if (callback != null) callback.onScanFailed("缺少定位或附近设备权限");
            return;
        }

        try {
            IntentFilter intentFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            context.registerReceiver(wifiScanReceiver, intentFilter);
            isScanning = true;

            if (callback != null) callback.onScanStarted();

            boolean started = wifiManager.startScan();
            if (!started) {
                // 部分系统由于扫描节流，直接读取已有扫描缓存
                handleScanCompleted(false);
            } else {
                // 超时兜底（5秒后若无广播则直接读取结果）
                mainHandler.postDelayed(() -> {
                    if (isScanning) {
                        handleScanCompleted(false);
                    }
                }, 6000);
            }
        } catch (Exception e) {
            unregisterReceiverQuietly();
            if (callback != null) callback.onScanFailed("扫描异常: " + e.getMessage());
        }
    }

    private synchronized void handleScanCompleted(boolean freshResults) {
        if (!isScanning) return;
        isScanning = false;
        unregisterReceiverQuietly();

        try {
            List<ScanResult> results = wifiManager.getScanResults();
            if (results == null) {
                results = new ArrayList<>();
            }

            // 合并相同 SSID（保留信号最强的一条）
            Map<String, ScanResult> uniqueMap = new HashMap<>();
            for (ScanResult sr : results) {
                if (sr.SSID == null || sr.SSID.isEmpty()) continue;
                if (!uniqueMap.containsKey(sr.SSID) || uniqueMap.get(sr.SSID).level < sr.level) {
                    uniqueMap.put(sr.SSID, sr);
                }
            }

            List<WifiApInfo> apList = new ArrayList<>();
            for (ScanResult sr : uniqueMap.values()) {
                apList.add(WifiApInfo.fromScanResult(sr));
            }

            // 按信号强弱降序排序 (level 数值越大信号越好, 如 -40dBm > -80dBm)
            Collections.sort(apList, (a, b) -> Integer.compare(b.getRssi(), a.getRssi()));

            mainHandler.post(() -> {
                if (currentCallback != null) {
                    currentCallback.onScanSuccess(apList);
                }
            });
        } catch (SecurityException se) {
            mainHandler.post(() -> {
                if (currentCallback != null) {
                    currentCallback.onScanFailed("扫描权限被系统拒绝");
                }
            });
        }
    }

    private void unregisterReceiverQuietly() {
        try {
            context.unregisterReceiver(wifiScanReceiver);
        } catch (Exception ignored) {
        }
    }
}
