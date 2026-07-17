package com.jrappspot.cashlipi.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.activities.AddTransactionActivity;
import com.jrappspot.cashlipi.activities.AnalysisActivity;
import com.jrappspot.cashlipi.adapters.IncomeExpenseCardAdapter;
import com.jrappspot.cashlipi.models.Transaction;
import com.jrappspot.cashlipi.utils.DatabaseManager;
import com.jrappspot.cashlipi.utils.DateFilterUtil;
import com.jrappspot.cashlipi.utils.TransactionSheetHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * আয়-ব্যয় পেজ — উপরে আয়/ব্যয় টগল দিয়ে দুটো তালিকার মধ্যে সুইচ করা যায়।
 * কার্ড ভিউ — সম্পাদনা/মুছুন/আরও বাটনসহ। ছক ভিউ — মাসিক-ভিত্তিক, শুধু দেখার জন্য (এডিট নেই)।
 */
public class IncomeExpenseFragment extends Fragment {

    private static final String[] BN_MONTHS = {
            "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
            "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    };

    private DatabaseManager db;
    private RecyclerView rv;
    private LinearLayout emptyState, tableMonthsContainer, chipRowLayout, monthSummaryCard;
    private View tableContainer;
    private EditText etSearch;
    private TextView tvMonthAmount, tvMonthTitle;
    private ImageView ivMonthIcon;
    private TextView tabIncome, tabExpense, btnViewCard, btnViewTable, btnViewAnalysis;
    private List<Transaction> allList = new ArrayList<>();
    private List<Transaction> filteredList = new ArrayList<>();
    private String currentFilter = "all";
    private String currentType = "income"; // "income" | "expense"
    private String viewMode = "card"; // "card" | "table"
    private View rootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_income_expense, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);
        db = DatabaseManager.getInstance(requireContext());
        rootView = root;
        rv = root.findViewById(R.id.rvList);
        emptyState = root.findViewById(R.id.emptyState);
        tableContainer = root.findViewById(R.id.tableContainer);
        tableMonthsContainer = root.findViewById(R.id.tableMonthsContainer);
        etSearch = root.findViewById(R.id.etSearch);
        tvMonthTitle = root.findViewById(R.id.tvMonthTitle);
        tvMonthAmount = root.findViewById(R.id.tvMonthAmount);
        ivMonthIcon = root.findViewById(R.id.ivMonthIcon);
        monthSummaryCard = root.findViewById(R.id.monthSummaryCard);
        tabIncome = root.findViewById(R.id.tabIncome);
        tabExpense = root.findViewById(R.id.tabExpense);
        btnViewCard = root.findViewById(R.id.btnViewCard);
        btnViewTable = root.findViewById(R.id.btnViewTable);
        btnViewAnalysis = root.findViewById(R.id.btnViewAnalysis);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        tabIncome.setOnClickListener(v -> switchType("income"));
        tabExpense.setOnClickListener(v -> switchType("expense"));
        btnViewCard.setOnClickListener(v -> switchViewMode("card"));
        btnViewTable.setOnClickListener(v -> switchViewMode("table"));
        btnViewAnalysis.setOnClickListener(v -> startActivity(new Intent(requireContext(), AnalysisActivity.class)));

        setupFilterChips(root);

