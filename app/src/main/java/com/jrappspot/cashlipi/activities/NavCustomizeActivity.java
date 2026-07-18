package com.jrappspot.cashlipi.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.utils.DatabaseManager;
import com.jrappspot.cashlipi.utils.NavBarStyler;

import java.util.List;

/**
 * নেভিগেশন মেন্যু — সম্পূর্ণ কাস্টমাইজেশন পেজ (আগে Settings-এ পপ-আপ ডায়ালগ ছিল, এখন পূর্ণ পেজ)।
 * প্রতিটা কন্ট্রোল (স্টাইল/সাইজ/রং/পজিশন/সোয়াইপ) ট্যাপ করা মাত্র DatabaseManager-এ সেভ হয়
 * এবং লাইভ প্রিভিউ কার্ড সাথে সাথে আপডেট হয়। এই Activity থেকে ফিরে গেলে (finish/back)
 * DashboardActivity.onResume() স্বয়ংক্রিয়ভাবে applyNavCustomization() চালায় — তাই আলাদা করে
 * অ্যাপ থেকে বের হয়ে আবার ঢোকা লাগে না, পরিবর্তন সাথে সাথেই লাইভ দেখা যায়।
 */
public class NavCustomizeActivity extends BaseActivity {

    private DatabaseManager db;

    private FrameLayout previewBarSlot;
    private LinearLayout styleCardsContainer;
    private TextView btnSizeLarge, btnSizeSmall, btnPosTop, btnPosBottom;
    private Switch switchNavSwipe;

    private final List<LinearLayout> styleCardViews = new java.util.ArrayList<>();

