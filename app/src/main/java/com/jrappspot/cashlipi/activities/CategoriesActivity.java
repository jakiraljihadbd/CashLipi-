package com.jrappspot.cashlipi.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.utils.DatabaseManager;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * স্থায়ী ক্যাটাগরি পেজ — সম্পূর্ণ নতুন প্রোফেশনাল ডিজাইন।
 *
 * পুরনো সিস্টেমে ব্যবহারকারীর ব্যক্তিগত ডিফল্ট ক্যাটাগরি (বাবা/মা/ভাই ইত্যাদি) হার্ডকোড করা
 * ছিল — এখন সেগুলো সরিয়ে সার্বজনীন ক্যাটাগরি ব্যবহার করা হচ্ছে (দেখুন DatabaseManager)।
 *
 * নতুন সংযোজন: Pollinations AI (কোনো key ছাড়াই ফ্রি) দিয়ে ইউজার তার পেশা/আয়ের উৎস বা
 * খরচের ধরন লিখে বা ভয়েসে বলে দিলেই AI বিশ্লেষণ করে একটি ইমোজিসহ উপযুক্ত ক্যাটাগরি নাম
 * বানিয়ে দেয় — এটাই এখন যোগ করার প্রধান পদ্ধতি। চাইলে ব্যবহারকারী আগের মতো সরাসরি হাতে
 * লিখেও ক্যাটাগরি যোগ করতে পারবে (ঐচ্ছিক, ভাঁজ করা অবস্থায় থাকে)।
 */
public class CategoriesActivity extends BaseActivity {

    private static final int REQ_VOICE_INPUT = 501;

    private DatabaseManager db;
    private String currentType = "income";

    private final Executor aiExecutor = Executors.newSingleThreadExecutor();

