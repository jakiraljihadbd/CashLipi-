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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.activities.AddSavingsActivity;
import com.jrappspot.cashlipi.adapters.TransactionListAdapter;
import com.jrappspot.cashlipi.adapters.TransactionTableAdapter;
import com.jrappspot.cashlipi.models.Transaction;
import com.jrappspot.cashlipi.utils.DatabaseManager;
import com.jrappspot.cashlipi.utils.DateFilterUtil;
import com.jrappspot.cashlipi.utils.TransactionSheetHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * সঞ্চয় পেজ — আয়-ব্যয় পেজের ভিজ্যুয়াল স্টাইলে (পিংক থিম) রিডিজাইন করা।
 * আয়/ব্যয়ের মতো টাইপ-টগল নেই (দরকার নেই), উপরে শুধু চিকন এন্ট্রি-কাউন্ট স্ট্রিপ,
 * তারপর মোট সঞ্চয়ের কমপ্যাক্ট কার্ড। নিচের বাকি অংশ (সার্চ, সর্ট, কার্ড/ছক ভিউ,
 * ফিল্টার চিপস, লিস্ট, ফ্লোটিং অ্যাড বাটন) আয়-ব্যয়ের মতোই, শুধু রঙ পিংক।
 */
public class SavingsFragment extends Fragment {

