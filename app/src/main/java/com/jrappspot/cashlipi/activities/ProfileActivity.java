package com.jrappspot.cashlipi.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;


import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.utils.FontUtils;
import com.jrappspot.cashlipi.utils.DatabaseManager;
import com.jrappspot.cashlipi.utils.FirestoreSyncManager;
import com.jrappspot.cashlipi.utils.GoogleSignInHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * ProfileActivity — সম্পূর্ণ প্রোফাইল ম্যানেজমেন্ট।
 *
 *  ১) সাইন-ইন অপশন — নম্বর/ইমেইল দিয়ে অথবা Google দিয়ে
 *  ২) প্রোফাইল সম্পাদনা — নাম, ইউজারনেম, নম্বর, ইমেইল
 *  ৩) পাসওয়ার্ড পরিবর্তন — পুরাতন/নতুন/নিশ্চিতকরণ পাসওয়ার্ড
 */
public class ProfileActivity extends BaseActivity {

    private static final String TAG = "ProfileActivity";

    private DatabaseManager db;
    private GoogleSignInHelper signInHelper;

    // Photo / name
    private ImageView ivProfilePhoto, ivGooglePhoto;
    private TextView tvDisplayName, tvGoogleBadge, tvGoogleName, tvGoogleEmail, tvProfileInitials;

    // Sign-in options
    private View layoutPhoneSignedIn, layoutPhoneSignIn, btnPhoneEmailSignIn, btnPhoneEmailSignOut;
    private TextView tvSignedInIdentity;
    private View layoutGoogleSignedIn, layoutGoogleSignIn, btnGoogleSignIn;
    // দুইটা সাইন-ইন মেথড একসাথে না দেখানোর জন্য — মোবাইল/ইমেইল প্রাইমারি ধরা হয়,
    // সেটা সাইন-ইন করা থাকলে Google অংশ (label + divider + সব state) পুরোপুরি hide
    private View tvPhoneSectionLabel, dividerSignInMethods, tvGoogleSectionLabel;
    // Google secondary (মোবাইল প্রাইমারি থাকা অবস্থায় Auto Sync-এর জন্য connected google) — compact row
    private View layoutGoogleSecondaryInfo, btnGoogleSecondaryDisconnect;
    private TextView tvGoogleSecondaryEmail;

    // Profile edit
    private TextInputEditText etProfileName, etUsername, etPhoneNumber, etEmail;
    private View headerEditProfile, bodyEditProfile;
    private TextView tvEditProfileChevron;

    // Change password
    private TextInputEditText etOldPassword, etNewPassword, etConfirmPassword;
    private TextView tvTogglePasswordVisibility;
    private boolean passwordsVisible = false;
    private View headerChangePassword, bodyChangePassword;
    private TextView tvChangePasswordChevron;

    // Auto Sync (Google Drive)
    private CompoundButton swAutoSync;
    private TextView tvAutoSyncStatus;

    private ActivityResultLauncher<Intent> signInLauncher;
    private ActivityResultLauncher<String> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        FontUtils.applyToView(this, findViewById(android.R.id.content));

        db = DatabaseManager.getInstance(this);

        bindViews();

