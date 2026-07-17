package com.jrappspot.cashlipi.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
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
import com.jrappspot.cashlipi.adapters.LedgerListAdapter;
import com.jrappspot.cashlipi.models.LedgerEntry;
import com.jrappspot.cashlipi.utils.AmountInputHelper;
import com.jrappspot.cashlipi.utils.DatabaseManager;
import com.jrappspot.cashlipi.utils.FirestoreSyncManager;
import com.jrappspot.cashlipi.utils.SuccessPopup;

import java.util.Calendar;
import java.util.List;

public class AddLedgerActivity extends BaseActivity {

    private DatabaseManager db;
    private TextInputEditText etPerson, etCategory, etAmount, etNote;
    private TextInputLayout tilPerson, tilAmount;
    private TextView tvDate, tvTime;
    private Button btnTypeDena, btnTypePabona;
    private RecyclerView rvRecentLedger;
    private LinearLayout emptyLedgerState;

    private String selectedType = "dena";
    private String selectedDate = "", selectedTime = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_ledger);
        FontUtils.applyToView(this, findViewById(android.R.id.content));
        db = DatabaseManager.getInstance(this);
        initViews();
        setDefaultDateTime();
        setupTypeToggle();
        setupQuickAmounts();
        setupClickListeners();
        loadRecentLedger();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecentLedger();
    }

    private void initViews() {
        etPerson    = findViewById(R.id.etPerson);
        etCategory  = findViewById(R.id.etCategory);
        etAmount    = findViewById(R.id.etAmount);
        etNote      = findViewById(R.id.etNote);
        tilPerson   = findViewById(R.id.tilPerson);
        tilAmount   = findViewById(R.id.tilAmount);
        tvDate      = findViewById(R.id.tvDate);
        tvTime      = findViewById(R.id.tvTime);
        btnTypeDena   = findViewById(R.id.btnTypeDena);
        btnTypePabona = findViewById(R.id.btnTypePabona);
        rvRecentLedger   = findViewById(R.id.rvRecentLedger);
        emptyLedgerState = findViewById(R.id.emptyLedgerState);
        rvRecentLedger.setLayoutManager(new LinearLayoutManager(this));
        rvRecentLedger.setNestedScrollingEnabled(false);

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
            selectedType = "dena";
            updateTypeUI();
        });
        btnTypePabona.setOnClickListener(v -> {
            selectedType = "pabona";
            updateTypeUI();
        });
    }

    private void updateTypeUI() {
        if ("dena".equals(selectedType)) {
            btnTypeDena.setBackground(getResources().getDrawable(R.drawable.bg_type_active_dena));
            btnTypeDena.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white));
            btnTypePabona.setBackground(getResources().getDrawable(R.drawable.bg_type_inactive));
            btnTypePabona.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.textSecondary));
        } else {
            btnTypePabona.setBackground(getResources().getDrawable(R.drawable.bg_type_active_pabona));
            btnTypePabona.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white));
            btnTypeDena.setBackground(getResources().getDrawable(R.drawable.bg_type_inactive));
            btnTypeDena.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.textSecondary));
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
        findViewById(R.id.btnSaveLedger).setOnClickListener(v -> saveLedger());
        findViewById(R.id.tvViewAllLedger).setOnClickListener(v ->
                startActivity(new Intent(this, LedgerListActivity.class)));
    }

    private void saveLedger() {
        String person = etPerson.getText() != null ? etPerson.getText().toString().trim() : "";
        String cat    = etCategory.getText() != null ? etCategory.getText().toString().trim() : "";
        String amtStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
        String note   = etNote.getText() != null ? etNote.getText().toString().trim() : "";

        if (person.isEmpty()) { tilPerson.setError("নাম লিখুন"); etPerson.requestFocus(); return; }
        tilPerson.setError(null);
        if (amtStr.isEmpty()) { tilAmount.setError("পরিমাণ লিখুন"); etAmount.requestFocus(); return; }
        double amount;
        try { amount = Double.parseDouble(amtStr); if (amount <= 0) throw new NumberFormatException(); }
        catch (Exception e) { tilAmount.setError("সঠিক পরিমাণ লিখুন"); etAmount.requestFocus(); return; }
        tilAmount.setError(null);

        LedgerEntry e = new LedgerEntry();
        e.setPerson(person);
        e.setType(selectedType);
        e.setCategory(cat);
        e.setAmount(amount);
        e.setDate(selectedDate.isEmpty() ? DatabaseManager.nowDate() : selectedDate);
        e.setTime(selectedTime.isEmpty() ? DatabaseManager.nowTime() : selectedTime);
        e.setNote(note);
        e.setPaid(false);
        db.addLedger(e);
        com.jrappspot.cashlipi.widgets.FinanceWidgetProvider.updateAll(this);
        com.jrappspot.cashlipi.utils.BackupManager.getInstance(this).triggerAutoGoogleDriveSync();
        // 🔥 Firebase auto-sync
        FirestoreSyncManager.getInstance(this).uploadAllData(null);

        String typeLabel = "dena".equals(selectedType) ? "দেনা" : "পাওনা";
        SuccessPopup.Category cat2 = "dena".equals(selectedType) ? SuccessPopup.Category.DENA : SuccessPopup.Category.PABONA;
        SuccessPopup.show(this, cat2,
                typeLabel + " যোগ সফল হয়েছে!",
                "আপনার " + typeLabel + " তালিকা সফলভাবে আপডেট হয়েছে।",
                () -> etPerson.requestFocus(),
                () -> startActivity(new Intent(this, LedgerListActivity.class)));
        etPerson.setText(""); etCategory.setText(""); etAmount.setText(""); etNote.setText("");
        setDefaultDateTime();
        loadRecentLedger();
    }

    private void loadRecentLedger() {
        List<LedgerEntry> list = db.getRecentLedger(5);
        if (list.isEmpty()) {
            rvRecentLedger.setVisibility(View.GONE);
            emptyLedgerState.setVisibility(View.VISIBLE);
        } else {
            rvRecentLedger.setVisibility(View.VISIBLE);
            emptyLedgerState.setVisibility(View.GONE);
            rvRecentLedger.setAdapter(new LedgerListAdapter(this, list,
                    (item, pos) -> {
                        new AlertDialog.Builder(this, R.style.AppDialog)
                                .setTitle(item.getPerson())
                                .setItems(new String[]{
                                        item.isPaid() ? "↩️ অপরিশোধিত করুন" : " পরিশোধ হয়েছে",
                                        " মুছুন"
                                }, (d, w) -> {
                                    if (w == 0) {
                                        db.toggleLedgerPaid(pos);
                                        loadRecentLedger();
                                    } else {
                                        db.deleteLedger(pos);
                                        com.jrappspot.cashlipi.utils.BackupManager.getInstance(this).triggerAutoGoogleDriveSync();
        // 🔥 Firebase auto-sync
        FirestoreSyncManager.getInstance(this).uploadAllData(null);
                                        loadRecentLedger();
                                        Toast.makeText(this, " মুছে গেছে", Toast.LENGTH_SHORT).show();
                                    }
                                }).show();
                    }));
        }
    }
}
