package com.jrappspot.cashlipi.activities;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.adapters.MainPagerAdapter;
import com.jrappspot.cashlipi.utils.DatabaseManager;
import com.jrappspot.cashlipi.utils.FirestoreSyncManager;
import com.jrappspot.cashlipi.utils.FontUtils;

import android.util.Log;

/**
 * DashboardActivity — এখন এই Activity শুধু "হোস্ট" হিসেবে কাজ করে:
 *   ১. কমপ্যাক্ট ব্র্যান্ড হেডার + টাইটেল-সাইকেল অ্যানিমেশন (header-animation logic)
 *   ২. ৭-আইকন টপ নেভ বার + ViewPager2 সিঙ্ক্রোনাইজেশন (navigation-controller logic)
 *   ৩. Admin/Announcement/Force-update/Block-check — আগের মতোই অক্ষত (অপরিবর্তিত)
 *
 * প্রতিটা পেজের নিজস্ব কনটেন্ট/লজিক এখন আলাদা Fragment-এ থাকে (com.jrappspot.cashlipi.fragments.*),
 * যাতে একটার সমস্যা বাকিগুলোকে প্রভাবিত না করে।
 */
public class DashboardActivity extends BaseActivity {

    private DatabaseManager db;
    private FirestoreSyncManager firestoreSync;

    // ── নেভিগেশন-কন্ট্রোলার ──────────────────────────────────────────
    private ViewPager2 viewPager;
    private final LinearLayout[] navItems = new LinearLayout[MainPagerAdapter.PAGE_COUNT];
    private final ImageView[] navIcons = new ImageView[MainPagerAdapter.PAGE_COUNT];
    private final TextView[] navLabels = new TextView[MainPagerAdapter.PAGE_COUNT];
    private final View[] navIndicators = new View[MainPagerAdapter.PAGE_COUNT];

    // ── নেভিগেশন মেন্যু কাস্টমাইজেশন (সাইজ/রং/পজিশন/সোয়াইপ) ──────────
    private View topNavBar;               // nav_bar_items.xml root — top/bottom স্লটের মধ্যে রিপ্যারেন্ট হয়
    private FrameLayout topNavSlot;
    private FrameLayout bottomNavSlot;
    private FrameLayout bottomNavContainer;
    private View fabNotchBackdrop;
    private ImageButton headerAddBtn;
    private ImageButton bottomFab;

    // ── + বাটন ফ্যান-আউট মেন্যু (পুরনো BottomSheetDialog-এর বদলে) ──────
    private FrameLayout fabMenuOverlay;
    private View fabMenuScrim;
    private LinearLayout fabMenuTop;
    private LinearLayout fabMenuBottom;
    private boolean fabMenuOpen = false;
    private ImageButton activeFabAnchor; // যে বাটন থেকে মেন্যু খোলা হয়েছে (রোটেশনের জন্য)

    private static final String STATE_CURRENT_PAGE = "state_current_page";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        FontUtils.applyToView(this, findViewById(android.R.id.content));
        db = DatabaseManager.getInstance(this);
        firestoreSync = FirestoreSyncManager.getInstance(this);

        // Admin notification + config listener
        setupAdminListeners();
        // User block check
        checkIfUserIsBlocked();

        setupNavigation();
        setupFabMenu();

