package com.jrappspot.cashlipi.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.activities.AccountingActivity;
import com.jrappspot.cashlipi.activities.AddSavingsActivity;
import com.jrappspot.cashlipi.activities.AiChatActivity;
import com.jrappspot.cashlipi.activities.AnalysisActivity;
import com.jrappspot.cashlipi.activities.CalculatorActivity;
import com.jrappspot.cashlipi.activities.ExpenseListActivity;
import com.jrappspot.cashlipi.activities.IncomeListActivity;
import com.jrappspot.cashlipi.activities.LedgerListActivity;
import com.jrappspot.cashlipi.activities.NotesActivity;
import com.jrappspot.cashlipi.utils.DatabaseManager;

import java.util.ArrayList;
import java.util.List;

/**
 * হোম পেজ — নতুন ডিজাইন:
 *  ১) উপরে অটো-স্লাইডিং AI পরামর্শ কার্ড (৪ সেকেন্ড পরপর বদলায়, নিচে ডট ইন্ডিকেটর)
 *  ২) পিংক ব্যালেন্স কার্ড
 *  ৩) আয় + ব্যয়
 *  ৪) দেনা + পাওনা  (আগের "লোন পেলাম/দিলাম" এর বদলে)
 *  ৫) চিকন সঞ্চয় বার
 *  ৬) কুইক মেনু গ্রিড — AI Chat / এনালাইসিস / বাকির খাতা / ক্যালকুলেটর / নোট / একাউন্টিং
 */
public class HomeFragment extends Fragment {

    private DatabaseManager db;

    private TextView tvMainBalance, tvTotalIncome, tvTotalExpense;
    private TextView tvTotalDena, tvTotalPabona, tvTotalSavings;

    private ViewFlipper tipFlipper;
    private LinearLayout tipDots;

    // ── ব্যালেন্স দেখা/লুকানো (চোখ আইকন) ───────────────────────────────
    private ImageView balanceEyeToggle;
    private double currentBalance = 0;
    private boolean balanceHidden = false;
    private static final String PREFS_NAME = "cashlipi_home_prefs";
    private static final String KEY_BALANCE_HIDDEN = "balance_hidden";

    private final Handler tipHandler = new Handler(Looper.getMainLooper());
    private int tipCount = 0;

    private static final class TipData {
        final String subtitle;
        final int iconRes;
        final int bgRes;
        TipData(String subtitle, int iconRes, int bgRes) {
            this.subtitle = subtitle;
            this.iconRes = iconRes;
            this.bgRes = bgRes;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = DatabaseManager.getInstance(requireContext());
        initViews(view);
        setupClickListeners(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDashboard();
        startAutoSlide();
    }

    @Override
    public void onPause() {
        super.onPause();
        tipHandler.removeCallbacksAndMessages(null);
    }

    private void initViews(View view) {
        tvMainBalance  = view.findViewById(R.id.tvMainBalance);
        tvTotalIncome  = view.findViewById(R.id.tvTotalIncome);
        tvTotalExpense = view.findViewById(R.id.tvTotalExpense);
        tvTotalDena    = view.findViewById(R.id.tvTotalDena);
        tvTotalPabona  = view.findViewById(R.id.tvTotalPabona);
        tvTotalSavings = view.findViewById(R.id.tvTotalSavings);
        tipFlipper     = view.findViewById(R.id.tipFlipper);
        tipDots        = view.findViewById(R.id.tipDots);
        balanceEyeToggle = view.findViewById(R.id.balanceEyeToggle);

        balanceHidden = requireContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_BALANCE_HIDDEN, false);

        balanceEyeToggle.setOnClickListener(v -> {
            balanceHidden = !balanceHidden;
            requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().putBoolean(KEY_BALANCE_HIDDEN, balanceHidden).apply();
            applyBalanceVisibility();
        });
    }

    // ── ব্যালেন্স মাস্ক করা/দেখানো টেক্সট + আইকন আপডেট ─────────────────
    private void applyBalanceVisibility() {
        if (tvMainBalance == null) return;
        if (balanceHidden) {
            tvMainBalance.setText("৳ ***");
            balanceEyeToggle.setImageResource(R.drawable.ic_eye_off);
        } else {
            tvMainBalance.setText(DatabaseManager.formatAmount(currentBalance));
            balanceEyeToggle.setImageResource(R.drawable.ic_eye);
        }
    }

    private void loadDashboard() {
        double income  = db.getTotalIncome();
        double expense = db.getTotalExpense();
        double savings = db.getTotalSavings();
        double dena    = db.getTotalDena();
        double pabona  = db.getTotalPabona();
        double balance = db.getBalance();

        currentBalance = balance;
        tvMainBalance.setTextColor(0xFFFFFFFF);
        applyBalanceVisibility();

        tvTotalIncome.setText(DatabaseManager.formatAmount(income));
        tvTotalExpense.setText(DatabaseManager.formatAmount(expense));
        tvTotalDena.setText(DatabaseManager.formatAmount(dena));
        tvTotalPabona.setText(DatabaseManager.formatAmount(pabona));
        tvTotalSavings.setText(DatabaseManager.formatAmount(savings));

        buildTipCarousel(income, expense, savings, dena, pabona, balance);
    }

