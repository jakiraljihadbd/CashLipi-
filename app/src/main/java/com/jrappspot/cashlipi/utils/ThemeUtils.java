package com.jrappspot.cashlipi.utils;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Centralized helper to apply the user's saved Dark/Light mode preference
 * using AppCompatDelegate, so the app always reflects the in-app toggle
 * regardless of the device's system theme.
 */
public class ThemeUtils {

    /** Call once on app startup (Application.onCreate) and whenever the
     *  preference changes, to force the correct night mode. */
    public static void applySavedTheme(DatabaseManager db) {
        if (db.isDarkMode()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
