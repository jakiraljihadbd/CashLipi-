package com.jrappspot.cashlipi.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;



import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.models.BackupRecord;
import com.jrappspot.cashlipi.utils.BackupManager;
import com.jrappspot.cashlipi.utils.DatabaseManager;

import java.util.List;

/**
 * BackupDetailsActivity — Shows full details of a single backup record.
 */
public class BackupDetailsActivity extends BaseActivity {

    private BackupManager backupManager;
    private BackupRecord record;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_details);

        backupManager = BackupManager.getInstance(this);

        String recordId = getIntent().getStringExtra("record_id");
        record = findRecord(recordId);

        if (record == null) {
            Toast.makeText(this, "রেকর্ড পাওয়া যায়নি", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        setupClickListeners();
        animateIn();
    }

    private BackupRecord findRecord(String id) {
        if (id == null) return backupManager.getLastBackupRecord();
        List<BackupRecord> history = backupManager.getBackupHistory();
        for (BackupRecord r : history) {
            if (id.equals(r.getId())) return r;
        }
        return backupManager.getLastBackupRecord();
    }

    private void bindViews() {
        // Header
        setText(R.id.tvDetailDateTime,
            BackupManager.formatDisplayDateTime(record.getDate(), record.getTime()));
        setText(R.id.tvDetailStatus, record.isSuccess() ? " সফল" : " ব্যর্থ");

        // Info rows
        setText(R.id.tvDetailBackupType,   record.getDataTypeDisplay());
        setText(R.id.tvDetailFormat,       record.getFormatDisplay());
        setText(R.id.tvDetailMethod,       record.getMethodIcon() + " " + record.getMethodDisplay());
        setText(R.id.tvDetailFileSize,     record.getFileSizeDisplay());
        setText(R.id.tvDetailTotalItems,   record.getTotalItems() + " টি এন্ট্রি");
        setText(R.id.tvDetailFileName,     record.getFileName() != null ? record.getFileName() : "--");

        // Summary amounts
        setText(R.id.tvDetailIncomeAmt,
            "৳ " + DatabaseManager.formatAmount(record.getIncomeAmount()));
        setText(R.id.tvDetailExpenseAmt,
            "৳ " + DatabaseManager.formatAmount(record.getExpenseAmount()));
        setText(R.id.tvDetailDebtAmt,
            "৳ " + DatabaseManager.formatAmount(record.getDebtAmount()));
        setText(R.id.tvDetailReceivableAmt,
            "৳ " + DatabaseManager.formatAmount(record.getReceivableAmount()));

        // Status color
        TextView tvStatus = findViewById(R.id.tvDetailStatus);
        if (tvStatus != null) {
            tvStatus.setTextColor(record.isSuccess()
                ? 0xFF10B981 : 0xFFEF4444);
        }
    }

    private void setupClickListeners() {
        // Back

        // Share
        View btnShare = findViewById(R.id.btnShareRecord);
        if (btnShare != null) {
            btnShare.setOnClickListener(v -> shareRecord());
        }

        // Restore from this backup
        View btnRestore = findViewById(R.id.btnRestoreFromThis);
        if (btnRestore != null) {
            btnRestore.setOnClickListener(v ->
                startActivity(new Intent(this, RestoreBackupActivity.class)));
        }

        // Delete
        View btnDelete = findViewById(R.id.btnDeleteRecord);
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("রেকর্ড মুছুন")
                    .setMessage("এই ব্যাকআপ রেকর্ডটি মুছে ফেলবেন?")
                    .setPositiveButton("মুছুন", (d, w) -> {
                        // Remove from history
                        List<BackupRecord> history = backupManager.getBackupHistory();
                        history.removeIf(r -> r.getId() != null
                            && r.getId().equals(record.getId()));
                        // Re-save
                        android.content.SharedPreferences prefs =
                            getSharedPreferences("cashlipi_backup_prefs",
                                android.content.Context.MODE_PRIVATE);
                        prefs.edit().putString("backup_history",
                            new com.google.gson.Gson().toJson(history)).apply();
                        Toast.makeText(this, " রেকর্ড মুছে ফেলা হয়েছে",
                            Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton("বাতিল", null)
                    .show();
            });
        }
    }

    private void shareRecord() {
        String text = " CashLipi ক্যাশলিপি ব্যাকআপ বিবরণ\n"
            + "━━━━━━━━━━━━━━━━━━\n"
            + " তারিখ: " + BackupManager.formatDisplayDateTime(
                record.getDate(), record.getTime()) + "\n"
            + " ধরন: " + record.getDataTypeDisplay() + "\n"
            + " ফরম্যাট: " + record.getFormatDisplay() + "\n"
            + " পদ্ধতি: " + record.getMethodDisplay() + "\n"
            + " আকার: " + record.getFileSizeDisplay() + "\n"
            + " অবস্থা: " + (record.isSuccess() ? "সফল" : "ব্যর্থ") + "\n"
            + "━━━━━━━━━━━━━━━━━━\n"
            + " আয়: ৳ " + DatabaseManager.formatAmount(record.getIncomeAmount()) + "\n"
            + " ব্যয়: ৳ " + DatabaseManager.formatAmount(record.getExpenseAmount()) + "\n"
            + " দেনা: ৳ " + DatabaseManager.formatAmount(record.getDebtAmount()) + "\n"
            + " পাওনা: ৳ " + DatabaseManager.formatAmount(record.getReceivableAmount());

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(share, "শেয়ার করুন"));
    }

    private void setText(int id, String text) {
        TextView tv = findViewById(id);
        if (tv != null) tv.setText(text);
    }

    private void animateIn() {
        int[] ids = {R.id.cardDetailHeader, R.id.cardDetailInfo,
            R.id.cardDetailSummary, R.id.layoutDetailActions};
        long delay = 80;
        for (int id : ids) {
            View v = findViewById(id);
            if (v != null) {
                v.setAlpha(0f);
                v.setTranslationY(30f);
                v.animate().alpha(1f).translationY(0f)
                    .setStartDelay(delay).setDuration(300).start();
                delay += 80;
            }
        }
    }
}
