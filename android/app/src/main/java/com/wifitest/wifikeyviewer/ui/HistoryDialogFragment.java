package com.wifitest.wifikeyviewer.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.wifitest.wifikeyviewer.R;
import com.wifitest.wifikeyviewer.core.model.SavedResult;
import com.wifitest.wifikeyviewer.data.ResultRepository;

import java.util.List;

public class HistoryDialogFragment extends BottomSheetDialogFragment {

    private ResultRepository repository;
    private HistoryAdapter adapter;
    private List<SavedResult> historyList;
    private TextView tvEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_history, container, false);

        repository = new ResultRepository(requireContext());
        RecyclerView recyclerView = v.findViewById(R.id.recycler_history);
        tvEmpty = v.findViewById(R.id.tv_history_empty);
        MaterialButton btnClear = v.findViewById(R.id.btn_clear_history);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        historyList = repository.getAllResults();

        adapter = new HistoryAdapter(requireContext(), historyList, new HistoryAdapter.OnHistoryActionListener() {
            @Override
            public void onCopyClick(SavedResult item) {
                ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("WiFi Password", item.getPassword()));
                    Toast.makeText(requireContext(), "已复制: " + item.getPassword(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onQrClick(SavedResult item) {
                ResultDialogFragment.newInstance(item.getSsid(), item.getPassword(), item.getSecurityType())
                        .show(getParentFragmentManager(), "qr_dialog");
            }
        });

        recyclerView.setAdapter(adapter);
        updateEmptyView();

        btnClear.setOnClickListener(view -> {
            repository.clearAll();
            historyList.clear();
            adapter.notifyDataSetChanged();
            updateEmptyView();
            Toast.makeText(requireContext(), "历史记录已清空", Toast.LENGTH_SHORT).show();
        });

        return v;
    }

    private void updateEmptyView() {
        if (historyList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
        }
    }
}
