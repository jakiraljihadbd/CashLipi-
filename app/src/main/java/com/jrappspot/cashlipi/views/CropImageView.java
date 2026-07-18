package com.jrappspot.cashlipi.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

/**
 * নতুন ব্যক্তির ছবির জন্য হালকা, নির্ভরযোগ্য ইন-অ্যাপ ক্রপ ভিউ — কোনো এক্সটার্নাল লাইব্রেরি ছাড়াই।
 * টেনে (pan) সরানো ও দুই আঙুলে জুম (pinch-zoom) করে বৃত্তাকার ফ্রেমের ভেতরে ছবির অংশ বসানো যায়।
 * সেইভ করার সময় বৃত্তটার বাউন্ডিং-স্কয়ার বিটম্যাপ থেকে ক্রপ করে ফেরত দেওয়া হয় (getCroppedBitmap)।
 */
public class CropImageView extends View {

    private Bitmap sourceBitmap;
    private final Matrix matrix = new Matrix();
    private final Matrix inverseMatrix = new Matrix();

    private float minScale = 1f;
    private float maxScale = 1f;
    private float currentScale = 1f;

    private final RectF circleRect = new RectF();
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private ScaleGestureDetector scaleDetector;
    private final PointF lastTouch = new PointF();
    private boolean isDragging = false;

    public CropImageView(Context context) { super(context); init(); }
    public CropImageView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public CropImageView(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }

    private void init() {
        dimPaint.setColor(Color.parseColor("#B3000000"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(Color.parseColor("#E2136E")); // bKash pink border — CashLipi accent
        borderPaint.setStrokeWidth(6f);
        scaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
        setLayerType(LAYER_TYPE_SOFTWARE, null); // Path.Op ক্লিপিং-এর জন্য নিরাপদ
    }

    public void setImageBitmap(Bitmap bitmap) {
        this.sourceBitmap = bitmap;
        requestLayout();
        post(this::resetToCover);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float diameter = Math.min(w, h) * 0.8f;
        float cx = w / 2f;
        float cy = h / 2f;
        circleRect.set(cx - diameter / 2f, cy - diameter / 2f, cx + diameter / 2f, cy + diameter / 2f);
        resetToCover();
    }

    /** বিটম্যাপ বৃত্তটাকে সম্পূর্ণ ঢেকে রাখবে এমন সর্বনিম্ন স্কেল দিয়ে কেন্দ্রে বসানো হয় (centerCrop এর মতো)। */
    private void resetToCover() {
        if (sourceBitmap == null || circleRect.isEmpty()) return;
        float bw = sourceBitmap.getWidth();
        float bh = sourceBitmap.getHeight();
        float scale = Math.max(circleRect.width() / bw, circleRect.height() / bh);
        minScale = scale;
        maxScale = scale * 4f;
        currentScale = scale;

        matrix.reset();
        matrix.postScale(scale, scale);
        float dx = circleRect.centerX() - (bw * scale) / 2f;
        float dy = circleRect.centerY() - (bh * scale) / 2f;
        matrix.postTranslate(dx, dy);
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (sourceBitmap == null) return true;
        scaleDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouch.set(event.getX(), event.getY());
                isDragging = true;
                break;
            case MotionEvent.ACTION_MOVE:
                if (isDragging && !scaleDetector.isInProgress() && event.getPointerCount() == 1) {
                    float dx = event.getX() - lastTouch.x;
                    float dy = event.getY() - lastTouch.y;
                    matrix.postTranslate(dx, dy);
                    constrainTranslation();
                    lastTouch.set(event.getX(), event.getY());
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                break;
        }
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float factor = detector.getScaleFactor();
            float newScale = currentScale * factor;
            newScale = Math.max(minScale, Math.min(maxScale, newScale));
            float appliedFactor = newScale / currentScale;
            currentScale = newScale;
            matrix.postScale(appliedFactor, appliedFactor, detector.getFocusX(), detector.getFocusY());
            constrainTranslation();
            invalidate();
            return true;
        }
    }

    /** ছবি সরাতে সরাতে বৃত্তের ভেতরে ফাঁকা জায়গা যেন না দেখা যায় সেজন্য সীমাবদ্ধ রাখা হয়। */
    private void constrainTranslation() {
        if (sourceBitmap == null) return;
        RectF bounds = new RectF(0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight());
        matrix.mapRect(bounds);

        float dx = 0, dy = 0;
        if (bounds.left > circleRect.left) dx = circleRect.left - bounds.left;
        if (bounds.right < circleRect.right) dx = circleRect.right - bounds.right;
        if (bounds.top > circleRect.top) dy = circleRect.top - bounds.top;
        if (bounds.bottom < circleRect.bottom) dy = circleRect.bottom - bounds.bottom;
        if (dx != 0 || dy != 0) matrix.postTranslate(dx, dy);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (sourceBitmap == null) return;

        canvas.save();
        canvas.drawBitmap(sourceBitmap, matrix, bitmapPaint);
        canvas.restore();

        // বৃত্তের বাইরের অংশ গাঢ় করে দেখানো (evenOdd path — ভেতরে গর্ত)
        Path overlay = new Path();
        overlay.setFillType(Path.FillType.EVEN_ODD);
        overlay.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
        overlay.addCircle(circleRect.centerX(), circleRect.centerY(), circleRect.width() / 2f, Path.Direction.CW);
        canvas.drawPath(overlay, dimPaint);

        canvas.drawCircle(circleRect.centerX(), circleRect.centerY(), circleRect.width() / 2f, borderPaint);
    }

    /**
     * বৃত্তের বাউন্ডিং-স্কয়ার অনুযায়ী মূল বিটম্যাপ থেকে ক্রপ করে {@code outputSize}×{@code outputSize}
     * পিক্সেলের একটা স্কয়ার বিটম্যাপ রিটার্ন করে (গোলাকার মাস্কিং প্রদর্শনের সময় Glide CircleCrop দিয়ে হয়)।
     */
    public Bitmap getCroppedBitmap(int outputSize) {
        if (sourceBitmap == null) return null;
        matrix.invert(inverseMatrix);

        RectF srcRect = new RectF(circleRect);
        inverseMatrix.mapRect(srcRect);

        int left = Math.max(0, Math.round(srcRect.left));
        int top = Math.max(0, Math.round(srcRect.top));
        int right = Math.min(sourceBitmap.getWidth(), Math.round(srcRect.right));
        int bottom = Math.min(sourceBitmap.getHeight(), Math.round(srcRect.bottom));
        int w = Math.max(1, right - left);
        int h = Math.max(1, bottom - top);

        Bitmap cropped = Bitmap.createBitmap(sourceBitmap, left, top, w, h);
        if (cropped.getWidth() == outputSize && cropped.getHeight() == outputSize) return cropped;
        return Bitmap.createScaledBitmap(cropped, outputSize, outputSize, true);
    }
}
