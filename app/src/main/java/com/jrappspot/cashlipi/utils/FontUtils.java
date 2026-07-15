package com.jrappspot.cashlipi.utils;

import android.content.Context;
import android.graphics.Typeface;

import java.io.File;

/**
 * FontUtils — user-imported TTF font apply করার utility।
 *
 * Flow:
 *   1. User file picker থেকে TTF pick করে
 *   2. AdvancedFeaturesActivity সেটা getCustomFontDir() তে copy করে
 *   3. DatabaseManager এ path save করে
 *   4. যেকোনো Activity তে applyToView() call করলে font apply হয়
 */
public class FontUtils {

    /** Custom font গুলো এই directory তে store হয় */
    public static File getCustomFontDir(Context ctx) {
        File dir = new File(ctx.getFilesDir(), "custom_fonts");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    /**
     * Saved font path থেকে Typeface বানায়।
     * "default" বা invalid path হলে null রিটার্ন করে।
     */
    public static Typeface getTypeface(Context ctx) {
        DatabaseManager db = DatabaseManager.getInstance(ctx);
        String fontPath = db.getCustomFontPath();

        if (fontPath == null || fontPath.equals("default") || fontPath.isEmpty()) {
            return null;
        }

        File fontFile = new File(fontPath);
        if (!fontFile.exists()) {
            // File মুছে গেছে — reset করো
            db.setCustomFontPath("default");
            return null;
        }

        try {
            return Typeface.createFromFile(fontFile);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * সব TextView তে font apply করে (recursive)।
     * Activity onCreate() এ setContentView() এর পরে call করো।
     */
    public static void applyToView(Context ctx, android.view.View root) {
        Typeface tf = getTypeface(ctx);
        if (tf == null || !(root instanceof android.view.ViewGroup)) return;
        applyRecursive((android.view.ViewGroup) root, tf);
    }

    private static void applyRecursive(android.view.ViewGroup group, Typeface tf) {
        for (int i = 0; i < group.getChildCount(); i++) {
            android.view.View child = group.getChildAt(i);
            if (child instanceof android.widget.TextView) {
                android.widget.TextView tv = (android.widget.TextView) child;
                if (tf != null) {
                    int style = tv.getTypeface() != null ? tv.getTypeface().getStyle() : Typeface.NORMAL;
                    tv.setTypeface(tf, style);
                } else {
                    // null = system theme font instant restore
                    tv.setTypeface(null);
                }
            } else if (child instanceof android.view.ViewGroup) {
                applyRecursive((android.view.ViewGroup) child, tf);
            }
        }
    }
}
