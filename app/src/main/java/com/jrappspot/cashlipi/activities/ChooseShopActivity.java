package com.jrappspot.cashlipi.activities;

import android.content.Intent;
import android.os.Bundle;
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
import com.jrappspot.cashlipi.models.Shop;
import com.jrappspot.cashlipi.utils.DatabaseManager;

import java.util.List;

/**
 * একই একাউন্টে একাধিক দোকান/ব্যবসার হিসাব আলাদা আলাদাভাবে রাখার জন্য দোকান পরিবর্তনের স্ক্রিন।
 * একটা দোকান সিলেক্ট করলে সেটাই সক্রিয় (active) হয়ে যায় — এরপর বাকির খাতা, স্টক, ইনভয়েস
 * সবকিছু ওই দোকানের নিজস্ব ডেটা দেখাবে (DatabaseManager.shopScope() দেখুন)।
 */
public class ChooseShopActivity extends AppCompatActivity {

    public static final String RESULT_SHOP_CHANGED = "shop_changed";

    private DatabaseManager db;
    private RecyclerView rvShops;
    private ShopAdapter adapter;
    private boolean shopChanged = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_shop);
        db = DatabaseManager.getInstance(this);

        rvShops = findViewById(R.id.rvShops);
        rvShops.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ShopAdapter();
        rvShops.setAdapter(adapter);

        findViewById(R.id.btnClose).setOnClickListener(v -> finishScreen());
        findViewById(R.id.btnAddShop).setOnClickListener(v -> showAddShopDialog());

        loadShops();
    }

    @Override
    public void onBackPressed() {
        finishScreen();
    }

    private void finishScreen() {
        Bundle result = new Bundle();
        result.putBoolean(RESULT_SHOP_CHANGED, shopChanged);
        Intent data = new Intent();
        data.putExtras(result);
        setResult(RESULT_OK, data);
        finish();
    }

    private void loadShops() {
        adapter.setItems(db.getShopList(), db.getActiveShopId());
    }

    private void showAddShopDialog() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        EditText etName = new EditText(this);
        etName.setHint("দোকানের নাম");
        container.addView(etName);

        EditText etBranch = new EditText(this);
        etBranch.setHint("শাখার নাম (ঐচ্ছিক) — যেমন Main Branch");
        container.addView(etBranch);

        new AlertDialog.Builder(this)
                .setTitle("নতুন দোকান যোগ করুন")
                .setView(container)
                .setPositiveButton("যোগ করুন", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "দোকানের নাম লিখুন", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Shop shop = new Shop(name, etBranch.getText().toString().trim());
                    db.addShop(shop);
                    db.setActiveShopId(shop.getId());
                    shopChanged = true;
                    loadShops();
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    private class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.VH> {
        private List<Shop> items;
        private String activeId;

        void setItems(List<Shop> items, String activeId) {
            this.items = items;
            this.activeId = activeId;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shop_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Shop shop = items.get(position);
            h.tvName.setText(shop.getDisplayName());
            boolean isActive = shop.getId().equals(activeId);
            h.ivCheck.setVisibility(isActive ? View.VISIBLE : View.INVISIBLE);
            h.root.setOnClickListener(v -> {
                if (!shop.getId().equals(activeId)) {
                    db.setActiveShopId(shop.getId());
                    shopChanged = true;
                    loadShops();
                }
            });
        }

        @Override
        public int getItemCount() { return items == null ? 0 : items.size(); }

        class VH extends RecyclerView.ViewHolder {
            View root;
            TextView tvName;
            View ivCheck;
            VH(@NonNull View itemView) {
                super(itemView);
                root = itemView.findViewById(R.id.shopRowRoot);
                tvName = itemView.findViewById(R.id.tvShopName);
                ivCheck = itemView.findViewById(R.id.ivShopCheck);
            }
        }
    }
}
