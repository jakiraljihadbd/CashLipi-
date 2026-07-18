package com.jrappspot.cashlipi.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import com.jrappspot.cashlipi.R;

import java.util.ArrayList;
import java.util.List;

/**
 * নেভ বার "স্টাইল প্রিসেট" — ৭টা রেডিমেড লুক। DashboardActivity (আসল নেভবার) আর
 * NavCustomizeActivity (লাইভ প্রিভিউ) — দুই জায়গা থেকেই একই লজিক ব্যবহার হয়, যাতে
 * প্রিভিউ আর আসল ফলাফল হুবহু এক থাকে।
 */
public class NavBarStyler {

    public static final String STYLE_CLASSIC  = "classic";
    public static final String STYLE_FLOATING = "floating";
    public static final String STYLE_GLASS    = "glass";
    public static final String STYLE_GRADIENT = "gradient";
    public static final String STYLE_MINIMAL  = "minimal";
    public static final String STYLE_NEON     = "neon";
    public static final String STYLE_CARD     = "card";

    /** স্টাইল সিলেক্টরে দেখানোর জন্য মেটাডেটা */
    public static class StyleInfo {
        public final String key, title, subtitle;
        public StyleInfo(String key, String title, String subtitle) {
            this.key = key; this.title = title; this.subtitle = subtitle;
        }
    }

    public static List<StyleInfo> allStyles() {
        List<StyleInfo> list = new ArrayList<>();
        list.add(new StyleInfo(STYLE_CLASSIC,  "ক্লাসিক বার",       "সলিড রং, স্ট্যান্ডার্ড শার্প বার"));
        list.add(new StyleInfo(STYLE_FLOATING, "ফ্লোটিং পিল",       "চারদিকে মার্জিনসহ ভাসমান গোলাকার বার"));
        list.add(new StyleInfo(STYLE_GLASS,    "গ্লাসমরফিজম",       "স্বচ্ছ কাচের মতো ব্লার-স্টাইল লুক"));
        list.add(new StyleInfo(STYLE_GRADIENT, "গ্রেডিয়েন্ট কার্ভ",  "দুই রঙের মসৃণ গ্রেডিয়েন্ট ব্যাকগ্রাউন্ড"));
        list.add(new StyleInfo(STYLE_MINIMAL,  "মিনিমাল লেবেল",     "ফ্ল্যাট, লেবেল সবসময় দেখা যায়"));
        list.add(new StyleInfo(STYLE_NEON,     "ডার্ক নিয়ন",        "গাঢ় ব্যাকগ্রাউন্ড + গ্লো ইন্ডিকেটর"));
        list.add(new StyleInfo(STYLE_CARD,     "কালারফুল কার্ড",     "সিলেক্টেড আইকনে রঙিন পিল ব্যাকগ্রাউন্ড"));
        return list;
    }

    /** বার-স্লট (topNavSlot/bottomNavSlot বা প্রিভিউ কন্টেইনার)-এ ব্যাকগ্রাউন্ড শেপ/রং/মার্জিন/এলিভেশন বসায়।
     *  (পুরনো সিগনেচার — নচ/কাটআউট ছাড়া, প্রিভিউ ও টপ-পজিশনে ব্যবহৃত হয়) */
    public static void applyBarBackground(Context ctx, View barSlot, String styleKey, int baseColor, boolean bottomPosition) {
        applyBarBackground(ctx, barSlot, styleKey, baseColor, bottomPosition, false);
    }

