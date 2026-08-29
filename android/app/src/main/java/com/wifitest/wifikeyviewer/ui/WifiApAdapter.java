package com.wifitest.wifikeyviewer.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.wifitest.wifikeyviewer.R;
import com.wifitest.wifikeyviewer.core.model.WifiApInfo;

import java.util.List;

public class WifiApAdapter extends RecyclerView.Adapter<WifiApAdapter.ViewHolder> {

    public interface OnApActionListener {
        void onTestClick(WifiApInfo ap);
        void onViewPasswordClick(WifiApInfo ap);
    }

    private final Context context;
    private final List<WifiApInfo> apList;
    private final OnApActionListener listener;

    public WifiApAdapter(Context context, List<WifiApInfo> apList, OnApActionListener listener) {
        this.context = context;
        this.apList = apList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_wifi_ap, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WifiApInfo ap = apList.get(position);

        holder.tvSsid.setText(ap.getSsid());
        holder.tvBssidSignal.setText(String.format("BSSID: %s  |  %d dBm", ap.getBssid(), ap.getRssi()));

        // 频段
        holder.tagFreq.setText(ap.is5GHz() ? "5.0 GHz" : "2.4 GHz");
        holder.tagFreq.setTextColor(ap.is5GHz() ? ContextCompat.getColor(context, R.color.primary) : ContextCompat.getColor(context, R.color.text_secondary));

        // 加密
        holder.tagSecurity.setText(ap.getSecurityType());

        // 信号指示
        int level = ap.getSignalLevel();
        if (level >= 4) {
            holder.tvSignalIcon.setText("📶 🟢");
        } else if (level >= 2) {
            holder.tvSignalIcon.setText("📶 🟡");
        } else {
            holder.tvSignalIcon.setText("📶 🔴");
        }

        // 已保存密码状态
        if (ap.isSaved() && ap.getSavedPassword() != null) {
            holder.tagSavedPwd.setVisibility(View.VISIBLE);
            holder.tagSavedPwd.setText("已获密码: " + ap.getSavedPassword());
            holder.btnAction.setText("查看");
            holder.btnAction.setBackgroundColor(ContextCompat.getColor(context, R.color.accent));
            holder.btnAction.setOnClickListener(v -> {
                if (listener != null) listener.onViewPasswordClick(ap);
            });
        } else {
            holder.tagSavedPwd.setVisibility(View.GONE);
            if (!ap.isSupportedForTest()) {
                holder.btnAction.setText("不支持");
                holder.btnAction.setEnabled(false);
                holder.btnAction.setBackgroundColor(Color.LTGRAY);
            } else {
                holder.btnAction.setText("测密");
                holder.btnAction.setEnabled(true);
                holder.btnAction.setBackgroundColor(ContextCompat.getColor(context, R.color.primary));
                holder.btnAction.setOnClickListener(v -> {
                    if (listener != null) listener.onTestClick(ap);
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return apList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSignalIcon, tvSsid, tvBssidSignal;
        TextView tagFreq, tagSecurity, tagSavedPwd;
        MaterialButton btnAction;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSignalIcon = itemView.findViewById(R.id.tv_signal_icon);
            tvSsid = itemView.findViewById(R.id.tv_ssid);
            tvBssidSignal = itemView.findViewById(R.id.tv_bssid_signal);
            tagFreq = itemView.findViewById(R.id.tag_freq);
            tagSecurity = itemView.findViewById(R.id.tag_security);
            tagSavedPwd = itemView.findViewById(R.id.tag_saved_pwd);
            btnAction = itemView.findViewById(R.id.btn_action);
        }
    }
}
