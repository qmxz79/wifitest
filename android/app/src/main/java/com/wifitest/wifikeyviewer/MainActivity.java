package com.wifitest.wifikeyviewer;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.wifitest.wifikeyviewer.core.dictionary.DictManager;
import com.wifitest.wifikeyviewer.core.model.SavedResult;
import com.wifitest.wifikeyviewer.core.model.WifiApInfo;
import com.wifitest.wifikeyviewer.core.scanner.WifiScanner;
import com.wifitest.wifikeyviewer.core.tester.EnvironmentDetector;
import com.wifitest.wifikeyviewer.core.tester.TestCallback;
import com.wifitest.wifikeyviewer.data.ResultRepository;
import com.wifitest.wifikeyviewer.service.WifiTestService;
import com.wifitest.wifikeyviewer.ui.HistoryDialogFragment;
import com.wifitest.wifikeyviewer.ui.ResultDialogFragment;
import com.wifitest.wifikeyviewer.ui.TestingBottomSheet;
import com.wifitest.wifikeyviewer.ui.WifiApAdapter;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_PERMISSIONS = 1001;

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private WifiApAdapter adapter;
    private final List<WifiApInfo> apList = new ArrayList<>();

    private TextView tvModeBadge;
    private MaterialButton btnDictSelect;
    private MaterialButton btnScan;
    private MaterialButton btnHistory;
    private LinearLayout layoutEmpty;
    private TextView tvEmptyHint;
    private MaterialButton btnPermissionGrant;

    private WifiScanner wifiScanner;
    private ResultRepository resultRepository;
    private EnvironmentDetector.TestMode currentMode;
    private DictManager.DictType selectedDictType = DictManager.DictType.TOP_20;
    private Uri customDictUri = null;

    private WifiTestService testService;
    private boolean isBound = false;
    private TestingBottomSheet currentTestingSheet;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            WifiTestService.LocalBinder localBinder = (WifiTestService.LocalBinder) binder;
            testService = localBinder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            testService = null;
            isBound = false;
        }
    };

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    layoutEmpty.setVisibility(View.GONE);
                    startWifiScan();
                } else {
                    showPermissionDeniedUI();
                }
            });

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    customDictUri = result.getData().getData();
                    selectedDictType = DictManager.DictType.CUSTOM;
                    btnDictSelect.setText("字典: 自定义文件 ▾");
                    Toast.makeText(this, "已加载自定义字典文件", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initCore();
        checkPermissionsAndStartScan();
        bindTestService();
    }

    private void initViews() {
        swipeRefresh = findViewById(R.id.swipe_refresh);
        recyclerView = findViewById(R.id.recycler_wifi);
        tvModeBadge = findViewById(R.id.tv_mode_badge);
        btnDictSelect = findViewById(R.id.btn_dict_select);
        btnScan = findViewById(R.id.btn_scan);
        btnHistory = findViewById(R.id.btn_history);
        layoutEmpty = findViewById(R.id.layout_empty);
        tvEmptyHint = findViewById(R.id.tv_empty_hint);
        btnPermissionGrant = findViewById(R.id.btn_permission_grant);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WifiApAdapter(this, apList, new WifiApAdapter.OnApActionListener() {
            @Override
            public void onTestClick(WifiApInfo ap) {
                onStartTestAp(ap);
            }

            @Override
            public void onViewPasswordClick(WifiApInfo ap) {
                ResultDialogFragment.newInstance(ap.getSsid(), ap.getSavedPassword(), ap.getSecurityType())
                        .show(getSupportFragmentManager(), "result_dialog");
            }

            @Override
            public void onShareConnectedWifiClick(WifiApInfo ap) {
                openSystemWifiShare(ap);
            }
        });
        recyclerView.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::startWifiScan);
        btnScan.setOnClickListener(v -> startWifiScan());
        btnHistory.setOnClickListener(v -> {
            new HistoryDialogFragment().show(getSupportFragmentManager(), "history_dialog");
        });
        btnDictSelect.setOnClickListener(v -> showDictSelectDialog());
        btnPermissionGrant.setOnClickListener(v -> requestRequiredPermissions());
    }

    private void initCore() {
        wifiScanner = new WifiScanner(this);
        resultRepository = new ResultRepository(this);

        // 检测环境并设置徽章
        currentMode = EnvironmentDetector.getRecommendedMode();
        if (currentMode == EnvironmentDetector.TestMode.ROOT_SILENT) {
            tvModeBadge.setText("⚡ Root 极速静默模式");
            tvModeBadge.setTextColor(ContextCompat.getColor(this, R.color.badge_root));
        } else {
            tvModeBadge.setText("🛡️ 免 Root 极简模式");
            tvModeBadge.setTextColor(ContextCompat.getColor(this, R.color.primary));
        }
    }

    private void bindTestService() {
        Intent intent = new Intent(this, WifiTestService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void checkPermissionsAndStartScan() {
        List<String> neededPermissions = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES)
                    != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.NEARBY_WIFI_DEVICES);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (!neededPermissions.isEmpty()) {
            permissionLauncher.launch(neededPermissions.toArray(new String[0]));
        } else {
            startWifiScan();
        }
    }

    private void requestRequiredPermissions() {
        checkPermissionsAndStartScan();
    }

    private void startWifiScan() {
        swipeRefresh.setRefreshing(true);
        tvEmptyHint.setText("正在扫描附近 WiFi 信号...");
        btnPermissionGrant.setVisibility(View.GONE);

        wifiScanner.startScan(new WifiScanner.ScanCallback() {
            @Override
            public void onScanStarted() {
                swipeRefresh.setRefreshing(true);
            }

            @Override
            public void onScanSuccess(List<WifiApInfo> results) {
                swipeRefresh.setRefreshing(false);
                apList.clear();

                // 关联已保存的历史破解密码
                for (WifiApInfo ap : results) {
                    String savedPwd = resultRepository.getPasswordForSsid(ap.getSsid());
                    if (savedPwd != null) {
                        ap.setSaved(true);
                        ap.setSavedPassword(savedPwd);
                    }
                    apList.add(ap);
                }

                adapter.notifyDataSetChanged();

                if (apList.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    tvEmptyHint.setText("附近未发现 WiFi 信号，请开启位置信息并重试");
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                }
            }

            @Override
            public void onScanFailed(String reason) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(MainActivity.this, reason, Toast.LENGTH_SHORT).show();
                if (apList.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    tvEmptyHint.setText(reason);
                }
            }
        });
    }

    private void showPermissionDeniedUI() {
        swipeRefresh.setRefreshing(false);
        layoutEmpty.setVisibility(View.VISIBLE);
        tvEmptyHint.setText("需要定位与附近设备权限才能扫描 WiFi");
        btnPermissionGrant.setVisibility(View.VISIBLE);
    }

    private void showDictSelectDialog() {
        String[] dicts = {
                "Top 20 极简弱口令 (推荐 · 快速)",
                "Top 100 常见弱口令 (深度覆盖)",
                "基于 SSID 智能衍生密码",
                "导入外部自定义字典 (.txt)"
        };

        new AlertDialog.Builder(this)
                .setTitle("选择测试字典")
                .setItems(dicts, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            selectedDictType = DictManager.DictType.TOP_20;
                            btnDictSelect.setText("字典: Top 20 (推荐) ▾");
                            break;
                        case 1:
                            selectedDictType = DictManager.DictType.TOP_100;
                            btnDictSelect.setText("字典: Top 100 ▾");
                            break;
                        case 2:
                            selectedDictType = DictManager.DictType.SMART_SSID;
                            btnDictSelect.setText("字典: 智能衍生 ▾");
                            break;
                        case 3:
                            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                            intent.setType("text/*");
                            filePickerLauncher.launch(intent);
                            break;
                    }
                })
                .show();
    }

    private void onStartTestAp(WifiApInfo ap) {
        if (testService == null || !isBound) {
            Toast.makeText(this, "测试服务尚未准备就绪", Toast.LENGTH_SHORT).show();
            return;
        }

        if (testService.isTesting()) {
            Toast.makeText(this, "已有正在执行的测试任务", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> passwords = DictManager.getPasswords(this, selectedDictType, ap.getSsid(), customDictUri);
        if (passwords == null || passwords.isEmpty()) {
            Toast.makeText(this, "字典为空，无法测试", Toast.LENGTH_SHORT).show();
            return;
        }

        String modeTag = (currentMode == EnvironmentDetector.TestMode.ROOT_SILENT) ? "⚡ Root 极速模式" : "🛡️ 免 Root 模式";
        currentTestingSheet = TestingBottomSheet.newInstance(ap, modeTag);
        currentTestingSheet.setService(testService);
        currentTestingSheet.show(getSupportFragmentManager(), "testing_sheet");

        testService.startTest(ap, passwords, currentMode, new TestCallback() {
            @Override
            public void onStart(WifiApInfo ap, int totalKeys) {
                if (currentTestingSheet != null) {
                    currentTestingSheet.appendLog("[系统] 开始测试 " + ap.getSsid() + "，共 " + totalKeys + " 个密码");
                }
            }

            @Override
            public void onProgress(String currentKey, int index, int total, double speed) {
                if (currentTestingSheet != null) {
                    currentTestingSheet.updateProgress(currentKey, index, total, speed);
                }
            }

            @Override
            public void onLog(String message) {
                if (currentTestingSheet != null) {
                    currentTestingSheet.appendLog(message);
                }
            }

            @Override
            public void onSuccess(String crackedKey, long durationMs) {
                triggerSuccessVibration();
                if (currentTestingSheet != null && currentTestingSheet.isAdded()) {
                    currentTestingSheet.dismiss();
                }

                // 刷新本地列表状态
                ap.setSaved(true);
                ap.setSavedPassword(crackedKey);
                adapter.notifyDataSetChanged();

                // 弹出成功二维码弹窗
                ResultDialogFragment.newInstance(ap.getSsid(), crackedKey, ap.getSecurityType())
                        .show(getSupportFragmentManager(), "result_dialog");
            }

            @Override
            public void onFailure(String reason) {
                if (currentTestingSheet != null && currentTestingSheet.isAdded()) {
                    currentTestingSheet.dismiss();
                }
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("测试结束")
                        .setMessage(reason)
                        .setPositiveButton("确定", null)
                        .show();
            }

            @Override
            public void onCancelled() {
                Toast.makeText(MainActivity.this, "测试已取消", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openSystemWifiShare(WifiApInfo ap) {
        String savedPwd = resultRepository.getPasswordForSsid(ap.getSsid());

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("调用系统分享「" + ap.getSsid() + "」密码")
                .setMessage("即将打开系统原生 WiFi 设置：\n\n" +
                        "1. 点击已连接网络「" + ap.getSsid() + "」旁的【分享/二维码】图标\n" +
                        "2. 验证指纹或锁屏密码\n" +
                        "3. 即可直接查看官方二维码与明文密码。")
                .setPositiveButton("前往系统设置", (dialog, which) -> {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            try {
                                Intent panelIntent = new Intent(android.provider.Settings.Panel.ACTION_WIFI);
                                startActivity(panelIntent);
                                return;
                            } catch (Exception ignored) {
                            }
                        }
                        Intent intent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "无法打开系统 WiFi 设置: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null);

        if (savedPwd != null && !savedPwd.isEmpty()) {
            builder.setNeutralButton("查看已存二维码", (dialog, which) -> {
                ResultDialogFragment.newInstance(ap.getSsid(), savedPwd, ap.getSecurityType())
                        .show(getSupportFragmentManager(), "result_dialog");
            });
        }

        builder.show();
    }

    private void triggerSuccessVibration() {
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) {
                v.vibrate(500);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }
}
