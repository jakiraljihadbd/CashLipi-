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

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.models.Transaction;
import com.jrappspot.cashlipi.utils.AmountInputHelper;
import com.jrappspot.cashlipi.utils.BackupManager;
import com.jrappspot.cashlipi.utils.DatabaseManager;
import com.jrappspot.cashlipi.utils.FirestoreSyncManager;
import com.jrappspot.cashlipi.utils.FontUtils;
import com.jrappspot.cashlipi.utils.SuccessPopup;

import java.util.Calendar;
import java.util.List;

/**
 * একই পেজে "আয় যুক্ত করুন" এবং "ব্যয় যুক্ত করুন" — উপরের ট্যাব দিয়ে সুইচ করা যায়।
 * এটি পুরনো AddIncomeActivity ও AddExpenseActivity এর জায়গা প্রতিস্থাপন করেছে।
 *
 * ট্যাব সবুজ (আয়) / লাল (ব্যয়) — সেভ বাটনও সবুজ / লাল।
 * ক্যাটাগরি এখন সরাসরি টাইপ করা যায়, নিচের চিপ থেকেও বাছা যায়।
 */
public class AddTransactionActivity extends BaseActivity {

    public static final String EXTRA_MODE = "mode"; // "income" | "expense"

    private DatabaseManager db;
    private boolean isIncome = true;

    private TextView tabIncome, tabExpense;
    private TextInputLayout tilAmount;
    private TextInputEditText etAmount, etNote;
    private TextView tvDate, tvTime;
    private TextInputLayout tilCategory;
    private TextInputEditText etCategory;
    private LinearLayout chipGroupCategories;
    private Button btnSaveTxn;

