package com.jrappspot.cashlipi.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.cardview.widget.CardView;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.models.BackupRecord;
import com.jrappspot.cashlipi.models.TelegramConfig;
import com.jrappspot.cashlipi.utils.BackupManager;
import com.jrappspot.cashlipi.utils.DatabaseManager;

import java.io.File;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * CreateBackupActivity — Premium Create Backup Screen.
 *
 * User selects:
 *  - Data type (Income / Expense / Debt / Receivable / All)
 *  - Format (JSON / PDF / DOCX / XLSX)
 *  - Method (Local / Telegram / Google Drive)
 * Then taps "Start Backup".
 */
public class CreateBackupActivity extends BaseActivity {

    private BackupManager backupManager;
    private DatabaseManager db;

    // Selected options
    private String selectedDataType = BackupManager.TYPE_ALL;
    private String selectedFormat   = BackupManager.FORMAT_JSON;
    private String selectedMethod   = BackupManager.METHOD_LOCAL;

    // Data type cards
    private CardView cardAll, cardIncome, cardExpense, cardDebt, cardReceivable;

    // Format cards
    private CardView cardJson, cardPdf, cardDocx, cardXlsx;

    // Method cards
    private CardView cardMethodTelegram, cardMethodGDrive, cardMethodLocal;

    // Counts
    private TextView tvIncomeCount, tvExpenseCount, tvDebtCount, tvReceivableCount, tvAllCount;

