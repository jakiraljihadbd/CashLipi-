package com.jrappspot.cashlipi.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.*;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.*;
import android.widget.*;
import androidx.activity.result.*;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.cardview.widget.CardView;
import com.google.android.material.card.MaterialCardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.gson.*;
import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.utils.DatabaseManager;
import com.jrappspot.cashlipi.models.Transaction;
import com.jrappspot.cashlipi.models.LedgerEntry;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

/**
 * BackupActivity — Premium Backup Center
 * Features:
 *   JSON backup (working)
 *   PDF backup (working — built-in PdfDocument)
 *   Word/DOCX backup (working — HTML→.doc trick)
 *   Excel/CSV backup (working — CSV format)
 *   Local storage → Downloads/CashLipi/ (auto-save)
 *   Telegram Bot API backup
 *   Auto backup on data change
 *   Restore from JSON
 *   Backup history
 *   Premium Material 3 UI (Light + Dark)
 */
public class BackupActivity extends BaseActivity {

    // ── Prefs ──────────────────────────────────────────────────────────────
    private static final String PREF_BACKUP         = "cashlipi_backup_meta";
    private static final String KEY_LAST_DATE       = "last_backup_date";
    private static final String KEY_LAST_METHOD     = "last_backup_method";
    private static final String KEY_LAST_SIZE       = "last_backup_size";
    private static final String KEY_LAST_FORMAT     = "last_backup_format";
    private static final String KEY_BOT_TOKEN       = "tg_bot_token";
    private static final String KEY_CHAT_ID         = "tg_chat_id";
    private static final String KEY_TG_ENABLED      = "tg_enabled";
    private static final String KEY_AUTO_BACKUP     = "auto_backup";
    private static final String KEY_BACKUP_COUNT    = "backup_count";
    private static final String KEY_HISTORY         = "backup_history";

    private static final int REQUEST_STORAGE = 1001;
    private static final int TAB_CENTER = 0, TAB_CREATE = 1, TAB_RESTORE = 2,
                             TAB_TELEGRAM = 3, TAB_HISTORY = 4, TAB_SETTINGS = 5;

    private DatabaseManager db;
    private SharedPreferences bp;
    private int currentTab = TAB_CENTER;

    // Format + Method selection
    private String selData = "all", selFmt = "json", selMethod = "local";

    // File launchers
    private ActivityResultLauncher<String>   createFileLauncher;
    private ActivityResultLauncher<String[]> openFileLauncher;
    private String pendingContent = "";
    private String pendingFileName = "";
    private String pendingMime = "application/json";

    // Section views
    private View sCenter, sCreate, sRestore, sTelegram, sHistory, sSettings;
    // Center
    private TextView tvLastDate, tvLastMethod, tvLastSize, tvLastStatus, tvStatTotal;
    private TextView tvStatIncome, tvStatExpense, tvStatLedger, tvStatSavings;
    private Switch   switchAuto;
    private TextView tvAutoStatus;
    // Create
    private MaterialCardView cdAll, cdIncome, cdExpense, cdDebt, cdReceivable;
    private MaterialCardView cfJson, cfPdf, cfDocx, cfXlsx;
    private MaterialCardView cmLocal, cmTelegram, cmGDrive;
    private TextView tvAmt, tvIncAmt, tvExpAmt, tvDebtAmt, tvRecAmt;
    // Telegram
    private EditText etToken, etChat;
    private Switch   swTg;
    private TextView tvTgStatus;
    private boolean  tokenVis = false;
    // Settings
    private Switch swTrigInc, swTrigExp, swTrigDebt, swTrigRec, swTrigUpd, swTrigDel;
    private Switch swMTg, swMGDrive, swMLocal;
    private RadioGroup rgFreq;

