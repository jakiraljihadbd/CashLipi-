package com.jrappspot.cashlipi.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.models.AppNotification;

import java.util.ArrayList;
import java.util.List;

/**
 * NotificationAdapter — অ্যাডমিন নোটিফিকেশন/ঘোষণা/ফোর্স-আপডেট/মেইনটেন্যান্স/নোটিশ
 * কালারফুল ফুল-উইডথ কার্ড আকারে দেখায়। টাইপ অনুযায়ী রং ও ইমোজি আলাদা।
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {

    public interface OnItemClickListener {
        void onItemClick(AppNotification item, int position);
    }

    private final Context context;
    private List<AppNotification> list = new ArrayList<>();
    private OnItemClickListener listener;

    public NotificationAdapter(Context context) {
        this.context = context;
    }

    public void setData(List<AppNotification> data) {
        this.list = data != null ? data : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        this.listener = l;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        AppNotification n = list.get(position);

        h.tvTitle.setText(n.getTitle());
        h.tvBody.setText(n.getBody() != null ? n.getBody() : "");
        h.tvTime.setText(relativeTime(n.getTimestamp()));
        h.dotUnread.setVisibility(n.isRead() ? View.GONE : View.VISIBLE);

        String type = n.getType() != null ? n.getType() : AppNotification.TYPE_NOTIFICATION;
        switch (type) {
            case AppNotification.TYPE_UPDATE:
                h.cardRoot.setBackgroundResource(R.drawable.bg_notif_update);
                h.tvIcon.setText("🚀");
                break;
            case AppNotification.TYPE_MAINTENANCE:
                h.cardRoot.setBackgroundResource(R.drawable.bg_notif_maintenance);
                h.tvIcon.setText("🔧");
                break;
            case AppNotification.TYPE_ANNOUNCEMENT:
                h.cardRoot.setBackgroundResource(R.drawable.bg_notif_announcement);
                h.tvIcon.setText("📣");
                break;
            case AppNotification.TYPE_NOTICE:
                h.cardRoot.setBackgroundResource(R.drawable.bg_notif_notice);
                h.tvIcon.setText("ℹ️");
                break;
            case AppNotification.TYPE_NOTIFICATION:
            default:
                h.cardRoot.setBackgroundResource(R.drawable.bg_notif_notification);
                h.tvIcon.setText("🔔");
                break;
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(n, h.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private String relativeTime(long timestamp) {
        if (timestamp <= 0) return "";
        long now = System.currentTimeMillis();
        if (timestamp > now) timestamp = now;
        CharSequence rel = DateUtils.getRelativeTimeSpanString(
                timestamp, now, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE);
        return rel.toString();
    }

    static class VH extends RecyclerView.ViewHolder {
        View cardRoot;
        TextView tvIcon, tvTitle, tvBody, tvTime;
        View dotUnread;

        VH(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.cardNotifRoot);
            tvIcon = itemView.findViewById(R.id.tvNotifIcon);
            tvTitle = itemView.findViewById(R.id.tvNotifTitle);
            tvBody = itemView.findViewById(R.id.tvNotifBody);
            tvTime = itemView.findViewById(R.id.tvNotifTime);
            dotUnread = itemView.findViewById(R.id.dotUnread);
        }
    }
}
