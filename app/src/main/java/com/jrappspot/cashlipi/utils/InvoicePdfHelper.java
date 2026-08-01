package com.jrappspot.cashlipi.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.jrappspot.cashlipi.models.Transaction;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * আয়/ব্যয় পেজের জন্য কালারফুল, প্রফেশনাল ইনভয়েস-স্টাইল PDF তৈরি ও প্রিন্ট করার হেল্পার।
 * PDF ড্রইং রুটিন (drawPage/generatePdf/renderPreviewBitmap), সেভ ও প্রিন্ট লজিক এখানে থাকে;
 * ফুল-পেজ এক্সপোর্ট স্ক্রিন (কোম্পানি নাম, টেমপ্লেট, তারিখ-রেঞ্জ, রঙ, স্বাক্ষর, লাইভ প্রিভিউ)
 * {@link com.jrappspot.cashlipi.activities.InvoiceExportActivity}-তে থাকে।
 */
public class InvoicePdfHelper {

    public static final int[] PRESET_COLORS = {
            0xFF6366F1, // ইন্ডিগো
            0xFF10B981, // সবুজ
            0xFF2563EB, // নীল
            0xFFEF4444, // লাল
            0xFFF59E0B, // কমলা
            0xFFEC4899  // গোলাপি
    };

    // টেমপ্লেট: ০ = সাধারণ, ১ = লেটারহেড (শিরোনাম/সাক্ষর-প্যাড স্টাইল), ২ = কম্প্যাক্ট (শুধু নাম, ছোট হেডার)
    public static final String[] TEMPLATE_NAMES = {"সাধারণ", "লেটারহেড", "কম্প্যাক্ট"};

    public static final int PAGE_W = 595, PAGE_H = 842, MARGIN = 36;

    // ── আয়-ব্যয় লিস্ট পেজ থেকে ফুল-পেজ InvoiceExportActivity-তে লিস্ট হ্যান্ডঅফ করার ব্রিজ ──
    // Transaction Parcelable/Serializable নয়, তাই Intent-এ না পাঠিয়ে একই প্রসেসে ইন-মেমরি
    // স্ট্যাটিক হোল্ডারে রাখা হয় — Activity শুরু হয়েই এটা নিয়ে নেয় এবং সাথে সাথে ক্লিয়ার করে দেয়।
    private static List<Transaction> pendingList;

    public static List<Transaction> takePendingList() {
        List<Transaction> l = pendingList;
        pendingList = null;
        return l;
    }

    // ── এক্সপোর্ট: আয়-ব্যয় লিস্ট পেজ থেকে কল হয় — এখন ফুল-পেজ Activity খোলে (পপ-আপ নয়) ──────
    public static void showExportDialog(Context ctx, String type, List<Transaction> list, boolean forPrint) {
        pendingList = list;
        Intent i = new Intent(ctx, com.jrappspot.cashlipi.activities.InvoiceExportActivity.class);
        i.putExtra(com.jrappspot.cashlipi.activities.InvoiceExportActivity.EXTRA_TYPE, type);
        i.putExtra(com.jrappspot.cashlipi.activities.InvoiceExportActivity.EXTRA_FOR_PRINT, forPrint);
        if (!(ctx instanceof android.app.Activity)) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(i);
    }

    public static void styleChip(TextView chip, boolean active) {
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(chip.getResources().getDisplayMetrics().density * 8);
        if (active) {
            gd.setColor(0xFF4F46E5);
            chip.setTextColor(Color.WHITE);
        } else {
            gd.setColor(0xFFF1F5F9);
            chip.setTextColor(0xFF475569);
        }
        chip.setBackground(gd);
    }

