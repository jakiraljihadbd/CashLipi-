package com.jrappspot.cashlipi.adapters;

import androidx.core.content.ContextCompat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.models.KhataEntry;
import com.jrappspot.cashlipi.utils.DatabaseManager;

import java.util.List;

public class KhataEntryListAdapter extends RecyclerView.Adapter<KhataEntryListAdapter.VH> {

    public interface OnItemClickListener {
        void onClick(KhataEntry item, int position);
    }

    private final Context ctx;
    private final List<KhataEntry> list;
    private final OnItemClickListener clickListener;
    private int lastAnimatedPosition = -1;

    public KhataEntryListAdapter(Context ctx, List<KhataEntry> list, OnItemClickListener click) {
        this.ctx = ctx;
        this.list = list;
        this.clickListener = click;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_khata_entry, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        KhataEntry e = list.get(position);

        h.tvKhataCustomer.setText(e.getCustomerName());
        h.tvAmount.setText(DatabaseManager.formatAmount(e.getAmount()));
        h.tvDate.setText(DatabaseManager.formatDateDisplay(e.getDate())
                + "  •  " + DatabaseManager.formatTimeDisplay(e.getTime()));

        // Type badge & icon
        boolean isDena = e.isBaki();
        h.tvTypeBadge.setText(isDena ? " দেনা" : " পাওনা");
        h.tvTypeBadge.setTextColor(androidx.core.content.ContextCompat.getColor(ctx, isDena ? R.color.amountDebt : R.color.amountReceivable));
        h.tvIcon.setImageResource(isDena ? R.drawable.emoji_book_red : R.drawable.emoji_book_green);
        h.tvIcon.setBackground(ctx.getResources().getDrawable(
                isDena ? R.drawable.bg_icon_circle_ledger : R.drawable.bg_icon_circle_receivable));

        // Amount color
        h.tvAmount.setTextColor(androidx.core.content.ContextCompat.getColor(ctx, isDena ? R.color.amountDebt : R.color.amountReceivable));

        // Paid badge
        if (e.isPaid()) {
            h.tvPaidBadge.setVisibility(View.VISIBLE);
            h.tvPaidBadge.setText(" পরিশোধিত");
            h.tvPaidBadge.setBackground(ctx.getResources().getDrawable(R.drawable.bg_paid_badge));
            h.tvPaidBadge.setTextColor(ContextCompat.getColor(ctx, R.color.amountIncome));
            h.tvAmount.setPaintFlags(h.tvAmount.getPaintFlags()
                    | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            h.tvPaidBadge.setVisibility(View.VISIBLE);
            h.tvPaidBadge.setText(" বাকি");
            h.tvPaidBadge.setBackground(ctx.getResources().getDrawable(R.drawable.bg_unpaid_badge));
            h.tvPaidBadge.setTextColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.debtGradStart));
            h.tvAmount.setPaintFlags(h.tvAmount.getPaintFlags()
                    & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        }

        // Category / note
        String sub = "";
        if (e.getCategory() != null && !e.getCategory().isEmpty()) sub = " " + e.getCategory();
        if (e.getNote() != null && !e.getNote().isEmpty()) {
            sub = sub.isEmpty() ? " " + e.getNote() : sub + "  •  " + e.getNote();
        }
        if (!sub.isEmpty()) {
            h.tvNote.setVisibility(View.VISIBLE);
            h.tvNote.setText(sub);
        } else {
            h.tvNote.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onClick(e, position);
        });

        if (position > lastAnimatedPosition) {
            h.itemView.startAnimation(AnimationUtils.loadAnimation(ctx, R.anim.item_anim));
            lastAnimatedPosition = position;
        } else {
            h.itemView.clearAnimation();
        }
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView tvIcon;
        TextView tvKhataCustomer, tvAmount, tvDate, tvTypeBadge, tvPaidBadge, tvNote;
        VH(@NonNull View v) {
            super(v);
            tvIcon      = v.findViewById(R.id.tvKhataEntryIcon);
            tvKhataCustomer    = v.findViewById(R.id.tvKhataEntryKhataCustomer);
            tvAmount    = v.findViewById(R.id.tvKhataEntryAmount);
            tvDate      = v.findViewById(R.id.tvKhataEntryDate);
            tvTypeBadge = v.findViewById(R.id.tvKhataEntryType);
            tvPaidBadge = v.findViewById(R.id.tvKhataEntryPaid);
            tvNote      = v.findViewById(R.id.tvKhataEntryNote);
        }
    }
}
