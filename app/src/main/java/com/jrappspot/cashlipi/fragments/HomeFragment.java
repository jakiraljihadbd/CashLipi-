package com.jrappspot.cashlipi.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.activities.AddSavingsActivity;
import com.jrappspot.cashlipi.activities.AiChatActivity;
import com.jrappspot.cashlipi.activities.AnalysisActivity;
import com.jrappspot.cashlipi.activities.ExpenseListActivity;
import com.jrappspot.cashlipi.activities.IncomeListActivity;
import com.jrappspot.cashlipi.activities.LedgerListActivity;
import com.jrappspot.cashlipi.utils.DatabaseManager;

import java.util.ArrayList;
import java.util.List;

/**
 * হোম পেজ — ব্যালেন্স কার্ড, সামারি কার্ডসমূহ (আয়/ব্যয়/দেনা/পাওনা/সঞ্চয়), AI টিপ ব্যানার।
 * বিদ্যমান DashboardActivity-এর ডিজাইন ও লজিক হুবহু অক্ষত রেখে Fragment-এ রূপান্তরিত করা হয়েছে।
 * (পুরনো headerSlideBar/ticker/greeting/internet-row অংশটুকু বাদ দেওয়া হয়েছে —
 *  সেই ভূমিকা এখন নতুন গ্লোবাল টপ-হেডারের টাইটেল-সাইকেল অ্যানিমেশন পালন করে।)
 */
public class HomeFragment extends Fragment {

    private DatabaseManager db;

    private TextView tvMainBalance, tvTotalIncome, tvTotalExpense;
    private TextView tvTotalDena, tvTotalPabona, tvTotalSavings, tvAiTip;

    private final Handler tipHandler = new Handler(Looper.getMainLooper());
    private int tipIndex = 0;
    private String[] aiTips;

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
        startAiTips();
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
        tvAiTip        = view.findViewById(R.id.tvAiTip);
    }

    private void loadDashboard() {
        double income  = db.getTotalIncome();
        double expense = db.getTotalExpense();
        double savings = db.getTotalSavings();
        double dena    = db.getTotalDena();
        double pabona  = db.getTotalPabona();
        double balance = db.getBalance();

        // Balance — WHITE always (positive/negative both white per spec)
        tvMainBalance.setText(DatabaseManager.formatAmount(balance));
        tvMainBalance.setTextColor(0xFFFFFFFF);

        tvTotalIncome.setText(DatabaseManager.formatAmount(income));
        tvTotalExpense.setText(DatabaseManager.formatAmount(expense));
        tvTotalDena.setText(DatabaseManager.formatAmount(dena));
        tvTotalPabona.setText(DatabaseManager.formatAmount(pabona));
        tvTotalSavings.setText(DatabaseManager.formatAmount(savings));

        // Build smart AI tips based on real data
        buildAiTips(income, expense, savings, dena, pabona, balance);
    }

    // ── AI TIPS — smart tips based on transaction data ────────────────
    private void buildAiTips(double income, double expense, double savings,
                              double dena, double pabona, double balance) {
        List<String> tips = new ArrayList<>();

        if (income == 0 && expense == 0) {
            tips.add("লেনদেন যোগ করুন, AI আপনাকে স্মার্ট পরামর্শ দেবে!");
        } else {
            if (income > 0 && expense > 0) {
                double ratio = expense / income * 100;
                if (ratio > 90)
                    tips.add(" আয়ের " + (int) ratio + "% ব্যয় হচ্ছে! সঞ্চয় বাড়ানো দরকার।");
                else if (ratio > 70)
                    tips.add(" আয়ের " + (int) ratio + "% ব্যয় হচ্ছে। নিয়ন্ত্রণে রাখুন।");
                else
                    tips.add(" চমৎকার! আয়ের মাত্র " + (int) ratio + "% ব্যয় হচ্ছে।");
            }
            if (savings == 0)
                tips.add(" প্রতি মাসে অন্তত ১০% আয় সঞ্চয় করার অভ্যাস করুন।");
            else
                tips.add(" সঞ্চয় " + DatabaseManager.formatAmount(savings) + " — দারুণ অভ্যাস!");
            if (dena > 0)
                tips.add(" দেনা " + DatabaseManager.formatAmount(dena) + " বাকি। আজই পরিশোধের পরিকল্পনা করুন।");
            if (pabona > 0)
                tips.add(" পাওনা " + DatabaseManager.formatAmount(pabona) + " — সংগ্রহ করতে ভুলবেন না।");
            if (balance < 0)
                tips.add(" ব্যালেন্স নেগেটিভ! অপ্রয়োজনীয় ব্যয় কমান।");
            else if (balance > 0 && income > 0)
                tips.add(" ব্যালেন্স ইতিবাচক। বিনিয়োগের কথা ভাবুন!");
        }

        if (tips.isEmpty()) tips.add("স্মার্ট পরামর্শ লোড হচ্ছে...");
        aiTips = tips.toArray(new String[0]);
        tipIndex = 0;
        if (tvAiTip != null && aiTips.length > 0)
            tvAiTip.setText(aiTips[0]);
    }

    private void startAiTips() {
        tipHandler.removeCallbacksAndMessages(null);
        tipHandler.postDelayed(() -> {
            if (aiTips == null || aiTips.length <= 1) return;
            tipIndex = (tipIndex + 1) % aiTips.length;
            if (tvAiTip != null) {
                AlphaAnimation fade = new AlphaAnimation(0f, 1f);
                fade.setDuration(500);
                tvAiTip.setText(aiTips[tipIndex]);
                tvAiTip.startAnimation(fade);
            }
            startAiTips();
        }, 5000);
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

        root.findViewById(R.id.aiTipBanner).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AiChatActivity.class)));
    }
}
