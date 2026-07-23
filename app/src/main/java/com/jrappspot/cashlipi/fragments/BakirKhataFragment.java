package com.jrappspot.cashlipi.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.activities.AddKhataCustomerActivity;
import com.jrappspot.cashlipi.activities.KhataCustomerDetailActivity;
import com.jrappspot.cashlipi.adapters.KhataCustomerAdapter;
import com.jrappspot.cashlipi.models.KhataEntry;
import com.jrappspot.cashlipi.models.KhataCustomer;
import com.jrappspot.cashlipi.models.KhataCustomerStat;
import com.jrappspot.cashlipi.utils.DatabaseManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * দেনা-পাওনা পেজ — এখানে ব্যবহারকারী তার সাথে হিসাব রাখা প্রতিটি ব্যক্তি/প্রতিষ্ঠান যোগ করে।
 *
 * এই রিডিজাইনে যোগ হয়েছে:
 *  ১) উপরে অটো-স্লাইড রঙিন ব্যানার — কোন কোন ব্যক্তির টাকা এখনও অপরিশোধিত তা পালা করে দেখায়
 *  ২) সার্চ বক্স (নাম/ফোন)
 *  ৩) ফিল্টার চিপ — সব / অপরিশোধিত / পরিশোধিত
 *  ৪) প্রতিটি কার্ডে মোট লেনদেন ও অপরিশোধিত এন্ট্রি সংখ্যার ব্যাজ
 *
 * ব্যক্তি (KhataCustomer) ও দেনা-পাওনা এন্ট্রি (KhataEntry) এখনও আলাদা মডেল — এন্ট্রির customerName
 * ফিল্ডে থাকা নাম KhataCustomer.getName()-এর সাথে মিলিয়ে (case-insensitive) সারসংক্ষেপ বানানো হয়।
 */
public class BakirKhataFragment extends Fragment {

    private DatabaseManager db;
    private RecyclerView rvList;
    private LinearLayout emptyState, noResultState;
    private TextView tvHeaderTitle, tvKhataCustomerCount;
    private EditText etSearch;
    private ImageView ivClearSearch;
    private FrameLayout btnFilter, btnThemeChange;
    private View filterActiveDot;

    private FrameLayout bannerContainer;
    private LinearLayout bannerCardRoot;
    private TextView tvBannerInitial, tvBannerName, tvBannerSub, tvBannerAmount, tvBannerLabel;
    private LinearLayout debtDots;
    private TextView tvKhataWalletBalance;
    private final Handler bannerHandler = new Handler(Looper.getMainLooper());
    private final List<KhataCustomer> bannerKhataCustomers = new ArrayList<>();
    private int bannerIndex = 0;

    private final List<KhataCustomer> allKhataCustomers = new ArrayList<>();
    private final Map<String, KhataCustomerStat> statsMap = new HashMap<>();

    private String currentQuery = "";
    private int currentFilter = 0; // 0 = সব, 1 = অপরিশোধিত, 2 = পরিশোধিত

