package com.jrappspot.cashlipi.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.models.Product;
import com.jrappspot.cashlipi.utils.DatabaseManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * পণ্য/স্টক তালিকা — বর্তমান সক্রিয় দোকানের (shop-scoped) সব পণ্য, মজুদ, ক্রয়-বিক্রয় মূল্য ও
 * লাভের সামারি একসাথে দেখায়। কার্ড থেকে সরাসরি "+ পণ্য যোগ" / "− পণ্য বাদ" দিয়ে মজুদ
 * দ্রুত আপডেট করা যায়।
 */
public class StockActivity extends AppCompatActivity {

    private DatabaseManager db;
    private RecyclerView rvProducts;
    private View emptyState;
    private EditText etSearch;
    private ProductAdapter adapter;
    private final List<Product> allProducts = new ArrayList<>();
    private String query = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_list);
        db = DatabaseManager.getInstance(this);

        rvProducts = findViewById(R.id.rvProducts);
        emptyState = findViewById(R.id.emptyProductState);
        etSearch = findViewById(R.id.etProductSearch);

        rvProducts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductAdapter();
        rvProducts.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnRefreshStock).setOnClickListener(v -> loadData());
        findViewById(R.id.btnAddProduct).setOnClickListener(v ->
                startActivity(new Intent(this, AddProductActivity.class)));

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                query = s.toString().trim();
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        allProducts.clear();
        allProducts.addAll(db.getProductList());
        refreshSummary();
        applyFilter();
    }

    private void refreshSummary() {
        setStat(R.id.statTotalProducts, "মোট পণ্য", String.valueOf(allProducts.size()));
        setStat(R.id.statBuyValue, "মোট ক্রয় মূল্য", DatabaseManager.formatAmount(db.getTotalStockBuyValue()));
        setStat(R.id.statSellValue, "মোট বিক্রয় মূল্য", DatabaseManager.formatAmount(db.getTotalStockSellValue()));
        setStat(R.id.statTotalStock, "মোট মজুদ", DatabaseManager.formatAmount(db.getTotalStockQty()));
        setStat(R.id.statTotalProfit, "মোট লাভ", "+" + DatabaseManager.formatAmount(db.getTotalStockProfit()));
        setStat(R.id.statLowStock, "কম স্টক আইটেম", String.valueOf(db.getLowStockProducts().size()));
    }

    private void setStat(int includeId, String label, String value) {
        View root = findViewById(includeId);
        ((TextView) root.findViewById(R.id.tvStatLabel)).setText(label);
        ((TextView) root.findViewById(R.id.tvStatValue)).setText(value);
    }

    private void applyFilter() {
        List<Product> filtered = new ArrayList<>();
        String q = query.toLowerCase(Locale.ROOT);
        for (Product p : allProducts) {
            if (q.isEmpty() || p.getName().toLowerCase(Locale.ROOT).contains(q)
                    || p.getBarcode().contains(q)) {
                filtered.add(p);
            }
        }
        adapter.setItems(filtered);
        rvProducts.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showAdjustStockDialog(Product product, boolean isAdd) {
        EditText et = new EditText(this);
        et.setHint(isAdd ? "কত " + product.getUnit() + " যোগ হবে?" : "কত " + product.getUnit() + " বাদ যাবে?");
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        et.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(this)
                .setTitle((isAdd ? "পণ্য যোগ — " : "পণ্য বাদ — ") + product.getName())
                .setView(et)
                .setPositiveButton(isAdd ? "যোগ করুন" : "বাদ দিন", (d, w) -> {
                    double qty;
                    try {
                        qty = Double.parseDouble(et.getText().toString().trim());
                        if (qty <= 0) throw new NumberFormatException();
                    } catch (Exception e) {
                        Toast.makeText(this, "সঠিক পরিমাণ লিখুন", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.adjustProductStock(product.getId(), isAdd ? qty : -qty);
                    loadData();
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    private class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.VH> {
        private List<Product> items = new ArrayList<>();

        void setItems(List<Product> items) { this.items = items; notifyDataSetChanged(); }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Product p = items.get(position);
            h.tvInitial.setText(p.getInitial());
            h.tvName.setText(p.getName());
            h.tvStock.setText("মজুদ আছে: " + DatabaseManager.formatAmount(p.getStockQty()) + " " + p.getUnit());
            h.tvPrices.setText("ক্রয়: ৳" + DatabaseManager.formatAmount(p.getBuyPrice())
                    + "  |  বিক্রয়: ৳" + DatabaseManager.formatAmount(p.getSellPrice()));
            h.tvLowStockBadge.setVisibility(p.isLowStock() ? View.VISIBLE : View.GONE);

            h.root.setOnClickListener(v -> {
                Intent i = new Intent(StockActivity.this, AddProductActivity.class);
                i.putExtra(AddProductActivity.EXTRA_PRODUCT_ID, p.getId());
                startActivity(i);
            });
            h.btnAdd.setOnClickListener(v -> showAdjustStockDialog(p, true));
            h.btnRemove.setOnClickListener(v -> showAdjustStockDialog(p, false));
        }

        @Override
        public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            View root;
            TextView tvInitial, tvName, tvStock, tvPrices, tvLowStockBadge, btnAdd, btnRemove;
            VH(@NonNull View itemView) {
                super(itemView);
                root = itemView.findViewById(R.id.productRowRoot);
                tvInitial = itemView.findViewById(R.id.tvProductInitial);
                tvName = itemView.findViewById(R.id.tvProductName);
                tvStock = itemView.findViewById(R.id.tvProductStock);
                tvPrices = itemView.findViewById(R.id.tvProductPrices);
                tvLowStockBadge = itemView.findViewById(R.id.tvLowStockBadge);
                btnAdd = itemView.findViewById(R.id.btnStockAdd);
                btnRemove = itemView.findViewById(R.id.btnStockRemove);
            }
        }
    }
}
