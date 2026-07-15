package com.jrappspot.cashlipi.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.text.TextUtils;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.utils.DatabaseManager;
import com.jrappspot.cashlipi.utils.LockUtils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executor;

/**
 * AppLockActivity — App Lock Setup Wizard (Material 3 / Material You).
 *
 * ধাপগুলো: Intro → Create PIN → Confirm PIN → Security Question (ONE, optional)
 *          → Fingerprint (optional) → Success.
 *
 * ডিজাইন নোট: PIN দুইবার (create + confirm) লেখার জন্য একটাই "stepPin" লেআউট
 * পুনরায় ব্যবহার করা হয়েছে (টাইটেল/সাবটাইটেল আর টার্গেট পরিবর্তন হয় কোডে) —
 * এতে দুইটা প্রায়-অভিন্ন কীপ্যাড লেআউট ডুপ্লিকেট করতে হয়নি।
 */
public class AppLockActivity extends BaseActivity {

    private enum Step { INTRO, PIN_CREATE, PIN_CONFIRM, QUESTION, FINGERPRINT, SUCCESS }

    private DatabaseManager db;
    private Step currentStep = Step.INTRO;
    private final Deque<Step> backStack = new ArrayDeque<>();

    // Top bar
    private View layoutTopBar;
    private TextView tvStepTitle, tvSkip;
    private View layoutStepDots;
    private View dotStep1, dotStep2, dotStep3, dotStep4;

    // Steps
    private View stepIntro, stepPin, stepQuestion, stepFingerprint, stepSuccess;

    // Intro
    private ImageView ivIntroShield;
    private TextView tvIntroTitle, tvIntroSubtitle;
    private MaterialButton btnStartSetup, btnSkipIntro, btnDisableLock;

    // PIN (shared between create + confirm)
    private TextView tvPinTitle, tvPinSubtitle, tvPinError;
    private View[] pinDots;
    private View[] keypadKeys;
    private final StringBuilder pinBuilder = new StringBuilder();
    private String tempNewPin = null;

    // Security question
    private RadioGroup radioGroupQuestions;
    private RadioButton[] radioQuestions;
    private TextInputLayout layoutAnswer;
    private TextInputEditText etAnswer;
    private MaterialButton btnNextQuestion;
    private int selectedQuestionIndex = -1;

    // Fingerprint
    private SwitchCompat switchFingerprint;
    private TextView tvFingerprintUnavailable;
    private MaterialButton btnBackFingerprint, btnFinish;
    private boolean biometricAvailable = false;
    private BiometricPrompt biometricPrompt;

