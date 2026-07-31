package com.jrappspot.cashlipi.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.models.Transaction;
import com.jrappspot.cashlipi.utils.InvoicePdfHelper;
import com.jrappspot.cashlipi.views.SignaturePadView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * আয়-ব্যয় লিস্ট পেজের "PDF এক্সপোর্ট" / "প্রিন্ট" — ফুল-পেজ স্ক্রিন (আর পপ-আপ ডায়ালগ নয়)।
 * কোম্পানির নাম, শিরোনাম/সাক্ষর-প্যাড টেমপ্লেট, কাস্টম তারিখ-রেঞ্জ (শুরু–শেষ ক্যালেন্ডার),
 * অ্যাকসেন্ট রঙ, এবং স্বাক্ষর (হাতে আঁকা পপ-আপ পেড অথবা টাইপ করা টেক্সট) — সবকিছু এখানে
 * সেট করে লাইভ প্রিভিউ দেখা যায়, তারপর PDF তৈরি বা প্রিন্ট করা যায়।
 */
public class InvoiceExportActivity extends BaseActivity {

    public static final String EXTRA_TYPE = "extra_type";
    public static final String EXTRA_FOR_PRINT = "extra_for_print";

    private List<Transaction> fullList;
    private String type;
    private boolean forPrint;

    private ImageView ivPreview;
    private EditText etCompany;
    private int selectedTemplate = 0;
    private int selectedColor;

    // ── তারিখ-রেঞ্জ স্টেট ──
    private boolean useRange = false;
    private String singleDisplay, singleYmd;
    private String startDisplay, startYmd, endDisplay, endYmd;
    private LinearLayout singleDateGroup, rangeDateGroup;
    private EditText etSingleDate, etStartDate, etEndDate;

    // ── স্বাক্ষর স্টেট: 0 = কিছুই না, 1 = হাতে আঁকা, 2 = টাইপ করা ──
    private int sigMode = 0;
    private Bitmap signatureBitmap;
    private String signatureText = "";
    private LinearLayout sigDrawSection, sigTypeSection;
    private ImageView ivSigThumb;
    private EditText etSigText;

    private int darkBg, cardBg, inputBg, inputBorder, textLight, textMuted, divider;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fullList = InvoicePdfHelper.takePendingList();
        type = getIntent().getStringExtra(EXTRA_TYPE);
        forPrint = getIntent().getBooleanExtra(EXTRA_FOR_PRINT, false);

        if (fullList == null) {
            Toast.makeText(this, "তালিকা পাওয়া যায়নি, আবার চেষ্টা করুন", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        darkBg = ContextCompat.getColor(this, R.color.darkBgDeep);
        cardBg = ContextCompat.getColor(this, R.color.darkCard);
        inputBg = ContextCompat.getColor(this, R.color.darkInputBg);
        inputBorder = ContextCompat.getColor(this, R.color.darkInputBorder);
        textLight = ContextCompat.getColor(this, R.color.textOnDark);
        textMuted = ContextCompat.getColor(this, R.color.textHint);
        divider = ContextCompat.getColor(this, R.color.darkInputBorder);

        selectedColor = "expense".equals(type)
                ? InvoicePdfHelper.PRESET_COLORS[3] : InvoicePdfHelper.PRESET_COLORS[1];

        Calendar today = Calendar.getInstance();
        singleYmd = ymd(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));
        singleDisplay = disp(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));
        startYmd = endYmd = singleYmd;
        startDisplay = endDisplay = singleDisplay;

