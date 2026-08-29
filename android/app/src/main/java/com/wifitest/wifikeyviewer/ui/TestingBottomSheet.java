package com.wifitest.wifikeyviewer.ui;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.wifitest.wifikeyviewer.R;
import com.wifitest.wifikeyviewer.core.model.WifiApInfo;
import com.wifitest.wifikeyviewer.service.WifiTestService;

public class TestingBottomSheet extends BottomSheetDialogFragment {

    private WifiApInfo targetAp;
    private String engineTag;
    private WifiTestService service;

    private TextView tvTitle, tvEngineTag, tvCurrentKey, tvProgressText, tvSpeedText, tvLogConsole;
    private LinearProgressIndicator progressBar;
    private MaterialButton btnPauseResume, btnSkip, btnStop;

    private final StringBuilder logBuilder = new StringBuilder();

    public static TestingBottomSheet newInstance(WifiApInfo ap, String engineTag) {
        TestingBottomSheet sheet = new TestingBottomSheet();
        Bundle args = new Bundle();
        args.putParcelable("target_ap", ap);
        args.putString("engine_tag", engineTag);
        sheet.setArguments(args);
        return sheet;
    }

    public void setService(WifiTestService service) {
        this.service = service;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        if (getArguments() != null) {
            targetAp = getArguments().getParcelable("target_ap");
            engineTag = getArguments().getString("engine_tag");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_testing, container, false);

        tvTitle = v.findViewById(R.id.tv_test_title);
        tvEngineTag = v.findViewById(R.id.tv_test_engine_tag);
        tvCurrentKey = v.findViewById(R.id.tv_current_key);
        tvProgressText = v.findViewById(R.id.tv_progress_text);
        tvSpeedText = v.findViewById(R.id.tv_speed_text);
        tvLogConsole = v.findViewById(R.id.tv_log_console);
        tvLogConsole.setMovementMethod(new ScrollingMovementMethod());
        progressBar = v.findViewById(R.id.progress_bar);

        btnPauseResume = v.findViewById(R.id.btn_pause_resume);
        btnSkip = v.findViewById(R.id.btn_skip);
        btnStop = v.findViewById(R.id.btn_stop);

        if (targetAp != null) {
            tvTitle.setText("正在测密: " + targetAp.getSsid());
        }
        if (engineTag != null) {
            tvEngineTag.setText(engineTag);
        }

        btnPauseResume.setOnClickListener(view -> {
            if (service != null) {
                if (service.isPaused()) {
                    service.resumeTesting();
                    btnPauseResume.setText("暂停");
                } else {
                    service.pauseTesting();
                    btnPauseResume.setText("继续");
                }
            }
        });

        btnSkip.setOnClickListener(view -> {
            if (service != null) {
                service.skipCurrentKey();
            }
        });

        btnStop.setOnClickListener(view -> {
            if (service != null) {
                service.stopTesting();
            }
            dismiss();
        });

        return v;
    }

    public void updateProgress(String currentKey, int index, int total, double speed) {
        if (tvCurrentKey != null) {
            tvCurrentKey.setText(currentKey);
        }
        if (tvProgressText != null) {
            int percent = (int) ((index * 100.0) / total);
            tvProgressText.setText(String.format("进度: %d / %d (%d%%)", index, total, percent));
            progressBar.setProgress(percent);
        }
        if (tvSpeedText != null) {
            tvSpeedText.setText(String.format("速率: %.1f key/s", speed));
        }
    }

    public void appendLog(String log) {
        if (tvLogConsole != null) {
            logBuilder.append(log).append("\n");
            tvLogConsole.setText(logBuilder.toString());
        }
    }
}
