package com.jrappspot.cashlipi.activities;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


import androidx.cardview.widget.CardView;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.models.BackupRecord;
import com.jrappspot.cashlipi.models.BackupSettings;
import com.jrappspot.cashlipi.utils.BackupManager;
import com.jrappspot.cashlipi.utils.DatabaseManager;
import com.jrappspot.cashlipi.utils.GoogleDriveSyncManager;
import android.os.Handler;
import android.os.Looper;

/**
 * BackupCenterActivity — Premium Backup Hub Screen.
 *
 * Shows:
 * - Latest backup card
 * - Quick action buttons (Create, Restore, History, Settings)
 * - Auto backup status
 * - Backup methods (Telegram, Google Drive, Local)
 * - Backup summary (Income/Expense/Debt/Receivable counts & amounts)
 */
public class BackupCenterActivity extends BaseActivity {

    private BackupManager backupManager;
    private DatabaseManager db;
    private GoogleDriveSyncManager driveSync;
    private Handler mainHandler;

    // Views — Latest Backup Card
    private CardView cardLatestBackup, cardNoBackup;
    private TextView tvLastDate, tvLastTime, tvLastType, tvLastMethod,
                     tvLastSize, tvLastStatus;

    // Views — Auto Backup
    private Switch switchAutoBackup;
    private TextView tvAutoStatus, tvLastBackupTime;

    // Views — Backup Methods
    private CardView cardTelegram, cardGoogleDrive, cardLocalStorage;
    private Switch switchTelegram, switchGoogleDrive, switchLocal;
    private TextView tvTelegramStatus, tvGoogleStatus, tvLocalStatus;

    // Views — Summary
    private TextView tvSummaryIncome, tvSummaryExpense, tvSummaryDebt, tvSummaryReceivable;

