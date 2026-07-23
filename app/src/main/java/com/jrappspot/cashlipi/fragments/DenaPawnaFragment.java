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
import com.jrappspot.cashlipi.activities.AddPersonActivity;
import com.jrappspot.cashlipi.activities.PersonDetailActivity;
import com.jrappspot.cashlipi.adapters.PersonAdapter;
import com.jrappspot.cashlipi.models.LedgerEntry;
import com.jrappspot.cashlipi.models.Person;
import com.jrappspot.cashlipi.models.PersonStat;
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
 * ব্যক্তি (Person) ও দেনা-পাওনা এন্ট্রি (LedgerEntry) এখনও আলাদা মডেল — এন্ট্রির person
 * ফিল্ডে থাকা নাম Person.getName()-এর সাথে মিলিয়ে (case-insensitive) সারসংক্ষেপ বানানো হয়।
 */
public class DenaPawnaFragment extends Fragment {

    private DatabaseManager db;
    private RecyclerView rvList;
    private LinearLayout emptyState, noResultState;
    private TextView tvHeaderTitle, tvPersonCount;
    private EditText etSearch;
    private ImageView ivClearSearch;
    private FrameLayout btnFilter, btnThemeChange;
    private View filterActiveDot;

    private FrameLayout bannerContainer;
    private LinearLayout bannerCardRoot;
    private TextView tvBannerInitial, tvBannerName, tvBannerSub, tvBannerAmount, tvBannerLabel;
    private LinearLayout debtDots;
    private final Handler bannerHandler = new Handler(Looper.getMainLooper());
    private final List<Person> bannerPersons = new ArrayList<>();
    private int bannerIndex = 0;

    private final List<Person> allPersons = new ArrayList<>();
    private final Map<String, PersonStat> statsMap = new HashMap<>();

    private String currentQuery = "";
    private int currentFilter = 0; // 0 = সব, 1 = অপরিশোধিত, 2 = পরিশোধিত

