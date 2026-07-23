package com.jrappspot.cashlipi.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.models.KhataExpense;
import com.jrappspot.cashlipi.utils.DatabaseManager;

import java.util.List;
import java.util.Map;

/**
 * বাকির খাতার ব্যবসায়িক খরচের পূর্ণাঙ্গ তালিকা ও ক্যাটাগরি-ভিত্তিক রিপোর্ট।
 * fragment_bakir_khata-এর "+ খরচ" বাটনে চেপে ধরলে (long-press) এখানে আসা যায়।
 */
public class KhataExpenseActivity extends AppCompatActivity {

    private DatabaseManager db;
    private RecyclerView rvList;
    private LinearLayout categoryTotalsRow, emptyState;
    private TextView tvTotalExpense;
    private ExpenseAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_khata_expense);
        db = DatabaseManager.getInstance(this);

        rvList = findViewById(R.id.rvExpenseList);
        categoryTotalsRow = findViewById(R.id.categoryTotalsRow);
        emptyState = findViewById(R.id.emptyExpenseState);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);

        rvList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExpenseAdapter();
        rvList.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddExpenseHeader).setOnClickListener(v -> showAddExpenseDialog());

        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        List<KhataExpense> list = db.getKhataExpenseList();
        adapter.setItems(list);
        rvList.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        tvTotalExpense.setText("৳" + DatabaseManager.formatAmount(db.getTotalKhataExpense()));

        categoryTotalsRow.removeAllViews();
        Map<String, Double> totals = db.getKhataExpenseTotalsByCategory();
        int pad = (int) (10 * getResources().getDisplayMetrics().density);
        for (Map.Entry<String, Double> e : totals.entrySet()) {
            LinearLayout chip = new LinearLayout(this);
            chip.setOrientation(LinearLayout.VERTICAL);
            chip.setBackgroundResource(R.drawable.bg_card_white);
            chip.setPadding(pad, pad, pad, pad);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(pad);
            chip.setLayoutParams(lp);

            TextView tvCat = new TextView(this);
            tvCat.setText(e.getKey());
            tvCat.setTextSize(11.5f);
            tvCat.setTextColor(getResources().getColor(R.color.textSecondary));
            chip.addView(tvCat);

            TextView tvAmt = new TextView(this);
            tvAmt.setText("৳" + DatabaseManager.formatAmount(e.getValue()));
            tvAmt.setTextSize(14.5f);
            tvAmt.setTypeface(null, android.graphics.Typeface.BOLD);
            tvAmt.setTextColor(getResources().getColor(R.color.bakiColor));
            chip.addView(tvAmt);

            categoryTotalsRow.addView(chip);
        }
    }

    private void showAddExpenseDialog() {
        List<String> categories = db.getKhataExpenseCategories();
        String[] catArr = categories.toArray(new String[0]);
        final int[] selected = {0};

        EditText et = new EditText(this);
        et.setHint("খরচের পরিমাণ");
        et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        et.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(this)
                .setTitle("ব্যবসায়িক খরচ যোগ করুন")
                .setSingleChoiceItems(catArr, 0, (d, which) -> selected[0] = which)
                .setView(et)
                .setPositiveButton("সংরক্ষণ করুন", (d, w) -> {
                    double amt;
                    try {
                        amt = Double.parseDouble(et.getText().toString().trim());
                        if (amt <= 0) throw new NumberFormatException();
                    } catch (Exception ex) {
                        Toast.makeText(this, "সঠিক পরিমাণ লিখুন", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    KhataExpense e = new KhataExpense();
                    e.setCategory(catArr.length > 0 ? catArr[selected[0]] : "অন্যান্য");
                    e.setAmount(amt);
                    e.setPayFromWallet(db.getKhataWalletBalance() >= amt);
                    db.addKhataExpense(e);
                    Toast.makeText(this, "খরচ যোগ হয়েছে", Toast.LENGTH_SHORT).show();
                    loadData();
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    private void confirmDelete(KhataExpense item) {
        new AlertDialog.Builder(this)
                .setTitle("মুছে ফেলবেন?")
                .setMessage("\"" + item.getCategory() + "\" খরচের এন্ট্রিটি মুছে যাবে।")
                .setPositiveButton("হ্যাঁ, মুছুন", (d, w) -> {
                    int idx = db.getKhataExpenseIndexById(item.getId());
                    if (idx >= 0) db.deleteKhataExpense(idx);
                    loadData();
                })
                .setNegativeButton("না", null)
                .show();
    }

    private class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.VH> {
        private List<KhataExpense> items;

        void setItems(List<KhataExpense> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_khata_expense, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            KhataExpense e = items.get(position);
            h.tvCategory.setText(e.getCategory());
            h.tvMeta.setText(DatabaseManager.formatDateDisplay(e.getDate()) + "  •  " + DatabaseManager.formatTimeDisplay(e.getTime())
                    + (e.isPayFromWallet() ? "  •  ওয়ালেট থেকে" : ""));
            h.tvAmount.setText("৳" + DatabaseManager.formatAmount(e.getAmount()));
            h.itemView.setOnLongClickListener(v -> {
                confirmDelete(e);
                return true;
            });
        }

        @Override
        public int getItemCount() { return items == null ? 0 : items.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvCategory, tvMeta, tvAmount;
            VH(@NonNull View itemView) {
                super(itemView);
                tvCategory = itemView.findViewById(R.id.tvExpenseCategory);
                tvMeta = itemView.findViewById(R.id.tvExpenseMeta);
                tvAmount = itemView.findViewById(R.id.tvExpenseAmount);
            }
        }
    }
}