    // Stats
    private TextView tvStatTotal, tvStatSuccessRate, tvStatTelegramCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_center);

        backupManager = BackupManager.getInstance(this);
        db = DatabaseManager.getInstance(this);
        driveSync = new GoogleDriveSyncManager(this);
        mainHandler = new Handler(Looper.getMainLooper());

        initViews();
        applyMethodCardGradients();
        setupClickListeners();
        loadData();
        animateCards();
    }

    /**
     *  FORCE-FIX: Telegram/Drive/Local card-এর গ্রেডিয়েন্ট ব্যাকগ্রাউন্ড
     * প্রোগ্রামেটিকভাবে সেট করা হচ্ছে — কোনো XML drawable resource resolution
     * বা build-cache সমস্যার উপর নির্ভর করবে না, ১০০% guaranteed apply হবে।
     */
    private void applyMethodCardGradients() {
        int radius = dpToPx(16);

        LinearLayout telegramBg = findViewById(R.id.innerTelegramGradient);
        if (telegramBg != null) {
            GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{androidx.core.content.ContextCompat.getColor(this, R.color.bkTelegramGradStart), androidx.core.content.ContextCompat.getColor(this, R.color.bkTelegramGradEnd)});
            g.setCornerRadius(radius);
            telegramBg.setBackground(g);
        }

        LinearLayout gdriveBg = findViewById(R.id.innerGDriveGradient);
        if (gdriveBg != null) {
            GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{androidx.core.content.ContextCompat.getColor(this, R.color.bkGDriveGradStart), androidx.core.content.ContextCompat.getColor(this, R.color.bkGDriveGradEnd)});
            g.setCornerRadius(radius);
            gdriveBg.setBackground(g);
        }

        LinearLayout localBg = findViewById(R.id.innerLocalGradient);
        if (localBg != null) {
            GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{androidx.core.content.ContextCompat.getColor(this, R.color.bkLocalGradStart), androidx.core.content.ContextCompat.getColor(this, R.color.bkLocalGradEnd)});
            g.setCornerRadius(radius);
            localBg.setBackground(g);
        }

        // ── Backup Summary cards — colours come from colors.xml (bkXxxGradStart/Mid/End) ──
        int summaryRadius = dpToPx(14);

        LinearLayout incomeBg = findViewById(R.id.innerIncomeSummaryGradient);
        if (incomeBg != null) {
            GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{
                    androidx.core.content.ContextCompat.getColor(this, R.color.bkIncomeGradStart),
                    androidx.core.content.ContextCompat.getColor(this, R.color.bkIncomeGradMid),
                    androidx.core.content.ContextCompat.getColor(this, R.color.bkIncomeGradEnd)});
            g.setCornerRadius(summaryRadius);
            incomeBg.setBackground(g);
        }

        LinearLayout expenseBg = findViewById(R.id.innerExpenseSummaryGradient);
        if (expenseBg != null) {
            GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{
                    androidx.core.content.ContextCompat.getColor(this, R.color.bkExpenseGradStart),
                    androidx.core.content.ContextCompat.getColor(this, R.color.bkExpenseGradMid),
                    androidx.core.content.ContextCompat.getColor(this, R.color.bkExpenseGradEnd)});
            g.setCornerRadius(summaryRadius);
            expenseBg.setBackground(g);
        }

        LinearLayout debtBg = findViewById(R.id.innerDebtSummaryGradient);
        if (debtBg != null) {
            GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{
                    androidx.core.content.ContextCompat.getColor(this, R.color.bkDebtGradStart),
                    androidx.core.content.ContextCompat.getColor(this, R.color.bkDebtGradMid),
                    androidx.core.content.ContextCompat.getColor(this, R.color.bkDebtGradEnd)});
            g.setCornerRadius(summaryRadius);
            debtBg.setBackground(g);
        }

        LinearLayout receivableBg = findViewById(R.id.innerReceivableSummaryGradient);
        if (receivableBg != null) {
            GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{
                    androidx.core.content.ContextCompat.getColor(this, R.color.bkReceivableGradStart),
                    androidx.core.content.ContextCompat.getColor(this, R.color.bkReceivableGradMid),
                    androidx.core.content.ContextCompat.getColor(this, R.color.bkReceivableGradEnd)});
            g.setCornerRadius(summaryRadius);
            receivableBg.setBackground(g);
        }

        // ── Force pure-white text on gradient method cards (guaranteed contrast) ──
        setTextColorIfExists(R.id.tvTelegramTitle, 0xFFFFFFFF);
        setTextColorIfExists(R.id.tvTelegramMethodStatus, androidx.core.content.ContextCompat.getColor(this, R.color.bkTelegramText));
        setTextColorIfExists(R.id.tvGDriveTitle, 0xFFFFFFFF);
        setTextColorIfExists(R.id.tvGDriveMethodStatus, androidx.core.content.ContextCompat.getColor(this, R.color.bkGDriveText));
        setTextColorIfExists(R.id.tvLocalTitle, 0xFFFFFFFF);
        setTextColorIfExists(R.id.tvLocalMethodStatus, androidx.core.content.ContextCompat.getColor(this, R.color.bkLocalText));
    }

    private void setTextColorIfExists(int viewId, int color) {
        TextView tv = findViewById(viewId);
        if (tv != null) tv.setTextColor(color);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData(); // Refresh on return from sub-screens
    }

    private void initViews() {
        // Back button

        // Latest Backup
        cardLatestBackup = findViewById(R.id.cardLatestBackup);
        cardNoBackup     = findViewById(R.id.cardNoBackup);
        tvLastDate   = findViewById(R.id.tvLastBackupDate);
        // tvLastTime: R.id.tvLastBackupTime does not exist in layout – assignment removed
        tvLastType   = findViewById(R.id.tvLastBackupType);
        tvLastMethod = findViewById(R.id.tvLastBackupMethod);
        tvLastSize   = findViewById(R.id.tvLastBackupSize);
        tvLastStatus = findViewById(R.id.tvLastBackupStatus);

        // Auto Backup
        switchAutoBackup  = findViewById(R.id.switchAutoBackup);
        tvAutoStatus      = findViewById(R.id.tvAutoBackupStatus);
        tvLastBackupTime  = findViewById(R.id.tvAutoLastBackupTime);

        // Methods
        cardTelegram    = findViewById(R.id.cardTelegramMethod);
        cardGoogleDrive = findViewById(R.id.cardGDriveMethod);
        cardLocalStorage= findViewById(R.id.cardLocalMethod);
        switchTelegram  = findViewById(R.id.switchTelegram);
        switchGoogleDrive = findViewById(R.id.switchGDrive);
        switchLocal     = findViewById(R.id.switchLocal);
        tvTelegramStatus= findViewById(R.id.tvTelegramMethodStatus);
        tvGoogleStatus  = findViewById(R.id.tvGDriveMethodStatus);
        tvLocalStatus   = findViewById(R.id.tvLocalMethodStatus);

        // Summary
        tvSummaryIncome    = findViewById(R.id.tvSummaryIncome);
        tvSummaryExpense   = findViewById(R.id.tvSummaryExpense);
        tvSummaryDebt      = findViewById(R.id.tvSummaryDebt);
        tvSummaryReceivable= findViewById(R.id.tvSummaryReceivable);

        // Stats
        tvStatTotal       = findViewById(R.id.tvStatTotalBackups);
        tvStatSuccessRate = findViewById(R.id.tvStatSuccessRate);
        tvStatTelegramCount= findViewById(R.id.tvStatTelegramCount);
    }

    private void setupClickListeners() {
        // Quick Actions
        findViewById(R.id.btnCreateBackup).setOnClickListener(v ->
            startActivity(new Intent(this, CreateBackupActivity.class)));

        findViewById(R.id.btnRestoreBackup).setOnClickListener(v ->
            startActivity(new Intent(this, RestoreBackupActivity.class)));

        findViewById(R.id.btnBackupHistory).setOnClickListener(v ->
            startActivity(new Intent(this, BackupHistoryActivity.class)));

        findViewById(R.id.btnBackupSettings).setOnClickListener(v ->
            startActivity(new Intent(this, BackupSettingsActivity.class)));

        // View Details button on latest backup card
        View btnViewDetails = findViewById(R.id.btnViewDetails);
        if (btnViewDetails != null) {
            btnViewDetails.setOnClickListener(v -> {
                BackupRecord last = backupManager.getLastBackupRecord();
                if (last != null) {
                    Intent i = new Intent(this, BackupDetailsActivity.class);
                    i.putExtra("record_id", last.getId());
                    startActivity(i);
                }
            });
        }

        // Auto backup toggle
        switchAutoBackup.setOnCheckedChangeListener((btn, checked) -> {
            BackupSettings settings = backupManager.getSettings();
            settings.setAutoBackupEnabled(checked);
            backupManager.saveSettings(settings);
            updateAutoBackupUI(checked, settings.getLastBackupAt());
        });

        // Method cards → open respective settings
        cardTelegram.setOnClickListener(v ->
            startActivity(new Intent(this, TelegramBackupActivity.class)));
        cardGoogleDrive.setOnClickListener(v ->
            startActivity(new Intent(this, GoogleDriveBackupActivity.class)));
        cardLocalStorage.setOnClickListener(v ->
            startActivity(new Intent(this, CreateBackupActivity.class)));

        // Method toggles
        switchTelegram.setOnCheckedChangeListener((btn, checked) -> {
            BackupSettings s = backupManager.getSettings();
            s.setTelegramEnabled(checked);
            backupManager.saveSettings(s);
            updateTelegramMethodStatus(checked);
        });
        switchGoogleDrive.setOnCheckedChangeListener((btn, checked) -> {
            if (checked && !db.isGoogleSignedIn()) {
                Toast.makeText(this, " আগে Google Sign-In করুন (প্রোফাইল থেকে)", Toast.LENGTH_LONG).show();
                btn.setChecked(false);
                return;
            }
            BackupSettings s = backupManager.getSettings();
            s.setGoogleDriveEnabled(checked);
            backupManager.saveSettings(s);
            db.setDriveAutoSyncEnabled(checked);
            updateGDriveMethodStatus(checked);
            if (checked) {
                Toast.makeText(this, " Google Drive সিঙ্ক চালু হচ্ছে...", Toast.LENGTH_SHORT).show();
                performGoogleDriveSync();
            } else {
                Toast.makeText(this, " Google Drive সিঙ্ক বন্ধ করা হয়েছে", Toast.LENGTH_SHORT).show();
            }
        });
        switchLocal.setOnCheckedChangeListener((btn, checked) -> {
            BackupSettings s = backupManager.getSettings();
            s.setLocalStorageEnabled(checked);
            backupManager.saveSettings(s);
            updateLocalMethodStatus(checked);
        });
    }

    private void loadData() {
        BackupSettings settings = backupManager.getSettings();
        BackupRecord lastRecord = backupManager.getLastBackupRecord();

        // Latest Backup Card
        if (lastRecord != null) {
            cardLatestBackup.setVisibility(View.VISIBLE);
            if (cardNoBackup != null) cardNoBackup.setVisibility(View.GONE);

            tvLastDate.setText(BackupManager.formatDisplayDateTime(
                lastRecord.getDate(), lastRecord.getTime()));
            tvLastType.setText(lastRecord.getDataTypeDisplay());
            tvLastMethod.setText(lastRecord.getMethodIcon() + " " + lastRecord.getMethodDisplay());
            tvLastSize.setText(" " + lastRecord.getFileSizeDisplay());
            tvLastStatus.setText(lastRecord.isSuccess() ? " সফল" : " ব্যর্থ");
        } else {
            cardLatestBackup.setVisibility(View.GONE);
            if (cardNoBackup != null) cardNoBackup.setVisibility(View.VISIBLE);
        }

        // Auto Backup Status
        switchAutoBackup.setOnCheckedChangeListener(null); // prevent trigger
        switchAutoBackup.setChecked(settings.isAutoBackupEnabled());
        switchAutoBackup.setOnCheckedChangeListener((btn, checked) -> {
            settings.setAutoBackupEnabled(checked);
            backupManager.saveSettings(settings);
            updateAutoBackupUI(checked, settings.getLastBackupAt());
        });
        updateAutoBackupUI(settings.isAutoBackupEnabled(), settings.getLastBackupAt());

        // Method switches
        switchTelegram.setOnCheckedChangeListener(null);
        switchGoogleDrive.setOnCheckedChangeListener(null);
        switchLocal.setOnCheckedChangeListener(null);

        switchTelegram.setChecked(settings.isTelegramEnabled());
        switchGoogleDrive.setChecked(settings.isGoogleDriveEnabled());
        switchLocal.setChecked(settings.isLocalStorageEnabled());

        switchTelegram.setOnCheckedChangeListener((b, c) -> {
            settings.setTelegramEnabled(c);
            backupManager.saveSettings(settings);
            updateTelegramMethodStatus(c);
        });
        switchGoogleDrive.setOnCheckedChangeListener((b, c) -> {
            if (c && !db.isGoogleSignedIn()) {
                Toast.makeText(this, " আগে Google Sign-In করুন (প্রোফাইল থেকে)", Toast.LENGTH_LONG).show();
                b.setChecked(false);
                return;
            }
            settings.setGoogleDriveEnabled(c);
            backupManager.saveSettings(settings);
            db.setDriveAutoSyncEnabled(c);
            updateGDriveMethodStatus(c);
            if (c) {
                Toast.makeText(this, " Google Drive সিঙ্ক চালু হচ্ছে...", Toast.LENGTH_SHORT).show();
                performGoogleDriveSync();
            } else {
                Toast.makeText(this, " Google Drive সিঙ্ক বন্ধ করা হয়েছে", Toast.LENGTH_SHORT).show();
            }
        });
        switchLocal.setOnCheckedChangeListener((b, c) -> {
            settings.setLocalStorageEnabled(c);
            backupManager.saveSettings(settings);
            updateLocalMethodStatus(c);
        });

        updateTelegramMethodStatus(settings.isTelegramEnabled());
        updateGDriveMethodStatus(settings.isGoogleDriveEnabled());
        updateLocalMethodStatus(settings.isLocalStorageEnabled());

        // Summary
        tvSummaryIncome.setText(DatabaseManager.formatAmount(db.getTotalIncome()));
        tvSummaryExpense.setText(DatabaseManager.formatAmount(db.getTotalExpense()));
        tvSummaryDebt.setText(DatabaseManager.formatAmount(db.getTotalDena()));
        tvSummaryReceivable.setText(DatabaseManager.formatAmount(db.getTotalPabona()));

        // Text colour from colors.xml — bkXxxText
        int incomeText    = androidx.core.content.ContextCompat.getColor(this, R.color.bkIncomeText);
        int expenseText   = androidx.core.content.ContextCompat.getColor(this, R.color.bkExpenseText);
        int debtText      = androidx.core.content.ContextCompat.getColor(this, R.color.bkDebtText);
        int receivableText= androidx.core.content.ContextCompat.getColor(this, R.color.bkReceivableText);
        tvSummaryIncome.setTextColor(incomeText);
        tvSummaryExpense.setTextColor(expenseText);
        tvSummaryDebt.setTextColor(debtText);
        tvSummaryReceivable.setTextColor(receivableText);
        TextView tvIncLabel = findViewById(R.id.tvSummaryIncomeLabel);
        TextView tvExpLabel = findViewById(R.id.tvSummaryExpenseLabel);
        TextView tvDbtLabel = findViewById(R.id.tvSummaryDebtLabel);
        TextView tvRecLabel = findViewById(R.id.tvSummaryReceivableLabel);
        if (tvIncLabel != null) tvIncLabel.setTextColor(incomeText);
        if (tvExpLabel != null) tvExpLabel.setTextColor(expenseText);
        if (tvDbtLabel != null) tvDbtLabel.setTextColor(debtText);
        if (tvRecLabel != null) tvRecLabel.setTextColor(receivableText);

        // Stats
        int totalBackups = backupManager.getTotalBackupCount();
        tvStatTotal.setText(totalBackups + " ব্যাকআপ");

        java.util.List<com.jrappspot.cashlipi.models.BackupRecord> history = backupManager.getBackupHistory();
        long success = history.stream().filter(BackupRecord::isSuccess).count();
        int rate = history.isEmpty() ? 0 : (int)((success * 100) / history.size());
        tvStatSuccessRate.setText(rate + "% সফল");

        tvStatTelegramCount.setText(
            backupManager.getBackupCountByMethod(BackupManager.METHOD_TELEGRAM) + " Telegram");
    }

    private void updateAutoBackupUI(boolean enabled, String lastAt) {
        if (enabled) {
            tvAutoStatus.setText(" অটো ব্যাকআপ চালু আছে");
            tvAutoStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.bkAutoActiveText));
        } else {
            tvAutoStatus.setText(" অটো ব্যাকআপ বন্ধ আছে");
            tvAutoStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.bkAutoInactiveText));
        }
        if (lastAt != null && !lastAt.isEmpty()) {
            tvLastBackupTime.setText("সর্বশেষ: " + lastAt);
            tvLastBackupTime.setVisibility(View.VISIBLE);
        } else {
            tvLastBackupTime.setVisibility(View.GONE);
        }
    }

    private void updateTelegramMethodStatus(boolean enabled) {
        tvTelegramStatus.setText(enabled
            ? "Bot Token ও Chat ID সেট করুন"
            : "সেটআপ প্রয়োজন");
        tvTelegramStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.bkTelegramText));
    }

    private void updateGDriveMethodStatus(boolean enabled) {
        if (!enabled) {
            tvGoogleStatus.setText("Google Drive সংযোগ প্রয়োজন");
            tvGoogleStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.bkGDriveText));
            return;
        }
        if (!db.isGoogleSignedIn()) {
            tvGoogleStatus.setText("Google Sign-In প্রয়োজন");
            tvGoogleStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.bkGDriveText));
            return;
        }
        long last = db.getLastDriveSyncTime();
        if (last == 0L) {
            tvGoogleStatus.setText(" চালু — এখনো সিঙ্ক হয়নি");
        } else {
            String formatted = android.text.format.DateFormat.format("dd MMM, HH:mm", last).toString();
            tvGoogleStatus.setText(" সর্বশেষ সিঙ্ক: " + formatted);
        }
        tvGoogleStatus.setTextColor(0xFFFFFFFF);
    }

    /**  সত্যিকারের Google Drive sync — তাৎক্ষণিকভাবে চালায় */
    private void performGoogleDriveSync() {
        String json = backupManager.generateJsonBackup(BackupManager.TYPE_ALL);
        driveSync.syncNow(json, new GoogleDriveSyncManager.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                mainHandler.post(() -> {
                    Toast.makeText(BackupCenterActivity.this, message, Toast.LENGTH_SHORT).show();
                    updateGDriveMethodStatus(true);
                });
            }
            @Override
            public void onFailure(String error) {
                mainHandler.post(() -> {
                    Toast.makeText(BackupCenterActivity.this, " " + error, Toast.LENGTH_LONG).show();
                    updateGDriveMethodStatus(backupManager.getSettings().isGoogleDriveEnabled());
                });
            }
        });
    }

    private void updateLocalMethodStatus(boolean enabled) {
        tvLocalStatus.setText(enabled
            ? "CashLipi ফোল্ডারে সেভ হচ্ছে"
            : "লোকাল ব্যাকআপ বন্ধ");
        tvLocalStatus.setTextColor(enabled ? androidx.core.content.ContextCompat.getColor(this, R.color.bkLocalText) : androidx.core.content.ContextCompat.getColor(this, R.color.bkGDriveText));
    }

    private void animateCards() {
        int[] ids = {R.id.cardLatestWrapper, R.id.cardQuickActions,
            R.id.cardAutoBackup, R.id.cardBackupMethods, R.id.cardSummary};
        long delay = 80;
        for (int id : ids) {
            View v = findViewById(id);
            if (v != null) {
                v.setAlpha(0f);
                v.setTranslationY(40f);
                v.animate().alpha(1f).translationY(0f)
                    .setStartDelay(delay).setDuration(350).start();
                delay += 80;
            }
        }
    }
}
