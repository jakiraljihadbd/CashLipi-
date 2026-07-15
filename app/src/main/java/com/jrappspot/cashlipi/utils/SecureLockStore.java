package com.jrappspot.cashlipi.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;

/**
 * SecureLockStore — App Lock (PIN/প্যাটার্ন + সিকিউরিটি প্রশ্নের উত্তর) এর জন্য
 * আলাদা, এনক্রিপ্টেড SharedPreferences স্টোর।
 *
 * কেন আলাদা ফাইল?
 *  - App Lock ডাটা (PIN hash, security answers) অ্যাপের বাকি সব ডাটা (income/expense/...)
 *    থেকে বেশি স্পর্শকাতর, তাই এটা Android Keystore-backed AES-256 encryption দিয়ে
 *    আলাদাভাবে সুরক্ষিত রাখা হয় (EncryptedSharedPreferences — কী ও ভ্যালু দুটোই এনক্রিপ্টেড)।
 *  - সাধারণ sign-in / sign-out / login flow কখনোই এই ফাইলটা ছোঁয় না, তাই PIN রিসেট হয় না।
 *  - শুধুমাত্র অ্যাপ আনইনস্টল করলে বা "Clear app data" করলে এই ডাটা মুছে যাবে —
 *    Android সিস্টেম নিজেই তখন পুরো অ্যাপের প্রাইভেট স্টোরেজ মুছে দেয়।
 *
 * পুরনো ভার্সনে এই ভ্যালুগুলো সাধারণ (un-encrypted) SharedPreferences-এ রাখা হতো;
 * প্রথমবার এই ক্লাস ব্যবহারের সময় সেই পুরনো ভ্যালু থাকলে এখানে migrate করে আনা হয়
 * এবং পুরনো জায়গা থেকে মুছে ফেলা হয়, যাতে কোনো ইউজারের সেট করা PIN হারিয়ে না যায়।
 */
public class SecureLockStore {

    private static final String TAG = "SecureLockStore";
    private static final String SECURE_PREF_NAME = "cashlipi_app_lock_secure";

    // Legacy (un-encrypted) location — শুধু এক-বারের migration-এর জন্য ব্যবহৃত হয়।
    private static final String LEGACY_PREF_NAME = "cashlipi_account_db";

    private static final String KEY_LOCK_ENABLED = "_lock_enabled";
    private static final String KEY_LOCK_TYPE    = "_lock_type";
    private static final String KEY_LOCK_SECRET  = "_lock_secret";
    private static final String KEY_LOCK_FINGER  = "_lock_finger";
    private static final String KEY_SEC_ANS_PREFIX = "_sec_ans_";
    private static final String KEY_MIGRATED = "_secure_migrated_v1";

    // ── নতুন "ONE question" রিসেট মডেল (Material 3 App Lock wizard) ──
    // পুরনো ৫-প্রশ্নের মডেল থেকে আলাদা রাখা হয়েছে যাতে আগের ডাটার সাথে সংঘর্ষ না হয়।
    private static final String KEY_SEC_QUESTION_INDEX = "_sec_q_index";
    private static final String KEY_SEC_SINGLE_ANSWER   = "_sec_single_ans";

    public static final String LOCK_PIN     = "pin";
    public static final String LOCK_PATTERN = "pattern";

    public static final String[] SECURITY_QUESTIONS = {
        "আপনার ডাকনাম কী?",
        "আপনার বাবার নাম কী?",
        "আপনার মায়ের নাম কী?",
        "আপনার প্রিয় রং কী?",
        "আপনার জন্ম তারিখ কত? (DD-MM-YYYY)"
    };

    private static volatile SecureLockStore instance;
    private final SharedPreferences securePrefs;

