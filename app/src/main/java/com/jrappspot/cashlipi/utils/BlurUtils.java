package com.jrappspot.cashlipi.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;

/**
 * হালকা, নির্ভরযোগ্য ব্লার ইউটিলিটি — কোনো RenderScript/API31+ নির্ভরতা নেই,
 * তাই minSdk 23 থেকে সব ডিভাইসে কাজ করবে। একটি ভিউকে বিটম্যাপে আঁকা হয়,
 * ছোট স্কেলে নামিয়ে দ্রুত বক্স-ব্লার (৩ পাস) করা হয়, যাতে পারফরম্যান্স ভালো থাকে।
 */
public class BlurUtils {

    /** দেওয়া ভিউটিকে ক্যাপচার করে একটি ব্লার করা বিটম্যাপ রিটার্ন করে। */
    public static Bitmap blurredSnapshot(View view, float downscale, int radius) {
        int w = Math.max(1, view.getWidth());
        int h = Math.max(1, view.getHeight());

        Bitmap full = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(full);
        view.draw(canvas);

        int sw = Math.max(1, Math.round(w * downscale));
        int sh = Math.max(1, Math.round(h * downscale));
        Bitmap small = Bitmap.createScaledBitmap(full, sw, sh, true);
        full.recycle();

        boxBlur(small, radius);
        return small;
    }

    /** সরল, দ্রুত ইন-প্লেস বক্স ব্লার (হরাইজন্টাল + ভার্টিক্যাল পাস, ৩ বার — গসিয়ানের কাছাকাছি ফলাফল)। */
    private static void boxBlur(Bitmap bmp, int radius) {
        if (radius < 1) return;
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int[] pixels = new int[w * h];
        bmp.getPixels(pixels, 0, w, 0, 0, w, h);

        for (int pass = 0; pass < 3; pass++) {
            horizontalPass(pixels, w, h, radius);
            verticalPass(pixels, w, h, radius);
        }

        bmp.setPixels(pixels, 0, w, 0, 0, w, h);
    }

    private static void horizontalPass(int[] pixels, int w, int h, int radius) {
        int[] result = new int[pixels.length];
        for (int y = 0; y < h; y++) {
            int rowStart = y * w;
            long sumA = 0, sumR = 0, sumG = 0, sumB = 0;
            int count = 0;
            for (int x = -radius; x <= radius; x++) {
                int xi = clamp(x, 0, w - 1);
                int p = pixels[rowStart + xi];
                sumA += (p >>> 24) & 0xFF;
                sumR += (p >>> 16) & 0xFF;
                sumG += (p >>> 8) & 0xFF;
                sumB += p & 0xFF;
                count++;
            }
            for (int x = 0; x < w; x++) {
                result[rowStart + x] = ((int) (sumA / count) << 24) | ((int) (sumR / count) << 16)
                        | ((int) (sumG / count) << 8) | (int) (sumB / count);

                int addX = clamp(x + radius + 1, 0, w - 1);
                int removeX = clamp(x - radius, 0, w - 1);
                int addP = pixels[rowStart + addX];
                int remP = pixels[rowStart + removeX];
                sumA += ((addP >>> 24) & 0xFF) - ((remP >>> 24) & 0xFF);
                sumR += ((addP >>> 16) & 0xFF) - ((remP >>> 16) & 0xFF);
                sumG += ((addP >>> 8) & 0xFF) - ((remP >>> 8) & 0xFF);
                sumB += (addP & 0xFF) - (remP & 0xFF);
            }
        }
        System.arraycopy(result, 0, pixels, 0, pixels.length);
    }

    private static void verticalPass(int[] pixels, int w, int h, int radius) {
        int[] result = new int[pixels.length];
        for (int x = 0; x < w; x++) {
            long sumA = 0, sumR = 0, sumG = 0, sumB = 0;
            int count = 0;
            for (int y = -radius; y <= radius; y++) {
                int yi = clamp(y, 0, h - 1);
                int p = pixels[yi * w + x];
                sumA += (p >>> 24) & 0xFF;
                sumR += (p >>> 16) & 0xFF;
                sumG += (p >>> 8) & 0xFF;
                sumB += p & 0xFF;
                count++;
            }
            for (int y = 0; y < h; y++) {
                result[y * w + x] = ((int) (sumA / count) << 24) | ((int) (sumR / count) << 16)
                        | ((int) (sumG / count) << 8) | (int) (sumB / count);

                int addY = clamp(y + radius + 1, 0, h - 1);
                int removeY = clamp(y - radius, 0, h - 1);
                int addP = pixels[addY * w + x];
                int remP = pixels[removeY * w + x];
                sumA += ((addP >>> 24) & 0xFF) - ((remP >>> 24) & 0xFF);
                sumR += ((addP >>> 16) & 0xFF) - ((remP >>> 16) & 0xFF);
                sumG += ((addP >>> 8) & 0xFF) - ((remP >>> 8) & 0xFF);
                sumB += (addP & 0xFF) - (remP & 0xFF);
            }
        }
        System.arraycopy(result, 0, pixels, 0, pixels.length);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
