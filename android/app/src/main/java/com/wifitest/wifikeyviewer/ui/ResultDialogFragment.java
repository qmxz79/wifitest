package com.wifitest.wifikeyviewer.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.wifitest.wifikeyviewer.R;
import com.wifitest.wifikeyviewer.utils.QrCodeUtils;

public class ResultDialogFragment extends DialogFragment {

    private String ssid;
    private String password;
    private String authType;

    public static ResultDialogFragment newInstance(String ssid, String password, String authType) {
        ResultDialogFragment fragment = new ResultDialogFragment();
        Bundle args = new Bundle();
        args.putString("ssid", ssid);
        args.putString("password", password);
        args.putString("auth_type", authType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            ssid = getArguments().getString("ssid");
            password = getArguments().getString("password");
            authType = getArguments().getString("auth_type");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_qr_result, container, false);

        TextView tvSsid = v.findViewById(R.id.tv_result_ssid);
        TextView tvPassword = v.findViewById(R.id.tv_result_password);
        ImageView ivQr = v.findViewById(R.id.iv_qrcode);
        MaterialButton btnCopy = v.findViewById(R.id.btn_copy_password);
        MaterialButton btnClose = v.findViewById(R.id.btn_close_result);

        tvSsid.setText("WiFi: " + ssid);
        tvPassword.setText(password);

        // 生成二维码
        Bitmap qrBitmap = QrCodeUtils.generateWifiQrBitmap(ssid, password, authType, 500);
        if (qrBitmap != null) {
            ivQr.setImageBitmap(qrBitmap);
        }

        btnCopy.setOnClickListener(view -> {
            ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("WiFi Password", password));
                Toast.makeText(requireContext(), "密码已复制到剪贴板", Toast.LENGTH_SHORT).show();
            }
        });

        btnClose.setOnClickListener(view -> dismiss());

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
