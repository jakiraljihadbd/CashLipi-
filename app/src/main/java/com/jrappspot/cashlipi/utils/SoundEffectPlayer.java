package com.jrappspot.cashlipi.utils;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import com.jrappspot.cashlipi.R;

/**
 * আয়/ব্যয় টগল করলে বা সংরক্ষণ করলে বাজবে এমন একটাই শেয়ার্ড সাউন্ড প্লেয়ার —
 * যাতে একাধিক জায়গা থেকে ব্যবহার করলেও নতুন করে সাউন্ড লোড না হয়।
 */
public class SoundEffectPlayer {

    private static SoundEffectPlayer instance;

    private final SoundPool soundPool;
    private int soundId = -1;
    private boolean loaded = false;

    private SoundEffectPlayer(Context appContext) {
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder().setMaxStreams(3).setAudioAttributes(attrs).build();
        soundPool.setOnLoadCompleteListener((sp, sampleId, status) -> loaded = status == 0);
        soundId = soundPool.load(appContext, R.raw.sound_save_tap, 1);
    }

    public static synchronized SoundEffectPlayer getInstance(Context context) {
        if (instance == null) {
            instance = new SoundEffectPlayer(context.getApplicationContext());
        }
        return instance;
    }

    public void playTap() {
        if (loaded) {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f);
        }
    }
}
