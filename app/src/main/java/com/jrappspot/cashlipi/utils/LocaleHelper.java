package com.jrappspot.cashlipi.utils;

import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

public class LocaleHelper {

    public static void setLocale(Context context, String langCode) {
        // Save to prefs
        DatabaseManager.getInstance(context).setAppLanguage(langCode);

        // Apply immediately
        applyLocaleCode(langCode);
    }

    public static void applyLocale(Context context) {
        String lang = DatabaseManager.getInstance(context).getAppLanguage();
        applyLocaleCode(lang);
    }

    private static void applyLocaleCode(String langCode) {
        // FIX: আগে "bn"-এর জন্য empty locale list সেট করা হতো, মানে "সিস্টেম ভাষা অনুসরণ কর"।
        // ফোনের সিস্টেম ভাষা ইংরেজি হলে Android values-en রিসোর্স তুলে নিত, ফলে অ্যাপে
        // বাংলা সিলেক্ট করা থাকলেও মাঝে মাঝে ইংরেজি টেক্সট দেখাত (যেমন হোম পেজের টিপ ব্যানার)।
        // এখন bn সহ প্রতিটি ভাষার জন্যই explicit locale force করা হচ্ছে, যাতে ইউজারের
        // বেছে নেওয়া ভাষাই সবসময় দেখায়, ডিভাইসের সিস্টেম ভাষা যাই হোক না কেন।
        LocaleListCompat localeList;
        if (langCode == null || langCode.isEmpty()) {
            // কোনো preference সেট করা নেই (একদম প্রথমবার) — সিস্টেম ভাষা অনুসরণ করবে
            localeList = LocaleListCompat.getEmptyLocaleList();
        } else {
            localeList = LocaleListCompat.forLanguageTags(langCode);
        }
        AppCompatDelegate.setApplicationLocales(localeList);
    }

    @Deprecated
    public static Context wrap(Context context, String language) {
        return context;
    }
}
