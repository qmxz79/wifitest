package com.wifitest.wifikeyviewer.core.dictionary;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmartWordlistGenerator {

    /**
     * 根据 SSID 名称特征生成针对性的候选密码
     */
    public static List<String> generateForSsid(String ssid) {
        Set<String> candidates = new LinkedHashSet<>();
        if (ssid == null || ssid.trim().isEmpty()) {
            return new ArrayList<>(BuiltinDict.TOP_20);
        }

        String cleanSsid = ssid.trim();

        // 1. 提取 SSID 中的数字序列
        Pattern numPattern = Pattern.compile("\\d+");
        Matcher matcher = numPattern.matcher(cleanSsid);
        List<String> numbers = new ArrayList<>();
        while (matcher.find()) {
            numbers.add(matcher.group());
        }

        for (String num : numbers) {
            // 如果数字本身长度 >= 8，直接作为高概率候选
            if (num.length() >= 8) {
                candidates.add(num);
                // 常见手机号截取（后8位）
                if (num.length() == 11) {
                    candidates.add(num.substring(3));
                }
            } else if (num.length() >= 4) {
                // 重复补齐 8 位，如 1234 -> 12341234
                candidates.add(num + num);
                // 前后补 8 或 1
                candidates.add("8888" + num);
                candidates.add(num + "8888");
                candidates.add("1234" + num);
                candidates.add(num + "1234");
            }
        }

        // 2. SSID 小写纯文本变体（如果长度 >= 8）
        String lowerSsid = cleanSsid.toLowerCase();
        if (lowerSsid.length() >= 8) {
            candidates.add(lowerSsid);
        }
        candidates.add(lowerSsid + "123");
        candidates.add(lowerSsid + "888");
        candidates.add("wifi" + lowerSsid);

        // 3. 补充 Top 20 兜底
        candidates.addAll(BuiltinDict.TOP_20);

        return new ArrayList<>(candidates);
    }
}
