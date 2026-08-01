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

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.activities.AddTransactionActivity;
import com.jrappspot.cashlipi.activities.AnalysisActivity;
import com.jrappspot.cashlipi.adapters.IncomeExpenseCardAdapter;
import com.jrappspot.cashlipi.models.Transaction;
import com.jrappspot.cashlipi.utils.DatabaseManager;
import com.jrappspot.cashlipi.utils.DateFilterUtil;
import com.jrappspot.cashlipi.utils.InvoicePdfHelper;
import com.jrappspot.cashlipi.utils.PaymentMethodUtil;
import com.jrappspot.cashlipi.utils.SoundEffectPlayer;
import com.jrappspot.cashlipi.utils.TransactionSheetHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * আয়-ব্যয় পেজ — উপরে আয়/ব্যয় টগল দিয়ে দুটো তালিকার মধ্যে সুইচ করা যায়।
 * কার্ড ভিউ — সম্পাদনা/মুছুন/আরও বাটনসহ। ছক ভিউ — মাসিক-ভিত্তিক, শুধু দেখার জন্য (এডিট নেই)।
 */
public class IncomeExpenseFragment extends Fragment {

    private String[] monthNames; // getResources().getStringArray থেকে ভাষা অনুযায়ী লোড হয়

    private DatabaseManager db;
    private RecyclerView rv;
    private LinearLayout emptyState, tableMonthsContainer, monthSummaryCard, activeFilterBar;
    private View tableContainer;
    private EditText etSearch;
    private TextView tvMonthAmount, tvMonthTitle, tvActiveFilter, btnClearFilter;
    private ImageView ivMonthIcon;
    private TextView tabIncome, tabExpense, btnViewCard, btnViewTable, btnViewAnalysis;
    private List<Transaction> allList = new ArrayList<>();
    private List<Transaction> filteredList = new ArrayList<>();
    private String currentFilter = "all";
    private String currentMethodFilter = "all"; // "all" | "cash" | "bkash" | "nagad" | "rocket" | "bank" | "other"
    private String currentCategoryFilter = "all"; // "all" অথবা category/source-এর নাম
    private String currentSort = "newest"; // "newest" | "oldest" | "amount_high" | "amount_low"
    private String currentType = "income"; // "income" | "expense"
    /** হোম পেজের আয়/ব্যয় কার্ডে ট্যাপ করলে এখানে "income"/"expense" সেট হয় — পরের onResume-এ
     *  সেই ট্যাব সরাসরি সিলেক্ট হয়ে যায় (একবার ব্যবহারের পর null করে দেওয়া হয়)। */
    public static String pendingTransactionType = null;
    private String viewMode = "card"; // "card" | "table"
    private View rootView;
    private SoundEffectPlayer soundEffectPlayer;

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
        monthNames = getResources().getStringArray(R.array.month_names);
        soundEffectPlayer = SoundEffectPlayer.getInstance(requireContext());
        rv = root.findViewById(R.id.rvList);
        emptyState = root.findViewById(R.id.emptyState);
        tableContainer = root.findViewById(R.id.tableContainer);
        tableMonthsContainer = root.findViewById(R.id.tableMonthsContainer);
        etSearch = root.findViewById(R.id.etSearch);
        tvMonthTitle = root.findViewById(R.id.tvMonthTitle);
        tvMonthAmount = root.findViewById(R.id.tvMonthAmount);
        ivMonthIcon = root.findViewById(R.id.ivMonthIcon);
        monthSummaryCard = root.findViewById(R.id.monthSummaryCard);
        activeFilterBar = root.findViewById(R.id.activeFilterBar);
        tvActiveFilter = root.findViewById(R.id.tvActiveFilter);
        btnClearFilter = root.findViewById(R.id.btnClearFilter);
        tabIncome = root.findViewById(R.id.tabIncome);
        tabExpense = root.findViewById(R.id.tabExpense);
        btnViewCard = root.findViewById(R.id.btnViewCard);
        btnViewTable = root.findViewById(R.id.btnViewTable);
        btnViewAnalysis = root.findViewById(R.id.btnViewAnalysis);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        tabIncome.setOnClickListener(v -> { switchType("income"); playTapSound(); });
        tabExpense.setOnClickListener(v -> { switchType("expense"); playTapSound(); });
        btnViewCard.setOnClickListener(v -> switchViewMode("card"));
        btnViewTable.setOnClickListener(v -> switchViewMode("table"));
        btnViewAnalysis.setOnClickListener(v -> startActivity(new Intent(requireContext(), AnalysisActivity.class)));