    private SecureLockStore(Context context) {
        SharedPreferences prefs;
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

            prefs = EncryptedSharedPreferences.create(
                SECURE_PREF_NAME,
                masterKeyAlias,
                context.getApplicationContext(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            // অত্যন্ত বিরল কেস (Keystore করাপ্টেড ইত্যাদি) — app crash না করিয়ে
            // সাধারণ SharedPreferences-এ fallback করা হচ্ছে যাতে ইউজার লক-আউট না হয়ে যায়।
            Log.e(TAG, "EncryptedSharedPreferences init failed, falling back to regular prefs", e);
            prefs = context.getApplicationContext()
                .getSharedPreferences(SECURE_PREF_NAME + "_fallback", Context.MODE_PRIVATE);
        }
        this.securePrefs = prefs;
        migrateFromLegacyIfNeeded(context);
    }

    public static SecureLockStore getInstance(Context context) {
        if (instance == null) {
            synchronized (SecureLockStore.class) {
                if (instance == null) {
                    instance = new SecureLockStore(context);
                }
            }
        }
        return instance;
    }

    /** পুরনো plain-text SharedPreferences থেকে একবারই ডাটা টেনে এনক্রিপ্টেড স্টোরে বসানো হয়। */
    private void migrateFromLegacyIfNeeded(Context context) {
        if (safeGetBoolean(KEY_MIGRATED, false)) return;

        SharedPreferences legacy = context.getApplicationContext()
            .getSharedPreferences(LEGACY_PREF_NAME, Context.MODE_PRIVATE);

        SharedPreferences.Editor secureEditor = securePrefs.edit();
        boolean hadLegacyData = false;

        if (legacy.contains(KEY_LOCK_ENABLED)) {
            secureEditor.putBoolean(KEY_LOCK_ENABLED, legacy.getBoolean(KEY_LOCK_ENABLED, false));
            hadLegacyData = true;
        }
        if (legacy.contains(KEY_LOCK_TYPE)) {
            secureEditor.putString(KEY_LOCK_TYPE, legacy.getString(KEY_LOCK_TYPE, LOCK_PIN));
            hadLegacyData = true;
        }
        if (legacy.contains(KEY_LOCK_SECRET)) {
            secureEditor.putString(KEY_LOCK_SECRET, legacy.getString(KEY_LOCK_SECRET, ""));
            hadLegacyData = true;
        }
        if (legacy.contains(KEY_LOCK_FINGER)) {
            secureEditor.putBoolean(KEY_LOCK_FINGER, legacy.getBoolean(KEY_LOCK_FINGER, false));
            hadLegacyData = true;
        }
        for (int i = 0; i < SECURITY_QUESTIONS.length; i++) {
            String k = KEY_SEC_ANS_PREFIX + i;
            if (legacy.contains(k)) {
                secureEditor.putString(k, legacy.getString(k, ""));
                hadLegacyData = true;
            }
        }
        secureEditor.putBoolean(KEY_MIGRATED, true);
        secureEditor.apply();

        if (hadLegacyData) {
            // পুরনো plain-text কপি মুছে ফেলা হচ্ছে যাতে সংবেদনশীল ডাটা দুই জায়গায় না থাকে।
            SharedPreferences.Editor legacyEditor = legacy.edit();
            legacyEditor.remove(KEY_LOCK_ENABLED);
            legacyEditor.remove(KEY_LOCK_TYPE);
            legacyEditor.remove(KEY_LOCK_SECRET);
            legacyEditor.remove(KEY_LOCK_FINGER);
            for (int i = 0; i < SECURITY_QUESTIONS.length; i++) {
                legacyEditor.remove(KEY_SEC_ANS_PREFIX + i);
            }
            legacyEditor.apply();
            Log.i(TAG, "Migrated App Lock data into encrypted storage");
        }
    }

    // ═══════════════════════════════════════════
    //  LOCK STATE
    // ═══════════════════════════════════════════
    // getBoolean/getString এখানে try-catch দিয়ে wrap করা — যদি কখনো Keystore key
    // invalidate হয় (যেমন কেউ ম্যানুয়ালি পুরনো ব্যাকআপ ফাইল কপি করে দেয়) এনক্রিপ্টেড
    // ডাটা decrypt করতে ব্যর্থ হলে অ্যাপ ক্র্যাশ না করে নিরাপদ ডিফল্টে ফিরে আসবে
    // (ব্যবহারকারীকে PIN আবার সেট করতে হবে, কিন্তু অ্যাপ বন্ধ হয়ে যাবে না)।
    public boolean isLockEnabled()  { return safeGetBoolean(KEY_LOCK_ENABLED, false); }
    public String  getLockType()    { return safeGetString(KEY_LOCK_TYPE, LOCK_PIN); }
    public String  getLockSecret()  { return safeGetString(KEY_LOCK_SECRET, ""); }
    public boolean isFingerprintEnabled() { return safeGetBoolean(KEY_LOCK_FINGER, false); }

    public void setLockEnabled(boolean v)  { securePrefs.edit().putBoolean(KEY_LOCK_ENABLED, v).apply(); }
    public void setLockType(String t)      { securePrefs.edit().putString(KEY_LOCK_TYPE, t).apply(); }
    public void setLockSecret(String h)    { securePrefs.edit().putString(KEY_LOCK_SECRET, h).apply(); }
    public void setFingerprintEnabled(boolean v) { securePrefs.edit().putBoolean(KEY_LOCK_FINGER, v).apply(); }

    private boolean safeGetBoolean(String key, boolean def) {
        try {
            return securePrefs.getBoolean(key, def);
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to read encrypted lock pref, resetting to default", e);
            return def;
        }
    }

    private String safeGetString(String key, String def) {
        try {
            return securePrefs.getString(key, def);
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to read encrypted lock pref, resetting to default", e);
            return def;
        }
    }

    /** PIN/প্যাটার্নের হ্যাশ + ফিঙ্গারপ্রিন্ট সেটিং একসাথে সেভ করে (hashedSecret — কখনো plain-text না)। */
    public void saveLock(String type, String hashedSecret, boolean fingerprint) {
        securePrefs.edit()
            .putBoolean(KEY_LOCK_ENABLED, true)
            .putString(KEY_LOCK_TYPE, type)
            .putString(KEY_LOCK_SECRET, hashedSecret)
            .putBoolean(KEY_LOCK_FINGER, fingerprint)
            .apply();
    }

    /** লক বন্ধ করে এবং সিক্রেট মুছে দেয়। সিকিউরিটি প্রশ্নের উত্তর অক্ষত থাকে (Forgot PIN reuse করতে পারবে)। */
    public void disableLock() {
        securePrefs.edit()
            .putBoolean(KEY_LOCK_ENABLED, false)
            .putString(KEY_LOCK_SECRET, "")
            .apply();
    }

    // ═══════════════════════════════════════════
    //  SECURITY QUESTIONS
    // ═══════════════════════════════════════════
    public void saveSecurityAnswers(String[] answers) {
        SharedPreferences.Editor e = securePrefs.edit();
        for (int i = 0; i < SECURITY_QUESTIONS.length && i < answers.length; i++) {
            String normalized = answers[i] == null ? "" : answers[i].trim().toLowerCase();
            e.putString(KEY_SEC_ANS_PREFIX + i, LockUtils.hash(normalized));
        }
        e.apply();
    }

    public boolean hasSecurityQuestions() {
        for (int i = 0; i < SECURITY_QUESTIONS.length; i++) {
            if (safeGetString(KEY_SEC_ANS_PREFIX + i, "").isEmpty()) return false;
        }
        return true;
    }

    public boolean verifySecurityAnswers(Map<Integer, String> givenAnswers) {
        for (Map.Entry<Integer, String> entry : givenAnswers.entrySet()) {
            String stored = safeGetString(KEY_SEC_ANS_PREFIX + entry.getKey(), "");
            String normalized = entry.getValue() == null ? "" : entry.getValue().trim().toLowerCase();
            if (stored.isEmpty() || !LockUtils.verify(normalized, stored)) return false;
        }
        return true;
    }

    // ═══════════════════════════════════════════
    //  SINGLE SECURITY QUESTION (App Lock wizard — "Never ask 3 questions")
    //  ব্যবহারকারী সেটআপের সময় মাত্র ১টা প্রশ্ন বেছে উত্তর দেয়; PIN রিসেটের
    //  সময় শুধু ওই একটা প্রশ্নই জিজ্ঞাসা করা হয়।
    // ═══════════════════════════════════════════

    /** নির্বাচিত প্রশ্নের ইনডেক্স ও (হ্যাশ করা) উত্তর সেভ করে। */
    public void saveSecurityQuestion(int questionIndex, String answer) {
        String normalized = answer == null ? "" : answer.trim().toLowerCase();
        securePrefs.edit()
            .putInt(KEY_SEC_QUESTION_INDEX, questionIndex)
            .putString(KEY_SEC_SINGLE_ANSWER, LockUtils.hash(normalized))
            .apply();
    }

    /** সিকিউরিটি প্রশ্ন সেট করা আছে কিনা (skip করা থাকলে false)। */
    public boolean hasSecurityQuestion() {
        return getSecurityQuestionIndex() >= 0 && !safeGetString(KEY_SEC_SINGLE_ANSWER, "").isEmpty();
    }

    /** সেভ করা প্রশ্নের ইনডেক্স, না থাকলে -1। */
    public int getSecurityQuestionIndex() {
        try {
            return securePrefs.getInt(KEY_SEC_QUESTION_INDEX, -1);
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to read security question index", e);
            return -1;
        }
    }

    /** ব্যবহারকারীর দেওয়া উত্তর সেভ করা (হ্যাশড) উত্তরের সাথে মেলে কিনা যাচাই করে। */
    public boolean verifySecurityAnswer(String givenAnswer) {
        String stored = safeGetString(KEY_SEC_SINGLE_ANSWER, "");
        if (stored.isEmpty()) return false;
        String normalized = givenAnswer == null ? "" : givenAnswer.trim().toLowerCase();
        return LockUtils.verify(normalized, stored);
    }

    /** সিকিউরিটি প্রশ্ন-উত্তর মুছে ফেলে। */
    public void clearSecurityQuestion() {
        securePrefs.edit()
            .remove(KEY_SEC_QUESTION_INDEX)
            .remove(KEY_SEC_SINGLE_ANSWER)
            .apply();
    }
}
