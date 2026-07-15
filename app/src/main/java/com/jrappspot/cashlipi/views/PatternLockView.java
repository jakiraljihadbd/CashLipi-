package com.jrappspot.cashlipi.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PatternLockView extends View {

    public interface OnPatternListener {
        void onPatternComplete(List<Integer> pattern);
        void onPatternStarted();
    }

    private static final int DOTS = 3;
    private static final float DOT_RADIUS_RATIO = 0.065f;
    private static final float CELL_RATIO = 1f / DOTS;

    private final Paint dotPaintNormal = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaintSelected = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaintError = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final float[][] dotCenters = new float[9][2];
    private final boolean[] selected = new boolean[9];
    private final List<Integer> pattern = new ArrayList<>();

    private float touchX, touchY;
    private boolean touching = false;
    private boolean errorMode = false;

    private OnPatternListener listener;
    private int primaryColor = 0xFF6366F1;
    private int errorColor  = 0xFFEF4444;

    public PatternLockView(Context ctx) { super(ctx); init(); }
    public PatternLockView(Context ctx, AttributeSet a) { super(ctx, a); init(); }
    public PatternLockView(Context ctx, AttributeSet a, int s) { super(ctx, a, s); init(); }

    private void init() {
        dotPaintNormal.setColor(0xFFD1D5DB);
        dotPaintNormal.setStyle(Paint.Style.FILL);
        dotPaintSelected.setColor(primaryColor);
        dotPaintSelected.setStyle(Paint.Style.FILL);
        dotPaintError.setColor(errorColor);
        dotPaintError.setStyle(Paint.Style.FILL);
        linePaint.setColor(primaryColor);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(6f);
        linePaint.setAlpha(180);
        centerPaint.setColor(ContextCompat.getColor(getContext(), com.jrappspot.cashlipi.R.color.white));
        centerPaint.setStyle(Paint.Style.FILL);
    }

    public void setOnPatternListener(OnPatternListener l) { listener = l; }

    public void setError() {
        errorMode = true;
        dotPaintSelected.setColor(errorColor);
        linePaint.setColor(errorColor);
        invalidate();
        postDelayed(() -> {
            reset();
            dotPaintSelected.setColor(primaryColor);
            linePaint.setColor(primaryColor);
            invalidate();
        }, 800);
    }

    public void reset() {
        pattern.clear();
        for (int i = 0; i < 9; i++) selected[i] = false;
        touching = false;
        errorMode = false;
        invalidate();
    }

    public List<Integer> getPattern() { return new ArrayList<>(pattern); }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        float cellW = w * CELL_RATIO;
        float cellH = h * CELL_RATIO;
        for (int r = 0; r < DOTS; r++) {
            for (int c = 0; c < DOTS; c++) {
                int idx = r * DOTS + c;
                dotCenters[idx][0] = cellW * c + cellW / 2f;
                dotCenters[idx][1] = cellH * r + cellH / 2f;
            }
        }
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int size = Math.min(MeasureSpec.getSize(wSpec), MeasureSpec.getSize(hSpec));
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float dotR = getWidth() * DOT_RADIUS_RATIO;
        float innerR = dotR * 0.45f;
        Path path = new Path();
        boolean first = true;
        for (int idx : pattern) {
            if (first) { path.moveTo(dotCenters[idx][0], dotCenters[idx][1]); first = false; }
            else path.lineTo(dotCenters[idx][0], dotCenters[idx][1]);
        }
        if (touching && !pattern.isEmpty()) {
            path.lineTo(touchX, touchY);
        }
        canvas.drawPath(path, linePaint);
        for (int i = 0; i < 9; i++) {
            float cx = dotCenters[i][0], cy = dotCenters[i][1];
            if (selected[i]) {
                Paint p = errorMode ? dotPaintError : dotPaintSelected;
                canvas.drawCircle(cx, cy, dotR, p);
                canvas.drawCircle(cx, cy, innerR, centerPaint);
            } else {
                canvas.drawCircle(cx, cy, dotR, dotPaintNormal);
                canvas.drawCircle(cx, cy, innerR * 0.7f, centerPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX(), y = e.getY();
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                reset();
                if (listener != null) listener.onPatternStarted();
                touching = true;
                checkHit(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                touchX = x; touchY = y;
                checkHit(x, y);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                touching = false;
                if (!pattern.isEmpty() && listener != null) {
                    listener.onPatternComplete(new ArrayList<>(pattern));
                }
                break;
        }
        invalidate();
        return true;
    }

    private void checkHit(float x, float y) {
        float dotR = getWidth() * DOT_RADIUS_RATIO * 2.2f;
        for (int i = 0; i < 9; i++) {
            if (!selected[i]) {
                float dx = x - dotCenters[i][0], dy = y - dotCenters[i][1];
                if (dx * dx + dy * dy <= dotR * dotR) {
                    selected[i] = true;
                    pattern.add(i);
                }
            }
        }
    }
}