        int startPage = savedInstanceState != null
                ? savedInstanceState.getInt(STATE_CURRENT_PAGE, 0) : 0;
        viewPager.setCurrentItem(startPage, false);
        updateNavSelection(startPage);
    }

    @Override
    protected void onSaveInstanceState(@androidx.annotation.NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (viewPager != null) outState.putInt(STATE_CURRENT_PAGE, viewPager.getCurrentItem());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Firebase auto sync
        syncToFirebase();
        // সেটিং থেকে ফিরে এলে নেভিগেশন মেন্যুর সাইজ/রং/পজিশন/সোয়াইপ তাৎক্ষণিক আপডেট হয়
        applyNavCustomization();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fabMenuOpen) closeFabMenu();
    }

    // ══════════════════════════════════════════════════════════════
    //  ১. হেডার — এখন স্ট্যাটিক "CashLipi" (activity_dashboard.xml-এ সরাসরি সেট করা)।
    //     আগের টাইটেল-সাইকেল/ব্যালেন্স-প্লেসহোল্ডার সরানো হয়েছে — headerSliderSlot (XML-এ)
    //     এখন ফাঁকা, ভবিষ্যতে কাস্টম স্লাইড/AI-advice কার্ড এখানে বসানো যাবে।
    // ══════════════════════════════════════════════════════════════
    //  ২. নেভিগেশন-কন্ট্রোলার — ViewPager2 + adapter + আইকন-বার সিঙ্ক
    // ══════════════════════════════════════════════════════════════
    private void setupNavigation() {
        viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(new MainPagerAdapter(this));
        viewPager.setOffscreenPageLimit(1);

        topNavSlot = findViewById(R.id.topNavSlot);
        bottomNavSlot = findViewById(R.id.bottomNavSlot);
        bottomNavContainer = findViewById(R.id.bottomNavContainer);
        fabNotchBackdrop = findViewById(R.id.fabNotchBackdrop);
        headerAddBtn = findViewById(R.id.headerAddBtn);
        bottomFab = findViewById(R.id.bottomFab);

        // ৭-আইকন নেভ বার একবারই inflate হয় — পরে position অনুযায়ী topNavSlot/bottomNavSlot-এ রিপ্যারেন্ট হয়
        topNavBar = LayoutInflater.from(this).inflate(R.layout.nav_bar_items, topNavSlot, false);
        topNavSlot.addView(topNavBar);

        navItems[MainPagerAdapter.POSITION_HOME] = topNavBar.findViewById(R.id.navHome);
        navItems[MainPagerAdapter.POSITION_INCOME_EXPENSE] = findViewById(R.id.navIncomeExpense);
        navItems[MainPagerAdapter.POSITION_DENA_PAWNA] = findViewById(R.id.navDenaPawna);
        navItems[MainPagerAdapter.POSITION_SAVINGS] = findViewById(R.id.navSavings);
        navItems[MainPagerAdapter.POSITION_BAKIR_KHATA] = findViewById(R.id.navBakirKhata);
        navItems[MainPagerAdapter.POSITION_BUDGET] = findViewById(R.id.navBudget);
        navItems[MainPagerAdapter.POSITION_SETTINGS] = findViewById(R.id.navSettings);

        navIcons[MainPagerAdapter.POSITION_HOME] = findViewById(R.id.iconNavHome);
        navIcons[MainPagerAdapter.POSITION_INCOME_EXPENSE] = findViewById(R.id.iconNavIncomeExpense);
        navIcons[MainPagerAdapter.POSITION_DENA_PAWNA] = findViewById(R.id.iconNavDenaPawna);
        navIcons[MainPagerAdapter.POSITION_SAVINGS] = findViewById(R.id.iconNavSavings);
        navIcons[MainPagerAdapter.POSITION_BAKIR_KHATA] = findViewById(R.id.iconNavBakirKhata);
        navIcons[MainPagerAdapter.POSITION_BUDGET] = findViewById(R.id.iconNavBudget);
        navIcons[MainPagerAdapter.POSITION_SETTINGS] = findViewById(R.id.iconNavSettings);

        navLabels[MainPagerAdapter.POSITION_HOME] = findViewById(R.id.labelNavHome);
        navLabels[MainPagerAdapter.POSITION_INCOME_EXPENSE] = findViewById(R.id.labelNavIncomeExpense);
        navLabels[MainPagerAdapter.POSITION_DENA_PAWNA] = findViewById(R.id.labelNavDenaPawna);
        navLabels[MainPagerAdapter.POSITION_SAVINGS] = findViewById(R.id.labelNavSavings);
        navLabels[MainPagerAdapter.POSITION_BAKIR_KHATA] = findViewById(R.id.labelNavBakirKhata);
        navLabels[MainPagerAdapter.POSITION_BUDGET] = findViewById(R.id.labelNavBudget);
        navLabels[MainPagerAdapter.POSITION_SETTINGS] = findViewById(R.id.labelNavSettings);

        navIndicators[MainPagerAdapter.POSITION_HOME] = findViewById(R.id.indicatorNavHome);
        navIndicators[MainPagerAdapter.POSITION_INCOME_EXPENSE] = findViewById(R.id.indicatorNavIncomeExpense);
        navIndicators[MainPagerAdapter.POSITION_DENA_PAWNA] = findViewById(R.id.indicatorNavDenaPawna);
        navIndicators[MainPagerAdapter.POSITION_SAVINGS] = findViewById(R.id.indicatorNavSavings);
        navIndicators[MainPagerAdapter.POSITION_BAKIR_KHATA] = findViewById(R.id.indicatorNavBakirKhata);
        navIndicators[MainPagerAdapter.POSITION_BUDGET] = findViewById(R.id.indicatorNavBudget);
        navIndicators[MainPagerAdapter.POSITION_SETTINGS] = findViewById(R.id.indicatorNavSettings);

        // আইকনে ট্যাপ করলে সংশ্লিষ্ট পেজে স্মুথ ট্রানজিশনে চলে যাবে
        for (int i = 0; i < navItems.length; i++) {
            final int position = i;
            navItems[i].setOnClickListener(v -> viewPager.setCurrentItem(position, true));
        }

        // সোয়াইপ বা ট্যাপ — যেভাবেই পেজ বদলাক, আইকন-বার + হেডার সবসময় সিঙ্ক থাকবে
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateNavSelection(position);
            }
        });
    }

    /** বর্তমান পেজের আইকন হাইলাইট করে, বাকি সবগুলো নিউট্রাল স্টাইলে রাখে। */
    private void updateNavSelection(int selectedPosition) {
        for (int i = 0; i < navIcons.length; i++) {
            boolean selected = (i == selectedPosition);
            navIcons[i].setColorFilter(ContextCompat.getColor(this,
                    selected ? R.color.topNavSelected : R.color.topNavUnselected));
            navIndicators[i].setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  ৩. নেভিগেশন মেন্যু কাস্টমাইজেশন — সাইজ / ব্যাকগ্রাউন্ড রং / পজিশন (উপরে-নিচে) / সোয়াইপ
    //     Settings → "নেভিগেশন মেন্যু" থেকে বদলানো প্রেফারেন্স এখানে প্রয়োগ হয়, onResume-এ সবসময় রিফ্রেশ হয়।
    // ══════════════════════════════════════════════════════════════
    private void applyNavCustomization() {
        if (topNavBar == null) return;

        boolean bottomPosition = "bottom".equals(db.getNavPosition());
        boolean large = db.isNavIconLarge();
        int navBgColor;
        try {
            navBgColor = Color.parseColor(db.getNavBgColor());
        } catch (IllegalArgumentException e) {
            navBgColor = ContextCompat.getColor(this, R.color.bottomNavBg);
        }

        // ── ১. আইকন সাইজ প্রয়োগ (top/bottom দুই ক্ষেত্রেই একই সেটিং কাজ করে) ──
        int iconSizePx = getResources().getDimensionPixelSize(
                bottomPosition
                        ? (large ? R.dimen.bottom_nav_icon_size_large : R.dimen.bottom_nav_icon_size_small)
                        : (large ? R.dimen.top_nav_icon_size_large : R.dimen.top_nav_icon_size_small));
        for (ImageView icon : navIcons) {
            if (icon == null) continue;
            ViewGroup.LayoutParams lp = icon.getLayoutParams();
            lp.width = iconSizePx;
            lp.height = iconSizePx;
            icon.setLayoutParams(lp);
        }

        // ── লেবেল শুধু "নিচে" পজিশনে দেখানো হয় — ছোট আইকনগুলো কী বোঝায় তা স্পষ্ট করতে ──
        for (TextView label : navLabels) {
            if (label == null) continue;
            label.setVisibility(bottomPosition ? View.VISIBLE : View.GONE);
        }

        // ── ২. পজিশন প্রয়োগ — একই topNavBar ভিউটাকে top স্লট বা bottom স্লটে রিপ্যারেন্ট করা ──
        ViewGroup currentParent = (ViewGroup) topNavBar.getParent();
        if (bottomPosition) {
            if (currentParent != bottomNavSlot) {
                if (currentParent != null) currentParent.removeView(topNavBar);
                bottomNavSlot.addView(topNavBar, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
            bottomNavContainer.setVisibility(View.VISIBLE);
            headerAddBtn.setVisibility(View.GONE);
            // গোলাকার কোণা রেখেই কাস্টম রং বসানো (bg_bottom_nav_dark-এর উপরে ওভাররাইড)
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(navBgColor);
            float r = getResources().getDisplayMetrics().density * 18f;
            bg.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
            bottomNavSlot.setBackground(bg);
            bottomFab.setVisibility(View.VISIBLE);
            // + বাটনের পেছনের গোল "কুশন" — একই নেভবার রঙে, যাতে + বাটনটা বারের সাথে মিশে
            // আধুনিক কার্ভ/নচ লুক দেয় (বারের রং থেকে আলাদা দেখাবে না)
            if (fabNotchBackdrop != null) {
                GradientDrawable notchBg = new GradientDrawable();
                notchBg.setShape(GradientDrawable.OVAL);
                notchBg.setColor(navBgColor);
                fabNotchBackdrop.setBackground(notchBg);
                fabNotchBackdrop.setVisibility(View.VISIBLE);
            }
        } else {
            if (currentParent != topNavSlot) {
                if (currentParent != null) currentParent.removeView(topNavBar);
                topNavSlot.addView(topNavBar, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            bottomNavContainer.setVisibility(View.GONE);
            headerAddBtn.setVisibility(View.VISIBLE);
            topNavSlot.setBackgroundColor(navBgColor);
        }

        // ── ৩. সোয়াইপ অন/অফ ──
        viewPager.setUserInputEnabled(db.isNavSwipeEnabled());
    }

    // ══════════════════════════════════════════════════════════════
    //  ৪. Add (+) বাটন — হেডারে (উপরে পজিশন) বা কার্ভ FAB (নিচে পজিশন), দুটোই একই ফ্যান-আউট মেন্যু খোলে।
    //     পুরনো BottomSheetDialog সরিয়ে এখন + বাটন থেকে সরাসরি ৪টা মিনি-বাটন "ফুটে ওঠে" —
    //     স্কেল+ফেড+ওভারশুট অ্যানিমেশনে, staggered delay দিয়ে, আর + আইকনটা ৪৫° ঘুরে ✕ হয়ে যায়।
    // ══════════════════════════════════════════════════════════════
    private void setupFabMenu() {
        fabMenuOverlay = findViewById(R.id.fabMenuOverlay);
        fabMenuScrim = findViewById(R.id.fabMenuScrim);
        fabMenuTop = findViewById(R.id.fabMenuTop);
        fabMenuBottom = findViewById(R.id.fabMenuBottom);

        // fabMenuTop-এর আইটেমগুলো ডানপাশে সারিবদ্ধ (হেডারের + বাটনের নিচে)
        buildFabMenuItems(fabMenuTop);
        // fabMenuBottom-এর আইটেমগুলো কেন্দ্রে সারিবদ্ধ (নিচের + বাটনের উপরে)
        buildFabMenuItems(fabMenuBottom);

        View.OnClickListener openMenu = v -> toggleFabMenu((ImageButton) v);
        headerAddBtn.setOnClickListener(openMenu);
        bottomFab.setOnClickListener(v -> {
            v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(90).withEndAction(() ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()).start();
            toggleFabMenu(bottomFab);
        });
        fabMenuScrim.setOnClickListener(v -> closeFabMenu());
    }

    /** ৪টা অ্যাকশন-আইটেম (লেবেল চিপ + রঙিন গোল আইকন) তৈরি করে দেওয়া container-এ যোগ করে। */
    private void buildFabMenuItems(LinearLayout container) {
        addFabMenuItem(container, R.drawable.ic_nav_income_expense, getString(R.string.add_menu_income_expense),
                R.color.incomeColor, () -> {
                    Intent i = new Intent(this, AddTransactionActivity.class);
                    i.putExtra(AddTransactionActivity.EXTRA_MODE, "expense");
                    startActivity(i);
                });
        addFabMenuItem(container, R.drawable.ic_nav_dena_pawna, getString(R.string.add_menu_dena_pawna),
                R.color.ledgerColor, () -> startActivity(new Intent(this, AddLedgerActivity.class)));
        addFabMenuItem(container, R.drawable.ic_nav_savings, getString(R.string.add_menu_savings),
                R.color.savingsColor, () -> startActivity(new Intent(this, AddSavingsActivity.class)));
        addFabMenuItem(container, R.drawable.ic_nav_settings, getString(R.string.add_menu_settings),
                R.color.secondaryTextDark, () -> viewPager.setCurrentItem(MainPagerAdapter.POSITION_SETTINGS, true));
    }

    private void addFabMenuItem(LinearLayout container, int iconRes, String label, int colorRes, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.gravity = android.view.Gravity.END;
        rowLp.bottomMargin = getResources().getDimensionPixelSize(R.dimen.fab_menu_item_spacing);
        row.setLayoutParams(rowLp);

        TextView labelChip = new TextView(this);
        labelChip.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        FontUtils.applyToView(this, labelChip);
        labelChip.setText(label);
        applyStyleFabMiniLabel(labelChip);

        ImageView iconBtn = new ImageView(this);
        int size = getResources().getDimensionPixelSize(R.dimen.fab_menu_icon_size);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(size, size);
        iconBtn.setLayoutParams(iconLp);
        int pad = getResources().getDimensionPixelSize(R.dimen.fab_menu_icon_padding);
        iconBtn.setPadding(pad, pad, pad, pad);
        iconBtn.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_circle_solid));
        iconBtn.setBackgroundTintList(ContextCompat.getColorStateList(this, colorRes));
        iconBtn.setImageResource(iconRes);
        iconBtn.setColorFilter(ContextCompat.getColor(this, R.color.white));
        iconBtn.setElevation(6f);

        row.addView(labelChip);
        row.addView(iconBtn);

        View.OnClickListener rowClick = v -> {
            closeFabMenu();
            action.run();
        };
        row.setOnClickListener(rowClick);
        iconBtn.setOnClickListener(rowClick);

        container.addView(row);
    }

    private void applyStyleFabMiniLabel(TextView tv) {
        int padH = getResources().getDimensionPixelSize(R.dimen.fab_menu_label_pad_h);
        int padV = getResources().getDimensionPixelSize(R.dimen.fab_menu_label_pad_v);
        tv.setPadding(padH, padV, padH, padV);
        int marginEnd = getResources().getDimensionPixelSize(R.dimen.fab_menu_label_margin_end);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tv.getLayoutParams();
        lp.setMarginEnd(marginEnd);
        tv.setLayoutParams(lp);
        tv.setTextSize(13f);
        tv.setTextColor(ContextCompat.getColor(this, R.color.white));
        tv.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_fab_mini_label));
        tv.setElevation(6f);
    }

    private void toggleFabMenu(ImageButton anchor) {
        if (fabMenuOpen) {
            closeFabMenu();
        } else {
            openFabMenu(anchor);
        }
    }

    private void openFabMenu(ImageButton anchor) {
        fabMenuOpen = true;
        activeFabAnchor = anchor;
        boolean bottomPosition = "bottom".equals(db.getNavPosition());
        LinearLayout activeContainer = bottomPosition ? fabMenuBottom : fabMenuTop;
        LinearLayout otherContainer = bottomPosition ? fabMenuTop : fabMenuBottom;
        otherContainer.setVisibility(View.GONE);

        fabMenuOverlay.setVisibility(View.VISIBLE);
        fabMenuScrim.setAlpha(0f);
        fabMenuScrim.animate().alpha(1f).setDuration(180).start();

        activeContainer.setVisibility(View.VISIBLE);
        anchor.animate().rotation(45f).setDuration(200).start();

        for (int i = 0; i < activeContainer.getChildCount(); i++) {
            View row = activeContainer.getChildAt(i);
            row.setAlpha(0f);
            row.setScaleX(0.4f);
            row.setScaleY(0.4f);
            row.setTranslationY(activeContainer.getHeight() > 0 ? 40f : 60f);
            row.animate()
                    .alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                    .setStartDelay(i * 45L)
                    .setDuration(240)
                    .setInterpolator(new OvershootInterpolator(1.1f))
                    .start();
        }
    }

    private void closeFabMenu() {
        if (!fabMenuOpen) return;
        fabMenuOpen = false;
        if (activeFabAnchor != null) activeFabAnchor.animate().rotation(0f).setDuration(160).start();

        fabMenuScrim.animate().alpha(0f).setDuration(150).withEndAction(() -> {
            fabMenuOverlay.setVisibility(View.GONE);
            fabMenuTop.setVisibility(View.GONE);
            fabMenuBottom.setVisibility(View.GONE);
        }).start();
    }

    // ── HELPERS ──────────────────────────────────────────────────────
    @Override
    public void onBackPressed() {
        if (fabMenuOpen) {
            closeFabMenu();
            return;
        }
        new AlertDialog.Builder(this, R.style.AppDialog)
                .setTitle(" অ্যাপ বন্ধ করবেন?")
                .setMessage("CashLipi ক্যাশলিপি থেকে বের হতে চান?")
                .setPositiveButton("হ্যাঁ", (d, w) -> finish())
                .setNegativeButton("না", null)
                .show();
    }

    // ── Firebase Sync ──────────────────────────────────
    // ── Admin Notification/Announcement Dialog (Image সহ) ──
    private void showAdminMessageDialog(String title, String body, String imageUrl) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_admin_message, null);
        ImageView ivImage = dialogView.findViewById(R.id.ivAdminMsgImage);
        TextView tvTitle = dialogView.findViewById(R.id.tvAdminMsgTitle);
        TextView tvBody = dialogView.findViewById(R.id.tvAdminMsgBody);
        View btnOk = dialogView.findViewById(R.id.btnAdminMsgOk);

        tvTitle.setText(title);
        tvBody.setText(body);

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            ivImage.setVisibility(View.VISIBLE);
            Glide.with(this).load(imageUrl).into(ivImage);
        }

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AppDialog)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        btnOk.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void syncToFirebase() {
        if (db.isGoogleSignedIn()) {
            firestoreSync.uploadAllData(new FirestoreSyncManager.SyncCallback() {
                @Override public void onSuccess(String msg) {
                    Log.d("Dashboard", "Firebase sync ✅");
                }
                @Override public void onFailure(String err) {
                    Log.w("Dashboard", "Firebase sync: " + err);
                }
            });
        }
    }

    private void setupAdminListeners() {
        // Admin notification
        firestoreSync.listenForAdminNotifications((title, body, imageUrl) ->
            runOnUiThread(() -> showAdminMessageDialog("🔔 " + title, body, imageUrl)));

        // Admin announcement (আলাদা icon দিয়ে distinguish করা)
        firestoreSync.listenForAnnouncements((title, body, imageUrl) ->
            runOnUiThread(() -> showAdminMessageDialog("📣 " + title, body, imageUrl)));

        // Force update / Maintenance
        firestoreSync.listenForAppConfig((forceUpdate, maintenance,
                latestVer, updateUrl, noticeEnabled, noticeTitle, noticeBody) ->
            runOnUiThread(() -> {
                if (maintenance) {
                    new AlertDialog.Builder(this)
                        .setTitle("🔧 রক্ষণাবেক্ষণ চলছে")
                        .setMessage("অ্যাপটি সাময়িক বন্ধ। একটু পরে চেষ্টা করুন।")
                        .setPositiveButton("ঠিক আছে", (d, w) -> finish())
                        .setCancelable(false).show();
                } else if (forceUpdate) {
                    new AlertDialog.Builder(this)
                        .setTitle("🚀 আপডেট প্রয়োজন")
                        .setMessage("নতুন version পাওয়া গেছে। এখনই আপডেট করুন।")
                        .setPositiveButton("আপডেট করুন", (d, w) -> {
                            startActivity(new Intent(Intent.ACTION_VIEW,
                                android.net.Uri.parse(updateUrl)));
                            finish();
                        })
                        .setCancelable(false).show();
                } else if (noticeEnabled && noticeTitle != null && !noticeTitle.isEmpty()) {
                    new AlertDialog.Builder(this)
                        .setTitle("ℹ️ " + noticeTitle)
                        .setMessage(noticeBody)
                        .setPositiveButton("বুঝেছি", null).show();
                }
            }));
    }

    private void checkIfUserIsBlocked() {
        firestoreSync.checkUserBlockStatus((isBlocked, status) -> {
            if (isBlocked) {
                runOnUiThread(() -> {
                    String msg = "DEVICE_BANNED".equals(status)
                        ? "এই ডিভাইসটি নিষিদ্ধ করা হয়েছে।"
                        : "আপনার account ব্লক করা হয়েছে।\nAdmin-এর সাথে যোগাযোগ করুন।";
                    new AlertDialog.Builder(this)
                        .setTitle("🚫 Access বন্ধ")
                        .setMessage(msg)
                        .setPositiveButton("বাহির", (d, w) -> finish())
                        .setCancelable(false).show();
                });
            }
        });
    }

}
