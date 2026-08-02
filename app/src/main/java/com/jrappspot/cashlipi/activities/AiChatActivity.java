package com.jrappspot.cashlipi.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.adapters.ChatAdapter;
import com.jrappspot.cashlipi.models.ChatMessage;
import com.jrappspot.cashlipi.models.LedgerEntry;
import com.jrappspot.cashlipi.models.Transaction;
import com.jrappspot.cashlipi.utils.DatabaseManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * ══════════════════════════════════════════════════════════════════
 *  ক্যাশলিপি AI চ্যাট — সম্পূর্ণ রিডিজাইন (v2)
 * ══════════════════════════════════════════════════════════════════
 *  UI/UX: welcome screen, প্রম্পট চিপ, বাবল/মেসেজ স্টাইল, টাইপিং
 *  ইন্ডিকেটর, কপি/রিজেনারেট অ্যাকশন, নতুন-চ্যাট বাটন ইত্যাদি।
 *
 *  AI ব্যাকএন্ড: Pollinations.ai এর ফ্রি টেক্সট API (কোনো key লাগে না)
 *  callAiApi() মেথডে যুক্ত করা আছে। অন্য কোনো API (Groq, নিজের
 *  ব্যাকএন্ড ইত্যাদি) দিয়ে বদলাতে চাইলে শুধু callAiApi() মেথডটা
 *  বদলালেই হবে — বাকি UI/ফ্লো অপরিবর্তিত থাকবে।
 * ══════════════════════════════════════════════════════════════════
 */
public class AiChatActivity extends BaseActivity implements ChatAdapter.ActionListener {

    private RecyclerView rvChat;
    private NestedScrollView welcomeScroll;
    private EditText etInput;
    private ImageView btnSend, btnMic, btnBack, btnNewChat;
    private TextView chip1, chip2, chip3, chip4;

    private final List<ChatMessage> messages = new ArrayList<>();
    private ChatAdapter adapter;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private DatabaseManager db;