        if (btnClearFilter != null) btnClearFilter.setOnClickListener(v -> {
            currentMethodFilter = "all";
            currentCategoryFilter = "all";
            onFiltersChanged();
        });

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
            playTapSound();
            Intent i = new Intent(requireContext(), AddTransactionActivity.class);
            i.putExtra(AddTransactionActivity.EXTRA_MODE, currentType);
            startActivity(i);
        });

        View btnPdf = root.findViewById(R.id.btnPdf);
        View btnPrint = root.findViewById(R.id.btnPrint);
        View btnFilter = root.findViewById(R.id.btnFilter);
        if (btnPdf != null) btnPdf.setOnClickListener(v ->
                InvoicePdfHelper.showExportDialog(requireContext(), currentType, filteredList, false));
        if (btnPrint != null) btnPrint.setOnClickListener(v ->
                InvoicePdfHelper.showExportDialog(requireContext(), currentType, filteredList, true));
        if (btnFilter != null) btnFilter.setOnClickListener(v -> { playTapSound(); showFilterSheet(); });

        refreshTypeUI();
        refreshViewModeUI();
        loadData();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (pendingTransactionType != null) {
            String type = pendingTransactionType;
            pendingTransactionType = null;
            if (!type.equals(currentType)) {
                currentType = type;
                refreshTypeUI();
            }
        }
        loadData();
    }

    // ══════════════════════════════════════════════════════════════
    //  ফিল্টার সিস্টেম — বড়, বটম-শিট ভিত্তিক (ক্যাটাগরি / সময় সাজান / ধরন)
    // ══════════════════════════════════════════════════════════════

    private void showFilterSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.PremiumBottomSheetDialog);
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_ie_filter, null);
        dialog.setContentView(v);

        View rowCategory = v.findViewById(R.id.rowFilterCategory);
        View rowSort = v.findViewById(R.id.rowFilterSort);
        View rowType = v.findViewById(R.id.rowFilterType);
        TextView subCategory = v.findViewById(R.id.subFilterCategory);
        TextView subSort = v.findViewById(R.id.subFilterSort);
        TextView subType = v.findViewById(R.id.subFilterType);

        subCategory.setText("all".equals(currentCategoryFilter) ? "সব ক্যাটাগরি" : currentCategoryFilter);
        subSort.setText(sortLabel(currentSort));
        subType.setText("all".equals(currentMethodFilter) ? "সব" : PaymentMethodUtil.getLabel(requireContext(), currentMethodFilter));

        rowCategory.setOnClickListener(v2 -> { playTapSound(); dialog.dismiss(); showCategoryPicker(); });
        rowSort.setOnClickListener(v2 -> { playTapSound(); dialog.dismiss(); showSortPicker(); });
        rowType.setOnClickListener(v2 -> { playTapSound(); dialog.dismiss(); showTypePicker(); });

        dialog.show();
    }

    private String sortLabel(String key) {
        switch (key) {
            case "oldest": return getString(R.string.sort_oldest_first);
            case "amount_high": return getString(R.string.sort_amount_high_first);
            case "amount_low": return getString(R.string.sort_amount_low_first);
            default: return getString(R.string.sort_newest_first);
        }
    }

    /** ১. ক্যাটাগরি অনুযায়ী — allList থেকে ইউনিক ক্যাটাগরি/উৎস বের করে দেখায়। */
    private void showCategoryPicker() {
        LinkedHashSet<String> categories = new LinkedHashSet<>();
        for (Transaction t : allList) {
            String name = "income".equals(currentType) ? t.getSource() : t.getCategory();
            if (name == null || name.trim().isEmpty()) {
                name = "income".equals(currentType) ? "আয়" : "ব্যয়";
            }
            categories.add(name);
        }
        List<String> sorted = new ArrayList<>(categories);
        java.util.Collections.sort(sorted, String::compareToIgnoreCase);

        List<String> keys = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        keys.add("all");
        labels.add("সব ক্যাটাগরি");
        for (String c : sorted) { keys.add(c); labels.add(c); }

        showPickerSheet("ক্যাটাগরি অনুযায়ী", keys, labels, currentCategoryFilter, selectedKey -> {
            currentCategoryFilter = selectedKey;
            onFiltersChanged();
        });
    }

    /** ২. সময় অনুযায়ী সাজান — নতুন/পুরাতন আগে, পরিমাণ বেশি/কম আগে। */
    private void showSortPicker() {
        List<String> keys = java.util.Arrays.asList("newest", "oldest", "amount_high", "amount_low");
        List<String> labels = java.util.Arrays.asList(
                getString(R.string.sort_newest_first), getString(R.string.sort_oldest_first),
                getString(R.string.sort_amount_high_first), getString(R.string.sort_amount_low_first));

        showPickerSheet("সময় অনুযায়ী সাজান", keys, labels, currentSort, selectedKey -> {
            currentSort = selectedKey;
            onFiltersChanged();
        });
    }

    /** ৩. ধরন — পেমেন্ট মাধ্যম (সব/ক্যাশ/বিকাশ/নগদ/রকেট/ব্যাংক/অন্যান্য)। */
    private void showTypePicker() {
        List<String> keys = java.util.Arrays.asList("all", "cash", "bkash", "nagad", "rocket", "bank", "other");
        List<String> labels = new ArrayList<>();
        for (String k : keys) {
            labels.add("all".equals(k) ? getString(R.string.filter_all) : PaymentMethodUtil.getLabel(requireContext(), k));
        }

        showPickerSheet("ধরন", keys, labels, currentMethodFilter, selectedKey -> {
            currentMethodFilter = selectedKey;
            onFiltersChanged();
        });
    }

    private interface OnPick { void pick(String key); }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /** সাধারণ একক-নির্বাচন পিকার শিট — বড়, মোবাইল-ফ্রেন্ডলি রো, নির্বাচিত আইটেমে হালকা রঙের
     *  ব্যাকগ্রাউন্ড + বোল্ড টেক্সট + টিক চিহ্ন, প্রতিটা আইটেমের মাঝে পাতলা ডিভাইডার। */
    private void showPickerSheet(String title, List<String> keys, List<String> labels, String selectedKey, OnPick onPick) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.PremiumBottomSheetDialog);
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_ie_picker, null);
        dialog.setContentView(v);

        TextView titleView = v.findViewById(R.id.pickerTitle);
        LinearLayout container = v.findViewById(R.id.pickerListContainer);
        titleView.setText(title);

        boolean isIncome = "income".equals(currentType);
        int accentColor = ContextCompat.getColor(requireContext(), isIncome ? R.color.ieIncomeDark : R.color.ieExpenseDark);
        int selectedBg = ContextCompat.getColor(requireContext(), isIncome ? R.color.ieIncomeLightBg : R.color.ieExpenseLightBg);

        android.util.TypedValue rippleAttr = new android.util.TypedValue();
        requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, rippleAttr, true);

        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            boolean selected = key.equals(selectedKey);

            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(dp(22), dp(17), dp(22), dp(17));
            row.setClickable(true);
            row.setFocusable(true);
            row.setBackgroundResource(rippleAttr.resourceId != 0 ? rippleAttr.resourceId : android.R.drawable.list_selector_background);
            if (selected) row.setBackgroundColor(selectedBg);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            row.setLayoutParams(rowLp);

            TextView label = new TextView(requireContext());
            label.setText(labels.get(i));
            label.setTextSize(17f);
            label.setTypeface(android.graphics.Typeface.DEFAULT, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            label.setTextColor(selected ? accentColor : ContextCompat.getColor(requireContext(), R.color.primaryTextDark));
            LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            label.setLayoutParams(labelLp);
            row.addView(label);

            if (selected) {
                ImageView check = new ImageView(requireContext());
                check.setImageResource(R.drawable.ic_checkmark_plain);
                LinearLayout.LayoutParams checkLp = new LinearLayout.LayoutParams(dp(22), dp(22));
                check.setLayoutParams(checkLp);
                check.setColorFilter(accentColor);
                row.addView(check);
            }

            row.setOnClickListener(v2 -> {
                playTapSound();
                dialog.dismiss();
                onPick.pick(key);
            });

            container.addView(row);

            if (i < keys.size() - 1) {
                View divider = new View(requireContext());
                divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
                divider.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.dividerColor));
                container.addView(divider);
            }
        }

        dialog.show();
    }

    /** যেকোনো ফিল্টার/সর্ট বদলানোর পর কল করতে হবে — তালিকা, মাস-সারসংক্ষেপ, ফিল্টার-বার সব রিফ্রেশ করে। */
    private void onFiltersChanged() {
        applyFilter();
        updateMonthSummary();
        updateActiveFilterBar();
    }

    /** সক্রিয় ফিল্টার থাকলে (সব ছাড়া অন্য কিছু নির্বাচিত হলে) একটা ছোট বার দেখায়, নাহলে লুকানো থাকে। */
    private void updateActiveFilterBar() {
        if (activeFilterBar == null) return;
        boolean hasCategory = !"all".equals(currentCategoryFilter);
        boolean hasMethod = !"all".equals(currentMethodFilter);

        if (!hasCategory && !hasMethod) {
            activeFilterBar.setVisibility(View.GONE);
            return;
        }

        StringBuilder sb = new StringBuilder("ফিল্টার: ");
        if (hasCategory) sb.append(currentCategoryFilter);
        if (hasCategory && hasMethod) sb.append(" • ");
        if (hasMethod) sb.append(PaymentMethodUtil.getLabel(requireContext(), currentMethodFilter));
        tvActiveFilter.setText(sb.toString());
        activeFilterBar.setVisibility(View.VISIBLE);
    }

    private void switchType(String type) {
        if (type.equals(currentType)) return;
        currentType = type;
        currentCategoryFilter = "all";
        refreshTypeUI();
        loadData();
    }

    private void playTapSound() {
        if (soundEffectPlayer != null) soundEffectPlayer.playTap();
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
        ImageView btnFilter = rootView.findViewById(R.id.btnFilter);
        if (btnPdf != null) btnPdf.setBackground(ContextCompat.getDrawable(requireContext(), iconBg));
        if (btnPrint != null) btnPrint.setBackground(ContextCompat.getDrawable(requireContext(), iconBg));
        if (btnFilter != null) btnFilter.setBackground(ContextCompat.getDrawable(requireContext(), iconBg));

        monthSummaryCard.setBackground(ContextCompat.getDrawable(requireContext(),
                isIncome ? R.drawable.ie_summary_bg_income : R.drawable.ie_summary_bg_expense));
        ivMonthIcon.setImageResource(isIncome ? R.drawable.ic_plus_circle : R.drawable.ic_minus_circle);
        ivMonthIcon.setBackground(ContextCompat.getDrawable(requireContext(),
                isIncome ? R.drawable.ie_summary_icon_income : R.drawable.ie_summary_icon_expense));
        int textColor = ContextCompat.getColor(requireContext(), isIncome ? R.color.ieIncomeText : R.color.ieExpenseText);
        tvMonthTitle.setTextColor(textColor);
        tvMonthAmount.setTextColor(textColor);
        tvMonthTitle.setText(isIncome ? getString(R.string.month_total_income) : getString(R.string.month_total_expense));

        refreshViewModeUI();
    }

    private void loadData() {
        allList = new ArrayList<>("expense".equals(currentType) ? db.getExpenseList() : db.getIncomeList());
        applyFilter();
        updateMonthSummary();
        updateActiveFilterBar();
    }

    /** কোনো ফিল্টার সিলেক্ট না থাকলে — "এই মাসের মোট আয়/ব্যয়"।
     *  ক্যাটাগরি এবং/অথবা ধরন (পেমেন্ট মাধ্যম) সিলেক্ট থাকলে — শিরোনাম সেই অনুযায়ী বদলে যায়
     *  (যেমন: "এই মাসের ক্যাশ আয়", "এই মাসের বেতন আয়", "এই মাসের বেতন • ক্যাশ আয়")
     *  এবং অ্যামাউন্ট শুধু সেই ফিল্টার অনুযায়ী মিলে যাওয়া লেনদেনের যোগফল দেখায়। */
    private void updateMonthSummary() {
        if (tvMonthAmount == null) return;
        boolean isIncome = "income".equals(currentType);
        boolean categorySelected = !"all".equals(currentCategoryFilter);
        boolean methodSelected = !"all".equals(currentMethodFilter);

        double monthTotal = 0;
        for (Transaction t : allList) {
            if (!DateFilterUtil.matches(t.getDate(), "month")) continue;
            if (!matchesCategoryFilter(t)) continue;
            if (!matchesMethodFilter(t)) continue;
            monthTotal += t.getAmount();
        }
        tvMonthAmount.setText(DatabaseManager.formatAmount(monthTotal));

        if (tvMonthTitle != null) {
            if (categorySelected || methodSelected) {
                StringBuilder label = new StringBuilder("এই মাসের ");
                if (categorySelected) label.append(currentCategoryFilter);
                if (categorySelected && methodSelected) label.append(" • ");
                if (methodSelected) label.append(PaymentMethodUtil.getLabel(requireContext(), currentMethodFilter));
                label.append(isIncome ? " আয়" : " ব্যয়");
                tvMonthTitle.setText(label.toString());
            } else {
                tvMonthTitle.setText(isIncome ? getString(R.string.month_total_income) : getString(R.string.month_total_expense));
            }
        }
    }

    private void applyFilter() {
        String q = etSearch.getText() != null ? etSearch.getText().toString().toLowerCase().trim() : "";
        filteredList = new ArrayList<>();
        for (Transaction t : allList) {
            if (!q.isEmpty() && !t.getDisplayTitle().toLowerCase().contains(q) && !t.getNote().toLowerCase().contains(q)) continue;
            if (!DateFilterUtil.matches(t.getDate(), currentFilter)) continue;
            if (!matchesMethodFilter(t)) continue;
            if (!matchesCategoryFilter(t)) continue;
            filteredList.add(t);
        }
        sortFilteredList(filteredList);

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
            String qOnly = q; // টেবিল সবসময় মাসিক-ভিত্তিক, শুধু সার্চ ও ধরন/ক্যাটাগরি ফিল্টার প্রযোজ্য
            List<Transaction> searchOnly = new ArrayList<>();
            for (Transaction t : allList) {
                if (!qOnly.isEmpty() && !t.getDisplayTitle().toLowerCase().contains(qOnly) && !t.getNote().toLowerCase().contains(qOnly)) continue;
                if (!matchesMethodFilter(t)) continue;
                if (!matchesCategoryFilter(t)) continue;
                searchOnly.add(t);
            }
            renderMonthlyTable(searchOnly);
        }
    }

    private void sortFilteredList(List<Transaction> list) {
        switch (currentSort) {
            case "oldest":
                list.sort((a, b) -> (a.getDate() + a.getTime()).compareTo(b.getDate() + b.getTime()));
                break;
            case "amount_high":
                list.sort((a, b) -> Double.compare(b.getAmount(), a.getAmount()));
                break;
            case "amount_low":
                list.sort((a, b) -> Double.compare(a.getAmount(), b.getAmount()));
                break;
            default: // "newest"
                list.sort((a, b) -> (b.getDate() + b.getTime()).compareTo(a.getDate() + a.getTime()));
                break;
        }
    }

    private boolean matchesMethodFilter(Transaction t) {
        if ("all".equals(currentMethodFilter)) return true;
        String m = t.getMethod().isEmpty() ? "cash" : t.getMethod();
        return currentMethodFilter.equals(m);
    }

    private boolean matchesCategoryFilter(Transaction t) {
        if ("all".equals(currentCategoryFilter)) return true;
        boolean isIncome = "income".equals(currentType);
        String name = isIncome ? t.getSource() : t.getCategory();
        if (name == null || name.trim().isEmpty()) name = isIncome ? "আয়" : "ব্যয়";
        return currentCategoryFilter.equals(name);
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
            String key = (t.getDate() != null && t.getDate().length() >= 7) ? t.getDate().substring(0, 7) : getString(R.string.month_unknown);
            byMonth.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
        }

        if (byMonth.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText(getString(R.string.no_data_found));
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
            tvTotal.setText(getString(R.string.table_total_prefix, DatabaseManager.formatAmount(monthTotal)));

            // কলাম হেডার
            rowsContainer.addView(buildRow(new String[]{
                    getString(R.string.table_col_source), getString(R.string.table_col_amount),
                    getString(R.string.table_col_date), getString(R.string.table_col_time)},
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
            LinearLayout totalRow = buildRow(new String[]{getString(R.string.table_total), DatabaseManager.formatAmount(monthTotal), "", ""},
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
            if (monthIdx >= 0 && monthIdx < 12) return monthNames[monthIdx] + " " + year;
        } catch (Exception ignored) {}
        return yyyyMM;
    }
}
