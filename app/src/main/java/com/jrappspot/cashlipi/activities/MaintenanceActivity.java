package com.jrappspot.cashlipi.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.jrappspot.cashlipi.R;

public class MaintenanceActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable checkRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maintenance_user);
        startChecking();
    }

    private void startChecking() {
        checkRunnable = new Runnable() {
            @Override
            public void run() {
                FirebaseFirestore.getInstance()
                    .collection("admin").document("config").get()
                    .addOnSuccessListener(doc -> {
                        Boolean maintenance = doc.getBoolean("maintenance");
                        if (maintenance == null || !maintenance) {
                            // Maintenance শেষ — app এ যাও
                            startActivity(new Intent(MaintenanceActivity.this, SplashActivity.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                            finish();
                        } else {
                            // ৩০ সেকেন্ড পরে আবার check
                            handler.postDelayed(this, 30000);
                        }
                    })
                    .addOnFailureListener(e -> handler.postDelayed(this, 30000));
            }
        };
        handler.postDelayed(checkRunnable, 30000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (checkRunnable != null) handler.removeCallbacks(checkRunnable);
    }
}
