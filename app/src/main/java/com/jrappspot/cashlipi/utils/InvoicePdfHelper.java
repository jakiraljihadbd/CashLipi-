package com.jrappspot.cashlipi.utils;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
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
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.jrappspot.cashlipi.models.Transaction;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * আয়/ব্যয় পেজের জন্য কালারফুল, প্রফেশনাল ইনভয়েস-স্টাইল PDF তৈরি ও প্রিন্ট করার হেল্পার।
 * কোম্পানি নাম, তারিখ (ডিফল্ট আজ), অ্যাকসেন্ট কালার — সব কাস্টমাইজেবল।
 * স্বাক্ষরের জন্য নিচে একটা লাইন-সহ জায়গা রাখা থাকে।
 */
public class InvoicePdfHelper {

    private static final int[] PRESET_COLORS = {
            0xFF6366F1, // ইন্ডিগো
            0xFF10B981, // সবুজ
            0xFF2563EB, // নীল
            0xFFEF4444, // লাল
            0xFFF59E0B, // কমলা
            0xFFEC4899  // গোলাপি
    };

    // ── এক্সপোর্ট ডায়ালগ: কোম্পানি নাম + তারিখ + রঙ ────────────────────────
    public static void showExportDialog(Context ctx, String type, List<Transaction> list, boolean forPrint) {
        int pad = dp(ctx, 20);
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView lblCompany = label(ctx, "কোম্পানি/প্রতিষ্ঠানের নাম");
        EditText etCompany = new EditText(ctx);
        etCompany.setHint("CashLipi ক্যাশলিপি");
        etCompany.setInputType(InputType.TYPE_CLASS_TEXT);

        TextView lblDate = label(ctx, "তারিখ");
        EditText etDate = new EditText(ctx);
        String today = new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(new java.util.Date());
        etDate.setText(today); // ডিফল্ট আজকের তারিখ — না বদলালে এটাই থাকবে
        etDate.setFocusable(false);
        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(ctx, (view, y, m, d) ->
                    etDate.setText(String.format(Locale.US, "%02d/%02d/%04d", d, m + 1, y)),
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        TextView lblColor = label(ctx, "রঙ পছন্দ করুন");
        LinearLayout colorRow = new LinearLayout(ctx);
        colorRow.setOrientation(LinearLayout.HORIZONTAL);
        colorRow.setPadding(0, dp(ctx, 6), 0, 0);
        final int[] selectedColor = {"expense".equals(type) ? PRESET_COLORS[3] : PRESET_COLORS[1]};
        List<android.view.View> swatches = new ArrayList<>();
        for (int c : PRESET_COLORS) {
            android.view.View sw = new android.view.View(ctx);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(ctx, 36), dp(ctx, 36));
            lp.setMarginEnd(dp(ctx, 10));
            sw.setLayoutParams(lp);
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            gd.setColor(c);
            if (c == selectedColor[0]) gd.setStroke(dp(ctx, 3), Color.WHITE);
            sw.setBackground(gd);
            sw.setOnClickListener(v -> {
                selectedColor[0] = c;
                for (android.view.View s : swatches) ((GradientDrawable) s.getBackground()).setStroke(0, Color.TRANSPARENT);
                ((GradientDrawable) sw.getBackground()).setStroke(dp(ctx, 3), Color.WHITE);
            });
            swatches.add(sw);
            colorRow.addView(sw);
        }

        root.addView(lblCompany);
        root.addView(etCompany);
        root.addView(spacer(ctx));
        root.addView(lblDate);
        root.addView(etDate);
        root.addView(spacer(ctx));
        root.addView(lblColor);
        root.addView(colorRow);

        new AlertDialog.Builder(ctx)
                .setTitle(forPrint ? "প্রিন্ট" : "PDF এক্সপোর্ট")
                .setView(root)
                .setPositiveButton(forPrint ? "প্রিন্ট করুন" : "PDF তৈরি করুন", (d, w) -> {
                    String company = etCompany.getText().toString().trim();
                    if (company.isEmpty()) company = "CashLipi ক্যাশলিপি";
                    String date = etDate.getText().toString().trim();
                    if (date.isEmpty()) date = today; // তারিখ না দিলে আজকের তারিখ
                    byte[] pdf = generatePdf(ctx, type, list, company, date, selectedColor[0]);
                    if (pdf == null) {
                        Toast.makeText(ctx, "তৈরি ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (forPrint) printPdf(ctx, pdf, company);
                    else savePdf(ctx, pdf);
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    private static android.view.View spacer(Context ctx) {
        android.view.View v = new android.view.View(ctx);
        v.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(ctx, 14)));
        return v;
    }

    private static TextView label(Context ctx, String text) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextSize(12.5f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(0xFF6B7280);
        tv.setPadding(0, 0, 0, dp(ctx, 4));
        return tv;
    }

    private static int dp(Context ctx, int v) {
        return (int) (v * ctx.getResources().getDisplayMetrics().density);
    }

    // ── কালারফুল প্রফেশনাল ইনভয়েস PDF তৈরি ──────────────────────────────
    private static byte[] generatePdf(Context ctx, String type, List<Transaction> list,
                                       String company, String dateStr, int accentColor) {
        try {
            PdfDocument doc = new PdfDocument();
            int pageW = 595, pageH = 842, margin = 36;
            PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(pageW, pageH, 1).create();
            PdfDocument.Page page = doc.startPage(info);
            Canvas cv = page.getCanvas();

            boolean isIncome = !"expense".equals(type);
            String typeLabel = isIncome ? "আয়ের রিপোর্ট" : "ব্যয়ের রিপোর্ট";

            // ── রঙিন হেডার ব্যান্ড ──
            Paint headerBg = new Paint();
            headerBg.setColor(accentColor);
            cv.drawRect(new RectF(0, 0, pageW, 108), headerBg);

            Paint titlePaint = new Paint();
            titlePaint.setColor(Color.WHITE);
            titlePaint.setTypeface(Typeface.DEFAULT_BOLD);
            titlePaint.setTextSize(21f);
            titlePaint.setAntiAlias(true);
            cv.drawText(company, margin, 44, titlePaint);

            Paint subPaint = new Paint();
            subPaint.setColor(0xE6FFFFFF);
            subPaint.setTypeface(Typeface.DEFAULT_BOLD);
            subPaint.setTextSize(13f);
            subPaint.setAntiAlias(true);
            cv.drawText(typeLabel, margin, 67, subPaint);

            Paint dateHeadPaint = new Paint(subPaint);
            dateHeadPaint.setTypeface(Typeface.DEFAULT);
            dateHeadPaint.setTextSize(11f);
            dateHeadPaint.setTextAlign(Paint.Align.RIGHT);
            cv.drawText("তারিখ: " + dateStr, pageW - margin, 44, dateHeadPaint);
            cv.drawText("CashLipi অ্যাপ দ্বারা প্রস্তুত", pageW - margin, 67, dateHeadPaint);

            int y = 138;

            // ── সামারি বক্স ──
            double total = 0;
            for (Transaction t : list) total += t.getAmount();

            Paint boxBg = new Paint();
            boxBg.setColor(lighten(accentColor, 0.90f));
            boxBg.setAntiAlias(true);
            RectF box = new RectF(margin, y, pageW - margin, y + 55);
            cv.drawRoundRect(box, 10, 10, boxBg);

            Paint boxLabel = new Paint();
            boxLabel.setColor(0xFF6B7280);
            boxLabel.setTextSize(11f);
            cv.drawText(isIncome ? "মোট আয়" : "মোট ব্যয়", margin + 16, y + 22, boxLabel);

            Paint boxAmount = new Paint();
            boxAmount.setColor(accentColor);
            boxAmount.setTypeface(Typeface.DEFAULT_BOLD);
            boxAmount.setTextSize(21f);
            boxAmount.setAntiAlias(true);
            cv.drawText(DatabaseManager.formatAmount(total), margin + 16, y + 45, boxAmount);

            Paint boxCount = new Paint();
            boxCount.setColor(0xFF6B7280);
            boxCount.setTextSize(11f);
            boxCount.setTextAlign(Paint.Align.RIGHT);
            cv.drawText(list.size() + " টি এন্ট্রি", pageW - margin - 16, y + 32, boxCount);

            y += 80;

            // ── টেবিল হেডার (রঙিন) ──
            Paint tableHeaderBg = new Paint();
            tableHeaderBg.setColor(accentColor);
            tableHeaderBg.setAntiAlias(true);
            cv.drawRoundRect(new RectF(margin, y, pageW - margin, y + 26), 6, 6, tableHeaderBg);
            cv.drawRect(margin, y + 13, pageW - margin, y + 26, tableHeaderBg);

            Paint thText = new Paint();
            thText.setColor(Color.WHITE);
            thText.setTypeface(Typeface.DEFAULT_BOLD);
            thText.setTextSize(11f);
            cv.drawText("তারিখ", margin + 12, y + 17, thText);
            cv.drawText("বিবরণ", margin + 110, y + 17, thText);
            thText.setTextAlign(Paint.Align.RIGHT);
            cv.drawText("পরিমাণ", pageW - margin - 12, y + 17, thText);

            y += 26;

            // ── ডেটা রো (অল্টারনেটিং ব্যাকগ্রাউন্ড) ──
            Paint rowAlt = new Paint();
            rowAlt.setColor(lighten(accentColor, 0.95f));
            Paint rowText = new Paint();
            rowText.setColor(0xFF1F2937);
            rowText.setTextSize(10.5f);
            Paint amtText = new Paint();
            amtText.setColor(accentColor);
            amtText.setTypeface(Typeface.DEFAULT_BOLD);
            amtText.setTextSize(11f);
            amtText.setTextAlign(Paint.Align.RIGHT);

            List<Transaction> sorted = new ArrayList<>(list);
            sorted.sort((a, b) -> {
                String da = a.getDate() != null ? a.getDate() : "";
                String dbv = b.getDate() != null ? b.getDate() : "";
                return dbv.compareTo(da);
            });

            int rowH = 24;
            int i = 0;
            for (Transaction t : sorted) {
                if (y + rowH > pageH - 130) break; // স্বাক্ষরের জায়গা রাখা
                if (i % 2 == 1) cv.drawRect(margin, y, pageW - margin, y + rowH, rowAlt);
                rowText.setTextAlign(Paint.Align.LEFT);
                cv.drawText(DatabaseManager.formatDateDisplay(t.getDate()), margin + 12, y + 16, rowText);
                String title = t.getDisplayTitle();
                if (title == null || title.trim().isEmpty()) title = isIncome ? "অন্যান্য আয়" : "অন্যান্য খরচ";
                if (title.length() > 32) title = title.substring(0, 29) + "...";
                cv.drawText(title, margin + 110, y + 16, rowText);
                cv.drawText(DatabaseManager.formatAmount(t.getAmount()), pageW - margin - 12, y + 16, amtText);
                y += rowH;
                i++;
            }

            // ── স্বাক্ষরের জায়গা (ফুটার) ──
            int sigY = pageH - 90;
            Paint linePaint = new Paint();
            linePaint.setColor(0xFFD1D5DB);
            linePaint.setStrokeWidth(1.2f);
            cv.drawLine(pageW - margin - 180, sigY, pageW - margin, sigY, linePaint);
            Paint sigLabel = new Paint();
            sigLabel.setColor(0xFF6B7280);
            sigLabel.setTextSize(10.5f);
            sigLabel.setTextAlign(Paint.Align.CENTER);
            cv.drawText("স্বাক্ষর / Signature", pageW - margin - 90, sigY + 16, sigLabel);

            Paint footerNote = new Paint();
            footerNote.setColor(0xFF9CA3AF);
            footerNote.setTextSize(9f);
            footerNote.setTextAlign(Paint.Align.LEFT);
            cv.drawText(company + " — " + dateStr + " তারিখে তৈরি", margin, pageH - 20, footerNote);

            doc.finishPage(page);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.writeTo(baos);
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
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
