package com.jrappspot.cashlipi.utils;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.SetOptions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * 🔥 FirestoreSyncManager
 *
 * Google Sign-In করলে ডাটা Firebase Firestore-এ auto save হবে।
 * একই Gmail দিয়ে login করলে ডাটা ফিরে আসবে।
 *
 * Firestore Structure:
 * users/{userEmail}/
 *   ├── profile/
 *   │     └── info  → { name, email, photoUrl, lastSync }
 *   └── backup/
 *         └── data  → { jsonBackup, timestamp, version }
 */
public class FirestoreSyncManager {

    private static final String TAG = "FirestoreSyncManager";
    private static FirestoreSyncManager instance;

    private final FirebaseFirestore db;
    private final DatabaseManager databaseManager;
    private final Context context;

    // Firestore collection/document paths
    private static final String COLLECTION_USERS  = "users";
    private static final String DOC_PROFILE       = "profile";
    private static final String DOC_BACKUP        = "backup";
    private static final String FIELD_DATA        = "data";
    private static final String FIELD_TIMESTAMP   = "timestamp";
    private static final String FIELD_VERSION     = "version";
    private static final String FIELD_EMAIL       = "email";

    public interface SyncCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    private FirestoreSyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.databaseManager = DatabaseManager.getInstance(context);

        // Firestore initialize
        db = FirebaseFirestore.getInstance();

