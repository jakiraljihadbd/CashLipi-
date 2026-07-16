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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.activities.AddTransactionActivity;
import com.jrappspot.cashlipi.activities.AnalysisActivity;
import com.jrappspot.cashlipi.models.Transaction;
import com.jrappspot.cashlipi.utils.DatabaseManager;
import com.jrappspot.cashlipi.utils.DateFilterUtil;
import com.jrappspot.cashlipi.utils.TransactionSheetHelper;
import com.jrappspot.cashlipi.utils.ListViewController;

import java.util.ArrayList;
import java.util.List;

public class IncomeExpenseFragment extends Fragment {

    private DatabaseManager db;
    private RecyclerView rv;
    private LinearLayout emptyState;
    private EditText etSearch;
    private TextView tvMonthAmount, tvMonthSubtitle, tvMonthTitle;
    private TextView tabIncome, tabExpense;
    private List<Transaction> allList = new ArrayList<>();
    private List<Transaction> filteredList = new ArrayList<>();
    private String currentFilter = "all";
    private String currentType = "income"; // "income" | "expense"
    private final ListViewController viewController = new ListViewController();
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
        etSearch = root.findViewById(R.id.etSearch);
        tvMonthTitle = root.findViewById(R.id.tvMonthTitle);
        tvMonthAmount = root.findViewById(R.id.tvMonthAmount);
        tvMonthSubtitle = root.findViewById(R.id.tvMonthSubtitle);
        tabIncome = root.findViewById(R.id.tabIncome);
        tabExpense = root.findViewById(R.id.tabExpense);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        tabIncome.setOnClickListener(v -> switchType("income"));
        tabExpense.setOnClickListener(v -> switchType("expense"));

        setupFilterChips(root);
        viewController.attachControls(requireContext(), root, this::applyFilter);

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

        View btnViewAnalysis = root.findViewById(R.id.btnViewAnalysis);
        if (btnViewAnalysis != null) {
            btnViewAnalysis.setOnClickListener(v -> startActivity(new Intent(requireContext(), AnalysisActivity.class)));
        }

        View btnPdf = root.findViewById(R.id.btnPdf);
        View btnPrint = root.findViewById(R.id.btnPrint);
        if (btnPdf != null) btnPdf.setOnClickListener(v ->
                Toast.makeText(requireContext(), "PDF এক্সপোর্ট শীঘ্রই আসছে", Toast.LENGTH_SHORT).show());
        if (btnPrint != null) btnPrint.setOnClickListener(v ->
                Toast.makeText(requireContext(), "প্রিন্ট ফিচার শীঘ্রই আসছে", Toast.LENGTH_SHORT).show());

        refreshTypeUI();
        loadData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void switchType(String type) {
        if (type.equals(currentType)) return;
        currentType = type;
        refreshTypeUI();
        loadData();
    }

    private void refreshTypeUI() {
        boolean isIncome = "income".equals(currentType);

        tabIncome.setBackground(isIncome ? ContextCompat.getDrawable(requireContext(), R.drawable.bg_txn_tab_selected_income) : null);
        tabIncome.setTextColor(ContextCompat.getColor(requireContext(), isIncome ? R.color.white : R.color.txnTabInactiveText));
        tabExpense.setBackground(!isIncome ? ContextCompat.getDrawable(requireContext(), R.drawable.bg_txn_tab_selected_expense) : null);
        tabExpense.setTextColor(ContextCompat.getColor(requireContext(), !isIncome ? R.color.white : R.color.txnTabInactiveText));

        View btnAddNew = rootView.findViewById(R.id.btnAddNew);
        if (btnAddNew != null) {
            btnAddNew.setBackground(ContextCompat.getDrawable(requireContext(),
                    isIncome ? R.drawable.bg_fab_income : R.drawable.bg_fab_expense));
        }

        ImageView btnPdf = rootView.findViewById(R.id.btnPdf);
        ImageView btnPrint = rootView.findViewById(R.id.btnPrint);
        int tint = ContextCompat.getColor(requireContext(), isIncome ? R.color.incomeColor : R.color.expenseColor);
        if (btnPdf != null) btnPdf.setColorFilter(tint);
        if (btnPrint != null) btnPrint.setColorFilter(tint);

        if (tvMonthTitle != null) {
            tvMonthTitle.setText(isIncome ? "বর্তমান মাসের সারসংক্ষেপ (আয়)" : "বর্তমান মাসের সারসংক্ষেপ (ব্যয়)");
        }
    }

    private void loadData() {
        allList = new ArrayList<>("expense".equals(currentType) ? db.getExpenseList() : db.getIncomeList());
        applyFilter();
        updateMonthSummary();
    }

    private void updateMonthSummary() {
        if (tvMonthAmount == null) return;
        boolean isIncome = "income".equals(currentType);
        double monthTotal = 0;
        String latestDate = null;
        for (Transaction t : allList) {
            if (!DateFilterUtil.matches(t.getDate(), "month")) continue;
            monthTotal += t.getAmount();
            if (latestDate == null || (t.getDate() != null && t.getDate().compareTo(latestDate) > 0)) {
                latestDate = t.getDate();
            }
        }
        String sign = isIncome ? "+ " : "- ";
        tvMonthAmount.setText(sign + DatabaseManager.formatAmount(monthTotal));
        tvMonthAmount.setTextColor(ContextCompat.getColor(requireContext(),
                isIncome ? R.color.amountIncome : R.color.amountExpense));
        if (tvMonthSubtitle != null) {
            String dateText = latestDate != null ? DatabaseManager.formatDateDisplay(latestDate) : "--";
            tvMonthSubtitle.setText("(সর্বশেষ হালনাগাদ: " + dateText + ")");
        }
    }

    private void setupFilterChips(View root) {
        String[] labels = {"সব", "আজ", "সপ্তাহ", "মাস", "বছর"};
        String[] keys = {"all", "today", "week", "month", "year"};
        LinearLayout chipRow = root.findViewById(R.id.chipRow);
        if (chipRow == null) return;
        chipRow.removeAllViews();
        for (int i = 0; i < labels.length; i++) {
            final String key = keys[i];
            TextView chip = new TextView(requireContext());
            chip.setText(labels[i]);
            chip.setTextSize(12.5f);
            chip.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setPadding(36, 18, 36, 18);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(8);
            chip.setLayoutParams(lp);
            chip.setClickable(true);
            chip.setFocusable(true);
            boolean selected = key.equals(currentFilter);
            chip.setBackground(ContextCompat.getDrawable(requireContext(),
                    selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected));
            chip.setTextColor(selected ? ContextCompat.getColor(requireContext(), R.color.white)
                    : ContextCompat.getColor(requireContext(), R.color.chipUnselectedText));
            if (selected) chip.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.chip_scale));
            chip.setOnClickListener(v -> { currentFilter = key; setupFilterChips(root); applyFilter(); });
            chipRow.addView(chip);
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
        viewController.applySort(filteredList);
        if (filteredList.isEmpty()) {
            rv.setVisibility(View.GONE);
            View tableContainer = rootView.findViewById(R.id.tableContainer);
            if (tableContainer != null) tableContainer.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            emptyState.setVisibility(View.GONE);
            rv.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_fall_down));
            viewController.render(requireContext(), rootView, filteredList, currentType,
                    (item, pos) -> TransactionSheetHelper.showTransactionSheet(requireActivity(), db, currentType, item, this::loadData),
                    (item, pos) -> TransactionSheetHelper.showTransactionSheet(requireActivity(), db, currentType, item, this::loadData));
            rv.scheduleLayoutAnimation();
        }
    }
}
