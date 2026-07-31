package com.jrappspot.cashlipi.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * কলমে লেখার মত হাতে-স্বাক্ষর দেওয়ার জন্য কাস্টম ভিউ।
 * ব্যবহারকারী আঙুল দিয়ে আঁকবে, {@link #getSignatureBitmap()} কল করলে সাদা ব্যাকগ্রাউন্ডে
 * কালো কালির স্বাক্ষরের বিটম্যাপ পাওয়া যাবে (PDF-এ বসানোর জন্য প্রস্তুত)।
 */
public class SignaturePadView extends View {

    private Path currentPath;
    private final java.util.List<Path> allPaths = new java.util.ArrayList<>();
    private final Paint inkPaint = new Paint();
    private float lastX, lastY;
    private boolean hasStroke = false;

    public SignaturePadView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        init();
    }

    public SignaturePadView(Context ctx) {
        super(ctx);
        init();
    }

    private void init() {
        setBackgroundColor(Color.WHITE);
        inkPaint.setColor(0xFF1A1A2E); // কালির মত গাঢ় নেভি-কালো
        inkPaint.setAntiAlias(true);
        inkPaint.setStyle(Paint.Style.STROKE);
        inkPaint.setStrokeJoin(Paint.Join.ROUND);
        inkPaint.setStrokeCap(Paint.Cap.ROUND);
        inkPaint.setStrokeWidth(6.5f);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX(), y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentPath = new Path();
                currentPath.moveTo(x, y);
                allPaths.add(currentPath);
                lastX = x;
                lastY = y;
                hasStroke = true;
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (currentPath != null) {
                    currentPath.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2);
                    lastX = x;
                    lastY = y;
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (currentPath != null) currentPath.lineTo(lastX, lastY);
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Path p : allPaths) canvas.drawPath(p, inkPaint);
    }

    /** পুরনো স্বাক্ষর মুছে নতুন করে আঁকা শুরু করে। */
    public void clear() {
        allPaths.clear();
        currentPath = null;
        hasStroke = false;
        invalidate();
    }

    /** আঙুল দিয়ে কিছু আঁকা হয়েছে কি না। */
    public boolean isEmpty() {
        return !hasStroke;
    }

    /** স্বাক্ষরের সাদা-ব্যাকগ্রাউন্ড বিটম্যাপ — PDF/প্রিভিউতে সরাসরি বসানো যাবে। */
    public Bitmap getSignatureBitmap() {
        int w = getWidth() > 0 ? getWidth() : 600;
        int h = getHeight() > 0 ? getHeight() : 220;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        c.drawColor(Color.WHITE);
        for (Path p : allPaths) c.drawPath(p, inkPaint);
        return bmp;
    }
}