    // File saver launcher (for local storage)
    private ActivityResultLauncher<String> createFileLauncher;
    private String pendingContent = "";
    private String pendingFileName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_backup);

        backupManager = BackupManager.getInstance(this);
        db = DatabaseManager.getInstance(this);

        registerFileLauncher();
        initViews();
        setupClickListeners();
        loadCounts();
        updateSelectionUI();
    }

    private void registerFileLauncher() {
        createFileLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"),
            uri -> {
                if (uri != null && !pendingContent.isEmpty()) {
                    saveToUri(uri, pendingContent, pendingFileName);
                }
            }
        );
    }

    private void initViews() {
        // Data type cards
        cardAll        = findViewById(R.id.cardDataAll);
        cardIncome     = findViewById(R.id.cardDataIncome);
        cardExpense    = findViewById(R.id.cardDataExpense);
        cardDebt       = findViewById(R.id.cardDataDebt);
        cardReceivable = findViewById(R.id.cardDataReceivable);

        tvAllCount        = findViewById(R.id.tvAllCount);
        tvIncomeCount     = findViewById(R.id.tvIncomeCount);
        tvExpenseCount    = findViewById(R.id.tvExpenseCount);
        tvDebtCount       = findViewById(R.id.tvDebtCount);
        tvReceivableCount = findViewById(R.id.tvReceivableCount);

        // Format cards
        cardJson = findViewById(R.id.cardFormatJson);
        cardPdf  = findViewById(R.id.cardFormatPdf);
        cardDocx = findViewById(R.id.cardFormatDocx);
        cardXlsx = findViewById(R.id.cardFormatXlsx);

        // Method cards
        cardMethodTelegram = findViewById(R.id.cardMethodTelegram);
        cardMethodGDrive   = findViewById(R.id.cardMethodGDrive);
        cardMethodLocal    = findViewById(R.id.cardMethodLocal);
    }

    private void setupClickListeners() {
        // Back

        // Data type selection
        cardAll.setOnClickListener(v -> selectDataType(BackupManager.TYPE_ALL));
        cardIncome.setOnClickListener(v -> selectDataType(BackupManager.TYPE_INCOME));
        cardExpense.setOnClickListener(v -> selectDataType(BackupManager.TYPE_EXPENSE));
        cardDebt.setOnClickListener(v -> selectDataType(BackupManager.TYPE_DEBT));
        cardReceivable.setOnClickListener(v -> selectDataType(BackupManager.TYPE_RECEIVABLE));

        // Format selection
        cardJson.setOnClickListener(v -> selectFormat(BackupManager.FORMAT_JSON));
        cardPdf.setOnClickListener(v -> selectFormat(BackupManager.FORMAT_PDF));
        cardDocx.setOnClickListener(v -> selectFormat(BackupManager.FORMAT_DOCX));
        cardXlsx.setOnClickListener(v -> selectFormat(BackupManager.FORMAT_XLSX));

        // Method selection
        cardMethodTelegram.setOnClickListener(v -> selectMethod(BackupManager.METHOD_TELEGRAM));
        cardMethodGDrive.setOnClickListener(v -> selectMethod(BackupManager.METHOD_GOOGLE_DRIVE));
        cardMethodLocal.setOnClickListener(v -> selectMethod(BackupManager.METHOD_LOCAL));

        // Start Backup
        findViewById(R.id.btnStartBackup).setOnClickListener(v -> startBackup());
    }

    private void loadCounts() {
        int incomeCount  = db.getIncomeList().size();
        int expenseCount = db.getExpenseList().size();
        int ledgerCount  = db.getLedgerList().size();
        int debtCount    = (int) db.getLedgerList().stream()
            .filter(e -> "dena".equals(e.getType())).count();
        int receivable   = (int) db.getLedgerList().stream()
            .filter(e -> "pabona".equals(e.getType())).count();

        tvIncomeCount.setText("৳ " + DatabaseManager.formatAmount(db.getTotalIncome()));
        tvExpenseCount.setText("৳ " + DatabaseManager.formatAmount(db.getTotalExpense()));
        tvDebtCount.setText("৳ " + DatabaseManager.formatAmount(db.getTotalDena()));
        tvReceivableCount.setText("৳ " + DatabaseManager.formatAmount(db.getTotalPabona()));
        tvAllCount.setText("৳ " + DatabaseManager.formatAmount(
            db.getTotalIncome() + db.getTotalExpense()
                + db.getTotalDena() + db.getTotalPabona()));
    }

    private void selectDataType(String type) {
        selectedDataType = type;
        updateSelectionUI();
    }

    private void selectFormat(String format) {
        selectedFormat = format;
        updateSelectionUI();
    }

    private void selectMethod(String method) {
        selectedMethod = method;
        updateSelectionUI();
    }

    /** Updates card highlight states to reflect current selection */
    private void updateSelectionUI() {
        // Data type
        setCardSelected(cardAll,        BackupManager.TYPE_ALL.equals(selectedDataType));
        setCardSelected(cardIncome,     BackupManager.TYPE_INCOME.equals(selectedDataType));
        setCardSelected(cardExpense,    BackupManager.TYPE_EXPENSE.equals(selectedDataType));
        setCardSelected(cardDebt,       BackupManager.TYPE_DEBT.equals(selectedDataType));
        setCardSelected(cardReceivable, BackupManager.TYPE_RECEIVABLE.equals(selectedDataType));

        // Format
        setFormatSelected(cardJson, BackupManager.FORMAT_JSON.equals(selectedFormat), ContextCompat.getColor(this, R.color.formatJsonCardBg));
        setFormatSelected(cardPdf,  BackupManager.FORMAT_PDF.equals(selectedFormat),  ContextCompat.getColor(this, R.color.formatPdfCardBg));
        setFormatSelected(cardDocx, BackupManager.FORMAT_DOCX.equals(selectedFormat), ContextCompat.getColor(this, R.color.formatJsonCardBg));
        setFormatSelected(cardXlsx, BackupManager.FORMAT_XLSX.equals(selectedFormat), ContextCompat.getColor(this, R.color.formatXlsxCardBg));

        // Method
        setMethodSelected(cardMethodTelegram, BackupManager.METHOD_TELEGRAM.equals(selectedMethod));
        setMethodSelected(cardMethodGDrive,   BackupManager.METHOD_GOOGLE_DRIVE.equals(selectedMethod));
        setMethodSelected(cardMethodLocal,    BackupManager.METHOD_LOCAL.equals(selectedMethod));
    }

    private void setCardSelected(CardView card, boolean selected) {
        if (card == null) return;
        if (selected) {
            card.setCardBackgroundColor(0xFF1E3A5F);
            card.setCardElevation(8f);
        } else {
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.bkCardInner));
            card.setCardElevation(2f);
        }
    }

    private void setFormatSelected(CardView card, boolean selected, int colorInt) {
        if (card == null) return;
        if (selected) {
            try {
                card.setCardBackgroundColor(colorInt);
            } catch (Exception e) {
                card.setCardBackgroundColor(0xFF1D4ED8);
            }
            card.setCardElevation(8f);
        } else {
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.bkCardInner));
            card.setCardElevation(2f);
        }
    }

    private void setMethodSelected(CardView card, boolean selected) {
        if (card == null) return;
        if (selected) {
            card.setCardBackgroundColor(0xFF0C2340);
            card.setCardElevation(10f);
            // setStrokeColor() is MaterialCardView-only; using foreground overlay instead
            card.setForeground(new android.graphics.drawable.ColorDrawable(0x2200C6FF));
        } else {
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.bkCardInner));
            card.setCardElevation(2f);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  START BACKUP
    // ═══════════════════════════════════════════════════════════════════════

    private void startBackup() {
        // Validate
        if (!BackupManager.FORMAT_JSON.equals(selectedFormat)) {
            Toast.makeText(this,
                " ফিচার শীঘ্রই আসছে! এখন শুধু JSON ব্যাকআপ সমর্থিত।",
                Toast.LENGTH_LONG).show();
            selectedFormat = BackupManager.FORMAT_JSON;
            updateSelectionUI();
            return;
        }

        String json = backupManager.generateJsonBackup(selectedDataType);
        if (json == null || json.isEmpty()) {
            Toast.makeText(this, " ব্যাকআপ তৈরি করতে ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = backupManager.generateFileName(selectedFormat);

        switch (selectedMethod) {
            case BackupManager.METHOD_LOCAL:
                doLocalBackup(json, fileName);
                break;
            case BackupManager.METHOD_TELEGRAM:
                doTelegramBackup(json, fileName);
                break;
            case BackupManager.METHOD_GOOGLE_DRIVE:
                doGoogleDriveBackup(json, fileName);
                break;
        }
    }

    // ── Local Backup ──────────────────────────────────────────────────────
    private void doLocalBackup(String json, String fileName) {
        pendingContent  = json;
        pendingFileName = fileName;
        createFileLauncher.launch(fileName);
    }

    private void saveToUri(Uri uri, String content, String fileName) {
        try (OutputStream os = getContentResolver().openOutputStream(uri)) {
            os.write(content.getBytes("UTF-8"));
            long size = content.getBytes("UTF-8").length;

            // Save record
            BackupRecord record = backupManager.buildRecord(
                selectedDataType, selectedFormat, BackupManager.METHOD_LOCAL,
                BackupManager.STATUS_SUCCESS, fileName, size);
            backupManager.addToHistory(record);

            // Update settings last backup
            com.jrappspot.cashlipi.models.BackupSettings s = backupManager.getSettings();
            s.setLastBackupAt(BackupManager.formatDisplayDateTime(record.getDate(), record.getTime()));
            s.setLastBackupStatus(BackupManager.STATUS_SUCCESS);
            backupManager.saveSettings(s);

            Toast.makeText(this, " ব্যাকআপ সফলভাবে সেভ হয়েছে!", Toast.LENGTH_LONG).show();
            finish();

        } catch (Exception e) {
            Toast.makeText(this, " সেভ ব্যর্থ: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ── Telegram Backup ───────────────────────────────────────────────────
    private void doTelegramBackup(String json, String fileName) {
        TelegramConfig cfg = backupManager.getTelegramConfig();
        if (!cfg.isValid()) {
            Toast.makeText(this,
                " Telegram Bot Token ও Chat ID সেট করুন আগে",
                Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, TelegramBackupActivity.class));
            return;
        }

        // Save temp file
        File tmpFile = backupManager.saveJsonToCache(json, fileName);
        if (tmpFile == null) {
            Toast.makeText(this, " ফাইল তৈরি ব্যর্থ", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage(" টেলিগ্রামে পাঠানো হচ্ছে...");
        pd.setCancelable(false);
        pd.show();

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... v) {
                return backupManager.sendFileToTelegram(
                    cfg.getBotToken(), cfg.getChatId(), json, fileName);
            }

            @Override
            protected void onPostExecute(Boolean success) {
                pd.dismiss();
                long size = json.getBytes().length;

                BackupRecord record = backupManager.buildRecord(
                    selectedDataType, selectedFormat, BackupManager.METHOD_TELEGRAM,
                    success ? BackupManager.STATUS_SUCCESS : BackupManager.STATUS_FAILED,
                    fileName, size);
                backupManager.addToHistory(record);

                if (success) {
                    // Also update settings
                    com.jrappspot.cashlipi.models.BackupSettings s = backupManager.getSettings();
                    s.setLastBackupAt(BackupManager.formatDisplayDateTime(
                        record.getDate(), record.getTime()));
                    backupManager.saveSettings(s);

                    Toast.makeText(CreateBackupActivity.this,
                        " টেলিগ্রামে ব্যাকআপ পাঠানো হয়েছে!", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    // Fallback to share intent
                    Uri uri = backupManager.getFileUri(tmpFile);
                    if (uri != null) {
                        Intent share = backupManager.createTelegramShareIntent(uri,
                            " CashLipi ক্যাশলিপি Backup — " + fileName);
                        try {
                            startActivity(share);
                        } catch (Exception ex) {
                            Toast.makeText(CreateBackupActivity.this,
                                " টেলিগ্রাম পাঠানো ব্যর্থ। সরাসরি শেয়ার করুন।",
                                Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
        }.execute();
    }

    // ── Google Drive Backup ───────────────────────────────────────────────
    private void doGoogleDriveBackup(String json, String fileName) {
        if (!db.isGoogleSignedIn()) {
            Toast.makeText(this,
                " Google Drive ব্যাকআপের জন্য আগে Google Sign-In করুন",
                Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, GoogleDriveBackupActivity.class));
            return;
        }

        // Save locally first, then share to Drive
        File tmpFile = backupManager.saveJsonToCache(json, fileName);
        if (tmpFile == null) {
            Toast.makeText(this, " ফাইল তৈরি ব্যর্থ", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = backupManager.getFileUri(tmpFile);
        if (uri == null) {
            Toast.makeText(this, " ফাইল URI তৈরি ব্যর্থ", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/json");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.putExtra(Intent.EXTRA_TITLE, fileName);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Try Google Drive directly
        shareIntent.setPackage("com.google.android.apps.docs");
        try {
            startActivity(shareIntent);
        } catch (android.content.ActivityNotFoundException e) {
            shareIntent.setPackage(null);
            startActivity(Intent.createChooser(shareIntent, "Google Drive-এ পাঠান"));
        }

        // Log record
        BackupRecord record = backupManager.buildRecord(
            selectedDataType, selectedFormat, BackupManager.METHOD_GOOGLE_DRIVE,
            BackupManager.STATUS_SUCCESS, fileName, json.getBytes().length);
        backupManager.addToHistory(record);
    }
}
