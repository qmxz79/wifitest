package com.wifitest.wifikeyviewer.core.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SavedResult {
    private final String ssid;
    private final String bssid;
    private final String password;
    private final String securityType;
    private final long timestamp;
    private final long durationMs;

    public SavedResult(String ssid, String bssid, String password, String securityType, long timestamp, long durationMs) {
        this.ssid = ssid;
        this.bssid = bssid;
        this.password = password;
        this.securityType = securityType;
        this.timestamp = timestamp;
        this.durationMs = durationMs;
    }

    public String getSsid() {
        return ssid;
    }

    public String getBssid() {
        return bssid;
    }

    public String getPassword() {
        return password;
    }

    public String getSecurityType() {
        return securityType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