    public static android.view.View spacer(Context ctx) {
        android.view.View v = new android.view.View(ctx);
        v.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(ctx, 14)));
        return v;
    }

    public static TextView label(Context ctx, String text) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextSize(12.5f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(0xFF6B7280);
        tv.setPadding(0, 0, 0, dp(ctx, 4));
        return tv;
    }

    public static int dp(Context ctx, int v) {
        return (int) (v * ctx.getResources().getDisplayMetrics().density);
    }

    // ── আয়-ব্যয় তালিকা কাস্টম তারিখ-রেঞ্জ (শুরু–শেষ, yyyy-MM-dd ফরম্যাট, উভয়ই ইনক্লুসিভ) দিয়ে ফিল্টার ──
    // startYmd/endYmd null বা খালি হলে পুরো লিস্টই ব্যবহৃত হবে (আগের আচরণ অপরিবর্তিত)।
    public static List<Transaction> filterByDateRange(List<Transaction> list, String startYmd, String endYmd) {
        if ((startYmd == null || startYmd.isEmpty()) && (endYmd == null || endYmd.isEmpty())) return list;
        List<Transaction> out = new ArrayList<>();
        for (Transaction t : list) {
            String d = t.getDate();
            if (d == null || d.isEmpty()) continue;
            if (startYmd != null && !startYmd.isEmpty() && d.compareTo(startYmd) < 0) continue;
            if (endYmd != null && !endYmd.isEmpty() && d.compareTo(endYmd) > 0) continue;
            out.add(t);
        }
        return out;
    }

    // ── লাইভ প্রিভিউ বিটম্যাপ — একই drawPage() রুটিন ছোট স্কেলে আঁকে (PDF-এর সাথে ১০০% মিল) ──
    public static Bitmap renderPreviewBitmap(Context ctx, String type, List<Transaction> list,
                                              String company, String dateStr, int accentColor, int template,
                                              Bitmap signatureBitmap, String signatureText) {
        int bmpW = dp(ctx, 200), bmpH = dp(ctx, 283);
        Bitmap bmp = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        cv.drawColor(Color.WHITE);
        cv.save();
        cv.scale(bmpW / (float) PAGE_W, bmpH / (float) PAGE_H);
        drawPage(cv, type, list, company, dateStr, accentColor, template, signatureBitmap, signatureText);
        cv.restore();
        return bmp;
    }

    // ── হাই-রেজ্যুলেশন বিটম্যাপ — ফুলস্ক্রিন জুম প্রিভিউ ডায়ালগের জন্য (একই drawPage() রুটিন,
    //    ৩ গুণ স্কেলে আঁকা হয় বলে জুম করলেও ঝাপসা হয় না, ভেক্টর-শার্প থাকে) ──
    public static Bitmap renderHiResBitmap(Context ctx, String type, List<Transaction> list,
                                            String company, String dateStr, int accentColor, int template,
                                            Bitmap signatureBitmap, String signatureText) {
        int scaleFactor = 3;
        int bmpW = PAGE_W * scaleFactor, bmpH = PAGE_H * scaleFactor;
        Bitmap bmp = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        cv.drawColor(Color.WHITE);
        cv.save();
        cv.scale(scaleFactor, scaleFactor);
        drawPage(cv, type, list, company, dateStr, accentColor, template, signatureBitmap, signatureText);
        cv.restore();
        return bmp;
    }

    // ── কালারফুল প্রফেশনাল ইনভয়েস PDF তৈরি ──────────────────────────────
    public static byte[] generatePdf(String type, List<Transaction> list,
                                      String company, String dateStr, int accentColor, int template,
                                      Bitmap signatureBitmap, String signatureText) {
        try {
            PdfDocument doc = new PdfDocument();
            PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create();
            PdfDocument.Page page = doc.startPage(info);
            Canvas cv = page.getCanvas();
            drawPage(cv, type, list, company, dateStr, accentColor, template, signatureBitmap, signatureText);
            doc.finishPage(page);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.writeTo(baos);
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    // ── PDF সেভ ও প্রিন্ট করার পাবলিক এন্ট্রি — Activity থেকে সরাসরি কল হয় ──
    public static void savePdfPublic(Context ctx, byte[] pdf) { savePdf(ctx, pdf); }

    public static void printPdfPublic(Context ctx, byte[] pdfBytes, String jobName) { printPdf(ctx, pdfBytes, jobName); }

    // ── মূল ড্রইং রুটিন — PDF পেজ ও লাইভ প্রিভিউ দুটোতেই এই একই ফাংশন ব্যবহার হয়, তাই প্রিভিউ = আসল ফলাফল ──
    // signatureBitmap দেওয়া থাকলে (হাতে আঁকা) সেটাই আঁকা হয়; না থাকলে signatureText (টাইপ করা নাম) থাকলে
    // সেটা ইটালিক/বোল্ড ফন্টে "সইয়ের মত" করে আঁকা হয়; দুটোর একটাও না থাকলে আগের মতোই ফাঁকা লাইন থাকবে।
    private static void drawPage(Canvas cv, String type, List<Transaction> list,
                                  String company, String dateStr, int accentColor, int template,
                                  Bitmap signatureBitmap, String signatureText) {
        int pageW = PAGE_W, pageH = PAGE_H, margin = MARGIN;
        boolean isIncome = !"expense".equals(type);
        String typeLabel = isIncome ? "আয়ের রিপোর্ট" : "ব্যয়ের রিপোর্ট";
        boolean letterhead = template == 1;   // শিরোনাম/সাক্ষর-প্যাড স্টাইল — কেন্দ্রে বড় নাম + ট্যাগলাইন
        boolean compact = template == 2;       // শুধু নাম — ছোট হেডার, বেশি রো ধরে

        int headerH = letterhead ? 132 : (compact ? 82 : 108);

        // ── রঙিন হেডার ব্যান্ড ──
        Paint headerBg = new Paint();
        headerBg.setColor(accentColor);
        headerBg.setAntiAlias(true);
        cv.drawRect(new RectF(0, 0, pageW, headerH), headerBg);

        if (letterhead) {
            // কেন্দ্রে বড় শিরোনাম + ট্যাগলাইন + নিচে ডাবল-রুল বর্ডার — চিঠির প্যাডের ধাঁচে
            Paint titlePaint = new Paint();
            titlePaint.setColor(Color.WHITE);
            titlePaint.setTypeface(Typeface.DEFAULT_BOLD);
            titlePaint.setTextSize(24f);
            titlePaint.setAntiAlias(true);
            titlePaint.setTextAlign(Paint.Align.CENTER);
            cv.drawText(company, pageW / 2f, 52, titlePaint);

            Paint tagPaint = new Paint();
            tagPaint.setColor(0xE6FFFFFF);
            tagPaint.setTypeface(Typeface.DEFAULT);
            tagPaint.setTextSize(12.5f);
            tagPaint.setAntiAlias(true);
            tagPaint.setTextAlign(Paint.Align.CENTER);
            cv.drawText(typeLabel + "  •  তারিখ: " + dateStr, pageW / 2f, 74, tagPaint);

            Paint rule = new Paint();
            rule.setColor(0x99FFFFFF);
            rule.setStrokeWidth(1.4f);
            cv.drawLine(margin, headerH - 14, pageW - margin, headerH - 14, rule);
            rule.setStrokeWidth(3f);
            cv.drawLine(margin, headerH - 8, pageW - margin, headerH - 8, rule);
        } else {
            Paint titlePaint = new Paint();
            titlePaint.setColor(Color.WHITE);
            titlePaint.setTypeface(Typeface.DEFAULT_BOLD);
            titlePaint.setTextSize(compact ? 17f : 21f);
            titlePaint.setAntiAlias(true);
            cv.drawText(company, margin, compact ? 34 : 44, titlePaint);

            Paint subPaint = new Paint();
            subPaint.setColor(0xE6FFFFFF);
            subPaint.setTypeface(Typeface.DEFAULT_BOLD);
            subPaint.setTextSize(compact ? 11f : 13f);
            subPaint.setAntiAlias(true);
            cv.drawText(typeLabel, margin, compact ? 54 : 67, subPaint);

            Paint dateHeadPaint = new Paint(subPaint);
            dateHeadPaint.setTypeface(Typeface.DEFAULT);
            dateHeadPaint.setTextSize(11f);
            dateHeadPaint.setTextAlign(Paint.Align.RIGHT);
            cv.drawText("তারিখ: " + dateStr, pageW - margin, compact ? 34 : 44, dateHeadPaint);
            if (!compact) cv.drawText("CashLipi অ্যাপ দ্বারা প্রস্তুত", pageW - margin, 67, dateHeadPaint);
        }

        int y = headerH + 30;

        // ── সামারি বক্স ──
        double total = 0;
        for (Transaction t : list) total += t.getAmount();

        int boxH = compact ? 40 : 55;
        Paint boxBg = new Paint();
        boxBg.setColor(lighten(accentColor, 0.90f));
        boxBg.setAntiAlias(true);
        RectF box = new RectF(margin, y, pageW - margin, y + boxH);
        cv.drawRoundRect(box, 10, 10, boxBg);

        Paint boxLabel = new Paint();
        boxLabel.setColor(0xFF6B7280);
        boxLabel.setTextSize(compact ? 9.5f : 11f);
        cv.drawText(isIncome ? "মোট আয়" : "মোট ব্যয়", margin + 16, y + (compact ? 16 : 22), boxLabel);

        Paint boxAmount = new Paint();
        boxAmount.setColor(accentColor);
        boxAmount.setTypeface(Typeface.DEFAULT_BOLD);
        boxAmount.setTextSize(compact ? 16f : 21f);
        boxAmount.setAntiAlias(true);
        cv.drawText(DatabaseManager.formatAmount(total), margin + 16, y + (compact ? 33 : 45), boxAmount);

        Paint boxCount = new Paint();
        boxCount.setColor(0xFF6B7280);
        boxCount.setTextSize(compact ? 9.5f : 11f);
        boxCount.setTextAlign(Paint.Align.RIGHT);
        cv.drawText(list.size() + " টি এন্ট্রি", pageW - margin - 16, y + boxH / 2f + 4, boxCount);

        y += boxH + (compact ? 16 : 25);

        // ── টেবিল হেডার (রঙিন) ──
        int thH = compact ? 20 : 26;
        Paint tableHeaderBg = new Paint();
        tableHeaderBg.setColor(accentColor);
        tableHeaderBg.setAntiAlias(true);
        cv.drawRoundRect(new RectF(margin, y, pageW - margin, y + thH), 6, 6, tableHeaderBg);
        cv.drawRect(margin, y + thH / 2f, pageW - margin, y + thH, tableHeaderBg);

        Paint thText = new Paint();
        thText.setColor(Color.WHITE);
        thText.setTypeface(Typeface.DEFAULT_BOLD);
        thText.setTextSize(compact ? 9.5f : 11f);
        cv.drawText("তারিখ", margin + 12, y + thH - 9, thText);
        cv.drawText("বিবরণ", margin + 110, y + thH - 9, thText);
        thText.setTextAlign(Paint.Align.RIGHT);
        cv.drawText("পরিমাণ", pageW - margin - 12, y + thH - 9, thText);

        y += thH;

        // ── ডেটা রো (অল্টারনেটিং ব্যাকগ্রাউন্ড) ──
        Paint rowAlt = new Paint();
        rowAlt.setColor(lighten(accentColor, 0.95f));
        Paint rowText = new Paint();
        rowText.setColor(0xFF1F2937);
        rowText.setTextSize(compact ? 9f : 10.5f);
        Paint amtText = new Paint();
        amtText.setColor(accentColor);
        amtText.setTypeface(Typeface.DEFAULT_BOLD);
        amtText.setTextSize(compact ? 9.5f : 11f);
        amtText.setTextAlign(Paint.Align.RIGHT);

        List<Transaction> sorted = new ArrayList<>(list);
        sorted.sort((a, b) -> {
            String da = a.getDate() != null ? a.getDate() : "";
            String dbv = b.getDate() != null ? b.getDate() : "";
            return dbv.compareTo(da);
        });

        int rowH = compact ? 19 : 24;
        int i = 0;
        int footerReserve = letterhead ? 140 : (compact ? 70 : 130);
        for (Transaction t : sorted) {
            if (y + rowH > pageH - footerReserve) break; // স্বাক্ষরের জায়গা রাখা
            if (i % 2 == 1) cv.drawRect(margin, y, pageW - margin, y + rowH, rowAlt);
            rowText.setTextAlign(Paint.Align.LEFT);
            cv.drawText(DatabaseManager.formatDateDisplay(t.getDate()), margin + 12, y + rowH - 8, rowText);
            String title = t.getDisplayTitle();
            if (title == null || title.trim().isEmpty()) title = isIncome ? "অন্যান্য আয়" : "অন্যান্য খরচ";
            if (title.length() > 32) title = title.substring(0, 29) + "...";
            cv.drawText(title, margin + 110, y + rowH - 8, rowText);
            cv.drawText(DatabaseManager.formatAmount(t.getAmount()), pageW - margin - 12, y + rowH - 8, amtText);
            y += rowH;
            i++;
        }

        // ── স্বাক্ষরের জায়গা (ফুটার) — লেটারহেড টেমপ্লেটে "সিলমোহর"-সহ ──
        int sigY = pageH - (compact ? 50 : 90);
        int sigLineStart = pageW - margin - 180, sigLineEnd = pageW - margin;

        if (signatureBitmap != null) {
            // ── হাতে আঁকা স্বাক্ষর — লাইনের ঠিক ওপরে বসানো, আকার-অনুপাত ঠিক রেখে ──
            int boxW = sigLineEnd - sigLineStart - 10, boxH = compact ? 26 : 36;
            float scale = Math.min(boxW / (float) signatureBitmap.getWidth(), boxH / (float) signatureBitmap.getHeight());
            int drawW = (int) (signatureBitmap.getWidth() * scale), drawH = (int) (signatureBitmap.getHeight() * scale);
            RectF dst = new RectF(sigLineStart + (boxW - drawW) / 2f + 5, sigY - drawH - 4,
                    sigLineStart + (boxW - drawW) / 2f + 5 + drawW, sigY - 4);
            cv.drawBitmap(signatureBitmap, null, dst, null);
        } else if (signatureText != null && !signatureText.trim().isEmpty()) {
            // ── টাইপ করা স্বাক্ষর — ইটালিক-বোল্ড ফন্টে, কলমে লেখার মত ──
            Paint typedSig = new Paint();
            typedSig.setAntiAlias(true);
            typedSig.setColor(0xFF1A1A2E);
            typedSig.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC));
            typedSig.setTextSize(compact ? 15f : 18f);
            typedSig.setTextAlign(Paint.Align.CENTER);
            cv.drawText(signatureText.trim(), (sigLineStart + sigLineEnd) / 2f, sigY - 8, typedSig);
        }

        Paint linePaint = new Paint();
        linePaint.setColor(0xFFD1D5DB);
        linePaint.setStrokeWidth(1.2f);
        cv.drawLine(sigLineStart, sigY, sigLineEnd, sigY, linePaint);
        Paint sigLabel = new Paint();
        sigLabel.setColor(0xFF6B7280);
        sigLabel.setTextSize(10.5f);
        sigLabel.setTextAlign(Paint.Align.CENTER);
        cv.drawText(letterhead ? "স্বাক্ষর ও সিলমোহর / Signature & Seal" : "স্বাক্ষর / Signature",
                pageW - margin - 90, sigY + 16, sigLabel);

        Paint footerNote = new Paint();
        footerNote.setColor(0xFF9CA3AF);
        footerNote.setTextSize(9f);
        footerNote.setTextAlign(Paint.Align.LEFT);
        cv.drawText(company + " — " + dateStr + " তারিখে তৈরি", margin, pageH - 20, footerNote);
    }

    private static int lighten(int color, float amount) {
        int r = (int) (((color >> 16) & 0xFF) + (255 - ((color >> 16) & 0xFF)) * amount);
        int g = (int) (((color >> 8) & 0xFF) + (255 - ((color >> 8) & 0xFF)) * amount);
        int b = (int) ((color & 0xFF) + (255 - (color & 0xFF)) * amount);
        return Color.rgb(Math.min(r, 255), Math.min(g, 255), Math.min(b, 255));
    }

    // ── PDF সেভ ও খোলা (parmission ছাড়া কাজ করে — MediaStore/app storage ব্যবহার করে) ──
    private static void savePdf(Context ctx, byte[] pdf) {
        try {
            String fname = "CashLipi_Report_" + System.currentTimeMillis() + ".pdf";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues cv = new ContentValues();
                cv.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fname);
                cv.put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/pdf");
                cv.put(android.provider.MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/CashLipi");
                Uri uri = ctx.getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                if (uri != null) {
                    try (OutputStream os = ctx.getContentResolver().openOutputStream(uri)) {
                        os.write(pdf);
                    }
                    Toast.makeText(ctx, "PDF সেভ হয়েছে: Downloads/CashLipi/" + fname, Toast.LENGTH_LONG).show();
                    openPdf(ctx, uri);
                    return;
                }
            }
            // পুরনো Android — অ্যাপের নিজস্ব ফোল্ডারে (permission ছাড়াই কাজ করে)
            File dir = new File(ctx.getExternalFilesDir(null), "CashLipi_Reports");
            dir.mkdirs();
            File f = new File(dir, fname);
            try (FileOutputStream fos = new FileOutputStream(f)) {
                fos.write(pdf);
            }
            Uri uri = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".provider", f);
            Toast.makeText(ctx, "PDF সেভ হয়েছে", Toast.LENGTH_LONG).show();
            openPdf(ctx, uri);
        } catch (Exception e) {
            Toast.makeText(ctx, "PDF সেভ ব্যর্থ: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private static void openPdf(Context ctx, Uri uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(intent);
        } catch (Exception ignored) {
            Toast.makeText(ctx, "PDF ভিউয়ার অ্যাপ পাওয়া যায়নি", Toast.LENGTH_SHORT).show();
        }
    }

    // ── প্রিন্ট (Android এর বিল্ট-ইন প্রিন্ট সার্ভিস ব্যবহার করে) ──────────
    private static void printPdf(Context ctx, byte[] pdfBytes, String jobName) {
        PrintManager printManager = (PrintManager) ctx.getSystemService(Context.PRINT_SERVICE);
        if (printManager == null) {
            Toast.makeText(ctx, "প্রিন্ট সার্ভিস পাওয়া যায়নি", Toast.LENGTH_SHORT).show();
            return;
        }
        PrintDocumentAdapter adapter = new PrintDocumentAdapter() {
            @Override
            public void onLayout(PrintAttributes oldAttrs, PrintAttributes newAttrs,
                                  CancellationSignal cancellationSignal, LayoutResultCallback callback, Bundle extras) {
                if (cancellationSignal.isCanceled()) {
                    callback.onLayoutCancelled();
                    return;
                }
                PrintDocumentInfo docInfo = new PrintDocumentInfo.Builder(jobName + ".pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(1)
                        .build();
                callback.onLayoutFinished(docInfo, true);
            }

            @Override
            public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                                 CancellationSignal cancellationSignal, WriteResultCallback callback) {
                try (OutputStream out = new FileOutputStream(destination.getFileDescriptor())) {
                    out.write(pdfBytes);
                    callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
                } catch (Exception e) {
                    callback.onWriteFailed(e.getMessage());
                }
            }
        };
        printManager.print(jobName + " - CashLipi", adapter, new PrintAttributes.Builder().build());
    }
}
