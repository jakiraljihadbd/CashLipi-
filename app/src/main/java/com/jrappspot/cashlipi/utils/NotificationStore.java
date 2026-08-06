package com.jrappspot.cashlipi.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.jrappspot.cashlipi.models.AppNotification;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * NotificationStore — অ্যাডমিন অ্যাপ থেকে আসা নোটিফিকেশন/ঘোষণা/ফোর্স-আপডেট/নোটিশ
 * লোকালি (SharedPreferences-এ JSON আকারে) সংরক্ষণ করে, যাতে বেল আইকনে ট্যাপ করলে
 * আগের সব নোটিফিকেশন ইতিহাস হিসেবে দেখা যায় — শুধু একবারের পপআপেই সীমাবদ্ধ না থেকে।
 *
 * সর্বোচ্চ ৫০টা এন্ট্রি রাখা হয় (পুরনোগুলো স্বয়ংক্রিয়ভাবে মুছে যায়)।
 */
public class NotificationStore {

    private static final String PREFS = "app_notifications_store";
    private static final String KEY_LIST = "notif_list_json";
    private static final int MAX_ITEMS = 50;

    private NotificationStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /**
     * নতুন নোটিফিকেশন যোগ করে (একই title+body+type ইতিমধ্যে সবার উপরে থাকলে ডুপ্লিকেট এড়ানো হয়)।
     */
    public static synchronized void add(Context context, String type, String title,
                                         String body, String imageUrl) {
        if (title == null || title.trim().isEmpty()) return;

        List<AppNotification> list = getAll(context);

        // ডুপ্লিকেট গার্ড — একদম সাম্প্রতিক এন্ট্রির সাথে হুবহু মিললে আর যোগ হবে না
        if (!list.isEmpty()) {
            AppNotification top = list.get(0);
            if (type.equals(top.getType())
                    && title.equals(top.getTitle())
                    && (body == null ? "" : body).equals(top.getBody() == null ? "" : top.getBody())) {
                return;
            }
        }

        AppNotification n = new AppNotification(
                String.valueOf(System.currentTimeMillis()) + "_" + list.size(),
                type, title, body, imageUrl,
                System.currentTimeMillis(), false);

        list.add(0, n);
        if (list.size() > MAX_ITEMS) {
            list = new ArrayList<>(list.subList(0, MAX_ITEMS));
        }
        saveAll(context, list);
    }

    public static synchronized List<AppNotification> getAll(Context context) {
        List<AppNotification> result = new ArrayList<>();
        String json = prefs(context).getString(KEY_LIST, null);
        if (json == null || json.trim().isEmpty()) return result;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                AppNotification n = new AppNotification(
                        o.optString("id"),
                        o.optString("type", AppNotification.TYPE_NOTIFICATION),
                        o.optString("title"),
                        o.optString("body"),
                        o.optString("imageUrl", ""),
                        o.optLong("timestamp"),
                        o.optBoolean("read", false));
                result.add(n);
            }
        } catch (Exception ignored) {}

        Collections.sort(result, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        return result;
    }

    private static void saveAll(Context context, List<AppNotification> list) {
        JSONArray arr = new JSONArray();
        try {
            for (AppNotification n : list) {
                JSONObject o = new JSONObject();
                o.put("id", n.getId());
                o.put("type", n.getType());
                o.put("title", n.getTitle());
                o.put("body", n.getBody());
                o.put("imageUrl", n.getImageUrl() != null ? n.getImageUrl() : "");
                o.put("timestamp", n.getTimestamp());
                o.put("read", n.isRead());
                arr.put(o);
            }
        } catch (Exception ignored) {}
        prefs(context).edit().putString(KEY_LIST, arr.toString()).apply();
    }

    public static synchronized int getUnreadCount(Context context) {
        int count = 0;
        for (AppNotification n : getAll(context)) {
            if (!n.isRead()) count++;
        }
        return count;
    }

    public static synchronized void markAllRead(Context context) {
        List<AppNotification> list = getAll(context);
        for (AppNotification n : list) n.setRead(true);
        saveAll(context, list);
    }

    public static synchronized void clearAll(Context context) {
        prefs(context).edit().remove(KEY_LIST).apply();
    }
}
