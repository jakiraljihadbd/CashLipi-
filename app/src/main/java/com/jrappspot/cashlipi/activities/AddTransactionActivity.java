package com.jrappspot.cashlipi.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.models.Transaction;
import com.jrappspot.cashlipi.utils.AmountInputHelper;
import com.jrappspot.cashlipi.utils.BackupManager;
import com.jrappspot.cashlipi.utils.BlurUtils;
import com.jrappspot.cashlipi.utils.DatabaseManager;
import com.jrappspot.cashlipi.utils.FirestoreSyncManager;
import com.jrappspot.cashlipi.utils.FontUtils;
import com.jrappspot.cashlipi.utils.SoundEffectPlayer;
import com.jrappspot.cashlipi.utils.SuccessPopup;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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
    private View ringSaveTxn;
    private LinearLayout bodyContainerTxn;
    private LinearLayout rowPaymentMethods1, rowPaymentMethods2;

    private String selectedDate = "";
    private String selectedTime = "";
    private String selectedMethod = "cash";

    private LinearLayout btnAiVoiceEntry;
    private final Executor aiExecutor = Executors.newSingleThreadExecutor();
    private static final int REQ_VOICE_INPUT = 9021;

    private View aiThinkingOverlay;
    private ImageView ivAiBlurBg;
    private TextView tvAiThinking;
    private TextView tvAiHeardText;
    private final android.os.Handler thinkingHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private int thinkingDotCount = 0;
    private final Runnable thinkingDotsRunnable = new Runnable() {
        @Override public void run() {
            if (tvAiThinking == null) return;
            thinkingDotCount = (thinkingDotCount + 1) % 4;
            StringBuilder dots = new StringBuilder();
            for (int i = 0; i < thinkingDotCount; i++) dots.append('.');
            tvAiThinking.setText("AI ভাবছে" + dots);
            thinkingHandler.postDelayed(this, 380);
        }
    };

    private SoundEffectPlayer soundEffectPlayer;

    /** ডিফল্ট লেনদেন মাধ্যম লেবেল — কী → প্রদর্শিত নাম */
    private static final Map<String, String> PAYMENT_LABELS = new LinkedHashMap<>();
    static {
        PAYMENT_LABELS.put("cash", "Cash");
        PAYMENT_LABELS.put("bkash", "bKash");
        PAYMENT_LABELS.put("nagad", "Nagad");
        PAYMENT_LABELS.put("rocket", "Rocket");
        PAYMENT_LABELS.put("bank", "Bank");
        PAYMENT_LABELS.put("other", "Others");
    }

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
        soundEffectPlayer = SoundEffectPlayer.getInstance(this);
    }

    private void playTapSound() {
        if (soundEffectPlayer != null) soundEffectPlayer.playTap();
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
        ringSaveTxn = findViewById(R.id.ringSaveTxn);
        bodyContainerTxn = findViewById(R.id.bodyContainerTxn);
        rowPaymentMethods1 = findViewById(R.id.rowPaymentMethods1);
        rowPaymentMethods2 = findViewById(R.id.rowPaymentMethods2);
        btnAiVoiceEntry = findViewById(R.id.btnAiVoiceEntry);
        aiThinkingOverlay = findViewById(R.id.aiThinkingOverlay);
        ivAiBlurBg = findViewById(R.id.ivAiBlurBg);
        tvAiThinking = findViewById(R.id.tvAiThinking);
        tvAiHeardText = findViewById(R.id.tvAiHeardText);

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
                playTapSound();
            }
        });
        tabExpense.setOnClickListener(v -> {
            if (isIncome) {
                isIncome = false;
                applyModeUI();
                playTapSound();
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

        btnAiVoiceEntry.setOnClickListener(v -> launchVoiceInput());
    }

    /** সিস্টেম ভয়েস রিকগনাইজার চালু করে — বাংলা ভাষায় শোনে */
    private void launchVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "আয় বা ব্যয়ের হিসাব বলুন...");
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
                String spokenText = results.get(0);
                parseWithAi(spokenText);
            }
        }
    }

    /** Pollinations AI (কোনো key লাগে না) দিয়ে ভয়েস টেক্সট থেকে লেনদেনের তথ্য বের করা */
    private void parseWithAi(String spokenText) {
        showAiThinking(spokenText);
        aiExecutor.execute(() -> {
            try {
                String incomeCats = String.join(", ", db.getCategories("income"));
                String expenseCats = String.join(", ", db.getCategories("expense"));

                String prompt = "তুমি CashLipi অ্যাপের একজন অভিজ্ঞ, মনোযোগী লেনদেন পার্সার। ইউজার যা বলেছে তা গভীরভাবে বিশ্লেষণ করে "
                        + "লেনদেনের প্রতিটি তথ্য বুদ্ধিমত্তার সাথে বের করবে, তারপর শুধুমাত্র একটি বিশুদ্ধ JSON অবজেক্ট দাও, "
                        + "অন্য কোনো লেখা, ব্যাখ্যা বা মার্কডাউন দিও না। "
                        + "ফরম্যাট ঠিক এরকম: {\"type\":\"income\" অথবা \"expense\",\"amount\":সংখ্যা,"
                        + "\"category\":\"একটি প্রাসঙ্গিক ইমোজিসহ সংক্ষিপ্ত, পরিষ্কার ক্যাটাগরি নাম (যেমন: 🍔 খাবার, 🚌 যাতায়াত, 💼 বেতন)\","
                        + "\"method\":\"cash\" বা \"bkash\" বা \"nagad\" বা \"rocket\" বা \"bank\" বা \"other\","
                        + "\"note\":\"একটি ছোট, সুন্দরভাবে গোছানো নোট — প্রাসঙ্গিক হলে একটি ইমোজি দিয়ে শুরু, প্রয়োজনীয় বিস্তারিত সংক্ষেপে, নাহলে খালি স্ট্রিং\"}. "
                        + "বিদ্যমান আয়ের ক্যাটাগরি: " + incomeCats + ". "
                        + "বিদ্যমান ব্যয়ের ক্যাটাগরি: " + expenseCats + ". "
                        + "সম্ভব হলে বিদ্যমান ক্যাটাগরির সাথে মিলিয়ে দাও, নাহলে নতুন উপযুক্ত ক্যাটাগরি বানাও। "
                        + "মাধ্যম উল্লেখ না থাকলে \"cash\" ধরে নাও। "
                        + "ইউজার বলেছে: \"" + spokenText + "\"";

                String encoded = URLEncoder.encode(prompt, "UTF-8");
                URL url = new URL("https://text.pollinations.ai/" + encoded + "?model=openai&json=true");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                String raw = sb.toString().trim();
                raw = raw.replaceAll("(?s)```json|```", "").trim();
                int start = raw.indexOf('{');
                int end = raw.lastIndexOf('}');
                if (start < 0 || end <= start) throw new IllegalStateException("AI থেকে সঠিক উত্তর আসেনি");
                JSONObject obj = new JSONObject(raw.substring(start, end + 1));

                runOnUiThread(() -> {
                    hideAiThinking();
                    showAiPreview(spokenText, obj);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    hideAiThinking();
                    Toast.makeText(this,
                            "AI বুঝতে পারেনি, আবার চেষ্টা করুন বা নিজে হাতে লিখুন", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /** পুরো পেজ ব্লার করে মাঝখানে বড় করে "AI ভাবছে..." দেখানো হয় — উপরে ইউজার যা বলেছে তা সুন্দর "কোট" আকারে দেখায় */
    private void showAiThinking(String spokenText) {
        if (aiThinkingOverlay == null) return;
        if (tvAiHeardText != null) {
            tvAiHeardText.setText("“" + spokenText + "”");
        }
        View root = findViewById(android.R.id.content);
        try {
            Bitmap blurred = BlurUtils.blurredSnapshot(root, 0.22f, 6);
            ivAiBlurBg.setImageDrawable(new BitmapDrawable(getResources(), blurred));
        } catch (Exception ignored) {
            // ব্লার তৈরি না হলেও ওভারলে ডিমসহ দেখানো হবে
        }
        aiThinkingOverlay.setAlpha(0f);
        aiThinkingOverlay.setVisibility(View.VISIBLE);
        aiThinkingOverlay.animate().alpha(1f).setDuration(220).start();

        thinkingDotCount = 0;
        thinkingHandler.removeCallbacks(thinkingDotsRunnable);
        thinkingHandler.post(thinkingDotsRunnable);
    }

    private void hideAiThinking() {
        if (aiThinkingOverlay == null) return;
        thinkingHandler.removeCallbacks(thinkingDotsRunnable);
        aiThinkingOverlay.animate().alpha(0f).setDuration(180)
                .withEndAction(() -> aiThinkingOverlay.setVisibility(View.GONE)).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        thinkingHandler.removeCallbacksAndMessages(null);
    }

    /** AI-এর পার্স করা তথ্য প্রিভিউ বটম-শিটে দেখানো, এডিট করা যাবে, তারপর সংরক্ষণ */
    private void showAiPreview(String spokenText, JSONObject parsed) {
        boolean aiIsIncome = !"expense".equals(parsed.optString("type", "income"));
        double aiAmount = parsed.optDouble("amount", 0);
        String aiCategory = parsed.optString("category", "");
        String aiMethod = parsed.optString("method", "cash");
        String aiNote = parsed.optString("note", "");
        if (!PAYMENT_LABELS.containsKey(aiMethod)) aiMethod = "cash";
        final String[] chosenMethod = {aiMethod};
        final boolean[] chosenIsIncome = {aiIsIncome};

        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.PremiumBottomSheetDialog);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_ai_voice_preview, null);
        dialog.setContentView(v);

        TextView tvRaw = v.findViewById(R.id.tvAiRawText);
        TextView aiTabIncome = v.findViewById(R.id.aiTabIncome);
        TextView aiTabExpense = v.findViewById(R.id.aiTabExpense);
        TextInputEditText aiEtAmount = v.findViewById(R.id.aiEtAmount);
        TextInputEditText aiEtCategory = v.findViewById(R.id.aiEtCategory);
        TextInputEditText aiEtNote = v.findViewById(R.id.aiEtNote);
        LinearLayout aiRowPaymentMethods = v.findViewById(R.id.aiRowPaymentMethods);
        TextView btnAiCancel = v.findViewById(R.id.btnAiCancel);
        TextView btnAiSave = v.findViewById(R.id.btnAiSave);

        tvRaw.setText("আপনি বলেছেন: \u201c" + spokenText + "\u201d");
        aiEtAmount.setText(aiAmount > 0 ? formatAmountPlain(aiAmount) : "");
        aiEtCategory.setText(aiCategory);
        aiEtNote.setText(aiNote);

        Runnable[] refreshMethodsHolder = new Runnable[1];
        Runnable refreshTabs = () -> {
            aiTabIncome.setBackground(chosenIsIncome[0]
                    ? getResources().getDrawable(R.drawable.bg_txn_tab_selected_income) : null);
            aiTabExpense.setBackground(!chosenIsIncome[0]
                    ? getResources().getDrawable(R.drawable.bg_txn_tab_selected_expense) : null);
            aiTabIncome.setTextColor(androidx.core.content.ContextCompat.getColor(this,
                    chosenIsIncome[0] ? R.color.white : R.color.txnTabInactiveText));
            aiTabExpense.setTextColor(androidx.core.content.ContextCompat.getColor(this,
                    !chosenIsIncome[0] ? R.color.white : R.color.txnTabInactiveText));
            if (refreshMethodsHolder[0] != null) refreshMethodsHolder[0].run();
        };

        Runnable refreshMethods = () -> {
            aiRowPaymentMethods.removeAllViews();
            int accentColor = androidx.core.content.ContextCompat.getColor(this,
                    chosenIsIncome[0] ? R.color.incomeColor : R.color.expenseColor);
            for (Map.Entry<String, String> entry : PAYMENT_LABELS.entrySet()) {
                String key = entry.getKey();
                boolean selected = key.equals(chosenMethod[0]);

                LinearLayout item = new LinearLayout(this);
                item.setOrientation(LinearLayout.VERTICAL);
                item.setGravity(Gravity.CENTER);
                item.setPadding(22, 14, 22, 14);
                item.setClickable(true);
                item.setFocusable(true);
                item.setBackground(getResources().getDrawable(selected
                        ? (chosenIsIncome[0] ? R.drawable.bg_txn_payment_item_selected_income : R.drawable.bg_txn_payment_item_selected_expense)
                        : R.drawable.bg_txn_payment_item));

                ImageView icon = new ImageView(this);
                icon.setImageResource(getMethodIcon(key));
                icon.setLayoutParams(new LinearLayout.LayoutParams(48, 48));
                if (selected) icon.setColorFilter(accentColor);

                TextView label = new TextView(this);
                label.setText(entry.getValue());
                label.setTextSize(11f);
                label.setTypeface(label.getTypeface(), android.graphics.Typeface.BOLD);
                label.setTextColor(selected ? accentColor : androidx.core.content.ContextCompat.getColor(this, R.color.textPrimary));
                LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                labelLp.topMargin = 6;
                label.setLayoutParams(labelLp);

                item.addView(icon);
                item.addView(label);

                LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                itemLp.setMarginEnd(10);
                item.setLayoutParams(itemLp);

                item.setOnClickListener(cv -> {
                    chosenMethod[0] = key;
                    refreshMethodsHolder[0].run();
                });

                aiRowPaymentMethods.addView(item);
            }
        };
        refreshMethodsHolder[0] = refreshMethods;
        refreshTabs.run();

        aiTabIncome.setOnClickListener(cv -> { chosenIsIncome[0] = true; refreshTabs.run(); });
        aiTabExpense.setOnClickListener(cv -> { chosenIsIncome[0] = false; refreshTabs.run(); });

        btnAiCancel.setOnClickListener(cv -> dialog.dismiss());

        btnAiSave.setOnClickListener(cv -> {
            String amtStr = aiEtAmount.getText() != null ? aiEtAmount.getText().toString().trim() : "";
            String catStr = aiEtCategory.getText() != null ? aiEtCategory.getText().toString().trim() : "";
            if (catStr.isEmpty()) {
                Toast.makeText(this, "ক্যাটাগরি লিখুন", Toast.LENGTH_SHORT).show();
                return;
            }
            double amt;
            try {
                amt = Double.parseDouble(amtStr);
                if (amt <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                Toast.makeText(this, "সঠিক পরিমাণ লিখুন", Toast.LENGTH_SHORT).show();
                return;
            }

            // মূল ফর্মে বসিয়ে, বিদ্যমান saveTransaction() লজিক ব্যবহার করা
            isIncome = chosenIsIncome[0];
            applyModeUI();
            etAmount.setText(String.valueOf(amt));
            etCategory.setText(catStr);
            etNote.setText(aiEtNote.getText() != null ? aiEtNote.getText().toString().trim() : "");
            selectedMethod = chosenMethod[0];
            loadPaymentMethods();

            dialog.dismiss();
            saveTransaction();
        });

        dialog.show();
    }

    /** ট্যাব সুইচ হলে পুরো UI (রঙ, লেবেল, ক্যাটাগরি) আপডেট করে — স্মুথ ট্রানজিশনসহ */
    private void applyModeUI() {
        if (bodyContainerTxn != null) {
            TransitionManager.beginDelayedTransition(bodyContainerTxn, new AutoTransition().setDuration(220));
        }

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

        // Save circle button — আয়ে সবুজ, ব্যয়ে লাল (বাইরের চিকন রিং সহ)
        btnSaveTxn.setBackground(getResources().getDrawable(
                isIncome ? R.drawable.circle_gradient_green : R.drawable.circle_gradient_red));
        // MaterialButton ডিফল্টভাবে থিমের colorPrimary (নীল) দিয়ে backgroundTint বসিয়ে দেয়,
        // যেটা উপরের গ্রেডিয়েন্ট ড্রয়েবলকে ঢেকে ফেলে — এটা null করে আসল সবুজ/লাল রঙ দেখানো নিশ্চিত করা হচ্ছে
        btnSaveTxn.setBackgroundTintList(null);
        btnSaveTxn.setText(isIncome ? "আয়\nসংরক্ষণ" : "ব্যয়\nসংরক্ষণ");
        if (ringSaveTxn != null) {
            ringSaveTxn.setBackground(getResources().getDrawable(
                    isIncome ? R.drawable.ring_outline_green : R.drawable.ring_outline_red));
        }

        // Category field icon + hint
        tilCategory.setStartIconDrawable(
                isIncome ? R.drawable.ic_plus_circle_txn : R.drawable.ic_minus_circle_txn);
        tilCategory.setHint(isIncome ? "আয়ের উৎস লিখুন বা নিচ থেকে বাছুন" : "ব্যয়ের ধরন লিখুন বা নিচ থেকে বাছুন");
        etCategory.setText("");

        loadCategoryChips();
        loadPaymentMethods();
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

    private String formatAmountPlain(double amount) {
        if (amount == Math.floor(amount) && !Double.isInfinite(amount)) {
            return String.valueOf((long) amount);
        }
        return String.valueOf(amount);
    }

    private int getMethodIcon(String key) {
        switch (key) {
            case "bkash":  return R.drawable.ic_method_bkash;
            case "nagad":  return R.drawable.ic_method_nagad;
            case "rocket": return R.drawable.ic_method_rocket;
            case "bank":   return R.drawable.ic_method_bank;
            case "other":  return R.drawable.ic_method_other;
            default:       return R.drawable.ic_method_cash;
        }
    }

    /** ডিফল্ট লেনদেন মাধ্যম গ্রিড (Cash, bKash, Nagad, Rocket, Bank, Others) — দুই সারিতে সমান ভাগে */
    private void loadPaymentMethods() {
        if (rowPaymentMethods1 == null || rowPaymentMethods2 == null) return;
        rowPaymentMethods1.removeAllViews();
        rowPaymentMethods2.removeAllViews();

        int accentColor = androidx.core.content.ContextCompat.getColor(
                this, isIncome ? R.color.incomeColor : R.color.expenseColor);

        int i = 0;
        for (Map.Entry<String, String> entry : PAYMENT_LABELS.entrySet()) {
            String key = entry.getKey();
            String label = entry.getValue();
            LinearLayout targetRow = (i < 3) ? rowPaymentMethods1 : rowPaymentMethods2;

            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(10, 16, 10, 16);
            item.setClickable(true);
            item.setFocusable(true);

            boolean selected = key.equals(selectedMethod);
            item.setBackground(getResources().getDrawable(selected
                    ? (isIncome ? R.drawable.bg_txn_payment_item_selected_income : R.drawable.bg_txn_payment_item_selected_expense)
                    : R.drawable.bg_txn_payment_item));

            ImageView icon = new ImageView(this);
            icon.setImageResource(getMethodIcon(key));
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(56, 56);
            icon.setLayoutParams(iconLp);
            if (selected) icon.setColorFilter(accentColor);

            TextView label1 = new TextView(this);
            label1.setText(label);
            label1.setTextSize(11.5f);
            label1.setTypeface(label1.getTypeface(), android.graphics.Typeface.BOLD);
            label1.setTextColor(selected ? accentColor : androidx.core.content.ContextCompat.getColor(this, R.color.textPrimary));
            LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            labelLp.topMargin = 8;
            label1.setLayoutParams(labelLp);

            item.addView(icon);
            item.addView(label1);

            LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            itemLp.setMarginEnd((i % 3 != 2) ? 10 : 0);
            item.setLayoutParams(itemLp);

            item.setOnClickListener(v -> {
                selectedMethod = key;
                loadPaymentMethods();
            });

            targetRow.addView(item);
            i++;
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
        t.setMethod(selectedMethod);

        if (isIncome) db.addIncome(t); else db.addExpense(t);
        playTapSound();
        com.jrappspot.cashlipi.widgets.FinanceWidgetProvider.updateAll(this);

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
        selectedMethod = "cash";
        loadCategoryChips();
        loadPaymentMethods();
    }
}
