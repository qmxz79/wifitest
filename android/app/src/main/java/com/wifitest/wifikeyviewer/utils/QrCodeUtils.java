package com.wifitest.wifikeyviewer.utils;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class QrCodeUtils {

    public static Bitmap generateWifiQrBitmap(String ssid, String password, String authType, int size) {
        String type = "WPA";
        if (authType != null && authType.contains("WEP")) {
            type = "WEP";
        } else if (authType != null && authType.contains("OPEN")) {
            type = "nopass";
        }

        // 标准 WiFi 二维码协议格式: WIFI:T:WPA;S:MySSID;P:MyPassword;;
        String qrContent = String.format("WIFI:T:%s;S:%s;P:%s;;", type, ssid, password != null ? password : "");

        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, size, size);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bmp;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
