package com.jrappspot.cashlipi.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.models.Transaction;
import com.jrappspot.cashlipi.utils.DatabaseManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * আয়-ব্যয় পেজের কার্ড ভিউয়ের জন্য ডেডিকেটেড অ্যাডাপ্টার — item_transaction_card.xml ব্যবহার করে।
 * সম্পাদনা / মুছুন / আরও (⋯) — এই তিনটার জন্য আলাদা ক্লিক কলব্যাক।
 */
public class IncomeExpenseCardAdapter extends RecyclerView.Adapter<IncomeExpenseCardAdapter.VH> {

    public interface OnAction { void run(Transaction item, int position); }

    private final Context ctx;
    private final List<Transaction> list;
    private final String type; // "income" | "expense"
    private final OnAction onEdit, onDelete, onMore;

    public IncomeExpenseCardAdapter(Context ctx, List<Transaction> list, String type,
                                     OnAction onEdit, OnAction onDelete, OnAction onMore) {
        this.ctx = ctx;
        this.list = list;
        this.type = type;
        this.onEdit = onEdit;
        this.onDelete = onDelete;
        this.onMore = onMore;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_transaction_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Transaction t = list.get(position);
        boolean isIncome = "income".equals(type);

        h.tvTitle.setText(t.getDisplayTitle());
        h.tvAmount.setText(DatabaseManager.formatAmount(t.getAmount()));
        h.tvAmount.setTextColor(ContextCompat.getColor(ctx, isIncome ? R.color.ieIncomeText : R.color.ieExpenseText));
        h.viewColorBar.setBackgroundColor(ContextCompat.getColor(ctx, isIncome ? R.color.ieIncomeDark : R.color.ieExpenseDark));

        h.tvDate.setText(DatabaseManager.formatDateDisplay(t.getDate()));
        h.tvTime.setText(DatabaseManager.formatTimeDisplay(t.getTime()));

        if (t.getNote() != null && !t.getNote().trim().isEmpty()) {
            h.rowNote.setVisibility(View.VISIBLE);
            h.tvNote.setText(t.getNote());
        } else {
            h.rowNote.setVisibility(View.GONE);
        }

        h.tvRelative.setText(relativeDaysText(t.getDate()));

        h.btnEdit.setOnClickListener(v -> onEdit.run(t, h.getBindingAdapterPosition()));
        h.btnDelete.setOnClickListener(v -> onDelete.run(t, h.getBindingAdapterPosition()));
        h.btnMore.setOnClickListener(v -> onMore.run(t, h.getBindingAdapterPosition()));
    }

    private String relativeDaysText(String dateStr) {
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date d = fmt.parse(dateStr);
            Date today = fmt.parse(fmt.format(new Date()));
            if (d == null) return "";
            long diffMs = today.getTime() - d.getTime();
            long days = TimeUnit.MILLISECONDS.toDays(diffMs);
            if (days <= 0) return "আজ";
            if (days == 1) return "১ দিন আগে";
            return days + " দিন আগে";
        } catch (ParseException e) {
            return "";
        }
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        View viewColorBar;
        TextView tvTitle, tvAmount, tvDate, tvTime, tvNote, tvRelative;
        LinearLayout rowNote, btnEdit, btnDelete;
        TextView btnMore;
        VH(@NonNull View v) {
            super(v);
            viewColorBar = v.findViewById(R.id.viewColorBar);
            tvTitle = v.findViewById(R.id.tvItemTitle);
            tvAmount = v.findViewById(R.id.tvItemAmount);
            tvDate = v.findViewById(R.id.tvItemDate);
            tvTime = v.findViewById(R.id.tvItemTime);
            rowNote = v.findViewById(R.id.rowNote);
            tvNote = v.findViewById(R.id.tvItemNote);
            tvRelative = v.findViewById(R.id.tvItemRelative);
            btnEdit = v.findViewById(R.id.btnEdit);
            btnDelete = v.findViewById(R.id.btnDelete);
            btnMore = v.findViewById(R.id.btnMore);
        }
    }
}
