package com.jrappspot.cashlipi.adapters;

import androidx.core.content.ContextCompat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.models.LedgerEntry;
import com.jrappspot.cashlipi.models.Transaction;
import com.jrappspot.cashlipi.utils.DatabaseManager;

import java.util.List;

public class RecentTransactionAdapter extends RecyclerView.Adapter<RecentTransactionAdapter.VH> {

    private final Context ctx;
    private final List<Object> list;

    public RecentTransactionAdapter(Context ctx, List<Object> list) {
        this.ctx  = ctx;
        this.list = list;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_transaction, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Object item = list.get(position);

        if (item instanceof Transaction) {
            Transaction t = (Transaction) item;
            h.tvTitle.setText(t.getDisplayTitle());
            h.tvAmount.setText(DatabaseManager.formatAmount(t.getAmount()));
            h.tvDate.setText(DatabaseManager.formatDateDisplay(t.getDate()));
            if (t.getNote() != null && !t.getNote().isEmpty()) {
                h.tvNote.setVisibility(View.VISIBLE);
                h.tvNote.setText(t.getNote());
            } else {
                h.tvNote.setVisibility(View.GONE);
            }
            switch (t.getType()) {
                case "income":
                    
                    h.tvAmount.setTextColor(ContextCompat.getColor(ctx, R.color.incomeColor));
                    h.itemView.setBackground(ctx.getResources().getDrawable(R.drawable.bg_list_item_income));
                    break;
                case "expense":
                    
                    h.tvAmount.setTextColor(ContextCompat.getColor(ctx, R.color.expenseColor));
                    h.itemView.setBackground(ctx.getResources().getDrawable(R.drawable.bg_list_item_expense));
                    break;
                case "savings":
                    
                    h.tvAmount.setTextColor(ContextCompat.getColor(ctx, R.color.savingsColor));
                    h.itemView.setBackground(ctx.getResources().getDrawable(R.drawable.bg_list_item_savings));
                    break;
            }
        } else if (item instanceof LedgerEntry) {
            LedgerEntry e = (LedgerEntry) item;
            h.tvTitle.setText(e.getPerson());
            h.tvAmount.setText(DatabaseManager.formatAmount(e.getAmount()));
            h.tvDate.setText(DatabaseManager.formatDateDisplay(e.getDate()));
            h.tvNote.setVisibility(View.VISIBLE);
            h.tvNote.setText(e.isDena() ? " দেনা" : " পাওনা");
            h.tvIcon.setImageResource(e.isDena() ? R.drawable.emoji_money_with_wings : R.drawable.emoji_money_bag);
            h.tvAmount.setTextColor(androidx.core.content.ContextCompat.getColor(ctx, e.isDena() ? R.color.denaColor : R.color.pabonaColor));
            h.itemView.setBackground(ctx.getResources().getDrawable(R.drawable.bg_list_item_ledger));
        }
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView tvIcon;
        TextView tvTitle, tvAmount, tvDate, tvNote;
        VH(@NonNull View v) {
            super(v);
            tvIcon   = v.findViewById(R.id.tvItemIcon);
            tvTitle  = v.findViewById(R.id.tvItemTitle);
            tvAmount = v.findViewById(R.id.tvItemAmount);
            tvDate   = v.findViewById(R.id.tvItemDate);
            tvNote   = v.findViewById(R.id.tvItemNote);
        }
    }
}