    private String lastUserPrompt = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);
        db = DatabaseManager.getInstance(this);

        bindViews();
        setupRecycler();
        setupInput();
        setupHeaderActions();
        setupSuggestionChips();
    }

    private void bindViews() {
        rvChat = findViewById(R.id.rvChat);
        welcomeScroll = findViewById(R.id.welcomeScroll);
        etInput = findViewById(R.id.etInput);
        btnSend = findViewById(R.id.btnSend);
        btnMic = findViewById(R.id.btnMic);
        btnBack = findViewById(R.id.btnBack);
        btnNewChat = findViewById(R.id.btnNewChat);
        chip1 = findViewById(R.id.chip1);
        chip2 = findViewById(R.id.chip2);
        chip3 = findViewById(R.id.chip3);
        chip4 = findViewById(R.id.chip4);
    }

    private void setupRecycler() {
        adapter = new ChatAdapter(messages, this);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvChat.setLayoutManager(lm);
        rvChat.setAdapter(adapter);
    }

    private void setupInput() {
        updateSendButtonState(false);
        etInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                boolean hasText = s != null && s.toString().trim().length() > 0;
                updateSendButtonState(hasText);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        btnSend.setOnClickListener(v -> sendMessage());
        btnMic.setOnClickListener(v ->
            Toast.makeText(this, "ভয়েস ইনপুট শীঘ্রই আসছে", Toast.LENGTH_SHORT).show());
    }

    private void setupHeaderActions() {
        btnBack.setOnClickListener(v -> finish());
        btnNewChat.setOnClickListener(v -> startNewChat());
    }

    private void setupSuggestionChips() {
        chip1.setOnClickListener(v -> sendMessageText("এই মাসের খরচ বিশ্লেষণ করো"));
        chip2.setOnClickListener(v -> sendMessageText("সঞ্চয়ের পরামর্শ দাও"));
        chip3.setOnClickListener(v -> sendMessageText("আমার আর্থিক স্বাস্থ্য স্কোর বুঝিয়ে দাও"));
        chip4.setOnClickListener(v -> sendMessageText("একটা মাসিক বাজেট প্ল্যান বানাও"));
    }

    private void updateSendButtonState(boolean active) {
        btnSend.setEnabled(active);
        btnSend.setBackgroundResource(active ? R.drawable.bg_ai_send_btn_active : R.drawable.bg_ai_send_btn_disabled);
        btnSend.setAlpha(active ? 1f : 0.7f);
    }

    private void startNewChat() {
        messages.clear();
        adapter.notifyDataSetChanged();
        etInput.setText("");
        showWelcome(true);
    }

    private void showWelcome(boolean show) {
        welcomeScroll.setVisibility(show ? View.VISIBLE : View.GONE);
        rvChat.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void sendMessage() {
        String text = etInput.getText() != null ? etInput.getText().toString().trim() : "";
        if (text.isEmpty()) return;
        etInput.setText("");
        sendMessageText(text);
    }

    private void sendMessageText(String text) {
        if (text == null || text.trim().isEmpty()) return;
        showWelcome(false);
        lastUserPrompt = text;

        messages.add(new ChatMessage(ChatMessage.Role.USER, text));
        adapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();

        requestAiReply(text);
    }

    private void requestAiReply(String prompt) {
        // টাইপিং ইন্ডিকেটর দেখাও
        messages.add(new ChatMessage(ChatMessage.Role.TYPING, ""));
        adapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();

        String context = buildFinancialContext();

        executor.execute(() -> {
            String reply;
            try {
                reply = callAiApi(context, prompt);
            } catch (Exception e) {
                reply = "উত্তর পেতে ব্যর্থ হয়েছে। ইন্টারনেট সংযোগ পরীক্ষা করুন।";
            }
            final String finalReply = reply;
            runOnUiThread(() -> {
                removeTypingIndicator();
                messages.add(new ChatMessage(ChatMessage.Role.BOT, finalReply));
                adapter.notifyItemInserted(messages.size() - 1);
                scrollToBottom();
            });
        });
    }

    private void removeTypingIndicator() {
        if (!messages.isEmpty() && messages.get(messages.size() - 1).getRole() == ChatMessage.Role.TYPING) {
            int idx = messages.size() - 1;
            messages.remove(idx);
            adapter.notifyItemRemoved(idx);
        }
    }

    private void scrollToBottom() {
        rvChat.post(() -> rvChat.scrollToPosition(messages.size() - 1));
    }

    private String buildFinancialContext() {
        StringBuilder sb = new StringBuilder();
        sb.append("── সারসংক্ষেপ ──\n");
        sb.append("মোট আয়: ").append(DatabaseManager.formatAmount(db.getTotalIncome())).append("\n");
        sb.append("মোট ব্যয়: ").append(DatabaseManager.formatAmount(db.getTotalExpense())).append("\n");
        sb.append("ব্যালেন্স: ").append(DatabaseManager.formatAmount(db.getBalance())).append("\n");
        sb.append("মোট সঞ্চয়: ").append(DatabaseManager.formatAmount(db.getTotalSavings())).append("\n");
        sb.append("মোট দেনা (বাকি আছে): ").append(DatabaseManager.formatAmount(db.getTotalDena())).append("\n");
        sb.append("মোট পাওনা (বাকি আছে): ").append(DatabaseManager.formatAmount(db.getTotalPabona())).append("\n");
        sb.append("আর্থিক স্বাস্থ্য স্কোর: ").append(db.calcHealthScore()).append("/100\n\n");

        appendTransactionBreakdown(sb, "── ব্যয় (ক্যাটাগরি অনুযায়ী, শীর্ষ ৫) ──",
            db.getExpenseList(), true);
        appendTransactionBreakdown(sb, "── আয় (উৎস অনুযায়ী, শীর্ষ ৫) ──",
            db.getIncomeList(), false);
        appendSavingsBreakdown(sb);
        appendLedgerBreakdown(sb);

        return sb.toString();
    }

    /** আয়/ব্যয়কে ক্যাটাগরি বা উৎস অনুযায়ী গ্রুপ করে শীর্ষ ৫টা দেখায়। */
    private void appendTransactionBreakdown(StringBuilder sb, String title, List<Transaction> list, boolean isExpense) {
        if (list == null || list.isEmpty()) return;
        Map<String, Double> totals = new LinkedHashMap<>();
        for (Transaction t : list) {
            String key = isExpense ? t.getCategory() : t.getSource();
            if (key == null || key.trim().isEmpty()) key = "অন্যান্য";
            totals.merge(key, t.getAmount(), Double::sum);
        }
        List<Map.Entry<String, Double>> entries = new ArrayList<>(totals.entrySet());
        entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        sb.append(title).append("\n");
        int limit = Math.min(5, entries.size());
        for (int i = 0; i < limit; i++) {
            Map.Entry<String, Double> e = entries.get(i);
            sb.append("• ").append(e.getKey()).append(": ")
              .append(DatabaseManager.formatAmount(e.getValue())).append("\n");
        }
        if (entries.size() > limit) {
            sb.append("...আরও ").append(entries.size() - limit).append("টি ক্যাটাগরি\n");
        }
        sb.append("\n");
    }

    /** সঞ্চয়কে মাধ্যম (ব্যাংক/বিকাশ/নগদ/রকেট) অনুযায়ী গ্রুপ করে দেখায়। */
    private void appendSavingsBreakdown(StringBuilder sb) {
        List<Transaction> list = db.getSavingsList();
        if (list == null || list.isEmpty()) return;
        Map<String, Double> totals = new LinkedHashMap<>();
        for (Transaction t : list) {
            String key = t.getMethodDisplay().trim();
            if (key.isEmpty()) key = "অন্যান্য";
            totals.merge(key, t.getAmount(), Double::sum);
        }
        sb.append("── সঞ্চয় (মাধ্যম অনুযায়ী) ──\n");
        for (Map.Entry<String, Double> e : totals.entrySet()) {
            sb.append("• ").append(e.getKey()).append(": ")
              .append(DatabaseManager.formatAmount(e.getValue())).append("\n");
        }
        sb.append("\n");
    }

    /** এখনো পুরোপুরি শোধ হয়নি এমন দেনা/পাওনা — ব্যক্তির নাম ও বাকি থাকা পরিমাণসহ। */
    private void appendLedgerBreakdown(StringBuilder sb) {
        List<LedgerEntry> list = db.getLedgerList();
        if (list == null || list.isEmpty()) return;

        List<LedgerEntry> denaUnpaid = new ArrayList<>();
        List<LedgerEntry> pabonaUnpaid = new ArrayList<>();
        for (LedgerEntry e : list) {
            if (e.getRemainingAmount() <= 0.009) continue;
            if (e.isDena()) denaUnpaid.add(e); else pabonaUnpaid.add(e);
        }

        if (!denaUnpaid.isEmpty()) {
            sb.append("── বাকি দেনা (যাকে দিতে হবে) ──\n");
            int limit = Math.min(6, denaUnpaid.size());
            for (int i = 0; i < limit; i++) {
                LedgerEntry e = denaUnpaid.get(i);
                sb.append("• ").append(e.getPerson()).append(": ")
                  .append(DatabaseManager.formatAmount(e.getRemainingAmount())).append("\n");
            }
            if (denaUnpaid.size() > limit) sb.append("...আরও ").append(denaUnpaid.size() - limit).append("জন\n");
            sb.append("\n");
        }
        if (!pabonaUnpaid.isEmpty()) {
            sb.append("── বাকি পাওনা (যার কাছ থেকে পাবেন) ──\n");
            int limit = Math.min(6, pabonaUnpaid.size());
            for (int i = 0; i < limit; i++) {
                LedgerEntry e = pabonaUnpaid.get(i);
                sb.append("• ").append(e.getPerson()).append(": ")
                  .append(DatabaseManager.formatAmount(e.getRemainingAmount())).append("\n");
            }
            if (pabonaUnpaid.size() > limit) sb.append("...আরও ").append(pabonaUnpaid.size() - limit).append("জন\n");
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  Pollinations.ai টেক্সট API — CashLipiAiHelper-এর মাধ্যমে কল হয়
    //  (মাল্টি-কি ফলব্যাকসহ, ভয়েস-এন্ট্রি ফিচারের সাথে shared লজিক)
    // ══════════════════════════════════════════════════════════════
    private String callAiApi(String context, String userMsg) throws Exception {
        String systemPrompt =
            "আপনি ক্যাশলিপি (CashLipi) অ্যাপের একজন বন্ধুত্বপূর্ণ ব্যক্তিগত আর্থিক সহায়ক AI। "
            + "সবসময় সংক্ষিপ্ত, সহজ ও স্পষ্ট বাংলায় উত্তর দিন। ব্যবহারকারীর নিচের সম্পূর্ণ আর্থিক "
            + "তথ্য (আয়-ব্যয়ের ক্যাটাগরি, সঞ্চয়, দেনা-পাওনার তালিকা) প্রয়োজন অনুযায়ী ব্যবহার করুন:\n\n"
            + context;

        try {
            return com.jrappspot.cashlipi.utils.PollinationsAiHelper.callText(systemPrompt, userMsg);
        } catch (Exception e) {
            return "উত্তর পাওয়া যায়নি। একটু পর আবার চেষ্টা করুন।";
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  ChatAdapter.ActionListener
    // ══════════════════════════════════════════════════════════════
    @Override
    public void onCopy(String text) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("CashLipi AI", text));
            Toast.makeText(this, "কপি হয়েছে", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRegenerate() {
        if (lastUserPrompt.isEmpty()) return;
        // শেষ বট মেসেজটা সরিয়ে আবার জিজ্ঞাসা করো
        if (!messages.isEmpty() && messages.get(messages.size() - 1).getRole() == ChatMessage.Role.BOT) {
            int idx = messages.size() - 1;
            messages.remove(idx);
            adapter.notifyItemRemoved(idx);
        }
        requestAiReply(lastUserPrompt);
    }
}
