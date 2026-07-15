package com.jrappspot.cashlipi.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import androidx.cardview.widget.CardView;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.models.BackupRecord;
import com.jrappspot.cashlipi.utils.BackupManager;
import com.jrappspot.cashlipi.utils.DatabaseManager;

import java.util.List;

/**
 * RestoreBackupActivity — Restore backup from Local / Telegram / Google Drive.
 */
public class RestoreBackupActivity extends BaseActivity {

    private BackupManager backupManager;
    private DatabaseManager db;

    private String selectedSource = BackupManager.METHOD_LOCAL;
    private String loadedJson = null;

    // Source cards
    private CardView cardSourceLocal, cardSourceTelegram, cardSourceGDrive;

    // Available backups list
    private View layoutAvailableBackups;
    private TextView tvBackup1Date, tvBackup1Size, tvBackup2Date, tvBackup2Size;
    private CardView cardBackup1, cardBackup2;
    private String selectedBackupJson = null;

    // Preview section
    private View layoutPreview;
    private TextView tvPreviewIncome, tvPreviewExpense, tvPreviewDebt, tvPreviewReceivable;

    // File picker for local restore
    private ActivityResultLauncher<String[]> openFileLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restore_backup);

        backupManager = BackupManager.getInstance(this);
        db = DatabaseManager.getInstance(this);

        registerFileLauncher();
        initViews();
        setupClickListeners();
        loadLocalBackups();
        updateSourceUI();
        animateIn();
    }

    private void registerFileLauncher() {
        openFileLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    String json = backupManager.readJsonFromUri(uri);
                    if (json != null && json.startsWith("{")) {
                        selectedBackupJson = json;
                        showPreview(json);
                    } else {
                        Toast.makeText(this, " অবৈধ ব্যাকআপ ফাইল", Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }

    private void initViews() {
        cardSourceLocal    = findViewById(R.id.cardSourceLocal);
        cardSourceTelegram = findViewById(R.id.cardSourceTelegram);
        cardSourceGDrive   = findViewById(R.id.cardSourceGDrive);

        layoutAvailableBackups = findViewById(R.id.layoutAvailableBackups);
        tvBackup1Date = findViewById(R.id.tvBackup1Date);
        tvBackup1Size = findViewById(R.id.tvBackup1Size);
        tvBackup2Date = findViewById(R.id.tvBackup2Date);
        tvBackup2Size = findViewById(R.id.tvBackup2Size);
        cardBackup1   = findViewById(R.id.cardLocalBackup1);
        cardBackup2   = findViewById(R.id.cardLocalBackup2);

        layoutPreview        = findViewById(R.id.layoutPreviewData);
        tvPreviewIncome      = findViewById(R.id.tvPreviewIncome);
        tvPreviewExpense     = findViewById(R.id.tvPreviewExpense);
        tvPreviewDebt        = findViewById(R.id.tvPreviewDebt);
        tvPreviewReceivable  = findViewById(R.id.tvPreviewReceivable);
    }

    private void loadLocalBackups() {
        // Load from backup history for the available backups section
        List<BackupRecord> history = backupManager.getBackupHistory();
        List<BackupRecord> localBackups = new java.util.ArrayList<>();
        for (BackupRecord r : history) {
            if (BackupManager.STATUS_SUCCESS.equals(r.getStatus())) {
                localBackups.add(r);
                if (localBackups.size() >= 2) break;
            }
        }

        if (!localBackups.isEmpty()) {
            BackupRecord r1 = localBackups.get(0);
            tvBackup1Date.setText(BackupManager.formatDisplayDateTime(r1.getDate(), r1.getTime())
                + "\n" + r1.getDataTypeDisplay() + " • " + r1.getFormatDisplay().toUpperCase());
            tvBackup1Size.setText(r1.getFileSizeDisplay());
            cardBackup1.setVisibility(View.VISIBLE);
        }

        if (localBackups.size() >= 2) {
            BackupRecord r2 = localBackups.get(1);
            tvBackup2Date.setText(BackupManager.formatDisplayDateTime(r2.getDate(), r2.getTime())
                + "\n" + r2.getDataTypeDisplay() + " • " + r2.getFormatDisplay().toUpperCase());
            tvBackup2Size.setText(r2.getFileSizeDisplay());
            cardBackup2.setVisibility(View.VISIBLE);
        }

        // Show preview for most recent
        if (!history.isEmpty()) {
            BackupRecord recent = history.get(0);
            tvPreviewIncome.setText("৳ " + DatabaseManager.formatAmount(recent.getIncomeAmount()));
            tvPreviewExpense.setText("৳ " + DatabaseManager.formatAmount(recent.getExpenseAmount()));
            tvPreviewDebt.setText("৳ " + DatabaseManager.formatAmount(recent.getDebtAmount()));
            tvPreviewReceivable.setText("৳ " + DatabaseManager.formatAmount(recent.getReceivableAmount()));
            layoutPreview.setVisibility(View.VISIBLE);
        }
    }

    private void setupClickListeners() {
        // Back

        // Source selection
        cardSourceLocal.setOnClickListener(v -> {
            selectedSource = BackupManager.METHOD_LOCAL;
            updateSourceUI();
            openFileLauncher.launch(new String[]{"application/json", "*/*"});
        });

        cardSourceTelegram.setOnClickListener(v -> {
            selectedSource = BackupManager.METHOD_TELEGRAM;
            updateSourceUI();
            Toast.makeText(this,
                " Telegram থেকে JSON ফাইল ডাউনলোড করে এই অ্যাপে শেয়ার করুন।",
                Toast.LENGTH_LONG).show();
        });

        cardSourceGDrive.setOnClickListener(v -> {
            selectedSource = BackupManager.METHOD_GOOGLE_DRIVE;
            updateSourceUI();
            Toast.makeText(this,
                " Google Drive থেকে ফাইল ডাউনলোড করে ব্রাউজ করুন।",
                Toast.LENGTH_LONG).show();
            openFileLauncher.launch(new String[]{"application/json", "*/*"});
        });

        // Browse file (local)
        View btnBrowse = findViewById(R.id.btnBrowseFile);
        if (btnBrowse != null) {
            btnBrowse.setOnClickListener(v ->
                openFileLauncher.launch(new String[]{"application/json", "*/*"}));
        }

        // Backup item click
        if (cardBackup1 != null) cardBackup1.setOnClickListener(v -> {
            // Select backup 1 (most recent local)
            List<BackupRecord> hist = backupManager.getBackupHistory();
            if (!hist.isEmpty()) highlightBackupCard(true);
        });
        if (cardBackup2 != null) cardBackup2.setOnClickListener(v -> {
            List<BackupRecord> hist = backupManager.getBackupHistory();
            if (hist.size() >= 2) highlightBackupCard(false);
        });

        // Restore button
        View btnRestore = findViewById(R.id.btnRestoreNow);
        if (btnRestore != null) {
            btnRestore.setOnClickListener(v -> showRestoreConfirmation());
        }
    }

    private void highlightBackupCard(boolean first) {
        if (cardBackup1 != null)
            cardBackup1.setCardBackgroundColor(first ? 0xFF1E3A5F : 0xFF1A2233);
        if (cardBackup2 != null)
            cardBackup2.setCardBackgroundColor(first ? 0xFF1A2233 : 0xFF1E3A5F);
    }

    private void showPreview(String json) {
        double[] amounts = backupManager.previewBackupAmounts(json);
        int[] counts = backupManager.previewBackupCounts(json);

        tvPreviewIncome.setText("৳ " + DatabaseManager.formatAmount(amounts[0]));
        tvPreviewExpense.setText("৳ " + DatabaseManager.formatAmount(amounts[1]));
        tvPreviewDebt.setText(counts[2] + " টি");
        tvPreviewReceivable.setText(counts[3] + " টি");

        if (layoutPreview != null) layoutPreview.setVisibility(View.VISIBLE);
        Toast.makeText(this, " ফাইল লোড হয়েছে। রিস্টোর করতে নিচের বোতাম চাপুন।",
            Toast.LENGTH_LONG).show();
    }

    private void showRestoreConfirmation() {
        if (selectedBackupJson == null) {
            Toast.makeText(this, " আগে একটি ব্যাকআপ ফাইল নির্বাচন করুন", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build preview values for dialog
        double[] amounts = backupManager.previewBackupAmounts(selectedBackupJson);
        String previewText = String.format(
            "আয়: ৳ %s\nব্যয়: ৳ %s",
            DatabaseManager.formatAmount(amounts[0]),
            DatabaseManager.formatAmount(amounts[1]));

        // Navigate to confirmation screen
        Intent i = new Intent(this, RestoreConfirmActivity.class);
        i.putExtra("backup_json", selectedBackupJson);
        i.putExtra("preview_income", amounts[0]);
        i.putExtra("preview_expense", amounts[1]);
        startActivity(i);
    }

    private void updateSourceUI() {
        resetSourceCard(cardSourceLocal);
        resetSourceCard(cardSourceTelegram);
        resetSourceCard(cardSourceGDrive);

        CardView selected;
        switch (selectedSource) {
            case BackupManager.METHOD_TELEGRAM:     selected = cardSourceTelegram; break;
            case BackupManager.METHOD_GOOGLE_DRIVE: selected = cardSourceGDrive;   break;
            default:                                selected = cardSourceLocal;     break;
        }
        if (selected != null) {
            selected.setCardBackgroundColor(0xFF1E3A5F);
            selected.setCardElevation(8f);
        }
    }

    private void resetSourceCard(CardView card) {
        if (card == null) return;
        card.setCardBackgroundColor(0xFF1A2233);
        card.setCardElevation(2f);
    }

    private void animateIn() {
        int[] ids = {R.id.cardSelectSource, R.id.cardAvailableBackups,
            R.id.cardPreviewData, R.id.btnRestoreNow};
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