    // Views
    private Button btnTypeIncome, btnTypeExpense;
    private EditText etAiInput, etNewCategory;
    private LinearLayout aiLoadingRow, aiSuggestionRow, manualAddRow, categoryContainer;
    private EditText etAiSuggestion;
    private TextView tvAiSubtitle, tvManualToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);
        db = DatabaseManager.getInstance(this);

        bindViews();
        setupBackButton();
        setupTypeButtons();
        setupManualAdd();
        setupAiSection();
        loadCategories();
    }

    private void bindViews() {
        btnTypeIncome = findViewById(R.id.btnTypeIncome);
        btnTypeExpense = findViewById(R.id.btnTypeExpense);
        etAiInput = findViewById(R.id.etAiInput);
        etNewCategory = findViewById(R.id.etNewCategory);
        aiLoadingRow = findViewById(R.id.aiLoadingRow);
        aiSuggestionRow = findViewById(R.id.aiSuggestionRow);
        manualAddRow = findViewById(R.id.manualAddRow);
        categoryContainer = findViewById(R.id.categoryContainer);
        etAiSuggestion = findViewById(R.id.etAiSuggestion);
        tvAiSubtitle = findViewById(R.id.tvAiSubtitle);
        tvManualToggle = findViewById(R.id.tvManualToggle);
    }

    private void setupBackButton() {
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    private void setupTypeButtons() {
        btnTypeIncome.setOnClickListener(v -> {
            currentType = "income";
            btnTypeIncome.setBackgroundResource(R.drawable.bg_btn_income);
            btnTypeIncome.setTextColor(0xFFFFFFFF);
            btnTypeExpense.setBackgroundResource(R.drawable.bg_type_inactive);
            btnTypeExpense.setTextColor(ContextCompat.getColor(this, R.color.textSecondary));
            refreshTypeDependentUi();
        });

        btnTypeExpense.setOnClickListener(v -> {
            currentType = "expense";
            btnTypeExpense.setBackgroundResource(R.drawable.bg_expense_active);
            btnTypeExpense.setTextColor(0xFFFFFFFF);
            btnTypeIncome.setBackgroundResource(R.drawable.bg_type_inactive);
            btnTypeIncome.setTextColor(ContextCompat.getColor(this, R.color.textSecondary));
            refreshTypeDependentUi();
        });
    }

    private void refreshTypeDependentUi() {
        boolean isIncome = "income".equals(currentType);
        tvAiSubtitle.setText(isIncome
                ? "আপনার পেশা বা আয়ের উৎস লিখুন বা বলুন"
                : "কোন খাতে খরচ হয় তা লিখুন বা বলুন");
        etAiInput.setHint(isIncome
                ? "যেমনঃ আমি একটি বেসরকারি কোম্পানিতে চাকরি করি"
                : "যেমনঃ প্রতি মাসে বাসার ইন্টারনেট বিল দিতে হয়");
        etNewCategory.setHint(isIncome ? "নতুন আয়ের ক্যাটাগরি" : "নতুন ব্যয়ের ক্যাটাগরি");
        aiSuggestionRow.setVisibility(View.GONE);
        loadCategories();
    }

    // ═══════════════════════════════════════════
    //  ম্যানুয়ালি হাতে লিখে যোগ করা (ঐচ্ছিক, ভাঁজ করা)
    // ═══════════════════════════════════════════
    private void setupManualAdd() {
        tvManualToggle.setOnClickListener(v -> {
            boolean showing = manualAddRow.getVisibility() == View.VISIBLE;
            manualAddRow.setVisibility(showing ? View.GONE : View.VISIBLE);
            tvManualToggle.setText(showing ? "+ সরাসরি হাতে লিখে যোগ করুন" : "− হাতে লেখা বন্ধ করুন");
        });

        Button btnAddCategory = findViewById(R.id.btnAddCategory);
        btnAddCategory.setOnClickListener(v -> {
            String nc = etNewCategory.getText() != null ? etNewCategory.getText().toString().trim() : "";
            if (nc.isEmpty()) {
                Toast.makeText(this, "ক্যাটাগরি লিখুন", Toast.LENGTH_SHORT).show();
                return;
            }
            db.addCategory(currentType, nc);
            etNewCategory.setText("");
            loadCategories();
            Toast.makeText(this, "✅ যোগ হয়েছে", Toast.LENGTH_SHORT).show();
        });
    }

    // ═══════════════════════════════════════════
    //  AI দিয়ে ক্যাটাগরি বানানো (Pollinations, key ছাড়াই)
    // ═══════════════════════════════════════════
    private void setupAiSection() {
        ImageView btnAiMic = findViewById(R.id.btnAiMic);
        Button btnAiGenerate = findViewById(R.id.btnAiGenerate);
        ImageView btnAiConfirm = findViewById(R.id.btnAiConfirm);
        ImageView btnAiDiscard = findViewById(R.id.btnAiDiscard);

        btnAiMic.setOnClickListener(v -> launchVoiceInput());

        btnAiGenerate.setOnClickListener(v -> {
            String text = etAiInput.getText() != null ? etAiInput.getText().toString().trim() : "";
            if (text.isEmpty()) {
                Toast.makeText(this, "আগে লিখুন বা বলুন আপনার পেশা/খরচের বিষয়ে", Toast.LENGTH_SHORT).show();
                return;
            }
            generateCategoryWithAi(text);
        });

        btnAiConfirm.setOnClickListener(v -> {
            String finalCat = etAiSuggestion.getText() != null ? etAiSuggestion.getText().toString().trim() : "";
            if (finalCat.isEmpty()) {
                Toast.makeText(this, "ক্যাটাগরি খালি রাখা যাবে না", Toast.LENGTH_SHORT).show();
                return;
            }
            db.addCategory(currentType, finalCat);
            aiSuggestionRow.setVisibility(View.GONE);
            etAiInput.setText("");
            loadCategories();
            Toast.makeText(this, "✅ যোগ হয়েছে", Toast.LENGTH_SHORT).show();
        });

        btnAiDiscard.setOnClickListener(v -> aiSuggestionRow.setVisibility(View.GONE));
    }

    /** সিস্টেম ভয়েস রিকগনাইজার — বাংলা ভাষায় শোনে */
    private void launchVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "আপনার পেশা বা খরচের বিষয়ে বলুন...");
        try {
            startActivityForResult(intent, REQ_VOICE_INPUT);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "ভয়েস ইনপুট সাপোর্ট করছে না এই ডিভাইসে", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_VOICE_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                etAiInput.setText(results.get(0));
                generateCategoryWithAi(results.get(0));
            }
        }
    }

    /** Pollinations AI (কোনো key লাগে না) দিয়ে বর্ণনা থেকে ইমোজিসহ ক্যাটাগরি বের করা।
     *  PollinationsAiHelper শেয়ার্ড ইউটিলিটি ব্যবহার করা হচ্ছে (POST + JSON body) যাতে
     *  ক্যাটাগরি তালিকা যতই বড় হোক, URL length limit-এর কারণে ব্যর্থ না হয়। */
    private void generateCategoryWithAi(String description) {
        aiSuggestionRow.setVisibility(View.GONE);
        aiLoadingRow.setVisibility(View.VISIBLE);

        final String typeForPrompt = currentType;
        aiExecutor.execute(() -> {
            try {
                String existing = String.join(", ", db.getCategories(typeForPrompt));
                String typeLabel = "income".equals(typeForPrompt) ? "আয়" : "ব্যয়";

                String prompt = "তুমি CashLipi অ্যাপের একজন অভিজ্ঞ ক্যাটাগরি বিশ্লেষক। ইউজার তার " + typeLabel
                        + "ের বিষয়ে যা লিখেছে/বলেছে তা গভীরভাবে বিশ্লেষণ করে একটি উপযুক্ত ক্যাটাগরি বানাবে, "
                        + "তারপর শুধুমাত্র একটি বিশুদ্ধ JSON অবজেক্ট দাও, অন্য কোনো লেখা, ব্যাখ্যা বা মার্কডাউন দিও না। "
                        + "ফরম্যাট ঠিক এরকম: {\"category\":\"একটি প্রাসঙ্গিক ইমোজিসহ সংক্ষিপ্ত, পরিষ্কার ক্যাটাগরি নাম "
                        + "(যেমন: 💼 বেতন, 🍔 খাবার, 🚌 যাতায়াত)\"}. "
                        + "বিদ্যমান " + typeLabel + "ের ক্যাটাগরি: " + existing + ". "
                        + "সম্ভব হলে বিদ্যমান কোনো ক্যাটাগরির সাথে মিলিয়ে দাও, নাহলে নতুন উপযুক্ত ক্যাটাগরি বানাও। "
                        + "ইউজার লিখেছে/বলেছে: \"" + description + "\"";

                JSONObject obj = com.jrappspot.cashlipi.utils.PollinationsAiHelper.callJson(prompt);
                String category = obj.optString("category", "").trim();
                if (category.isEmpty()) throw new IllegalStateException("ক্যাটাগরি পাওয়া যায়নি");

                runOnUiThread(() -> {
                    aiLoadingRow.setVisibility(View.GONE);
                    etAiSuggestion.setText(category);
                    aiSuggestionRow.setVisibility(View.VISIBLE);
                });
            } catch (Exception e) {
                android.util.Log.e("CashLipiAI", "generateCategoryWithAi ব্যর্থ হয়েছে", e);
                runOnUiThread(() -> {
                    aiLoadingRow.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "AI বুঝতে পারেনি, আবার চেষ্টা করুন বা হাতে লিখুন", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // ═══════════════════════════════════════════
    //  ক্যাটাগরি তালিকা — প্রোফেশনাল কার্ড স্টাইল
    // ═══════════════════════════════════════════
    private void loadCategories() {
        if (categoryContainer == null) return;
        categoryContainer.removeAllViews();

        List<String> cats = db.getCategories(currentType);
        for (String cat : cats) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(R.drawable.bg_card_white);
            row.setPadding(16, 14, 12, 14);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 8);
            row.setLayoutParams(lp);

            TextView tv = new TextView(this);
            tv.setText(cat);
            tv.setTextSize(14.5f);
            tv.setTextColor(ContextCompat.getColor(this, R.color.textPrimary));
            tv.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            ImageView btnDel = new ImageView(this);
            btnDel.setImageResource(R.drawable.ic_delete);
            btnDel.setImageTintList(ContextCompat.getColorStateList(this, R.color.errorColor));
            int pad = (int) (10 * getResources().getDisplayMetrics().density);
            btnDel.setPadding(pad, pad, pad, pad);
            btnDel.setBackgroundResource(android.R.drawable.list_selector_background);
            btnDel.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            final String catToRemove = cat;
            btnDel.setOnClickListener(v -> {
                db.removeCategory(currentType, catToRemove);
                loadCategories();
                Toast.makeText(this, "🗑 মুছে গেছে", Toast.LENGTH_SHORT).show();
            });

            row.addView(tv);
            row.addView(btnDel);
            categoryContainer.addView(row);
        }
    }
}