    /** একই মেথড, কিন্তু drawCradle=true দিলে বারের উপরের কিনারায় + বাটনের জন্য সত্যিকারের কাটআউট নচ
     *  আঁকা হয় (NotchedBarDrawable দিয়ে) — শুধু আসল বটম নেভবারে (DashboardActivity) ব্যবহার হয়, যেখানে
     *  + বাটনটা সত্যিই ঐ গর্তের ভেতর বসে। প্রিভিউ/টপ-বারে drawCradle=false-ই থাকে। */
    public static void applyBarBackground(Context ctx, View barSlot, String styleKey, int baseColor,
                                           boolean bottomPosition, boolean drawCradle) {
        float d = ctx.getResources().getDisplayMetrics().density;
        String style = styleKey == null ? STYLE_CLASSIC : styleKey;

        boolean cradle = drawCradle && bottomPosition;
        float cradleRadiusPx = cradle
                ? ctx.getResources().getDimensionPixelSize(R.dimen.bottom_fab_notch_size) / 2f
                : 0f;

        float cornerFull = 22f * d;
        float cornerTopOnly = 18f * d;
        int marginH = 0, marginTop = 0, marginBottom = 0;
        float elevation = bottomPosition ? 8f * d : 0f;

        // স্টাইল অনুযায়ী রং/স্ট্রোক/কোণা নির্ধারণ — GradientDrawable (নন-কার্ভ পথ) বা
        // NotchedBarDrawable (কার্ভ/নচ পথ), দুই ক্ষেত্রেই একই ভ্যালুগুলো ব্যবহৃত হয়
        int fillColor = baseColor;
        Integer gradStart = null, gradEnd = null;
        Integer strokeColor = null;
        float strokeWidth = 0f;
        float topRadius = 0f; // top-only radius (bottomPosition-এ ব্যবহৃত), না হলে ০
        boolean allRounded = false;

        switch (style) {
            case STYLE_FLOATING:
                allRounded = true;
                fillColor = baseColor;
                marginH = (int) (12 * d);
                if (bottomPosition) marginBottom = (int) (10 * d);
                elevation = 14f * d;
                break;
            case STYLE_GLASS:
                allRounded = true;
                fillColor = withAlpha(baseColor, 205);
                strokeColor = withAlpha(Color.WHITE, 55);
                strokeWidth = 1 * d;
                marginH = (int) (8 * d);
                if (bottomPosition) marginBottom = (int) (6 * d);
                elevation = 4f * d;
                break;
            case STYLE_GRADIENT:
                gradStart = lighten(baseColor, 0.16f);
                gradEnd = darken(baseColor, 0.18f);
                if (bottomPosition) topRadius = cornerTopOnly;
                elevation = 10f * d;
                break;
            case STYLE_MINIMAL:
                fillColor = ContextCompat.getColor(ctx, R.color.cardBg);
                strokeColor = ContextCompat.getColor(ctx, R.color.dividerColor);
                strokeWidth = 1 * d;
                elevation = 0f;
                break;
            case STYLE_NEON:
                fillColor = Color.parseColor("#0F1B2E");
                if (bottomPosition) topRadius = cornerTopOnly;
                elevation = 10f * d;
                break;
            case STYLE_CARD:
                fillColor = ContextCompat.getColor(ctx, R.color.cardBg);
                if (bottomPosition) topRadius = cornerTopOnly;
                elevation = 8f * d;
                break;
            case STYLE_CLASSIC:
            default:
                fillColor = baseColor;
                if (bottomPosition) topRadius = cornerTopOnly;
                break;
        }

        if (!cradle) {
            // ── পুরনো পথ: GradientDrawable (কোনো নচ/কাটআউট নেই) ──
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            if (allRounded) {
                bg.setCornerRadius(cornerFull);
            } else if (topRadius > 0) {
                bg.setCornerRadii(topOnlyRadii(topRadius));
            }
            if (gradStart != null) {
                bg.setOrientation(GradientDrawable.Orientation.TL_BR);
                bg.setColors(new int[]{gradStart, gradEnd});
            } else {
                bg.setColor(fillColor);
            }
            if (strokeColor != null) bg.setStroke((int) strokeWidth, strokeColor);
            barSlot.setBackground(bg);
        } else {
            // ── নতুন পথ: NotchedBarDrawable — বারের সিলুয়েটেই + বাটনের জন্য কার্ভড কাটআউট ──
            float tl = allRounded ? cornerFull : topRadius;
            float tr = allRounded ? cornerFull : topRadius;
            float bl = allRounded ? cornerFull : 0f;
            float br = allRounded ? cornerFull : 0f;
            NotchedBarDrawable bg = new NotchedBarDrawable(tl, tr, bl, br, cradleRadiusPx, true);
            if (gradStart != null) {
                bg.setGradientColors(gradStart, gradEnd);
            } else {
                bg.setSolidColor(fillColor);
            }
            if (strokeColor != null) bg.setStroke(strokeWidth, strokeColor);
            barSlot.setBackground(bg);
        }

        barSlot.setElevation(elevation);

        ViewGroup.LayoutParams raw = barSlot.getLayoutParams();
        if (raw instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) raw;
            mlp.setMargins(marginH, marginTop, marginH, marginBottom);
            barSlot.setLayoutParams(mlp);
        }
    }

    /** সিলেক্টেড আইটেমের এক্সট্রা ভিজ্যুয়াল (কার্ড-পিল / নিয়ন-গ্লো) — icon color filter কলার নিজে করে। */
    public static void applyItemSelection(Context ctx, LinearLayout[] items, View[] indicators,
                                           int selectedPosition, String styleKey, int baseColor) {
        float d = ctx.getResources().getDisplayMetrics().density;
        String style = styleKey == null ? STYLE_CLASSIC : styleKey;

        for (int i = 0; i < items.length; i++) {
            boolean selected = (i == selectedPosition);
            if (items[i] == null) continue;

            if (STYLE_CARD.equals(style)) {
                if (selected) {
                    GradientDrawable pill = new GradientDrawable();
                    pill.setShape(GradientDrawable.RECTANGLE);
                    pill.setCornerRadius(16f * d);
                    pill.setColor(withAlpha(baseColor, 40));
                    items[i].setBackground(pill);
                } else {
                    items[i].setBackground(null);
                }
                if (indicators[i] != null) indicators[i].setVisibility(View.GONE);
            } else if (STYLE_NEON.equals(style)) {
                items[i].setBackground(null);
                if (indicators[i] != null) {
                    if (selected) {
                        GradientDrawable glow = new GradientDrawable();
                        glow.setShape(GradientDrawable.RECTANGLE);
                        glow.setCornerRadius(6f * d);
                        glow.setColor(baseColor);
                        indicators[i].setBackground(glow);
                        indicators[i].setVisibility(View.VISIBLE);
                    } else {
                        indicators[i].setVisibility(View.INVISIBLE);
                    }
                }
            } else {
                items[i].setBackground(null);
                if (indicators[i] != null) {
                    indicators[i].setBackgroundResource(R.drawable.bg_nav_indicator);
                    indicators[i].setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
                }
            }
        }
    }

    /** নচ/কুশন সার্কেলের জন্য — স্টাইল অনুযায়ী বারের "কার্যকর" সলিড রং (গ্লাস/গ্রেডিয়েন্টের জন্য approx)। */
    public static int effectiveBarColor(Context ctx, String styleKey, int baseColor) {
        String style = styleKey == null ? STYLE_CLASSIC : styleKey;
        switch (style) {
            case STYLE_NEON:
                return Color.parseColor("#0F1B2E");
            case STYLE_MINIMAL:
            case STYLE_CARD:
                return ContextCompat.getColor(ctx, R.color.cardBg);
            default:
                return baseColor;
        }
    }

    private static float[] topOnlyRadii(float r) {
        return new float[]{r, r, r, r, 0, 0, 0, 0};
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private static int lighten(int color, float factor) {
        int r = Math.min(255, (int) (Color.red(color) + (255 - Color.red(color)) * factor));
        int g = Math.min(255, (int) (Color.green(color) + (255 - Color.green(color)) * factor));
        int b = Math.min(255, (int) (Color.blue(color) + (255 - Color.blue(color)) * factor));
        return Color.rgb(r, g, b);
    }

    private static int darken(int color, float factor) {
        int r = (int) (Color.red(color) * (1 - factor));
        int g = (int) (Color.green(color) * (1 - factor));
        int b = (int) (Color.blue(color) * (1 - factor));
        return Color.rgb(Math.max(r, 0), Math.max(g, 0), Math.max(b, 0));
    }
}
