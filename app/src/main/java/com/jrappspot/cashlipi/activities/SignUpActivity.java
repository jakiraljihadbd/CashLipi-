package com.jrappspot.cashlipi.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.utils.AuthDialogHelper;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private EditText etName, etUsername, etPhone, etGmail, etPassword, etConfirmPassword;
    private Button btnSignUp;
    private TextView tvLoginLink;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // ── Country code (মোবাইল নম্বর ফিল্ডের জন্য) ──
    private TextView tvCountryCode;
    private LinearLayout layoutCountryCode;
    private int selectedCountryIndex = 0; // default: +880 (Bangladesh)

    // { label, code, hint, digitLength, requireLeadingZero("1"=yes / "0"=no) }
    private static final String[][] COUNTRY_CODES = {
        {"🇧🇩 +880", "+880", "01XXXXXXXXX", "11", "1"},
        {"🇮🇳 +91",  "+91",  "XXXXXXXXXX",  "10", "0"},
        {"🇵🇰 +92",  "+92",  "XXXXXXXXXX",  "10", "0"},
        {"🇸🇦 +966", "+966", "XXXXXXXXX",   "9",  "0"},
        {"🇦🇪 +971", "+971", "XXXXXXXXX",   "9",  "0"},
        {"🇲🇾 +60",  "+60",  "XXXXXXXXX",   "9",  "0"},
        {"🇸🇬 +65",  "+65",  "XXXXXXXX",    "8",  "0"},
        {"🇺🇸 +1",   "+1",   "XXXXXXXXXX",  "10", "0"},
        {"🇬🇧 +44",  "+44",  "XXXXXXXXXX",  "10", "0"}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etName = findViewById(R.id.etName);
        etUsername = findViewById(R.id.etUsername);
        etPhone = findViewById(R.id.etPhone);
        etGmail = findViewById(R.id.etGmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvLoginLink = findViewById(R.id.tvLoginLink);
        progressBar = findViewById(R.id.progressBar);

        tvCountryCode = findViewById(R.id.tvCountryCode);
        layoutCountryCode = findViewById(R.id.layoutCountryCode);
        if (layoutCountryCode != null) layoutCountryCode.setOnClickListener(v -> showCountryCodePicker());
        applyPhoneFormat();

        // Password toggle
        TextView tvToggle = findViewById(R.id.tvTogglePassword);
        if (tvToggle != null) {
            final boolean[] vis = {false};
            tvToggle.setOnClickListener(v -> {
                vis[0] = !vis[0];
                etPassword.setTransformationMethod(vis[0]
                    ? HideReturnsTransformationMethod.getInstance()
                    : PasswordTransformationMethod.getInstance());
                tvToggle.setText(vis[0] ? "🙈" : "👁");
                etPassword.setSelection(etPassword.getText().length());
            });
        }
        TextView tvToggleConfirm = findViewById(R.id.tvToggleConfirmPassword);
        if (tvToggleConfirm != null) {
            final boolean[] vis2 = {false};
            tvToggleConfirm.setOnClickListener(v -> {
                vis2[0] = !vis2[0];
                etConfirmPassword.setTransformationMethod(vis2[0]
                    ? HideReturnsTransformationMethod.getInstance()
                    : PasswordTransformationMethod.getInstance());
                tvToggleConfirm.setText(vis2[0] ? "🙈" : "👁");
                etConfirmPassword.setSelection(etConfirmPassword.getText().length());
            });
        }

        btnSignUp.setOnClickListener(v -> doSignUp());
        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(this, EmailLoginActivity.class));
            finish();
        });
    }

    /** নির্বাচিত country code অনুযায়ী phone number field-এর hint, দৈর্ঘ্য এবং leading-zero নিয়ম আপডেট করে। */
    private void applyPhoneFormat() {
        String[] country = COUNTRY_CODES[selectedCountryIndex];
        String hint = country[2];
        final int digitLength = Integer.parseInt(country[3]);
        final boolean requireLeadingZero = "1".equals(country[4]);

        etPhone.setHint(hint);
        etPhone.setFilters(new InputFilter[]{
            new InputFilter.LengthFilter(digitLength),
            (source, start, end, dest, dstart, dend) -> {
                for (int i = start; i < end; i++) {
                    if (!Character.isDigit(source.charAt(i))) return "";
                }
                if (requireLeadingZero) {
                    String result = dest.toString().substring(0, dstart)
                        + source.subSequence(start, end)
                        + dest.toString().substring(dend);
                    if (!result.isEmpty() && result.charAt(0) != '0') return "";
                }
                return null;
            }
        });
    }

    private void showCountryCodePicker() {
        String[] labels = new String[COUNTRY_CODES.length];
        for (int i = 0; i < COUNTRY_CODES.length; i++) labels[i] = COUNTRY_CODES[i][0];

        new AlertDialog.Builder(this, R.style.AppDialog)
            .setTitle("দেশ নির্বাচন করুন")
            .setItems(labels, (d, which) -> {
                selectedCountryIndex = which;
                if (tvCountryCode != null) tvCountryCode.setText(labels[which]);
                etPhone.setText("");
                applyPhoneFormat();
            })
            .show();
    }

    // ═══════════════════════════════════════════════════════
    //  Sign up
    // ═══════════════════════════════════════════════════════
    private void doSignUp() {
        String name = etName.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String rawPhone = etPhone.getText().toString().trim();
        String gmail = etGmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etName.setError("নাম দিন");
            return;
        }
        if (TextUtils.isEmpty(username)) {
            etUsername.setError("ইউজারনেম দিন");
            return;
        }
        if (username.length() < 3) {
            etUsername.setError("কমপক্ষে ৩ অক্ষরের ইউজারনেম দিন");
            return;
        }
        if (!username.matches("^[a-zA-Z0-9_.]+$")) {
            etUsername.setError("শুধু ইংরেজি অক্ষর, সংখ্যা, _ এবং . ব্যবহার করুন");
            return;
        }

        String[] country = COUNTRY_CODES[selectedCountryIndex];
        String selectedCountryCode = country[1];
        int requiredDigitLength = Integer.parseInt(country[3]);
        boolean requireLeadingZero = "1".equals(country[4]);

        if (TextUtils.isEmpty(rawPhone)) {
            etPhone.setError("মোবাইল নম্বর দিন");
            return;
        }
        if (rawPhone.length() != requiredDigitLength) {
            etPhone.setError("সঠিক " + requiredDigitLength + " ডিজিটের মোবাইল নম্বর দিন");
            return;
        }
        if (requireLeadingZero && !rawPhone.startsWith("0")) {
            etPhone.setError("নম্বরটি অবশ্যই ০ (0) দিয়ে শুরু হতে হবে");
            return;
        }

        if (TextUtils.isEmpty(gmail)) {
            etGmail.setError("Gmail দিন");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(gmail).matches()) {
            etGmail.setError("সঠিক Gmail/Email দিন");
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("কমপক্ষে ৬ অক্ষরের password দিন");
            return;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Password মিলছে না");
            return;
        }

        final String phone = "+880".equals(selectedCountryCode) ? rawPhone : (selectedCountryCode + rawPhone);

        progressBar.setVisibility(View.VISIBLE);
        btnSignUp.setEnabled(false);

        checkUsernameAvailable(username, available -> {
            if (!available) {
                progressBar.setVisibility(View.GONE);
                btnSignUp.setEnabled(true);
                etUsername.setError("এই ইউজারনেমটি আগে থেকেই ব্যবহৃত হচ্ছে");
                Toast.makeText(this, "❌ ইউজারনেমটি আগে থেকেই আছে, অন্য একটি চেষ্টা করুন", Toast.LENGTH_LONG).show();
                return;
            }

            // Gmail + password দিয়ে Firebase Auth অ্যাকাউন্ট তৈরি হয়, মোবাইল নম্বরটি
            // প্রোফাইলে অতিরিক্ত তথ্য হিসেবে সংরক্ষিত থাকে।
            auth.createUserWithEmailAndPassword(gmail, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    saveUserProfile(user.getUid(), name, username, gmail, phone);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnSignUp.setEnabled(true);
                    showSignUpErrorPopup(e.getMessage());
                });
        });
    }

    private interface AvailabilityCallback {
        void onResult(boolean available);
    }

    /** Firestore-এ query করে দেখা হয় এই ইউজারনেমটি আগে থেকেই কেউ ব্যবহার করছে কিনা। */
    private void checkUsernameAvailable(String username, AvailabilityCallback callback) {
        db.collection("users")
            .whereEqualTo("username", username)
            .limit(1)
            .get()
            .addOnSuccessListener((QuerySnapshot snapshot) -> callback.onResult(snapshot.isEmpty()))
            .addOnFailureListener(e -> {
                // যাচাই ব্যর্থ হলেও সাইন-আপ চালিয়ে যাওয়ার সুযোগ দিই (network/rules সমস্যা হতে পারে)
                callback.onResult(true);
            });
    }

    private void showSignUpErrorPopup(String rawMessage) {
        String msg = rawMessage != null ? rawMessage : "";
        if (msg.contains("already in use")) {
            AuthDialogHelper.show(this, AuthDialogHelper.Type.WRONG_PASSWORD,
                "অ্যাকাউন্ট আগে থেকেই আছে",
                "এই Gmail দিয়ে আগেই একটি অ্যাকাউন্ট তৈরি করা আছে। লগইন করুন।",
                "লগইন করুন", () -> {
                    startActivity(new Intent(this, EmailLoginActivity.class));
                    finish();
                },
                "বাতিল", null);
        } else {
            AuthDialogHelper.show(this, AuthDialogHelper.Type.GENERIC,
                "সমস্যা হয়েছে", friendlyError(msg),
                "ঠিক আছে", null, null, null);
        }
    }

    private void saveUserProfile(String uid, String name, String username, String email, String phone) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("uid", uid);
        profile.put("name", name);
        profile.put("username", username);
        profile.put("email", email);
        profile.put("phone", phone);
        profile.put("blocked", false);
        profile.put("lastActive", System.currentTimeMillis());
        profile.put("authType", "email");

        db.collection("users").document(uid)
            .set(profile)
            .addOnSuccessListener(v -> {
                progressBar.setVisibility(View.GONE);

                // অ্যাকাউন্ট তৈরি হওয়ার পর Firebase স্বয়ংক্রিয়ভাবে sign-in করে রাখে —
                // কিন্তু আমরা চাই ইউজার লগইন পেজে ফিরে গিয়ে নিজে manually login করুক
                auth.signOut();

                Toast.makeText(this, "✅ অ্যাকাউন্ট তৈরি হয়েছে! এখন লগইন করুন", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, EmailLoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                btnSignUp.setEnabled(true);
                Toast.makeText(this, "❌ প্রোফাইল সেভ করতে সমস্যা: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }

    private String friendlyError(String msg) {
        if (msg == null) return "অজানা সমস্যা";
        if (msg.contains("email address is already in use")) return "এই Gmail দিয়ে আগেই অ্যাকাউন্ট আছে";
        if (msg.contains("badly formatted")) return "সঠিক Gmail দিন";
        if (msg.contains("network")) return "ইন্টারনেট সংযোগ চেক করুন";
        return msg;
    }
}
