package com.jrappspot.cashlipi.activities;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.models.Product;
import com.jrappspot.cashlipi.utils.DatabaseManager;

public class AddProductActivity extends AppCompatActivity {

    public static final String EXTRA_PRODUCT_ID = "extra_product_id";

    private DatabaseManager db;
    private EditText etName, etBarcode, etBuyPrice, etSellPrice, etStockQty, etLowStockAlert, etUnit;
    private TextView tvFormTitle;
    private Product editingProduct = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);
        db = DatabaseManager.getInstance(this);

        tvFormTitle = findViewById(R.id.tvFormTitle);
        etName = findViewById(R.id.etProductName);
        etBarcode = findViewById(R.id.etProductBarcode);
        etBuyPrice = findViewById(R.id.etBuyPrice);
        etSellPrice = findViewById(R.id.etSellPrice);
        etStockQty = findViewById(R.id.etStockQty);
        etLowStockAlert = findViewById(R.id.etLowStockAlert);
        etUnit = findViewById(R.id.etUnit);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSaveProduct).setOnClickListener(v -> saveProduct());

        String productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        if (productId != null) {
            editingProduct = db.getProductById(productId);
            if (editingProduct != null) loadForEdit(editingProduct);
        }
    }

    private void loadForEdit(Product p) {
        tvFormTitle.setText("পণ্য সম্পাদনা করুন");
        etName.setText(p.getName());
        etBarcode.setText(p.getBarcode());
        etBuyPrice.setText(formatForEdit(p.getBuyPrice()));
        etSellPrice.setText(formatForEdit(p.getSellPrice()));
        etStockQty.setText(formatForEdit(p.getStockQty()));
        etLowStockAlert.setText(formatForEdit(p.getLowStockAlert()));
        etUnit.setText(p.getUnit());
    }

    private String formatForEdit(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    private double parseOrZero(EditText et) {
        try {
            String s = et.getText() != null ? et.getText().toString().trim() : "";
            return s.isEmpty() ? 0 : Double.parseDouble(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private void saveProduct() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        if (name.isEmpty()) {
            Toast.makeText(this, "পণ্যের নাম লিখুন", Toast.LENGTH_SHORT).show();
            return;
        }

        Product p = editingProduct != null ? editingProduct : new Product();
        p.setName(name);
        p.setBarcode(etBarcode.getText() != null ? etBarcode.getText().toString().trim() : "");
        p.setBuyPrice(parseOrZero(etBuyPrice));
        p.setSellPrice(parseOrZero(etSellPrice));
        p.setStockQty(parseOrZero(etStockQty));
        p.setLowStockAlert(parseOrZero(etLowStockAlert));
        String unit = etUnit.getText() != null ? etUnit.getText().toString().trim() : "";
        p.setUnit(unit.isEmpty() ? "পিস" : unit);

        if (editingProduct != null) {
            int idx = db.getProductIndexById(editingProduct.getId());
            if (idx >= 0) db.updateProduct(idx, p);
        } else {
            db.addProduct(p);
        }

        Toast.makeText(this, "সংরক্ষণ হয়েছে", Toast.LENGTH_SHORT).show();
        finish();
    }
}
