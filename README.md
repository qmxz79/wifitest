# WiFi 测密工具 (WiFi Test Suite)

本项目包含 **Android 原生端应用** 与 **Python 桌面端工具**，用于 WiFi 热点扫描、信号分析与弱密码自动化测试。

---

## 📱 Android 端应用：WiFi 测密大师 (WiFi Test Master)

> **最新版本**: `v2.0.1`  
> **📥 安装包直接下载**: [WiFiTestMaster_v2.0.1.apk (GitHub加速/直链)](https://raw.githubusercontent.com/qmxz79/wifitest/master/release/WiFiTestMaster_v2.0.1.apk) | [仓库内文件地址](https://github.com/qmxz79/wifitest/blob/master/release/WiFiTestMaster_v2.0.1.apk) (约 5.7 MB)

### 核心功能
- **双引擎自适应**：
  - **🛡️ 免 Root 极简模式**：通过 Android 10+ 官方 API 验证常用 Top 20 极简弱口令。
  - **⚡ Root 极速静默模式**：基于 `wpa_cli` / `cmd wifi` 底层轮询（2~3 秒/key）。
- **信号扫描与分析**：适配 Android 8.0 ~ 14+ 权限，展示 2.4G/5G 频段、信号 dBm/等级与加密类型。
- **智能密码字典**：内置 Top 20 极简字典、Top 100 常用字典、基于 SSID 智能衍生密码，支持导入 `.txt` 外部字典。
- **后台保活与常驻通知**：`ForegroundService` 动态进度，支持随时暂停/继续/跳过/终止。
- **一键分享**：集成 ZXing 引擎生成 WiFi 连接二维码，方便扫码即连。

---

## 💻 Python 桌面端工具

- 扫描并显示附近的 WiFi 热点（按信号强度排序）
- 选择 WiFi 热点和密码字典进行多线程测试
- 成功连接后自动保存记录

### 使用方法
```bash
pip install pywifi
python wifitest.py
```