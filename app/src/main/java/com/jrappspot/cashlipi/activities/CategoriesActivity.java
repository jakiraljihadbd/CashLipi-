package com.jrappspot.cashlipi.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.utils.DatabaseManager;

import java.util.List;

/**
 * স্থায়ী ক্যাটাগরি পেজ — প্রোফেশনাল, সহজ, মোবাইল-ফ্রেন্ডলি নতুন ডিজাইন।
 *
 * এই ভার্সনে:
 *  - হেডার এখন অ্যাপের স্ট্যান্ডার্ড পিংক রঙে, একদম সোজা (আগের বাঁকানো ইন্ডিগো হেডার বাদ)।
 *  - আয়/ব্যয় টগল এখন আয়-ব্যয় পেজের মতোই সবুজ/লাল স্টাইলে।
 *  - AI দিয়ে ক্যাটাগরি বানানোর সিস্টেম সম্পূর্ণ বাদ দেওয়া হয়েছে (ভয়েস ইনপুট, Pollinations
 *    কল ইত্যাদি সহ) — এখন শুধু সরাসরি হাতে লিখে সহজে ক্যাটাগরি যোগ করা যায়।
 *  - ক্যাটাগরি তালিকায় প্রতিটি আইটেমে এখন এডিট (নাম পরিবর্তন) ও ডিলিট (নিশ্চিতকরণসহ) দুটোই আছে।
 */
public class CategoriesActivity extends BaseActivity {

    private DatabaseManager db;
    private String currentType = "income";

