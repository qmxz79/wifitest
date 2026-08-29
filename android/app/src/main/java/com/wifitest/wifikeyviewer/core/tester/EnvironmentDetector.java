package com.wifitest.wifikeyviewer.core.tester;

import android.os.Build;

import java.io.File;

public class EnvironmentDetector {

    private static Boolean sHasRoot = null;

    public enum TestMode {
        ROOT_SILENT,
        STANDARD_SPECIFIER
    }

    public static boolean checkRoot() {
        if (sHasRoot != null) {
            return sHasRoot;
        }

        String[] paths = {
                "/system/app/Superuser.apk",
                "/sbin/su",
                "/system/bin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su",
                "/su/bin/su"
        };

        for (String path : paths) {
            if (new File(path).exists()) {
                sHasRoot = true;
                return true;
            }
        }

        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"/system/xbin/which", "su"});
            int exitCode = process.waitFor();
            sHasRoot = (exitCode == 0);
            return sHasRoot;
        } catch (Exception e) {
            try {
                process = Runtime.getRuntime().exec(new String[]{"which", "su"});
                int exitCode = process.waitFor();
                sHasRoot = (exitCode == 0);
                return sHasRoot;
            } catch (Exception ignored) {
                sHasRoot = false;
                return false;
            }
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    public static TestMode getRecommendedMode() {
        if (checkRoot()) {
            return TestMode.ROOT_SILENT;
        }
        return TestMode.STANDARD_SPECIFIER;
    }
}
