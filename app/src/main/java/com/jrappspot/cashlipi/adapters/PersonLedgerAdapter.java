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
import com.jrappspot.cashlipi.models.LedgerEntry;
import com.jrappspot.cashlipi.utils.DatabaseManager;

import java.util.List;

/**
 * একজন নির্দিষ্ট ব্যক্তির (PersonDetailActivity) দেনা-পাওনা তালিকা দেখায় — প্রতিটা এন্ট্রির
 * পাশে সেই মুহূর্ত পর্যন্ত রানিং ব্যালেন্স (কে কাকে কত পাবে) একটা ছোট চিপে দেখানো হয়।
 * আইটেমে ট্যাপ করলে বিদ্যমান TransactionSheetHelper.showLedgerSheet() (সম্পাদনা/বিস্তারিত/
 * শেয়ার/পরিশোধ/মুছুন) চালু হয় — নতুন করে কিছু বানানো হয়নি, বিদ্যমান সিস্টেম পুনর্ব্যবহার করা হয়েছে।
 */
public class PersonLedgerAdapter extends RecyclerView.Adapter<PersonLedgerAdapter.VH> {

    /** একটা row-এর জন্য এন্ট্রি + সেই পর্যন্ত রানিং ব্যালেন্স — শুধু UI-এর জন্য, সংরক্ষিত হয় না। */
    public static class Row {
        public final LedgerEntry entry;
        public final double balanceAfter; // ধনাত্মক = ব্যক্তি আপনাকে দেবে (পাওনা), ঋণাত্মক = আপনি তাকে দেবেন (দেনা)
        public Row(LedgerEntry entry, double balanceAfter) {
            this.entry = entry;
            this.balanceAfter = balanceAfter;
        }
    }

    public interface OnRowClick { void onClick(LedgerEntry entry); }

    private final Context ctx;
    private final List<Row> rows;
    private final OnRowClick clickListener;
    private int lastAnimatedPosition = -1;

    public PersonLedgerAdapter(Context ctx, List<Row> rows, OnRowClick clickListener) {
        this.ctx = ctx;
        this.rows = rows;
        this.clickListener = clickListener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_person_ledger, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Row row = rows.get(position);
        LedgerEntry e = row.entry;
        boolean isDena = e.isDena();

        // দেনা এন্ট্রি (আমি দেব) থেকে হয়েছে "দিলাম" বাটন, পাওনা এন্ট্রি (আমি পাব) থেকে হয়েছে "পেলাম" বাটন
        h.tvRowType.setText(isDena ? " দিলাম" : " পেলাম");
        h.tvRowType.setTextColor(ContextCompat.getColor(ctx, isDena ? R.color.denaColor : R.color.pabonaColor));

        h.ivRowIcon.setImageResource(isDena ? R.drawable.emoji_book_red : R.drawable.emoji_book_green);
        h.ivRowIcon.setBackground(ctx.getResources().getDrawable(
                isDena ? R.drawable.bg_icon_circle_ledger : R.drawable.bg_icon_circle_receivable));

        h.tvRowAmount.setText(DatabaseManager.formatAmount(e.getAmount()));
        h.tvRowAmount.setTextColor(ContextCompat.getColor(ctx, isDena ? R.color.denaColor : R.color.pabonaColor));

        h.tvRowDate.setText(DatabaseManager.formatDateDisplay(e.getDate())
                + "  •  " + DatabaseManager.formatTimeDisplay(e.getTime()));

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
            h.tvRowAmount.setPaintFlags(h.tvRowAmount.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            // পরিশোধিত এন্ট্রি রানিং হিসাবে যোগ হয় না, তাই আলাদা চিপ না দেখিয়ে ✓ পরিশোধিত badge-ই যথেষ্ট
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
    public int getItemCount() { return rows.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivRowIcon;
        TextView tvRowType, tvRowPaidBadge, tvRowNote, tvRowDate, tvRowAmount, tvRowBalanceChip;
        VH(@NonNull View v) {
            super(v);
            ivRowIcon       = v.findViewById(R.id.ivRowIcon);
            tvRowType       = v.findViewById(R.id.tvRowType);
            tvRowPaidBadge  = v.findViewById(R.id.tvRowPaidBadge);
            tvRowNote       = v.findViewById(R.id.tvRowNote);
            tvRowDate       = v.findViewById(R.id.tvRowDate);
            tvRowAmount     = v.findViewById(R.id.tvRowAmount);
            tvRowBalanceChip = v.findViewById(R.id.tvRowBalanceChip);
        }
    }
}