    // ═══════════════════════════════════════════════════════════════════════
    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_backup);
        db = DatabaseManager.getInstance(this);
        bp = getSharedPreferences(PREF_BACKUP, MODE_PRIVATE);
        registerLaunchers();
        initViews();
        requestStoragePermission();
        showTab(TAB_CENTER);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  STORAGE PERMISSION
    // ═══════════════════════════════════════════════════════════════════════
    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                 Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_STORAGE);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  FILE LAUNCHERS
    // ═══════════════════════════════════════════════════════════════════════
    private void registerLaunchers() {
        createFileLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("*/*"), uri -> {
                if (uri != null && !pendingContent.isEmpty()) {
                    saveContentToUri(uri, pendingContent, pendingFileName);
                }
            });
        openFileLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) processRestoreFile(uri);
            });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  INIT
    // ═══════════════════════════════════════════════════════════════════════
    private void initViews() {

        sCenter   = findViewById(R.id.sectionCenter);
        sCreate   = findViewById(R.id.sectionCreate);
        sRestore  = findViewById(R.id.sectionRestore);
        sTelegram = findViewById(R.id.sectionTelegram);
        sHistory  = findViewById(R.id.sectionHistory);
        sSettings = findViewById(R.id.sectionSettings);

        // Nav
        int[] navIds = {R.id.navCenter,R.id.navCreate,R.id.navRestore,
                        R.id.navTelegram,R.id.navHistory,R.id.navSettings};
        for (int i = 0; i < navIds.length; i++) {
            final int tab = i;
            findViewById(navIds[i]).setOnClickListener(v -> showTab(tab));
        }

        // ── CENTER ──────────────────────────────────────────────────────
        tvLastDate   = findViewById(R.id.tvLastBackupDate);
        tvLastMethod = findViewById(R.id.tvLastBackupMethod);
        tvLastSize   = findViewById(R.id.tvLastBackupSize);
        tvLastStatus = findViewById(R.id.tvLastBackupStatus);
        tvStatTotal  = findViewById(R.id.tvStatTotal);
        tvStatIncome = findViewById(R.id.tvStatIncome);
        tvStatExpense= findViewById(R.id.tvStatExpense);
        tvStatLedger = findViewById(R.id.tvStatLedger);
        tvStatSavings= findViewById(R.id.tvStatSavings);
        switchAuto   = findViewById(R.id.switchAutoBackup);
        tvAutoStatus = findViewById(R.id.tvAutoStatus);

        switchAuto.setOnCheckedChangeListener((b, c) -> {
            bp.edit().putBoolean(KEY_AUTO_BACKUP, c).apply();
            tvAutoStatus.setText(c ? " অটো ব্যাকআপ চালু" : " অটো ব্যাকআপ বন্ধ");
            tvAutoStatus.setTextColor(getColor(c ? R.color.successColor : R.color.textSecondary));
        });

        findViewById(R.id.btnCenterCreate).setOnClickListener(v   -> showTab(TAB_CREATE));
        findViewById(R.id.btnCenterRestore).setOnClickListener(v  -> showTab(TAB_RESTORE));
        findViewById(R.id.btnCenterHistory).setOnClickListener(v  -> showTab(TAB_HISTORY));
        findViewById(R.id.btnCenterSettings).setOnClickListener(v -> showTab(TAB_SETTINGS));

        // Telegram / GDrive setআপ row clicks
        View rowTg = findViewById(R.id.rowTelegramMethod);
        View rowGd = findViewById(R.id.rowGDriveMethod);
        if (rowTg != null) rowTg.setOnClickListener(v -> showTab(TAB_TELEGRAM));
        if (rowGd != null) rowGd.setOnClickListener(v -> showTab(TAB_TELEGRAM));

        // ── CREATE ──────────────────────────────────────────────────────
        cdAll        = findViewById(R.id.cardDataAll);
        cdIncome     = findViewById(R.id.cardDataIncome);
        cdExpense    = findViewById(R.id.cardDataExpense);
        cdDebt       = findViewById(R.id.cardDataDebt);
        cdReceivable = findViewById(R.id.cardDataReceivable);
        cfJson  = findViewById(R.id.cardFmtJson);
        cfPdf   = findViewById(R.id.cardFmtPdf);
        cfDocx  = findViewById(R.id.cardFmtDocx);
        cfXlsx  = findViewById(R.id.cardFmtXlsx);
        cmLocal    = findViewById(R.id.cardMethodLocal);
        cmTelegram = findViewById(R.id.cardMethodTelegram);
        cmGDrive   = findViewById(R.id.cardMethodGDrive);
        tvAmt    = findViewById(R.id.tvCreateAllAmt);
        tvIncAmt = findViewById(R.id.tvCreateIncomeAmt);
        tvExpAmt = findViewById(R.id.tvCreateExpenseAmt);
        tvDebtAmt= findViewById(R.id.tvCreateDebtAmt);
        tvRecAmt = findViewById(R.id.tvCreateReceivableAmt);

        cdAll.setOnClickListener(v        -> selectData("all"));
        cdIncome.setOnClickListener(v     -> selectData("income"));
        cdExpense.setOnClickListener(v    -> selectData("expense"));
        cdDebt.setOnClickListener(v       -> selectData("debt"));
        cdReceivable.setOnClickListener(v -> selectData("receivable"));
        cfJson.setOnClickListener(v  -> selectFmt("json"));
        cfPdf.setOnClickListener(v   -> selectFmt("pdf"));
        cfDocx.setOnClickListener(v  -> selectFmt("docx"));
        cfXlsx.setOnClickListener(v  -> selectFmt("xlsx"));
        cmLocal.setOnClickListener(v    -> selectMethod("local"));
        cmTelegram.setOnClickListener(v -> selectMethod("telegram"));
        cmGDrive.setOnClickListener(v   -> selectMethod("gdrive"));

        findViewById(R.id.btnStartBackup).setOnClickListener(v -> startBackup());

        // ── RESTORE ──────────────────────────────────────────────────────
        findViewById(R.id.btnBrowseFile).setOnClickListener(v ->
            openFileLauncher.launch(new String[]{"application/json","*/*"}));
        View btnTgRestore = findViewById(R.id.btnRestoreFromTelegram);
        if (btnTgRestore != null) btnTgRestore.setOnClickListener(v ->
            toast(" Telegram থেকে JSON ডাউনলোড করে 'ফাইল ব্রাউজ' করুন"));

        // ── TELEGRAM ─────────────────────────────────────────────────────
        etToken   = findViewById(R.id.etBotToken);
        etChat    = findViewById(R.id.etChatId);
        swTg      = findViewById(R.id.switchTgEnabled);
        tvTgStatus= findViewById(R.id.tvTgStatus);

        View btnToggle = findViewById(R.id.btnToggleToken);
        if (btnToggle != null) btnToggle.setOnClickListener(v -> {
            tokenVis = !tokenVis;
            etToken.setTransformationMethod(tokenVis
                ? HideReturnsTransformationMethod.getInstance()
                : PasswordTransformationMethod.getInstance());
            etToken.setSelection(etToken.getText().length());
        });
        View btnTest = findViewById(R.id.btnTestConnection);
        View btnSave = findViewById(R.id.btnSaveTelegram);
        View btnSend = findViewById(R.id.btnSendBackupTg);
        if (btnTest != null) btnTest.setOnClickListener(v -> testTelegram());
        if (btnSave != null) btnSave.setOnClickListener(v -> saveTelegram());
        if (btnSend != null) btnSend.setOnClickListener(v -> sendNowTelegram());

        // ── SETTINGS ─────────────────────────────────────────────────────
        swTrigInc  = findViewById(R.id.swTrigIncome);
        swTrigExp  = findViewById(R.id.swTrigExpense);
        swTrigDebt = findViewById(R.id.swTrigDebt);
        swTrigRec  = findViewById(R.id.swTrigReceivable);
        swTrigUpd  = findViewById(R.id.swTrigUpdate);
        swTrigDel  = findViewById(R.id.swTrigDelete);
        swMTg      = findViewById(R.id.swSettingsTg);
        swMGDrive  = findViewById(R.id.swSettingsGDrive);
        swMLocal   = findViewById(R.id.swSettingsLocal);
        rgFreq     = findViewById(R.id.rgFrequency);
        if (swMTg != null) swMTg.setOnClickListener(v -> showTab(TAB_TELEGRAM));
        View btnSaveSet  = findViewById(R.id.btnSaveSettings);
        View btnResetSet = findViewById(R.id.btnResetSettings);
        if (btnSaveSet  != null) btnSaveSet.setOnClickListener(v  -> saveSettings());
        if (btnResetSet != null) btnResetSet.setOnClickListener(v -> resetSettings());

        // ── HISTORY ──────────────────────────────────────────────────────
        View btnClear = findViewById(R.id.btnClearHistory);
        if (btnClear != null) btnClear.setOnClickListener(v -> clearHistory());

        // ── PREMIUM PRESS FEEDBACK (scale to 98% on touch) ────────────────
        attachPressAnimation(findViewById(R.id.btnStartBackup));
        attachPressAnimation(findViewById(R.id.btnBrowseFile));
        attachPressAnimation(btnTgRestore);
        attachPressAnimation(btnTest);
        attachPressAnimation(btnSave);
        attachPressAnimation(btnSend);
        attachPressAnimation(btnSaveSet);
    }

    /** Premium 3D press feedback for any view: scales to 98% on press, springs back on release. */
    private void attachPressAnimation(View v) {
        if (v == null) return;
        v.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    view.animate().scaleX(0.98f).scaleY(0.98f).setDuration(90).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                    break;
            }
            return false; // allow the underlying click listener to still fire
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TAB NAVIGATION
    // ═══════════════════════════════════════════════════════════════════════
    private void showTab(int tab) {
        currentTab = tab;
        sCenter.setVisibility(tab == TAB_CENTER   ? View.VISIBLE : View.GONE);
        sCreate.setVisibility(tab == TAB_CREATE   ? View.VISIBLE : View.GONE);
        sRestore.setVisibility(tab == TAB_RESTORE ? View.VISIBLE : View.GONE);
        sTelegram.setVisibility(tab == TAB_TELEGRAM ? View.VISIBLE : View.GONE);
        sHistory.setVisibility(tab == TAB_HISTORY  ? View.VISIBLE : View.GONE);
        sSettings.setVisibility(tab == TAB_SETTINGS ? View.VISIBLE : View.GONE);

        int[] navIds = {R.id.navCenter,R.id.navCreate,R.id.navRestore,
                        R.id.navTelegram,R.id.navHistory,R.id.navSettings};
        for (int i = 0; i < navIds.length; i++) {
            View v = findViewById(navIds[i]);
            if (v != null) v.setAlpha(i == tab ? 1f : 0.4f);
        }
        switch (tab) {
            case TAB_CENTER:   loadCenter();   break;
            case TAB_CREATE:   loadCreate();   break;
            case TAB_TELEGRAM: loadTelegram(); break;
            case TAB_HISTORY:  loadHistory();  break;
            case TAB_SETTINGS: loadSettings(); break;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CENTER
    // ═══════════════════════════════════════════════════════════════════════
    private void loadCenter() {
        String lastDate = bp.getString(KEY_LAST_DATE, null);
        if (lastDate != null) {
            tvLastDate.setText(" " + lastDate);
            tvLastMethod.setText(" " + bp.getString(KEY_LAST_METHOD, "লোকাল") +
                " · " + bp.getString(KEY_LAST_FORMAT, "JSON").toUpperCase());
            tvLastSize.setText(" " + bp.getString(KEY_LAST_SIZE, "--"));
            tvLastStatus.setText(" সফল");
            tvLastStatus.setTextColor(getColor(R.color.successColor));
        } else {
            tvLastDate.setText("কোনো ব্যাকআপ নেই");
            tvLastMethod.setText("—");
            tvLastSize.setText("—");
            tvLastStatus.setText(" নেই");
            tvLastStatus.setTextColor(getColor(R.color.warningColor));
        }

        int count = bp.getInt(KEY_BACKUP_COUNT, 0);
        tvStatTotal.setText("মোট " + count + "টি ব্যাকআপ");
        tvStatIncome.setText(DatabaseManager.formatAmount(db.getTotalIncome()));
        tvStatExpense.setText(DatabaseManager.formatAmount(db.getTotalExpense()));
        tvStatLedger.setText(DatabaseManager.formatAmount(db.getTotalDena() + db.getTotalPabona()));
        tvStatSavings.setText(DatabaseManager.formatAmount(db.getTotalSavings()));

        boolean auto = bp.getBoolean(KEY_AUTO_BACKUP, false);
        switchAuto.setOnCheckedChangeListener(null);
        switchAuto.setChecked(auto);
        switchAuto.setOnCheckedChangeListener((b, c) -> {
            bp.edit().putBoolean(KEY_AUTO_BACKUP, c).apply();
            tvAutoStatus.setText(c ? " অটো ব্যাকআপ চালু" : " অটো ব্যাকআপ বন্ধ");
            tvAutoStatus.setTextColor(getColor(c ? R.color.successColor : R.color.textSecondary));
        });
        tvAutoStatus.setText(auto ? " অটো ব্যাকআপ চালু" : " অটো ব্যাকআপ বন্ধ");
        tvAutoStatus.setTextColor(getColor(auto ? R.color.successColor : R.color.textSecondary));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CREATE TAB
    // ═══════════════════════════════════════════════════════════════════════
    private void loadCreate() {
        tvIncAmt.setText(DatabaseManager.formatAmount(db.getTotalIncome()));
        tvExpAmt.setText(DatabaseManager.formatAmount(db.getTotalExpense()));
        tvDebtAmt.setText(DatabaseManager.formatAmount(db.getTotalDena()));
        tvRecAmt.setText(DatabaseManager.formatAmount(db.getTotalPabona()));
        double total = db.getTotalIncome() + db.getTotalExpense()
                     + db.getTotalDena()   + db.getTotalPabona() + db.getTotalSavings();
        tvAmt.setText(DatabaseManager.formatAmount(total));
        updateCreateUI();
    }

    private void selectData(String d)   { selData = d;   updateCreateUI(); }
    private void selectFmt(String f)    { selFmt  = f;   updateCreateUI(); }
    private void selectMethod(String m) { selMethod = m; updateCreateUI(); }

    private void updateCreateUI() {
        // Data cards — each carries its own deep "selected" solid color so text/icons
        // (always white) stay legible, and a neutral light "unselected" state.
        setCardSelDeep(cdAll,        "all".equals(selData),        R.color.bkSolidPrimary);
        setCardSelDeep(cdIncome,     "income".equals(selData),     R.color.bkSolidIncome);
        setCardSelDeep(cdExpense,    "expense".equals(selData),    R.color.bkSolidExpense);
        setCardSelDeep(cdDebt,       "debt".equals(selData),       R.color.bkSolidDebt);
        setCardSelDeep(cdReceivable, "receivable".equals(selData), R.color.bkSolidReceivable);

        // Format cards
        setCardSelDeep(cfJson, "json".equals(selFmt),  R.color.bkSolidJson);
        setCardSelDeep(cfPdf,  "pdf".equals(selFmt),   R.color.bkSolidPdf);
        setCardSelDeep(cfDocx, "docx".equals(selFmt),  R.color.bkSolidDocx);
        setCardSelDeep(cfXlsx, "xlsx".equals(selFmt),  R.color.bkSolidXlsx);

        // Method cards
        setCardSelDeep(cmLocal,    "local".equals(selMethod),    R.color.bkSolidLocal);
        setCardSelDeep(cmTelegram, "telegram".equals(selMethod), R.color.bkSolidTelegram);
        setCardSelDeep(cmGDrive,   "gdrive".equals(selMethod),   R.color.bkSolidGDrive);
    }

    /**
     * Premium, contrast-safe card selection.
     * Selected  -> deep solid brand color + raised elevation + glowing primary stroke + white text/icons.
     * Unselected-> neutral white surface + soft border + dark text/icons (always legible).
     * Also applies a subtle press-scale (98%) touch animation for tactile, "clickable" feedback.
     */
    private void setCardSelDeep(MaterialCardView c, boolean sel, int deepColorRes) {
        if (c == null) return;
        int deepColor    = ContextCompat.getColor(this, deepColorRes);
        int unselBg      = ContextCompat.getColor(this, R.color.bkUnselectedBg);
        int textOnSolid  = ContextCompat.getColor(this, R.color.bkTextOnSolid);
        int textNeutral  = ContextCompat.getColor(this, R.color.bkTextPrimaryDeep);
        int iconNeutral  = ContextCompat.getColor(this, R.color.bkUnselectedIcon);
        int glowBorder   = ContextCompat.getColor(this, R.color.bkSelectedBorder);
        int softBorder   = ContextCompat.getColor(this, R.color.bkUnselectedBorder);

        c.setCardBackgroundColor(sel ? deepColor : unselBg);
        c.setCardElevation(sel ? dpToPx(10) : dpToPx(2));
        c.setRadius(dpToPx(18));
        c.setStrokeColor(sel ? glowBorder : softBorder);
        c.setStrokeWidth(sel ? dpToPx(2) : dpToPx(1));
        c.setForeground(ContextCompat.getDrawable(this, R.drawable.ripple_selector_card));

        recolorCardContents(c, sel, textOnSolid, textNeutral, iconNeutral);
        attachPressAnimation(c);
    }

    /** Walks every child of the card and recolors TextViews/ImageViews so contrast is always safe. */
    private void recolorCardContents(MaterialCardView card, boolean selected, int textOnSolid, int textNeutral, int iconNeutral) {
        View inner = card.getChildAt(0);
        if (!(inner instanceof ViewGroup)) return;
        recolorViewTree((ViewGroup) inner, selected, textOnSolid, textNeutral, iconNeutral);
    }

    private void recolorViewTree(ViewGroup group, boolean selected, int textOnSolid, int textNeutral, int iconNeutral) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View ch = group.getChildAt(i);
            if (ch instanceof TextView) {
                ((TextView) ch).setTextColor(selected ? textOnSolid : textNeutral);
            } else if (ch instanceof ImageView) {
                ((ImageView) ch).setColorFilter(selected ? textOnSolid : iconNeutral);
            } else if (ch instanceof ViewGroup) {
                recolorViewTree((ViewGroup) ch, selected, textOnSolid, textNeutral, iconNeutral);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  START BACKUP — ALL FORMATS
    // ═══════════════════════════════════════════════════════════════════════
    private void startBackup() {
        switch (selFmt) {
            case "json": doJsonBackup(); break;
            case "pdf":  doPdfBackup();  break;
            case "docx": doDocxBackup(); break;
            case "xlsx": doXlsxBackup(); break;
        }
    }

    // ── JSON ──────────────────────────────────────────────────────────────
    private void doJsonBackup() {
        String json = db.exportToJson();
        if (json == null) { toast(" ডেটা পাওয়া যায়নি"); return; }
        String fname = genFileName("json");
        deliverBackup(json.getBytes(), fname, "application/json", "json");
    }

    // ── PDF ───────────────────────────────────────────────────────────────
    private void doPdfBackup() {
        ProgressDialog pd = showProgress(" PDF তৈরি হচ্ছে...");
        new Thread(() -> {
            try {
                byte[] pdfBytes = generatePdf();
                runOnUiThread(() -> {
                    pd.dismiss();
                    if (pdfBytes != null) {
                        String fname = genFileName("pdf");
                        deliverBackup(pdfBytes, fname, "application/pdf", "pdf");
                    } else toast(" PDF তৈরি ব্যর্থ");
                });
            } catch (Exception e) {
                runOnUiThread(() -> { pd.dismiss(); toast(" PDF error: " + e.getMessage()); });
            }
        }).start();
    }

    private byte[] generatePdf() {
        try {
            PdfDocument doc = new PdfDocument();
            Paint paint = new Paint();
            Paint titlePaint = new Paint();
            titlePaint.setTypeface(Typeface.DEFAULT_BOLD);
            titlePaint.setTextSize(18f);
            titlePaint.setColor(ContextCompat.getColor(this, R.color.colorPrimary));

            Paint headerPaint = new Paint();
            headerPaint.setTypeface(Typeface.DEFAULT_BOLD);
            headerPaint.setTextSize(13f);
            headerPaint.setColor(ContextCompat.getColor(this, R.color.textPrimary));

            paint.setTextSize(12f);
            paint.setColor(ContextCompat.getColor(this, R.color.htmlHeading));

            int pageW = 595, pageH = 842;
            int y = 60, margin = 40;

            PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(pageW, pageH, 1).create();
            PdfDocument.Page page = doc.startPage(info);
            Canvas cv = page.getCanvas();

            // Title
            titlePaint.setTextSize(22f);
            cv.drawText("CashLipi ক্যাশলিপি — Backup Report", margin, y, titlePaint);
            y += 30;
            paint.setColor(ContextCompat.getColor(this, R.color.textSecondary));
            cv.drawText("Date: " + new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(new Date()), margin, y, paint);
            y += 30;

            // Line
            Paint linePaint = new Paint();
            linePaint.setColor(ContextCompat.getColor(this, R.color.borderColor));
            linePaint.setStrokeWidth(1f);
            cv.drawLine(margin, y, pageW - margin, y, linePaint);
            y += 20;

            // Summary section
            titlePaint.setTextSize(16f);
            titlePaint.setColor(ContextCompat.getColor(this, R.color.textPrimary));
            cv.drawText("Financial Summary", margin, y, titlePaint);
            y += 20;

            String[][] rows = {
                {" মোট আয়",   DatabaseManager.formatAmount(db.getTotalIncome())},
                {" মোট ব্যয়", DatabaseManager.formatAmount(db.getTotalExpense())},
                {" মোট দেনা", DatabaseManager.formatAmount(db.getTotalDena())},
                {" মোট পাওনা",DatabaseManager.formatAmount(db.getTotalPabona())},
                {" মোট সঞ্চয়",DatabaseManager.formatAmount(db.getTotalSavings())},
                {" নেট ব্যালেন্স", DatabaseManager.formatAmount(
                    db.getTotalIncome() - db.getTotalExpense())},
            };

            for (String[] row : rows) {
                paint.setColor(ContextCompat.getColor(this, R.color.htmlHeading));
                cv.drawText(row[0], margin + 10, y, paint);
                Paint vPaint = new Paint(paint);
                vPaint.setTypeface(Typeface.DEFAULT_BOLD);
                vPaint.setColor(ContextCompat.getColor(this, R.color.colorPrimary));
                cv.drawText(row[1], pageW - margin - 120, y, vPaint);
                y += 22;
            }

            y += 10;
            cv.drawLine(margin, y, pageW - margin, y, linePaint);
            y += 20;

            // Income list
            if (!"expense".equals(selData) && !"debt".equals(selData) && !"receivable".equals(selData)) {
                titlePaint.setTextSize(14f);
                cv.drawText("Income Entries", margin, y, titlePaint);
                y += 18;
                paint.setColor(ContextCompat.getColor(this, R.color.textSecondary));
                paint.setTextSize(10f);
                cv.drawText("Date", margin, y, paint);
                cv.drawText("Description", margin + 80, y, paint);
                cv.drawText("Amount", pageW - margin - 80, y, paint);
                y += 4;
                cv.drawLine(margin, y, pageW - margin, y, linePaint);
                y += 14;
                paint.setTextSize(11f);
                paint.setColor(ContextCompat.getColor(this, R.color.htmlHeading));

                List<Transaction> incList = db.getIncomeList();
                int shown = 0;
                for (Transaction t : incList) {
                    if (y > pageH - 80) break;
                    cv.drawText(t.getDate() != null ? t.getDate() : "", margin, y, paint);
                    String note = t.getNote() != null ? t.getNote() : t.getCategory();
                    if (note != null && note.length() > 28) note = note.substring(0, 25) + "...";
                    cv.drawText(note != null ? note : "", margin + 80, y, paint);
                    Paint ap = new Paint(paint);
                    ap.setColor(ContextCompat.getColor(this, R.color.incomeDark));
                    ap.setTypeface(Typeface.DEFAULT_BOLD);
                    cv.drawText(DatabaseManager.formatAmount(t.getAmount()), pageW - margin - 80, y, ap);
                    y += 18; shown++;
                }
                if (shown == 0) { paint.setColor(androidx.core.content.ContextCompat.getColor(this, R.color.bkAutoInactiveText)); cv.drawText("কোনো আয় নেই", margin + 10, y, paint); y += 18; }
            }

            doc.finishPage(page);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.writeTo(baos);
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    // ── DOCX (HTML → .doc) ───────────────────────────────────────────────
    private void doDocxBackup() {
        ProgressDialog pd = showProgress(" Word ফাইল তৈরি হচ্ছে...");
        new Thread(() -> {
            try {
                String html = generateWordHtml();
                byte[] bytes = html.getBytes("UTF-8");
                runOnUiThread(() -> {
                    pd.dismiss();
                    String fname = genFileName("doc");
                    deliverBackup(bytes, fname, "application/msword", "docx");
                });
            } catch (Exception e) {
                runOnUiThread(() -> { pd.dismiss(); toast(" Word ফাইল ব্যর্থ: " + e.getMessage()); });
            }
        }).start();
    }

    private String generateWordHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><meta charset='UTF-8'><style>")
          .append("body{font-family:Arial;margin:30px;color:#111;}")
          .append("h1{color:#6366F1;border-bottom:2px solid #6366F1;padding-bottom:8px;}")
          .append("h2{color:#374151;margin-top:20px;}")
          .append("table{width:100%;border-collapse:collapse;margin:10px 0;}")
          .append("th{background:#6366F1;color:#fff;padding:8px;text-align:left;}")
          .append("td{border:1px solid #E5E7EB;padding:6px;}")
          .append("tr:nth-child(even){background:#F9FAFB;}")
          .append(".inc{color:#059669;font-weight:bold;}")
          .append(".exp{color:#DC2626;font-weight:bold;}")
          .append(".debt{color:#D97706;font-weight:bold;}")
          .append(".rec{color:#2563EB;font-weight:bold;}")
          .append("</style></head><body>");

        sb.append("<h1>CashLipi ক্যাশলিপি — ব্যাকআপ রিপোর্ট</h1>");
        sb.append("<p>তারিখ: ")
          .append(new SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.US).format(new Date()))
          .append("</p>");

        // Summary table
        sb.append("<h2>সারসংক্ষেপ</h2><table>")
          .append("<tr><th>বিভাগ</th><th>পরিমাণ</th></tr>")
          .append("<tr><td> মোট আয়</td><td class='inc'>")
          .append(DatabaseManager.formatAmount(db.getTotalIncome())).append("</td></tr>")
          .append("<tr><td> মোট ব্যয়</td><td class='exp'>")
          .append(DatabaseManager.formatAmount(db.getTotalExpense())).append("</td></tr>")
          .append("<tr><td> মোট দেনা</td><td class='debt'>")
          .append(DatabaseManager.formatAmount(db.getTotalDena())).append("</td></tr>")
          .append("<tr><td> মোট পাওনা</td><td class='rec'>")
          .append(DatabaseManager.formatAmount(db.getTotalPabona())).append("</td></tr>")
          .append("<tr><td> মোট সঞ্চয়</td><td>")
          .append(DatabaseManager.formatAmount(db.getTotalSavings())).append("</td></tr>")
          .append("<tr><td><b> নেট ব্যালেন্স</b></td><td><b>")
          .append(DatabaseManager.formatAmount(db.getTotalIncome() - db.getTotalExpense()))
          .append("</b></td></tr></table>");

        // Income list
        List<Transaction> incList = db.getIncomeList();
        if (!incList.isEmpty()) {
            sb.append("<h2>আয়ের তালিকা (").append(incList.size()).append("টি)</h2><table>")
              .append("<tr><th>তারিখ</th><th>ক্যাটাগরি</th><th>বিবরণ</th><th>পরিমাণ</th></tr>");
            for (Transaction t : incList) {
                sb.append("<tr><td>").append(t.getDate()).append("</td>")
                  .append("<td>").append(t.getCategory() != null ? t.getCategory() : "").append("</td>")
                  .append("<td>").append(t.getNote() != null ? t.getNote() : "").append("</td>")
                  .append("<td class='inc'>").append(DatabaseManager.formatAmount(t.getAmount())).append("</td></tr>");
            }
            sb.append("</table>");
        }

        // Expense list
        List<Transaction> expList = db.getExpenseList();
        if (!expList.isEmpty()) {
            sb.append("<h2>ব্যয়ের তালিকা (").append(expList.size()).append("টি)</h2><table>")
              .append("<tr><th>তারিখ</th><th>ক্যাটাগরি</th><th>বিবরণ</th><th>পরিমাণ</th></tr>");
            for (Transaction t : expList) {
                sb.append("<tr><td>").append(t.getDate()).append("</td>")
                  .append("<td>").append(t.getCategory() != null ? t.getCategory() : "").append("</td>")
                  .append("<td>").append(t.getNote() != null ? t.getNote() : "").append("</td>")
                  .append("<td class='exp'>").append(DatabaseManager.formatAmount(t.getAmount())).append("</td></tr>");
            }
            sb.append("</table>");
        }

        sb.append("<br><p style='color:#9CA3AF;font-size:11px;'>Generated by CashLipi ক্যাশলিপি App</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    // ── XLSX (CSV format) ─────────────────────────────────────────────────
    private void doXlsxBackup() {
        ProgressDialog pd = showProgress(" Excel ফাইল তৈরি হচ্ছে...");
        new Thread(() -> {
            try {
                String csv = generateCsv();
                byte[] bytes = csv.getBytes("UTF-8");
                runOnUiThread(() -> {
                    pd.dismiss();
                    String fname = genFileName("csv");
                    deliverBackup(bytes, fname, "text/csv", "xlsx");
                });
            } catch (Exception e) {
                runOnUiThread(() -> { pd.dismiss(); toast(" Excel ব্যর্থ: " + e.getMessage()); });
            }
        }).start();
    }

    private String generateCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("\uFEFF"); // BOM for Excel UTF-8

        sb.append("CashLipi ক্যাশলিপি Backup\n");
        sb.append("Generated,").append(new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US).format(new Date())).append("\n\n");

        // Summary
        sb.append("=== SUMMARY ===\n");
        sb.append("মোট আয়,").append(db.getTotalIncome()).append("\n");
        sb.append("মোট ব্যয়,").append(db.getTotalExpense()).append("\n");
        sb.append("মোট দেনা,").append(db.getTotalDena()).append("\n");
        sb.append("মোট পাওনা,").append(db.getTotalPabona()).append("\n");
        sb.append("মোট সঞ্চয়,").append(db.getTotalSavings()).append("\n\n");

        // Income
        sb.append("=== আয় ===\n");
        sb.append("তারিখ,ক্যাটাগরি,বিবরণ,পরিমাণ\n");
        for (Transaction t : db.getIncomeList()) {
            sb.append(csv(t.getDate())).append(",")
              .append(csv(t.getCategory())).append(",")
              .append(csv(t.getNote())).append(",")
              .append(t.getAmount()).append("\n");
        }
        sb.append("\n");

        // Expense
        sb.append("=== ব্যয় ===\n");
        sb.append("তারিখ,ক্যাটাগরি,বিবরণ,পরিমাণ\n");
        for (Transaction t : db.getExpenseList()) {
            sb.append(csv(t.getDate())).append(",")
              .append(csv(t.getCategory())).append(",")
              .append(csv(t.getNote())).append(",")
              .append(t.getAmount()).append("\n");
        }
        sb.append("\n");

        // Ledger
        sb.append("=== দেনা/পাওনা ===\n");
        sb.append("তারিখ,নাম,ধরন,বিবরণ,পরিমাণ\n");
        for (LedgerEntry e : db.getLedgerList()) {
            sb.append(csv(e.getDate())).append(",")
              .append(csv(e.getPerson())).append(",")
              .append("dena".equals(e.getType()) ? "দেনা" : "পাওনা").append(",")
              .append(csv(e.getNote())).append(",")
              .append(e.getAmount()).append("\n");
        }

        return sb.toString();
    }

    private String csv(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n"))
            return "\"" + v.replace("\"", "\"\"") + "\"";
        return v;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  DELIVER BACKUP — Local / Telegram / GDrive
    // ═══════════════════════════════════════════════════════════════════════
    private void deliverBackup(byte[] data, String fname, String mime, String fmt) {
        switch (selMethod) {
            case "telegram": deliverTelegram(data, fname, fmt); break;
            case "gdrive":   deliverGDrive(data, fname, mime, fmt); break;
            default:         deliverLocal(data, fname, mime, fmt); break;
        }
    }

    // ── Local: auto-save to Downloads/CashLipi/ ────────────────────────
    private void deliverLocal(byte[] data, String fname, String mime, String fmt) {
        ProgressDialog pd = showProgress(" সেভ হচ্ছে...");
        new Thread(() -> {
            boolean ok = false;
            String savedPath = "";
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // MediaStore for Android 10+
                    ContentValues cv = new ContentValues();
                    cv.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fname);
                    cv.put(android.provider.MediaStore.Downloads.MIME_TYPE, mime);
                    cv.put(android.provider.MediaStore.Downloads.RELATIVE_PATH,
                        android.os.Environment.DIRECTORY_DOWNLOADS + "/CashLipi");
                    Uri uri = getContentResolver().insert(
                        android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                    if (uri != null) {
                        try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                            os.write(data);
                        }
                        ok = true;
                        savedPath = "Downloads/CashLipi/" + fname;
                    }
                } else {
                    // Direct file for older Android
                    File dir = new File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        "CashLipi");
                    dir.mkdirs();
                    File f = new File(dir, fname);
                    try (FileOutputStream fos = new FileOutputStream(f)) {
                        fos.write(data);
                    }
                    ok = true;
                    savedPath = f.getAbsolutePath();
                }
            } catch (Exception e) {
                // Fallback: save to app cache and share
                savedPath = "cache";
                ok = saveToCache(data, fname);
            }

            final boolean success = ok;
            final String path = savedPath;
            final long size = data.length;
            final String fmtFinal = fmt;

            runOnUiThread(() -> {
                pd.dismiss();
                if (success) {
                    recordBackup("লোকাল স্টোরেজ", fmtFinal, size);
                    new AlertDialog.Builder(this)
                        .setTitle(" ব্যাকআপ সফল!")
                        .setMessage(" সেভ হয়েছে:\n" + path + "\n\nআকার: " + formatSize(size))
                        .setPositiveButton("OK", null)
                        .show();
                    showTab(TAB_CENTER);
                } else {
                    // Last resort: file picker
                    pendingContent = new String(data);
                    pendingFileName = fname;
                    pendingMime = mime;
                    createFileLauncher.launch(fname);
                }
            });
        }).start();
    }

    private boolean saveToCache(byte[] data, String fname) {
        try {
            File dir = new File(getCacheDir(), "backups");
            dir.mkdirs();
            File f = new File(dir, fname);
            try (FileOutputStream fos = new FileOutputStream(f)) {
                fos.write(data);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void saveContentToUri(Uri uri, String content, String fname) {
        try (OutputStream os = getContentResolver().openOutputStream(uri)) {
            os.write(content.getBytes("UTF-8"));
            recordBackup("লোকাল স্টোরেজ", selFmt, content.getBytes().length);
            toast(" ব্যাকআপ সেভ হয়েছে!");
            showTab(TAB_CENTER);
        } catch (Exception e) {
            toast(" সেভ ব্যর্থ: " + e.getMessage());
        }
    }

    // ── Telegram ──────────────────────────────────────────────────────────
    private void deliverTelegram(byte[] data, String fname, String fmt) {
        String token = bp.getString(KEY_BOT_TOKEN, "");
        String chat  = bp.getString(KEY_CHAT_ID, "");
        if (token.isEmpty() || chat.isEmpty()) {
            new AlertDialog.Builder(this)
                .setTitle(" Telegram সেটআপ প্রয়োজন")
                .setMessage("Bot Token ও Chat ID সেট করুন।")
                .setPositiveButton("এখনই সেটআপ", (d, w) -> showTab(TAB_TELEGRAM))
                .setNegativeButton("বাতিল", null).show();
            return;
        }
        ProgressDialog pd = showProgress(" Telegram-এ পাঠানো হচ্ছে...");
        new Thread(() -> {
            boolean ok = sendTelegramFile(token, chat, data, fname, fmt.toUpperCase() + " Backup");
            runOnUiThread(() -> {
                pd.dismiss();
                if (ok) {
                    recordBackup("Telegram", fmt, data.length);
                    toast(" Telegram-এ পাঠানো হয়েছে!");
                    showTab(TAB_CENTER);
                } else {
                    toast(" API ব্যর্থ — Share দিয়ে পাঠাচ্ছি...");
                    shareFileFallback(data, fname, getMimeForFmt(fmt));
                }
            });
        }).start();
    }

    // ── Google Drive ──────────────────────────────────────────────────────
    private void deliverGDrive(byte[] data, String fname, String mime, String fmt) {
        ProgressDialog pd = showProgress(" শেয়ার করা হচ্ছে...");
        new Thread(() -> {
            boolean cached = saveToCache(data, fname);
            runOnUiThread(() -> {
                pd.dismiss();
                shareFileFallback(data, fname, mime);
                recordBackup("Google Drive", fmt, data.length);
            });
        }).start();
    }

    private void shareFileFallback(byte[] data, String fname, String mime) {
        try {
            File dir = new File(getCacheDir(), "backups"); dir.mkdirs();
            File f = new File(dir, fname);
            try (FileOutputStream fos = new FileOutputStream(f)) { fos.write(data); }
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", f);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(mime);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "ব্যাকআপ পাঠান"));
        } catch (Exception e) {
            toast(" শেয়ার ব্যর্থ: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  RESTORE
    // ═══════════════════════════════════════════════════════════════════════
    private void processRestoreFile(Uri uri) {
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            byte[] buf = new byte[is.available()]; is.read(buf);
            String content = new String(buf, "UTF-8");

            // Parse preview
            String preview = buildRestorePreview(content);
            new AlertDialog.Builder(this)
                .setTitle(" Restore Backup?")
                .setMessage(preview)
                .setPositiveButton(" রিস্টোর করুন", (d, w) -> {
                    if (db.importFromJson(content)) {
                        toast(" রিস্টোর সফল হয়েছে!");
                        showTab(TAB_CENTER);
                    } else toast(" রিস্টোর ব্যর্থ — JSON ফাইল চেক করুন");
                })
                .setNegativeButton("বাতিল", null).show();
        } catch (Exception e) {
            toast(" ফাইল পড়তে ব্যর্থ: " + e.getMessage());
        }
    }

    private String buildRestorePreview(String content) {
        try {
            JsonObject obj = new Gson().fromJson(content, JsonObject.class);
            int inc = obj.has("income")  ? obj.getAsJsonArray("income").size()  : 0;
            int exp = obj.has("expense") ? obj.getAsJsonArray("expense").size() : 0;
            int led = obj.has("ledger")  ? obj.getAsJsonArray("ledger").size()  : 0;
            int sav = obj.has("savings") ? obj.getAsJsonArray("savings").size() : 0;
            return " ব্যাকআপ ডেটা:\n আয়: " + inc + " টি\n ব্যয়: " + exp
                + " টি\n দেনা/পাওনা: " + led + " টি\n সঞ্চয়: " + sav
                + " টি\n\n বর্তমান ডেটা মুছে এই ডেটা রিস্টোর হবে।";
        } catch (Exception e) {
            return "ব্যাকআপ রিস্টোর করতে চান? বর্তমান ডেটা প্রতিস্থাপিত হবে।";
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TELEGRAM
    // ═══════════════════════════════════════════════════════════════════════
    private void loadTelegram() {
        etToken.setText(bp.getString(KEY_BOT_TOKEN, ""));
        etChat.setText(bp.getString(KEY_CHAT_ID, ""));
        swTg.setChecked(bp.getBoolean(KEY_TG_ENABLED, false));
        tvTgStatus.setVisibility(View.GONE);
    }

    private void testTelegram() {
        String token = etToken.getText().toString().trim();
        if (token.isEmpty()) { toast(" Bot Token দিন"); return; }
        ProgressDialog pd = showProgress(" সংযোগ পরীক্ষা...");
        new Thread(() -> {
            boolean ok = false;
            try {
                HttpURLConnection c = (HttpURLConnection)
                    new URL("https://api.telegram.org/bot" + token + "/getMe").openConnection();
                c.setConnectTimeout(8000); c.setReadTimeout(8000);
                ok = c.getResponseCode() == 200; c.disconnect();
            } catch (Exception ignored) {}
            boolean res = ok;
            runOnUiThread(() -> {
                pd.dismiss();
                tvTgStatus.setVisibility(View.VISIBLE);
                tvTgStatus.setText(res ? " সংযোগ সফল! Bot কাজ করছে।" : " সংযোগ ব্যর্থ। Token চেক করুন।");
                tvTgStatus.setTextColor(getColor(res ? R.color.successColor : R.color.errorColor));
            });
        }).start();
    }

    private void saveTelegram() {
        String token = etToken.getText().toString().trim();
        String chat  = etChat.getText().toString().trim();
        if (swTg.isChecked() && (token.isEmpty() || chat.isEmpty())) {
            toast(" Token ও Chat ID দিন"); return;
        }
        bp.edit().putString(KEY_BOT_TOKEN, token).putString(KEY_CHAT_ID, chat)
            .putBoolean(KEY_TG_ENABLED, swTg.isChecked()).apply();
        toast(" Telegram সেটিংস সেভ হয়েছে!");
    }

    private void sendNowTelegram() {
        String token = bp.getString(KEY_BOT_TOKEN, "");
        String chat  = bp.getString(KEY_CHAT_ID, "");
        if (token.isEmpty() || chat.isEmpty()) { toast(" আগে Token ও Chat ID সেভ করুন"); return; }
        String json  = db.exportToJson();
        if (json == null) { toast(" ডেটা পাওয়া যায়নি"); return; }
        String fname = genFileName("json");
        ProgressDialog pd = showProgress(" পাঠানো হচ্ছে...");
        new Thread(() -> {
            boolean ok = sendTelegramFile(token, chat, json.getBytes(), fname, "JSON Backup");
            runOnUiThread(() -> {
                pd.dismiss();
                if (ok) { recordBackup("Telegram", "json", json.getBytes().length); toast(" পাঠানো হয়েছে!"); }
                else { toast(" ব্যর্থ — Share দিয়ে পাঠাচ্ছি..."); shareFileFallback(json.getBytes(), fname, "application/json"); }
            });
        }).start();
    }

    private boolean sendTelegramFile(String token, String chat, byte[] data, String fname, String caption) {
        try {
            String boundary = "CashLipi" + System.currentTimeMillis();
            HttpURLConnection c = (HttpURLConnection)
                new URL("https://api.telegram.org/bot" + token + "/sendDocument").openConnection();
            c.setConnectTimeout(20000); c.setReadTimeout(20000);
            c.setRequestMethod("POST"); c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            OutputStream os = c.getOutputStream();
            os.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"chat_id\"\r\n\r\n" + chat + "\r\n").getBytes("UTF-8"));
            os.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"caption\"\r\n\r\n CashLipi ক্যাশলিপি " + caption + "\n " + new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.US).format(new Date()) + "\r\n").getBytes("UTF-8"));
            os.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"document\"; filename=\"" + fname + "\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes("UTF-8"));
            os.write(data);
            os.write(("\r\n--" + boundary + "--\r\n").getBytes("UTF-8"));
            os.flush();
            int code = c.getResponseCode(); c.disconnect();
            return code == 200;
        } catch (Exception e) { return false; }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HISTORY
    // ═══════════════════════════════════════════════════════════════════════
    private void loadHistory() {
        LinearLayout container = findViewById(R.id.historyContainer);
        if (container == null) return;
        container.removeAllViews();
        String histJson = bp.getString(KEY_HISTORY, "[]");
        try {
            JsonArray arr = new Gson().fromJson(histJson, JsonArray.class);
            if (arr.size() == 0) { addEmptyHistory(container); return; }
            for (int i = arr.size() - 1; i >= 0; i--) {
                JsonObject item = arr.get(i).getAsJsonObject();
                addHistoryCard(container, item);
            }
        } catch (Exception e) { addEmptyHistory(container); }
    }

    private void addEmptyHistory(LinearLayout c) {
        TextView tv = new TextView(this);
        tv.setText(" কোনো ব্যাকআপ ইতিহাস নেই");
        tv.setTextColor(getColor(R.color.textSecondary));
        tv.setTextSize(14f); tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dpToPx(48), 0, dpToPx(48));
        c.addView(tv);
    }

    private void addHistoryCard(LinearLayout container, JsonObject item) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dpToPx(10));
        card.setLayoutParams(lp); card.setRadius(dpToPx(14)); card.setCardElevation(dpToPx(3));
        card.setCardBackgroundColor(getColor(R.color.cardBg));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
        row.setGravity(Gravity.CENTER_VERTICAL);

        // Method icon circle
        String method = item.has("method") ? item.get("method").getAsString() : "local";
        String fmt    = item.has("format") ? item.get("format").getAsString() : "json";
        String icon   = "Telegram".equals(method) ? "" : "Drive".contains(method) ? "" : "";
        int iconColor = "Telegram".equals(method) ? androidx.core.content.ContextCompat.getColor(this, R.color.bkMethodTelegramSel) : "Drive".contains(method) ? androidx.core.content.ContextCompat.getColor(this, R.color.bkMethodGDriveSel) : androidx.core.content.ContextCompat.getColor(this, R.color.bkMethodOtherSel);

        LinearLayout iconBg = new LinearLayout(this);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dpToPx(40), dpToPx(40));
        iconLp.setMarginEnd(dpToPx(12));
        iconBg.setLayoutParams(iconLp);
        iconBg.setGravity(Gravity.CENTER);
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL); gd.setColor(iconColor | 0x20000000);
        iconBg.setBackground(gd);
        TextView iconTv = new TextView(this);
        iconTv.setText(icon); iconTv.setTextSize(18f);
        iconBg.addView(iconTv);
        row.addView(iconBg);

        // Info
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView tvDate = new TextView(this);
        tvDate.setText(item.has("date") ? item.get("date").getAsString() : "--");
        tvDate.setTextColor(getColor(R.color.textPrimary));
        tvDate.setTextSize(13f); tvDate.setTypeface(null, Typeface.BOLD);
        TextView tvMeta = new TextView(this);
        tvMeta.setText(method + " · " + fmt.toUpperCase() + " · " + (item.has("size") ? item.get("size").getAsString() : "--"));
        tvMeta.setTextColor(getColor(R.color.textSecondary)); tvMeta.setTextSize(11f);
        info.addView(tvDate); info.addView(tvMeta);
        row.addView(info);

        // Status
        TextView status = new TextView(this);
        status.setText(""); status.setTextSize(16f);
        row.addView(status);
        card.addView(row); container.addView(card);
    }

    private void clearHistory() {
        new AlertDialog.Builder(this)
            .setTitle("ইতিহাস মুছুন")
            .setMessage("সব ব্যাকআপ ইতিহাস মুছে ফেলবেন?")
            .setPositiveButton("মুছুন", (d, w) -> {
                bp.edit().remove(KEY_HISTORY).putInt(KEY_BACKUP_COUNT, 0).apply();
                loadHistory(); toast(" ইতিহাস মুছে ফেলা হয়েছে");
            })
            .setNegativeButton("বাতিল", null).show();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SETTINGS
    // ═══════════════════════════════════════════════════════════════════════
    private void loadSettings() {
        set(swTrigInc,  bp.getBoolean("trig_income", true));
        set(swTrigExp,  bp.getBoolean("trig_expense", true));
        set(swTrigDebt, bp.getBoolean("trig_debt", true));
        set(swTrigRec,  bp.getBoolean("trig_receivable", true));
        set(swTrigUpd,  bp.getBoolean("trig_update", true));
        set(swTrigDel,  bp.getBoolean("trig_delete", true));
        set(swMTg,      bp.getBoolean(KEY_TG_ENABLED, false));
        set(swMGDrive,  bp.getBoolean("gdrive_enabled", false));
        set(swMLocal,   bp.getBoolean("local_enabled", true));
        String freq = bp.getString("backup_freq", "realtime");
        if (rgFreq != null) {
            int id = R.id.rbRealtime;
            if ("daily".equals(freq))        id = R.id.rbDaily;
            else if ("weekly".equals(freq))  id = R.id.rbWeekly;
            else if ("monthly".equals(freq)) id = R.id.rbMonthly;
            rgFreq.check(id);
        }
    }

    private void set(Switch sw, boolean v) { if (sw != null) sw.setChecked(v); }

    private void saveSettings() {
        String freq = "realtime";
        if (rgFreq != null) {
            int c = rgFreq.getCheckedRadioButtonId();
            if (c == R.id.rbDaily)   freq = "daily";
            else if (c == R.id.rbWeekly)  freq = "weekly";
            else if (c == R.id.rbMonthly) freq = "monthly";
        }
        bp.edit()
            .putBoolean("trig_income",     val(swTrigInc))
            .putBoolean("trig_expense",    val(swTrigExp))
            .putBoolean("trig_debt",       val(swTrigDebt))
            .putBoolean("trig_receivable", val(swTrigRec))
            .putBoolean("trig_update",     val(swTrigUpd))
            .putBoolean("trig_delete",     val(swTrigDel))
            .putBoolean(KEY_TG_ENABLED,    val(swMTg))
            .putBoolean("gdrive_enabled",  val(swMGDrive))
            .putBoolean("local_enabled",   val(swMLocal))
            .putString("backup_freq",      freq)
            .apply();
        toast(" সেটিংস সেভ হয়েছে!");
    }

    private boolean val(Switch sw) { return sw != null && sw.isChecked(); }

    private void resetSettings() {
        new AlertDialog.Builder(this)
            .setTitle("রিসেট করবেন?").setMessage("ডিফল্ট সেটিংসে ফিরবে।")
            .setPositiveButton("হ্যাঁ", (d, w) -> {
                bp.edit().putBoolean("trig_income", true).putBoolean("trig_expense", true)
                    .putBoolean("trig_debt", true).putBoolean("trig_receivable", true)
                    .putBoolean("trig_update", true).putBoolean("trig_delete", true)
                    .putBoolean("local_enabled", true).putString("backup_freq", "realtime")
                    .apply();
                loadSettings(); toast(" ডিফল্টে ফিরে এসেছে");
            }).setNegativeButton("বাতিল", null).show();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════════════
    private void recordBackup(String method, String fmt, long size) {
        String date = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(new Date());
        int count   = bp.getInt(KEY_BACKUP_COUNT, 0) + 1;
        String histJson = bp.getString(KEY_HISTORY, "[]");
        try {
            JsonArray arr = new Gson().fromJson(histJson, JsonArray.class);
            JsonObject e  = new JsonObject();
            e.addProperty("date", date);
            e.addProperty("method", method);
            e.addProperty("format", fmt);
            e.addProperty("size", formatSize(size));
            arr.add(e);
            while (arr.size() > 50) arr.remove(0);
            histJson = arr.toString();
        } catch (Exception ignored) {}
        bp.edit().putString(KEY_LAST_DATE, date).putString(KEY_LAST_METHOD, method)
            .putString(KEY_LAST_SIZE, formatSize(size)).putString(KEY_LAST_FORMAT, fmt)
            .putInt(KEY_BACKUP_COUNT, count).putString(KEY_HISTORY, histJson).apply();
    }

    private String genFileName(String ext) {
        return "CashLipi_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + "." + ext;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return String.format(Locale.US, "%.1f MB", bytes / 1048576.0);
    }

    private String getMimeForFmt(String fmt) {
        switch (fmt) {
            case "pdf":  return "application/pdf";
            case "docx": return "application/msword";
            case "xlsx": case "csv": return "text/csv";
            default:     return "application/json";
        }
    }

    private ProgressDialog showProgress(String msg) {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage(msg); pd.setCancelable(false); pd.show();
        return pd;
    }

    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_LONG).show(); }
    private int dpToPx(int dp) { return (int)(dp * getResources().getDisplayMetrics().density); }

    // ═══════════════════════════════════════════════════════════════════════
    //  PUBLIC: Auto Backup trigger
    // ═══════════════════════════════════════════════════════════════════════
    public static void triggerAutoBackup(Context ctx, String trigger) {
        SharedPreferences bp = ctx.getSharedPreferences(PREF_BACKUP, Context.MODE_PRIVATE);
        if (!bp.getBoolean(KEY_AUTO_BACKUP, false)) return;
        if (!bp.getBoolean("trig_" + trigger, true)) return;
        DatabaseManager db = DatabaseManager.getInstance(ctx);
        String json = db.exportToJson();
        if (json == null) return;
        String fname = "CashLipi_AUTO_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".json";
        byte[] data  = json.getBytes();

        // Save to Downloads/CashLipi/ automatically
        new Thread(() -> {
            boolean ok = false;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues cv = new ContentValues();
                    cv.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fname);
                    cv.put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/json");
                    cv.put(android.provider.MediaStore.Downloads.RELATIVE_PATH,
                        android.os.Environment.DIRECTORY_DOWNLOADS + "/CashLipi");
                    Uri uri = ctx.getContentResolver().insert(
                        android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                    if (uri != null) {
                        try (OutputStream os = ctx.getContentResolver().openOutputStream(uri)) {
                            os.write(data); ok = true;
                        }
                    }
                } else {
                    File dir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), "CashLipi");
                    dir.mkdirs();
                    File f = new File(dir, fname);
                    try (FileOutputStream fos = new FileOutputStream(f)) { fos.write(data); ok = true; }
                }
            } catch (Exception ignored) {}

            // Save metadata
            String date = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(new Date());
            int count = bp.getInt(KEY_BACKUP_COUNT, 0) + 1;
            String histJson = bp.getString(KEY_HISTORY, "[]");
            try {
                JsonArray arr = new Gson().fromJson(histJson, JsonArray.class);
                JsonObject e  = new JsonObject();
                e.addProperty("date", date); e.addProperty("method", "Auto লোকাল");
                e.addProperty("format", "json"); e.addProperty("size", (data.length / 1024) + " KB");
                arr.add(e); if (arr.size() > 50) arr.remove(0); histJson = arr.toString();
            } catch (Exception ignored) {}

            bp.edit().putString(KEY_LAST_DATE, date).putString(KEY_LAST_METHOD, "Auto লোকাল")
                .putString(KEY_LAST_SIZE, (data.length / 1024) + " KB").putString(KEY_LAST_FORMAT, "json")
                .putInt(KEY_BACKUP_COUNT, count).putString(KEY_HISTORY, histJson).apply();

            // Also Telegram if enabled
            if (bp.getBoolean(KEY_TG_ENABLED, false)) {
                String token = bp.getString(KEY_BOT_TOKEN, "");
                String chat  = bp.getString(KEY_CHAT_ID, "");
                if (!token.isEmpty() && !chat.isEmpty()) {
                    try {
                        String boundary = "AUTO" + System.currentTimeMillis();
                        HttpURLConnection c = (HttpURLConnection)
                            new URL("https://api.telegram.org/bot" + token + "/sendDocument").openConnection();
                        c.setConnectTimeout(15000); c.setReadTimeout(15000);
                        c.setRequestMethod("POST"); c.setDoOutput(true);
                        c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                        OutputStream os = c.getOutputStream();
                        os.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"chat_id\"\r\n\r\n" + chat + "\r\n").getBytes("UTF-8"));
                        os.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"caption\"\r\n\r\n Auto Backup — " + fname + "\r\n").getBytes("UTF-8"));
                        os.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"document\"; filename=\"" + fname + "\"\r\nContent-Type: application/json\r\n\r\n").getBytes("UTF-8"));
                        os.write(data);
                        os.write(("\r\n--" + boundary + "--\r\n").getBytes("UTF-8"));
                        os.flush(); c.getResponseCode(); c.disconnect();
                    } catch (Exception ignored) {}
                }
            }
        }).start();
    }
}
