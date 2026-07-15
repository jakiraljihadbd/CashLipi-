package com.jrappspot.cashlipi.activities;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.jrappspot.cashlipi.R;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etResetEmail;
    private Button btnSendResetEmail;
    private ProgressBar progressBar;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        auth = FirebaseAuth.getInstance();

        etResetEmail = findViewById(R.id.etResetEmail);
        btnSendResetEmail = findViewById(R.id.btnSendResetEmail);
        progressBar = findViewById(R.id.progressBar);

        btnSendResetEmail.setOnClickListener(v -> sendResetEmail());
    }

    // ── Email reset (📧 ১০০% ফ্রি — Firebase Auth এর নিজস্ব secure reset link) ──
    private void sendResetEmail() {
        String email = etResetEmail.getText().toString().trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etResetEmail.setError("সঠিক Email দিন");
            return;
        }

        progressBar.setVisibility(android.view.View.VISIBLE);
        btnSendResetEmail.setEnabled(false);

        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener(v -> {
                progressBar.setVisibility(android.view.View.GONE);
                Toast.makeText(this, "✅ Reset link পাঠানো হয়েছে — Gmail চেক করুন", Toast.LENGTH_LONG).show();
                finish();
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(android.view.View.GONE);
                btnSendResetEmail.setEnabled(true);
                Toast.makeText(this, "❌ " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }
}
