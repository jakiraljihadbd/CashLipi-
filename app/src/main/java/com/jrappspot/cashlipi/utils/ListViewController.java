package com.jrappspot.cashlipi.utils;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.adapters.TransactionListAdapter;
import com.jrappspot.cashlipi.adapters.TransactionTableAdapter;
import com.jrappspot.cashlipi.models.Transaction;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * আয়/ব্যয় লিস্ট পেজের জন্য "কার্ড" ও "ছক" (টেবিল) ভিউ টগল + সর্টিং একসাথে সামলায়।
 * IncomeListActivity, ExpenseListActivity, IncomeExpenseFragment — তিনটাতেই একই আচরণ থাকার জন্য
 * এই লজিক একটাই জায়গায় রাখা হয়েছে।
 */
public class ListViewController {

    public String viewMode = "card";     // "card" | "table"
    public String sortMode = "date_desc"; // date_desc | date_asc | amount_desc | amount_asc | name_asc

    /** টগল বাটন ও সর্ট বাটনের ক্লিক লিসেনার বসায়। onChange কল হয় মোড/সর্ট বদলালে। */
    public void attachControls(Context ctx, View root, Runnable onChange) {
        TextView btnCard = root.findViewById(R.id.btnViewCard);
        TextView btnTable = root.findViewById(R.id.btnViewTable);
        ImageView btnSort = root.findViewById(R.id.btnSort);
        if (btnCard == null || btnTable == null) return;

        refreshToggleUI(ctx, btnCard, btnTable);

        btnCard.setOnClickListener(v -> {
            if (!"card".equals(viewMode)) {
                viewMode = "card";
                refreshToggleUI(ctx, btnCard, btnTable);
                onChange.run();
            }
        });
        btnTable.setOnClickListener(v -> {
            if (!"table".equals(viewMode)) {
                viewMode = "table";
                refreshToggleUI(ctx, btnCard, btnTable);
                onChange.run();
            }
        });
        if (btnSort != null) {
            btnSort.setOnClickListener(v -> {
                PopupMenu menu = new PopupMenu(ctx, btnSort);
                menu.getMenuInflater().inflate(R.menu.menu_sort_options, menu.getMenu());
                menu.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.sortDateDesc) sortMode = "date_desc";
                    else if (id == R.id.sortDateAsc) sortMode = "date_asc";
                    else if (id == R.id.sortAmountDesc) sortMode = "amount_desc";
                    else if (id == R.id.sortAmountAsc) sortMode = "amount_asc";
                    else if (id == R.id.sortNameAsc) sortMode = "name_asc";
                    onChange.run();
                    return true;
                });
                menu.show();
            });
        }
    }

    private void refreshToggleUI(Context ctx, TextView btnCard, TextView btnTable) {
        boolean isCard = "card".equals(viewMode);
        btnCard.setBackground(ContextCompat.getDrawable(ctx,
                isCard ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected));
        btnCard.setTextColor(ContextCompat.getColor(ctx, isCard ? R.color.white : R.color.chipUnselectedText));
        btnTable.setBackground(ContextCompat.getDrawable(ctx,
                !isCard ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected));
        btnTable.setTextColor(ContextCompat.getColor(ctx, !isCard ? R.color.white : R.color.chipUnselectedText));
    }

    /** currentSortMode অনুযায়ী লিস্ট সর্ট করে (in-place)। */
    public void applySort(List<Transaction> list) {
        Comparator<Transaction> cmp;
        switch (sortMode) {
            case "date_asc":
                cmp = (a, b) -> (a.getDate() + a.getTime()).compareTo(b.getDate() + b.getTime());
                break;
            case "amount_desc":
                cmp = (a, b) -> Double.compare(b.getAmount(), a.getAmount());
                break;
            case "amount_asc":
                cmp = (a, b) -> Double.compare(a.getAmount(), b.getAmount());
                break;
            case "name_asc":
                cmp = (a, b) -> a.getDisplayTitle().compareToIgnoreCase(b.getDisplayTitle());
                break;
            default: // date_desc
                cmp = (a, b) -> (b.getDate() + b.getTime()).compareTo(a.getDate() + a.getTime());
                break;
        }
        Collections.sort(list, cmp);
    }

    /**
     * ভিউমোড অনুযায়ী rvList (কার্ড) বা tableContainer+rvTable (ছক) দেখায়/হাইড করে এবং
     * উপযুক্ত অ্যাডাপ্টার বসায়। "type" = income | expense | savings.
     */
    public void render(Context ctx, View root, List<Transaction> filteredList, String type,
                        TransactionListAdapter.OnItemClickListener cardClick,
                        TransactionTableAdapter.OnItemClickListener tableClick) {

        RecyclerView rvList = root.findViewById(R.id.rvList);
        LinearLayout tableContainer = root.findViewById(R.id.tableContainer);
        RecyclerView rvTable = root.findViewById(R.id.rvTable);
        View tableHeader = root.findViewById(R.id.tableHeader);

        boolean isCard = "card".equals(viewMode);

        if (rvList != null) rvList.setVisibility(isCard ? View.VISIBLE : View.GONE);
        if (tableContainer != null) tableContainer.setVisibility(isCard ? View.GONE : View.VISIBLE);

        if (isCard) {
            if (rvList != null) {
                rvList.setAdapter(new TransactionListAdapter(ctx, filteredList, type, cardClick, null));
            }
        } else {
            if (rvTable != null) {
                if (rvTable.getLayoutManager() == null) {
                    rvTable.setLayoutManager(new LinearLayoutManager(ctx));
                    rvTable.addItemDecoration(new DividerItemDecoration(ctx, DividerItemDecoration.VERTICAL));
                }
                rvTable.setAdapter(new TransactionTableAdapter(ctx, filteredList, type, tableClick));
            }
            if (tableHeader != null) {
                TextView tvColSource = tableHeader.findViewById(R.id.tvColSource);
                if (tvColSource != null) {
                    tvColSource.setText("expense".equals(type) ? "ক্যাটাগরি" : "উৎস");
                }
                tableHeader.setBackgroundResource(
                        "expense".equals(type) ? R.drawable.bg_table_header_expense : R.drawable.bg_table_header_income);
            }
        }
    }
}