    // ── AI TIP CAROUSEL — real-data-based smart tips, one card per tip ─────
    private void buildTipCarousel(double income, double expense, double savings,
                                   double dena, double pabona, double balance) {
        List<TipData> tips = new ArrayList<>();

        if (income == 0 && expense == 0) {
            tips.add(new TipData("লেনদেন যোগ করুন, AI আপনাকে স্মার্ট পরামর্শ দেবে!",
                    R.drawable.emoji_bulb, R.drawable.bg_tip_card_purple));
        } else {
            if (income > 0 && expense > 0) {
                double ratio = expense / income * 100;
                if (ratio > 90)
                    tips.add(new TipData("আয়ের " + (int) ratio + "% ব্যয় হচ্ছে! সঞ্চয় বাড়ানো দরকার।",
                            R.drawable.emoji_warning, R.drawable.bg_tip_card_rose));
                else if (ratio > 70)
                    tips.add(new TipData("আয়ের " + (int) ratio + "% ব্যয় হচ্ছে। নিয়ন্ত্রণে রাখুন।",
                            R.drawable.emoji_bulb, R.drawable.bg_tip_card_orange));
                else
                    tips.add(new TipData("চমৎকার! আয়ের মাত্র " + (int) ratio + "% ব্যয় হচ্ছে।",
                            R.drawable.emoji_check_mark_green, R.drawable.bg_tip_card_green));
            }
            if (savings == 0)
                tips.add(new TipData("সঞ্চয় শূন্য! আজই সঞ্চয় শুরু করুন।",
                        R.drawable.emoji_green_heart, R.drawable.bg_tip_card_orange));
            else
                tips.add(new TipData("সঞ্চয় " + DatabaseManager.formatAmount(savings) + " — দারুণ অভ্যাস!",
                        R.drawable.emoji_green_heart, R.drawable.bg_tip_card_green));
            if (dena > 0)
                tips.add(new TipData("দেনা " + DatabaseManager.formatAmount(dena) + " বাকি। আজই পরিশোধের পরিকল্পনা করুন।",
                        R.drawable.emoji_book_red, R.drawable.bg_tip_card_rose));
            if (pabona > 0)
                tips.add(new TipData("পাওনা " + DatabaseManager.formatAmount(pabona) + " — সংগ্রহ করতে ভুলবেন না।",
                        R.drawable.emoji_book_green, R.drawable.bg_tip_card_blue));
            if (balance < 0)
                tips.add(new TipData("ব্যালেন্স নেগেটিভ! অপ্রয়োজনীয় ব্যয় কমান।",
                        R.drawable.emoji_warning, R.drawable.bg_tip_card_rose));
            else if (balance > 0 && income > 0)
                tips.add(new TipData("ব্যালেন্স ইতিবাচক। বিনিয়োগের কথা ভাবুন!",
                        R.drawable.emoji_chart_up, R.drawable.bg_tip_card_purple));
        }

        if (tips.isEmpty())
            tips.add(new TipData("স্মার্ট পরামর্শ লোড হচ্ছে...",
                    R.drawable.emoji_bulb, R.drawable.bg_tip_card_purple));

        tipFlipper.removeAllViews();
        tipDots.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (TipData tip : tips) {
            View card = inflater.inflate(R.layout.item_tip_card, tipFlipper, false);
            card.setBackgroundResource(tip.bgRes);
            ((ImageView) card.findViewById(R.id.tipIcon)).setImageResource(tip.iconRes);
            ((TextView) card.findViewById(R.id.tipTitle)).setText("স্মার্ট পরামর্শ");
            ((TextView) card.findViewById(R.id.tipSubtitle)).setText(tip.subtitle);
            tipFlipper.addView(card);

            View dot = new View(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(3, 0, 3, 0);
            dot.setLayoutParams(lp);
            dot.setBackgroundResource(R.drawable.dot_inactive);
            tipDots.addView(dot);
        }

        tipCount = tips.size();
        tipFlipper.setDisplayedChild(0);
        updateDots(0);
    }

    private void updateDots(int activeIndex) {
        for (int i = 0; i < tipDots.getChildCount(); i++) {
            tipDots.getChildAt(i).setBackgroundResource(
                    i == activeIndex ? R.drawable.dot_active : R.drawable.dot_inactive);
        }
    }

    // ── AUTO-SLIDE — advances the tip carousel every 4 seconds ────────
    private void startAutoSlide() {
        tipHandler.removeCallbacksAndMessages(null);
        tipHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (tipFlipper != null && tipCount > 1) {
                    tipFlipper.showNext();
                    updateDots(tipFlipper.getDisplayedChild());
                }
                tipHandler.postDelayed(this, 4000);
            }
        }, 4000);
    }

    // ── CLICK LISTENERS ──────────────────────────────────────────────
    private void setupClickListeners(View root) {
        root.findViewById(R.id.cardIncome).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), IncomeListActivity.class)));
        root.findViewById(R.id.cardExpense).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ExpenseListActivity.class)));
        root.findViewById(R.id.cardDena).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), LedgerListActivity.class)));
        root.findViewById(R.id.cardPabona).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), LedgerListActivity.class)));
        root.findViewById(R.id.cardSavings).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddSavingsActivity.class)));

        root.findViewById(R.id.balanceCardClick).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AnalysisActivity.class)));
        root.findViewById(R.id.balanceCard).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AnalysisActivity.class)));

        root.findViewById(R.id.tipFlipper).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AiChatActivity.class)));

        // Quick menu grid
        root.findViewById(R.id.menuAiChat).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AiChatActivity.class)));
        root.findViewById(R.id.menuAnalysis).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AnalysisActivity.class)));
        root.findViewById(R.id.menuLedger).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), LedgerListActivity.class)));
        root.findViewById(R.id.menuCalculator).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CalculatorActivity.class)));
        root.findViewById(R.id.menuNotes).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), NotesActivity.class)));
        root.findViewById(R.id.menuAccounting).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AccountingActivity.class)));
    }
}
