package com.jrappspot.cashlipi.views;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * পিঞ্চ-জুম, ডাবল-ট্যাপ-জুম ও প্যান সাপোর্ট করা ImageView — কোনো এক্সটার্নাল লাইব্রেরি ছাড়াই।
 * "PDF এক্সপোর্ট" পেজের লাইভ প্রিভিউ ফুলস্ক্রিন জুম ডায়ালগে ব্যবহৃত হয় (InvoiceExportActivity)।
 *
 * ব্যবহার: setImageBitmap()/setImageDrawable() দিয়ে ছবি সেট করার পর layout সম্পন্ন হলে
 * ছবিটি স্বয়ংক্রিয়ভাবে ভিউতে fit হয়ে বসে; এরপর ইউজার পিঞ্চ করে জুম-ইন/আউট এবং টেনে (pan)
 * সরাতে পারে, বা ডাবল-ট্যাপ করে দ্রুত জুম-ইন/ফিট-ব্যাক করতে পারে।
 */
public class ZoomableImageView extends AppCompatImageView {

    private final Matrix matrix = new Matrix();

    private static final float MIN_REL_SCALE = 1f;   // fit-to-screen বেসলাইন
    private static final float MAX_REL_SCALE = 6f;
    private static final float DOUBLE_TAP_SCALE = 2.6f;

    private float currentRelScale = 1f; // fit-to-screen অবস্থার সাপেক্ষে বর্তমান জুম

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    private int bmpW, bmpH;
    private float startX, startY;
    private int activePointerCount;

    public ZoomableImageView(Context context) {
        super(context);
        init(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context ctx) {
        setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(ctx, new ScaleListener());
        gestureDetector = new GestureDetector(ctx, new GestureListener());
        setOnTouchListener((v, event) -> {
            scaleDetector.onTouchEvent(event);
            gestureDetector.onTouchEvent(event);
            handlePan(event);
            return true;
        });
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) fitToScreen();
    }

    /** ছবিটি ভিউয়ের মাঝখানে, পুরোপুরি দেখা যায় এমনভাবে বসায় — জুম রিসেট করে দেয়। */
    public void fitToScreen() {
        if (getDrawable() == null) return;
        bmpW = getDrawable().getIntrinsicWidth();
        bmpH = getDrawable().getIntrinsicHeight();
        int vw = getWidth(), vh = getHeight();
        if (bmpW == 0 || bmpH == 0 || vw == 0 || vh == 0) return;

        float scale = Math.min(vw / (float) bmpW, vh / (float) bmpH);
        matrix.reset();
        matrix.postScale(scale, scale);
        matrix.postTranslate((vw - bmpW * scale) / 2f, (vh - bmpH * scale) / 2f);
        currentRelScale = 1f;
        setImageMatrix(matrix);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float factor = detector.getScaleFactor();
            float newRel = currentRelScale * factor;
            if (newRel < MIN_REL_SCALE) factor = MIN_REL_SCALE / currentRelScale;
            else if (newRel > MAX_REL_SCALE) factor = MAX_REL_SCALE / currentRelScale;
            currentRelScale = Math.max(MIN_REL_SCALE, Math.min(MAX_REL_SCALE, newRel));
            matrix.postScale(factor, factor, detector.getFocusX(), detector.getFocusY());
            constrainMatrix();
            setImageMatrix(matrix);
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (currentRelScale > 1.05f) {
                fitToScreen();
            } else {
                float factor = DOUBLE_TAP_SCALE / currentRelScale;
                matrix.postScale(factor, factor, e.getX(), e.getY());
                currentRelScale = DOUBLE_TAP_SCALE;
                constrainMatrix();
                setImageMatrix(matrix);
            }
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
    }

    private void handlePan(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                startY = event.getY();
                activePointerCount = 1;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                activePointerCount = event.getPointerCount();
                break;
            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() == 1 && activePointerCount == 1
                        && !scaleDetector.isInProgress() && currentRelScale > 1.02f) {
                    float dx = event.getX() - startX;
                    float dy = event.getY() - startY;
                    matrix.postTranslate(dx, dy);
                    constrainMatrix();
                    setImageMatrix(matrix);
                    startX = event.getX();
                    startY = event.getY();
                }
                break;
            default:
                break;
        }
    }

    /** ছবি ভিউয়ের বাইরে চলে যাওয়া ঠেকায় — জুম/প্যান করলেও কিনারা সবসময় ভিউয়ের মধ্যে থাকে। */
    private void constrainMatrix() {
        if (bmpW == 0 || bmpH == 0) return;
        RectF rect = new RectF(0, 0, bmpW, bmpH);
        matrix.mapRect(rect);
        int vw = getWidth(), vh = getHeight();

        float dx = 0, dy = 0;
        if (rect.width() <= vw) dx = (vw - rect.width()) / 2f - rect.left;
        else if (rect.left > 0) dx = -rect.left;
        else if (rect.right < vw) dx = vw - rect.right;

        if (rect.height() <= vh) dy = (vh - rect.height()) / 2f - rect.top;
        else if (rect.top > 0) dy = -rect.top;
        else if (rect.bottom < vh) dy = vh - rect.bottom;

        matrix.postTranslate(dx, dy);
    }
}
