package com.jrappspot.cashlipi.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.adapters.NotificationAdapter;
import com.jrappspot.cashlipi.models.AppNotification;
import com.jrappspot.cashlipi.utils.NotificationStore;

import java.util.List;

/**
 * NotificationsActivity — অ্যাডমিন অ্যাপ থেকে পাঠানো নোটিফিকেশন/ঘোষণা/ফোর্স-আপডেট/
 * মেইনটেন্যান্স/নোটিশের ইতিহাস দেখায়। ড্যাশবোর্ডের বেল আইকনে ট্যাপ করলে এটি খোলে।
 */
public class NotificationsActivity extends BaseActivity {

    private RecyclerView rvNotifications;
    private View emptyState;
    private NotificationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        findViewById(R.id.btnNotifBack).setOnClickListener(v -> finish());

        TextView btnMarkAllRead = findViewById(R.id.btnMarkAllRead);
        btnMarkAllRead.setOnClickListener(v -> {
            NotificationStore.markAllRead(this);
            loadList();
        });

        rvNotifications = findViewById(R.id.rvNotifications);
        emptyState = findViewById(R.id.emptyNotifState);

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(this);
        rvNotifications.setAdapter(adapter);

        loadList();

        // লিস্ট খোলা মাত্রই সব unread read হয়ে যাবে — বেল ব্যাজও পরের বার আপডেট হবে
        NotificationStore.markAllRead(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadList();
    }

    private void loadList() {
        List<AppNotification> list = NotificationStore.getAll(this);
        adapter.setData(list);

        boolean empty = list.isEmpty();
        rvNotifications.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
    }
}
