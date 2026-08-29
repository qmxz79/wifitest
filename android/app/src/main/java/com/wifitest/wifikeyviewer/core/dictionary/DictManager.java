package com.wifitest.wifikeyviewer.core.dictionary;

import android.content.Context;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DictManager {

    public enum DictType {
        TOP_20,
        TOP_100,
        SMART_SSID,
        CUSTOM
    }

    public static List<String> getPasswords(Context context, DictType type, String ssid, Uri customFileUri) {
        switch (type) {
            case TOP_20:
                return BuiltinDict.TOP_20;
            case TOP_100:
                return BuiltinDict.TOP_100;
            case SMART_SSID:
                return SmartWordlistGenerator.generateForSsid(ssid);
            case CUSTOM:
                if (customFileUri != null && context != null) {
                    return loadCustomDict(context, customFileUri);
                }
                return BuiltinDict.TOP_20;
            default:
                return BuiltinDict.TOP_20;
        }
    }

    public static List<String> loadCustomDict(Context context, Uri uri) {
        Set<String> words = new LinkedHashSet<>();
        try (InputStream is = context.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // WiFi WPA 密码最短要求 8 位
                if (line.length() >= 8) {
                    words.add(line);
                }
                // 最多保护加载 5000 条，避免内存过载
                if (words.size() >= 5000) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>(words);
    }
}
