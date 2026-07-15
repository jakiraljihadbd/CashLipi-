package com.jrappspot.cashlipi.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;


import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.utils.DatabaseManager;
import com.jrappspot.cashlipi.utils.FirestoreSyncManager;
import com.jrappspot.cashlipi.utils.GoogleDriveSyncManager;
import com.jrappspot.cashlipi.utils.GoogleSignInHelper;

/**
 * 🔐 LoginActivity (Firebase Sync সহ আপডেট)
 *
 * ✅ Google Sign-In করলে Firebase থেকে পুরোনো ডাটা auto-restore হবে
 * ✅ নতুন user হলে fresh start হবে
 * ✅ Internet না থাকলেও login হবে (offline cache)
 */
public class LoginActivity extends BaseActivity {

    private DatabaseManager db;
    private FirestoreSyncManager firestoreSync;
    private GoogleSignInHelper signInHelper;
    private ActivityResultLauncher<Intent> signInLauncher;
    private FrameLayout loadingOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = DatabaseManager.getInstance(this);
        firestoreSync = FirestoreSyncManager.getInstance(this);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        setupGoogleSignIn();
        setupClickListeners();
        startEntranceAnimation();
    }

    private void setupGoogleSignIn() {
        signInHelper = new GoogleSignInHelper(this, null);
        signInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                Intent data = result.getData();
                if (data != null) {
                    signInHelper.handleResult(data, new GoogleSignInHelper.SignInCallback() {
                        @Override
                        public void onSuccess(String name, String email, String photoUrl) {
                            // ১. Local DB তে Google info save করুন
                            db.saveGoogleAccount(name, email, photoUrl);
                            db.setLoginDone(true);
                            // Auto Sync ডিফল্টভাবে চালু রাখি (নতুন sign-in / re-install)
                            db.setDriveAutoSyncEnabled(true);

                            // ২. এই Gmail দিয়ে আগে থেকেই (username/phone সহ) কোনো অ্যাকাউন্ট
                            // registered থাকলে (SignUpActivity দিয়ে তৈরি, একই email — Firebase
                            // একই uid এ link করে দেয়) সেই username/phone প্রোফাইলে auto-fill করি
                            fillProfileFromExistingAccount();

                            // ৩. পুরোনো ডাটা restore করার চেষ্টা — Google Drive আগে, না পেলে Firebase
                            attemptAutoRestore(name, email);
                        }

                        @Override
                        public void onFailure(String error) {
                            hideLoading();
                            Toast.makeText(LoginActivity.this,
                                "❌ " + error, Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    hideLoading();
                    Toast.makeText(this, "Google সাইন ইন বাতিল হয়েছে", Toast.LENGTH_SHORT).show();
                }
            });
    }

    /**
     * 👤 Google দিয়ে সাইন-ইন করলে, একই Gmail দিয়ে আগে থেকে (SignUpActivity দিয়ে)
     * তৈরি করা কোনো অ্যাকাউন্ট থাকলে সেই username/phone/name প্রোফাইলে auto-fill
     * করি। Firebase-এ একই email → একই uid হওয়ায় (one-account-per-email), Google
     * sign-in-এর পর FirebaseAuth-এর uid দিয়েই সেই পুরনো "users/{uid}" ডকুমেন্ট
     * পাওয়া যায়।
     *
     * এখানে দুইটা field-ই সরাসরি db.saveUsername()/savePhoneNumber() দিয়ে সেভ
     * করা নিরাপদ — কারণ isEmailPhoneSignedIn() এখন আলাদা explicit flag দিয়ে ঠিক
     * হয়, তাই এতে প্রোফাইলের "Google দিয়ে সাইন-ইন করা" UI ভুলবশত পাল্টে যাবে না।
     */
    private void fillProfileFromExistingAccount() {
        com.google.firebase.auth.FirebaseUser currentUser =
            com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        String uid = currentUser.getUid();

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener(doc -> {
                if (doc == null || !doc.exists()) return;

                String existingUsername = doc.getString("username");
                String existingPhone = doc.getString("phone");
                String existingName = doc.getString("name");

                if (existingUsername != null && !existingUsername.isEmpty()) {
                    db.saveUsername(existingUsername);
                }
                if (existingPhone != null && !existingPhone.isEmpty()) {
                    db.savePhoneNumber(existingPhone);
                }
                // Registration-এর সময় দেওয়া আসল নামটাই প্রাধান্য পাক Google display name-এর চেয়ে
                if (existingName != null && !existingName.isEmpty()) {
                    db.saveCustomName(existingName);
                }
            });
        // ব্যর্থ হলেও Google sign-in flow স্বাভাবিকভাবে চলবে (নতুন user বা প্রথমবার হলে
        // এই ডকুমেন্ট থাকবেই না, সেটা normal)।
    }

    /**
     * ☁️ Auto Restore — reinstall/re-login হলে ডাটা ফিরিয়ে আনার চেষ্টা।
     * ডিভাইসে আগে থেকে কোনো ডাটা না থাকলে (fresh install) প্রথমে Google Drive-এ
     * ব্যাকআপ আছে কিনা দেখি — থাকলে সরাসরি silent restore করে ফেলি (ইউজারকে
     * কিছু করতে হয় না)। Drive-এ কিছু না পেলে পুরনো Firebase (Firestore) flow
     * fallback হিসেবে ব্যবহার হয়, backward compatibility বজায় রাখতে।
     */
    private void attemptAutoRestore(String name, String email) {
        boolean localDataEmpty =
            db.getIncomeList().isEmpty() &&
            db.getExpenseList().isEmpty() &&
            db.getSavingsList().isEmpty() &&
            db.getLedgerList().isEmpty();

        if (!localDataEmpty) {
            // এই ডিভাইসে আগে থেকেই ডাটা আছে — overwrite এড়াতে Drive auto-restore বাদ,
            // শুধু পুরনো Firebase sync flow চালাই (আগের মতোই)
            restoreFromFirebase(name, email);
            return;
        }

        showRestoringDialog();

        GoogleDriveSyncManager driveSync = new GoogleDriveSyncManager(this);
        driveSync.downloadBackup(new GoogleDriveSyncManager.SyncCallback() {
            @Override
            public void onSuccess(String json) {
                runOnUiThread(() -> {
                    boolean restored = db.importFromJson(json);
                    hideLoading();
                    if (restored) {
                        Toast.makeText(LoginActivity.this,
                            "☁️ Google Drive থেকে ডাটা ফিরে এসেছে! স্বাগতম, " + name,
                            Toast.LENGTH_LONG).show();
                        goToNextScreen();
                    } else {
                        // Drive-এ ফাইল পাওয়া গেলেও import ব্যর্থ হলে Firebase fallback
                        restoreFromFirebase(name, email);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                // Drive-এ backup পাওয়া যায়নি — পুরনো Firebase (Firestore) flow দিয়ে try করি
                runOnUiThread(() -> restoreFromFirebase(name, email));
            }
        });
    }

    /**
     * 🔥 Firebase থেকে ডাটা restore করুন
     */
    private void restoreFromFirebase(String name, String email) {
        // Loading dialog দেখান
        showRestoringDialog();

        firestoreSync.downloadAndRestore(new FirestoreSyncManager.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    hideLoading();
                    if (message.contains("নতুন account")) {
                        // প্রথমবার — সরাসরি যান
                        Toast.makeText(LoginActivity.this,
                            "স্বাগতম, " + name + "! 🎉", Toast.LENGTH_SHORT).show();
                    } else {
                        // পুরোনো ডাটা পাওয়া গেছে
                        Toast.makeText(LoginActivity.this,
                            "✅ ডাটা ফিরে এসেছে! স্বাগতম, " + name,
                            Toast.LENGTH_LONG).show();
                    }
                    goToNextScreen();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    hideLoading();
                    // Firebase error হলেও login চালিয়ে যান (local data থাকলে সেটা দেখাবে)
                    Toast.makeText(LoginActivity.this,
                        "স্বাগতম, " + name + "!\n(Sync: " + error + ")",
                        Toast.LENGTH_LONG).show();
                    goToNextScreen();
                });
            }
        });
    }

    private void showRestoringDialog() {
        // Loading overlay দেখান
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(View.VISIBLE);
        }
    }

    private void setupClickListeners() {
        View btnGoogle = findViewById(R.id.btnGoogleSignIn);
        if (btnGoogle != null) {
            btnGoogle.setOnClickListener(v -> {
                showLoading();
                signInHelper.getSignInIntentAfterSignOut(this,
                    intent -> signInLauncher.launch(intent));
            });
        }

        View btnSkip = findViewById(R.id.btnSkipLogin);
        if (btnSkip != null) {
            btnSkip.setOnClickListener(v -> {
                db.setLoginDone(true);
                goToNextScreen();
            });
        }

        View btnEmailPhoneLogin = findViewById(R.id.btnEmailPhoneLogin);
        if (btnEmailPhoneLogin != null) {
            btnEmailPhoneLogin.setOnClickListener(v ->
                startActivity(new Intent(this, EmailLoginActivity.class))
            );
        }
    }

    private void goToNextScreen() {
        // AppLock check
        if (db.isLockEnabled()) {
            startActivity(new Intent(this, LockScreenActivity.class));
        } else {
            startActivity(new Intent(this, DashboardActivity.class));
        }
        finish();
    }

    private void showLoading() {
        if (loadingOverlay != null) loadingOverlay.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
    }

    private void startEntranceAnimation() {
        View logo = findViewById(R.id.sectionLogo);
        View card = findViewById(R.id.cardLogin);
        if (logo == null || card == null) return;

        logo.setAlpha(0f);
        logo.setScaleX(0.6f);
        logo.setScaleY(0.6f);
        card.setAlpha(0f);
        card.setTranslationY(80f);

        AnimatorSet logoAnim = new AnimatorSet();
        logoAnim.playTogether(
            ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f).setDuration(600),
            ObjectAnimator.ofFloat(logo, "scaleX", 0.6f, 1f).setDuration(600),
            ObjectAnimator.ofFloat(logo, "scaleY", 0.6f, 1f).setDuration(600)
        );
        logoAnim.setInterpolator(new DecelerateInterpolator());
        logoAnim.setStartDelay(200);

        AnimatorSet cardAnim = new AnimatorSet();
        cardAnim.playTogether(
            ObjectAnimator.ofFloat(card, "alpha", 0f, 1f).setDuration(500),
            ObjectAnimator.ofFloat(card, "translationY", 80f, 0f).setDuration(500)
        );
        cardAnim.setInterpolator(new DecelerateInterpolator());
        cardAnim.setStartDelay(600);

        logoAnim.start();
        cardAnim.start();
    }
}