    // Views
    private TextView btnTypeIncome, btnTypeExpense;
    private TextView tvCategoryCount, tvAddCategorySubtitle;
    private ImageView ivAddCategoryIcon;
    private com.google.android.material.textfield.TextInputLayout tilNewCategory;
    private EditText etNewCategory;
    private Button btnAddCategory;
    private LinearLayout categoryContainer, emptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);
        db = DatabaseManager.getInstance(this);

        bindViews();
        setupBackButton();
        setupTypeButtons();
        setupAddCategory();
        applyTypeStyles();
        loadCategories();
    }

    private void bindViews() {
        btnTypeIncome = findViewById(R.id.btnTypeIncome);
        btnTypeExpense = findViewById(R.id.btnTypeExpense);
        tvCategoryCount = findViewById(R.id.tvCategoryCount);
        tvAddCategorySubtitle = findViewById(R.id.tvAddCategorySubtitle);
        ivAddCategoryIcon = findViewById(R.id.ivAddCategoryIcon);
        tilNewCategory = findViewById(R.id.tilNewCategory);
        etNewCategory = findViewById(R.id.etNewCategory);
        btnAddCategory = findViewById(R.id.btnAddCategory);
        categoryContainer = findViewById(R.id.categoryContainer);
        emptyState = findViewById(R.id.emptyState);
    }

    private void setupBackButton() {
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    // ═══════════════════════════════════════════
    //  আয়/ব্যয় টগল — আয়-ব্যয় পেজের সবুজ/লাল স্টাইল অনুসরণ করে
    // ═══════════════════════════════════════════
    private void setupTypeButtons() {
        btnTypeIncome.setOnClickListener(v -> {
            if ("income".equals(currentType)) return;
            currentType = "income";
            applyTypeStyles();
            loadCategories();
        });

        btnTypeExpense.setOnClickListener(v -> {
            if ("expense".equals(currentType)) return;
            currentType = "expense";
            applyTypeStyles();
            loadCategories();
        });
    }

    private void applyTypeStyles() {
        boolean isIncome = "income".equals(currentType);

        btnTypeIncome.setBackground(ContextCompat.getDrawable(this,
                isIncome ? R.drawable.ie_toggle_income_active : R.drawable.ie_toggle_income_inactive));
        btnTypeIncome.setTextColor(ContextCompat.getColor(this,
                isIncome ? R.color.ieWhite : R.color.ieIncomeDark));

        btnTypeExpense.setBackground(ContextCompat.getDrawable(this,
                !isIncome ? R.drawable.ie_toggle_expense_active : R.drawable.ie_toggle_expense_inactive));
        btnTypeExpense.setTextColor(ContextCompat.getColor(this,
                !isIncome ? R.color.ieWhite : R.color.ieExpenseDark));

        tvAddCategorySubtitle.setText(isIncome
                ? "আয়ের একটি নতুন খাতের নাম লিখুন"
                : "ব্যয়ের একটি নতুন খাতের নাম লিখুন");

        // আগে এখানে দুইভাবে হিন্ট দেখানো হতো (TextInputLayout-এর XML hint + EditText-এর runtime
        // hint) — একই উদাহরণ দুইবার দেখা যেত, তাই এখন একটাই সহজ নির্দেশনা।
        etNewCategory.setHint("আয় বা ব্যয় ক্যাটাগরি লিখুন");

        ivAddCategoryIcon.setBackgroundResource(isIncome
                ? R.drawable.bg_icon_circle_income : R.drawable.bg_icon_circle_expense);
        btnAddCategory.setBackgroundResource(isIncome
                ? R.drawable.bg_add_cat_btn_income : R.drawable.bg_add_cat_btn_expense);

        // ইনপুট বক্স — সক্রিয় ট্যাব অনুযায়ী রং (সবুজ/লাল) নেয়, যাতে বাটন-আইকনের সাথে মিলে যায়
        int accentColor = ContextCompat.getColor(this,
                isIncome ? R.color.ieIncomeBright : R.color.ieExpenseBright);
        android.content.res.ColorStateList accentTint = android.content.res.ColorStateList.valueOf(accentColor);
        if (tilNewCategory != null) {
            tilNewCategory.setBoxStrokeColor(accentColor);
            tilNewCategory.setHintTextColor(accentTint);
            tilNewCategory.setStartIconTintList(accentTint);
        }
    }

    // ═══════════════════════════════════════════
    //  নতুন ক্যাটাগরি যোগ করা — সহজ, সরাসরি
    // ═══════════════════════════════════════════
    private void setupAddCategory() {
        btnAddCategory.setOnClickListener(v -> addNewCategory());

        etNewCategory.setOnEditorActionListener((v, actionId, event) -> {
            addNewCategory();
            return true;
        });
    }

    private void addNewCategory() {
        String nc = etNewCategory.getText() != null ? etNewCategory.getText().toString().trim() : "";
        if (nc.isEmpty()) {
            Toast.makeText(this, "ক্যাটাগরির নাম লিখুন", Toast.LENGTH_SHORT).show();
            return;
        }
        if (db.getCategories(currentType).contains(nc)) {
            Toast.makeText(this, "এই ক্যাটাগরি আগে থেকেই আছে", Toast.LENGTH_SHORT).show();
            return;
        }
        db.addCategory(currentType, nc);
        etNewCategory.setText("");
        loadCategories();
        Toast.makeText(this, "✅ ক্যাটাগরি যোগ হয়েছে", Toast.LENGTH_SHORT).show();
    }

    // ═══════════════════════════════════════════
    //  ক্যাটাগরি তালিকা — প্রোফেশনাল কার্ড, এডিট ও ডিলিট সহ
    // ═══════════════════════════════════════════
    private void loadCategories() {
        if (categoryContainer == null) return;
        categoryContainer.removeAllViews();

        List<String> cats = db.getCategories(currentType);
        boolean isIncome = "income".equals(currentType);

        tvCategoryCount.setText("মোট " + cats.size() + "টি ক্যাটাগরি");
        emptyState.setVisibility(cats.isEmpty() ? View.VISIBLE : View.GONE);
        categoryContainer.setVisibility(cats.isEmpty() ? View.GONE : View.VISIBLE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (String cat : cats) {
            View row = inflater.inflate(R.layout.item_category_row, categoryContainer, false);

            View stripe = row.findViewById(R.id.stripe);
            TextView tvName = row.findViewById(R.id.tvCategoryName);
            ImageView btnEdit = row.findViewById(R.id.btnEdit);
            ImageView btnDelete = row.findViewById(R.id.btnDelete);

            stripe.setBackgroundResource(isIncome ? R.drawable.dot_stripe_income : R.drawable.dot_stripe_expense);
            tvName.setText(cat);

            btnEdit.setOnClickListener(v -> showEditDialog(cat));
            btnDelete.setOnClickListener(v -> showDeleteConfirm(cat));

            categoryContainer.addView(row);
        }
    }

    /** ক্যাটাগরির নাম সম্পাদনার ডায়ালগ */
    private void showEditDialog(String oldName) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_simple_input, null, false);
        EditText input = dialogView.findViewById(R.id.etDialogInput);
        input.setText(oldName);
        input.setSelection(input.getText().length());

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AppDialog)
                .setTitle("ক্যাটাগরি সম্পাদনা করুন")
                .setView(dialogView)
                .setPositiveButton("সংরক্ষণ করুন", (d, w) -> {
                    String newName = input.getText() != null ? input.getText().toString().trim() : "";
                    if (newName.isEmpty()) {
                        Toast.makeText(this, "ক্যাটাগরির নাম খালি রাখা যাবে না", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!newName.equals(oldName) && db.getCategories(currentType).contains(newName)) {
                        Toast.makeText(this, "এই নামে আরেকটি ক্যাটাগরি আগে থেকেই আছে", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.renameCategory(currentType, oldName, newName);
                    loadCategories();
                    Toast.makeText(this, "✅ পরিবর্তন সংরক্ষণ হয়েছে", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("বাতিল", null)
                .create();
        dialog.show();
    }

    /** ক্যাটাগরি ডিলিটের আগে নিশ্চিতকরণ */
    private void showDeleteConfirm(String cat) {
        new AlertDialog.Builder(this, R.style.AppDialog)
                .setTitle("ক্যাটাগরি মুছবেন?")
                .setMessage("\"" + cat + "\" ক্যাটাগরিটি স্থায়ীভাবে মুছে ফেলা হবে। এই কাজটি ফিরিয়ে আনা যাবে না।")
                .setPositiveButton("মুছে ফেলুন", (d, w) -> {
                    db.removeCategory(currentType, cat);
                    loadCategories();
                    Toast.makeText(this, "🗑 মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }
}
