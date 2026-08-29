package com.wifitest.wifikeyviewer.core.tester;

public class WifiTesterFactory {

    public static IWifiTester createTester(EnvironmentDetector.TestMode mode) {
        if (mode == EnvironmentDetector.TestMode.ROOT_SILENT) {
            return new RootWifiTester();
        } else {
            return new StandardWifiTester();
        }
    }
}