    // ── বর্তমান (এখনো সেভ-না-করা নয়, প্রতিটা ট্যাপেই সাথে সাথে সেভ হয়) স্টেট ──
    private String selectedStyle;
    private boolean sizeLarge;
    private boolean posBottom;
    private String navColorHex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nav_customize);
        db = DatabaseManager.getInstance(this);

        // বর্তমান সেভ করা সেটিং লোড
        selectedStyle = db.getNavStyle();
        sizeLarge = db.isNavIconLarge();
        posBottom = "bottom".equals(db.getNavPosition());
        navColorHex = db.getNavBgColor();

        findViewById(R.id.btnNavBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnNavDone).setOnClickListener(v -> finish());

        previewBarSlot = findViewById(R.id.previewBarSlot);
        styleCardsContainer = findViewById(R.id.styleCardsContainer);
        btnSizeLarge = findViewById(R.id.btnSizeLarge);
        btnSizeSmall = findViewById(R.id.btnSizeSmall);
        btnPosTop = findViewById(R.id.btnPosTop);
        btnPosBottom = findViewById(R.id.btnPosBottom);
        switchNavSwipe = findViewById(R.id.switchNavSwipe);

        buildStyleCards();
        setupSizeToggle();
        setupSwatches();
        setupPositionToggle();
        setupSwipeSwitch();

        refreshAllControls();
        refreshPreview();
    }

    // ══════════════════════════════════════════════════════════════
    //  ৭টা স্টাইল প্রিসেট কার্ড — ট্যাপ করলেই সেভ + প্রিভিউ রিফ্রেশ
    // ══════════════════════════════════════════════════════════════
    private void buildStyleCards() {
        for (NavBarStyler.StyleInfo info : NavBarStyler.allStyles()) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(android.view.Gravity.CENTER);
            int w = getResources().getDimensionPixelSize(R.dimen.nav_style_card_width);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(getResources().getDimensionPixelSize(R.dimen.nav_style_card_spacing));
            card.setLayoutParams(lp);
            card.setPadding(dp(12), dp(14), dp(12), dp(14));
            card.setTag(info.key);

            TextView title = new TextView(this);
            title.setText(info.title);
            title.setTextSize(13f);
            title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            title.setGravity(android.view.Gravity.CENTER);
            title.setTextColor(ContextCompat.getColor(this, R.color.textPrimary));

            TextView subtitle = new TextView(this);
            subtitle.setText(info.subtitle);
            subtitle.setTextSize(10.5f);
            subtitle.setGravity(android.view.Gravity.CENTER);
            subtitle.setTextColor(ContextCompat.getColor(this, R.color.textSecondary));
            LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            subLp.topMargin = dp(3);
            subtitle.setLayoutParams(subLp);

            card.addView(title);
            card.addView(subtitle);

            card.setOnClickListener(v -> {
                selectedStyle = info.key;
                db.setNavStyle(selectedStyle);
                refreshStyleCards();
                refreshPreview();
            });

            styleCardsContainer.addView(card);
            styleCardViews.add(card);
        }
    }

    private void refreshStyleCards() {
        for (LinearLayout card : styleCardViews) {
            boolean selected = selectedStyle.equals(card.getTag());
            card.setBackground(ContextCompat.getDrawable(this,
                    selected ? R.drawable.bg_login_toggle_selected : R.drawable.bg_card_white));
            for (int i = 0; i < card.getChildCount(); i++) {
                if (card.getChildAt(i) instanceof TextView) {
                    ((TextView) card.getChildAt(i)).setTextColor(ContextCompat.getColor(this,
                            selected ? R.color.white : (i == 0 ? R.color.textPrimary : R.color.textSecondary)));
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  আইকন সাইজ / ব্যাকগ্রাউন্ড রং / পজিশন / সোয়াইপ — বিদ্যমান dialog_nav_customize.xml-এর
    //  একই যুক্তি, শুধু এখন প্রতিটা ট্যাপেই সাথে সাথে DatabaseManager-এ সেভ হয়
    // ══════════════════════════════════════════════════════════════
    private void setupSizeToggle() {
        btnSizeLarge.setOnClickListener(v -> {
            sizeLarge = true;
            db.setNavIconLarge(true);
            refreshSizeToggle();
            refreshPreview();
        });
        btnSizeSmall.setOnClickListener(v -> {
            sizeLarge = false;
            db.setNavIconLarge(false);
            refreshSizeToggle();
            refreshPreview();
        });
    }

    private void refreshSizeToggle() {
        btnSizeLarge.setBackground(sizeLarge ? ContextCompat.getDrawable(this, R.drawable.bg_login_toggle_selected) : null);
        btnSizeLarge.setTextColor(ContextCompat.getColor(this, sizeLarge ? R.color.white : R.color.textSecondary));
        btnSizeSmall.setBackground(!sizeLarge ? ContextCompat.getDrawable(this, R.drawable.bg_login_toggle_selected) : null);
        btnSizeSmall.setTextColor(ContextCompat.getColor(this, !sizeLarge ? R.color.white : R.color.textSecondary));
    }

    private void setupPositionToggle() {
        btnPosTop.setOnClickListener(v -> {
            posBottom = false;
            db.setNavPosition("top");
            refreshPositionToggle();
            refreshPreview();
        });
        btnPosBottom.setOnClickListener(v -> {
            posBottom = true;
            db.setNavPosition("bottom");
            refreshPositionToggle();
            refreshPreview();
        });
    }

    private void refreshPositionToggle() {
        btnPosTop.setBackground(!posBottom ? ContextCompat.getDrawable(this, R.drawable.bg_login_toggle_selected) : null);
        btnPosTop.setTextColor(ContextCompat.getColor(this, !posBottom ? R.color.white : R.color.textSecondary));
        btnPosBottom.setBackground(posBottom ? ContextCompat.getDrawable(this, R.drawable.bg_login_toggle_selected) : null);
        btnPosBottom.setTextColor(ContextCompat.getColor(this, posBottom ? R.color.white : R.color.textSecondary));
    }

    private void setupSwatches() {
        int[] swatchIds = {
                R.id.swatchNavy, R.id.swatchIndigo, R.id.swatchPurple, R.id.swatchTeal,
                R.id.swatchGreen, R.id.swatchOrange, R.id.swatchPink, R.id.swatchCharcoal
        };
        for (int id : swatchIds) {
            View sw = findViewById(id);
            sw.setOnClickListener(v -> {
                navColorHex = String.valueOf(v.getTag());
                db.setNavBgColor(navColorHex);
                refreshSwatches();
                refreshPreview();
            });
        }
        refreshSwatches();
    }

    private void refreshSwatches() {
        int[] swatchIds = {
                R.id.swatchNavy, R.id.swatchIndigo, R.id.swatchPurple, R.id.swatchTeal,
                R.id.swatchGreen, R.id.swatchOrange, R.id.swatchPink, R.id.swatchCharcoal
        };
        for (int id : swatchIds) {
            View sw = findViewById(id);
            boolean selected = navColorHex.equalsIgnoreCase(String.valueOf(sw.getTag()));
            sw.setScaleX(selected ? 1.22f : 1f);
            sw.setScaleY(selected ? 1.22f : 1f);
        }
    }

    private void setupSwipeSwitch() {
        switchNavSwipe.setChecked(db.isNavSwipeEnabled());
        switchNavSwipe.setOnCheckedChangeListener((btn, checked) -> db.setNavSwipeEnabled(checked));
    }

    private void refreshAllControls() {
        refreshStyleCards();
        refreshSizeToggle();
        refreshPositionToggle();
        refreshSwatches();
    }

    // ══════════════════════════════════════════════════════════════
    //  লাইভ প্রিভিউ — আসল nav_bar_items.xml ইনফ্লেট করে DashboardActivity-এর মতোই
    //  NavBarStyler দিয়ে স্টাইল প্রয়োগ করে, যাতে প্রিভিউ আর আসল ফলাফল হুবহু এক থাকে
    // ══════════════════════════════════════════════════════════════
    private void refreshPreview() {
        previewBarSlot.removeAllViews();
        View bar = LayoutInflater.from(this).inflate(R.layout.nav_bar_items, previewBarSlot, false);
        previewBarSlot.addView(bar);

        int navBgColor;
        try {
            navBgColor = Color.parseColor(navColorHex);
        } catch (IllegalArgumentException e) {
            navBgColor = ContextCompat.getColor(this, R.color.bottomNavBg);
        }

        int iconSizePx = getResources().getDimensionPixelSize(
                posBottom
                        ? (sizeLarge ? R.dimen.bottom_nav_icon_size_large : R.dimen.bottom_nav_icon_size_small)
                        : (sizeLarge ? R.dimen.top_nav_icon_size_large : R.dimen.top_nav_icon_size_small));

        int[] iconIds = {
                R.id.iconNavHome, R.id.iconNavIncomeExpense, R.id.iconNavDenaPawna,
                R.id.iconNavBakirKhata, R.id.iconNavSavings, R.id.iconNavBudget, R.id.iconNavSettings
        };
        int[] labelIds = {
                R.id.labelNavHome, R.id.labelNavIncomeExpense, R.id.labelNavDenaPawna,
                R.id.labelNavBakirKhata, R.id.labelNavSavings, R.id.labelNavBudget, R.id.labelNavSettings
        };
        int[] indicatorIds = {
                R.id.indicatorNavHome, R.id.indicatorNavIncomeExpense, R.id.indicatorNavDenaPawna,
                R.id.indicatorNavBakirKhata, R.id.indicatorNavSavings, R.id.indicatorNavBudget, R.id.indicatorNavSettings
        };
        int[] itemIds = {
                R.id.navHome, R.id.navIncomeExpense, R.id.navDenaPawna,
                R.id.navBakirKhata, R.id.navSavings, R.id.navBudget, R.id.navSettings
        };

        LinearLayout[] items = new LinearLayout[7];
        View[] indicators = new View[7];
        boolean showLabels = posBottom || NavBarStyler.STYLE_MINIMAL.equals(selectedStyle);

        for (int i = 0; i < 7; i++) {
            ImageView icon = bar.findViewById(iconIds[i]);
            TextView label = bar.findViewById(labelIds[i]);
            View indicator = bar.findViewById(indicatorIds[i]);
            items[i] = bar.findViewById(itemIds[i]);
            indicators[i] = indicator;

            ViewGroup.LayoutParams lp = icon.getLayoutParams();
            lp.width = iconSizePx;
            lp.height = iconSizePx;
            icon.setLayoutParams(lp);
            label.setVisibility(showLabels ? View.VISIBLE : View.GONE);

            // প্রিভিউতে "হোম" সিলেক্টেড ধরে দেখানো হয় — বাস্তব ব্যবহারকারী অভিজ্ঞতা বোঝাতে
            icon.setColorFilter(ContextCompat.getColor(this,
                    i == 0 ? R.color.topNavSelected : R.color.topNavUnselected));
        }

        NavBarStyler.applyBarBackground(this, previewBarSlot, selectedStyle, navBgColor, posBottom);
        NavBarStyler.applyItemSelection(this, items, indicators, 0, selectedStyle, navBgColor);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
