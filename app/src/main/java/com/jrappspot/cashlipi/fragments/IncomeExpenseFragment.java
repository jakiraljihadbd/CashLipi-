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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.activities.AddTransactionActivity;
import com.jrappspot.cashlipi.adapters.TransactionListAdapter;
import com.jrappspot.cashlipi.models.Transaction;
import com.jrappspot.cashlipi.utils.DatabaseManager;
import com.jrappspot.cashlipi.utils.DateFilterUtil;
import com.jrappspot.cashlipi.utils.TransactionSheetHelper;
import com.jrappspot.cashlipi.utils.ListViewController;

import java.util.ArrayList;
import java.util.List;

/**
 * আয়-ব্যয় পেজ — এই ধাপে শুধু আয়ের তালিকার দিকে পয়েন্ট করা আছে (স্পেসিফিকেশন অনুযায়ী,
 * এই ধাপে যেকোনো একটা লিস্ট দেখালেই যথেষ্ট)। ভবিষ্যতে আয়+ব্যয় মার্জড পেজ হবে।
 * লজিক IncomeListActivity থেকে হুবহু নেওয়া, শুধু Activity → Fragment রূপান্তর।
 */
public class IncomeExpenseFragment extends Fragment {

    private DatabaseManager db;
    private RecyclerView rv;
    private LinearLayout emptyState;
    private EditText etSearch;
    private TextView tvTotal, tvCount;
    private List<Transaction> allList = new ArrayList<>();
    private List<Transaction> filteredList = new ArrayList<>();
    private String currentFilter = "all";
    private final ListViewController viewController = new ListViewController();
    private View rootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_list_common, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);
        db = DatabaseManager.getInstance(requireContext());
        rootView = root;
        rv = root.findViewById(R.id.rvList);
        emptyState = root.findViewById(R.id.emptyState);
        etSearch = root.findViewById(R.id.etSearch);
        tvTotal = root.findViewById(R.id.tvSummaryTotal);
        tvCount = root.findViewById(R.id.tvSummaryCount);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        TextView tvHeader = root.findViewById(R.id.tvListHeader);
        if (tvHeader != null) tvHeader.setText(" আয়ের তালিকা");
        View header = root.findViewById(R.id.listHeader);
        if (header != null) header.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_header_income));

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
            i.putExtra(AddTransactionActivity.EXTRA_MODE, "income");
            startActivity(i);
        });
        loadData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        allList = new ArrayList<>(db.getIncomeList());
        applyFilter();
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
        double total = 0;
        for (Transaction t : filteredList) total += t.getAmount();
        tvTotal.setText(DatabaseManager.formatAmount(total));
        tvCount.setText(filteredList.size() + " টি এন্ট্রি");
        if (filteredList.isEmpty()) {
            rv.setVisibility(View.GONE);
            View tableContainer = rootView.findViewById(R.id.tableContainer);
            if (tableContainer != null) tableContainer.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            emptyState.setVisibility(View.GONE);
            rv.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_fall_down));
            viewController.render(requireContext(), rootView, filteredList, "income",
                    (item, pos) -> TransactionSheetHelper.showTransactionSheet(requireActivity(), db, "income", item, this::loadData),
                    (item, pos) -> TransactionSheetHelper.showTransactionSheet(requireActivity(), db, "income", item, this::loadData));
            rv.scheduleLayoutAnimation();
        }
    }
}
