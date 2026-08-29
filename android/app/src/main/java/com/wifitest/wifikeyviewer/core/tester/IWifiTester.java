package com.wifitest.wifikeyviewer.core.tester;

import android.content.Context;

import com.wifitest.wifikeyviewer.core.model.WifiApInfo;

import java.util.List;

public interface IWifiTester {
    void startTest(Context context, WifiApInfo apInfo, List<String> passwords, TestCallback callback);
    void pause();
    void resume();
    void skipCurrent();
    void cancel();
    boolean isTesting();
    boolean isPaused();
    String getEngineName();
}
