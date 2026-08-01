package com.jrappspot.cashlipi.activities;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.utils.DatabaseManager;
import com.jrappspot.cashlipi.utils.FirestoreSyncManager;

public class SplashActivity extends BaseActivity {

    private static final int SPLASH_DELAY = 2600;

    // Loading label cycles through these as the progress bar fills — gives a
    // dynamic, "something is actually happening" feel instead of a static label.
    private static final String[] LOADING_STEPS = {
            "প্রাথমিক প্রস্তুতি...",
            "ডাটা লোড হচ্ছে...",
            "প্রায় সম্পন্ন..."
    };

    // Angle (degrees, screen convention: 0=right, 90=down) at which each of the
    // 8 pink petal pieces sits around the logo ring — matches the source artwork.
    private static final float[] PETAL_ANGLES = {
            54.6f, 100.35f, 145.25f, 186.975f, 229.15f, 273.775f, 320.025f, 7.925f
    };

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        DatabaseManager db = DatabaseManager.getInstance(this);

        // Views
        ImageView logoRing      = findViewById(R.id.logoRing);
        ImageView logoCoin      = findViewById(R.id.logoCoin);
        ImageView logoTaka      = findViewById(R.id.logoTaka);
        TextView  appName       = findViewById(R.id.splashAppName);
        View      divider       = findViewById(R.id.splashDivider);
        TextView  tagline       = findViewById(R.id.splashTagline);
        TextView  version       = findViewById(R.id.splashVersion);
        TextView  loadingLabel  = findViewById(R.id.splashLoadingLabel);
        TextView  percentText   = findViewById(R.id.splashPercent);
        TextView  credit        = findViewById(R.id.splashCredit);
        LinearProgressIndicator progressBar = findViewById(R.id.splashProgressBar);

        int[] petalIds = {
                R.id.logoPetal0, R.id.logoPetal1, R.id.logoPetal2, R.id.logoPetal3,
                R.id.logoPetal4, R.id.logoPetal5, R.id.logoPetal6, R.id.logoPetal7
        };

        // ── Logo assembly: ring → coin → taka pop in ──
        popIn(logoRing, 80, 380);
        popIn(logoCoin, 200, 360);
        popIn(logoTaka, 320, 360);

        // ── Pink petals fly in from outside, one after another, and land in place ──
        float density = getResources().getDisplayMetrics().density;
        float flyDistancePx = 260f * density;
        int petalBaseDelay = 430;
        int petalStagger = 50;

        for (int i = 0; i < petalIds.length; i++) {
            ImageView petal = findViewById(petalIds[i]);
            if (petal == null) continue;

            double rad = Math.toRadians(PETAL_ANGLES[i]);
            float fromX = (float) Math.cos(rad) * flyDistancePx;
            float fromY = (float) Math.sin(rad) * flyDistancePx;
            float spinFrom = (i % 2 == 0) ? -130f : 130f;

            petal.setAlpha(0f);
            petal.setScaleX(0.35f);
            petal.setScaleY(0.35f);
            petal.setTranslationX(fromX);
            petal.setTranslationY(fromY);
            petal.setRotation(spinFrom);

            int startDelay = petalBaseDelay + i * petalStagger;
            handler.postDelayed(() -> petal.animate()
                    .alpha(1f)
                    .scaleX(1f).scaleY(1f)
                    .translationX(0f).translationY(0f)
                    .rotation(0f)
                    .setDuration(620)
                    .setInterpolator(new OvershootInterpolator(1.25f))
                    .start(), startDelay);
        }

        // ── Text / element fade-in sequence (after petals settle) ──
        fadeInView(appName,      1220);
        fadeInView(divider,      1340);
        fadeInView(tagline,      1440);
        fadeInView(version,      1540);
        fadeInView(loadingLabel, 1640);
        if (percentText != null) fadeInView(percentText, 1640);
        fadeInView(credit,       1780);

