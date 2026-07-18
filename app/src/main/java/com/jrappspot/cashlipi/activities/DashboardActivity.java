package com.jrappspot.cashlipi.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.adapters.MainPagerAdapter;
import com.jrappspot.cashlipi.utils.DatabaseManager;
import com.jrappspot.cashlipi.utils.FirestoreSyncManager;
import com.jrappspot.cashlipi.utils.FontUtils;
import com.jrappspot.cashlipi.utils.NavBarStyler;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

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

    // ── নেভিগেশন ড্রয়ার ──────────────────────────────────────────────
    private androidx.drawerlayout.widget.DrawerLayout drawerLayout;

    // ── টপ আইডেন্টিটি বার (হ্যামবার্গার/সালাম-নাম-সময়-তারিখ স্লাইড/নোটিফিকেশন/প্রোফাইল) ──
    private ImageButton headerMenuBtn, headerNotifBtn, headerProfileBtn;
    private ViewFlipper headerIdentityFlipper;
    private ViewFlipper headerAiFlipper;
    private final Handler identityHandler = new Handler(Looper.getMainLooper());
    private final Handler aiAdviceHandler = new Handler(Looper.getMainLooper());

    // ── নেভিগেশন-কন্ট্রোলার ──────────────────────────────────────────
    private ViewPager2 viewPager;
    private final LinearLayout[] navItems = new LinearLayout[MainPagerAdapter.PAGE_COUNT];
    private final ImageView[] navIcons = new ImageView[MainPagerAdapter.PAGE_COUNT];
    private final TextView[] navLabels = new TextView[MainPagerAdapter.PAGE_COUNT];
    private final View[] navIndicators = new View[MainPagerAdapter.PAGE_COUNT];

    // ── নেভিগেশন মেন্যু কাস্টমাইজেশন (সাইজ/রং/পজিশন/সোয়াইপ) ──────────
    private LinearLayout topNavBar;               // nav_bar_items.xml root — top/bottom স্লটের মধ্যে রিপ্যারেন্ট হয়
    private FrameLayout topNavSlot;
    private FrameLayout bottomNavSlot;
    private FrameLayout bottomNavContainer;
    private View fabNotchBackdrop;
    private LinearLayout headerRow;
    private View topBlockDivider;
    private ImageButton headerAddBtn;
    private ImageButton bottomFab;

    // ── + বাটন ফ্যান-আউট মেন্যু (পুরনো BottomSheetDialog-এর বদলে) ──────
    private FrameLayout fabMenuOverlay;
    private View fabMenuScrim;
    private LinearLayout fabMenuTop;
    private LinearLayout fabMenuBottom;
    private boolean fabMenuOpen = false;
    private ImageButton activeFabAnchor; // যে বাটন থেকে মেন্যু খোলা হয়েছে (রোটেশনের জন্য)
    private boolean navSavingsGapActive = false; // + বাটনের নিচে "সঞ্চয়"-স্লট এখন "যোগ"-গ্যাপ কিনা
    private String currentNavStyle = NavBarStyler.STYLE_CLASSIC;

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
        setupTopIdentityBar();
        setupNavDrawer();

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
        refreshAiAdviceTips();
        startIdentityAutoSlide();
        startAiAdviceAutoSlide();
        refreshDrawerHeader();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fabMenuOpen) closeFabMenu();
        identityHandler.removeCallbacksAndMessages(null);
        aiAdviceHandler.removeCallbacksAndMessages(null);
    }

    // ══════════════════════════════════════════════════════════════
    //  ০. টপ আইডেন্টিটি বার — হ্যামবার্গার + সালাম/নাম/সময়/তারিখ স্লাইড + নোটিফিকেশন/প্রোফাইল
    // ══════════════════════════════════════════════════════════════
    private void setupTopIdentityBar() {
        headerMenuBtn    = findViewById(R.id.headerMenuBtn);
        headerNotifBtn   = findViewById(R.id.headerNotifBtn);
        headerProfileBtn = findViewById(R.id.headerProfileBtn);
        headerIdentityFlipper = findViewById(R.id.headerIdentityFlipper);
        headerAiFlipper       = findViewById(R.id.headerAiFlipper);

        // হ্যামবার্গার — প্রোফেশনাল নেভিগেশন ড্রয়ার খোলে (আগে সরাসরি SettingsActivity খুলত)
        headerMenuBtn.setOnClickListener(v -> {
            if (drawerLayout != null) drawerLayout.openDrawer(Gravity.START);
        });

        // নোটিফিকেশন — আপাতত নতুন নোটিফিকেশন থাকলে জানাবে (অ্যাডমিন লিসেনার আলাদাভাবে ডায়ালগ দেখায়)
        headerNotifBtn.setOnClickListener(v ->
                Toast.makeText(this, "কোনো নতুন নোটিফিকেশন নেই", Toast.LENGTH_SHORT).show());

        // প্রোফাইল
        headerProfileBtn.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        buildIdentityFlipper();
    }

    // ══════════════════════════════════════════════════════════════
    //  ০.৫ প্রোফেশনাল নেভিগেশন ড্রয়ার — হেডার (ছবি/নাম/Google একাউন্ট) + মেনু রো-গুলো
    // ══════════════════════════════════════════════════════════════
    private void setupNavDrawer() {
        drawerLayout = findViewById(R.id.dashboardDrawerLayout);
        if (drawerLayout == null) return;

        refreshDrawerHeader();

        // হেডারে ট্যাপ করলে প্রোফাইল পেজ
        View headerRoot = ((View) ((TextView) findViewById(R.id.drawerProfileName)).getParent());
        headerRoot.setOnClickListener(v -> {
            drawerLayout.closeDrawer(Gravity.START);
            startActivity(new Intent(this, ProfileActivity.class));
        });

        // ── মেনু রো ক্লিক লিসেনার ──
        setDrawerRowClick(R.id.drawerItemFbGroup, v ->
                openExternalUrl(getString(R.string.drawer_facebook_group_url)));

        setDrawerRowClick(R.id.drawerItemBackup, v ->
                startActivity(new Intent(this, BackupCenterActivity.class)));

        setDrawerRowClick(R.id.drawerItemSettings, v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        setDrawerRowClick(R.id.drawerItemTerms, v ->
                openExternalUrl(getString(R.string.drawer_terms_url)));

        setDrawerRowClick(R.id.drawerItemPrivacy, v ->
                openExternalUrl(getString(R.string.drawer_privacy_policy_url)));

        setDrawerRowClick(R.id.drawerItemFeedback, v -> {
            Intent email = new Intent(Intent.ACTION_SENDTO);
            email.setData(android.net.Uri.parse("mailto:" + getString(R.string.drawer_support_email)));
            email.putExtra(Intent.EXTRA_SUBJECT, "CashLipi — মতামত");
            try {
                startActivity(email);
            } catch (Exception e) {
                Toast.makeText(this, "কোনো ইমেইল অ্যাপ পাওয়া যায়নি", Toast.LENGTH_SHORT).show();
            }
        });

        setDrawerRowClick(R.id.drawerItemFaq, v ->
                openExternalUrl(getString(R.string.drawer_faq_url)));

        setDrawerRowClick(R.id.drawerItemOtherApps, v ->
                openExternalUrl("https://play.google.com/store/apps/dev?id=" + getPackageName()));

        setDrawerRowClick(R.id.drawerItemDeveloper, v ->
                startActivity(new Intent(this, AboutActivity.class)));

        setDrawerRowClick(R.id.drawerItemSignOut, v ->
                new AlertDialog.Builder(this)
                        .setTitle("বের হন")
                        .setMessage("আপনি কি সাইন আউট করতে চান? এতে লোকাল ডাটা মুছে যাবে না।")
                        .setPositiveButton("হ্যাঁ, বের হন", (d, w) -> {
                            com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
                            db.clearGoogleAccount();
                            drawerLayout.closeDrawer(Gravity.START);
                            Intent i = new Intent(this, LoginActivity.class);
                            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(i);
                            finish();
                        })
                        .setNegativeButton("বাতিল", null)
                        .show());
    }

    private void setDrawerRowClick(int viewId, View.OnClickListener action) {
        View row = findViewById(viewId);
        if (row == null) return;
        row.setOnClickListener(v -> {
            if (drawerLayout != null) drawerLayout.closeDrawer(Gravity.START);
            action.onClick(v);
        });
    }

    /** প্রোফাইল থেকে ফিরে এলে নাম/ছবি/একাউন্ট আপ-টু-ডেট থাকতে onResume থেকেও কল হয়। */
    private void refreshDrawerHeader() {
        ImageView photo   = findViewById(R.id.drawerProfilePhoto);
        TextView initials = findViewById(R.id.drawerProfileInitials);
        TextView name     = findViewById(R.id.drawerProfileName);
        TextView account  = findViewById(R.id.drawerProfileAccount);
        if (photo == null || initials == null || name == null || account == null) return;

        String displayName = db.getDisplayName();
        name.setText(displayName == null || displayName.trim().isEmpty() ? "ব্যবহারকারী" : displayName);

        String accountLine = db.isGoogleSignedIn() && !db.getGoogleEmail().isEmpty()
                ? db.getGoogleEmail()
                : (!db.getUserEmail().isEmpty() ? db.getUserEmail() : "সাইন ইন করা নেই");
        account.setText(accountLine);

        String photoSource = db.getEffectivePhotoSource();
        if (photoSource != null && !photoSource.isEmpty()) {
            Object loadFrom = photoSource.startsWith("http")
                    ? photoSource : new java.io.File(photoSource);
            Glide.with(this)
                    .load(loadFrom)
                    .transform(new com.bumptech.glide.load.resource.bitmap.CircleCrop())
                    .placeholder(android.R.drawable.ic_menu_myplaces)
                    .error(android.R.drawable.ic_menu_myplaces)
                    .into(photo);
            photo.setVisibility(View.VISIBLE);
            initials.setVisibility(View.GONE);
        } else {
            String initialsText;
            String n = displayName == null ? "" : displayName.trim();
            if (n.isEmpty()) initialsText = "👤";
            else {
                String[] parts = n.split("\\s+");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < parts.length && sb.length() < 2; i++)
                    if (!parts[i].isEmpty()) sb.append(parts[i].charAt(0));
                initialsText = sb.length() > 0 ? sb.toString().toUpperCase() : "👤";
            }
            initials.setText(initialsText);
            photo.setVisibility(View.GONE);
            initials.setVisibility(View.VISIBLE);
        }
    }

    private void openExternalUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "লিংকটি খোলা যায়নি", Toast.LENGTH_SHORT).show();
        }
    }

    // সালাম (সময়ভিত্তিক) → CashLipi → সময় → তারিখ — একের পর এক স্লাইড
    private void buildIdentityFlipper() {
        headerIdentityFlipper.removeAllViews();
        List<String> slides = new ArrayList<>();
        slides.add(greetingByTime());
        slides.add("CashLipi");
        slides.add(currentTimeText());
        slides.add(currentDateText());

        for (String text : slides) {
            TextView tv = new TextView(this);
            tv.setText(text);
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(13.5f);
            tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
            tv.setMaxLines(1);
            tv.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            tv.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            headerIdentityFlipper.addView(tv);
        }
    }

    private String greetingByTime() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) return "সুপ্রভাত!";
        if (hour < 16) return "শুভ দুপুর!";
        if (hour < 19) return "শুভ বিকাল!";
        return "শুভ সন্ধ্যা!";
    }

    private String currentTimeText() {
        return new SimpleDateFormat("hh:mm a", new Locale("bn")).format(new java.util.Date());
    }

    private String currentDateText() {
        return new SimpleDateFormat("dd MMM, yyyy", new Locale("bn")).format(new java.util.Date());
    }

    private void startIdentityAutoSlide() {
        identityHandler.removeCallbacksAndMessages(null);
        identityHandler.postDelayed(new Runnable() {
            @Override public void run() {
                if (headerIdentityFlipper != null) {
                    // সময়/তারিখ স্লাইড দেখানোর ঠিক আগে টেক্সট রিফ্রেশ করা হয় যাতে সবসময় বর্তমান সময় দেখায়
                    int next = (headerIdentityFlipper.getDisplayedChild() + 1) % headerIdentityFlipper.getChildCount();
                    if (next == 2 && headerIdentityFlipper.getChildAt(2) instanceof TextView) {
                        ((TextView) headerIdentityFlipper.getChildAt(2)).setText(currentTimeText());
                    }
                    headerIdentityFlipper.showNext();
                }
                identityHandler.postDelayed(this, 2600);
            }
        }, 2600);
    }

    // ══════════════════════════════════════════════════════════════
    //  ০.১ চিকন AI-advice স্লাইড — headerSliderSlot-এ, সবসময় দৃশ্যমান
    // ══════════════════════════════════════════════════════════════
    private void refreshAiAdviceTips() {
        if (headerAiFlipper == null) return;
        double income  = db.getTotalIncome();
        double expense = db.getTotalExpense();
        double savings = db.getTotalSavings();
        double dena    = db.getTotalDena();
        double pabona  = db.getTotalPabona();
        double balance = db.getBalance();

        List<String> tips = new ArrayList<>();
        if (income == 0 && expense == 0) {
            tips.add("লেনদেন যোগ করুন, AI আপনাকে স্মার্ট পরামর্শ দেবে!");
        } else {
            if (income > 0 && expense > 0) {
                double ratio = expense / income * 100;
                if (ratio > 90) tips.add("আয়ের " + (int) ratio + "% ব্যয় হচ্ছে! সঞ্চয় বাড়ানো দরকার।");
                else if (ratio > 70) tips.add("আয়ের " + (int) ratio + "% ব্যয় হচ্ছে। নিয়ন্ত্রণে রাখুন।");
                else tips.add("চমৎকার! আয়ের মাত্র " + (int) ratio + "% ব্যয় হচ্ছে।");
            }
            if (savings == 0) tips.add("সঞ্চয় শূন্য! আজই সঞ্চয় শুরু করুন।");
            else tips.add("সঞ্চয় " + DatabaseManager.formatAmount(savings) + " — দারুণ অভ্যাস!");
            if (dena > 0) tips.add("দেনা " + DatabaseManager.formatAmount(dena) + " বাকি। পরিশোধের পরিকল্পনা করুন।");
            if (pabona > 0) tips.add("পাওনা " + DatabaseManager.formatAmount(pabona) + " — সংগ্রহ করতে ভুলবেন না।");
            if (balance < 0) tips.add("ব্যালেন্স নেগেটিভ! অপ্রয়োজনীয় ব্যয় কমান।");
        }
        if (tips.isEmpty()) tips.add("স্মার্ট পরামর্শ লোড হচ্ছে...");

        headerAiFlipper.removeAllViews();
        for (String tip : tips) {
            TextView tv = new TextView(this);
            tv.setText("💡 " + tip);
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(11.5f);
            tv.setMaxLines(1);
            tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
            tv.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            tv.setPadding(dp(16), 0, dp(16), 0);
            tv.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            headerAiFlipper.addView(tv);
        }
        headerAiFlipper.setOnClickListener(v ->
                startActivity(new Intent(this, com.jrappspot.cashlipi.activities.AiChatActivity.class)));
    }

    private void startAiAdviceAutoSlide() {
        aiAdviceHandler.removeCallbacksAndMessages(null);
        if (headerAiFlipper == null) return;
        aiAdviceHandler.postDelayed(new Runnable() {
            @Override public void run() {
                if (headerAiFlipper != null && headerAiFlipper.getChildCount() > 1) {
                    headerAiFlipper.showNext();
                }
                aiAdviceHandler.postDelayed(this, 4000);
            }
        }, 4000);
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }


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
        headerRow = findViewById(R.id.headerRow);
        topBlockDivider = findViewById(R.id.topBlockDivider);
        headerAddBtn = findViewById(R.id.headerAddBtn);
        bottomFab = findViewById(R.id.bottomFab);

        // ৭-আইকন নেভ বার একবারই inflate হয় — পরে position অনুযায়ী topNavSlot/bottomNavSlot-এ রিপ্যারেন্ট হয়
        topNavBar = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.nav_bar_items, topNavSlot, false);
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
        }
        int navBgColor;
        try {
            navBgColor = Color.parseColor(db.getNavBgColor());
        } catch (IllegalArgumentException e) {
            navBgColor = ContextCompat.getColor(this, R.color.bottomNavBg);
        }
        NavBarStyler.applyItemSelection(this, navItems, navIndicators, selectedPosition, currentNavStyle, navBgColor);
        // "যোগ"-গ্যাপ স্লটে (বাকির খাতা, বটম-পজিশনে) ইন্ডিকেটর কখনোই দেখানো হয় না
        if (navSavingsGapActive && navIndicators[MainPagerAdapter.POSITION_BAKIR_KHATA] != null) {
            navIndicators[MainPagerAdapter.POSITION_BAKIR_KHATA].setVisibility(View.GONE);
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
        currentNavStyle = db.getNavStyle();
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

        // ── লেবেল "নিচে" পজিশনে বা "মিনিমাল" স্টাইলে সবসময় দেখানো হয় — স্পষ্টতার জন্য ──
        boolean showLabels = bottomPosition || NavBarStyler.STYLE_MINIMAL.equals(currentNavStyle);
        for (TextView label : navLabels) {
            if (label == null) continue;
            label.setVisibility(showLabels ? View.VISIBLE : View.GONE);
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
            headerRow.setVisibility(View.GONE);
            topBlockDivider.setVisibility(View.GONE);
            // drawCradle=true — বারের নিজের সিলুয়েটেই + বাটনের জন্য সত্যিকারের বাঁকানো কাটআউট নচ আঁকা হয়
            NavBarStyler.applyBarBackground(this, bottomNavSlot, currentNavStyle, navBgColor, true, true);
            bottomFab.setVisibility(View.VISIBLE);
            // পুরনো "পেজ-রঙা গোল কুশন" হ্যাক আর দরকার নেই — বারের ড্রয়েবলই এখন আসল গর্ত কেটে আঁকে
            if (fabNotchBackdrop != null) {
                fabNotchBackdrop.setVisibility(View.GONE);
            }
        } else {
            if (currentParent != topNavSlot) {
                if (currentParent != null) currentParent.removeView(topNavBar);
                topNavSlot.addView(topNavBar, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            bottomNavContainer.setVisibility(View.GONE);
            headerRow.setVisibility(View.VISIBLE);
            headerRow.setBackgroundColor(NavBarStyler.effectiveBarColor(this, currentNavStyle, navBgColor));
            topBlockDivider.setVisibility(View.VISIBLE);
            headerAddBtn.setVisibility(View.VISIBLE);
            NavBarStyler.applyBarBackground(this, topNavSlot, currentNavStyle, navBgColor, false);
        }

        // ── ৩. সোয়াইপ অন/অফ ──
        viewPager.setUserInputEnabled(db.isNavSwipeEnabled());

        // ── ৪. + বাটনের ঠিক নিচে যে আইটেমটা পড়ে (বাকির খাতা, ৭টার ৪র্থ/মাঝেরটা) তাকে "যোগ"-গ্যাপে বদলে দেওয়া ──
        updateGapMode(bottomPosition);

        // ── ৫. স্টাইল/পজিশন বদলের পর সিলেক্টেড আইটেমের পিল/গ্লো রিফ্রেশ ──
        if (viewPager != null) updateNavSelection(viewPager.getCurrentItem());
    }

    /**
     * নিচের নেভবারে + বাটন ঠিক মাঝের আইটেম ("বাকির খাতা")-এর উপর ভাসে (nav_bar_items.xml-এ ৪র্থ)।
     * বটম-পজিশনে সেই স্লটের আইকন লুকিয়ে, লেবেল "যোগ" করে, ট্যাপ করলে যোগ-মেন্যু খোলে (বাকির খাতা
     * পেজে যায় না) — + বাটনেরই অংশ মনে হয়। সঞ্চয় এখন সবসময় স্বাভাবিক/দৃশ্যমান/ট্যাপযোগ্য থাকে।
     * উপরের পজিশনে ফিরলে "বাকির খাতা" আবার স্বাভাবিক আইকন/লেবেল/নেভিগেশনে ফিরে আসে (top position-এ
     * পুরো ৭টা আইকনই স্বাভাবিকভাবে অ্যাক্সেসযোগ্য থাকে)।
     */
    private void updateGapMode(boolean bottomPosition) {
        int i = MainPagerAdapter.POSITION_BAKIR_KHATA;
        if (navItems[i] == null) return;
        navSavingsGapActive = bottomPosition; // নাম পুরনো রাখা হলো, এখন এটা "গ্যাপ স্লট" বোঝায় (বাকির খাতা)

        if (bottomPosition) {
            if (navIcons[i] != null) navIcons[i].setVisibility(View.INVISIBLE);
            if (navLabels[i] != null) navLabels[i].setText(R.string.nav_label_fab_gap);
            if (navIndicators[i] != null) navIndicators[i].setVisibility(View.GONE);
            navItems[i].setOnClickListener(v -> toggleFabMenu(bottomFab));
        } else {
            if (navIcons[i] != null) navIcons[i].setVisibility(View.VISIBLE);
            if (navLabels[i] != null) navLabels[i].setText(R.string.nav_label_bakir_khata);
            if (navIndicators[i] != null) navIndicators[i].setVisibility(View.INVISIBLE);
            navItems[i].setOnClickListener(v -> viewPager.setCurrentItem(i, true));
        }
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

    /** ৩টা অ্যাকশন-আইটেম (বড় রঙিন গোল আইকন + নিচে ছোট লেবেল) পাশাপাশি সাজিয়ে container-এ যোগ করে —
     *  সেটিং অপশনটা এখান থেকে সরিয়ে দেওয়া হয়েছে, শুধু আয়-ব্যয় / দেনা-পাওনা / সঞ্চয় থাকবে। */
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
    }

    /** একটা আইটেম = বড় গোল আইকন বাটন, ঠিক নিচে ছোট লেবেল — উল্লম্ব মিনি-স্ট্যাক, যা container-এ পাশাপাশি বসে। */
    private void addFabMenuItem(LinearLayout container, int iconRes, String label, int colorRes, Runnable action) {
        LinearLayout stack = new LinearLayout(this);
        stack.setOrientation(LinearLayout.VERTICAL);
        stack.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams stackLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int sideMargin = getResources().getDimensionPixelSize(R.dimen.fab_menu_item_spacing);
        stackLp.setMarginStart(sideMargin);
        stackLp.setMarginEnd(sideMargin);
        stack.setLayoutParams(stackLp);

        ImageView iconBtn = new ImageView(this);
        int size = getResources().getDimensionPixelSize(R.dimen.fab_menu_icon_size);
        iconBtn.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        int pad = getResources().getDimensionPixelSize(R.dimen.fab_menu_icon_padding);
        iconBtn.setPadding(pad, pad, pad, pad);
        iconBtn.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_circle_solid));
        iconBtn.setBackgroundTintList(ContextCompat.getColorStateList(this, colorRes));
        iconBtn.setImageResource(iconRes);
        iconBtn.setColorFilter(ContextCompat.getColor(this, R.color.white));
        iconBtn.setElevation(8f);

        TextView labelChip = new TextView(this);
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        labelLp.topMargin = getResources().getDimensionPixelSize(R.dimen.fab_menu_label_top_margin);
        labelChip.setLayoutParams(labelLp);
        FontUtils.applyToView(this, labelChip);
        labelChip.setText(label);
        applyStyleFabMiniLabel(labelChip);

        stack.addView(iconBtn);
        stack.addView(labelChip);

        View.OnClickListener stackClick = v -> {
            closeFabMenu();
            action.run();
        };
        stack.setOnClickListener(stackClick);
        iconBtn.setOnClickListener(stackClick);

        container.addView(stack);
    }

    private void applyStyleFabMiniLabel(TextView tv) {
        int padH = getResources().getDimensionPixelSize(R.dimen.fab_menu_label_pad_h);
        int padV = getResources().getDimensionPixelSize(R.dimen.fab_menu_label_pad_v);
        tv.setPadding(padH, padV, padH, padV);
        tv.setTextSize(11f);
        tv.setMaxLines(1);
        tv.setTextColor(ContextCompat.getColor(this, R.color.white));
        tv.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_fab_mini_label));
        tv.setElevation(8f);
    }

    private void toggleFabMenu(ImageButton anchor) {
        if (fabMenuOpen) {
            closeFabMenu();
        } else {
            openFabMenu(anchor);
        }
    }

    /**
     * ফ্যান-আউট মেন্যু খোলে — আইটেমগুলো পাশাপাশি (horizontal), + বাটনের ঠিক উপরে একটা বাঁকানো/আর্ক
     * আকৃতিতে ফুটে ওঠে (মাঝেরগুলো একটু বেশি উপরে ওঠে, দুই পাশেরগুলো একটু কম — arcOffset ফর্মুলা দিয়ে)।
     * সাথে (বটম-পজিশনে) নেভবারের বাম-অর্ধেক আর ডান-অর্ধেক + বাটনকে কেন্দ্র করে হালকা ফাঁক হয়ে সরে যায় —
     * রেফারেন্স মাইক্রো-ইন্টার‍্যাকশনের মতো, "সবকিছু + থেকেই ছড়িয়ে পড়ছে" এই অনুভূতি দেয়।
     */
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

        int count = activeContainer.getChildCount();
        float amplitude = getResources().getDimensionPixelSize(R.dimen.fab_menu_arc_amplitude);
        float mid = (count - 1) / 2f;

        for (int i = 0; i < count; i++) {
            View item = activeContainer.getChildAt(i);
            float normalized = mid == 0 ? 0 : (i - mid) / mid;   // -1..1, ০ মাঝখানে
            float arcLift = amplitude * (1f - normalized * normalized); // মাঝেরটা সবচেয়ে বেশি উপরে

            item.setAlpha(0f);
            item.setScaleX(0.3f);
            item.setScaleY(0.3f);
            item.setTranslationY(50f);
            item.animate()
                    .alpha(1f).scaleX(1f).scaleY(1f).translationY(-arcLift)
                    .setStartDelay(i * 50L)
                    .setDuration(260)
                    .setInterpolator(new OvershootInterpolator(1.15f))
                    .start();
        }

        if (bottomPosition) animateNavSplit(true);
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

        if ("bottom".equals(db.getNavPosition())) animateNavSplit(false);
    }

    /**
     * নিচের নেভবারের + বাটনের বাঁ-পাশের আইটেমগুলো (হোম, আয়-ব্যয়, দেনা-পাওনা) বাম দিকে আর ডান-পাশের
     * আইটেমগুলো (সঞ্চয়, বাজেট, সেটিং) ডান দিকে হালকা সরে গিয়ে + বাটনের চারপাশে ফাঁক তৈরি করে —
     * open=true হলে ফাঁক হয় (স্প্লিট), open=false হলে আবার জোড়া লেগে যায়। মাঝের গ্যাপ আইটেম
     * (বাকির খাতা, + বাটনের ঠিক নিচে) সরে না, তাই referencePoint হিসেবে ওটার child-index ব্যবহার হয়।
     */
    private void animateNavSplit(boolean open) {
        if (topNavBar == null) return;
        int childCount = topNavBar.getChildCount();
        int gapIndex = -1;
        for (int i = 0; i < childCount; i++) {
            if (topNavBar.getChildAt(i).getId() == R.id.navBakirKhata) {
                gapIndex = i;
                break;
            }
        }
        if (gapIndex == -1) return;

        float splitDistance = getResources().getDimensionPixelSize(R.dimen.fab_menu_arc_amplitude) * 0.55f;
        for (int i = 0; i < childCount; i++) {
            View child = topNavBar.getChildAt(i);
            float targetX;
            if (i < gapIndex) {
                targetX = open ? -splitDistance : 0f;
            } else if (i > gapIndex) {
                targetX = open ? splitDistance : 0f;
            } else {
                continue; // মাঝের গ্যাপ আইটেম নিজে সরে না
            }
            child.animate()
                    .translationX(targetX)
                    .setDuration(open ? 240 : 200)
                    .setInterpolator(open ? new OvershootInterpolator(1.0f) : new android.view.animation.DecelerateInterpolator())
                    .start();
        }
    }

    // ── HELPERS ──────────────────────────────────────────────────────
    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(Gravity.START)) {
            drawerLayout.closeDrawer(Gravity.START);
            return;
        }
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