        // Offline persistence চালু করুন (internet না থাকলেও কাজ করবে)
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        db.setFirestoreSettings(settings);
    }

    public static synchronized FirestoreSyncManager getInstance(Context context) {
        if (instance == null) {
            instance = new FirestoreSyncManager(context);
        }
        return instance;
    }

    // ─────────────────────────────────────────────
    //  🔼 UPLOAD: Local → Firestore
    //  Google sign-in এর পরে এই method call করুন
    // ─────────────────────────────────────────────

    /**
     * সমস্ত ডাটা Firestore-এ save করুন।
     * DashboardActivity এবং যেকোনো data add/edit এর পরে call করুন।
     *
     * 🔑 Key strategy: Email/Phone দিয়ে login করা থাকলে (Firebase Auth session
     * আছে) সেই uid দিয়ে save হয় — কারণ বেশিরভাগ Firestore security rule
     * request.auth.uid == path-এর uid চেক করে, আর phone/email কে path এ
     * ব্যবহার করলে সেটা auth.uid এর সাথে না মেলায় write silently ব্যর্থ হচ্ছিল।
     * Auth session না থাকলে (পুরোনো Google Sign-In flow) আগের মতো email
     * key ব্যবহার হবে।
     */
    public void uploadAllData(SyncCallback callback) {
        ensureAuthSession(() -> doUploadAllData(callback));
    }

    private void doUploadAllData(SyncCallback callback) {
        String syncKey = getSyncKey();
        if (syncKey == null || syncKey.isEmpty()) {
            if (callback != null) callback.onFailure("Login করা নেই");
            return;
        }

        // JSON backup generate করুন (DatabaseManager এর existing method)
        String jsonBackup = databaseManager.exportToJson();
        if (jsonBackup == null || jsonBackup.isEmpty()) {
            if (callback != null) callback.onFailure("ডাটা export করতে পারেনি");
            return;
        }

        String email = databaseManager.getGoogleEmail();
        String timestamp = DatabaseManager.nowIso();

        // ── backup/data document ──
        Map<String, Object> backupData = new HashMap<>();
        backupData.put(FIELD_DATA, jsonBackup);
        backupData.put(FIELD_TIMESTAMP, timestamp);
        backupData.put(FIELD_VERSION, getAppVersionCode());
        backupData.put(FIELD_EMAIL, email);

        db.collection(COLLECTION_USERS)
                .document(syncKey)
                .collection("backup")
                .document("data")
                .set(backupData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Firestore upload সফল: " + syncKey);
                    // Profile info ও save করুন (admin panel visibility অক্ষুণ্ণ রাখতে email key দিয়েও)
                    uploadProfileInfo(syncKey, email);
                    if (callback != null) callback.onSuccess("Firebase-এ ডাটা সেভ হয়েছে ✅");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Firestore upload ব্যর্থ (key=" + syncKey + "): " + e.getMessage());
                    if (callback != null) {
                        callback.onFailure("সেভ হয়নি: " + e.getMessage());
                    } else {
                        // caller callback দেয়নি (auto-save চলছিল), তাই error silently হারিয়ে
                        // না যাক — অন্তত একটা Toast দেখাও যাতে debug করা যায়
                        String errMsg = e.getMessage() != null ? e.getMessage() : "অজানা সমস্যা";
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            android.widget.Toast.makeText(context, "⚠️ Cloud sync ব্যর্থ: " + errMsg, android.widget.Toast.LENGTH_LONG).show());
                    }
                });
    }

    /**
     * 🔧 SESSION SELF-HEAL: এই fix আসার আগে যারা Google দিয়ে login করেছিল,
     * তাদের FirebaseAuth session কখনও তৈরি হয়নি (শুধু GoogleSignInClient
     * দিয়ে sign-in হয়েছিল)। তাদের logout/login করতে বলার বদলে, প্রতিটা
     * sync attempt-এর আগে এখানে চুপচাপ session check ও repair হয়ে যায়।
     *
     * - FirebaseAuth session আগে থেকেই থাকলে সরাসরি এগিয়ে যায়।
     * - না থাকলে কিন্তু Google account cache করা থাকলে silent re-auth
     *   করে session তৈরি করে দেয় (কোনো popup/UI ছাড়াই)।
     * - Google account-ই না থাকলে (email/phone user) যেমন ছিল তেমন এগিয়ে যায়।
     */
    private void ensureAuthSession(Runnable action) {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            action.run();
            return;
        }
        com.google.android.gms.auth.api.signin.GoogleSignInAccount cached =
                com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context);
        if (cached == null) {
            // Google দিয়ে login করা ছিল না — email/phone flow, যেমন ছিল তেমন এগিয়ে যাও
            action.run();
            return;
        }
        GoogleSignInHelper.silentReauth(context, new GoogleSignInHelper.SignInCallback() {
            @Override
            public void onSuccess(String name, String email, String photoUrl) {
                Log.d(TAG, "🔧 FirebaseAuth session silently repaired");
                action.run();
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "🔧 Session repair ব্যর্থ, পুরোনো fallback দিয়ে এগোনো হচ্ছে: " + error);
                action.run();
            }
        });
    }

    private void uploadProfileInfo(String syncKey, String email) {
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("name", databaseManager.getGoogleName());
        profileData.put("email", email);
        profileData.put("photoUrl", databaseManager.getGooglePhotoUrl());
        profileData.put("lastSync", DatabaseManager.nowIso());
        profileData.put("appVersion", getAppVersionCode());
        profileData.put("lastActive", System.currentTimeMillis());

        // sync হচ্ছে যে key (uid বা email) দিয়ে, সেখানে profile merge করো
        // (blocked field স্পর্শ করছি না, যাতে admin panel এর blocked flag মুছে না যায়)
        db.collection(COLLECTION_USERS)
                .document(syncKey)
                .collection(DOC_PROFILE)
                .document("info")
                .set(profileData, SetOptions.merge())
                .addOnSuccessListener(v -> Log.d(TAG, "Profile info saved"))
                .addOnFailureListener(e -> Log.e(TAG, "Profile save failed: " + e.getMessage()));

        db.collection(COLLECTION_USERS)
                .document(syncKey)
                .set(profileData, SetOptions.merge())
                .addOnSuccessListener(v -> Log.d(TAG, "Admin-visible profile saved (key=" + syncKey + ")"))
                .addOnFailureListener(e -> Log.e(TAG, "Admin profile save failed: " + e.getMessage()));

        // ── আগে এখানে email-key দিয়ে একটা "legacy" duplicate top-level ডকুমেন্টও
        // লেখা হতো backward-compatibility এর জন্য — কিন্তু এতে admin panel-এ
        // একই user দুইবার (uid-key + email-key) দেখাচ্ছিল। uid-key ই এখন
        // primary/একমাত্র source of truth, তাই legacy write বাদ দেওয়া হলো।
    }

    // ─────────────────────────────────────────────
    //  🟢 Presence: online/offline + lastActive
    //  BaseActivity onResume()/onPause() থেকে call হয়
    // ─────────────────────────────────────────────

    /**
     * App সামনে এলে online=true, background/close হলে online=false।
     * Admin panel-এ live listener দিয়ে এটা সাথে সাথে reflect হয়।
     */
    public void updateOnlineStatus(boolean online) {
        String syncKey = getSyncKey();
        if (syncKey == null || syncKey.isEmpty()) return;

        Map<String, Object> data = new HashMap<>();
        data.put("online", online);
        data.put("lastActive", System.currentTimeMillis());

        db.collection(COLLECTION_USERS)
                .document(syncKey)
                .set(data, SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "Presence update ব্যর্থ: " + e.getMessage()));
    }

    /**
     * Device name/id একবার সেভ করলেই যথেষ্ট — বারবার লেখার দরকার নেই,
     * তাই SharedPreferences flag দিয়ে "already sent" চেক করা হয়।
     */
    public void saveDeviceInfoIfNeeded() {
        String syncKey = getSyncKey();
        if (syncKey == null || syncKey.isEmpty()) return;

        android.content.SharedPreferences prefs =
                context.getSharedPreferences("device_info_prefs", Context.MODE_PRIVATE);
        String savedForKey = prefs.getString("device_info_sent_for", "");
        if (savedForKey.equals(syncKey)) return; // এই syncKey এর জন্য আগেই পাঠানো হয়েছে

        String deviceName = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
        String deviceId = android.provider.Settings.Secure.getString(
                context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        Map<String, Object> data = new HashMap<>();
        data.put("deviceName", deviceName);
        data.put("deviceId", deviceId);

        db.collection(COLLECTION_USERS)
                .document(syncKey)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v -> {
                    prefs.edit().putString("device_info_sent_for", syncKey).apply();
                    Log.d(TAG, "Device info saved");
                })
                .addOnFailureListener(e -> Log.e(TAG, "Device info save ব্যর্থ: " + e.getMessage()));

        // IP address আলাদা network call, background thread এ
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("https://api.ipify.org");
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(url.openStream()));
                String ip = reader.readLine();
                reader.close();
                if (ip != null && !ip.isEmpty()) {
                    db.collection(COLLECTION_USERS)
                            .document(syncKey)
                            .update("ipAddress", ip);
                }
            } catch (Exception e) {
                Log.e(TAG, "IP fetch ব্যর্থ: " + e.getMessage());
            }
        }).start();
    }

    // ─────────────────────────────────────────────
    //  🔽 DOWNLOAD: Firestore → Local
    //  Login এর পরে ডাটা restore করতে call করুন
    // ─────────────────────────────────────────────

    /**
     * Login করার পরে Firestore থেকে ডাটা ফিরিয়ে আনুন।
     * uid-key তে ডাটা না পাওয়া গেলে (পুরোনো email-key তে সেভ হওয়া ডাটার
     * জন্য) সেটাও একবার চেক করা হয়।
     */
    public void downloadAndRestore(SyncCallback callback) {
        ensureAuthSession(() -> doDownloadAndRestore(callback));
    }

    private void doDownloadAndRestore(SyncCallback callback) {
        String syncKey = getSyncKey();
        if (syncKey == null || syncKey.isEmpty()) {
            if (callback != null) callback.onFailure("Login তথ্য পাওয়া যায়নি");
            return;
        }

        String email = databaseManager.getGoogleEmail();
        String legacyKey = (email != null && !email.isEmpty()) ? emailToKey(email) : null;
        boolean hasLegacyFallback = legacyKey != null && !legacyKey.equals(syncKey);

        db.collection(COLLECTION_USERS)
                .document(syncKey)
                .collection("backup")
                .document("data")
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        applyBackupDoc(document, syncKey, callback);
                    } else if (hasLegacyFallback) {
                        // এই uid-key তে ডাটা নেই — পুরোনো email-key তে একবার চেক করো
                        fetchBackupDoc(legacyKey, callback);
                    } else {
                        Log.d(TAG, "নতুন user, Firebase-এ ডাটা নেই।");
                        if (callback != null) callback.onSuccess("নতুন account — ডাটা সংরক্ষণ শুরু হবে ✅");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Download ব্যর্থ (key=" + syncKey + "): " + e.getMessage());
                    if (hasLegacyFallback) {
                        fetchBackupDoc(legacyKey, callback);
                    } else if (callback != null) {
                        callback.onFailure("Firebase থেকে ডাটা আনতে পারেনি: " + e.getMessage());
                    }
                });
    }

    private void fetchBackupDoc(String key, SyncCallback callback) {
        db.collection(COLLECTION_USERS)
                .document(key)
                .collection("backup")
                .document("data")
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        applyBackupDoc(document, key, callback);
                    } else {
                        Log.d(TAG, "নতুন user অথবা এই key তে ডাটা নেই: " + key);
                        if (callback != null) callback.onSuccess("নতুন account — ডাটা সংরক্ষণ শুরু হবে ✅");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Download ব্যর্থ (key=" + key + "): " + e.getMessage());
                    if (callback != null) callback.onFailure("Firebase থেকে ডাটা আনতে পারেনি: " + e.getMessage());
                });
    }

    private void applyBackupDoc(com.google.firebase.firestore.DocumentSnapshot document, String key, SyncCallback callback) {
        String jsonBackup = document.getString(FIELD_DATA);
        String timestamp  = document.getString(FIELD_TIMESTAMP);

        if (jsonBackup != null && !jsonBackup.isEmpty()) {
            boolean success = databaseManager.importFromJson(jsonBackup);
            if (success) {
                Log.d(TAG, "✅ Restore সফল (key=" + key + ")। Timestamp: " + timestamp);
                if (callback != null)
                    callback.onSuccess("ডাটা ফিরে এসেছে ✅\nশেষ sync: " + formatTimestamp(timestamp));
            } else {
                if (callback != null) callback.onFailure("Import করতে সমস্যা হয়েছে");
            }
        } else {
            if (callback != null) callback.onFailure("Firebase-এ কোনো ডাটা নেই");
        }
    }

    /**
     * 🔑 sync-এর জন্য primary Firestore document key নির্ধারণ করুন।
     * Firebase Auth session থাকলে (Email/Phone login) uid ব্যবহার হয় —
     * এটাই বেশিরভাগ security rules এর সাথে মেলে। Auth session না থাকলে
     * (পুরোনো Google Sign-In flow, যেখানে real FirebaseAuth session তৈরি
     * হয় না) email/phone থেকে তৈরি key ব্যবহার হয়।
     */
    private String getSyncKey() {
        com.google.firebase.auth.FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            return currentUser.getUid();
        }
        String email = databaseManager.getGoogleEmail();
        if (email == null || email.isEmpty()) return null;
        return emailToKey(email);
    }

    // ─────────────────────────────────────────────
    //  🔎 Login: Password ভুল নাকি Account-ই নেই সেটা যাচাই
    // ─────────────────────────────────────────────

    public interface AccountCheckCallback {
        void onResult(boolean registered);
        void onError(String error);
    }

    /**
     * Login ব্যর্থ হলে এই method দিয়ে যাচাই করুন এই Email/Phone দিয়ে
     * আদৌ কোনো অ্যাকাউন্ট Firestore-এ registered আছে কিনা।
     * থাকলে → Password ভুল popup, না থাকলে → User Not Registered popup।
     *
     * নোট: এই query unauthenticated অবস্থায় চলে, তাই Firestore security
     * rules-এ "users" collection-এর "email"/"phone" field read করার
     * অনুমতি (list/query) থাকতে হবে। অনুমতি না থাকলে onError() আসবে —
     * সেক্ষেত্রে কলিং কোড generic message দেখাবে।
     */
    public void checkAccountRegistered(String identifier, boolean isEmail, AccountCheckCallback callback) {
        String field = isEmail ? "email" : "phone";
        db.collection(COLLECTION_USERS)
                .whereEqualTo(field, identifier)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (callback != null) callback.onResult(!querySnapshot.isEmpty());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Account check ব্যর্থ: " + e.getMessage());
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    // ─────────────────────────────────────────────
    //  👤 Admin: User info read করতে
    // ─────────────────────────────────────────────

    /**
     * Admin panel থেকে ব্যবহারকারীর তথ্য দেখুন।
     * Firestore → users collection এর সব document list করুন।
     */
    public void getAllUsersForAdmin(AdminCallback callback) {
        db.collection(COLLECTION_USERS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    java.util.List<Map<String, Object>> userList = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        // প্রতিটি user এর profile লোড করুন
                        doc.getReference()
                                .collection(DOC_PROFILE)
                                .document("info")
                                .get()
                                .addOnSuccessListener(profileDoc -> {
                                    if (profileDoc.exists()) {
                                        userList.add(profileDoc.getData());
                                    }
                                });
                    }
                    if (callback != null) callback.onResult(userList);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public interface AdminCallback {
        void onResult(java.util.List<Map<String, Object>> users);
        void onError(String error);
    }

    // ─────────────────────────────────────────────
    //  🔔 Admin: Notification পাঠানো
    //  Admin app Firestore-এ notification document লিখবে
    //  User app সেটা listen করবে
    // ─────────────────────────────────────────────

    /**
     * User app এ এই listener চালু করুন AppLock/Dashboard onCreate()-এ।
     * Admin যখন notification পাঠাবে, এখানে আসবে।
     */
    /**
     * User app এ এই listener চালু করুন AppLock/Dashboard onCreate()-এ।
     * Admin যখন notification পাঠাবে, এখানে আসবে।
     *
     * 🔁 একই notification যাতে app খোলার প্রতিবার popup না হয় (আগে এই bug ছিল),
     * তাই SharedPreferences এ শেষবার দেখানো timestamp রাখা হচ্ছে — নতুন হলেই দেখাবে।
     */
    public void listenForAdminNotifications(NotificationListener listener) {
        db.collection("admin_notifications")
                .orderBy(FIELD_TIMESTAMP, com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Notification listener error: " + error.getMessage());
                        return;
                    }
                    if (snapshots != null && !snapshots.isEmpty()) {
                        com.google.firebase.firestore.DocumentSnapshot doc = snapshots.getDocuments().get(0);
                        String title   = doc.getString("title");
                        String body    = doc.getString("body");
                        String imageUrl = doc.getString("imageUrl");
                        String target  = doc.getString("target"); // "all" বা specific email
                        Long timestamp = doc.getLong(FIELD_TIMESTAMP);
                        String userEmail = databaseManager.getGoogleEmail();

                        android.content.SharedPreferences prefs =
                                context.getSharedPreferences("admin_msg_prefs", Context.MODE_PRIVATE);
                        long lastShown = prefs.getLong("last_shown_notification_ts", 0);
                        if (timestamp != null && timestamp <= lastShown) return; // আগেই দেখানো হয়েছে

                        // সবার জন্য অথবা এই specific user এর জন্য
                        if ("all".equals(target) || (userEmail != null && userEmail.equals(target))) {
                            if (listener != null && title != null && body != null) {
                                if (timestamp != null) {
                                    prefs.edit().putLong("last_shown_notification_ts", timestamp).apply();
                                }
                                listener.onNotificationReceived(title, body, imageUrl != null ? imageUrl : "");
                            }
                        }
                    }
                });
    }

    public interface NotificationListener {
        void onNotificationReceived(String title, String body, String imageUrl);
    }

    // ─────────────────────────────────────────────
    //  📣 Admin: Announcement পাঠানো
    //  Notification থেকে আলাদা — সাধারণত app-wide ঘোষণার জন্য
    // ─────────────────────────────────────────────

    public void listenForAnnouncements(NotificationListener listener) {
        db.collection("announcements")
                .orderBy(FIELD_TIMESTAMP, com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Announcement listener error: " + error.getMessage());
                        return;
                    }
                    if (snapshots != null && !snapshots.isEmpty()) {
                        com.google.firebase.firestore.DocumentSnapshot doc = snapshots.getDocuments().get(0);
                        Boolean active = doc.getBoolean("active");
                        if (active != null && !active) return;

                        String title   = doc.getString("title");
                        String body    = doc.getString("body");
                        String imageUrl = doc.getString("imageUrl");
                        Long timestamp = doc.getLong(FIELD_TIMESTAMP);

                        android.content.SharedPreferences prefs =
                                context.getSharedPreferences("admin_msg_prefs", Context.MODE_PRIVATE);
                        long lastShown = prefs.getLong("last_shown_announcement_ts", 0);
                        if (timestamp != null && timestamp <= lastShown) return;

                        if (listener != null && title != null && body != null) {
                            if (timestamp != null) {
                                prefs.edit().putLong("last_shown_announcement_ts", timestamp).apply();
                            }
                            listener.onNotificationReceived(title, body, imageUrl != null ? imageUrl : "");
                        }
                    }
                });
    }

    // ─────────────────────────────────────────────
    //  🚫 Admin: User block status check
    //  App start-এ এই check চালান
    // ─────────────────────────────────────────────

    /**
     * User blocked কিনা check করুন।
     * SplashActivity বা DashboardActivity এর শুরুতে call করুন।
     */
    public void checkUserBlockStatus(BlockStatusCallback callback) {
        String email = databaseManager.getGoogleEmail();
        if (email == null || email.isEmpty()) {
            if (callback != null) callback.onResult(false, "active");
            return;
        }

        String safeEmail = emailToKey(email);

        // Check top-level users collection (admin sets blocked here)
        db.collection("users")
                .document(safeEmail)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Boolean blocked = doc.getBoolean("blocked");
                        String status = doc.getString("status");
                        boolean isBlocked = (blocked != null && blocked)
                            || "BLOCKED".equals(status)
                            || "DEVICE_BANNED".equals(status);
                        if (callback != null) callback.onResult(isBlocked, status != null ? status : "ACTIVE");
                    } else {
                        if (callback != null) callback.onResult(false, "ACTIVE");
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onResult(false, "UNKNOWN");
                });
    }

    public interface BlockStatusCallback {
        void onResult(boolean isBlocked, String status);
    }

    // ─────────────────────────────────────────────
    //  ⚙️ Admin Config: Force Update / Maintenance
    // ─────────────────────────────────────────────

    /**
     * Admin যখন force update বা maintenance mode set করবে,
     * এই listener user app কে জানাবে।
     */
    public void listenForAppConfig(AppConfigListener listener) {
        db.collection("app_config")
                .document("global")
                .addSnapshotListener((doc, error) -> {
                    if (error != null || doc == null) return;
                    if (doc.exists()) {
                        boolean forceUpdate   = Boolean.TRUE.equals(doc.getBoolean("forceUpdateEnabled"));
                        boolean maintenance   = Boolean.TRUE.equals(doc.getBoolean("maintenanceModeEnabled"));
                        String  latestVersion = doc.getString("latestVersionName");
                        String  updateUrl     = doc.getString("updateUrl");
                        String  noticeTitle   = doc.getString("remoteNoticeTitle");
                        String  noticeBody    = doc.getString("remoteNoticeBody");
                        boolean noticeEnabled = Boolean.TRUE.equals(doc.getBoolean("remoteNoticeEnabled"));

                        if (listener != null) {
                            listener.onConfigReceived(forceUpdate, maintenance,
                                    latestVersion != null ? latestVersion : "",
                                    updateUrl != null ? updateUrl : "",
                                    noticeEnabled, noticeTitle, noticeBody);
                        }
                    }
                });
    }

    public interface AppConfigListener {
        void onConfigReceived(boolean forceUpdate, boolean maintenance,
                              String latestVersion, String updateUrl,
                              boolean noticeEnabled, String noticeTitle, String noticeBody);
    }

    // ─────────────────────────────────────────────
    //  🛠️ Utility Methods
    // ─────────────────────────────────────────────

    /**
     * Email কে Firestore document ID safe করুন।
     * example@gmail.com → example_at_gmail_com
     */
    private String emailToKey(String email) {
        return email.replace("@", "_at_").replace(".", "_");
    }

    private String formatTimestamp(String iso) {
        if (iso == null) return "অজানা";
        try {
            // "2026-06-25T14:30:00" → "25 Jun 2026, 14:30"
            return iso.replace("T", " | ").substring(0, 18);
        } catch (Exception e) {
            return iso;
        }
    }

    private int getAppVersionCode() {
        try {
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (Exception e) {
            return 1;
        }
    }
}
