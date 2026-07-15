package com.jrappspot.cashlipi.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
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
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.utils.AuthDialogHelper;
import com.jrappspot.cashlipi.utils.DatabaseManager;
import com.jrappspot.cashlipi.utils.FirestoreSyncManager;

public class EmailLoginActivity extends AppCompatActivity {

    private static final String TAG = "EmailLoginActivity";

    private EditText etEmailOrPhone, etPassword;
    private Button btnLogin;
    private TextView tvSignUpLink, tvForgotPassword, tvTogglePassword;
    private boolean passwordVisible = false;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirestoreSyncManager firestoreSync;

    // ── Login mode toggle (Phone / Gmail) ──
    private TextView btnTogglePhone, btnToggleGmail, tvIdentifierLabel, tvCountryCode;
    private LinearLayout layoutCountryCode;
    private boolean isPhoneMode = true;
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
        setContentView(R.layout.activity_email_login);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        firestoreSync = FirestoreSyncManager.getInstance(this);

        etEmailOrPhone = findViewById(R.id.etEmailOrPhone);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignUpLink = findViewById(R.id.tvSignUpLink);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        progressBar = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> doLogin());

        tvSignUpLink.setOnClickListener(v -> {
            startActivity(new Intent(this, SignUpActivity.class));
            finish();
        });

        tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });

        tvTogglePassword = findViewById(R.id.tvTogglePassword);
        if (tvTogglePassword != null) {
            tvTogglePassword.setOnClickListener(v -> {
                passwordVisible = !passwordVisible;
                if (passwordVisible) {
                    etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    tvTogglePassword.setText("🙈");
                } else {
                    etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    tvTogglePassword.setText("👁");
                }
                etPassword.setSelection(etPassword.getText().length());
            });
        }

        setupLoginModeToggle();
    }

    // ═══════════════════════════════════════════════════════
    //  Phone / Gmail mode toggle
    // ═══════════════════════════════════════════════════════
    private void setupLoginModeToggle() {
        btnTogglePhone     = findViewById(R.id.btnTogglePhone);
        btnToggleGmail     = findViewById(R.id.btnToggleGmail);
        tvIdentifierLabel  = findViewById(R.id.tvIdentifierLabel);
        tvCountryCode      = findViewById(R.id.tvCountryCode);
        layoutCountryCode  = findViewById(R.id.layoutCountryCode);

        if (btnTogglePhone != null) btnTogglePhone.setOnClickListener(v -> setLoginMode(true));
        if (btnToggleGmail != null) btnToggleGmail.setOnClickListener(v -> setLoginMode(false));
        if (layoutCountryCode != null) layoutCountryCode.setOnClickListener(v -> showCountryCodePicker());

        setLoginMode(false); // default: Gmail
    }

    private void setLoginMode(boolean phoneMode) {
        isPhoneMode = phoneMode;
        etEmailOrPhone.setText("");

        if (btnTogglePhone != null && btnToggleGmail != null) {
            if (phoneMode) {
                btnTogglePhone.setBackgroundResource(R.drawable.bg_login_toggle_selected);
                btnTogglePhone.setTextColor(0xFFFFFFFF);
                btnToggleGmail.setBackgroundColor(0x00000000);
                btnToggleGmail.setTextColor(0xFF94A3B8);
            } else {
                btnToggleGmail.setBackgroundResource(R.drawable.bg_login_toggle_selected);
                btnToggleGmail.setTextColor(0xFFFFFFFF);
                btnTogglePhone.setBackgroundColor(0x00000000);
                btnTogglePhone.setTextColor(0xFF94A3B8);
            }
        }

        if (phoneMode) {
            if (tvIdentifierLabel != null) tvIdentifierLabel.setText("মোবাইল নম্বর");
            if (layoutCountryCode != null) layoutCountryCode.setVisibility(View.VISIBLE);
            etEmailOrPhone.setInputType(InputType.TYPE_CLASS_PHONE);
            applyPhoneFormat();
        } else {
            if (tvIdentifierLabel != null) tvIdentifierLabel.setText("Gmail / Username");
            if (layoutCountryCode != null) layoutCountryCode.setVisibility(View.GONE);
            etEmailOrPhone.setHint("example@gmail.com");
            etEmailOrPhone.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            etEmailOrPhone.setFilters(new InputFilter[]{});
        }
    }

    /** নির্বাচিত country code অনুযায়ী phone number field-এর hint, দৈর্ঘ্য এবং leading-zero নিয়ম আপডেট করে। */
    private void applyPhoneFormat() {
        String[] country = COUNTRY_CODES[selectedCountryIndex];
        String hint = country[2];
        final int digitLength = Integer.parseInt(country[3]);
        final boolean requireLeadingZero = "1".equals(country[4]);

        etEmailOrPhone.setHint(hint);
        etEmailOrPhone.setFilters(new InputFilter[]{
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
                etEmailOrPhone.setText("");
                if (isPhoneMode) applyPhoneFormat();
            })
            .show();
    }

    private void doLogin() {
        String rawInput = etEmailOrPhone.getText().toString().trim();
        String[] country = COUNTRY_CODES[selectedCountryIndex];
        String selectedCountryCode = country[1];
        int requiredDigitLength = Integer.parseInt(country[3]);
        boolean requireLeadingZero = "1".equals(country[4]);

        if (TextUtils.isEmpty(rawInput)) {
            etEmailOrPhone.setError(isPhoneMode ? "মোবাইল নম্বর দিন" : "Gmail/Username দিন");
            return;
        }

        if (isPhoneMode && rawInput.length() != requiredDigitLength) {
            etEmailOrPhone.setError("সঠিক " + requiredDigitLength + " ডিজিটের মোবাইল নম্বর দিন");
            return;
        }
        if (isPhoneMode && requireLeadingZero && !rawInput.startsWith("0")) {
            etEmailOrPhone.setError("নম্বরটি অবশ্যই ০ (0) দিয়ে শুরু হতে হবে");
            return;
        }

        // ফোন মোডে +880 (ডিফল্ট) হলে পুরনো অ্যাকাউন্টের সাথে সামঞ্জস্য রাখতে
        // শুধু নম্বরটাই ব্যবহার হবে। অন্য country code বাছাই করলে সেটা যুক্ত হবে।
        final String identifier;
        if (isPhoneMode) {
            identifier = "+880".equals(selectedCountryCode) ? rawInput : (selectedCountryCode + rawInput);
        } else {
            identifier = rawInput;
        }

        String password = etPassword.getText().toString().trim();
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password দিন");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        final boolean isEmail = !isPhoneMode && Patterns.EMAIL_ADDRESS.matcher(identifier).matches();

        if (isEmail) {
            // সরাসরি Gmail/Email দিয়ে সাইন-ইন — অ্যাকাউন্ট না থাকলে auth থেকেই এরর আসবে
            signInWithEmail(identifier, password, true, false, "email", identifier);
            return;
        }

        if (isPhoneMode) {
            // মোবাইল নম্বর দিয়ে Firestore-এ খুঁজে linked Gmail বের করা হচ্ছে
            resolveEmailByField("phone", identifier, resolvedEmail -> {
                if (resolvedEmail != null) {
                    signInWithEmail(resolvedEmail, password, false, true, "phone", identifier);
                } else {
                    // পুরনো (legacy) phone-pseudo-email অ্যাকাউন্টের জন্য fallback
                    String normalizedPhone = identifier.replaceAll("[^0-9+]", "");
                    String pseudoEmail = normalizedPhone.replace("+", "") + "@phone.cashlipi.app";
                    signInWithEmail(pseudoEmail, password, false, false, "phone", identifier);
                }
            });
        } else {
            // ইউজারনেম দিয়ে Firestore-এ খুঁজে linked Gmail বের করা হচ্ছে
            resolveEmailByField("username", identifier, resolvedEmail -> {
                if (resolvedEmail != null) {
                    signInWithEmail(resolvedEmail, password, false, true, "username", identifier);
                } else {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    showNotRegisteredPopup();
                }
            });
        }
    }

    private interface EmailLookupCallback {
        void onResult(String email);
    }

    /** Firestore-এর "users" কালেকশনে একটি field (phone/username) দিয়ে খুঁজে সংশ্লিষ্ট email বের করে। */
    private void resolveEmailByField(String field, String value, EmailLookupCallback callback) {
        db.collection("users")
            .whereEqualTo(field, value)
            .limit(1)
            .get()
            .addOnSuccessListener(snapshot -> {
                if (!snapshot.isEmpty()) {
                    callback.onResult(snapshot.getDocuments().get(0).getString("email"));
                } else {
                    callback.onResult(null);
                }
            })
            .addOnFailureListener(e -> callback.onResult(null));
    }

    /**
     * @param loginEmail          Firebase Auth-এ ব্যবহৃত হবে এমন email (আসল Gmail অথবা legacy pseudo-email)
     * @param isEmail             বার্তায় "Email" বলা হবে নাকি "নম্বর/ইউজারনেম" — শুধু popup-এর ভাষার জন্য
     * @param alreadyConfirmedExists  আগে থেকেই Firestore lookup-এ অ্যাকাউন্ট পাওয়া গেছে কিনা — পেলে
     *                                আলাদা করে checkAccountRegistered কল করার দরকার নেই, ভুল password ধরে নেওয়া যায়
     * @param checkField / checkValue  alreadyConfirmedExists false হলে, "user আছে কিনা" যাচাই করতে
     *                                  কোন field (email/phone) দিয়ে কী value খোঁজা হবে
     */
    private void signInWithEmail(String loginEmail, String password, boolean isEmail,
                                  boolean alreadyConfirmedExists, String checkField, String checkValue) {
        auth.signInWithEmailAndPassword(loginEmail, password)
            .addOnSuccessListener(result -> loadProfileAndContinue(result.getUser().getUid()))
            .addOnFailureListener(e -> {
                Log.e(TAG, "Sign-in failed", e);
                String msg = e.getMessage() != null ? e.getMessage() : "";

                if (msg.contains("no user record") || msg.contains("There is no user")
                        || msg.contains("user-not-found") || msg.contains("INVALID_LOGIN_CREDENTIALS")
                        || msg.contains("badly formatted") || msg.contains("INVALID_EMAIL")) {

                    if (alreadyConfirmedExists) {
                        // Firestore lookup-এ আগেই অ্যাকাউন্ট পাওয়া গেছে — মানে password-টাই ভুল
                        progressBar.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);
                        showWrongPasswordPopup(isEmail);
                        return;
                    }

                    // নতুন Firebase Auth ভার্সনে "wrong password" আর "user not found" দুটোই
                    // একই generic error code (INVALID_LOGIN_CREDENTIALS) হিসেবে আসে। তাই
                    // Firestore-এ query করে দেখা হচ্ছে এই Email/Phone দিয়ে আসলেই কোনো
                    // অ্যাকাউন্ট registered আছে কিনা — তার ভিত্তিতে সঠিক popup দেখানো হবে।
                    firestoreSync.checkAccountRegistered(checkValue, "email".equals(checkField), new FirestoreSyncManager.AccountCheckCallback() {
                        @Override
                        public void onResult(boolean registered) {
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                btnLogin.setEnabled(true);
                                if (registered) {
                                    showWrongPasswordPopup(isEmail);
                                } else {
                                    showNotRegisteredPopup();
                                }
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                btnLogin.setEnabled(true);
                                showCombinedPopup(isEmail);
                            });
                        }
                    });
                } else if (msg.contains("network") || msg.contains("NETWORK_REQUEST_FAILED")) {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    AuthDialogHelper.show(this, AuthDialogHelper.Type.NETWORK_ERROR,
                        "ইন্টারনেট সংযোগ নেই", "ইন্টারনেট সংযোগ চেক করে আবার চেষ্টা করুন।",
                        "ঠিক আছে", null, null, null);
                } else if (msg.contains("too-many-requests")) {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    AuthDialogHelper.show(this, AuthDialogHelper.Type.GENERIC,
                        "অনেকবার চেষ্টা হয়েছে", "নিরাপত্তার জন্য সাময়িকভাবে বন্ধ করা হয়েছে। কিছুক্ষণ পর আবার চেষ্টা করুন।",
                        "ঠিক আছে", null, null, null);
                } else {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    AuthDialogHelper.show(this, AuthDialogHelper.Type.GENERIC,
                        "সমস্যা হয়েছে", friendlyError(msg),
                        "ঠিক আছে", null, null, null);
                }
            });
    }

    private void showWrongPasswordPopup(boolean isEmail) {
        AuthDialogHelper.show(this, AuthDialogHelper.Type.WRONG_PASSWORD,
            "Password ভুল হয়েছে",
            "আপনার " + (isEmail ? "Email" : "নম্বর") + "-টি সঠিক, কিন্তু Password মিলছে না। আবার চেষ্টা করুন।",
            "আবার চেষ্টা করুন", null,
            "Password ভুলে গেছেন?", () -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }

    private void showNotRegisteredPopup() {
        AuthDialogHelper.show(this, AuthDialogHelper.Type.NOT_REGISTERED,
            "অ্যাকাউন্ট পাওয়া যায়নি",
            "এই Email/Phone দিয়ে কোনো অ্যাকাউন্ট রেজিস্টার করা নেই। নতুন অ্যাকাউন্ট তৈরি করুন।",
            "সাইন আপ করুন", () -> {
                startActivity(new Intent(this, SignUpActivity.class));
                finish();
            },
            "বাতিল", null);
    }

    private void showCombinedPopup(boolean isEmail) {
        AuthDialogHelper.show(this, AuthDialogHelper.Type.GENERIC,
            "লগইন ব্যর্থ হয়েছে",
            "এই " + (isEmail ? "Email" : "নম্বর") + " অথবা Password সঠিক নয়। অ্যাকাউন্ট না থাকলে সাইন আপ করুন।",
            "আবার চেষ্টা করুন", null,
            "সাইন আপ করুন", () -> {
                startActivity(new Intent(this, SignUpActivity.class));
                finish();
            });
    }

    private void loadProfileAndContinue(String uid) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener(doc -> {
                Boolean blocked = doc.getBoolean("blocked");
                if (blocked != null && blocked) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "🚫 আপনার অ্যাকাউন্ট ব্লক করা হয়েছে", Toast.LENGTH_LONG).show();
                    btnLogin.setEnabled(true);
                    auth.signOut();
                    return;
                }

                // ✅ Login সম্পন্ন — flag সেট করো, নাহলে app restart (language/font change)
                // হলে SplashActivity আবার LoginActivity-তে পাঠিয়ে দেবে
                DatabaseManager dbManager = DatabaseManager.getInstance(this);
                dbManager.setLoginDone(true);
                // ✅ এইটা প্রকৃত email/phone/username Firebase Auth সাইন-ইন —
                // তাই প্রোফাইল স্ক্রিনে এখন "নম্বর/ইমেইল দিয়ে সাইন-ইন করা" অংশ দেখাবে
                dbManager.setEmailPhoneSignedIn(true);

                String name = doc.getString("name");
                String username = doc.getString("username");
                String email = doc.getString("email");
                String phone = doc.getString("phone");

                dbManager.saveGoogleAccount(name, email != null ? email : phone, null);
                // নতুন অ্যাকাউন্টে সাইন-আপের সময় দেওয়া প্রকৃত ইউজারনেম থাকবে;
                // পুরোনো অ্যাকাউন্টে না থাকলে নাম দিয়েই fallback করা হয়
                if (username != null && !username.isEmpty()) {
                    dbManager.saveUsername(username);
                } else if (dbManager.getUsername().isEmpty() && name != null) {
                    dbManager.saveUsername(name);
                }
                if (email != null) dbManager.saveUserEmail(email);
                if (phone != null) dbManager.savePhoneNumber(phone);

                // 🔥 Firestore-এ আগে থেকে save করা ডাটা (transactions/income/expense/ledger)
                // থাকলে সেটা এখানে auto-restore হবে।
                restoreDataAndGoToDashboard(name);
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                Toast.makeText(this, "❌ প্রোফাইল লোড করতে সমস্যা", Toast.LENGTH_SHORT).show();
            });
    }

    private void restoreDataAndGoToDashboard(String name) {
        firestoreSync.downloadAndRestore(new FirestoreSyncManager.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (message.contains("নতুন account")) {
                        Toast.makeText(EmailLoginActivity.this, "✅ স্বাগতম, " + name, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(EmailLoginActivity.this, "✅ ডাটা ফিরে এসেছে! স্বাগতম, " + name, Toast.LENGTH_LONG).show();
                    }
                    goToDashboard();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    // Restore ব্যর্থ হলেও login চালিয়ে যাও (local data থাকলে সেটাই দেখাবে)
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(EmailLoginActivity.this, "✅ স্বাগতম, " + name, Toast.LENGTH_SHORT).show();
                    goToDashboard();
                });
            }
        });
    }

    private void goToDashboard() {
        startActivity(new Intent(this, DashboardActivity.class)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    private String friendlyError(String msg) {
        if (msg == null) return "⚠️ অজানা সমস্যা হয়েছে";
        if (msg.contains("no user record") || msg.contains("There is no user") || msg.contains("user-not-found"))
            return "⚠️ এই Email/Phone দিয়ে কোনো অ্যাকাউন্ট নেই";
        if (msg.contains("password is invalid") || msg.contains("wrong-password") || msg.contains("incorrect") || msg.contains("INVALID_LOGIN_CREDENTIALS"))
            return "⚠️ Password ভুল হয়েছে";
        if (msg.contains("badly formatted") || msg.contains("INVALID_EMAIL"))
            return "⚠️ সঠিক Email/Phone দিন";
        if (msg.contains("network") || msg.contains("NETWORK_REQUEST_FAILED"))
            return "⚠️ ইন্টারনেট সংযোগ চেক করুন";
        if (msg.contains("too-many-requests"))
            return "⚠️ অনেকবার চেষ্টা হয়েছে, কিছুক্ষণ পর চেষ্টা করুন";
        return "⚠️ লগইন ব্যর্থ হয়েছে";
    }
}