    private String selectedDate = "";
    private String selectedTime = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);
        FontUtils.applyToView(this, findViewById(android.R.id.content));
        db = DatabaseManager.getInstance(this);

        String mode = getIntent().getStringExtra(EXTRA_MODE);
        isIncome = !"expense".equals(mode);

        initViews();
        setDefaultDateTime();
        setupClickListeners();
        applyModeUI();
    }

    private void initViews() {
        tabIncome = findViewById(R.id.tabIncome);
        tabExpense = findViewById(R.id.tabExpense);

        tilAmount = findViewById(R.id.tilAmount);
        etAmount = findViewById(R.id.etAmount);
        etNote = findViewById(R.id.etNote);

        tvDate = findViewById(R.id.tvDate);
        tvTime = findViewById(R.id.tvTime);

        tilCategory = findViewById(R.id.tilCategory);
        etCategory = findViewById(R.id.etCategory);
        chipGroupCategories = findViewById(R.id.chipGroupCategories);

        btnSaveTxn = findViewById(R.id.btnSaveTxn);

        // Custom calculator keyboard on amount field
        AmountInputHelper.attach(this, etAmount);
    }

    private void setDefaultDateTime() {
        selectedDate = DatabaseManager.nowDate();
        selectedTime = DatabaseManager.nowTime();
        tvDate.setText(DatabaseManager.formatDateDisplay(selectedDate));
        tvTime.setText(DatabaseManager.formatTimeDisplay(selectedTime));
    }

    private void setupClickListeners() {
        tabIncome.setOnClickListener(v -> {
            if (!isIncome) {
                isIncome = true;
                applyModeUI();
            }
        });
        tabExpense.setOnClickListener(v -> {
            if (isIncome) {
                isIncome = false;
                applyModeUI();
            }
        });

        tvDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, y, m, d) -> {
                selectedDate = String.format("%04d-%02d-%02d", y, m + 1, d);
                tvDate.setText(DatabaseManager.formatDateDisplay(selectedDate));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        tvTime.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new TimePickerDialog(this, (view, h, min) -> {
                selectedTime = String.format("%02d:%02d", h, min);
                tvTime.setText(DatabaseManager.formatTimeDisplay(selectedTime));
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show();
        });

        btnSaveTxn.setOnClickListener(v -> saveTransaction());
    }

    /** ট্যাব সুইচ হলে পুরো UI (রঙ, লেবেল, ক্যাটাগরি) আপডেট করে */
    private void applyModeUI() {
        int accentColor = androidx.core.content.ContextCompat.getColor(
                this, isIncome ? R.color.incomeColor : R.color.expenseColor);

        // Tabs — সিলেক্ট করা ট্যাব সবুজ (আয়) বা লাল (ব্যয়), অন্যটি সাদামাটা
        tabIncome.setBackground(isIncome
                ? getResources().getDrawable(R.drawable.bg_txn_tab_selected_income) : null);
        tabExpense.setBackground(!isIncome
                ? getResources().getDrawable(R.drawable.bg_txn_tab_selected_expense) : null);
        tabIncome.setTextColor(androidx.core.content.ContextCompat.getColor(
                this, isIncome ? R.color.white : R.color.txnTabInactiveText));
        tabExpense.setTextColor(androidx.core.content.ContextCompat.getColor(
                this, !isIncome ? R.color.white : R.color.txnTabInactiveText));

        // Amount field
        tilAmount.setHint(isIncome ? "আয়ের পরিমাণ" : "ব্যয়ের পরিমাণ");
        etAmount.setTextColor(accentColor);

        // Save circle button — আয়ে সবুজ, ব্যয়ে লাল
        btnSaveTxn.setBackground(getResources().getDrawable(
                isIncome ? R.drawable.circle_gradient_green : R.drawable.circle_gradient_red));
        btnSaveTxn.setText(isIncome ? "আয়\nসংরক্ষণ" : "ব্যয়\nসংরক্ষণ");

        // Category field icon + hint
        tilCategory.setStartIconDrawable(
                isIncome ? R.drawable.ic_plus_circle_txn : R.drawable.ic_minus_circle_txn);
        tilCategory.setHint(isIncome ? "আয়ের উৎস লিখুন বা নিচ থেকে বাছুন" : "ব্যয়ের ধরন লিখুন বা নিচ থেকে বাছুন");
        etCategory.setText("");

        loadCategoryChips();
    }

    private void loadCategoryChips() {
        chipGroupCategories.removeAllViews();
        List<String> cats = db.getCategories(isIncome ? "income" : "expense");

        int accentColor = androidx.core.content.ContextCompat.getColor(
                this, isIncome ? R.color.incomeColor : R.color.expenseColor);

        for (String cat : cats) {
            TextView chip = new TextView(this);
            chip.setText(cat);
            chip.setTextSize(13f);
            chip.setTypeface(chip.getTypeface(), android.graphics.Typeface.BOLD);
            chip.setTextColor(accentColor);
            chip.setBackground(getResources().getDrawable(
                    isIncome ? R.drawable.bg_paid_badge : R.drawable.bg_unpaid_badge));
            chip.setPadding(26, 14, 26, 14);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(10);
            chip.setLayoutParams(lp);
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setOnClickListener(v -> {
                etCategory.setText(cat);
                etCategory.setSelection(etCategory.getText().length());
            });
            chipGroupCategories.addView(chip);
        }
    }

    private void saveTransaction() {
        String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
        String note = etNote.getText() != null ? etNote.getText().toString().trim() : "";
        String category = etCategory.getText() != null ? etCategory.getText().toString().trim() : "";

        if (category.isEmpty()) {
            tilCategory.setError(isIncome ? "আয়ের উৎস লিখুন" : "ব্যয়ের ধরন লিখুন");
            etCategory.requestFocus();
            return;
        }
        tilCategory.setError(null);

        if (amountStr.isEmpty()) {
            tilAmount.setError("পরিমাণ লিখুন");
            etAmount.requestFocus();
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            tilAmount.setError("সঠিক পরিমাণ লিখুন");
            etAmount.requestFocus();
            return;
        }
        tilAmount.setError(null);

        // নতুন ক্যাটাগরি হলে তালিকায় যোগ করে রাখা, যাতে পরের বার চিপে দেখা যায়
        List<String> existingCats = db.getCategories(isIncome ? "income" : "expense");
        if (!existingCats.contains(category)) {
            db.addCategory(isIncome ? "income" : "expense", category);
        }

        Transaction t = new Transaction();
        t.setSource(category);
        t.setCategory(category);
        t.setAmount(amount);
        t.setDate(selectedDate.isEmpty() ? DatabaseManager.nowDate() : selectedDate);
        t.setTime(selectedTime.isEmpty() ? DatabaseManager.nowTime() : selectedTime);
        t.setNote(note);
        t.setType(isIncome ? "income" : "expense");

        if (isIncome) db.addIncome(t); else db.addExpense(t);

        BackupManager.getInstance(this).triggerAutoGoogleDriveSync();
        FirestoreSyncManager.getInstance(this).uploadAllData(null);

        SuccessPopup.show(this,
                isIncome ? SuccessPopup.Category.INCOME : SuccessPopup.Category.EXPENSE,
                isIncome ? "আয় যোগ সফল হয়েছে!" : "ব্যয় যোগ সফল হয়েছে!",
                isIncome ? "আপনার আয় তালিকা সফলভাবে আপডেট হয়েছে।" : "আপনার ব্যয় তালিকা সফলভাবে আপডেট হয়েছে।",
                () -> etAmount.requestFocus(),
                () -> startActivity(new Intent(this,
                        isIncome ? IncomeListActivity.class : ExpenseListActivity.class)));

        // Reset form
        etAmount.setText("");
        etNote.setText("");
        etCategory.setText("");
        setDefaultDateTime();
        loadCategoryChips();
    }
}
