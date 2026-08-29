package com.wifitest.wifikeyviewer.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.wifitest.wifikeyviewer.R;
import com.wifitest.wifikeyviewer.core.model.SavedResult;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    public interface OnHistoryActionListener {
        void onCopyClick(SavedResult item);
        void onQrClick(SavedResult item);
    }

    private final Context context;
    private final List<SavedResult> list;
    private final OnHistoryActionListener listener;

    public HistoryAdapter(Context context, List<SavedResult> list, OnHistoryActionListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SavedResult item = list.get(position);
        holder.tvSsid.setText(item.getSsid());
        holder.tvPassword.setText("密码: " + item.getPassword());
        holder.tvTime.setText(item.getFormattedDate());

        holder.btnCopy.setOnClickListener(v -> {
            if (listener != null) listener.onCopyClick(item);
        });

        holder.btnQr.setOnClickListener(v -> {
            if (listener != null) listener.onQrClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSsid, tvPassword, tvTime;
        MaterialButton btnCopy, btnQr;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSsid = itemView.findViewById(R.id.tv_hist_ssid);
            tvPassword = itemView.findViewById(R.id.tv_hist_password);
            tvTime = itemView.findViewById(R.id.tv_hist_time);
            btnCopy = itemView.findViewById(R.id.btn_hist_copy);
            btnQr = itemView.findViewById(R.id.btn_hist_qr);
        }
    }
}
