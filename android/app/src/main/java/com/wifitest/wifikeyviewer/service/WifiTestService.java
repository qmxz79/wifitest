package com.wifitest.wifikeyviewer.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.wifitest.wifikeyviewer.MainActivity;
import com.wifitest.wifikeyviewer.core.model.SavedResult;
import com.wifitest.wifikeyviewer.core.model.WifiApInfo;
import com.wifitest.wifikeyviewer.core.tester.EnvironmentDetector;
import com.wifitest.wifikeyviewer.core.tester.IWifiTester;
import com.wifitest.wifikeyviewer.core.tester.TestCallback;
import com.wifitest.wifikeyviewer.core.tester.WifiTesterFactory;
import com.wifitest.wifikeyviewer.data.ResultRepository;

import java.util.List;

public class WifiTestService extends Service {

    public static final String CHANNEL_ID = "wifi_test_channel";
    public static final int NOTIFICATION_ID = 1001;
    public static final String ACTION_STOP = "com.wifitest.wifikeyviewer.ACTION_STOP";

    private final IBinder binder = new LocalBinder();
    private IWifiTester currentTester;
    private NotificationManager notificationManager;
    private ResultRepository resultRepository;

    public class LocalBinder extends Binder {
        public WifiTestService getService() {
            return WifiTestService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        resultRepository = new ResultRepository(this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopTesting();
        }
        return START_NOT_STICKY;
    }

    public void startTest(WifiApInfo apInfo, List<String> passwords, EnvironmentDetector.TestMode mode, TestCallback clientCallback) {
        currentTester = WifiTesterFactory.createTester(mode);

        Notification notification = buildNotification("准备开始测试: " + apInfo.getSsid(), 0, passwords.size());
        startForeground(NOTIFICATION_ID, notification);

        currentTester.startTest(this, apInfo, passwords, new TestCallback() {
            @Override
            public void onStart(WifiApInfo ap, int totalKeys) {
                if (clientCallback != null) clientCallback.onStart(ap, totalKeys);
            }

            @Override
            public void onProgress(String currentKey, int index, int total, double speed) {
                updateNotification("正在测试 (" + index + "/" + total + ") 密码: " + currentKey, index, total);
                if (clientCallback != null) clientCallback.onProgress(currentKey, index, total, speed);
            }

            @Override
            public void onLog(String message) {
                if (clientCallback != null) clientCallback.onLog(message);
            }

            @Override
            public void onSuccess(String crackedKey, long durationMs) {
                SavedResult res = new SavedResult(
                        apInfo.getSsid(),
                        apInfo.getBssid(),
                        crackedKey,
                        apInfo.getSecurityType(),
                        System.currentTimeMillis(),
                        durationMs
                );
                resultRepository.saveResult(res);

                updateNotification("🎉 成功破解: " + apInfo.getSsid() + " 密码: " + crackedKey, 100, 100);
                if (clientCallback != null) clientCallback.onSuccess(crackedKey, durationMs);
                stopForeground(false);
            }

            @Override
            public void onFailure(String reason) {
                updateNotification("测试结束: " + reason, 100, 100);
                if (clientCallback != null) clientCallback.onFailure(reason);
                stopForeground(false);
            }

            @Override
            public void onCancelled() {
                if (clientCallback != null) clientCallback.onCancelled();
                stopForeground(true);
            }
        });
    }

    public void pauseTesting() {
        if (currentTester != null) currentTester.pause();
    }

    public void resumeTesting() {
        if (currentTester != null) currentTester.resume();
    }

    public void skipCurrentKey() {
        if (currentTester != null) currentTester.skipCurrent();
    }

    public void stopTesting() {
        if (currentTester != null) {
            currentTester.cancel();
        }
        stopForeground(true);
        stopSelf();
    }

    public boolean isTesting() {
        return currentTester != null && currentTester.isTesting();
    }

    public boolean isPaused() {
        return currentTester != null && currentTester.isPaused();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "WiFi 测密服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("显示当前 WiFi 弱密码测试进度");
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification(String content, int progress, int max) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        Intent stopIntent = new Intent(this, WifiTestService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 1, stopIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("WiFi 弱密码测试中")
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "停止", stopPendingIntent);

        if (max > 0) {
            builder.setProgress(max, progress, false);
        }

        return builder.build();
    }

    private void updateNotification(String content, int progress, int max) {
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, buildNotification(content, progress, max));
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