        setContentView(buildRoot());
        refreshPreview();
    }

    // ══════════════════════════════ UI বিল্ড ══════════════════════════════

    private View buildRoot() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        root.setBackgroundColor(darkBg);

        root.addView(buildHeader());

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        body.setPadding(pad, pad, pad, dp(24));
        scroll.addView(body);
        root.addView(scroll);

        body.addView(buildPreviewCard());
        body.addView(gap(14));
        body.addView(buildCompanyCard());
        body.addView(gap(14));
        body.addView(buildTemplateCard());
        body.addView(gap(14));
        body.addView(buildDateCard());
        body.addView(gap(14));
        body.addView(buildColorCard());
        body.addView(gap(14));
        body.addView(buildSignatureCard());

        root.addView(buildBottomBar());
        return root;
    }

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundResource(R.drawable.bg_header_generic);
        int padH = dp(14), padV = dp(14);
        header.setPadding(padH, padV, padH, padV);

        ImageView back = new ImageView(this);
        back.setImageResource(R.drawable.ic_arrow_back);
        back.setColorFilter(Color.WHITE);
        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(dp(26), dp(26));
        backLp.setMarginEnd(dp(14));
        back.setLayoutParams(backLp);
        back.setOnClickListener(v -> finish());
        header.addView(back);

        TextView title = new TextView(this);
        title.setText(forPrint ? "প্রিন্ট" : "PDF এক্সপোর্ট");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        header.addView(title);

        return header;
    }

    private View card(View... children) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(cardBg);
        gd.setCornerRadius(dp(14));
        gd.setStroke(dp(1), divider);
        card.setBackground(gd);
        int pad = dp(16);
        card.setPadding(pad, pad, pad, pad);
        for (View c : children) card.addView(c);
        return card;
    }

    private View buildPreviewCard() {
        TextView lbl = sectionLabel("লাইভ প্রিভিউ");
        FrameLayout previewFrame = new FrameLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(210), dp(297));
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        lp.topMargin = dp(8);
        previewFrame.setLayoutParams(lp);
        previewFrame.setBackgroundColor(0xFFE5E7EB);
        previewFrame.setPadding(dp(3), dp(3), dp(3), dp(3));
        ivPreview = new ImageView(this);
        ivPreview.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        ivPreview.setScaleType(ImageView.ScaleType.FIT_XY);
        previewFrame.addView(ivPreview);
        return card(lbl, previewFrame);
    }

    private View buildCompanyCard() {
        TextView lbl = sectionLabel("কোম্পানি/প্রতিষ্ঠানের নাম (খালি রাখলে শুধু \"CashLipi\")");
        etCompany = themedInput();
        etCompany.setHint("CashLipi ক্যাশলিপি");
        etCompany.setInputType(InputType.TYPE_CLASS_TEXT);
        etCompany.addTextChangedListener(simpleWatcher(this::refreshPreview));
        return card(lbl, etCompany);
    }

    private View buildTemplateCard() {
        TextView lbl = sectionLabel("শিরোনাম/সাক্ষর-প্যাড স্টাইল বেছে নিন");
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(6), 0, 0);
        List<TextView> chips = new ArrayList<>();
        for (int t = 0; t < InvoicePdfHelper.TEMPLATE_NAMES.length; t++) {
            TextView chip = chip(InvoicePdfHelper.TEMPLATE_NAMES[t], t < InvoicePdfHelper.TEMPLATE_NAMES.length - 1);
            final int idx = t;
            chip.setOnClickListener(v -> {
                selectedTemplate = idx;
                for (int k = 0; k < chips.size(); k++) InvoicePdfHelper.styleChip(chips.get(k), k == idx);
                refreshPreview();
            });
            chips.add(chip);
            row.addView(chip);
        }
        for (int k = 0; k < chips.size(); k++) InvoicePdfHelper.styleChip(chips.get(k), k == 0);
        return card(lbl, row);
    }

    private View buildDateCard() {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);

        LinearLayout toggleRow = new LinearLayout(this);
        toggleRow.setOrientation(LinearLayout.HORIZONTAL);
        toggleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView toggleLbl = sectionLabel("তারিখ");
        toggleLbl.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        TextView rangeSwitch = new TextView(this);
        rangeSwitch.setText("📅 কাস্টম রেঞ্জ");
        rangeSwitch.setTextSize(11.5f);
        rangeSwitch.setTypeface(Typeface.DEFAULT_BOLD);
        rangeSwitch.setPadding(dp(12), dp(7), dp(12), dp(7));
        toggleRow.addView(toggleLbl);
        toggleRow.addView(rangeSwitch);
        wrap.addView(toggleRow);
        wrap.addView(gap(8));

        // ── একক তারিখ (ডিফল্ট) ──
        singleDateGroup = new LinearLayout(this);
        singleDateGroup.setOrientation(LinearLayout.VERTICAL);
        etSingleDate = themedInput();
        etSingleDate.setText(singleDisplay);
        etSingleDate.setFocusable(false);
        etSingleDate.setOnClickListener(v -> pickDate(singleYmd, (y, m, d) -> {
            singleYmd = ymd(y, m, d);
            singleDisplay = disp(y, m, d);
            etSingleDate.setText(singleDisplay);
            refreshPreview();
        }));
        singleDateGroup.addView(etSingleDate);

        // ── কাস্টম রেঞ্জ: শুরু – শেষ ──
        rangeDateGroup = new LinearLayout(this);
        rangeDateGroup.setOrientation(LinearLayout.HORIZONTAL);
        rangeDateGroup.setVisibility(View.GONE);

        LinearLayout startCol = new LinearLayout(this);
        startCol.setOrientation(LinearLayout.VERTICAL);
        startCol.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        TextView startLbl = subLabel("শুরুর তারিখ");
        etStartDate = themedInput();
        etStartDate.setText(startDisplay);
        etStartDate.setFocusable(false);
        etStartDate.setOnClickListener(v -> pickDate(startYmd, (y, m, d) -> {
            startYmd = ymd(y, m, d);
            startDisplay = disp(y, m, d);
            etStartDate.setText(startDisplay);
            refreshPreview();
        }));
        startCol.addView(startLbl);
        startCol.addView(etStartDate);

        LinearLayout endCol = new LinearLayout(this);
        endCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams endLp = new LinearLayout.LayoutParams(0, -2, 1f);
        endLp.setMarginStart(dp(10));
        endCol.setLayoutParams(endLp);
        TextView endLbl = subLabel("শেষ তারিখ");
        etEndDate = themedInput();
        etEndDate.setText(endDisplay);
        etEndDate.setFocusable(false);
        etEndDate.setOnClickListener(v -> pickDate(endYmd, (y, m, d) -> {
            endYmd = ymd(y, m, d);
            endDisplay = disp(y, m, d);
            etEndDate.setText(endDisplay);
            refreshPreview();
        }));
        endCol.addView(endLbl);
        endCol.addView(etEndDate);

        rangeDateGroup.addView(startCol);
        rangeDateGroup.addView(endCol);

        wrap.addView(singleDateGroup);
        wrap.addView(rangeDateGroup);

        rangeSwitch.setOnClickListener(v -> {
            useRange = !useRange;
            styleToggle(rangeSwitch, useRange);
            singleDateGroup.setVisibility(useRange ? View.GONE : View.VISIBLE);
            rangeDateGroup.setVisibility(useRange ? View.VISIBLE : View.GONE);
            refreshPreview();
        });
        styleToggle(rangeSwitch, false);

        return card(wrap);
    }

    private View buildColorCard() {
        TextView lbl = sectionLabel("রঙ পছন্দ করুন");
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(8), 0, 0);
        List<View> swatches = new ArrayList<>();
        for (int c : InvoicePdfHelper.PRESET_COLORS) {
            View sw = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(36), dp(36));
            lp.setMarginEnd(dp(12));
            sw.setLayoutParams(lp);
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            gd.setColor(c);
            if (c == selectedColor) gd.setStroke(dp(3), Color.WHITE);
            sw.setBackground(gd);
            swatches.add(sw);
            row.addView(sw);
        }
        for (int idx = 0; idx < swatches.size(); idx++) {
            View sw = swatches.get(idx);
            int c = InvoicePdfHelper.PRESET_COLORS[idx];
            sw.setOnClickListener(v -> {
                selectedColor = c;
                for (View s : swatches) ((GradientDrawable) s.getBackground()).setStroke(0, Color.TRANSPARENT);
                ((GradientDrawable) sw.getBackground()).setStroke(dp(3), Color.WHITE);
                refreshPreview();
            });
        }
        return card(lbl, row);
    }

    private View buildSignatureCard() {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.addView(sectionLabel("স্বাক্ষর"));
        wrap.addView(gap(6));

        LinearLayout modeRow = new LinearLayout(this);
        modeRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] modeNames = {"রাখবো না", "✍️ হাতে লিখা", "⌨️ টাইপ করে"};
        List<TextView> modeChips = new ArrayList<>();
        for (int m = 0; m < modeNames.length; m++) {
            TextView chip = chip(modeNames[m], m < modeNames.length - 1);
            final int idx = m;
            chip.setOnClickListener(v -> {
                sigMode = idx;
                for (int k = 0; k < modeChips.size(); k++) InvoicePdfHelper.styleChip(modeChips.get(k), k == idx);
                sigDrawSection.setVisibility(idx == 1 ? View.VISIBLE : View.GONE);
                sigTypeSection.setVisibility(idx == 2 ? View.VISIBLE : View.GONE);
                if (idx != 1) signatureBitmap = null;
                refreshPreview();
            });
            modeChips.add(chip);
            modeRow.addView(chip);
        }
        for (int k = 0; k < modeChips.size(); k++) InvoicePdfHelper.styleChip(modeChips.get(k), k == 0);
        wrap.addView(modeRow);
        wrap.addView(gap(10));

        // ── হাতে আঁকা সেকশন ──
        sigDrawSection = new LinearLayout(this);
        sigDrawSection.setOrientation(LinearLayout.HORIZONTAL);
        sigDrawSection.setGravity(Gravity.CENTER_VERTICAL);
        sigDrawSection.setVisibility(View.GONE);

        ivSigThumb = new ImageView(this);
        LinearLayout.LayoutParams thumbLp = new LinearLayout.LayoutParams(dp(90), dp(46));
        thumbLp.setMarginEnd(dp(12));
        ivSigThumb.setLayoutParams(thumbLp);
        ivSigThumb.setScaleType(ImageView.ScaleType.FIT_CENTER);
        GradientDrawable thumbBg = new GradientDrawable();
        thumbBg.setColor(Color.WHITE);
        thumbBg.setCornerRadius(dp(8));
        ivSigThumb.setBackground(thumbBg);
        sigDrawSection.addView(ivSigThumb);

        TextView btnDraw = pillButton("স্বাক্ষর আঁকুন");
        btnDraw.setOnClickListener(v -> showSignaturePadDialog());
        sigDrawSection.addView(btnDraw);

        wrap.addView(sigDrawSection);

        // ── টাইপ করা সেকশন ──
        sigTypeSection = new LinearLayout(this);
        sigTypeSection.setOrientation(LinearLayout.VERTICAL);
        sigTypeSection.setVisibility(View.GONE);
        etSigText = themedInput();
        etSigText.setHint("আপনার নাম লিখুন (স্বাক্ষর হিসেবে বসবে)");
        etSigText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        etSigText.addTextChangedListener(simpleWatcher(() -> {
            signatureText = etSigText.getText().toString();
            refreshPreview();
        }));
        sigTypeSection.addView(etSigText);
        wrap.addView(sigTypeSection);

        return card(wrap);
    }

    private void showSignaturePadDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(14);
        box.setPadding(pad, pad, pad, pad);

        TextView hint = new TextView(this);
        hint.setText("নিচে আঙুল দিয়ে কলমের মতো স্বাক্ষর আঁকুন");
        hint.setTextColor(0xFF6B7280);
        hint.setTextSize(12.5f);
        hint.setPadding(0, 0, 0, dp(10));
        box.addView(hint);

        SignaturePadView pad2 = new SignaturePadView(this);
        LinearLayout.LayoutParams padLp = new LinearLayout.LayoutParams(-1, dp(200));
        pad2.setLayoutParams(padLp);
        GradientDrawable padBg = new GradientDrawable();
        padBg.setColor(Color.WHITE);
        padBg.setStroke(dp(2), 0xFFD1D5DB);
        padBg.setCornerRadius(dp(10));
        pad2.setBackground(padBg);
        box.addView(pad2);

        TextView clear = new TextView(this);
        clear.setText("মুছে ফেলুন");
        clear.setTextColor(0xFFDC2626);
        clear.setTypeface(Typeface.DEFAULT_BOLD);
        clear.setPadding(0, dp(10), 0, 0);
        clear.setOnClickListener(v -> pad2.clear());
        box.addView(clear);

        new AlertDialog.Builder(this)
                .setTitle("হাতে স্বাক্ষর দিন")
                .setView(box)
                .setPositiveButton("সংরক্ষণ করুন", (d, w) -> {
                    if (pad2.isEmpty()) {
                        Toast.makeText(this, "কিছু আঁকা হয়নি", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    signatureBitmap = pad2.getSignatureBitmap();
                    ivSigThumb.setImageBitmap(signatureBitmap);
                    refreshPreview();
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    private View buildBottomBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        int padH = dp(16), padV = dp(12);
        bar.setPadding(padH, padV, padH, padV);
        GradientDrawable barBg = new GradientDrawable();
        barBg.setColor(cardBg);
        bar.setBackground(barBg);
        bar.setElevation(dp(8));

        TextView cancel = new TextView(this);
        cancel.setText("বাতিল");
        cancel.setTextColor(textMuted);
        cancel.setTypeface(Typeface.DEFAULT_BOLD);
        cancel.setTextSize(14.5f);
        cancel.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(0, dp(48), 1f);
        cancel.setLayoutParams(cancelLp);
        cancel.setOnClickListener(v -> finish());
        bar.addView(cancel);

        TextView generate = new TextView(this);
        generate.setText(forPrint ? "প্রিন্ট করুন" : "PDF তৈরি করুন");
        generate.setTextColor(Color.WHITE);
        generate.setTypeface(Typeface.DEFAULT_BOLD);
        generate.setTextSize(14.5f);
        generate.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams genLp = new LinearLayout.LayoutParams(0, dp(48), 2f);
        genLp.setMarginStart(dp(12));
        generate.setLayoutParams(genLp);
        GradientDrawable genBg = new GradientDrawable();
        genBg.setColor(0xFF4F46E5);
        genBg.setCornerRadius(dp(12));
        generate.setBackground(genBg);
        generate.setOnClickListener(v -> generateAndFinish());
        bar.addView(generate);

        return bar;
    }

    // ══════════════════════════════ লজিক ══════════════════════════════

    private void refreshPreview() {
        String company = etCompany != null ? etCompany.getText().toString().trim() : "";
        if (company.isEmpty()) company = "CashLipi ক্যাশলিপি";
        String headerDate = useRange ? (startDisplay + " – " + endDisplay) : singleDisplay;
        List<Transaction> listForPreview = useRange
                ? InvoicePdfHelper.filterByDateRange(fullList, startYmd, endYmd) : fullList;
        Bitmap bmp = InvoicePdfHelper.renderPreviewBitmap(this, type, listForPreview, company, headerDate,
                selectedColor, selectedTemplate, sigMode == 1 ? signatureBitmap : null,
                sigMode == 2 ? signatureText : null);
        ivPreview.setImageBitmap(bmp);
    }

    private void generateAndFinish() {
        String company = etCompany.getText().toString().trim();
        if (company.isEmpty()) company = "CashLipi ক্যাশলিপি";
        String headerDate = useRange ? (startDisplay + " – " + endDisplay) : singleDisplay;
        List<Transaction> listForPdf = useRange
                ? InvoicePdfHelper.filterByDateRange(fullList, startYmd, endYmd) : fullList;

        byte[] pdf = InvoicePdfHelper.generatePdf(type, listForPdf, company, headerDate,
                selectedColor, selectedTemplate,
                sigMode == 1 ? signatureBitmap : null,
                sigMode == 2 ? signatureText : null);
        if (pdf == null) {
            Toast.makeText(this, "তৈরি ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show();
            return;
        }
        if (forPrint) InvoicePdfHelper.printPdfPublic(this, pdf, company);
        else InvoicePdfHelper.savePdfPublic(this, pdf);
        finish();
    }

    private interface DatePicked { void on(int y, int m, int d); }

    private void pickDate(String currentYmd, DatePicked cb) {
        Calendar c = Calendar.getInstance();
        try {
            String[] parts = currentYmd.split("-");
            c.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]));
        } catch (Exception ignored) {}
        new DatePickerDialog(this, (view, y, m, d) -> cb.on(y, m, d),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private String ymd(int y, int m, int d) {
        return String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d);
    }

    private String disp(int y, int m, int d) {
        return String.format(Locale.US, "%02d/%02d/%04d", d, m + 1, y);
    }

    // ══════════════════════════════ ছোট UI হেল্পার ══════════════════════════════

    private TextView sectionLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(12.5f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(textLight);
        return tv;
    }

    private TextView subLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(10.5f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(textMuted);
        tv.setPadding(0, 0, 0, dp(4));
        return tv;
    }

    private EditText themedInput() {
        EditText et = new EditText(this);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(inputBg);
        gd.setCornerRadius(dp(10));
        gd.setStroke(dp(1), inputBorder);
        et.setBackground(gd);
        et.setTextColor(textLight);
        et.setHintTextColor(textMuted);
        et.setTextSize(13.5f);
        et.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(6);
        et.setLayoutParams(lp);
        return et;
    }

    private TextView chip(String text, boolean marginEnd) {
        TextView chip = new TextView(this);
        chip.setText(text);
        chip.setTextSize(11.5f);
        chip.setTypeface(Typeface.DEFAULT_BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        if (marginEnd) lp.setMarginEnd(dp(6));
        chip.setLayoutParams(lp);
        return chip;
    }

    private TextView pillButton(String text) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setTextSize(12.5f);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(dp(16), dp(10), dp(16), dp(10));
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(0xFF4F46E5);
        gd.setCornerRadius(dp(24));
        btn.setBackground(gd);
        return btn;
    }

    private void styleToggle(TextView chip, boolean active) {
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(dp(18));
        if (active) {
            gd.setColor(0xFF4F46E5);
            chip.setTextColor(Color.WHITE);
        } else {
            gd.setColor(inputBg);
            gd.setStroke(dp(1), inputBorder);
            chip.setTextColor(textMuted);
        }
        chip.setBackground(gd);
    }

    private View gap(int dpVal) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(dpVal)));
        return v;
    }

    private int dp(int v) {
        return InvoicePdfHelper.dp(this, v);
    }

    private TextWatcher simpleWatcher(Runnable onChange) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { onChange.run(); }
        };
    }
}