        // Photo picker
        galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) saveLocalPhoto(uri);
            });

        setupGoogleSignIn();
        setupClickListeners();
        setupAutoSyncToggle();
        refreshUI();
    }

    private void bindViews() {
        ivProfilePhoto    = findViewById(R.id.ivProfilePhoto);
        ivGooglePhoto     = findViewById(R.id.ivGooglePhoto);
        tvDisplayName     = findViewById(R.id.tvDisplayName);
        tvGoogleBadge     = findViewById(R.id.tvGoogleBadge);
        tvGoogleName      = findViewById(R.id.tvGoogleName);
        tvGoogleEmail     = findViewById(R.id.tvGoogleEmail);
        tvProfileInitials = findViewById(R.id.tvProfileInitials);

        layoutPhoneSignedIn  = findViewById(R.id.layoutPhoneSignedIn);
        layoutPhoneSignIn    = findViewById(R.id.layoutPhoneSignIn);
        btnPhoneEmailSignIn  = findViewById(R.id.btnPhoneEmailSignIn);
        btnPhoneEmailSignOut = findViewById(R.id.btnPhoneEmailSignOut);
        tvSignedInIdentity   = findViewById(R.id.tvSignedInIdentity);

        layoutGoogleSignedIn = findViewById(R.id.layoutGoogleSignedIn);
        layoutGoogleSignIn   = findViewById(R.id.layoutGoogleSignIn);
        btnGoogleSignIn      = findViewById(R.id.btnGoogleSignIn);

        tvPhoneSectionLabel  = findViewById(R.id.tvPhoneSectionLabel);
        dividerSignInMethods = findViewById(R.id.dividerSignInMethods);
        tvGoogleSectionLabel = findViewById(R.id.tvGoogleSectionLabel);

        layoutGoogleSecondaryInfo   = findViewById(R.id.layoutGoogleSecondaryInfo);
        tvGoogleSecondaryEmail      = findViewById(R.id.tvGoogleSecondaryEmail);
        btnGoogleSecondaryDisconnect = findViewById(R.id.btnGoogleSecondaryDisconnect);

        etProfileName  = findViewById(R.id.etProfileName);
        etUsername     = findViewById(R.id.etUsername);
        etPhoneNumber  = findViewById(R.id.etPhoneNumber);
        etEmail        = findViewById(R.id.etEmail);
        headerEditProfile   = findViewById(R.id.headerEditProfile);
        bodyEditProfile     = findViewById(R.id.bodyEditProfile);
        tvEditProfileChevron = findViewById(R.id.tvEditProfileChevron);

        etOldPassword     = findViewById(R.id.etOldPassword);
        etNewPassword     = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        tvTogglePasswordVisibility = findViewById(R.id.tvTogglePasswordVisibility);
        headerChangePassword   = findViewById(R.id.headerChangePassword);
        bodyChangePassword     = findViewById(R.id.bodyChangePassword);
        tvChangePasswordChevron = findViewById(R.id.tvChangePasswordChevron);

        swAutoSync       = findViewById(R.id.swAutoSync);
        tvAutoSyncStatus = findViewById(R.id.tvAutoSyncStatus);
    }

    // ═══════════════════════════════════════════════════════
    //  Google Sign-In
    // ═══════════════════════════════════════════════════════
    private void setupGoogleSignIn() {
        signInHelper = new GoogleSignInHelper(this, null);

        signInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                Intent data = result.getData();
                if (data != null) {
                    signInHelper.handleResult(data, new GoogleSignInHelper.SignInCallback() {
                        public void onSuccess(String name, String email, String photo) {
                            //  Google photo URL সরাসরি সেভ করি (resize parameter Glide লোডের সময় যুক্ত হবে)
                            db.saveGoogleAccount(name, email, photo);
                            FirestoreSyncManager.getInstance(ProfileActivity.this).uploadAllData(null);

                            // Google name → custom name-ও সেট করি (override না থাকলে)
                            if (db.getCustomName().isEmpty()) {
                                db.saveCustomName(name);
                            }

                            //  নতুন সাইন-ইনে local photo override থাকলে সরিয়ে Google ছবি প্রাধান্য দিই
                            db.saveLocalPhotoPath("");

                            refreshUI();
                            Toast.makeText(ProfileActivity.this,
                                " Google সংযুক্ত হয়েছে!", Toast.LENGTH_SHORT).show();
                        }
                        public void onFailure(String error) {
                            Toast.makeText(ProfileActivity.this, " " + error, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
    }

    // ═══════════════════════════════════════════════════════
    //  Click listeners
    // ═══════════════════════════════════════════════════════
    private void setupClickListeners() {

        // Profile photo change
        View btnCam = findViewById(R.id.btnChangePhoto);
        if (btnCam != null) btnCam.setOnClickListener(v -> showPhotoOptions());
        if (ivProfilePhoto != null) ivProfilePhoto.setOnClickListener(v -> showPhotoOptions());

        // ── Sign-in options ──
        if (btnPhoneEmailSignIn != null) btnPhoneEmailSignIn.setOnClickListener(v ->
            startActivity(new Intent(this, EmailLoginActivity.class)));

        if (btnPhoneEmailSignOut != null) btnPhoneEmailSignOut.setOnClickListener(v ->
            new AlertDialog.Builder(this, R.style.AppDialog)
                .setTitle("সাইন আউট")
                .setMessage("নম্বর/ইমেইল অ্যাকাউন্ট সাইন আউট করবেন?")
                .setPositiveButton("হ্যাঁ", (d, w) -> {
                    FirebaseAuth.getInstance().signOut();
                    db.clearEmailPhoneAccount();
                    refreshUI();
                    Toast.makeText(this, "সাইন আউট হয়েছে", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("না", null).show());

        // Google sign-in
        if (btnGoogleSignIn != null) btnGoogleSignIn.setOnClickListener(v ->
            signInHelper.getSignInIntentAfterSignOut(this,
                intent -> signInLauncher.launch(intent)));

        // Google sign-out (বড় প্রাইমারি কার্ড থেকে)
        View btnSignOut = findViewById(R.id.btnGoogleSignOut);
        if (btnSignOut != null) btnSignOut.setOnClickListener(v -> confirmGoogleDisconnect());

        // Google disconnect (compact secondary info row থেকে — মোবাইল প্রাইমারি থাকা অবস্থায়)
        if (btnGoogleSecondaryDisconnect != null)
            btnGoogleSecondaryDisconnect.setOnClickListener(v -> confirmGoogleDisconnect());

        // ── Profile edit — save all fields together ──
        View btnSave = findViewById(R.id.btnSaveName);
        if (btnSave != null) btnSave.setOnClickListener(v -> saveProfileFields());

        // Edit Profile card খোলা/বন্ধ করার টগল
        if (headerEditProfile != null) headerEditProfile.setOnClickListener(v -> toggleEditProfile());

        // ── Change password ──
        View btnChangePassword = findViewById(R.id.btnChangePassword);
        if (btnChangePassword != null) btnChangePassword.setOnClickListener(v -> changePassword());

        // Change Password card খোলা/বন্ধ করার টগল
        if (headerChangePassword != null) headerChangePassword.setOnClickListener(v -> toggleChangePassword());

        if (tvTogglePasswordVisibility != null) {
            tvTogglePasswordVisibility.setOnClickListener(v -> {
                passwordsVisible = !passwordsVisible;
                int inputType = passwordsVisible
                    ? (android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
                    : (android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                for (TextInputEditText et : new TextInputEditText[]{etOldPassword, etNewPassword, etConfirmPassword}) {
                    if (et == null) continue;
                    et.setInputType(inputType);
                    et.setSelection(et.getText() != null ? et.getText().length() : 0);
                }
                tvTogglePasswordVisibility.setText(passwordsVisible ? "🙈  লুকান" : "👁  দেখান");
            });
        }
    }

    // ═══════════════════════════════════════════════════════
    //  Google disconnect — বড় কার্ড ও compact secondary row দুই জায়গা থেকেই ডাকা হয়
    // ═══════════════════════════════════════════════════════
    private void confirmGoogleDisconnect() {
        new AlertDialog.Builder(this, R.style.AppDialog)
            .setTitle("Google সাইন আউট")
            .setMessage("Google অ্যাকাউন্ট সংযোগ বিচ্ছিন্ন করবেন? এতে Auto Sync (Google Drive) বন্ধ হয়ে যাবে।")
            .setPositiveButton("হ্যাঁ", (d, w) ->
                signInHelper.signOut(this, () -> {
                    db.clearGoogleAccount();
                    // Google অ্যাকাউন্ট বিচ্ছিন্ন হলে Auto Sync-ও বন্ধ করে দিই — নাহলে
                    // toggle অন থাকা অবস্থায় sync চলার চেষ্টা করবে অথচ account নেই
                    db.setDriveAutoSyncEnabled(false);
                    com.jrappspot.cashlipi.utils.BackupManager backupManager =
                        com.jrappspot.cashlipi.utils.BackupManager.getInstance(this);
                    com.jrappspot.cashlipi.models.BackupSettings settings = backupManager.getSettings();
                    settings.setGoogleDriveEnabled(false);
                    settings.setAutoBackupEnabled(false);
                    backupManager.saveSettings(settings);
                    refreshUI();
                    Toast.makeText(this, "সাইন আউট হয়েছে", Toast.LENGTH_SHORT).show();
                }))
            .setNegativeButton("না", null).show();
    }

    // ═══════════════════════════════════════════════════════
    //  Edit Profile / Change Password কার্ড খোলা-বন্ধ করার টগল
    // ═══════════════════════════════════════════════════════
    private void toggleEditProfile() {
        if (bodyEditProfile == null) return;
        boolean expanding = bodyEditProfile.getVisibility() != View.VISIBLE;
        bodyEditProfile.setVisibility(expanding ? View.VISIBLE : View.GONE);
        if (tvEditProfileChevron != null) {
            tvEditProfileChevron.setText(expanding ? "✏️ বন্ধ করুন  ▴" : "✏️ এডিট করুন  ▾");
        }
    }

    private void toggleChangePassword() {
        if (bodyChangePassword == null) return;
        boolean expanding = bodyChangePassword.getVisibility() != View.VISIBLE;
        bodyChangePassword.setVisibility(expanding ? View.VISIBLE : View.GONE);
        if (tvChangePasswordChevron != null) {
            tvChangePasswordChevron.setText(expanding ? "🔑 বন্ধ করুন  ▴" : "🔑 পরিবর্তন  ▾");
        }
    }

    // ═══════════════════════════════════════════════════════
    //  Save profile fields (name / username / phone / email)
    // ═══════════════════════════════════════════════════════
    private void saveProfileFields() {
        String name  = etProfileName  != null && etProfileName.getText()  != null ? etProfileName.getText().toString().trim()  : "";
        String user  = etUsername     != null && etUsername.getText()     != null ? etUsername.getText().toString().trim()     : "";
        String phone = etPhoneNumber  != null && etPhoneNumber.getText()  != null ? etPhoneNumber.getText().toString().trim()  : "";
        String email = etEmail        != null && etEmail.getText()        != null ? etEmail.getText().toString().trim()        : "";

        if (name.isEmpty()) {
            etProfileName.setError("নাম লিখুন");
            Toast.makeText(this, "নাম লিখুন", Toast.LENGTH_SHORT).show();
            return;
        }

        db.saveCustomName(name);
        db.saveUsername(user);
        db.savePhoneNumber(phone);
        db.saveUserEmail(email);

        refreshUI();
        Toast.makeText(this, " প্রোফাইল তথ্য সেভ হয়েছে!", Toast.LENGTH_SHORT).show();

        // সেভ হওয়ার পর ফর্মটা বন্ধ করে দিই — প্রোফাইল কার্ড আবার কম্প্যাক্ট দেখাবে
        if (bodyEditProfile != null) bodyEditProfile.setVisibility(View.GONE);
        if (tvEditProfileChevron != null) tvEditProfileChevron.setText("✏️ এডিট করুন  ▾");
    }

    // ═══════════════════════════════════════════════════════
    //  Change password (Firebase Auth — email/phone account only)
    // ═══════════════════════════════════════════════════════
    private void changePassword() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || TextUtils.isEmpty(user.getEmail())) {
            Toast.makeText(this,
                "পাসওয়ার্ড পরিবর্তন করতে নম্বর/ইমেইল দিয়ে সাইন ইন থাকতে হবে",
                Toast.LENGTH_LONG).show();
            return;
        }

        String oldPass = etOldPassword     != null && etOldPassword.getText()     != null ? etOldPassword.getText().toString()     : "";
        String newPass = etNewPassword     != null && etNewPassword.getText()     != null ? etNewPassword.getText().toString()     : "";
        String confirm = etConfirmPassword != null && etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString() : "";

        if (TextUtils.isEmpty(oldPass)) {
            etOldPassword.setError("পুরাতন পাসওয়ার্ড দিন");
            return;
        }
        if (TextUtils.isEmpty(newPass) || newPass.length() < 6) {
            etNewPassword.setError("কমপক্ষে ৬ ডিজিট/অক্ষরের নতুন পাসওয়ার্ড দিন");
            return;
        }
        if (!newPass.equals(confirm)) {
            etConfirmPassword.setError("পাসওয়ার্ড মিলছে না");
            Toast.makeText(this, "নতুন পাসওয়ার্ড ও নিশ্চিতকরণ পাসওয়ার্ড মিলছে না", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldPass);

        user.reauthenticate(credential)
            .addOnSuccessListener(unused -> user.updatePassword(newPass)
                .addOnSuccessListener(unused2 -> {
                    Toast.makeText(this, " পাসওয়ার্ড পরিবর্তন হয়েছে!", Toast.LENGTH_SHORT).show();
                    if (etOldPassword != null) etOldPassword.setText("");
                    if (etNewPassword != null) etNewPassword.setText("");
                    if (etConfirmPassword != null) etConfirmPassword.setText("");
                    // পরিবর্তন হওয়ার পর ফর্মটা বন্ধ করে দিই
                    if (bodyChangePassword != null) bodyChangePassword.setVisibility(View.GONE);
                    if (tvChangePasswordChevron != null) tvChangePasswordChevron.setText("🔑 পরিবর্তন  ▾");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "updatePassword failed", e);
                    Toast.makeText(this, "❌ পাসওয়ার্ড আপডেট ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show();
                }))
            .addOnFailureListener(e -> {
                Log.e(TAG, "reauthenticate failed", e);
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (msg.contains("INVALID_LOGIN_CREDENTIALS") || msg.contains("wrong-password") || msg.contains("password is invalid")) {
                    etOldPassword.setError("পুরাতন পাসওয়ার্ড ভুল হয়েছে");
                    Toast.makeText(this, "❌ পুরাতন পাসওয়ার্ড ভুল হয়েছে", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "❌ যাচাই করতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show();
                }
            });
    }

    /**
     * নাম থেকে avatar-এ দেখানোর জন্য ১-২ অক্ষরের initials বানায়।
     * যেমন: "Jakir Al Jihad" → "JJ", "জাকির" → "জা"
     */
    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty() || name.equals("ব্যবহারকারী")) return "👤";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length && sb.length() < 2; i++) {
            if (!parts[i].isEmpty()) sb.append(parts[i].charAt(0));
        }
        return sb.length() > 0 ? sb.toString().toUpperCase() : "👤";
    }

    // ═══════════════════════════════════════════════════════
    //  Photo picker dialog
    // ═══════════════════════════════════════════════════════
    private void showPhotoOptions() {
        boolean hasLocal  = !db.getLocalPhotoPath().isEmpty();
        boolean hasGoogle = db.isGoogleSignedIn() && !db.getGooglePhotoUrl().isEmpty();

        java.util.List<String> opts = new java.util.ArrayList<>();
        opts.add(" গ্যালারি থেকে বেছে নিন");
        if (hasGoogle && hasLocal) opts.add(" Google ফটো ব্যবহার করুন");
        if (hasLocal || hasGoogle) opts.add(" ছবি সরিয়ে দিন");

        String[] options = opts.toArray(new String[0]);

        new AlertDialog.Builder(this, R.style.AppDialog)
            .setTitle("প্রোফাইল ছবি")
            .setItems(options, (d, which) -> {
                String choice = options[which];
                if (choice.contains("গ্যালারি")) {
                    galleryLauncher.launch("image/*");
                } else if (choice.contains("Google")) {
                    db.saveLocalPhotoPath(""); // local সরিয়ে Google URL ব্যবহার
                    refreshUI();
                } else if (choice.contains("সরিয়ে")) {
                    db.saveLocalPhotoPath("");
                    refreshUI();
                }
            }).show();
    }

    // ═══════════════════════════════════════════════════════
    //  Save photo from gallery to internal storage
    // ═══════════════════════════════════════════════════════
    private void saveLocalPhoto(Uri uri) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            File file = new File(getFilesDir(), "profile_photo.jpg");
            FileOutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in.close(); out.close();
            db.saveLocalPhotoPath(file.getAbsolutePath());
            refreshUI();
            Toast.makeText(this, " ছবি সেভ হয়েছে!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "saveLocalPhoto failed", e);
            Toast.makeText(this, "ছবি সেভ করতে ব্যর্থ", Toast.LENGTH_SHORT).show();
        }
    }

    // ═══════════════════════════════════════════════════════
    //  Refresh UI
    // ═══════════════════════════════════════════════════════
    // ── Auto Sync (Google Drive) ──
    private void setupAutoSyncToggle() {
        if (swAutoSync == null) return;

        swAutoSync.setChecked(db.isDriveAutoSyncEnabled());
        updateAutoSyncStatusText();

        swAutoSync.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked && !db.isGoogleSignedIn()) {
                // Drive-এ sync করতে হলে আগে Google দিয়ে সাইন-ইন থাকা লাগবে
                Toast.makeText(this, "Auto Sync চালু করতে আগে Google দিয়ে সাইন ইন করুন", Toast.LENGTH_LONG).show();
                swAutoSync.setChecked(false);
                return;
            }

            db.setDriveAutoSyncEnabled(isChecked);

            com.jrappspot.cashlipi.utils.BackupManager backupManager = com.jrappspot.cashlipi.utils.BackupManager.getInstance(this);
            com.jrappspot.cashlipi.models.BackupSettings settings = backupManager.getSettings();
            settings.setGoogleDriveEnabled(isChecked);
            settings.setAutoBackupEnabled(isChecked);
            backupManager.saveSettings(settings);

            updateAutoSyncStatusText();

            if (isChecked) {
                Toast.makeText(this, "☁️ Auto Sync চালু হয়েছে", Toast.LENGTH_SHORT).show();
                backupManager.triggerAutoGoogleDriveSync();
            } else {
                Toast.makeText(this, "Auto Sync বন্ধ হয়েছে", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateAutoSyncStatusText() {
        if (tvAutoSyncStatus == null) return;
        if (!db.isGoogleSignedIn()) {
            tvAutoSyncStatus.setText("⚪ Google সাইন-ইন করুন Auto Sync চালু করতে");
        } else if (db.isDriveAutoSyncEnabled()) {
            tvAutoSyncStatus.setText("🟢 চালু আছে — প্রতিটি পরিবর্তন সাথে সাথে Drive-এ সেভ হবে");
        } else {
            tvAutoSyncStatus.setText("🔴 বন্ধ আছে — ম্যানুয়ালি Backup Center থেকে ব্যাকআপ নিতে হবে");
        }
    }

    private void refreshUI() {
        if (swAutoSync != null) {
            swAutoSync.setChecked(db.isDriveAutoSyncEnabled());
        }
        updateAutoSyncStatusText();

        // Display name
        String name = db.getDisplayName();
        if (tvDisplayName != null) tvDisplayName.setText(name);
        if (etProfileName != null) etProfileName.setText(db.getCustomName().isEmpty()
            ? db.getGoogleName() : db.getCustomName());
        if (etUsername != null) etUsername.setText(db.getUsername());
        if (etPhoneNumber != null) etPhoneNumber.setText(db.getPhoneNumber());

        // 'Google email' ফিল্ড আসলে ফোন-সাইনআপ ইউজারদের জন্য পুরোনো ডাটা-restore
        // ফিচারের কাজে (legacy backup key হিসেবে) ফোন নম্বরও ধারণ করতে পারে —
        // তাই এখানে শুধু তখনই fallback হিসেবে ব্যবহার করি যখন এটা আসলেই একটা ইমেইল
        // (মানে "@" আছে), নাহলে ফোন নম্বর ভুলবশত ইমেইল বক্সে দেখানো হয়ে যাবে।
        String googleEmail = db.getGoogleEmail();
        boolean googleEmailLooksValid = googleEmail != null && googleEmail.contains("@");
        if (etEmail != null) etEmail.setText(!db.getUserEmail().isEmpty()
            ? db.getUserEmail()
            : (googleEmailLooksValid ? googleEmail : ""));

        // Google badge
        if (tvGoogleBadge != null)
            tvGoogleBadge.setVisibility(db.isGoogleSignedIn() ? View.VISIBLE : View.GONE);

        // Main profile photo (local → Google URL → default)
        String photoSource = db.getEffectivePhotoSource();
        boolean hasRealPhoto = photoSource != null && !photoSource.isEmpty();

        // ছবি না থাকলে জেনেরিক ধূসর আইকনের বদলে নামের আদ্যক্ষর দিয়ে
        // একটা রঙিন avatar দেখানো হয় — বেশিরভাগ modern অ্যাপের মতো
        if (tvProfileInitials != null) {
            tvProfileInitials.setText(getInitials(name));
            tvProfileInitials.setVisibility(hasRealPhoto ? View.GONE : View.VISIBLE);
        }
        if (ivProfilePhoto != null) {
            ivProfilePhoto.setVisibility(hasRealPhoto ? View.VISIBLE : View.GONE);
        }
        loadProfilePhoto(ivProfilePhoto, photoSource, 100);

        // ── Sign-in options: মোবাইল/ইমেইল ও Google — একসাথে দুইটা "অপশন" দেখানো হবে না ──
        // মোবাইল/ইমেইল দিয়ে সাইন-ইন থাকলে সেটাই প্রাইমারি ধরা হয় — তখন Google অংশ
        // (label + divider + signed-in/sign-in দুই state-ই) সম্পূর্ণ hide থাকবে।
        // Google অ্যাকাউন্ট Auto Sync (Drive)-এর জন্য এখনও connect করা যাবে,
        // কিন্তু সেটা নিচের "Auto Sync (Google Drive)" টগল থেকেই হবে।
        boolean phoneSignedIn  = db.isEmailPhoneSignedIn();
        boolean googleSignedIn = db.isGoogleSignedIn();

        if (phoneSignedIn) {
            // শুধু মোবাইল/ইমেইল প্রাইমারি কার্ড
            if (tvPhoneSectionLabel != null) tvPhoneSectionLabel.setVisibility(View.VISIBLE);
            if (layoutPhoneSignedIn != null) layoutPhoneSignedIn.setVisibility(View.VISIBLE);
            if (layoutPhoneSignIn   != null) layoutPhoneSignIn.setVisibility(View.GONE);

            // Google-এর বড় label/divider/full block — hidden (দুইটা আলাদা "অপশন" দেখাবে না)
            if (dividerSignInMethods != null) dividerSignInMethods.setVisibility(View.GONE);
            if (tvGoogleSectionLabel != null) tvGoogleSectionLabel.setVisibility(View.GONE);
            if (layoutGoogleSignedIn != null) layoutGoogleSignedIn.setVisibility(View.GONE);
            if (layoutGoogleSignIn   != null) layoutGoogleSignIn.setVisibility(View.GONE);

            // Google-ও যদি connected থাকে (Auto Sync-এর জন্য) — compact secondary row দেখাও
            if (layoutGoogleSecondaryInfo != null)
                layoutGoogleSecondaryInfo.setVisibility(googleSignedIn ? View.VISIBLE : View.GONE);
            if (googleSignedIn && tvGoogleSecondaryEmail != null) {
                tvGoogleSecondaryEmail.setText(db.getGoogleEmail());
            }

        } else if (googleSignedIn) {
            // মোবাইল/ইমেইল সাইন-ইন নেই কিন্তু Google আছে — Google-ই প্রাইমারি কার্ড
            if (tvPhoneSectionLabel != null) tvPhoneSectionLabel.setVisibility(View.GONE);
            if (layoutPhoneSignedIn != null) layoutPhoneSignedIn.setVisibility(View.GONE);
            if (layoutPhoneSignIn   != null) layoutPhoneSignIn.setVisibility(View.GONE);

            if (dividerSignInMethods != null) dividerSignInMethods.setVisibility(View.GONE);
            if (tvGoogleSectionLabel != null) tvGoogleSectionLabel.setVisibility(View.VISIBLE);
            if (layoutGoogleSignedIn != null) layoutGoogleSignedIn.setVisibility(View.VISIBLE);
            if (layoutGoogleSignIn   != null) layoutGoogleSignIn.setVisibility(View.GONE);

            if (layoutGoogleSecondaryInfo != null) layoutGoogleSecondaryInfo.setVisibility(View.GONE);

            if (tvGoogleName  != null) tvGoogleName.setText(db.getGoogleName());
            if (tvGoogleEmail != null) tvGoogleEmail.setText(db.getGoogleEmail());
            loadProfilePhoto(ivGooglePhoto, db.getGooglePhotoUrl(), 44);

        } else {
            // কেউই সাইন-ইন করা নেই — দুইটা মেথডই "সাইন ইন করুন" prompt আকারে দেখাও
            if (tvPhoneSectionLabel != null) tvPhoneSectionLabel.setVisibility(View.VISIBLE);
            if (layoutPhoneSignedIn != null) layoutPhoneSignedIn.setVisibility(View.GONE);
            if (layoutPhoneSignIn   != null) layoutPhoneSignIn.setVisibility(View.VISIBLE);

            if (dividerSignInMethods != null) dividerSignInMethods.setVisibility(View.VISIBLE);
            if (tvGoogleSectionLabel != null) tvGoogleSectionLabel.setVisibility(View.VISIBLE);
            if (layoutGoogleSignedIn != null) layoutGoogleSignedIn.setVisibility(View.GONE);
            if (layoutGoogleSignIn   != null) layoutGoogleSignIn.setVisibility(View.VISIBLE);

            if (layoutGoogleSecondaryInfo != null) layoutGoogleSecondaryInfo.setVisibility(View.GONE);
        }

        if (phoneSignedIn && tvSignedInIdentity != null) {
            String identity = !db.getPhoneNumber().isEmpty() ? db.getPhoneNumber() : db.getUserEmail();
            tvSignedInIdentity.setText(identity);
        }
    }

    /**
     * Load image with Glide — supports:
     * - https:// Google photo URL (size param auto-added)
     * - /data/... local file path
     * - empty → default icon with tint
     */
    private void loadProfilePhoto(ImageView iv, String source, int sizeDp) {
        if (iv == null) return;

        if (source == null || source.isEmpty()) {
            iv.setImageResource(android.R.drawable.ic_menu_myplaces);
            iv.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.profileIconTint));
            return;
        }

        // আগের কোনো tint থাকলে clear করি — না হলে Glide-এর ছবিও tinted দেখাবে
        iv.clearColorFilter();

        Object loadFrom;
        if (source.startsWith("http")) {
            //  FIX: Google photo URL-এ size parameter যুক্ত করি (নাহলে অনেক সময় লোড হয় না বা ছোট আসে)
            String sized = source;
            int pxSize = sizeDp * 3; // approx px for hi-dpi
            if (sized.contains("=s")) {
                sized = sized.replaceAll("=s\\d+(-c)?", "=s" + pxSize + "-c");
            } else {
                sized = sized + "=s" + pxSize + "-c";
            }
            loadFrom = sized;
        } else {
            loadFrom = new File(source);
        }

        Glide.with(this)
            .load(loadFrom)
            .transform(new CircleCrop())
            .placeholder(android.R.drawable.ic_menu_myplaces)
            .error(android.R.drawable.ic_menu_myplaces)
            .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                @Override
                public boolean onLoadFailed(GlideException e, Object model, Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                    Log.e(TAG, " Profile photo load failed for: " + model, e);
                    // Fallback চেষ্টা: size param ছাড়া আবার লোড করি (URL malformed হতে পারে)
                    if (model instanceof String && ((String) model).contains("=s")) {
                        String fallback = ((String) model).replaceAll("=s\\d+(-c)?", "");
                        Glide.with(ProfileActivity.this)
                            .load(fallback)
                            .transform(new CircleCrop())
                            .placeholder(android.R.drawable.ic_menu_myplaces)
                            .error(android.R.drawable.ic_menu_myplaces)
                            .into(iv);
                    }
                    return false;
                }

                @Override
                public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, Target<android.graphics.drawable.Drawable> target, DataSource dataSource, boolean isFirstResource) {
                    Log.d(TAG, " Profile photo loaded: " + model);
                    return false;
                }
            })
            .into(iv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUI();
    }
}
