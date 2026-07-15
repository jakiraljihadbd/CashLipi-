package com.jrappspot.cashlipi.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;



import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.models.BackupSettings;
import com.jrappspot.cashlipi.utils.BackupManager;

/**
 * BackupSettingsActivity — Configure all auto-backup triggers and methods.
 */
public class BackupSettingsActivity extends BaseActivity {

    private BackupManager backupManager;

    // Trigger switches
    private Switch swIncomeAdded, swExpenseAdded, swDebtAdded,
                   swReceivableAdded, swDataUpdated, swDataDeleted;

    // Method switches
    private Switch swMethodTelegram, swMethodGDrive, swMethodLocal;

    // Frequency
    private RadioGroup rgFrequency;
    private RadioButton rbRealtime, rbDaily, rbWeekly, rbMonthly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_settings);

        backupManager = BackupManager.getInstance(this);

        initViews();
        loadSettings();
        setupClickListeners();
        animateIn();
    }

    private void initViews() {
        swIncomeAdded    = findViewById(R.id.swTriggerIncome);
        swExpenseAdded   = findViewById(R.id.swTriggerExpense);
        swDebtAdded      = findViewById(R.id.swTriggerDebt);
        swReceivableAdded= findViewById(R.id.swTriggerReceivable);
        swDataUpdated    = findViewById(R.id.swTriggerUpdated);
        swDataDeleted    = findViewById(R.id.swTriggerDeleted);

        swMethodTelegram = findViewById(R.id.swMethodTelegram);
        swMethodGDrive   = findViewById(R.id.swMethodGDrive);
        swMethodLocal    = findViewById(R.id.swMethodLocal);

        rgFrequency = findViewById(R.id.rgFrequency);
        rbRealtime  = findViewById(R.id.rbRealtime);
        rbDaily     = findViewById(R.id.rbDaily);
        rbWeekly    = findViewById(R.id.rbWeekly);
        rbMonthly   = findViewById(R.id.rbMonthly);
    }

    private void loadSettings() {
        BackupSettings s = backupManager.getSettings();

        // Triggers
        safeSet(swIncomeAdded,     s.isTriggerOnIncomeAdded());
        safeSet(swExpenseAdded,    s.isTriggerOnExpenseAdded());
        safeSet(swDebtAdded,       s.isTriggerOnDebtAdded());
        safeSet(swReceivableAdded, s.isTriggerOnReceivableAdded());
        safeSet(swDataUpdated,     s.isTriggerOnDataUpdated());
        safeSet(swDataDeleted,     s.isTriggerOnDataDeleted());

        // Methods
        safeSet(swMethodTelegram, s.isTelegramEnabled());
        safeSet(swMethodGDrive,   s.isGoogleDriveEnabled());
        safeSet(swMethodLocal,    s.isLocalStorageEnabled());

        // Frequency
        String freq = s.getBackupFrequency();
        if (freq == null) freq = "realtime";
        switch (freq) {
            case "daily":   if (rbDaily   != null) rbDaily.setChecked(true);   break;
            case "weekly":  if (rbWeekly  != null) rbWeekly.setChecked(true);  break;
            case "monthly": if (rbMonthly != null) rbMonthly.setChecked(true); break;
            default:        if (rbRealtime!= null) rbRealtime.setChecked(true); break;
        }
    }

    private void safeSet(Switch sw, boolean val) {
        if (sw != null) sw.setChecked(val);
    }

    private void setupClickListeners() {
        // Back

        // Save
        View btnSave = findViewById(R.id.btnSaveBackupSettings);
        if (btnSave != null) btnSave.setOnClickListener(v -> saveSettings());

        // Reset
        View btnReset = findViewById(R.id.btnResetSettings);
        if (btnReset != null) {
            btnReset.setOnClickListener(v -> {
                backupManager.saveSettings(new BackupSettings());
                loadSettings();
                Toast.makeText(this, " ডিফল্ট সেটিংসে ফিরে এসেছে", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void saveSettings() {
        BackupSettings s = backupManager.getSettings();

        // Triggers
        if (swIncomeAdded     != null) s.setTriggerOnIncomeAdded(swIncomeAdded.isChecked());
        if (swExpenseAdded    != null) s.setTriggerOnExpenseAdded(swExpenseAdded.isChecked());
        if (swDebtAdded       != null) s.setTriggerOnDebtAdded(swDebtAdded.isChecked());
        if (swReceivableAdded != null) s.setTriggerOnReceivableAdded(swReceivableAdded.isChecked());
        if (swDataUpdated     != null) s.setTriggerOnDataUpdated(swDataUpdated.isChecked());
        if (swDataDeleted     != null) s.setTriggerOnDataDeleted(swDataDeleted.isChecked());

        // Methods
        if (swMethodTelegram != null) s.setTelegramEnabled(swMethodTelegram.isChecked());
        if (swMethodGDrive   != null) s.setGoogleDriveEnabled(swMethodGDrive.isChecked());
        if (swMethodLocal    != null) s.setLocalStorageEnabled(swMethodLocal.isChecked());

        // Frequency
        if (rgFrequency != null) {
            int selId = rgFrequency.getCheckedRadioButtonId();
            if (selId == R.id.rbDaily)       s.setBackupFrequency("daily");
            else if (selId == R.id.rbWeekly) s.setBackupFrequency("weekly");
            else if (selId == R.id.rbMonthly)s.setBackupFrequency("monthly");
            else                              s.setBackupFrequency("realtime");
        }

        // Auto backup enabled if any method + any trigger is on
        boolean anyTrigger = s.isTriggerOnIncomeAdded() || s.isTriggerOnExpenseAdded()
            || s.isTriggerOnDebtAdded() || s.isTriggerOnReceivableAdded()
            || s.isTriggerOnDataUpdated() || s.isTriggerOnDataDeleted();
        boolean anyMethod = s.isTelegramEnabled() || s.isGoogleDriveEnabled()
            || s.isLocalStorageEnabled();
        s.setAutoBackupEnabled(anyTrigger && anyMethod);

        backupManager.saveSettings(s);
        Toast.makeText(this, " ব্যাকআপ সেটিংস সেভ হয়েছে!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void animateIn() {
        int[] ids = {R.id.cardAutoTriggers, R.id.cardBackupMethodSettings,
            R.id.cardFrequency, R.id.btnSaveBackupSettings};
        long delay = 80;
        for (int id : ids) {
            View v = findViewById(id);
            if (v != null) {
                v.setAlpha(0f);
                v.setTranslationY(30f);
                v.animate().alpha(1f).translationY(0f)
                    .setStartDelay(delay).setDuration(280).start();
                delay += 80;
            }
        }
    }
}
