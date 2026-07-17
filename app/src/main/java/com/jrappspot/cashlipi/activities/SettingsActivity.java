package com.jrappspot.cashlipi.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.utils.FontUtils;
import com.jrappspot.cashlipi.utils.DatabaseManager;

import java.io.File;

/**
 * SettingsActivity — Profile card + menu items.
 */
public class SettingsActivity extends BaseActivity {

    private DatabaseManager db;
    private Switch switchDarkMode;

    private ImageView ivSettingsPhoto;
    private TextView  tvSettingsName, tvSettingsSubtitle;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_settings);
        FontUtils.applyToView(this, findViewById(android.R.id.content));
        db = DatabaseManager.getInstance(this);

        // Views
        ivSettingsPhoto   = findViewById(R.id.ivSettingsPhoto);
        tvSettingsName    = findViewById(R.id.tvSettingsName);
        tvSettingsSubtitle= findViewById(R.id.tvSettingsSubtitle);
        switchDarkMode    = findViewById(R.id.switchDarkMode);

        // Profile card → ProfileActivity
        View cardProfile = findViewById(R.id.cardProfile);
        if (cardProfile != null)
            cardProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        // Dark mode
        if (switchDarkMode != null) {
            switchDarkMode.setChecked(db.isDarkMode());
            switchDarkMode.setOnCheckedChangeListener((btn, checked) -> {
                db.setDarkMode(checked);
                com.jrappspot.cashlipi.utils.ThemeUtils.applySavedTheme(db);
                Toast.makeText(this,
                    checked ? "🌙 ডার্ক মোড চালু" : "☀️ লাইট মোড চালু",
                    Toast.LENGTH_SHORT).show();
                recreate();
            });
        }

        // Advanced Features → AdvancedFeaturesActivity
        View menuAdvanced = findViewById(R.id.menuAdvancedFeatures);
        if (menuAdvanced != null)
            menuAdvanced.setOnClickListener(v ->
                startActivity(new Intent(this, AdvancedFeaturesActivity.class)));

        // Menu items
        View menuAppLock = findViewById(R.id.menuAppLock);
        if (menuAppLock != null)
            menuAppLock.setOnClickListener(v ->
                startActivity(new Intent(this, AppLockActivity.class)));

        // নেভিগেশন মেন্যু কাস্টমাইজ (সাইজ / ব্যাকগ্রাউন্ড রং / পজিশন / সোয়াইপ)
        View menuNavCustomize = findViewById(R.id.menuNavCustomize);
        if (menuNavCustomize != null)
            menuNavCustomize.setOnClickListener(v -> showNavCustomizeDialog());

        View menuCat = findViewById(R.id.menuCategories);
        if (menuCat != null)
            menuCat.setOnClickListener(v ->
                startActivity(new Intent(this, CategoriesActivity.class)));

        View menuTrash = findViewById(R.id.menuTrash);
        if (menuTrash != null)
            menuTrash.setOnClickListener(v ->
                startActivity(new Intent(this, TrashActivity.class)));

        View menuBackup = findViewById(R.id.menuBackup);
        if (menuBackup != null)
            menuBackup.setOnClickListener(v ->
                startActivity(new Intent(this, BackupActivity.class)));

        View menuAbout = findViewById(R.id.menuAbout);
        if (menuAbout != null)
            menuAbout.setOnClickListener(v ->
                startActivity(new Intent(this, AboutActivity.class)));

        View menuClear = findViewById(R.id.menuClearData);
        if (menuClear != null)
            menuClear.setOnClickListener(v -> showDeleteAllWarningDialog());
    }

    // ─── নেভিগেশন মেন্যু কাস্টমাইজ ডায়ালগ (সাইজ / ব্যাকগ্রাউন্ড রং / পজিশন / সোয়াইপ) ───
    private void showNavCustomizeDialog() {
        View body = getLayoutInflater().inflate(R.layout.dialog_nav_customize, null);
        FontUtils.applyToView(this, body);

        TextView btnSizeLarge = body.findViewById(R.id.btnSizeLarge);
        TextView btnSizeSmall = body.findViewById(R.id.btnSizeSmall);
        TextView btnPosTop    = body.findViewById(R.id.btnPosTop);
        TextView btnPosBottom = body.findViewById(R.id.btnPosBottom);
        Switch   switchSwipe  = body.findViewById(R.id.switchNavSwipe);
        Button   btnSave      = body.findViewById(R.id.btnNavSave);

        final int[] swatchIds = {
                R.id.swatchNavy, R.id.swatchIndigo, R.id.swatchPurple, R.id.swatchTeal,
                R.id.swatchGreen, R.id.swatchOrange, R.id.swatchPink, R.id.swatchCharcoal
        };
        final View[] swatches = new View[swatchIds.length];
        for (int i = 0; i < swatchIds.length; i++) swatches[i] = body.findViewById(swatchIds[i]);

        // ── বর্তমান সেটিং লোড করে দেখানো ──
        final boolean[] sizeLarge = {db.isNavIconLarge()};
        final boolean[] posBottom = {"bottom".equals(db.getNavPosition())};
        final String[]  navColor  = {db.getNavBgColor()};

        Runnable refreshSizeToggle = () -> {
            btnSizeLarge.setBackground(sizeLarge[0] ? ContextCompat.getDrawable(this, R.drawable.bg_login_toggle_selected) : null);
            btnSizeLarge.setTextColor(ContextCompat.getColor(this, sizeLarge[0] ? R.color.white : R.color.textSecondary));
            btnSizeSmall.setBackground(!sizeLarge[0] ? ContextCompat.getDrawable(this, R.drawable.bg_login_toggle_selected) : null);
            btnSizeSmall.setTextColor(ContextCompat.getColor(this, !sizeLarge[0] ? R.color.white : R.color.textSecondary));
        };
        Runnable refreshPosToggle = () -> {
            btnPosTop.setBackground(!posBottom[0] ? ContextCompat.getDrawable(this, R.drawable.bg_login_toggle_selected) : null);
            btnPosTop.setTextColor(ContextCompat.getColor(this, !posBottom[0] ? R.color.white : R.color.textSecondary));
            btnPosBottom.setBackground(posBottom[0] ? ContextCompat.getDrawable(this, R.drawable.bg_login_toggle_selected) : null);
            btnPosBottom.setTextColor(ContextCompat.getColor(this, posBottom[0] ? R.color.white : R.color.textSecondary));
        };
        Runnable refreshSwatches = () -> {
            for (View sw : swatches) {
                boolean sel = navColor[0].equalsIgnoreCase(String.valueOf(sw.getTag()));
                sw.setScaleX(sel ? 1.22f : 1f);
                sw.setScaleY(sel ? 1.22f : 1f);
            }
        };

        refreshSizeToggle.run();
        refreshPosToggle.run();
        refreshSwatches.run();
        switchSwipe.setChecked(db.isNavSwipeEnabled());

        btnSizeLarge.setOnClickListener(v -> { sizeLarge[0] = true;  refreshSizeToggle.run(); });
        btnSizeSmall.setOnClickListener(v -> { sizeLarge[0] = false; refreshSizeToggle.run(); });
        btnPosTop.setOnClickListener(v -> { posBottom[0] = false; refreshPosToggle.run(); });
        btnPosBottom.setOnClickListener(v -> { posBottom[0] = true;  refreshPosToggle.run(); });
        for (View sw : swatches) {
            sw.setOnClickListener(v -> { navColor[0] = String.valueOf(v.getTag()); refreshSwatches.run(); });
        }

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AppDialog)
                .setView(body)
                .setCancelable(true)
                .create();

        btnSave.setOnClickListener(v -> {
            db.setNavIconLarge(sizeLarge[0]);
            db.setNavPosition(posBottom[0] ? "bottom" : "top");
            db.setNavBgColor(navColor[0]);
            db.setNavSwipeEnabled(switchSwipe.isChecked());
            Toast.makeText(this, "✅ নেভিগেশন মেন্যু আপডেট হয়েছে", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }


    private void showDeleteAllWarningDialog() {
        View body = getLayoutInflater().inflate(R.layout.dialog_delete_all_warning, null);
        FontUtils.applyToView(this, body);

        new AlertDialog.Builder(this, R.style.AppDialog)
                .setView(body)
                .setPositiveButton("মুছে ফেলুন", (d, w) -> showDeletePasswordDialog())
                .setNegativeButton("বাতিল করুন", null)
                .show();
    }

    // ─── ডেটা মুছে ফেলার ফ্লো (ধাপ ২: পাসওয়ার্ড কনফার্মেশন) ──
    private static final String CLEAR_DATA_PASSWORD = "0000";

    private void showDeletePasswordDialog() {
        View body = getLayoutInflater().inflate(R.layout.dialog_delete_password, null);
        FontUtils.applyToView(this, body);

        EditText et1 = body.findViewById(R.id.etPinDigit1);
        EditText et2 = body.findViewById(R.id.etPinDigit2);
        EditText et3 = body.findViewById(R.id.etPinDigit3);
        EditText et4 = body.findViewById(R.id.etPinDigit4);
        TextView tvError = body.findViewById(R.id.tvPinError);
        final EditText[] boxes = {et1, et2, et3, et4};

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AppDialog)
                .setView(body)
                .setPositiveButton("নিশ্চিত করুন", null) // নিচে override করা হয়েছে, ভুল পাসওয়ার্ডে dialog বন্ধ হবে না
                .setNegativeButton("বাতিল করুন", null)
                .create();

        // বক্স থেকে বক্সে অটো-মুভ করার জন্য
        setupPinAutoAdvance(boxes, tvError);

        dialog.setOnShowListener(dlg -> {
            et1.requestFocus();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
                StringBuilder entered = new StringBuilder();
                for (EditText box : boxes) entered.append(box.getText().toString().trim());

                if (entered.toString().equals(CLEAR_DATA_PASSWORD)) {
                    db.clearAllData();
                    Toast.makeText(this, "✅ সব ডেটা সফলভাবে মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    tvError.setVisibility(View.VISIBLE);
                    for (EditText box : boxes) box.setText("");
                    et1.requestFocus();
                }
            });
        });

        dialog.show();
    }

    /** ৪টি PIN বক্সের মধ্যে টাইপ করলে অটোমেটিক পরের বক্সে চলে যাবে, Backspace দিলে আগের বক্সে ফিরে যাবে। */
    private void setupPinAutoAdvance(EditText[] boxes, TextView tvError) {
        for (int i = 0; i < boxes.length; i++) {
            final int index = i;
            boxes[i].addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                    if (tvError.getVisibility() == View.VISIBLE) tvError.setVisibility(View.GONE);
                    if (s.length() == 1 && index < boxes.length - 1) {
                        boxes[index + 1].requestFocus();
                    }
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });

            boxes[i].setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == android.view.KeyEvent.ACTION_DOWN
                        && keyCode == android.view.KeyEvent.KEYCODE_DEL
                        && boxes[index].getText().toString().isEmpty()
                        && index > 0) {
                    boxes[index - 1].requestFocus();
                    boxes[index - 1].setText("");
                }
                return false;
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshProfileCard();
    }

    // ─── Profile card update ────────────────────────────
    private void refreshProfileCard() {
        if (tvSettingsName != null)
            tvSettingsName.setText(db.getDisplayName());

        if (tvSettingsSubtitle != null) {
            if (db.isGoogleSignedIn()) {
                tvSettingsSubtitle.setText(db.getGoogleEmail());
            } else {
                tvSettingsSubtitle.setText("প্রোফাইল দেখুন ও সম্পাদনা করুন");
            }
        }

        if (ivSettingsPhoto != null) {
            String source = db.getEffectivePhotoSource();
            if (source != null && !source.isEmpty()) {
                ivSettingsPhoto.clearColorFilter();
                Object from;
                if (source.startsWith("http")) {
                    String sized = source;
                    if (sized.contains("=s")) {
                        sized = sized.replaceAll("=s\\d+(-c)?", "=s168-c");
                    } else {
                        sized = sized + "=s168-c";
                    }
                    from = sized;
                } else {
                    from = new File(source);
                }
                Glide.with(this)
                    .load(from)
                    .transform(new CircleCrop())
                    .placeholder(android.R.drawable.ic_menu_myplaces)
                    .error(android.R.drawable.ic_menu_myplaces)
                    .into(ivSettingsPhoto);
            } else {
                ivSettingsPhoto.setImageResource(android.R.drawable.ic_menu_myplaces);
                ivSettingsPhoto.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.profileIconTint));
            }
        }
    }
}
