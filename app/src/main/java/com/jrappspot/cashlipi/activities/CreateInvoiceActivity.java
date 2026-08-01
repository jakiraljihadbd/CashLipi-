package com.jrappspot.cashlipi.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.models.InvoiceItem;
import com.jrappspot.cashlipi.models.SalesInvoice;
import com.jrappspot.cashlipi.utils.DatabaseManager;

import java.util.ArrayList;
import java.util.List;

/**
 * পণ্যের ক্রয়/বিক্রয়ের নতুন ইনভয়েস তৈরি — একাধিক আইটেম যোগ করা যায়, প্রতিটার পরিমাণ×দর
 * থেকে স্বয়ংক্রিয় টাকা ও সর্বমোট হিসাব হয়। সংরক্ষণের পর টেক্সট আকারে যেকোনো অ্যাপে
 * (WhatsApp/Messenger/Telegram ইত্যাদি) শেয়ার করা যায়।
 */
public class CreateInvoiceActivity extends AppCompatActivity {

    private DatabaseManager db;
    private LinearLayout itemRowsContainer;
    private EditText etCustomerName, etCustomerAddress, etNote;
    private TextView tvTotal;
    private final List<View> itemRows = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_invoice);
        db = DatabaseManager.getInstance(this);

        itemRowsContainer = findViewById(R.id.itemRowsContainer);
        etCustomerName = findViewById(R.id.etCustomerName);
        etCustomerAddress = findViewById(R.id.etCustomerAddress);
        etNote = findViewById(R.id.etInvoiceNote);
        tvTotal = findViewById(R.id.tvInvoiceTotal);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddItemRow).setOnClickListener(v -> addItemRow());
        findViewById(R.id.btnSaveInvoice).setOnClickListener(v -> saveInvoice(false));
        findViewById(R.id.btnSaveAndShare).setOnClickListener(v -> saveInvoice(true));

        addItemRow(); // শুরুতে একটা খালি সারি থাকবে
    }

    private void addItemRow() {
        View row = LayoutInflater.from(this).inflate(R.layout.item_invoice_row, itemRowsContainer, false);
        EditText etQty = row.findViewById(R.id.etItemQty);
        EditText etRate = row.findViewById(R.id.etItemRate);
        TextView tvAmount = row.findViewById(R.id.tvItemAmount);

        TextWatcher recalc = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                double qty = parseOrZero(etQty);
                double rate = parseOrZero(etRate);
                tvAmount.setText("৳" + DatabaseManager.formatAmount(qty * rate));
                refreshGrandTotal();
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        etQty.addTextChangedListener(recalc);
        etRate.addTextChangedListener(recalc);

        row.findViewById(R.id.btnRemoveItem).setOnClickListener(v -> {
            itemRowsContainer.removeView(row);
            itemRows.remove(row);
            refreshGrandTotal();
        });

        itemRowsContainer.addView(row);
        itemRows.add(row);
    }

    private double parseOrZero(EditText et) {
        try {
            String s = et.getText() != null ? et.getText().toString().trim() : "";
            return s.isEmpty() ? 0 : Double.parseDouble(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private void refreshGrandTotal() {
        double total = 0;
        for (View row : itemRows) {
            EditText etQty = row.findViewById(R.id.etItemQty);
            EditText etRate = row.findViewById(R.id.etItemRate);
            total += parseOrZero(etQty) * parseOrZero(etRate);
        }
        tvTotal.setText("৳" + DatabaseManager.formatAmount(total));
    }

    private List<InvoiceItem> collectItems() {
        List<InvoiceItem> items = new ArrayList<>();
        for (View row : itemRows) {
            EditText etName = row.findViewById(R.id.etItemName);
            EditText etQty = row.findViewById(R.id.etItemQty);
            EditText etRate = row.findViewById(R.id.etItemRate);
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            double qty = parseOrZero(etQty);
            double rate = parseOrZero(etRate);
            if (name.isEmpty() && qty == 0 && rate == 0) continue; // পুরোপুরি খালি সারি বাদ
            items.add(new InvoiceItem(name.isEmpty() ? "আইটেম" : name, qty, rate));
        }
        return items;
    }

    private void saveInvoice(boolean shareAfter) {
        List<InvoiceItem> items = collectItems();
        if (items.isEmpty()) {
            Toast.makeText(this, "অন্তত একটা আইটেম যোগ করুন", Toast.LENGTH_SHORT).show();
            return;
        }

        SalesInvoice inv = new SalesInvoice();
        inv.setCustomerName(etCustomerName.getText() != null ? etCustomerName.getText().toString().trim() : "");
        inv.setCustomerAddress(etCustomerAddress.getText() != null ? etCustomerAddress.getText().toString().trim() : "");
        inv.setItems(items);
        inv.setNote(etNote.getText() != null ? etNote.getText().toString().trim() : "");
        SalesInvoice saved = db.addInvoice(inv);

        Toast.makeText(this, "ইনভয়েস সংরক্ষণ হয়েছে", Toast.LENGTH_SHORT).show();

        if (shareAfter) {
            shareInvoiceText(saved);
        } else {
            finish();
        }
    }

    private void shareInvoiceText(SalesInvoice inv) {
        StringBuilder sb = new StringBuilder();
        sb.append(db.getActiveShop().getName()).append("\n");
        sb.append("ইনভয়েস #").append(inv.getInvoiceNo())
                .append("  •  ").append(DatabaseManager.formatDateDisplay(inv.getDate())).append("\n");
        if (!inv.getCustomerName().isEmpty()) sb.append("ক্রেতা: ").append(inv.getCustomerName()).append("\n");
        if (!inv.getCustomerAddress().isEmpty()) sb.append(inv.getCustomerAddress()).append("\n");
        sb.append("─────────────────────\n");
        for (InvoiceItem it : inv.getItems()) {
            sb.append(it.getName()).append("  x").append(DatabaseManager.formatAmount(it.getQty()))
                    .append(" @ ৳").append(DatabaseManager.formatAmount(it.getRate()))
                    .append(" = ৳").append(DatabaseManager.formatAmount(it.getAmount())).append("\n");
        }
        sb.append("─────────────────────\n");
        sb.append("সর্বমোট: ৳").append(DatabaseManager.formatAmount(inv.getTotal())).append("\n");
        if (!inv.getNote().isEmpty()) sb.append("\nনোট: ").append(inv.getNote());

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(intent, "ইনভয়েস শেয়ার করুন"));
        finish();
    }
}