    private static final String PREFS_NAME = "cashlipi_dena_pawna_prefs";
    private static final String KEY_CARD_STYLE = "card_style";
    private static final String[] STYLE_NAMES = {
            "ক্লাসিক", "মিনিমাল", "গ্র্যাডিয়েন্ট", "বোল্ড", "কমপ্যাক্ট"
    };
    private SharedPreferences prefs;
    private int cardStyle = PersonAdapter.STYLE_CLASSIC;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dena_pawna, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);
        db = DatabaseManager.getInstance(requireContext());
        prefs = requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
        cardStyle = prefs.getInt(KEY_CARD_STYLE, PersonAdapter.STYLE_CLASSIC);

        rvList = root.findViewById(R.id.rvPersonList);
        emptyState = root.findViewById(R.id.emptyState);
        noResultState = root.findViewById(R.id.noResultState);
        tvHeaderTitle = root.findViewById(R.id.tvHeaderTitle);
        tvPersonCount = root.findViewById(R.id.tvPersonCount);
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

        rvList.setLayoutManager(new LinearLayoutManager(requireContext()));

        root.findViewById(R.id.btnAddPerson).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddPersonActivity.class)));

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

    // ── Person ও LedgerEntry দুইটা আলাদা মডেল, নাম মিলিয়ে জোড়া লাগানো হয় (person.id
    //    সংযোগ নেই) — তাই সাধারণ trim/lowercase যথেষ্ট না, কারণ ইউজার মাঝে মাঝে একাধিক
    //    স্পেস দেয় বা non-breaking space (\u00A0) কিবোর্ড থেকে ঢুকে যায়, যেটা .trim() মুছে
    //    না। এসব normalize না করলে ব্যক্তির নাম আর লেনদেনের নাম না মেলায় ব্যানার/ব্যাজে
    //    ভুল (খালি/সাদা) ফলাফল দেখা যায়। সব জায়গায় এই একই ফাংশন দিয়ে key বানানো হয়।
    private static String normalizeKey(String raw) {
        if (raw == null) return "";
        return raw.replace('\u00A0', ' ')      // non-breaking space → সাধারণ স্পেস
                .trim()
                .replaceAll("\\s+", " ")        // একাধিক স্পেস → একটা
                .toLowerCase(Locale.ROOT);
    }


    private void loadData() {
        allPersons.clear();
        allPersons.addAll(db.getPersonList());
        statsMap.clear();

        List<LedgerEntry> allLedger = db.getLedgerList();
        for (Person p : allPersons) {
            String key = normalizeKey(p.getName());
            PersonStat stat = statsMap.get(key);
            if (stat == null) { stat = new PersonStat(); statsMap.put(key, stat); }
            for (LedgerEntry e : allLedger) {
                if (!normalizeKey(e.getPerson()).equals(key)) continue;
                stat.totalCount++;
                if (!e.isPaid()) {
                    stat.unpaidCount++;
                    if ("dena".equals(e.getType())) stat.unpaidDena += e.getAmount();
                    else stat.unpaidPabona += e.getAmount();
                }
            }
        }

        int totalTxn = 0;
        for (PersonStat s : statsMap.values()) totalTxn += s.totalCount;

        tvHeaderTitle.setText("ব্যক্তি ও প্রতিষ্ঠান: " + allPersons.size());
        if (totalTxn == 0) {
            tvPersonCount.setText("মোট লেনদেন নেই");
        } else {
            tvPersonCount.setText("মোট লেনদেন: " + totalTxn);
        }

        setupBanner();
        applyFilters();
    }

    // ── উপরের অটো-স্লাইড ব্যানার — শুধু অপরিশোধিত-সহ ব্যক্তিদের নিয়ে ──
    private void setupBanner() {
        bannerHandler.removeCallbacksAndMessages(null);

        bannerPersons.clear();
        for (Person p : allPersons) {
            PersonStat s = statsMap.get(normalizeKey(p.getName()));
            if (s != null && s.hasUnpaid()) bannerPersons.add(p);
        }
        // যার বকেয়া সবচেয়ে বেশি সে আগে দেখাবে
        bannerPersons.sort((a, b) -> {
            PersonStat sa = statsMap.get(normalizeKey(a.getName()));
            PersonStat sb = statsMap.get(normalizeKey(b.getName()));
            double na = sa != null ? sa.getNetAmount() : 0;
            double nb = sb != null ? sb.getNetAmount() : 0;
            return Double.compare(nb, na);
        });

        if (bannerPersons.isEmpty()) {
            bannerContainer.setVisibility(View.GONE);
            return;
        }

        bannerContainer.setVisibility(View.VISIBLE);
        debtDots.removeAllViews();
        for (int i = 0; i < bannerPersons.size(); i++) {
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
        if (bannerPersons.size() > 1) startBannerAutoSlide();
    }

    /**
     * bannerCardRoot XML-এই স্ট্যাটিকভাবে ডিফাইন করা এবং সবসময় বিদ্যমান — এখানে শুধু তার
     * background ও টেক্সট বদলানো হয়, কখনও কোনো ভিউ যোগ/বাদ দেওয়া হয় না। তাই কোনো অবস্থাতেই
     * কার্ডটা খালি/সাদা দেখানোর সুযোগ নেই। পরিবর্তনের সময় (animate=true) শুধু একটা হালকা
     * scale/translate "পপ" ইফেক্ট হয় — background/content কখনও অস্বচ্ছতা হারায় না।
     */
    private void showBannerCard(int index, boolean animate) {
        if (bannerCardRoot == null || index < 0 || index >= bannerPersons.size()) return;
        Person p = bannerPersons.get(index);

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

    private void bindBannerCard(Person p) {
        PersonStat s = statsMap.get(normalizeKey(p.getName()));
        if (s == null) s = new PersonStat();
        boolean isDena = s.isNetDena();

        // দেনা ও পাওনার জন্য সঠিক ব্যাকগ্রাউন্ড সরাসরি bannerCardRoot-এ সেট করা হয় — এটা
        // সবসময়ই বিদ্যমান কোনো একটা background নিয়ে থাকে (কখনও transparent/blank নয়)।
        bannerCardRoot.setBackgroundResource(
                isDena ? R.drawable.bg_debt_banner_dena : R.drawable.bg_debt_banner_pabona);

        tvBannerInitial.setText(p.getInitial());
        tvBannerName.setText(p.getName().isEmpty() ? "নাম নেই" : p.getName());
        tvBannerSub.setText(s.unpaidCount + " টি অপরিশোধিত এন্ট্রি" + (p.hasRelation() ? " • " + p.getRelation() : ""));
        tvBannerLabel.setText(isDena ? "আপনি দেবেন" : "আপনি পাবেন");
        double amount = s.getNetAmount() > 0 ? s.getNetAmount() : Math.max(s.unpaidDena, s.unpaidPabona);
        tvBannerAmount.setText(DatabaseManager.formatAmount(amount));

        bannerCardRoot.setOnClickListener(v -> openPerson(p));
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
                if (bannerCardRoot != null && bannerPersons.size() > 1) {
                    bannerIndex = (bannerIndex + 1) % bannerPersons.size();
                    showBannerCard(bannerIndex, true);
                    updateBannerDots(bannerIndex);
                }
                bannerHandler.postDelayed(this, 5000);
            }
        }, 5000);
    }

    private void openPerson(Person person) {
        Intent i = new Intent(requireContext(), PersonDetailActivity.class);
        i.putExtra(PersonDetailActivity.EXTRA_PERSON_ID, person.getId());
        startActivity(i);
    }

    // ── সার্চ + ফিল্টার প্রয়োগ করে তালিকা আপডেট ────────────────────
    private void applyFilters() {
        if (allPersons.isEmpty()) {
            rvList.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            noResultState.setVisibility(View.GONE);
            return;
        }

        String q = currentQuery.toLowerCase(Locale.ROOT);
        List<Person> filtered = new ArrayList<>();
        for (Person p : allPersons) {
            boolean matchesQuery = q.isEmpty()
                    || p.getName().toLowerCase(Locale.ROOT).contains(q)
                    || p.getPhone().toLowerCase(Locale.ROOT).contains(q)
                    || p.getRelation().toLowerCase(Locale.ROOT).contains(q);
            if (!matchesQuery) continue;

            PersonStat stat = statsMap.get(normalizeKey(p.getName()));
            boolean matchesFilter;
            if (currentFilter == 1) matchesFilter = stat != null && stat.hasUnpaid();
            else if (currentFilter == 2) matchesFilter = stat != null && stat.isFullyPaid();
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

        PersonAdapter adapter = new PersonAdapter(requireContext(), filtered,
                (person, position) -> openPerson(person), statsMap, cardStyle);
        rvList.setAdapter(adapter);
        rvList.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_fall_down));
        rvList.scheduleLayoutAnimation();
    }
}