        ImageView ivClear = root.findViewById(R.id.ivClearSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                if (ivClear != null) ivClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                applyFilter();
            }
            public void afterTextChanged(Editable s) {}
        });
        if (ivClear != null) ivClear.setOnClickListener(v -> { etSearch.setText(""); etSearch.requestFocus(); });

        root.findViewById(R.id.btnAddNew).setOnClickListener(v -> {
            Intent i = new Intent(requireContext(), AddTransactionActivity.class);
            i.putExtra(AddTransactionActivity.EXTRA_MODE, currentType);
            startActivity(i);
        });

        View btnPdf = root.findViewById(R.id.btnPdf);
        View btnPrint = root.findViewById(R.id.btnPrint);
        View btnSort = root.findViewById(R.id.btnSort);
        if (btnPdf != null) btnPdf.setOnClickListener(v ->
                Toast.makeText(requireContext(), "PDF এক্সপোর্ট শীঘ্রই আসছে", Toast.LENGTH_SHORT).show());
        if (btnPrint != null) btnPrint.setOnClickListener(v ->
                Toast.makeText(requireContext(), "প্রিন্ট ফিচার শীঘ্রই আসছে", Toast.LENGTH_SHORT).show());
        if (btnSort != null) btnSort.setOnClickListener(this::showSortMenu);

        refreshTypeUI();
        refreshViewModeUI();
        loadData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void showSortMenu(View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add("নতুন আগে");
        menu.getMenu().add("পুরাতন আগে");
        menu.getMenu().add("বেশি পরিমাণ আগে");
        menu.getMenu().add("কম পরিমাণ আগে");
        menu.setOnMenuItemClickListener(item -> { applyFilter(); return true; });
        menu.show();
    }

    private void switchType(String type) {
        if (type.equals(currentType)) return;
        currentType = type;
        refreshTypeUI();
        loadData();
    }

    private void switchViewMode(String mode) {
        viewMode = mode;
        refreshViewModeUI();
        applyFilter();
    }

    private void refreshViewModeUI() {
        boolean isIncome = "income".equals(currentType);
        int activeBg = isIncome ? R.drawable.ie_view_tab_active_income : R.drawable.ie_view_tab_active_expense;

        btnViewCard.setBackground("card".equals(viewMode) ? ContextCompat.getDrawable(requireContext(), activeBg) : null);
        btnViewCard.setTextColor(ContextCompat.getColor(requireContext(), "card".equals(viewMode) ? R.color.ieWhite : R.color.ieDarkText));

        btnViewTable.setBackground("table".equals(viewMode) ? ContextCompat.getDrawable(requireContext(), activeBg) : null);
        btnViewTable.setTextColor(ContextCompat.getColor(requireContext(), "table".equals(viewMode) ? R.color.ieWhite : R.color.ieDarkText));

        rv.setVisibility("card".equals(viewMode) ? View.VISIBLE : View.GONE);
        tableContainer.setVisibility("table".equals(viewMode) ? View.VISIBLE : View.GONE);
    }

    /** টগল, FAB, আইকন বাটন, সারসংক্ষেপ কার্ড — currentType অনুযায়ী কালার আপডেট করে। */
    private void refreshTypeUI() {
        boolean isIncome = "income".equals(currentType);

        tabIncome.setBackground(isIncome ? ContextCompat.getDrawable(requireContext(), R.drawable.ie_toggle_income_active) : ContextCompat.getDrawable(requireContext(), R.drawable.ie_toggle_income_inactive));
        tabIncome.setTextColor(ContextCompat.getColor(requireContext(), isIncome ? R.color.ieWhite : R.color.ieIncomeDark));
        tabExpense.setBackground(!isIncome ? ContextCompat.getDrawable(requireContext(), R.drawable.ie_toggle_expense_active) : ContextCompat.getDrawable(requireContext(), R.drawable.ie_toggle_expense_inactive));
        tabExpense.setTextColor(ContextCompat.getColor(requireContext(), !isIncome ? R.color.ieWhite : R.color.ieExpenseDark));

        View btnAddNew = rootView.findViewById(R.id.btnAddNew);
        if (btnAddNew != null) {
            btnAddNew.setBackground(ContextCompat.getDrawable(requireContext(),
                    isIncome ? R.drawable.ie_fab_income : R.drawable.ie_fab_expense));
        }

        int iconBg = isIncome ? R.drawable.ie_icon_btn_income : R.drawable.ie_icon_btn_expense;
        ImageView btnPdf = rootView.findViewById(R.id.btnPdf);
        ImageView btnPrint = rootView.findViewById(R.id.btnPrint);
        ImageView btnSort = rootView.findViewById(R.id.btnSort);
        if (btnPdf != null) btnPdf.setBackground(ContextCompat.getDrawable(requireContext(), iconBg));
        if (btnPrint != null) btnPrint.setBackground(ContextCompat.getDrawable(requireContext(), iconBg));
        if (btnSort != null) btnSort.setBackground(ContextCompat.getDrawable(requireContext(), iconBg));

        monthSummaryCard.setBackground(ContextCompat.getDrawable(requireContext(),
                isIncome ? R.drawable.ie_summary_bg_income : R.drawable.ie_summary_bg_expense));
        ivMonthIcon.setImageResource(isIncome ? R.drawable.ic_plus_circle : R.drawable.ic_minus_circle);
        ivMonthIcon.setBackground(ContextCompat.getDrawable(requireContext(),
                isIncome ? R.drawable.ie_summary_icon_income : R.drawable.ie_summary_icon_expense));
        int textColor = ContextCompat.getColor(requireContext(), isIncome ? R.color.ieIncomeText : R.color.ieExpenseText);
        tvMonthTitle.setTextColor(textColor);
        tvMonthAmount.setTextColor(textColor);
        tvMonthTitle.setText(isIncome ? "এই মাসের মোট আয়" : "এই মাসের মোট ব্যয়");

        refreshViewModeUI();
    }

    private void loadData() {
        allList = new ArrayList<>("expense".equals(currentType) ? db.getExpenseList() : db.getIncomeList());
        applyFilter();
        updateMonthSummary();
    }

    private void updateMonthSummary() {
        if (tvMonthAmount == null) return;
        double monthTotal = 0;
        for (Transaction t : allList) {
            if (!DateFilterUtil.matches(t.getDate(), "month")) continue;
            monthTotal += t.getAmount();
        }
        tvMonthAmount.setText(DatabaseManager.formatAmount(monthTotal));
    }

    private void setupFilterChips(View root) {
        String[] labels = {"সব", "আজ", "সপ্তাহ", "মাস", "বছর"};
        String[] keys = {"all", "today", "week", "month", "year"};
        chipRowLayout = root.findViewById(R.id.chipRow);
        if (chipRowLayout == null) return;
        chipRowLayout.removeAllViews();
        for (int i = 0; i < labels.length; i++) {
            final String key = keys[i];
            TextView chip = new TextView(requireContext());
            chip.setText(labels[i]);
            chip.setTextSize(12.5f);
            chip.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setPadding(38, 18, 38, 18);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(8);
            chip.setLayoutParams(lp);
            chip.setClickable(true);
            chip.setFocusable(true);
            boolean selected = key.equals(currentFilter);
            boolean isIncome = "income".equals(currentType);
            chip.setBackground(ContextCompat.getDrawable(requireContext(),
                    selected ? (isIncome ? R.drawable.ie_view_tab_active_income : R.drawable.ie_view_tab_active_expense) : R.drawable.ie_view_tab_track));
            chip.setTextColor(selected ? ContextCompat.getColor(requireContext(), R.color.ieWhite)
                    : ContextCompat.getColor(requireContext(), R.color.ieDarkText));
            if (selected) chip.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.chip_scale));
            chip.setOnClickListener(v -> { currentFilter = key; setupFilterChips(root); applyFilter(); });
            chipRowLayout.addView(chip);
        }
    }

    private void applyFilter() {
        String q = etSearch.getText() != null ? etSearch.getText().toString().toLowerCase().trim() : "";
        filteredList = new ArrayList<>();
        for (Transaction t : allList) {
            if (!q.isEmpty() && !t.getDisplayTitle().toLowerCase().contains(q) && !t.getNote().toLowerCase().contains(q)) continue;
            if (!DateFilterUtil.matches(t.getDate(), currentFilter)) continue;
            filteredList.add(t);
        }

        if ("card".equals(viewMode) && filteredList.isEmpty()) {
            rv.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else if ("card".equals(viewMode)) {
            emptyState.setVisibility(View.GONE);
            rv.setVisibility(View.VISIBLE);
            renderCardList();
        }

        if ("table".equals(viewMode)) {
            emptyState.setVisibility(View.GONE);
            String qOnly = q; // টেবিল সবসময় মাসিক-ভিত্তিক, শুধু সার্চ ফিল্টার প্রযোজ্য
            List<Transaction> searchOnly = new ArrayList<>();
            for (Transaction t : allList) {
                if (!qOnly.isEmpty() && !t.getDisplayTitle().toLowerCase().contains(qOnly) && !t.getNote().toLowerCase().contains(qOnly)) continue;
                searchOnly.add(t);
            }
            renderMonthlyTable(searchOnly);
        }
    }

    private void renderCardList() {
        IncomeExpenseCardAdapter adapter = new IncomeExpenseCardAdapter(requireContext(), filteredList, currentType,
                (item, pos) -> TransactionSheetHelper.showEditTransactionDialog(requireActivity(), db, currentType, item, this::loadData),
                (item, pos) -> TransactionSheetHelper.confirmDeleteTransaction(requireActivity(), db, currentType, item, this::loadData),
                (item, pos) -> TransactionSheetHelper.showTransactionSheet(requireActivity(), db, currentType, item, this::loadData));
        rv.setAdapter(adapter);
        rv.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_fall_down));
        rv.scheduleLayoutAnimation();
    }

    /** ছক ভিউ — মাসভিত্তিক গ্রুপ করে সাজানো, প্রতি মাসের হেডারে মোট, নিচে "মোট" রো। */
    private void renderMonthlyTable(List<Transaction> list) {
        tableMonthsContainer.removeAllViews();
        boolean isIncome = "income".equals(currentType);
        int headerBg = ContextCompat.getColor(requireContext(), isIncome ? R.color.ieIncomeDark : R.color.ieExpenseDark);
        int colHeaderBg = ContextCompat.getColor(requireContext(), isIncome ? R.color.ieIncomeLightBg : R.color.ieExpenseLightBg);
        int colHeaderText = ContextCompat.getColor(requireContext(), isIncome ? R.color.ieIncomeText : R.color.ieExpenseText);

        // yyyy-MM অনুযায়ী গ্রুপ করা, সাম্প্রতিক মাস আগে
        Map<String, List<Transaction>> byMonth = new LinkedHashMap<>();
        List<Transaction> sorted = new ArrayList<>(list);
        sorted.sort((a, b) -> {
            String da = a.getDate() != null ? a.getDate() : "";
            String db2 = b.getDate() != null ? b.getDate() : "";
            return db2.compareTo(da);
        });
        for (Transaction t : sorted) {
            String key = (t.getDate() != null && t.getDate().length() >= 7) ? t.getDate().substring(0, 7) : "অজানা";
            byMonth.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
        }

        if (byMonth.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("কোনো তথ্য নেই");
            empty.setTextColor(ContextCompat.getColor(requireContext(), R.color.ieDarkText));
            empty.setPadding(24, 60, 24, 24);
            empty.setGravity(android.view.Gravity.CENTER);
            tableMonthsContainer.addView(empty);
            return;
        }

        for (Map.Entry<String, List<Transaction>> entry : byMonth.entrySet()) {
            String monthKey = entry.getKey();
            List<Transaction> monthList = entry.getValue();
            double monthTotal = 0;
            for (Transaction t : monthList) monthTotal += t.getAmount();

            View card = LayoutInflater.from(requireContext()).inflate(R.layout.ie_table_month_card, tableMonthsContainer, false);
            LinearLayout headerBar = card.findViewById(R.id.monthHeaderBar);
            TextView tvLabel = card.findViewById(R.id.tvMonthLabel);
            TextView tvTotal = card.findViewById(R.id.tvMonthTotal);
            LinearLayout rowsContainer = card.findViewById(R.id.rowsContainer);

            headerBar.setBackgroundColor(headerBg);
            tvLabel.setText(monthLabel(monthKey));
            tvTotal.setText("মোট: " + DatabaseManager.formatAmount(monthTotal));

            // কলাম হেডার
            rowsContainer.addView(buildRow(new String[]{"উৎস", "পরিমাণ", "তারিখ", "সময়"},
                    colHeaderBg, colHeaderText, true, 0));

            // ডেটা রো — অল্টারনেটিং ব্যাকগ্রাউন্ড
            for (int i = 0; i < monthList.size(); i++) {
                Transaction t = monthList.get(i);
                int rowBg = (i % 2 == 0) ? ContextCompat.getColor(requireContext(), R.color.ieWhite) : ContextCompat.getColor(requireContext(), R.color.ieGreyBg);
                rowsContainer.addView(buildRow(new String[]{
                        t.getDisplayTitle(),
                        DatabaseManager.formatAmount(t.getAmount()),
                        DatabaseManager.formatDateDisplay(t.getDate()),
                        DatabaseManager.formatTimeDisplay(t.getTime())
                }, rowBg, ContextCompat.getColor(requireContext(), R.color.ieDarkText), false, i));
            }

            // মোট রো
            View divider = new View(requireContext());
            divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2));
            divider.setBackgroundColor(headerBg);
            rowsContainer.addView(divider);
            LinearLayout totalRow = buildRow(new String[]{"মোট", DatabaseManager.formatAmount(monthTotal), "", ""},
                    ContextCompat.getColor(requireContext(), R.color.ieWhite), headerBg, true, -1);
            rowsContainer.addView(totalRow);

            tableMonthsContainer.addView(card);
        }
    }

    private LinearLayout buildRow(String[] cols, int bgColor, int textColor, boolean bold, int index) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundColor(bgColor);
        row.setPadding(28, 22, 28, 22);
        float[] weights = {1.4f, 1f, 1f, 0.9f};
        for (int i = 0; i < cols.length; i++) {
            TextView tv = new TextView(requireContext());
            tv.setText(cols[i]);
            tv.setTextColor(textColor);
            tv.setTextSize(12.5f);
            if (bold) tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            tv.setMaxLines(1);
            tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
            tv.setGravity(i == 0 ? android.view.Gravity.START : android.view.Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weights[i]);
            tv.setLayoutParams(lp);
            row.addView(tv);
        }
        return row;
    }

    private String monthLabel(String yyyyMM) {
        try {
            String[] parts = yyyyMM.split("-");
            int monthIdx = Integer.parseInt(parts[1]) - 1;
            String year = parts[0];
            if (monthIdx >= 0 && monthIdx < 12) return BN_MONTHS[monthIdx] + " " + year;
        } catch (Exception ignored) {}
        return yyyyMM;
    }
}