        // ── Progress bar: fills smoothly, updates a live % and cycles the
        //    status label so the loading feels dynamic rather than static ──
        if (progressBar != null) {
            ObjectAnimator progressAnim = ObjectAnimator.ofInt(progressBar, "progress", 0, 100);
            progressAnim.setDuration(1750);
            progressAnim.setStartDelay(400);
            progressAnim.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
            progressAnim.addUpdateListener(anim -> {
                int value = (int) anim.getAnimatedValue();
                if (percentText != null) percentText.setText(value + "%");
                if (loadingLabel != null) {
                    int stepIndex = Math.min(LOADING_STEPS.length - 1, value * LOADING_STEPS.length / 100);
                    loadingLabel.setText(LOADING_STEPS[stepIndex]);
                }
            });
            progressAnim.start();
        }

        // ── Navigate after delay ──
        handler.postDelayed(() -> {
            // Step 1: Admin config check (maintenance + force update)
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("admin").document("config")
                .get()
                .addOnSuccessListener(doc -> {
                    runOnUiThread(() -> {
                        // Maintenance check
                        Boolean maintenance = doc.getBoolean("maintenance");
                        if (maintenance != null && maintenance) {
                            startActivity(new Intent(this, MaintenanceActivity.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                            finish();
                            return;
                        }

                        // Force update check
                        Boolean forceUpdate = doc.getBoolean("forceUpdate");
                        String minVersion = doc.getString("minVersion");
                        String updateMsg = doc.getString("updateMessage");
                        if (forceUpdate != null && forceUpdate && minVersion != null) {
                            String currentVersion = "1.0";
                            try {
                                currentVersion = getPackageManager()
                                    .getPackageInfo(getPackageName(), 0).versionName;
                            } catch (Exception ignored) {}

                            if (!currentVersion.equals(minVersion)) {
                                String msg = updateMsg != null ? updateMsg : "নতুন version পাওয়া গেছে। আপডেট করুন।";
                                new AlertDialog.Builder(this)
                                    .setTitle("🔄 আপডেট প্রয়োজন")
                                    .setMessage(msg)
                                    .setPositiveButton("পরে", (d, w) -> checkBlockAndGoNext(db))
                                    .setCancelable(false).show();
                                return;
                            }
                        }

                        checkBlockAndGoNext(db);
                    });
                })
                .addOnFailureListener(e -> checkBlockAndGoNext(db));
        }, SPLASH_DELAY);
    }

    private void popIn(View view, long delayMs, long duration) {
        if (view == null) return;
        view.setAlpha(0f);
        view.setScaleX(0.55f);
        view.setScaleY(0.55f);
        handler.postDelayed(() ->
            view.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(duration)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start(),
        delayMs);
    }

    private void fadeInView(View view, long delayMs) {
        if (view == null) return;
        view.setAlpha(0f);
        view.setTranslationY(24f);
        handler.postDelayed(() ->
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start(),
        delayMs);
    }

    private void checkBlockAndGoNext(DatabaseManager db) {
        if (db.isGoogleSignedIn()) {
            FirestoreSyncManager.getInstance(this).checkUserBlockStatus((isBlocked, status) -> {
                runOnUiThread(() -> {
                    if (isBlocked) {
                        String msg = "DEVICE_BANNED".equals(status)
                            ? "এই ডিভাইসটি নিষিদ্ধ করা হয়েছে।"
                            : "আপনার account ব্লক করা হয়েছে।\nAdmin-এর সাথে যোগাযোগ করুন।";
                        new AlertDialog.Builder(this)
                            .setTitle("🚫 Access বন্ধ")
                            .setMessage(msg)
                            .setPositiveButton("বাহির", (d, w) -> finishAffinity())
                            .setCancelable(false).show();
                    } else {
                        goNext(db);
                    }
                });
            });
        } else {
            goNext(db);
        }
    }

    private void goNext(DatabaseManager db) {
        Intent intent;
        if (!db.isLoginDone()) {
            intent = new Intent(this, LoginActivity.class);
        } else if (db.isLockEnabled()) {
            intent = new Intent(this, LockScreenActivity.class);
        } else {
            intent = new Intent(this, DashboardActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }
}
