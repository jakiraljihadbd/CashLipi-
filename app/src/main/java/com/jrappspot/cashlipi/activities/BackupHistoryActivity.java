package com.jrappspot.cashlipi.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;


import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.adapters.BackupHistoryAdapter;
import com.jrappspot.cashlipi.models.BackupRecord;
import com.jrappspot.cashlipi.utils.BackupManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * BackupHistoryActivity — Shows backup history with filter tabs.
 */
public class BackupHistoryActivity extends BaseActivity {

    private BackupManager backupManager;
    private BackupHistoryAdapter adapter;

    private CardView chipAll, chipTelegram, chipGDrive, chipLocal;
    private TextView tvEmptyHistory, tvHistoryCount;
    private RecyclerView rvHistory;

    private String currentFilter = "all";
    private List<BackupRecord> allRecords = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_history);

        backupManager = BackupManager.getInstance(this);

        initViews();
        loadHistory();
        setupClickListeners();
    }

    private void initViews() {
        chipAll      = findViewById(R.id.chipFilterAll);
        chipTelegram = findViewById(R.id.chipFilterTelegram);
        chipGDrive   = findViewById(R.id.chipFilterGDrive);
        chipLocal    = findViewById(R.id.chipFilterLocal);

        tvEmptyHistory = findViewById(R.id.tvEmptyHistory);
        tvHistoryCount = findViewById(R.id.tvHistoryCount);

        rvHistory = findViewById(R.id.rvBackupHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        adapter = new BackupHistoryAdapter(this);
        adapter.setOnItemClickListener((record, pos) -> {
            Intent i = new Intent(this, BackupDetailsActivity.class);
            i.putExtra("record_id", record.getId());
            startActivity(i);
        });
        rvHistory.setAdapter(adapter);
    }

    private void loadHistory() {
        allRecords = backupManager.getBackupHistory();
        applyFilter(currentFilter);
    }

    private void applyFilter(String filter) {
        currentFilter = filter;

        List<BackupRecord> filtered;
        if ("all".equals(filter)) {
            filtered = allRecords;
        } else {
            filtered = allRecords.stream()
                .filter(r -> filter.equals(r.getMethod()))
                .collect(Collectors.toList());
        }

        adapter.setData(filtered);
        updateFilterUI(filter);

        if (filtered.isEmpty()) {
            tvEmptyHistory.setVisibility(View.VISIBLE);
            rvHistory.setVisibility(View.GONE);
            tvHistoryCount.setText("কোনো ব্যাকআপ নেই");
        } else {
            tvEmptyHistory.setVisibility(View.GONE);
            rvHistory.setVisibility(View.VISIBLE);
            tvHistoryCount.setText(filtered.size() + "টি ব্যাকআপ পাওয়া গেছে");
        }
    }

    private void updateFilterUI(String active) {
        resetChip(chipAll);
        resetChip(chipTelegram);
        resetChip(chipGDrive);
        resetChip(chipLocal);

        CardView activeChip;
        switch (active) {
            case BackupManager.METHOD_TELEGRAM:     activeChip = chipTelegram; break;
            case BackupManager.METHOD_GOOGLE_DRIVE: activeChip = chipGDrive;   break;
            case BackupManager.METHOD_LOCAL:        activeChip = chipLocal;    break;
            default:                                activeChip = chipAll;      break;
        }
        if (activeChip != null) {
            activeChip.setCardBackgroundColor(0xFF0072FF);
            // Set child TextView color to white
            if (activeChip.getChildAt(0) instanceof TextView) {
                ((TextView) activeChip.getChildAt(0)).setTextColor(0xFFFFFFFF);
            }
        }
    }

    private void resetChip(CardView chip) {
        if (chip == null) return;
        chip.setCardBackgroundColor(0xFF1A2233);
        if (chip.getChildAt(0) instanceof TextView) {
            ((TextView) chip.getChildAt(0)).setTextColor(0xFF9CA3AF);
        }
    }

    private void setupClickListeners() {

        chipAll.setOnClickListener(v -> applyFilter("all"));
        chipTelegram.setOnClickListener(v -> applyFilter(BackupManager.METHOD_TELEGRAM));
        chipGDrive.setOnClickListener(v -> applyFilter(BackupManager.METHOD_GOOGLE_DRIVE));
        chipLocal.setOnClickListener(v -> applyFilter(BackupManager.METHOD_LOCAL));

        // Clear history
        View btnClear = findViewById(R.id.btnClearHistory);
        if (btnClear != null) {
            btnClear.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("ইতিহাস মুছুন")
                    .setMessage("সব ব্যাকআপ ইতিহাস মুছে ফেলবেন?")
                    .setPositiveButton("মুছুন", (d, w) -> {
                        backupManager.clearHistory();
                        allRecords = new ArrayList<>();
                        applyFilter("all");
                    })
                    .setNegativeButton("বাতিল", null)
                    .show();
            });
        }
    }
}
