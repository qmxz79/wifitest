# WiFi 测密大师 (WiFi Test Master) Android 应用

这是一个功能完善、界面现代的 Android WiFi 信号检测与弱密码自动化测试管理工具。

## ✨ 核心特性

- **双引擎自适应架构**：
  - **🛡️ 免 Root 极简模式**：基于 Android 10+ 官方 `WifiNetworkSpecifier` / `ConnectivityManager`，支持快速验证 Top 20 高频弱密码与单点测试。
  - **⚡ Root 极速静默模式**：基于 `wpa_cli` / `cmd wifi` 底层指令，实现无弹窗、极速后台自动化轮询（2~3 秒/密码）。
- **周边热点扫描与分析**：
  - 支持 Android 8.0 至 Android 14+ 现代权限规范（`NEARBY_WIFI_DEVICES`、精确定位）。
  - 自动识别 2.4GHz / 5.0GHz 频段、信号强度等级 (dBm) 与加密类型 (WPA2/WPA3/WEP/Open)。
- **智能密码字典系统**：
  - **Top 20 极简弱口令**：高频常用弱密码，极速验证。
  - **Top 100 常用弱口令**：常见组合与弱口令。
  - **基于 SSID 智能衍生**：从 WiFi 名称中提取数字、尾号、前后缀智能生成针对性猜解字典。
  - **自定义外部字典**：支持从手机本地导入 `.txt` 字典文件（流式加载）。
- **后台测试与保活**：
  - 采用 `ForegroundService` 前台常驻通知栏，动态显示测试进度与速率。
  - 支持随时「暂停」、「继续」、「跳过当前密码」、「终止测试」。
- **成果管理与二维码分享**：
  - 测密成功后自动震动提醒并持久化保存。
  - 集成 ZXing 引擎，一键生成标准 WiFi 连接二维码，方便其他手机扫码即连。
  - 支持一键复制密码与历史记录管理。

---

## 🛠️ 编译与安装

### 命令行编译
```bash
cd android
./gradlew assembleDebug
```
产出 APK 路径：`android/app/build/outputs/apk/debug/app-debug.apk`

---

## 📱 项目架构

```text
android/app/src/main/
├── java/com/wifitest/wifikeyviewer/
│   ├── MainActivity.java                # 主活动与界面交互控制
│   ├── core/
│   │   ├── dictionary/                  # 字典管理模块 (BuiltinDict, SmartWordlistGenerator, DictManager)
│   │   ├── model/                       # 数据模型 (WifiApInfo, SavedResult)
│   │   ├── scanner/                     # 扫描管理 (WifiScanner)
│   │   └── tester/                      # 双模测试引擎 (IWifiTester, StandardWifiTester, RootWifiTester, EnvironmentDetector)
│   ├── data/
│   │   └── ResultRepository.java        # 结果本地持久化仓库
│   ├── service/
│   │   └── WifiTestService.java         # 前台服务与通知栏控制
│   ├── ui/                              # 适配器与弹窗 (WifiApAdapter, HistoryAdapter, TestingBottomSheet, ResultDialogFragment, HistoryDialogFragment)
│   └── utils/
│       └── QrCodeUtils.java             # ZXing 二维码生成工具
├── res/
│   ├── layout/                          # 现代 Material 3 布局
│   └── values/                          # 样式、配色、字符串
└── AndroidManifest.xml                  # 现代权限与服务声明
```