    private DatabaseManager db;
    private RecyclerView rv, rvTable;
    private View tableContainer, tableHeader;
    private LinearLayout emptyState;
    private EditText etSearch;
    private TextView tvTotal, tvCount;
    private TextView btnViewCard, btnViewTable;
    private List<Transaction> allList = new ArrayList<>();
    private List<Transaction> filteredList = new ArrayList<>();
    private String currentFilter = "all";
    private String currentSort = "date_desc"; // date_desc | date_asc | amount_desc | amount_asc | name_asc
    private String viewMode = "card"; // "card" | "table"

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_savings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);
        db = DatabaseManager.getInstance(requireContext());
        rv = root.findViewById(R.id.rvList);
        rvTable = root.findViewById(R.id.rvTable);
        tableContainer = root.findViewById(R.id.tableContainer);
        tableHeader = root.findViewById(R.id.tableHeader);
        emptyState = root.findViewById(R.id.emptyState);
        etSearch = root.findViewById(R.id.etSearch);
        tvTotal = root.findViewById(R.id.tvSummaryTotal);
        tvCount = root.findViewById(R.id.tvSummaryCount);
        btnViewCard = root.findViewById(R.id.btnViewCard);
        btnViewTable = root.findViewById(R.id.btnViewTable);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        TextView tvColSource = tableHeader.findViewById(R.id.tvColSource);
        if (tvColSource != null) tvColSource.setText("বিবরণ");
        tableHeader.setBackgroundResource(R.drawable.bg_table_header_savings);

        setupFilterChips(root);

        btnViewCard.setOnClickListener(v -> switchViewMode("card"));
        btnViewTable.setOnClickListener(v -> switchViewMode("table"));
        refreshViewModeUI();

        ImageView btnSort = root.findViewById(R.id.btnSort);
        if (btnSort != null) btnSort.setOnClickListener(v -> showSortMenu(btnSort));

        ImageView ivClearS = root.findViewById(R.id.ivClearSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                if (ivClearS != null) ivClearS.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                applyFilter();
            }
            public void afterTextChanged(Editable s) {}
        });
        if (ivClearS != null) ivClearS.setOnClickListener(v -> { etSearch.setText(""); etSearch.requestFocus(); });
        root.findViewById(R.id.btnAddNew).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddSavingsActivity.class)));
        loadData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        allList = new ArrayList<>(db.getSavingsList());
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
                    selected ? R.drawable.bg_chip_selected_pink : R.drawable.bg_chip_unselected));
            chip.setTextColor(selected ? ContextCompat.getColor(requireContext(), R.color.white)
                    : ContextCompat.getColor(requireContext(), R.color.chipUnselectedText));
            if (selected) chip.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.chip_scale));
            chip.setOnClickListener(v -> { currentFilter = key; setupFilterChips(root); applyFilter(); });
            chipRow.addView(chip);
        }
    }

    private void switchViewMode(String mode) {
        if (mode.equals(viewMode)) return;
        viewMode = mode;
        refreshViewModeUI();
        applyFilter();
    }

    private void refreshViewModeUI() {
        boolean isCard = "card".equals(viewMode);
        btnViewCard.setBackground(ContextCompat.getDrawable(requireContext(),
                isCard ? R.drawable.bg_chip_selected_pink : R.drawable.bg_chip_unselected));
        btnViewCard.setTextColor(ContextCompat.getColor(requireContext(), isCard ? R.color.white : R.color.chipUnselectedText));
        btnViewTable.setBackground(ContextCompat.getDrawable(requireContext(),
                !isCard ? R.drawable.bg_chip_selected_pink : R.drawable.bg_chip_unselected));
        btnViewTable.setTextColor(ContextCompat.getColor(requireContext(), !isCard ? R.color.white : R.color.chipUnselectedText));
        rv.setVisibility(isCard ? View.VISIBLE : View.GONE);
        tableContainer.setVisibility(isCard ? View.GONE : View.VISIBLE);
    }

    private void showSortMenu(View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenuInflater().inflate(R.menu.menu_sort_options, menu.getMenu());
        menu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.sortDateDesc) currentSort = "date_desc";
            else if (id == R.id.sortDateAsc) currentSort = "date_asc";
            else if (id == R.id.sortAmountDesc) currentSort = "amount_desc";
            else if (id == R.id.sortAmountAsc) currentSort = "amount_asc";
            else if (id == R.id.sortNameAsc) currentSort = "name_asc";
            applyFilter();
            return true;
        });
        menu.show();
    }

    private void sortFilteredList(List<Transaction> list) {
        switch (currentSort) {
            case "date_asc":
                list.sort((a, b) -> (a.getDate() + a.getTime()).compareTo(b.getDate() + b.getTime()));
                break;
            case "amount_desc":
                list.sort((a, b) -> Double.compare(b.getAmount(), a.getAmount()));
                break;
            case "amount_asc":
                list.sort((a, b) -> Double.compare(a.getAmount(), b.getAmount()));
                break;
            case "name_asc":
                list.sort((a, b) -> a.getDisplayTitle().compareToIgnoreCase(b.getDisplayTitle()));
                break;
            default: // date_desc
                list.sort((a, b) -> (b.getDate() + b.getTime()).compareTo(a.getDate() + a.getTime()));
                break;
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
        sortFilteredList(filteredList);

        double total = 0;
        for (Transaction t : filteredList) total += t.getAmount();
        tvTotal.setText(DatabaseManager.formatAmount(total));
        tvCount.setText(filteredList.size() + " টি এন্ট্রি");

        if (filteredList.isEmpty()) {
            rv.setVisibility(View.GONE);
            tableContainer.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            return;
        }
        emptyState.setVisibility(View.GONE);

        if ("card".equals(viewMode)) {
            rv.setVisibility(View.VISIBLE);
            tableContainer.setVisibility(View.GONE);
            rv.setAdapter(new TransactionListAdapter(requireContext(), filteredList, "savings", (item, pos) ->
                    TransactionSheetHelper.showTransactionSheet(requireActivity(), db, "savings", item, this::loadData), null));
            rv.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_fall_down));
            rv.scheduleLayoutAnimation();
        } else {
            rv.setVisibility(View.GONE);
            tableContainer.setVisibility(View.VISIBLE);
            if (rvTable.getLayoutManager() == null) {
                rvTable.setLayoutManager(new LinearLayoutManager(requireContext()));
                rvTable.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
            }
            rvTable.setAdapter(new TransactionTableAdapter(requireContext(), filteredList, "savings", (item, pos) ->
                    TransactionSheetHelper.showTransactionSheet(requireActivity(), db, "savings", item, this::loadData)));
        }
    }
}