    // Success
    private MaterialButton btnGoToDashboard;

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_lock);
        db = DatabaseManager.getInstance(this);

        bindViews();
        wireListeners();
        renderStep();
    }

    private void bindViews() {
        layoutTopBar   = findViewById(R.id.layoutTopBar);
        tvStepTitle    = findViewById(R.id.tvStepTitle);
        tvSkip         = findViewById(R.id.tvSkip);
        layoutStepDots = findViewById(R.id.layoutStepDots);
        dotStep1 = findViewById(R.id.dotStep1);
        dotStep2 = findViewById(R.id.dotStep2);
        dotStep3 = findViewById(R.id.dotStep3);
        dotStep4 = findViewById(R.id.dotStep4);

        stepIntro       = findViewById(R.id.stepIntro);
        stepPin         = findViewById(R.id.stepPin);
        stepQuestion    = findViewById(R.id.stepQuestion);
        stepFingerprint = findViewById(R.id.stepFingerprint);
        stepSuccess     = findViewById(R.id.stepSuccess);

        ivIntroShield   = findViewById(R.id.ivIntroShield);
        tvIntroTitle    = findViewById(R.id.tvIntroTitle);
        tvIntroSubtitle = findViewById(R.id.tvIntroSubtitle);
        btnStartSetup   = findViewById(R.id.btnStartSetup);
        btnSkipIntro    = findViewById(R.id.btnSkipIntro);
        btnDisableLock  = findViewById(R.id.btnDisableLock);

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
                updatePinDots(pinBuilder.length(), false);
                tvPinError.setVisibility(View.INVISIBLE);
            }
        });

        radioGroupQuestions = findViewById(R.id.radioGroupQuestions);
        radioQuestions = new RadioButton[]{
            findViewById(R.id.radioQ0), findViewById(R.id.radioQ1), findViewById(R.id.radioQ2),
            findViewById(R.id.radioQ3), findViewById(R.id.radioQ4)
        };
        for (int i = 0; i < radioQuestions.length && i < DatabaseManager.SECURITY_QUESTIONS.length; i++) {
            radioQuestions[i].setText(DatabaseManager.SECURITY_QUESTIONS[i]);
        }
        layoutAnswer = findViewById(R.id.layoutAnswer);
        etAnswer     = findViewById(R.id.etAnswer);
        btnNextQuestion = findViewById(R.id.btnNextQuestion);

        switchFingerprint = findViewById(R.id.switchFingerprint);
        tvFingerprintUnavailable = findViewById(R.id.tvFingerprintUnavailable);
        btnBackFingerprint = findViewById(R.id.btnBackFingerprint);
        btnFinish = findViewById(R.id.btnFinish);

        btnGoToDashboard = findViewById(R.id.btnGoToDashboard);

        findViewById(R.id.btnBack).setOnClickListener(v -> goBack());
    }

    private void wireListeners() {
        btnStartSetup.setOnClickListener(v -> {
            tempNewPin = null;
            pinBuilder.setLength(0);
            goToStep(Step.PIN_CREATE);
        });

        btnSkipIntro.setOnClickListener(v -> finish());

        btnDisableLock.setOnClickListener(v -> confirmDisableLock());

        tvSkip.setOnClickListener(v -> {
            if (currentStep == Step.QUESTION) skipQuestionStep();
        });

        radioGroupQuestions.setOnCheckedChangeListener((group, checkedId) -> {
            for (int i = 0; i < radioQuestions.length; i++) {
                if (radioQuestions[i].getId() == checkedId) {
                    selectedQuestionIndex = i;
                    layoutAnswer.setVisibility(View.VISIBLE);
                    etAnswer.setText("");
                    return;
                }
            }
        });

        btnNextQuestion.setOnClickListener(v -> onNextQuestion());

        switchFingerprint.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return; // programmatic changes shouldn't re-trigger prompts
            if (isChecked) {
                if (biometricAvailable) {
                    showBiometricEnrollPrompt();
                } else {
                    switchFingerprint.setChecked(false);
                    Toast.makeText(this, "Fingerprint is not available on this device.", Toast.LENGTH_SHORT).show();
                }
            } else {
                db.setFingerprintEnabled(false);
            }
        });

        btnBackFingerprint.setOnClickListener(v -> goBack());
        btnFinish.setOnClickListener(v -> {
            backStack.clear();
            goToStep(Step.SUCCESS);
        });

        btnGoToDashboard.setOnClickListener(v -> {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    // ═══════════════════ Step navigation ═══════════════════

    private void goToStep(Step next) {
        backStack.push(currentStep);
        currentStep = next;
        renderStep();
    }

    private void goBack() {
        if (backStack.isEmpty()) {
            finish();
            return;
        }
        currentStep = backStack.pop();
        renderStep();
    }

    private void renderStep() {
        stepIntro.setVisibility(currentStep == Step.INTRO ? View.VISIBLE : View.GONE);
        stepPin.setVisibility((currentStep == Step.PIN_CREATE || currentStep == Step.PIN_CONFIRM) ? View.VISIBLE : View.GONE);
        stepQuestion.setVisibility(currentStep == Step.QUESTION ? View.VISIBLE : View.GONE);
        stepFingerprint.setVisibility(currentStep == Step.FINGERPRINT ? View.VISIBLE : View.GONE);
        stepSuccess.setVisibility(currentStep == Step.SUCCESS ? View.VISIBLE : View.GONE);

        layoutTopBar.setVisibility(currentStep == Step.SUCCESS ? View.GONE : View.VISIBLE);
        layoutStepDots.setVisibility(
            (currentStep == Step.INTRO || currentStep == Step.SUCCESS) ? View.GONE : View.VISIBLE);
        tvSkip.setVisibility(currentStep == Step.QUESTION ? View.VISIBLE : View.GONE);

        switch (currentStep) {
            case PIN_CREATE:
                tvStepTitle.setText("Create PIN");
                tvPinTitle.setText("Create Your App Lock");
                tvPinSubtitle.setText("Protect your personal data using a secure 4-digit PIN.");
                pinBuilder.setLength(0);
                updatePinDots(0, false);
                tvPinError.setVisibility(View.INVISIBLE);
                setStepDot(1);
                break;
            case PIN_CONFIRM:
                tvStepTitle.setText("Confirm PIN");
                tvPinTitle.setText("Confirm Your PIN");
                tvPinSubtitle.setText("Re-enter the 4-digit PIN to confirm.");
                pinBuilder.setLength(0);
                updatePinDots(0, false);
                tvPinError.setVisibility(View.INVISIBLE);
                setStepDot(1);
                break;
            case QUESTION:
                tvStepTitle.setText("Security Question");
                setStepDot(2);
                break;
            case FINGERPRINT:
                tvStepTitle.setText("Fingerprint Lock");
                checkFingerprintAvailability();
                setStepDot(3);
                break;
            case SUCCESS:
                setStepDot(4);
                break;
            case INTRO:
            default:
                tvStepTitle.setText("App Lock");
                updateIntroState();
                break;
        }
    }

    /**
     * Intro ধাপের চেহারা লক বর্তমানে চালু আছে কিনা তার উপর নির্ভর করে বদলায়:
     * চালু থাকলে "Protected" (সবুজ শিল্ড) + "Disable App Lock" অপশন দেখানো হয়,
     * বন্ধ থাকলে আগের মতোই "Protect Your Data" (বেগুনি শিল্ড) সেটআপ প্রম্পট দেখানো হয়।
     */
    private void updateIntroState() {
        boolean locked = db.isLockEnabled();
        if (locked) {
            tvIntroTitle.setText("Protected");
            tvIntroSubtitle.setText("Your app and data are currently protected with a PIN.");
            ivIntroShield.setImageTintList(ContextCompat.getColorStateList(this, R.color.applockSuccess));
            btnStartSetup.setText("Change PIN");
            btnSkipIntro.setVisibility(View.GONE);
            btnDisableLock.setVisibility(View.VISIBLE);
        } else {
            tvIntroTitle.setText("Protect Your Data");
            tvIntroSubtitle.setText("Set a 4-digit PIN to keep your app and data secure.");
            ivIntroShield.setImageTintList(ContextCompat.getColorStateList(this, R.color.applockPrimary));
            btnStartSetup.setText("Create PIN");
            btnSkipIntro.setVisibility(View.VISIBLE);
            btnDisableLock.setVisibility(View.GONE);
        }
    }

    private void confirmDisableLock() {
        new AlertDialog.Builder(this)
            .setTitle("Disable App Lock")
            .setMessage("Are you sure you want to disable App Lock? Your app will no longer require a PIN to open.")
            .setPositiveButton("Disable", (dialog, which) -> {
                db.disableLock();
                Toast.makeText(this, "App Lock disabled", Toast.LENGTH_SHORT).show();
                updateIntroState();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void setStepDot(int activeIndex) {
        View[] dots = {dotStep1, dotStep2, dotStep3, dotStep4};
        for (int i = 0; i < dots.length; i++) {
            dots[i].setBackgroundResource(i == activeIndex - 1
                ? R.drawable.shape_applock_step_dot_active
                : R.drawable.shape_applock_step_dot_inactive);
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
        if (currentStep == Step.PIN_CREATE) {
            tempNewPin = entered;
            pinBuilder.setLength(0);
            goToStep(Step.PIN_CONFIRM);
        } else if (currentStep == Step.PIN_CONFIRM) {
            if (entered.equals(tempNewPin)) {
                // PIN নিশ্চিত হলো — সাথে সাথে এনক্রিপ্টেড স্টোরে সেভ করা হচ্ছে যাতে
                // ইউজার মাঝপথে বেরিয়ে গেলেও অন্তত মূল PIN লক চালু থাকে।
                db.saveLock(DatabaseManager.LOCK_PIN, LockUtils.hash(tempNewPin), db.isFingerprintEnabled());
                goToStep(Step.QUESTION);
            } else {
                showPinMismatch();
            }
        }
    }

    private void showPinMismatch() {
        tvPinError.setText("PIN মেলেনি! আবার চেষ্টা করুন");
        tvPinError.setVisibility(View.VISIBLE);
        updatePinDots(4, true);
        stepPin.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake_error));
        stepPin.postDelayed(() -> {
            tempNewPin = null;
            pinBuilder.setLength(0);
            // মিসম্যাচের পর সরাসরি Create ধাপে ফিরিয়ে নেওয়া হচ্ছে — এটা নতুন "forward"
            // navigation না, তাই backStack-এ আগের PIN_CONFIRM entry-টা push না করেই
            // (যেটা goToStep() করত) শুধু বাড়তি PIN_CREATE entry-টা pop করে বাদ দেওয়া হচ্ছে,
            // যাতে "Back" চাপলে সরাসরি Intro-তে যায়, ডুপ্লিকেট Create ধাপে না গিয়ে।
            if (!backStack.isEmpty()) backStack.pop();
            currentStep = Step.PIN_CREATE;
            renderStep();
        }, 450);
    }

    // ═══════════════════ Security question ═══════════════════

    private void onNextQuestion() {
        if (selectedQuestionIndex >= 0) {
            String answer = etAnswer.getText() != null ? etAnswer.getText().toString().trim() : "";
            if (TextUtils.isEmpty(answer)) {
                layoutAnswer.setError("Answer is required");
                return;
            }
            layoutAnswer.setError(null);
            db.saveSecurityQuestion(selectedQuestionIndex, answer);
        }
        goToStep(Step.FINGERPRINT);
    }

    private void skipQuestionStep() {
        selectedQuestionIndex = -1;
        radioGroupQuestions.clearCheck();
        layoutAnswer.setVisibility(View.GONE);
        goToStep(Step.FINGERPRINT);
    }

    // ═══════════════════ Fingerprint ═══════════════════

    private void checkFingerprintAvailability() {
        BiometricManager bm = BiometricManager.from(this);
        biometricAvailable = bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                              == BiometricManager.BIOMETRIC_SUCCESS;
        tvFingerprintUnavailable.setVisibility(biometricAvailable ? View.GONE : View.VISIBLE);
        switchFingerprint.setEnabled(biometricAvailable);
        switchFingerprint.setChecked(biometricAvailable && db.isFingerprintEnabled());
    }

    private void showBiometricEnrollPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt.AuthenticationCallback cb = new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                db.setFingerprintEnabled(true);
                Toast.makeText(AppLockActivity.this, "Fingerprint enabled", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED
                        && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    Toast.makeText(AppLockActivity.this, "Fingerprint setup failed: " + errString, Toast.LENGTH_SHORT).show();
                }
                switchFingerprint.setChecked(false);
            }

            @Override
            public void onAuthenticationFailed() {
                Toast.makeText(AppLockActivity.this, "Fingerprint not recognized, try again", Toast.LENGTH_SHORT).show();
            }
        };
        biometricPrompt = new BiometricPrompt(this, executor, cb);
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
            .setTitle("Enable Fingerprint")
            .setSubtitle("Scan your fingerprint to confirm")
            .setNegativeButtonText("Cancel")
            .build();
        biometricPrompt.authenticate(info);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (biometricPrompt != null) biometricPrompt.cancelAuthentication();
    }

    @Override
    public void onBackPressed() {
        goBack();
    }
}
