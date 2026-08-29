package com.wifitest.wifikeyviewer.core.model;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Parcel;
import android.os.Parcelable;

public class WifiApInfo implements Parcelable {
    private final String ssid;
    private final String bssid;
    private final int rssi;
    private final int signalLevel;
    private final int frequency;
    private final String capabilities;
    private final String securityType;
    private final boolean is5GHz;
    private boolean isSaved;
    private String savedPassword;
    private boolean isConnected;

    public WifiApInfo(String ssid, String bssid, int rssi, int frequency, String capabilities) {
        this.ssid = (ssid == null || ssid.isEmpty()) ? "<隐藏SSID>" : ssid;
        this.bssid = bssid != null ? bssid : "";
        this.rssi = rssi;
        this.signalLevel = WifiManager.calculateSignalLevel(rssi, 5);
        this.frequency = frequency;
        this.capabilities = capabilities != null ? capabilities : "";
        this.securityType = parseSecurityType(this.capabilities);
        this.is5GHz = frequency >= 4900 && frequency <= 5900;
        this.isSaved = false;
        this.savedPassword = null;
    }

    public static WifiApInfo fromScanResult(ScanResult scanResult) {
        return new WifiApInfo(
                scanResult.SSID,
                scanResult.BSSID,
                scanResult.level,
                scanResult.frequency,
                scanResult.capabilities
        );
    }

    private static String parseSecurityType(String capabilities) {
        if (capabilities.contains("WPA3") || capabilities.contains("SAE")) {
            return "WPA3";
        } else if (capabilities.contains("WPA2") || capabilities.contains("WPA-PSK")) {
            return "WPA2";
        } else if (capabilities.contains("WPA")) {
            return "WPA";
        } else if (capabilities.contains("WEP")) {
            return "WEP";
        } else if (capabilities.contains("EAP") || capabilities.contains("ENTERPRISE")) {
            return "EAP企业级";
        } else {
            return "OPEN开放";
        }
    }

    public boolean isSupportedForTest() {
        return !securityType.equals("EAP企业级") && !securityType.equals("OPEN开放");
    }

    public String getSsid() {
        return ssid;
    }

    public String getBssid() {
        return bssid;
    }

    public int getRssi() {
        return rssi;
    }

    public int getSignalLevel() {
        return signalLevel;
    }

    public int getFrequency() {
        return frequency;
    }

    public String getCapabilities() {
        return capabilities;
    }

    public String getSecurityType() {
        return securityType;
    }

    public boolean is5GHz() {
        return is5GHz;
    }

    public boolean isSaved() {
        return isSaved;
    }

    public void setSaved(boolean saved) {
        isSaved = saved;
    }

    public String getSavedPassword() {
        return savedPassword;
    }

    public void setSavedPassword(String savedPassword) {
        this.savedPassword = savedPassword;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    protected WifiApInfo(Parcel in) {
        ssid = in.readString();
        bssid = in.readString();
        rssi = in.readInt();
        signalLevel = in.readInt();
        frequency = in.readInt();
        capabilities = in.readString();
        securityType = in.readString();
        is5GHz = in.readByte() != 0;
        isSaved = in.readByte() != 0;
        savedPassword = in.readString();
        isConnected = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(ssid);
        dest.writeString(bssid);
        dest.writeInt(rssi);
        dest.writeInt(signalLevel);
        dest.writeInt(frequency);
        dest.writeString(capabilities);
        dest.writeString(securityType);
        dest.writeByte((byte) (is5GHz ? 1 : 0));
        dest.writeByte((byte) (isSaved ? 1 : 0));
        dest.writeString(savedPassword);
        dest.writeByte((byte) (isConnected ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<WifiApInfo> CREATOR = new Creator<WifiApInfo>() {
        @Override
        public WifiApInfo createFromParcel(Parcel in) {
            return new WifiApInfo(in);
        }

        @Override
        public WifiApInfo[] newArray(int size) {
            return new WifiApInfo[size];
        }
    };
}
