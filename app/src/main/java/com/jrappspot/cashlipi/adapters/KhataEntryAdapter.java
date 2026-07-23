package com.jrappspot.cashlipi.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.models.KhataEntry;
import com.jrappspot.cashlipi.utils.DatabaseManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * একজন নির্দিষ্ট ব্যক্তির (KhataCustomerDetailActivity) দেনা-পাওনা তালিকা — তারিখ অনুযায়ী গ্রুপ করে
 * ("আজ" / "গতকাল" / আসল তারিখ হেডার) দেখায়, প্রতিটা এন্ট্রির পাশে সেই মুহূর্ত পর্যন্ত রানিং
 * ব্যালেন্স। আইটেমে ট্যাপ করলে বিদ্যমান TransactionSheetHelper.showKhataEntrySheet() (সম্পাদনা/
 * বিস্তারিত/শেয়ার/পরিশোধ/মুছুন) চালু হয় — বিদ্যমান সিস্টেম পুনর্ব্যবহার করা হয়েছে, নতুন করে
 * ডুপ্লিকেট মেনু বানানো হয়নি।
 */
public class KhataEntryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ROW = 1;

    /** একটা এন্ট্রি + সেই পর্যন্ত রানিং ব্যালেন্স — শুধু UI-এর জন্য, সংরক্ষিত হয় না। */
    public static class Row {
        public final KhataEntry entry;
        public final double balanceAfter; // ধনাত্মক = ব্যক্তি আপনাকে দেবে (পাওনা), ঋণাত্মক = আপনি তাকে দেবেন (দেনা)
        public Row(KhataEntry entry, double balanceAfter) {
            this.entry = entry;
            this.balanceAfter = balanceAfter;
        }
    }

    public interface OnRowClick { void onClick(KhataEntry entry); }

    // অভ্যন্তরীণ তালিকা: String (তারিখ হেডার) অথবা Row (লেনদেন) মিশিয়ে
    private final List<Object> items = new ArrayList<>();
    private final Context ctx;
    private final OnRowClick clickListener;
    private int lastAnimatedPosition = -1;

    /** rows অবশ্যই নতুন-থেকে-পুরনো (তারিখ অনুযায়ী descending) ক্রমে সাজানো থাকতে হবে। */
    public KhataEntryAdapter(Context ctx, List<Row> rows, OnRowClick clickListener) {
        this.ctx = ctx;
        this.clickListener = clickListener;
        buildItems(rows);
    }

    private void buildItems(List<Row> rows) {
        items.clear();
        String lastDate = null;
        for (Row r : rows) {
            String d = r.entry.getDate();
            if (d == null) d = "";
            if (!d.equals(lastDate)) {
                items.add(dateHeaderLabel(d));
                lastDate = d;
            }
            items.add(r);
        }
    }

    private String dateHeaderLabel(String isoDate) {
        try {
            SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            java.util.Date d = iso.parse(isoDate);
            Calendar target = Calendar.getInstance();
            target.setTime(d);

            Calendar today = Calendar.getInstance();
            Calendar yesterday = Calendar.getInstance();
            yesterday.add(Calendar.DAY_OF_YEAR, -1);

            if (sameDay(target, today)) return "আজ";
            if (sameDay(target, yesterday)) return "গতকাল";
            return DatabaseManager.formatDateDisplay(isoDate);
        } catch (Exception e) {
            return DatabaseManager.formatDateDisplay(isoDate);
        }
    }

    private boolean sameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_HEADER : TYPE_ROW;
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(ctx).inflate(R.layout.item_khata_entry_date_header, parent, false);
            return new HeaderVH(v);
        }
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_khata_entry_row, parent, false);
        return new RowVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);
        if (holder instanceof HeaderVH) {
            ((HeaderVH) holder).tv.setText((String) item);
            return;
        }
        bindRow((RowVH) holder, (Row) item, position);
    }

    private void bindRow(RowVH h, Row row, int position) {
        KhataEntry e = row.entry;
        boolean isDena = e.isBaki();

        h.tvRowType.setText(isDena ? " দেনা" : " পাওনা");
        h.tvRowType.setTextColor(ContextCompat.getColor(ctx, isDena ? R.color.denaColor : R.color.pabonaColor));

        h.ivRowIcon.setImageResource(isDena ? R.drawable.emoji_book_red : R.drawable.emoji_book_green);
        h.ivRowIcon.setBackgroundResource(isDena ? R.drawable.bg_icon_circle_ledger : R.drawable.bg_icon_circle_receivable);

        h.tvRowAmount.setText(DatabaseManager.formatAmount(e.getAmount()));
        h.tvRowAmount.setTextColor(ContextCompat.getColor(ctx, isDena ? R.color.denaColor : R.color.pabonaColor));

        h.tvRowDate.setText(DatabaseManager.formatTimeDisplay(e.getTime()));

        String note = "";
        if (e.getCategory() != null && !e.getCategory().isEmpty()) note = e.getCategory();
        if (e.getNote() != null && !e.getNote().isEmpty()) {
            note = note.isEmpty() ? e.getNote() : note + "  •  " + e.getNote();
        }
        if (!note.isEmpty()) {
            h.tvRowNote.setVisibility(View.VISIBLE);
            h.tvRowNote.setText(note);
        } else {
            h.tvRowNote.setVisibility(View.GONE);
        }

        if (e.isPaid()) {
            h.tvRowPaidBadge.setVisibility(View.VISIBLE);
            // পাওনা পরিশোধ হলে "পেলাম" (টাকা পাওয়া হয়েছে), দেনা পরিশোধ হলে "দিলাম" (টাকা দেওয়া হয়েছে) —
            // উপরের tvRowType-এর দেনা/পাওনা লেবেল অপরিবর্তিত থাকে, শুধু এই স্ট্যাটাস ব্যাজ পাল্টায়
            h.tvRowPaidBadge.setText(isDena ? " দিলাম" : " পেলাম");
            h.tvRowAmount.setPaintFlags(h.tvRowAmount.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            h.tvRowBalanceChip.setVisibility(View.GONE);
        } else {
            h.tvRowPaidBadge.setVisibility(View.GONE);
            h.tvRowAmount.setPaintFlags(h.tvRowAmount.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            h.tvRowBalanceChip.setVisibility(View.VISIBLE);

            double bal = row.balanceAfter;
            int chipColor, chipBg;
            String chipText;
            if (bal > 0.5) {
                chipText = "পাবেন " + DatabaseManager.formatAmount(bal);
                chipColor = ContextCompat.getColor(ctx, R.color.pabonaColor);
                chipBg = ContextCompat.getColor(ctx, R.color.pabonaLight);
            } else if (bal < -0.5) {
                chipText = "দেবেন " + DatabaseManager.formatAmount(Math.abs(bal));
                chipColor = ContextCompat.getColor(ctx, R.color.denaColor);
                chipBg = ContextCompat.getColor(ctx, R.color.denaLight);
            } else {
                chipText = "হিসাব সমান";
                chipColor = ContextCompat.getColor(ctx, R.color.textSecondary);
                chipBg = ContextCompat.getColor(ctx, R.color.dividerColor);
            }
            h.tvRowBalanceChip.setText(chipText);
            h.tvRowBalanceChip.setTextColor(chipColor);
            DrawableCompat.setTint(DrawableCompat.wrap(h.tvRowBalanceChip.getBackground().mutate()), chipBg);
        }

        h.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onClick(e);
        });

        if (position > lastAnimatedPosition) {
            h.itemView.startAnimation(AnimationUtils.loadAnimation(ctx, R.anim.item_anim));
            lastAnimatedPosition = position;
        } else {
            h.itemView.clearAnimation();
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView tv;
        HeaderVH(@NonNull View v) { super(v); tv = (TextView) v; }
    }

    static class RowVH extends RecyclerView.ViewHolder {
        ImageView ivRowIcon;
        TextView tvRowType, tvRowPaidBadge, tvRowNote, tvRowDate, tvRowAmount, tvRowBalanceChip;
        RowVH(@NonNull View v) {
            super(v);
            ivRowIcon        = v.findViewById(R.id.ivRowIcon);
            tvRowType        = v.findViewById(R.id.tvRowType);
            tvRowPaidBadge   = v.findViewById(R.id.tvRowPaidBadge);
            tvRowNote        = v.findViewById(R.id.tvRowNote);
            tvRowDate        = v.findViewById(R.id.tvRowDate);
            tvRowAmount      = v.findViewById(R.id.tvRowAmount);
            tvRowBalanceChip = v.findViewById(R.id.tvRowBalanceChip);
        }
    }
}
