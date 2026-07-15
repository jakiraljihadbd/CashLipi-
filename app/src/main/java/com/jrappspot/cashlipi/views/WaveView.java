package com.jrappspot.cashlipi.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class WaveView extends View {

    // Wave 1 — front, faster, more opaque
    private final Path mPath1 = new Path();
    private final Paint mPaint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float mOffset1 = 0f;

    // Wave 2 — back, slower, more transparent
    private final Path mPath2 = new Path();
    private final Paint mPaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float mOffset2 = 0f;

    // Wave 3 — mid layer
    private final Path mPath3 = new Path();
    private final Paint mPaint3 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float mOffset3 = 0f;

    private ValueAnimator mAnimator1, mAnimator2, mAnimator3;

    // Wave shape params
    private float mWaveHeight = 18f;
    private float mWaveLength;

    public WaveView(Context context) {
        super(context);
        init();
    }

    public WaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WaveView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPaint1.setStyle(Paint.Style.FILL);
        mPaint2.setStyle(Paint.Style.FILL);
        mPaint3.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        if (w == 0 || h == 0) return;

        mWaveLength = w * 1.2f;
        mWaveHeight = h * 0.10f;

        // Front wave — green tinted
        mPaint1.setShader(new LinearGradient(0, h * 0.55f, 0, h,
                0x5500D4FF, 0x1500D4FF, Shader.TileMode.CLAMP));

        // Back wave — dark green tinted
        mPaint2.setShader(new LinearGradient(0, h * 0.45f, 0, h,
                0x336D28D9, 0x0D6D28D9, Shader.TileMode.CLAMP));

        // Mid wave — white shimmer
        mPaint3.setShader(new LinearGradient(0, h * 0.50f, 0, h,
                0x22FFFFFF, 0x05FFFFFF, Shader.TileMode.CLAMP));

        startAnimators();
    }

    private void startAnimators() {
        stopAnimators();

        // Front wave — 3.2s
        mAnimator1 = ValueAnimator.ofFloat(0f, mWaveLength);
        mAnimator1.setDuration(3200);
        mAnimator1.setRepeatCount(ValueAnimator.INFINITE);
        mAnimator1.setInterpolator(new LinearInterpolator());
        mAnimator1.addUpdateListener(a -> {
            mOffset1 = (float) a.getAnimatedValue();
            invalidate();
        });
        mAnimator1.start();

        // Back wave — 4.8s, opposite direction
        mAnimator2 = ValueAnimator.ofFloat(0f, -mWaveLength);
        mAnimator2.setDuration(4800);
        mAnimator2.setRepeatCount(ValueAnimator.INFINITE);
        mAnimator2.setInterpolator(new LinearInterpolator());
        mAnimator2.addUpdateListener(a -> mOffset2 = (float) a.getAnimatedValue());
        mAnimator2.start();

        // Mid wave — 5.5s
        mAnimator3 = ValueAnimator.ofFloat(0f, mWaveLength);
        mAnimator3.setDuration(5500);
        mAnimator3.setRepeatCount(ValueAnimator.INFINITE);
        mAnimator3.setInterpolator(new LinearInterpolator());
        mAnimator3.addUpdateListener(a -> mOffset3 = (float) a.getAnimatedValue());
        mAnimator3.start();
    }

    private void stopAnimators() {
        if (mAnimator1 != null) mAnimator1.cancel();
        if (mAnimator2 != null) mAnimator2.cancel();
        if (mAnimator3 != null) mAnimator3.cancel();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        drawWave(canvas, mPath2, mPaint2, w, h, mOffset2, h * 0.48f, mWaveHeight * 1.1f, mWaveLength * 1.3f);
        drawWave(canvas, mPath3, mPaint3, w, h, mOffset3, h * 0.52f, mWaveHeight * 0.7f, mWaveLength * 0.9f);
        drawWave(canvas, mPath1, mPaint1, w, h, mOffset1, h * 0.56f, mWaveHeight, mWaveLength);
    }

    private void drawWave(Canvas canvas, Path path, Paint paint,
                          int w, int h, float offset, float baseline,
                          float amplitude, float waveLen) {
        path.reset();
        path.moveTo(-waveLen + offset, baseline);

        float x = -waveLen + offset;
        while (x < w + waveLen) {
            path.rQuadTo(waveLen / 4f, -amplitude, waveLen / 2f, 0);
            path.rQuadTo(waveLen / 4f,  amplitude, waveLen / 2f, 0);
            x += waveLen;
        }

        path.lineTo(w + waveLen, h);
        path.lineTo(-waveLen + offset, h);
        path.close();

        canvas.drawPath(path, paint);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mWaveLength > 0) startAnimators();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimators();
    }
}
