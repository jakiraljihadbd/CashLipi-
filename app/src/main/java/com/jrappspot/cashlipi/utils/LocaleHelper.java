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
        LocaleListCompat localeList;
        if (langCode == null || langCode.isEmpty() || langCode.equals("bn")) {
            // বাংলা — app default (values/strings.xml)
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
