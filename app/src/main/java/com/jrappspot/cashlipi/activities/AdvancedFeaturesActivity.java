package com.jrappspot.cashlipi.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.utils.DatabaseManager;
import com.jrappspot.cashlipi.utils.FontUtils;
import com.jrappspot.cashlipi.utils.LocaleHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class AdvancedFeaturesActivity extends BaseActivity {

    private DatabaseManager db;
    private Switch switchCustomKeyboard;
    private TextView tvKeyboardStatus;
    private RadioGroup rgLanguage;
    private boolean isInitializing = true;

    // Font UI
    private TextView tvCurrentFont;   // বর্তমান ফন্টের নাম দেখায়
    private TextView tvFontPreview;   // "আমার হিসাব ১২৩" preview

    // Expandable sections — unified card (header + content একসাথে)
    private ViewGroup rootContainer;

    private LinearLayout keyboardContent;
    private LinearLayout fontContent;
    private LinearLayout languageContent;

    private TextView arrowKeyboard;
    private TextView arrowFont;
    private TextView arrowLanguage;

    private boolean isKeyboardExpanded = false;
    private boolean isFontExpanded = false;
    private boolean isLanguageExpanded = false;

    private ActivityResultLauncher<Intent> fontPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_features);

        db = DatabaseManager.getInstance(this);

        // Saved font apply
        FontUtils.applyToView(this, findViewById(android.R.id.content));

        // রুট কনটেইনার — smooth expand/collapse animation এর জন্য
        rootContainer = findViewById(android.R.id.content);

        // ── File picker launcher ─────────────────────────────────────────
        fontPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) importFontFile(uri);
                }
            }
        );

        // ── Initialize expandable sections (unified card header+content) ──
        keyboardContent = findViewById(R.id.keyboardContent);
        fontContent = findViewById(R.id.fontContent);
        languageContent = findViewById(R.id.languageContent);

        arrowKeyboard = findViewById(R.id.arrowKeyboard);
        arrowFont = findViewById(R.id.arrowFont);
        arrowLanguage = findViewById(R.id.arrowLanguage);

        findViewById(R.id.headerKeyboard).setOnClickListener(v -> toggleKeyboard());
        findViewById(R.id.headerFont).setOnClickListener(v -> toggleFont());
        findViewById(R.id.headerLanguage).setOnClickListener(v -> toggleLanguage());

        // ── Custom Keyboard ──────────────────────────────────────────────
        switchCustomKeyboard = findViewById(R.id.switchCustomKeyboard);
        tvKeyboardStatus     = findViewById(R.id.tvKeyboardStatus);

        boolean isEnabled = db.isCustomKeyboardEnabled();
        switchCustomKeyboard.setChecked(isEnabled);
        updateStatusText(isEnabled);

        switchCustomKeyboard.setOnCheckedChangeListener((btn, checked) -> {
            db.setCustomKeyboardEnabled(checked);
            updateStatusText(checked);
            Toast.makeText(this,
                checked ? "🧮 Custom Keyboard ON" : "⌨️ System Keyboard ON",
                Toast.LENGTH_SHORT).show();
        });

        // ── Language ─────────────────────────────────────────────────────
        rgLanguage = findViewById(R.id.rgLanguage);
        isInitializing = true;
        selectCurrentLanguage(db.getAppLanguage());
        isInitializing = false;

        rgLanguage.setOnCheckedChangeListener((group, checkedId) -> {
            if (isInitializing) return;

            String langCode;
            if      (checkedId == R.id.rbBangla)  langCode = "bn";
            else if (checkedId == R.id.rbEnglish) langCode = "en";
            else if (checkedId == R.id.rbHindi)   langCode = "hi";
            else if (checkedId == R.id.rbArabic)  langCode = "ar";
            else return;

            if (langCode.equals(db.getAppLanguage())) return;
            Toast.makeText(this, "🌐 Changing language...", Toast.LENGTH_SHORT).show();
            LocaleHelper.setLocale(this, langCode);

            // App restart — না হলে UI change হবে না
            android.content.Intent intent = getPackageManager()
                    .getLaunchIntentForPackage(getPackageName());
            if (intent != null) {
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                        | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        // ── Font Section ─────────────────────────────────────────────────
        tvCurrentFont = findViewById(R.id.tvCurrentFont);
        tvFontPreview = findViewById(R.id.tvFontPreview);

        refreshFontUI();

        // ফন্ট বেছে নাও বাটন
        findViewById(R.id.btnPickFont).setOnClickListener(v -> openFilePicker());

        // Default font এ ফিরে যাও
        findViewById(R.id.btnResetFont).setOnClickListener(v -> {
            db.setCustomFontPath("default");
            db.setCustomFontName("");
            Toast.makeText(this, "✅ System font restored", Toast.LENGTH_SHORT).show();
            restartApp();
        });
    }

    // ── Toggle Sections (smooth animated expand/collapse) ───────────────
    private void toggleKeyboard() {
        isKeyboardExpanded = !isKeyboardExpanded;
        animateExpand(keyboardContent, arrowKeyboard, isKeyboardExpanded);
    }

    private void toggleFont() {
        isFontExpanded = !isFontExpanded;
        animateExpand(fontContent, arrowFont, isFontExpanded);
    }

    private void toggleLanguage() {
        isLanguageExpanded = !isLanguageExpanded;
        animateExpand(languageContent, arrowLanguage, isLanguageExpanded);
    }

    private void animateExpand(LinearLayout content, TextView arrow, boolean expand) {
        TransitionManager.beginDelayedTransition(rootContainer);
        content.setVisibility(expand ? View.VISIBLE : View.GONE);
        arrow.animate().rotation(expand ? 90f : 0f).setDuration(200).start();
    }

    // ── Font: file picker open ────────────────────────────────────────────────
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        // TTF / OTF উভয়ই support
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"font/ttf", "font/otf", "application/octet-stream"});
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        fontPickerLauncher.launch(Intent.createChooser(intent, "TTF/OTF ফন্ট ফাইল বেছে নিন"));
    }

    // ── Font: import করো ─────────────────────────────────────────────────────
    private void importFontFile(Uri uri) {
        try {
            // ফাইলের নাম বের করো
            String fileName = getFileNameFromUri(uri);
            if (!fileName.toLowerCase().endsWith(".ttf") && !fileName.toLowerCase().endsWith(".otf")) {
                Toast.makeText(this, "❌ শুধু .ttf বা .otf ফাইল দিন", Toast.LENGTH_SHORT).show();
                return;
            }

            // Internal storage এ copy করো
            File destDir  = FontUtils.getCustomFontDir(this);
            File destFile = new File(destDir, fileName);

            try (InputStream in  = getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(destFile)) {
                byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            }

            // Validate — ফাইল থেকে Typeface তৈরি হয় কিনা চেক করো
            Typeface test = Typeface.createFromFile(destFile);
            if (test == null) {
                destFile.delete();
                Toast.makeText(this, "❌ Valid font ফাইল না", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save path & name
            db.setCustomFontPath(destFile.getAbsolutePath());
            db.setCustomFontName(fileName);

            Toast.makeText(this,
                "✅ ফন্ট import হয়েছে: " + fileName,
                Toast.LENGTH_SHORT).show();
            restartApp();

        } catch (Exception e) {
            Toast.makeText(this, "❌ ফন্ট import ব্যর্থ: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ── Font: instantly সব screen এ apply করো ────────────────────────────────
    private void restartApp() {
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            android.content.Intent intent = new android.content.Intent(this,
                com.jrappspot.cashlipi.activities.SplashActivity.class);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }, 600);
    }

    // tf = null হলে system default restore করে
    private void applyFontNow(android.graphics.Typeface tf) {
        android.view.View root = findViewById(android.R.id.content);
        if (root instanceof android.view.ViewGroup) {
            applyTypefaceRecursive((android.view.ViewGroup) root, tf);
        }
    }

    private void applyTypefaceRecursive(android.view.ViewGroup group, android.graphics.Typeface tf) {
        for (int i = 0; i < group.getChildCount(); i++) {
            android.view.View child = group.getChildAt(i);
            if (child instanceof android.widget.TextView) {
                android.widget.TextView tv = (android.widget.TextView) child;
                if (tf != null) {
                    int style = tv.getTypeface() != null ? tv.getTypeface().getStyle() : android.graphics.Typeface.NORMAL;
                    tv.setTypeface(tf, style);
                } else {
                    // null = system theme font restore (instant)
                    tv.setTypeface(null);
                }
            } else if (child instanceof android.view.ViewGroup) {
                applyTypefaceRecursive((android.view.ViewGroup) child, tf);
            }
        }
    }

    // ── Font: UI refresh ──────────────────────────────────────────────────────
    private void refreshFontUI() {
        String fontName = db.getCustomFontName();
        String fontPath = db.getCustomFontPath();

        if (fontPath == null || fontPath.equals("default") || fontPath.isEmpty()) {
            tvCurrentFont.setText("বর্তমান ফন্ট: System Default");
            tvFontPreview.setTypeface(Typeface.DEFAULT);
        } else {
            tvCurrentFont.setText("বর্তমান ফন্ট: " + fontName);
            try {
                Typeface tf = Typeface.createFromFile(new File(fontPath));
                tvFontPreview.setTypeface(tf);
            } catch (Exception e) {
                tvCurrentFont.setText("বর্তমান ফন্ট: System Default (ফাইল খুঁজে পাওয়া যায়নি)");
            }
        }
        tvFontPreview.setText("আমার হিসাব ১২৩\nআয় ব্যয় সঞ্চয়\nCashLipi ক্যাশলিপি");
    }

    // ── URI থেকে file name বের করো ───────────────────────────────────────────
    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) result = cursor.getString(idx);
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result != null ? result : "custom_font.ttf";
    }

    // ── Language helper ───────────────────────────────────────────────────────
    private void selectCurrentLanguage(String lang) {
        if (lang == null) lang = "bn";
        switch (lang) {
            case "en": ((RadioButton) findViewById(R.id.rbEnglish)).setChecked(true); break;
            case "hi": ((RadioButton) findViewById(R.id.rbHindi)).setChecked(true);   break;
            case "ar": ((RadioButton) findViewById(R.id.rbArabic)).setChecked(true);  break;
            default:   ((RadioButton) findViewById(R.id.rbBangla)).setChecked(true);  break;
        }
    }

    // ── Keyboard status ───────────────────────────────────────────────────────
    private void updateStatusText(boolean enabled) {
        if (tvKeyboardStatus == null) return;
        tvKeyboardStatus.setText(enabled ? "ON — Calculator Keyboard" : "OFF — System Keyboard");
        tvKeyboardStatus.setTextColor(
            androidx.core.content.ContextCompat.getColor(this,
                enabled ? R.color.incomeColor : R.color.textSecondary));
    }
}
