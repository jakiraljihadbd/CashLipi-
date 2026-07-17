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
import com.jrappspot.cashlipi.models.Transaction;
import com.jrappspot.cashlipi.utils.DatabaseManager;

import java.util.List;

public class TransactionListAdapter extends RecyclerView.Adapter<TransactionListAdapter.VH> {

    public interface OnItemClickListener {
        void onClick(Transaction item, int position);
    }

    private final Context ctx;
    private final List<Transaction> list;
    private final String type; // "income" | "expense" | "savings"
    private final OnItemClickListener clickListener;
    private final OnItemClickListener longClickListener;
    private int lastAnimatedPosition = -1;

    public TransactionListAdapter(Context ctx, List<Transaction> list, String type,
                                   OnItemClickListener click, OnItemClickListener longClick) {
        this.ctx = ctx;
        this.list = list;
        this.type = type;
        this.clickListener = click;
        this.longClickListener = longClick;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_transaction, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Transaction t = list.get(position);

        // Title (as a colored category tag)
        String title = t.getDisplayTitle();
        if (title == null || title.trim().isEmpty()) {
            switch (type) {
                case "expense": title = " অন্যান্য খরচ"; break;
                case "savings": title = " সঞ্চয়"; break;
                default: title = " অন্যান্য আয়"; break;
            }
        }
        h.tvTitle.setText(title);

        // Amount
        h.tvAmount.setText(DatabaseManager.formatAmount(t.getAmount()));

        // Date & Time
        h.tvDate.setText(DatabaseManager.formatDateDisplay(t.getDate())
                + "  •  " + DatabaseManager.formatTimeDisplay(t.getTime()));

        // Note
        if (t.getNote() != null && !t.getNote().isEmpty()) {
            h.tvNote.setVisibility(View.VISIBLE);
            h.tvNote.setText(" " + t.getNote());
        } else {
            h.tvNote.setVisibility(View.GONE);
        }

        // Icon & colors
        switch (type) {
            case "income":
                h.tvIcon.setImageResource(R.drawable.emoji_money_bag);
                h.tvAmount.setTextColor(ContextCompat.getColor(ctx, R.color.amountIncome));
                h.tvIcon.setBackground(ctx.getResources().getDrawable(R.drawable.bg_icon_circle_income));
                h.tvTitle.setBackground(ctx.getResources().getDrawable(R.drawable.bg_tag_income));
                h.tvTitle.setTextColor(ContextCompat.getColor(ctx, R.color.incomeGradStart));
                if (h.viewColorBar != null) h.viewColorBar.setBackground(roundedBar(ContextCompat.getColor(ctx, R.color.amountIncome)));
                setEdgeBarColor(h, ContextCompat.getColor(ctx, R.color.amountIncome));
                break;
            case "expense":
                h.tvIcon.setImageResource(R.drawable.emoji_money_with_wings);
                h.tvAmount.setTextColor(ContextCompat.getColor(ctx, R.color.amountExpense));
                h.tvIcon.setBackground(ctx.getResources().getDrawable(R.drawable.bg_icon_circle_expense));
                h.tvTitle.setBackground(ctx.getResources().getDrawable(R.drawable.bg_tag_expense));
                h.tvTitle.setTextColor(ContextCompat.getColor(ctx, R.color.expenseGradStart));
                if (h.viewColorBar != null) h.viewColorBar.setBackground(roundedBar(ContextCompat.getColor(ctx, R.color.amountExpense)));
                setEdgeBarColor(h, ContextCompat.getColor(ctx, R.color.amountExpense));
                break;
            case "savings":
                h.tvIcon.setImageResource(R.drawable.emoji_green_heart);
                h.tvAmount.setTextColor(ContextCompat.getColor(ctx, R.color.savingsColor));
                h.tvIcon.setBackground(ctx.getResources().getDrawable(R.drawable.bg_icon_circle_savings));
                h.tvTitle.setBackground(ctx.getResources().getDrawable(R.drawable.bg_tag_savings));
                h.tvTitle.setTextColor(ContextCompat.getColor(ctx, R.color.savingsGradStart));
                if (h.viewColorBar != null) h.viewColorBar.setBackground(roundedBar(ContextCompat.getColor(ctx, R.color.savingsColor)));
                setEdgeBarColor(h, ContextCompat.getColor(ctx, R.color.savingsColor));
                break;
        }

        // Click -> opens action bottom sheet
        h.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onClick(t, position);
        });
        h.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) longClickListener.onClick(t, position);
            return true;
        });

        // Entrance animation (only animate forward, once per position)
        if (position > lastAnimatedPosition) {
            h.itemView.startAnimation(AnimationUtils.loadAnimation(ctx, R.anim.item_anim));
            lastAnimatedPosition = position;
        } else {
            h.itemView.clearAnimation();
        }
    }

    // বাম দাগ — দুই প্রান্ত হালকা বাঁকানো (pill shape)
    private android.graphics.drawable.GradientDrawable roundedBar(int color) {
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        gd.setColor(color);
        gd.setCornerRadius(8f * ctx.getResources().getDisplayMetrics().density);
        return gd;
    }

    // উপরে-নিচের দাগ — বাম দাগের একই রঙ, কিন্তু প্রায় অদৃশ্য (~১২% স্বচ্ছতা), দুই প্রান্ত বাঁকানো
    private void setEdgeBarColor(VH h, int color) {
        int lightColor = (31 << 24) | (color & 0x00FFFFFF); // 31/255 ≈ 12% alpha
        if (h.viewColorBarTop != null) h.viewColorBarTop.setBackground(roundedBar(lightColor));
        if (h.viewColorBarBottom != null) h.viewColorBarBottom.setBackground(roundedBar(lightColor));
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView tvIcon;
        TextView tvTitle, tvAmount, tvDate, tvNote;
        View viewColorBar, viewColorBarTop, viewColorBarBottom;
        VH(@NonNull View v) {
            super(v);
            tvIcon   = v.findViewById(R.id.tvItemIcon);
            tvTitle  = v.findViewById(R.id.tvItemTitle);
            tvAmount = v.findViewById(R.id.tvItemAmount);
            tvDate   = v.findViewById(R.id.tvItemDate);
            tvNote   = v.findViewById(R.id.tvItemNote);
            viewColorBarTop = v.findViewById(R.id.viewColorBarTop);
            viewColorBarBottom = v.findViewById(R.id.viewColorBarBottom);
            viewColorBar = v.findViewById(R.id.viewColorBar);
        }
    }
}
