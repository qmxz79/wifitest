package com.wifitest.wifikeyviewer.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.wifitest.wifikeyviewer.core.model.SavedResult;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ResultRepository {

    private static final String PREF_NAME = "wifi_test_results_pref";
    private static final String KEY_RESULTS_JSON = "saved_results_json";

    private final SharedPreferences prefs;

    public ResultRepository(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public synchronized void saveResult(SavedResult result) {
        List<SavedResult> list = getAllResults();
        // 如果已存在该 SSID，先移除旧的
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getSsid().equals(result.getSsid())) {
                list.remove(i);
                break;
            }
        }
        list.add(0, result);
        saveList(list);
    }

    public synchronized List<SavedResult> getAllResults() {
        List<SavedResult> list = new ArrayList<>();
        String jsonStr = prefs.getString(KEY_RESULTS_JSON, "[]");
        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(new SavedResult(
                        obj.optString("ssid"),
                        obj.optString("bssid"),
                        obj.optString("password"),
                        obj.optString("securityType"),
                        obj.optLong("timestamp"),
                        obj.optLong("durationMs")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public synchronized String getPasswordForSsid(String ssid) {
        if (ssid == null) return null;
        List<SavedResult> list = getAllResults();
        for (SavedResult r : list) {
            if (ssid.equals(r.getSsid())) {
                return r.getPassword();
            }
        }
        return null;
    }

    public synchronized void clearAll() {
        prefs.edit().remove(KEY_RESULTS_JSON).apply();
    }

    private void saveList(List<SavedResult> list) {
        try {
            JSONArray arr = new JSONArray();
            for (SavedResult r : list) {
                JSONObject obj = new JSONObject();
                obj.put("ssid", r.getSsid());
                obj.put("bssid", r.getBssid());
                obj.put("password", r.getPassword());
                obj.put("securityType", r.getSecurityType());
                obj.put("timestamp", r.getTimestamp());
                obj.put("durationMs", r.getDurationMs());
                arr.put(obj);
            }
            prefs.edit().putString(KEY_RESULTS_JSON, arr.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