    private static final String PREFS_NAME = "cashlipi_bakir_khata_prefs";
    private static final String KEY_CARD_STYLE = "card_style";
    private static final String[] STYLE_NAMES = {
            "ক্লাসিক", "মিনিমাল", "গ্র্যাডিয়েন্ট", "বোল্ড", "কমপ্যাক্ট"
    };
    private SharedPreferences prefs;
    private int cardStyle = KhataCustomerAdapter.STYLE_CLASSIC;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bakir_khata, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);
        db = DatabaseManager.getInstance(requireContext());
        prefs = requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
        cardStyle = prefs.getInt(KEY_CARD_STYLE, KhataCustomerAdapter.STYLE_CLASSIC);

        rvList = root.findViewById(R.id.rvKhataCustomerList);
        emptyState = root.findViewById(R.id.emptyState);
        noResultState = root.findViewById(R.id.noResultState);
        tvHeaderTitle = root.findViewById(R.id.tvHeaderTitle);
        tvKhataCustomerCount = root.findViewById(R.id.tvKhataCustomerCount);
        etSearch = root.findViewById(R.id.etSearch);
        ivClearSearch = root.findViewById(R.id.ivClearSearch);
        btnFilter = root.findViewById(R.id.btnFilter);
        btnThemeChange = root.findViewById(R.id.btnThemeChange);
        filterActiveDot = root.findViewById(R.id.filterActiveDot);
        bannerContainer = root.findViewById(R.id.bannerContainer);
        bannerCardRoot = root.findViewById(R.id.bannerCardRoot);
        tvBannerInitial = root.findViewById(R.id.tvBannerInitial);
        tvBannerName = root.findViewById(R.id.tvBannerName);
        tvBannerSub = root.findViewById(R.id.tvBannerSub);
        tvBannerAmount = root.findViewById(R.id.tvBannerAmount);
        tvBannerLabel = root.findViewById(R.id.tvBannerLabel);
        debtDots = root.findViewById(R.id.debtDots);
        tvKhataWalletBalance = root.findViewById(R.id.tvKhataWalletBalance);

        rvList.setLayoutManager(new LinearLayoutManager(requireContext()));

        root.findViewById(R.id.btnAddKhataCustomer).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddKhataCustomerActivity.class)));

        root.findViewById(R.id.chipKhataWallet).setOnClickListener(v -> showWalletDialog());
        root.findViewById(R.id.btnKhataAddExpense).setOnClickListener(v -> showAddExpenseDialog());
        root.findViewById(R.id.btnKhataAddExpense).setOnLongClickListener(v -> {
            startActivity(new Intent(requireContext(), com.jrappspot.cashlipi.activities.KhataExpenseActivity.class));
            return true;
        });
        root.findViewById(R.id.btnKhataAddJoma).setOnClickListener(v -> showFundWalletDialog());

        setupSearch();
        setupFilterAndTheme();

        loadData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData(); // নতুন ব্যক্তি/লেনদেন যোগ-এডিট-মুছার পর তালিকা রিফ্রেশ
    }

    @Override
    public void onPause() {
        super.onPause();
        bannerHandler.removeCallbacksAndMessages(null);
    }

    // ── সার্চ ────────────────────────────────────────────────────────
    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                currentQuery = s.toString().trim();
                ivClearSearch.setVisibility(currentQuery.isEmpty() ? View.GONE : View.VISIBLE);
                applyFilters();
            }
        });
        ivClearSearch.setOnClickListener(v -> etSearch.setText(""));
    }

    // ── ফিল্টার আইকন — পপআপ মেনুতে সব / অপরিশোধিত / পরিশোধিত ────────
    // ── থিম-চেঞ্জ আইকন — কার্ডের ৫টি ভিজুয়াল স্টাইলের একটা বাছাই ─────
    private void setupFilterAndTheme() {
        btnFilter.setOnClickListener(this::showFilterMenu);
        btnThemeChange.setOnClickListener(v -> showThemeDialog());
        updateFilterDot();
    }

    private void showFilterMenu(View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        Menu m = menu.getMenu();
        m.add(Menu.NONE, 0, 0, "সব");
        m.add(Menu.NONE, 1, 1, "অপরিশোধিত");
        m.add(Menu.NONE, 2, 2, "পরিশোধিত");
        menu.setOnMenuItemClickListener((MenuItem item) -> {
            selectFilter(item.getItemId());
            return true;
        });
        menu.show();
    }

    private void selectFilter(int filter) {
        currentFilter = filter;
        updateFilterDot();
        applyFilters();
    }

    private void updateFilterDot() {
        filterActiveDot.setVisibility(currentFilter != 0 ? View.VISIBLE : View.GONE);
    }

    private void showThemeDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("কার্ডের থিম বেছে নিন")
                .setSingleChoiceItems(STYLE_NAMES, cardStyle, (dialog, which) -> {
                    cardStyle = which;
                    prefs.edit().putInt(KEY_CARD_STYLE, cardStyle).apply();
                    applyFilters();
                    dialog.dismiss();
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    // ── ডেটা লোড + প্রতি-গ্রাহক সারসংক্ষেপ হিসাব ───────────────────
    private void loadData() {
        allKhataCustomers.clear();
        allKhataCustomers.addAll(db.getKhataCustomerList());
        statsMap.clear();

        List<KhataEntry> allKhataEntry = db.getKhataEntryList();
        for (KhataCustomer p : allKhataCustomers) {
            String key = p.getId();
            KhataCustomerStat stat = statsMap.get(key);
            if (stat == null) { stat = new KhataCustomerStat(); statsMap.put(key, stat); }
            for (KhataEntry e : allKhataEntry) {
                if (!key.equals(e.getCustomerId())) continue;
                stat.totalCount++;
                if (e.isBaki()) stat.totalBaki += e.getAmount();
                else stat.totalJoma += e.getAmount();
            }
        }

        int totalTxn = 0;
        for (KhataCustomerStat s : statsMap.values()) totalTxn += s.totalCount;

        tvHeaderTitle.setText("গ্রাহক: " + allKhataCustomers.size());
        if (totalTxn == 0) {
            tvKhataCustomerCount.setText("মোট লেনদেন নেই");
        } else {
            tvKhataCustomerCount.setText("মোট লেনদেন: " + totalTxn);
        }

        setupBanner();
        applyFilters();
        refreshWalletChip();
    }

    private void refreshWalletChip() {
        if (tvKhataWalletBalance != null) {
            tvKhataWalletBalance.setText("৳" + DatabaseManager.formatAmount(db.getKhataWalletBalance()));
        }
    }

    // ── ওয়ালেট ব্যালেন্স দেখা + মূল ব্যালেন্স থেকে/এ স্থানান্তর ──────────
    private void showWalletDialog() {
        double walletBalance = db.getKhataWalletBalance();
        double mainBalance = db.getBalance();

        String message = "ওয়ালেট ব্যালেন্স: ৳" + DatabaseManager.formatAmount(walletBalance)
                + "\nমূল একাউন্ট ব্যালেন্স: ৳" + DatabaseManager.formatAmount(mainBalance)
                + "\n\nওয়ালেট আপনার মূল ব্যালেন্স থেকে সম্পূর্ণ আলাদা একটা পকেট। এখান থেকে "
                + "চাইলে কিছু বা পুরো টাকা মূল ব্যালেন্সে ফেরত নেওয়া যায়, আবার মূল ব্যালেন্স "
                + "থেকেও ওয়ালেটে টাকা আনা যায়।";

        new AlertDialog.Builder(requireContext())
                .setTitle("বিজনেস ওয়ালেট")
                .setMessage(message)
                .setPositiveButton("মূল ব্যালেন্স থেকে জমা করুন", (d, w) -> showFundWalletDialog())
                .setNeutralButton("ওয়ালেট থেকে ফেরত নিন", (d, w) -> showWithdrawWalletDialog())
                .setNegativeButton("বন্ধ করুন", null)
                .show();
    }

    private EditText buildAmountInput(String hint) {
        EditText et = new EditText(requireContext());
        et.setHint(hint);
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        et.setPadding(pad, pad, pad, pad);
        return et;
    }

    private void showFundWalletDialog() {
        EditText et = buildAmountInput("কত টাকা ওয়ালেটে আনবেন? (মূল ব্যালেন্স ৳"
                + DatabaseManager.formatAmount(db.getBalance()) + ")");
        new AlertDialog.Builder(requireContext())
                .setTitle("মূল ব্যালেন্স থেকে ওয়ালেটে জমা")
                .setView(et)
                .setPositiveButton("জমা করুন", (d, w) -> {
                    double amt = parseAmount(et);
                    if (amt <= 0) return;
                    if (db.fundWalletFromMainBalance(amt, "")) {
                        android.widget.Toast.makeText(requireContext(), "ওয়ালেটে জমা হয়েছে", android.widget.Toast.LENGTH_SHORT).show();
                        loadData();
                    }
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    private void showWithdrawWalletDialog() {
        EditText et = buildAmountInput("কত টাকা ফেরত নেবেন? (ওয়ালেটে আছে ৳"
                + DatabaseManager.formatAmount(db.getKhataWalletBalance()) + ")");
        new AlertDialog.Builder(requireContext())
                .setTitle("ওয়ালেট থেকে মূল ব্যালেন্সে ফেরত")
                .setView(et)
                .setPositiveButton("ফেরত নিন", (d, w) -> {
                    double amt = parseAmount(et);
                    if (amt <= 0) return;
                    if (!db.withdrawWalletToMainBalance(amt, "")) {
                        android.widget.Toast.makeText(requireContext(), "ওয়ালেটে এত টাকা নেই", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    android.widget.Toast.makeText(requireContext(), "মূল ব্যালেন্সে ফেরত নেওয়া হয়েছে", android.widget.Toast.LENGTH_SHORT).show();
                    loadData();
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    // ── বিজনেস খরচ যোগ করা (ক্যাটাগরি বেছে নিয়ে) ──────────────────────
    private void showAddExpenseDialog() {
        List<String> categories = db.getKhataExpenseCategories();
        String[] catArr = categories.toArray(new String[0]);
        final int[] selected = {0};

        EditText et = buildAmountInput("খরচের পরিমাণ");

        new AlertDialog.Builder(requireContext())
                .setTitle("ব্যবসায়িক খরচ যোগ করুন")
                .setSingleChoiceItems(catArr, 0, (d, which) -> selected[0] = which)
                .setView(et)
                .setPositiveButton("সংরক্ষণ করুন", (d, w) -> {
                    double amt = parseAmount(et);
                    if (amt <= 0) return;
                    com.jrappspot.cashlipi.models.KhataExpense e = new com.jrappspot.cashlipi.models.KhataExpense();
                    e.setCategory(catArr.length > 0 ? catArr[selected[0]] : "অন্যান্য");
                    e.setAmount(amt);
                    e.setPayFromWallet(db.getKhataWalletBalance() >= amt);
                    db.addKhataExpense(e);
                    android.widget.Toast.makeText(requireContext(), "খরচ যোগ হয়েছে", android.widget.Toast.LENGTH_SHORT).show();
                    loadData();
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    private double parseAmount(EditText et) {
        try {
            double v = Double.parseDouble(et.getText().toString().trim());
            return v > 0 ? v : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // ── উপরের অটো-স্লাইড ব্যানার — শুধু বাকি-থাকা গ্রাহকদের নিয়ে ──
    private void setupBanner() {
        bannerHandler.removeCallbacksAndMessages(null);

        bannerKhataCustomers.clear();
        for (KhataCustomer p : allKhataCustomers) {
            KhataCustomerStat s = statsMap.get(p.getId());
            if (s != null && s.isDue()) bannerKhataCustomers.add(p);
        }
        // যার বকেয়া সবচেয়ে বেশি সে আগে দেখাবে
        bannerKhataCustomers.sort((a, b) -> {
            KhataCustomerStat sa = statsMap.get(a.getId());
            KhataCustomerStat sb = statsMap.get(b.getId());
            double na = sa != null ? sa.getNetAmount() : 0;
            double nb = sb != null ? sb.getNetAmount() : 0;
            return Double.compare(nb, na);
        });

        if (bannerKhataCustomers.isEmpty()) {
            bannerContainer.setVisibility(View.GONE);
            return;
        }

        bannerContainer.setVisibility(View.VISIBLE);
        debtDots.removeAllViews();
        for (int i = 0; i < bannerKhataCustomers.size(); i++) {
            View dot = new View(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(3, 0, 3, 0);
            dot.setLayoutParams(lp);
            dot.setBackgroundResource(R.drawable.dot_inactive);
            debtDots.addView(dot);
        }

        bannerIndex = 0;
        showBannerCard(bannerIndex, false);
        updateBannerDots(bannerIndex);
        if (bannerKhataCustomers.size() > 1) startBannerAutoSlide();
    }

    /**
     * bannerCardRoot XML-এই স্ট্যাটিকভাবে ডিফাইন করা এবং সবসময় বিদ্যমান — এখানে শুধু তার
     * background ও টেক্সট বদলানো হয়, কখনও কোনো ভিউ যোগ/বাদ দেওয়া হয় না। তাই কোনো অবস্থাতেই
     * কার্ডটা খালি/সাদা দেখানোর সুযোগ নেই (দেনা-পাওনা পেজেও একই বাগ ছিল, একইভাবে ঠিক করা হলো)।
     * পরিবর্তনের সময় (animate=true) শুধু একটা হালকা scale/translate "পপ" ইফেক্ট হয় —
     * background/content কখনও অস্বচ্ছতা হারায় না।
     */
    private void showBannerCard(int index, boolean animate) {
        if (bannerCardRoot == null || index < 0 || index >= bannerKhataCustomers.size()) return;
        KhataCustomer p = bannerKhataCustomers.get(index);

        Runnable applyContent = () -> bindBannerCard(p);

        if (!animate) {
            applyContent.run();
            return;
        }

        bannerCardRoot.animate().cancel();
        bannerCardRoot.animate()
                .translationY(-6f)
                .setDuration(120)
                .withEndAction(() -> {
                    applyContent.run();
                    bannerCardRoot.setTranslationY(-6f);
                    bannerCardRoot.animate().translationY(0f).setDuration(160).start();
                }).start();
    }

    private void bindBannerCard(KhataCustomer p) {
        KhataCustomerStat s = statsMap.get(p.getId());
        if (s == null) s = new KhataCustomerStat();
        boolean isDue = s.isDue();

        // বাকি ও জমা-বেশি (অগ্রিম) এর জন্য সঠিক ব্যাকগ্রাউন্ড সরাসরি bannerCardRoot-এ সেট করা
        // হয় — এটা সবসময়ই বিদ্যমান কোনো একটা background নিয়ে থাকে (কখনও transparent/blank নয়)।
        bannerCardRoot.setBackgroundResource(
                isDue ? R.drawable.bg_debt_banner_khata : R.drawable.bg_debt_banner_khata_joma);

        tvBannerInitial.setText(p.getInitial());
        tvBannerName.setText(p.getName().isEmpty() ? "নাম নেই" : p.getName());
        tvBannerSub.setText(s.totalCount + " টি লেনদেন" + (p.hasBusinessTag() ? " • " + p.getBusinessTag() : ""));
        tvBannerLabel.setText(isDue ? "গ্রাহক বাকি আছেন" : "অগ্রিম জমা আছে");
        tvBannerAmount.setText(DatabaseManager.formatAmount(s.getNetAmount()));

        bannerCardRoot.setOnClickListener(v -> openKhataCustomer(p));
    }

    private void updateBannerDots(int activeIndex) {
        for (int i = 0; i < debtDots.getChildCount(); i++) {
            debtDots.getChildAt(i).setBackgroundResource(
                    i == activeIndex ? R.drawable.dot_active : R.drawable.dot_inactive);
        }
    }

    private void startBannerAutoSlide() {
        bannerHandler.removeCallbacksAndMessages(null);
        bannerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (bannerCardRoot != null && bannerKhataCustomers.size() > 1) {
                    bannerIndex = (bannerIndex + 1) % bannerKhataCustomers.size();
                    showBannerCard(bannerIndex, true);
                    updateBannerDots(bannerIndex);
                }
                bannerHandler.postDelayed(this, 5000);
            }
        }, 5000);
    }

    private void openKhataCustomer(KhataCustomer customerName) {
        Intent i = new Intent(requireContext(), KhataCustomerDetailActivity.class);
        i.putExtra(KhataCustomerDetailActivity.EXTRA_PERSON_ID, customerName.getId());
        startActivity(i);
    }

    // ── সার্চ + ফিল্টার প্রয়োগ করে তালিকা আপডেট ────────────────────
    private void applyFilters() {
        if (allKhataCustomers.isEmpty()) {
            rvList.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            noResultState.setVisibility(View.GONE);
            return;
        }

        String q = currentQuery.toLowerCase(Locale.ROOT);
        List<KhataCustomer> filtered = new ArrayList<>();
        for (KhataCustomer p : allKhataCustomers) {
            boolean matchesQuery = q.isEmpty()
                    || p.getName().toLowerCase(Locale.ROOT).contains(q)
                    || p.getPhone().toLowerCase(Locale.ROOT).contains(q)
                    || p.getBusinessTag().toLowerCase(Locale.ROOT).contains(q);
            if (!matchesQuery) continue;

            KhataCustomerStat stat = statsMap.get(p.getId());
            boolean matchesFilter;
            if (currentFilter == 1) matchesFilter = stat != null && stat.isDue();
            else if (currentFilter == 2) matchesFilter = stat != null && stat.isSettled() && stat.hasAnyTxn();
            else matchesFilter = true;

            if (matchesFilter) filtered.add(p);
        }

        if (filtered.isEmpty()) {
            rvList.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
            noResultState.setVisibility(View.VISIBLE);
            return;
        }

        emptyState.setVisibility(View.GONE);
        noResultState.setVisibility(View.GONE);
        rvList.setVisibility(View.VISIBLE);

        KhataCustomerAdapter adapter = new KhataCustomerAdapter(requireContext(), filtered,
                (customerName, position) -> openKhataCustomer(customerName), statsMap, cardStyle);
        rvList.setAdapter(adapter);
        rvList.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_fall_down));
        rvList.scheduleLayoutAnimation();
    }
}
