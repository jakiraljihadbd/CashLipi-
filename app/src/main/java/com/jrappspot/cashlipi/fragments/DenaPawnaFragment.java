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
import android.widget.ViewFlipper;

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
    private ViewFlipper debtFlipper;
    private LinearLayout debtDots;
    private final Handler bannerHandler = new Handler(Looper.getMainLooper());
    private int bannerCount = 0;

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
        debtFlipper = root.findViewById(R.id.debtFlipper);
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

    // ── ডেটা লোড + প্রতি-ব্যক্তি সারসংক্ষেপ হিসাব ───────────────────
    private void loadData() {
        allPersons.clear();
        allPersons.addAll(db.getPersonList());
        statsMap.clear();

        List<LedgerEntry> allLedger = db.getLedgerList();
        for (Person p : allPersons) {
            String key = p.getName().trim().toLowerCase(Locale.ROOT);
            PersonStat stat = statsMap.get(key);
            if (stat == null) { stat = new PersonStat(); statsMap.put(key, stat); }
            for (LedgerEntry e : allLedger) {
                if (!e.getPerson().trim().toLowerCase(Locale.ROOT).equals(key)) continue;
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

        List<Person> unpaidPersons = new ArrayList<>();
        for (Person p : allPersons) {
            PersonStat s = statsMap.get(p.getName().trim().toLowerCase(Locale.ROOT));
            if (s != null && s.hasUnpaid()) unpaidPersons.add(p);
        }
        // যার বকেয়া সবচেয়ে বেশি সে আগে দেখাবে
        unpaidPersons.sort((a, b) -> {
            PersonStat sa = statsMap.get(a.getName().trim().toLowerCase(Locale.ROOT));
            PersonStat sb = statsMap.get(b.getName().trim().toLowerCase(Locale.ROOT));
            double na = sa != null ? sa.getNetAmount() : 0;
            double nb = sb != null ? sb.getNetAmount() : 0;
            return Double.compare(nb, na);
        });

        if (unpaidPersons.isEmpty()) {
            bannerContainer.setVisibility(View.GONE);
            return;
        }

        bannerContainer.setVisibility(View.VISIBLE);
        debtFlipper.removeAllViews();
        debtDots.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (Person p : unpaidPersons) {
            PersonStat s = statsMap.get(p.getName().trim().toLowerCase(Locale.ROOT));
            if (s == null) s = new PersonStat();

            View card = inflater.inflate(R.layout.item_debt_banner, debtFlipper, false);
            // bannerCardRoot-এর background XML-এ ডিফল্ট (bg_debt_banner_solid) হিসেবে থাকে,
            // তারপর এখানে ইনফ্লেট হওয়ার সাথে সাথেই দেনা/পাওনা অনুযায়ী নির্দিষ্ট রং বসানো
            // হয় — তাই কখনও ফাঁকা/সাদা দেখা যায় না, view যোগ হওয়ার আগেই রং ঠিক থাকে।
            boolean isDena = s.isNetDena();
            View bannerCardRoot = card.findViewById(R.id.bannerCardRoot);
            bannerCardRoot.setBackgroundResource(isDena ? R.drawable.bg_debt_banner_dena : R.drawable.bg_debt_banner_pabona);

            ((TextView) card.findViewById(R.id.tvBannerInitial)).setText(p.getInitial());
            ((TextView) card.findViewById(R.id.tvBannerName)).setText(
                    p.getName().isEmpty() ? "নাম নেই" : p.getName());
            ((TextView) card.findViewById(R.id.tvBannerSub)).setText(
                    s.unpaidCount + " টি অপরিশোধিত এন্ট্রি" + (p.hasRelation() ? " • " + p.getRelation() : ""));

            ((TextView) card.findViewById(R.id.tvBannerLabel)).setText(isDena ? "আপনি দেবেন" : "আপনি পাবেন");
            double amount = s.getNetAmount() > 0 ? s.getNetAmount() : Math.max(s.unpaidDena, s.unpaidPabona);
            ((TextView) card.findViewById(R.id.tvBannerAmount)).setText(DatabaseManager.formatAmount(amount));

            Person finalP = p;
            card.setOnClickListener(v -> openPerson(finalP));
            debtFlipper.addView(card);

            View dot = new View(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(3, 0, 3, 0);
            dot.setLayoutParams(lp);
            dot.setBackgroundResource(R.drawable.dot_inactive);
            debtDots.addView(dot);
        }

        bannerCount = unpaidPersons.size();
        debtFlipper.setDisplayedChild(0);
        updateBannerDots(0);
        if (bannerCount > 1) startBannerAutoSlide();
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
                if (debtFlipper != null && bannerCount > 1) {
                    debtFlipper.showNext();
                    updateBannerDots(debtFlipper.getDisplayedChild());
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

            PersonStat stat = statsMap.get(p.getName().trim().toLowerCase(Locale.ROOT));
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
