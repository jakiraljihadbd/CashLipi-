package com.jrappspot.cashlipi.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.utils.FontUtils;
import com.jrappspot.cashlipi.adapters.KhataEntryListAdapter;
import com.jrappspot.cashlipi.adapters.MainPagerAdapter;
import com.jrappspot.cashlipi.models.KhataEntry;
import com.jrappspot.cashlipi.models.KhataCustomer;
import com.jrappspot.cashlipi.utils.AmountInputHelper;
import com.jrappspot.cashlipi.utils.DatabaseManager;
import com.jrappspot.cashlipi.utils.FirestoreSyncManager;
import com.jrappspot.cashlipi.utils.SuccessPopup;

import java.util.Calendar;
import java.util.List;

public class AddKhataEntryActivity extends BaseActivity {

    private DatabaseManager db;
    private TextInputEditText etKhataCustomer, etCategory, etAmount, etNote;
    private TextInputLayout tilKhataCustomer, tilAmount;
    private TextView tvDate, tvTime;
    private Button btnTypeDena, btnTypePabona;
    private RecyclerView rvRecentKhataEntry;
    private LinearLayout emptyKhataEntryState;

    private String selectedType = "baki";
    private String selectedDate = "", selectedTime = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_khata_entry);
        FontUtils.applyToView(this, findViewById(android.R.id.content));
        db = DatabaseManager.getInstance(this);
        initViews();
        setDefaultDateTime();
        setupTypeToggle();
        setupQuickAmounts();
        setupClickListeners();
        loadRecentKhataEntry();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecentKhataEntry();
    }

    private void initViews() {
        etKhataCustomer    = findViewById(R.id.etKhataCustomer);
        etCategory  = findViewById(R.id.etCategory);
        etAmount    = findViewById(R.id.etAmount);
        etNote      = findViewById(R.id.etNote);
        tilKhataCustomer   = findViewById(R.id.tilKhataCustomer);
        tilAmount   = findViewById(R.id.tilAmount);
        tvDate      = findViewById(R.id.tvDate);
        tvTime      = findViewById(R.id.tvTime);
        btnTypeDena   = findViewById(R.id.btnTypeDena);
        btnTypePabona = findViewById(R.id.btnTypePabona);
        rvRecentKhataEntry   = findViewById(R.id.rvRecentKhataEntry);
        emptyKhataEntryState = findViewById(R.id.emptyKhataEntryState);
        rvRecentKhataEntry.setLayoutManager(new LinearLayoutManager(this));
        rvRecentKhataEntry.setNestedScrollingEnabled(false);

        // Attach custom calculator keyboard
        AmountInputHelper.attach(this, etAmount);
    }

    private void setDefaultDateTime() {
        selectedDate = DatabaseManager.nowDate();
        selectedTime = DatabaseManager.nowTime();
        tvDate.setText(DatabaseManager.formatDateDisplay(selectedDate));
        tvTime.setText(DatabaseManager.formatTimeDisplay(selectedTime));
    }

    private void setupTypeToggle() {
        updateTypeUI();
        btnTypeDena.setOnClickListener(v -> {
            selectedType = "baki";
            updateTypeUI();
        });
        btnTypePabona.setOnClickListener(v -> {
            selectedType = "joma";
            updateTypeUI();
        });
    }

    private void updateTypeUI() {
        if ("baki".equals(selectedType)) {
            btnTypeDena.setBackground(getResources().getDrawable(R.drawable.bg_type_active_dena));
            btnTypeDena.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white));
            btnTypePabona.setBackground(getResources().getDrawable(R.drawable.bg_type_inactive_pabona));
            btnTypePabona.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.pabonaColor));
        } else {
            btnTypePabona.setBackground(getResources().getDrawable(R.drawable.bg_type_active_pabona));
            btnTypePabona.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white));
            btnTypeDena.setBackground(getResources().getDrawable(R.drawable.bg_type_inactive_dena));
            btnTypeDena.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.denaColor));
        }
    }

    private void setupQuickAmounts() {
        int[] ids = {R.id.btn50,R.id.btn100,R.id.btn500,R.id.btn1000,
                     R.id.btn2000,R.id.btn5000,R.id.btn10000,R.id.btn20000};
        int[] vals = {5,10,20,50,100,200,500,1000};
        for (int i = 0; i < ids.length; i++) {
            final int val = vals[i];
            Button btn = findViewById(ids[i]);
            if (btn != null) btn.setOnClickListener(v -> {
                String cur = etAmount.getText() != null ? etAmount.getText().toString() : "";
                if (cur.isEmpty()) etAmount.setText(String.valueOf(val));
                else {
                    try { etAmount.setText(String.valueOf((int)(Double.parseDouble(cur) + val))); }
                    catch (Exception e) { etAmount.setText(String.valueOf(val)); }
                }
                etAmount.setSelection(etAmount.getText().length());
            });
        }
    }

    private void setupClickListeners() {
        tvDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, y, m, d) -> {
                selectedDate = String.format("%04d-%02d-%02d", y, m+1, d);
                tvDate.setText(DatabaseManager.formatDateDisplay(selectedDate));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
        tvTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(this, (view, h, min) -> {
                selectedTime = String.format("%02d:%02d", h, min);
                tvTime.setText(DatabaseManager.formatTimeDisplay(selectedTime));
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
        });
        findViewById(R.id.btnSaveKhataEntry).setOnClickListener(v -> saveKhataEntry());
        findViewById(R.id.tvViewAllKhataEntry).setOnClickListener(v -> {
            // পুরনো KhataEntryListActivity আর নেই — নতুন নেভ-বারের "দেনা-পাওনা" পেজে ফিরে যায়
            DashboardActivity.pendingTargetPage = MainPagerAdapter.POSITION_DENA_PAWNA;
            finish();
        });
    }

    private void saveKhataEntry() {
        String customerName = etKhataCustomer.getText() != null ? etKhataCustomer.getText().toString().trim() : "";
        String cat    = etCategory.getText() != null ? etCategory.getText().toString().trim() : "";
        String amtStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
        String note   = etNote.getText() != null ? etNote.getText().toString().trim() : "";

        if (customerName.isEmpty()) { tilKhataCustomer.setError("নাম লিখুন"); etKhataCustomer.requestFocus(); return; }
        tilKhataCustomer.setError(null);
        if (amtStr.isEmpty()) { tilAmount.setError("পরিমাণ লিখুন"); etAmount.requestFocus(); return; }
        double amount;
        try { amount = Double.parseDouble(amtStr); if (amount <= 0) throw new NumberFormatException(); }
        catch (Exception e) { tilAmount.setError("সঠিক পরিমাণ লিখুন"); etAmount.requestFocus(); return; }
        tilAmount.setError(null);

        KhataEntry e = new KhataEntry();
        e.setCustomerName(customerName);
        for (KhataCustomer c : db.getKhataCustomerList()) {
            if (c.getName().trim().equalsIgnoreCase(customerName)) { e.setCustomerId(c.getId()); break; }
        }
        e.setType(selectedType);
        e.setCategory(cat);
        e.setAmount(amount);
        e.setDate(selectedDate.isEmpty() ? DatabaseManager.nowDate() : selectedDate);
        e.setTime(selectedTime.isEmpty() ? DatabaseManager.nowTime() : selectedTime);
        e.setNote(note);
        e.setPaid(false);
        db.addKhataEntry(e);
        com.jrappspot.cashlipi.widgets.FinanceWidgetProvider.updateAll(this);
        com.jrappspot.cashlipi.utils.BackupManager.getInstance(this).triggerAutoGoogleDriveSync();
        // 🔥 Firebase auto-sync
        FirestoreSyncManager.getInstance(this).uploadAllData(null);

        String typeLabel = "baki".equals(selectedType) ? "দেনা" : "পাওনা";
        SuccessPopup.Category cat2 = "baki".equals(selectedType) ? SuccessPopup.Category.DENA : SuccessPopup.Category.PABONA;
        SuccessPopup.show(this, cat2,
                typeLabel + " যোগ সফল হয়েছে!",
                "আপনার " + typeLabel + " তালিকা সফলভাবে আপডেট হয়েছে।",
                () -> etKhataCustomer.requestFocus(),
                () -> {
                    // পুরনো KhataEntryListActivity আর নেই — নতুন নেভ-বারের "দেনা-পাওনা" পেজে ফিরে যায়
                    DashboardActivity.pendingTargetPage = MainPagerAdapter.POSITION_DENA_PAWNA;
                    finish();
                });
        etKhataCustomer.setText(""); etCategory.setText(""); etAmount.setText(""); etNote.setText("");
        setDefaultDateTime();
        loadRecentKhataEntry();
    }

    private void loadRecentKhataEntry() {
        List<KhataEntry> list = db.getRecentKhataEntry(5);
        if (list.isEmpty()) {
            rvRecentKhataEntry.setVisibility(View.GONE);
            emptyKhataEntryState.setVisibility(View.VISIBLE);
        } else {
            rvRecentKhataEntry.setVisibility(View.VISIBLE);
            emptyKhataEntryState.setVisibility(View.GONE);
            rvRecentKhataEntry.setAdapter(new KhataEntryListAdapter(this, list,
                    (item, pos) -> {
                        new AlertDialog.Builder(this, R.style.AppDialog)
                                .setTitle(item.getCustomerName())
                                .setItems(new String[]{
                                        item.isPaid() ? "↩️ অপরিশোধিত করুন" : " পরিশোধ হয়েছে",
                                        " মুছুন"
                                }, (d, w) -> {
                                    if (w == 0) {
                                        db.toggleKhataEntryPaid(pos);
                                        loadRecentKhataEntry();
                                    } else {
                                        db.deleteKhataEntry(pos);
                                        com.jrappspot.cashlipi.utils.BackupManager.getInstance(this).triggerAutoGoogleDriveSync();
        // 🔥 Firebase auto-sync
        FirestoreSyncManager.getInstance(this).uploadAllData(null);
                                        loadRecentKhataEntry();
                                        Toast.makeText(this, " মুছে গেছে", Toast.LENGTH_SHORT).show();
                                    }
                                }).show();
                    }));
        }
    }
}
