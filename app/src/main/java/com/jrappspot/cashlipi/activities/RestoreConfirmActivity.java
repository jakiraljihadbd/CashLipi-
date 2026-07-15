package com.jrappspot.cashlipi.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;



import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.utils.BackupManager;
import com.jrappspot.cashlipi.utils.DatabaseManager;

/**
 * RestoreConfirmActivity — Confirmation dialog before restoring backup.
 * Shows what will be replaced and requires explicit confirmation.
 */
public class RestoreConfirmActivity extends BaseActivity {

    private BackupManager backupManager;
    private DatabaseManager db;
    private String backupJson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restore_confirm);

        backupManager = BackupManager.getInstance(this);
        db = DatabaseManager.getInstance(this);

        backupJson = getIntent().getStringExtra("backup_json");
        double previewIncome  = getIntent().getDoubleExtra("preview_income", 0);
        double previewExpense = getIntent().getDoubleExtra("preview_expense", 0);

        // Show preview data
        TextView tvPreviewIncome  = findViewById(R.id.tvConfirmIncome);
        TextView tvPreviewExpense = findViewById(R.id.tvConfirmExpense);
        TextView tvPreviewDebt    = findViewById(R.id.tvConfirmDebt);
        TextView tvPreviewReceiv  = findViewById(R.id.tvConfirmReceivable);

        if (backupJson != null) {
            double[] amounts = backupManager.previewBackupAmounts(backupJson);
            int[] counts = backupManager.previewBackupCounts(backupJson);

            if (tvPreviewIncome  != null) tvPreviewIncome.setText(
                "৳ " + DatabaseManager.formatAmount(amounts[0]));
            if (tvPreviewExpense != null) tvPreviewExpense.setText(
                "৳ " + DatabaseManager.formatAmount(amounts[1]));
            if (tvPreviewDebt != null) tvPreviewDebt.setText(counts[2] + " টি");
            if (tvPreviewReceiv  != null) tvPreviewReceiv.setText(counts[3] + " টি");
        }

        // Cancel
        View btnCancel = findViewById(R.id.btnCancelRestore);
        if (btnCancel != null) btnCancel.setOnClickListener(v -> finish());

        // Restore
        View btnRestore = findViewById(R.id.btnConfirmRestore);
        if (btnRestore != null) {
            btnRestore.setOnClickListener(v -> doRestore());
        }

        // Animate icon
        View icon = findViewById(R.id.ivRestoreIcon);
        if (icon != null) {
            icon.setScaleX(0f); icon.setScaleY(0f);
            icon.animate().scaleX(1f).scaleY(1f).setDuration(400)
                .setInterpolator(new android.view.animation.OvershootInterpolator()).start();
        }
    }

    private void doRestore() {
        if (backupJson == null) {
            Toast.makeText(this, " ব্যাকআপ ডেটা পাওয়া যায়নি", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean success = db.importFromJson(backupJson);
        if (success) {
            Toast.makeText(this,
                " ব্যাকআপ সফলভাবে রিস্টোর হয়েছে!", Toast.LENGTH_LONG).show();
            // Go back to dashboard
            finishAffinity();
            startActivity(new android.content.Intent(this,
                com.jrappspot.cashlipi.activities.DashboardActivity.class)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP));
        } else {
            Toast.makeText(this,
                " রিস্টোর ব্যর্থ হয়েছে। ব্যাকআপ ফাইল সঠিক কিনা পরীক্ষা করুন।",
                Toast.LENGTH_LONG).show();
        }
    }
}
