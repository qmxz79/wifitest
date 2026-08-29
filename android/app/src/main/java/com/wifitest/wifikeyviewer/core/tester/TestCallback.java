package com.wifitest.wifikeyviewer.core.tester;

import com.wifitest.wifikeyviewer.core.model.WifiApInfo;

public interface TestCallback {
    void onStart(WifiApInfo ap, int totalKeys);
    void onProgress(String currentKey, int index, int total, double speed);
    void onLog(String message);
    void onSuccess(String crackedKey, long durationMs);
    void onFailure(String reason);
    void onCancelled();
}
