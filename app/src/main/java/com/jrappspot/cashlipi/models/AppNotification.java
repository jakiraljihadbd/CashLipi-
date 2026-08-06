package com.jrappspot.cashlipi.models;

/**
 * AppNotification — অ্যাডমিন অ্যাপ থেকে পাঠানো নোটিফিকেশন/ঘোষণা/ফোর্স-আপডেট/মেইনটেন্যান্স/নোটিশ
 * লোকালি সংরক্ষণের জন্য মডেল। বেল আইকনে ট্যাপ করলে এই লিস্টই দেখানো হয়।
 */
public class AppNotification {

    // টাইপ অনুযায়ী রঙ/আইকন ঠিক হয় (NotificationAdapter দেখুন)
    public static final String TYPE_UPDATE       = "update";       // 🚀 ফোর্স আপডেট
    public static final String TYPE_MAINTENANCE  = "maintenance";  // 🔧 রক্ষণাবেক্ষণ
    public static final String TYPE_NOTIFICATION = "notification"; // 🔔 সাধারণ নোটিফিকেশন
    public static final String TYPE_ANNOUNCEMENT = "announcement"; // 📣 ঘোষণা
    public static final String TYPE_NOTICE       = "notice";       // ℹ️ নোটিশ

    private String id;
    private String type;
    private String title;
    private String body;
    private String imageUrl;
    private long timestamp;
    private boolean read;

    public AppNotification() {}

    public AppNotification(String id, String type, String title, String body,
                            String imageUrl, long timestamp, boolean read) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.body = body;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
        this.read = read;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}
