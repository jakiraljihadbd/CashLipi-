package com.jrappspot.cashlipi.adapters;

import android.content.Context;
import androidx.core.content.ContextCompat;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.models.BackupRecord;
import com.jrappspot.cashlipi.utils.BackupManager;

import java.util.ArrayList;
import java.util.List;

/**
 * BackupHistoryAdapter — Displays backup history items in RecyclerView.
 */
public class BackupHistoryAdapter extends RecyclerView.Adapter<BackupHistoryAdapter.VH> {

    public interface OnItemClickListener {
        void onItemClick(BackupRecord record, int position);
    }

    private List<BackupRecord> list;
    private final Context context;
    private OnItemClickListener listener;

    public BackupHistoryAdapter(Context context) {
        this.context = context;
        this.list = new ArrayList<>();
    }

    public void setData(List<BackupRecord> data) {
        this.list = data != null ? data : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        this.listener = l;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
            .inflate(R.layout.item_backup_history, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        BackupRecord r = list.get(pos);

        // Date & Time
        h.tvDateTime.setText(BackupManager.formatDisplayDateTime(r.getDate(), r.getTime()));

        // Format badge
        h.tvFormat.setText(r.getFormatDisplay());

        // Method icon + name
        h.tvMethod.setText(r.getMethodIcon() + " " + r.getMethodDisplay());

        // File size
        h.tvSize.setText(r.getFileSizeDisplay());

        // Data type
        h.tvDataType.setText(r.getDataTypeDisplay());

        // Status badge
        if (r.isSuccess()) {
            h.tvStatus.setText(" সফল");
            h.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.backupSuccessText));
            h.cardStatus.setCardBackgroundColor(ContextCompat.getColor(context, R.color.backupSuccessBg));
        } else {
            h.tvStatus.setText(" ব্যর্থ");
            h.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.backupFailText));
            h.cardStatus.setCardBackgroundColor(ContextCompat.getColor(context, R.color.backupFailBg));
        }

        // Format badge color
        switch (r.getFormat() != null ? r.getFormat() : "json") {
            case "json": h.tvFormat.setBackgroundColor(ContextCompat.getColor(context, R.color.backupFormatJsonBg)); break;
            case "pdf":  h.tvFormat.setBackgroundColor(ContextCompat.getColor(context, R.color.backupFormatPdfBg)); break;
            case "docx": h.tvFormat.setBackgroundColor(ContextCompat.getColor(context, R.color.backupFormatJsonBg)); break;
            case "xlsx": h.tvFormat.setBackgroundColor(ContextCompat.getColor(context, R.color.backupFormatXlsxBg)); break;
        }

        // Click
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(r, pos);
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDateTime, tvFormat, tvMethod, tvSize, tvDataType, tvStatus;
        CardView cardStatus;

        VH(View v) {
            super(v);
            tvDateTime = v.findViewById(R.id.tvHistoryDateTime);
            tvFormat   = v.findViewById(R.id.tvHistoryFormat);
            tvMethod   = v.findViewById(R.id.tvHistoryMethod);
            tvSize     = v.findViewById(R.id.tvHistorySize);
            tvDataType = v.findViewById(R.id.tvHistoryDataType);
            tvStatus   = v.findViewById(R.id.tvHistoryStatus);
            cardStatus = v.findViewById(R.id.cardHistoryStatus);
        }
    }
}
