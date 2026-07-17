package com.jrappspot.cashlipi.activities;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
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
    private final View[] navIndicators = new View[MainPagerAdapter.PAGE_COUNT];

    // ── নেভিগেশন মেন্যু কাস্টমাইজেশন (সাইজ/রং/পজিশন/সোয়াইপ) ──────────
    private View topNavBar;               // nav_bar_items.xml root — top/bottom স্লটের মধ্যে রিপ্যারেন্ট হয়
    private FrameLayout topNavSlot;
    private FrameLayout bottomNavSlot;
    private FrameLayout bottomNavContainer;
    private ImageButton headerAddBtn;
    private ImageButton bottomFab;

    // ── হেডার-অ্যানিমেশন (টাইটেল-সাইকেল) ─────────────────────────────
    private TextSwitcher headerSwitcher;
    private final Handler headerHandler = new Handler(Looper.getMainLooper());
    private static final String BRAND_TITLE = "CashLipi";
    private static final String[] PAGE_NAMES = {
            "হোম", "আয়-ব্যয়", "দেনা-পাওনা", "সঞ্চয়", "বাকির খাতা", "বাজেট", "সেটিং"
    };
    private static final String BALANCE_PLACEHOLDER = "ব্যালেন্স: ৳০.০০"; // পরবর্তী ধাপে আসল ডেটা যুক্ত হবে
    private static final long STEP_DELAY_MS = 2000L;

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

        setupHeader();
        setupNavigation();
        setupAddMenu();

        int startPage = savedInstanceState != null
                ? savedInstanceState.getInt(STATE_CURRENT_PAGE, 0) : 0;
        viewPager.setCurrentItem(startPage, false);
        updateNavSelection(startPage);
        runHeaderCycle(startPage);
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
        headerHandler.removeCallbacksAndMessages(null);
    }

    // ══════════════════════════════════════════════════════════════
    //  ১. হেডার-অ্যানিমেশন (টাইটেল-সাইকেল) — অন্য অংশ থেকে স্বাধীন
    // ══════════════════════════════════════════════════════════════
    private void setupHeader() {
        headerSwitcher = findViewById(R.id.headerTitleSwitcher);
        headerSwitcher.setFactory(() -> {
            TextView tv = new TextView(DashboardActivity.this);
            tv.setTextColor(ContextCompat.getColor(this, R.color.topHeaderTitle));
            tv.setTextSize(18f);
            tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            tv.setLetterSpacing(0.06f);
            tv.setSingleLine(true);
            tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
            return tv;
        });
        headerSwitcher.setCurrentText(BRAND_TITLE);
    }

    /** প্রতিটা পেজে গেলে ৩-ধাপের চক্র নতুন করে শুরু হয় (state reset)। */
    private void runHeaderCycle(int position) {
        headerHandler.removeCallbacksAndMessages(null);

        // ধাপ ১ — সবসময় "CashLipi" ব্র্যান্ড নাম দিয়ে শুরু
        headerSwitcher.setCurrentText(BRAND_TITLE);

        // ধাপ ২ — ২ সেকেন্ড পর বর্তমান পেজের নাম
        headerHandler.postDelayed(() -> {
            String pageName = position >= 0 && position < PAGE_NAMES.length ? PAGE_NAMES[position] : "";
            headerSwitcher.setText(pageName);

            // ধাপ ৩ — তারপর ব্যালেন্স/সামারি প্লেসহোল্ডার
            headerHandler.postDelayed(() -> headerSwitcher.setText(BALANCE_PLACEHOLDER), STEP_DELAY_MS);
        }, STEP_DELAY_MS);
    }

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
                runHeaderCycle(position);
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
    //  ৪. Add (+) বাটন — হেডারে (উপরে পজিশন) বা কার্ভ FAB (নিচে পজিশন), দুটোই একই পপ-আপ খোলে
    // ══════════════════════════════════════════════════════════════
    private void setupAddMenu() {
        View.OnClickListener openMenu = v -> showAddMenu(v);
        headerAddBtn.setOnClickListener(openMenu);
        bottomFab.setOnClickListener(v -> {
            // সাধারণ স্কেল-বাউন্স অ্যানিমেশন — সহজ ও হালকা
            v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(90).withEndAction(() ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()).start();
            showAddMenu(v);
        });
    }

    private void showAddMenu(View anchor) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheet = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_add_menu, null);
        dialog.setContentView(sheet);

        sheet.findViewById(R.id.menuAddIncomeExpense).setOnClickListener(v -> {
            dialog.dismiss();
            Intent i = new Intent(this, AddTransactionActivity.class);
            i.putExtra(AddTransactionActivity.EXTRA_MODE, "expense");
            startActivity(i);
        });
        sheet.findViewById(R.id.menuAddDenaPawna).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, AddLedgerActivity.class));
        });
        sheet.findViewById(R.id.menuAddSavings).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, AddSavingsActivity.class));
        });
        sheet.findViewById(R.id.menuAddSettings).setOnClickListener(v -> {
            dialog.dismiss();
            viewPager.setCurrentItem(MainPagerAdapter.POSITION_SETTINGS, true);
        });

        dialog.show();
    }

    // ── HELPERS ──────────────────────────────────────────────────────
    @Override
    public void onBackPressed() {
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
