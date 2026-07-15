package com.jrappspot.cashlipi.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.models.Transaction;
import com.jrappspot.cashlipi.utils.DatabaseManager;

import java.util.List;

/**
 * "ছক" (টেবিল/গ্রিড) ভিউ — Card ভিউয়ের বদলে সারি-কলাম আকারে আয়/ব্যয় দেখায়।
 * প্রতিটি এন্ট্রির ক্যাটাগরি/উৎস যা আসলে সংরক্ষিত আছে তাই দেখানো হয় (Transaction.getDisplayTitle()),
 * তাই ক্যাটাগরি না থাকা এন্ট্রি ছাড়া আর কোনোটাই ভুলভাবে "অন্যান্য" দেখাবে না।
 */
public class TransactionTableAdapter extends RecyclerView.Adapter<TransactionTableAdapter.VH> {

    public interface OnItemClickListener {
        void onClick(Transaction item, int position);
    }

    private final Context ctx;
    private final List<Transaction> list;
    private final String type; // "income" | "expense" | "savings"
    private final OnItemClickListener clickListener;

    public TransactionTableAdapter(Context ctx, List<Transaction> list, String type,
                                    OnItemClickListener click) {
        this.ctx = ctx;
        this.list = list;
        this.type = type;
        this.clickListener = click;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_transaction_table_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Transaction t = list.get(position);

        // এন্ট্রির নিজস্ব ক্যাটাগরি/উৎস — খালি থাকলেই কেবল ডিফল্ট লেবেল দেখাবে
        String title = t.getDisplayTitle();
        h.tvTitle.setText(title);

        h.tvAmount.setText(DatabaseManager.formatAmount(t.getAmount()));
        h.tvDate.setText(DatabaseManager.formatDateDisplay(t.getDate()));
        h.tvTime.setText(DatabaseManager.formatTimeDisplay(t.getTime()));

        if (t.getNote() != null && !t.getNote().isEmpty()) {
            h.tvNote.setVisibility(View.VISIBLE);
            h.tvNote.setText(t.getNote());
        } else {
            h.tvNote.setVisibility(View.GONE);
        }

        int amountColor;
        switch (type) {
            case "expense":
                amountColor = ContextCompat.getColor(ctx, R.color.amountExpense);
                break;
            case "savings":
                amountColor = ContextCompat.getColor(ctx, R.color.savingsColor);
                break;
            default:
                amountColor = ContextCompat.getColor(ctx, R.color.amountIncome);
                break;
        }
        h.tvAmount.setTextColor(amountColor);

        // জেব্রা স্ট্রাইপিং — সারিগুলো আলাদা করে চেনার জন্য
        boolean even = position % 2 == 0;
        h.rowRoot.setBackgroundColor(even
                ? ContextCompat.getColor(ctx, R.color.cardWhite)
                : Color.parseColor("#F6F9FC"));

        h.rowRoot.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onClick(t, position);
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        View rowRoot;
        TextView tvTitle, tvAmount, tvDate, tvTime, tvNote;
        VH(@NonNull View v) {
            super(v);
            rowRoot  = v.findViewById(R.id.rowRoot);
            tvTitle  = v.findViewById(R.id.tvRowTitle);
            tvAmount = v.findViewById(R.id.tvRowAmount);
            tvDate   = v.findViewById(R.id.tvRowDate);
            tvTime   = v.findViewById(R.id.tvRowTime);
            tvNote   = v.findViewById(R.id.tvRowNote);
        }
    }
}
