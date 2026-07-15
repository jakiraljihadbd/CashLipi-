package com.jrappspot.cashlipi.activities;

import android.content.Intent;
import android.text.TextUtils;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.utils.DatabaseManager;
import com.jrappspot.cashlipi.utils.LockUtils;

/**
 * ResetPinActivity — "PIN ভুলে গেছেন?" ফ্লো (Material 3)।
 *
 * শুধুমাত্র সেটআপের সময় বাছাই করা ONE সিকিউরিটি প্রশ্নই জিজ্ঞাসা করা হয়
 * (কখনো ৩টা প্রশ্ন না) — যাচাই সফল হলে নতুন PIN সেট + কনফার্ম করানো হয়।
 */
public class ResetPinActivity extends BaseActivity {

    private enum Step { VERIFY, PIN_NEW, PIN_CONFIRM, DONE }

    private DatabaseManager db;
    private Step currentStep = Step.VERIFY;
    private int failCount = 0;

    private TextView tvStepTitle, tvSavedQuestion, tvPinTitle, tvPinSubtitle, tvPinError;
    private View stepVerify, stepPin, stepDone;
    private TextInputLayout layoutVerifyAnswer;
    private TextInputEditText etVerifyAnswer;
    private MaterialButton btnVerify, btnDone;
    private View[] pinDots;

