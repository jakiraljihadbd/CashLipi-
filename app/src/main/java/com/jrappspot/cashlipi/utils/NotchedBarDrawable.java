package com.jrappspot.cashlipi.utils;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * নেভ বারের ব্যাকগ্রাউন্ড শেপ — বারের উপরের কিনারায় মাঝখানে + বাটনের জন্য একটা সত্যিকারের
 * "কাটআউট/কার্ভড নচ" (cradle) আঁকে, আগের মতো + বাটনের পেছনে একটা আলাদা পেজ-রঙা গোল বসিয়ে
 * ফাঁকা-হোল হওয়ার ভান করা হতো না — এখানে বারের নিজের সিলুয়েটই বাঁকানো (Path দিয়ে), ঠিক যেভাবে
 * রেফারেন্স স্ক্রিনশটে + বাটনটা বারের মধ্যে "বসে" থাকতে দেখা যায়।
 */
public class NotchedBarDrawable extends Drawable {

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint;

    private int solidColor = 0;
    private int[] gradientColors = null; // non-null হলে TL→BR গ্রেডিয়েন্ট আঁকা হয়

    private final float topLeftRadius, topRightRadius, bottomLeftRadius, bottomRightRadius;
    private final float cradleRadius;   // + বাটনের চারপাশে কতটা বড় গোল কেটে বসানো হবে
    private final boolean cradleEnabled;

    private final Path path = new Path();

    public NotchedBarDrawable(float topLeftRadius, float topRightRadius,
                               float bottomLeftRadius, float bottomRightRadius,
                               float cradleRadius, boolean cradleEnabled) {
        this.topLeftRadius = topLeftRadius;
        this.topRightRadius = topRightRadius;
        this.bottomLeftRadius = bottomLeftRadius;
        this.bottomRightRadius = bottomRightRadius;
        this.cradleRadius = cradleRadius;
        this.cradleEnabled = cradleEnabled;
        this.strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.strokePaint.setStyle(Paint.Style.STROKE);
    }

    public void setSolidColor(int color) {
        this.solidColor = color;
        this.gradientColors = null;
    }

    public void setGradientColors(int startColor, int endColor) {
        this.gradientColors = new int[]{startColor, endColor};
    }

    public void setStroke(float widthPx, int color) {
        strokePaint.setStrokeWidth(widthPx);
        strokePaint.setColor(color);
    }

    @Override
    protected void onBoundsChange(android.graphics.Rect bounds) {
        super.onBoundsChange(bounds);
        rebuildPath(bounds.width(), bounds.height());
        if (gradientColors != null) {
            fillPaint.setShader(new LinearGradient(
                    0, 0, bounds.width(), bounds.height(),
                    gradientColors[0], gradientColors[1], Shader.TileMode.CLAMP));
        }
    }

    private void rebuildPath(int w, int h) {
        path.reset();
        float cx = w / 2f;
        float R = cradleEnabled ? cradleRadius : 0f;

        // ── উপরের কিনারা: বাম কোণা → (কেন্দ্রে থাকলে) নচ কেটে ডানের কোণা ──
        path.moveTo(0, topLeftRadius);
        if (topLeftRadius > 0) {
            path.arcTo(new RectF(0, 0, topLeftRadius * 2, topLeftRadius * 2), 180, 90);
        } else {
            path.lineTo(0, 0);
        }

        if (cradleEnabled && R > 0) {
            path.lineTo(cx - R, 0);
            // কেন্দ্রে (cx,0) বিন্দু-কেন্দ্রিক বৃত্তের নিচের অর্ধেক ধরে বারের ভেতর দিকে বাঁকানো —
            // এটাই আসল "ফাঁকা কার্ভ কাটআউট", + বাটন এই গর্তের ভেতর বসবে
            path.arcTo(new RectF(cx - R, -R, cx + R, R), 180, -180);
        } else {
            path.lineTo(w - topRightRadius, 0);
        }

        if (topRightRadius > 0) {
            path.lineTo(w - topRightRadius, 0);
            path.arcTo(new RectF(w - topRightRadius * 2, 0, w, topRightRadius * 2), 270, 90);
        } else {
            path.lineTo(w, 0);
        }

        // ── ডান পাশ নিচে নেমে বটম-রাইট কোণা ──
        path.lineTo(w, h - bottomRightRadius);
        if (bottomRightRadius > 0) {
            path.arcTo(new RectF(w - bottomRightRadius * 2, h - bottomRightRadius * 2, w, h), 0, 90);
        }

        // ── নিচের কিনারা ধরে বাম দিকে, বটম-লেফট কোণা ──
        path.lineTo(bottomLeftRadius, h);
        if (bottomLeftRadius > 0) {
            path.arcTo(new RectF(0, h - bottomLeftRadius * 2, bottomLeftRadius * 2, h), 90, 90);
        }

        path.lineTo(0, topLeftRadius);
        path.close();
    }

    @Override
    public void draw(Canvas canvas) {
        if (gradientColors == null) {
            fillPaint.setShader(null);
            fillPaint.setColor(solidColor);
        }
        canvas.drawPath(path, fillPaint);
        if (strokePaint.getStrokeWidth() > 0) {
            canvas.drawPath(path, strokePaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        fillPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        fillPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
