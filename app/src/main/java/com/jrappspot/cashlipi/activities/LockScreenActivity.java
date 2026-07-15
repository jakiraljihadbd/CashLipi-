package com.jrappspot.cashlipi.activities;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.utils.DatabaseManager;
import com.jrappspot.cashlipi.utils.LockUtils;
import com.jrappspot.cashlipi.views.PatternLockView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class LockScreenActivity extends BaseActivity {

    private DatabaseManager db;
    private LinearLayout layoutPin, layoutPattern, layoutFingerprint;
    private TextView tvError, tvTitle, tvSubtitle, tvForgotPin;
    private ImageView btnFingerprint;
    private View fingerprintPulseRing;
    private AnimatorSet pulseAnimatorSet;
    private BiometricPrompt biometricPrompt; // held so it can be cancelled safely on stop/destroy
    private PatternLockView patternLockView;
    private View[] pinDots;
    private View[] keypadKeys;
    private String lockType;
    private int failCount = 0;
    private int secFailCount = 0;

    // PIN এখন system keyboard দিয়ে না, custom keypad দিয়ে টাইপ হয়
    private final StringBuilder pinBuilder = new StringBuilder();

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_lock_screen);
        db = DatabaseManager.getInstance(this);
        lockType = db.getLockType();

        tvTitle           = findViewById(R.id.tvLockTitle);
        tvSubtitle        = findViewById(R.id.tvLockSubtitle);
        tvError           = findViewById(R.id.tvLockError);
        tvForgotPin       = findViewById(R.id.tvForgotPin);
        layoutPin         = findViewById(R.id.layoutPinInput);
        layoutPattern     = findViewById(R.id.layoutPatternInput);
        layoutFingerprint = findViewById(R.id.layoutFingerprint);
        btnFingerprint    = findViewById(R.id.btnFingerprint);
        fingerprintPulseRing = findViewById(R.id.fingerprintPulseRing);
        patternLockView   = findViewById(R.id.patternLockView);

        pinDots = new View[]{
            findViewById(R.id.dotPin1), findViewById(R.id.dotPin2),
            findViewById(R.id.dotPin3), findViewById(R.id.dotPin4)
        };

        if (DatabaseManager.LOCK_PATTERN.equals(lockType)) {
            layoutPin.setVisibility(View.GONE);
            findViewById(R.id.keypad).setVisibility(View.GONE);
            layoutPattern.setVisibility(View.VISIBLE);
            tvSubtitle.setText("প্যাটার্ন এঁকে আনলক করুন");
            tvForgotPin.setText("প্যাটার্ন ভুলে গেছেন?");
            setupPattern();
        } else {
            layoutPin.setVisibility(View.VISIBLE);
            findViewById(R.id.keypad).setVisibility(View.VISIBLE);
            layoutPattern.setVisibility(View.GONE);
            tvSubtitle.setText("PIN দিয়ে আনলক করুন");
            tvForgotPin.setText("PIN ভুলে গেছেন?");
            setupKeypad();
        }

        tvForgotPin.setOnClickListener(v -> startSecurityQuestionReset());

        // FIX: ফিঙ্গারপ্রিন্ট থাকলেও এখন আর স্ক্রিন খোলার সাথে সাথে system popup
        // auto-launch হয় না। শুধু green fingerprint আইকনটা দেখাবে —
        // popup তখনই আসবে যখন ইউজার নিজে আইকনে ট্যাপ করবে। এই popup আসলে
        // Android OS-ই দেখায় (BiometricPrompt-এর নিরাপত্তা নীতি অনুযায়ী,
        // অ্যাপ চাইলেও এটা সম্পূর্ণ popup-বিহীন করা সম্ভব না) — তবে ট্যাপ করার
        // সাথে সাথেই সেন্সরে আঙুল দিলে সাথে সাথে আনলক হয়ে যাবে, extra ধাপ নেই।
        if (db.isFingerprintEnabled() && canUseBiometric()) {
            layoutFingerprint.setVisibility(View.VISIBLE);
            layoutFingerprint.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
            btnFingerprint.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                showBiometricPrompt();
            });
            startFingerprintPulse();
        } else {
            layoutFingerprint.setVisibility(View.GONE);
        }
    }

    /** Soft breathing scale+fade loop around the fingerprint icon — purely decorative, no popup involved. */
    private void startFingerprintPulse() {
        if (fingerprintPulseRing == null) return;
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(fingerprintPulseRing, View.SCALE_X, 1f, 1.18f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(fingerprintPulseRing, View.SCALE_Y, 1f, 1.18f);
        ObjectAnimator alpha  = ObjectAnimator.ofFloat(fingerprintPulseRing, View.ALPHA, 0.9f, 0f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY, alpha);
        set.setDuration(1400);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                fingerprintPulseRing.setScaleX(1f);
                fingerprintPulseRing.setScaleY(1f);
                fingerprintPulseRing.setAlpha(0.9f);
                if (!isFinishing() && !isDestroyed()) animation.start();
            }
        });
        pulseAnimatorSet = set;
        set.start();
    }

    private void stopFingerprintPulse() {
        if (pulseAnimatorSet != null) {
            pulseAnimatorSet.removeAllListeners();
            pulseAnimatorSet.cancel();
            pulseAnimatorSet = null;
        }
    }

    // ═══════════════ Custom Numeric Keypad ═══════════════
    private void setupKeypad() {
        int[] digitIds = {R.id.keyPad0, R.id.keyPad1, R.id.keyPad2, R.id.keyPad3, R.id.keyPad4,
                           R.id.keyPad5, R.id.keyPad6, R.id.keyPad7, R.id.keyPad8, R.id.keyPad9};
        String[] digits = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};

        keypadKeys = new View[digitIds.length + 1];
        for (int i = 0; i < digitIds.length; i++) {
            final String digit = digits[i];
            View key = findViewById(digitIds[i]);
            keypadKeys[i] = key;
            key.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                onDigit(digit);
            });
        }

        View backspace = findViewById(R.id.keyBackspace);
        keypadKeys[digitIds.length] = backspace;
        backspace.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            if (pinBuilder.length() > 0) {
                pinBuilder.deleteCharAt(pinBuilder.length() - 1);
                updatePinDots(pinBuilder.length());
                tvError.setVisibility(View.GONE);
            }
        });
    }

    private void onDigit(String digit) {
        if (pinBuilder.length() >= 4) return;
        tvError.setVisibility(View.GONE);
        pinBuilder.append(digit);
        updatePinDots(pinBuilder.length());
        if (pinBuilder.length() == 4) checkPin(pinBuilder.toString());
    }

    private void updatePinDots(int filledCount) {
        for (int i = 0; i < pinDots.length; i++) {
            pinDots[i].setBackgroundResource(
                i < filledCount ? R.drawable.shape_pin_dot_filled : R.drawable.shape_pin_dot_empty);
        }
    }

    private void checkPin(String pin) {
        if (LockUtils.verify(pin, db.getLockSecret())) {
            unlock();
        } else {
            failCount++;
            tvError.setText(" ভুল PIN! (" + failCount + "/5)");
            tvError.setVisibility(View.VISIBLE);
            shakeView(layoutPin);
            pinBuilder.setLength(0);
            updatePinDots(0);
            if (failCount >= 5) showLockoutMessage();
        }
    }

    private void setupPattern() {
        patternLockView.setOnPatternListener(new PatternLockView.OnPatternListener() {
            public void onPatternStarted() { tvError.setVisibility(View.GONE); }
            public void onPatternComplete(List<Integer> pattern) {
                if (pattern.size() < 3) {
                    tvError.setText("অন্তত ৩টি বিন্দু সংযুক্ত করুন");
                    tvError.setVisibility(View.VISIBLE);
                    patternLockView.setError();
                    return;
                }
                String input = LockUtils.patternToString(pattern);
                if (LockUtils.verify(input, db.getLockSecret())) {
                    unlock();
                } else {
                    failCount++;
                    tvError.setText(" ভুল প্যাটার্ন! (" + failCount + "/5)");
                    tvError.setVisibility(View.VISIBLE);
                    patternLockView.setError();
                    shakeView(layoutPattern);
                    if (failCount >= 5) showLockoutMessage();
                }
            }
        });
    }

    private void shakeView(View v) {
        if (v != null) v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake_error));
    }

    // ═══════════════ Forgot PIN — নতুন Material 3 রিসেট ফ্লো (শুধু ONE সিকিউরিটি প্রশ্ন) ═══════════════
    private void startSecurityQuestionReset() {
        if (!db.hasSecurityQuestion()) {
            new AlertDialog.Builder(this, R.style.AppDialog)
                .setTitle("সিকিউরিটি প্রশ্ন সেট নেই")
                .setMessage("এই লকের জন্য কোনো সিকিউরিটি প্রশ্নের উত্তর সেভ করা নেই, তাই এখান থেকে রিসেট করা যাবে না। "
                    + "App Lock সেটিংসে গিয়ে PIN নতুন করে সেট করার সময় একটা সিকিউরিটি প্রশ্নের উত্তর দিন — "
                    + "তাহলে পরের বার ভুলে গেলে এখান থেকেই রিসেট করা যাবে।")
                .setPositiveButton("ঠিক আছে", null)
                .show();
            return;
        }
        startActivity(new Intent(this, ResetPinActivity.class));
    }

    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt.AuthenticationCallback cb = new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult r) {
                unlock();
            }

            @Override
            public void onAuthenticationError(int code, CharSequence msg) {
                // শুধু user cancel হলে error দেখাবে না
                if (code != BiometricPrompt.ERROR_USER_CANCELED
                        && code != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    tvError.setText("ফিঙ্গারপ্রিন্ট ব্যর্থ: " + msg);
                    tvError.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onAuthenticationFailed() {
                tvError.setText("ফিঙ্গারপ্রিন্ট মিলছে না, আবার চেষ্টা করুন");
                tvError.setVisibility(View.VISIBLE);
                shakeView(layoutFingerprint);
            }
        };

        biometricPrompt = new BiometricPrompt(this, executor, cb);
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
            .setTitle("CashLipi ক্যাশলিপি আনলক")
            .setSubtitle("ফিঙ্গারপ্রিন্ট স্ক্যান করুন")
            .setNegativeButtonText("PIN/প্যাটার্ন ব্যবহার করুন")
            .build();
        biometricPrompt.authenticate(info);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Cancel any in-flight biometric auth so no callback fires after the activity is gone (avoids leaks/crashes).
        if (biometricPrompt != null) {
            biometricPrompt.cancelAuthentication();
        }
    }

    @Override
    protected void onDestroy() {
        stopFingerprintPulse();
        super.onDestroy();
    }

    private boolean canUseBiometric() {
        BiometricManager bm = BiometricManager.from(this);
        return bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
               == BiometricManager.BIOMETRIC_SUCCESS;
    }

    private void unlock() {
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    private void showLockoutMessage() {
        tvError.setText(" অনেকবার ভুল হয়েছে। পরে চেষ্টা করুন।");
        tvError.setVisibility(View.VISIBLE);
        if (DatabaseManager.LOCK_PATTERN.equals(lockType)) {
            patternLockView.setEnabled(false);
        } else if (keypadKeys != null) {
            for (View key : keypadKeys) { key.setEnabled(false); key.setAlpha(0.4f); }
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