    private final StringBuilder pinBuilder = new StringBuilder();
    private String tempNewPin = null;

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_pin);
        db = DatabaseManager.getInstance(this);

        if (!db.hasSecurityQuestion()) {
            new androidx.appcompat.app.AlertDialog.Builder(this, R.style.AppDialog)
                .setTitle("সিকিউরিটি প্রশ্ন সেট নেই")
                .setMessage("এই লকের জন্য কোনো সিকিউরিটি প্রশ্নের উত্তর সেভ করা নেই, তাই এখান থেকে রিসেট করা যাবে না। "
                    + "App Lock সেটিংসে গিয়ে PIN নতুন করে সেট করার সময় একটা সিকিউরিটি প্রশ্নের উত্তর দিন — "
                    + "তাহলে পরের বার ভুলে গেলে এখান থেকেই রিসেট করা যাবে।")
                .setPositiveButton("ঠিক আছে", (d, w) -> finish())
                .setCancelable(false)
                .show();
            return;
        }

        bindViews();
        wireListeners();

        int qIndex = db.getSecurityQuestionIndex();
        String question = (qIndex >= 0 && qIndex < DatabaseManager.SECURITY_QUESTIONS.length)
            ? DatabaseManager.SECURITY_QUESTIONS[qIndex] : "";
        tvSavedQuestion.setText(question);

        renderStep();
    }

    private void bindViews() {
        tvStepTitle    = findViewById(R.id.tvStepTitle);
        stepVerify     = findViewById(R.id.stepVerify);
        stepPin        = findViewById(R.id.stepPin);
        stepDone       = findViewById(R.id.stepDone);
        tvSavedQuestion = findViewById(R.id.tvSavedQuestion);
        layoutVerifyAnswer = findViewById(R.id.layoutVerifyAnswer);
        etVerifyAnswer = findViewById(R.id.etVerifyAnswer);
        btnVerify      = findViewById(R.id.btnVerify);

        tvPinTitle    = findViewById(R.id.tvPinTitle);
        tvPinSubtitle = findViewById(R.id.tvPinSubtitle);
        tvPinError    = findViewById(R.id.tvPinError);
        pinDots = new View[]{
            findViewById(R.id.dotPin1), findViewById(R.id.dotPin2),
            findViewById(R.id.dotPin3), findViewById(R.id.dotPin4)
        };

        int[] digitIds = {R.id.keyPad0, R.id.keyPad1, R.id.keyPad2, R.id.keyPad3, R.id.keyPad4,
                           R.id.keyPad5, R.id.keyPad6, R.id.keyPad7, R.id.keyPad8, R.id.keyPad9};
        String[] digits = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
        for (int i = 0; i < digitIds.length; i++) {
            final String digit = digits[i];
            View key = findViewById(digitIds[i]);
            key.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                onDigit(digit);
            });
        }
        View backspace = findViewById(R.id.keyBackspace);
        backspace.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            if (pinBuilder.length() > 0) {
                pinBuilder.deleteCharAt(pinBuilder.length() - 1);
                updatePinDots(pinBuilder.length(), false);
                tvPinError.setVisibility(View.INVISIBLE);
            }
        });

        btnDone = findViewById(R.id.btnDone);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void wireListeners() {
        btnVerify.setOnClickListener(v -> onVerify());
        btnDone.setOnClickListener(v -> {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void renderStep() {
        stepVerify.setVisibility(currentStep == Step.VERIFY ? View.VISIBLE : View.GONE);
        stepPin.setVisibility((currentStep == Step.PIN_NEW || currentStep == Step.PIN_CONFIRM) ? View.VISIBLE : View.GONE);
        stepDone.setVisibility(currentStep == Step.DONE ? View.VISIBLE : View.GONE);

        switch (currentStep) {
            case VERIFY:
                tvStepTitle.setText("Verify Identity");
                break;
            case PIN_NEW:
                tvStepTitle.setText("Set New PIN");
                tvPinTitle.setText("Create New PIN");
                tvPinSubtitle.setText("Enter a new 4-digit PIN.");
                pinBuilder.setLength(0);
                updatePinDots(0, false);
                tvPinError.setVisibility(View.INVISIBLE);
                break;
            case PIN_CONFIRM:
                tvStepTitle.setText("Confirm New PIN");
                tvPinTitle.setText("Confirm New PIN");
                tvPinSubtitle.setText("Re-enter the 4-digit PIN to confirm.");
                pinBuilder.setLength(0);
                updatePinDots(0, false);
                tvPinError.setVisibility(View.INVISIBLE);
                break;
            case DONE:
                break;
        }
    }

    private void onVerify() {
        String answer = etVerifyAnswer.getText() != null ? etVerifyAnswer.getText().toString() : "";
        if (TextUtils.isEmpty(answer.trim())) {
            layoutVerifyAnswer.setError("উত্তর লিখুন");
            return;
        }
        if (db.verifySecurityAnswer(answer)) {
            layoutVerifyAnswer.setError(null);
            currentStep = Step.PIN_NEW;
            renderStep();
        } else {
            failCount++;
            layoutVerifyAnswer.setError("Incorrect Answer");
            stepVerify.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake_error));
            if (failCount >= 5) {
                Toast.makeText(this, "অনেকবার ভুল হয়েছে। পরে চেষ্টা করুন।", Toast.LENGTH_LONG).show();
                btnVerify.setEnabled(false);
            }
        }
    }

    // ═══════════════════ PIN keypad ═══════════════════

    private void onDigit(String digit) {
        if (pinBuilder.length() >= 4) return;
        tvPinError.setVisibility(View.INVISIBLE);
        pinBuilder.append(digit);
        updatePinDots(pinBuilder.length(), false);
        if (pinBuilder.length() == 4) {
            String entered = pinBuilder.toString();
            stepPin.postDelayed(() -> handlePinComplete(entered), 150);
        }
    }

    private void updatePinDots(int filledCount, boolean error) {
        for (int i = 0; i < pinDots.length; i++) {
            int res = error
                ? R.drawable.shape_applock_pin_dot_error
                : (i < filledCount ? R.drawable.shape_applock_pin_dot_filled : R.drawable.shape_applock_pin_dot_empty);
            pinDots[i].setBackgroundResource(res);
        }
    }

    private void handlePinComplete(String entered) {
        if (currentStep == Step.PIN_NEW) {
            tempNewPin = entered;
            currentStep = Step.PIN_CONFIRM;
            renderStep();
        } else if (currentStep == Step.PIN_CONFIRM) {
            if (entered.equals(tempNewPin)) {
                // ফিঙ্গারপ্রিন্ট সেটিং অপরিবর্তিত রেখে শুধু নতুন PIN সেভ করা হচ্ছে।
                db.saveLock(DatabaseManager.LOCK_PIN, LockUtils.hash(tempNewPin), db.isFingerprintEnabled());
                currentStep = Step.DONE;
                renderStep();
            } else {
                tvPinError.setText("PIN মেলেনি! আবার চেষ্টা করুন");
                tvPinError.setVisibility(View.VISIBLE);
                updatePinDots(4, true);
                stepPin.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake_error));
                stepPin.postDelayed(() -> {
                    tempNewPin = null;
                    currentStep = Step.PIN_NEW;
                    renderStep();
                }, 450);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (currentStep == Step.VERIFY) {
            super.onBackPressed();
        } else {
            // সেটআপের মাঝে ফিরে গেলে অসম্পূর্ণ অবস্থায় লক আউট এড়াতে সরাসরি বন্ধ করা হচ্ছে না —
            // ব্যবহারকারীকে ফ্লো শেষ করতে বলা হচ্ছে।
            Toast.makeText(this, "নতুন PIN সেট করা শেষ করুন", Toast.LENGTH_SHORT).show();
        }
    }
}
