package com.jrappspot.cashlipi.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;



import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.models.BackupSettings;
import com.jrappspot.cashlipi.utils.BackupManager;
import com.jrappspot.cashlipi.utils.DatabaseManager;
import com.jrappspot.cashlipi.utils.GoogleDriveSyncManager;

/**
 * GoogleDriveBackupActivity — Google Drive backup configuration ও real auto-sync।
 */
public class GoogleDriveBackupActivity extends BaseActivity {

    private BackupManager backupManager;
    private DatabaseManager db;
    private GoogleDriveSyncManager driveSync;
    private Switch switchAutoSync;
    private TextView tvSignInStatus, tvDriveFolder, tvLastSyncTime;
    private ProgressBar progressSync;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_drive_backup);

        backupManager = BackupManager.getInstance(this);
        db = DatabaseManager.getInstance(this);
        driveSync = new GoogleDriveSyncManager(this);
        mainHandler = new Handler(Looper.getMainLooper());

        switchAutoSync    = findViewById(R.id.switchGDriveAutoSync);
        tvSignInStatus    = findViewById(R.id.tvGDriveSignInStatus);
        tvDriveFolder     = findViewById(R.id.tvDriveFolderPath);
        tvLastSyncTime    = findViewById(R.id.tvGDriveLastSync); // null হলেও ক্ষতি নেই
        progressSync      = findViewById(R.id.progressGDriveSync); // null হলেও ক্ষতি নেই

        setupViews();
        setupClickListeners();
        animateIn();
    }

    private void setupViews() {
        boolean signedIn = db.isGoogleSignedIn();

        if (tvSignInStatus != null) {
            if (signedIn) {
                String email = db.getGoogleEmail();
                tvSignInStatus.setText(" সংযুক্ত: " + (email != null ? email : "Google Account"));
                tvSignInStatus.setTextColor(0xFF10B981);
            } else {
                tvSignInStatus.setText(" Google Account সংযুক্ত নয়");
                tvSignInStatus.setTextColor(0xFFEF4444);
            }
        }

        if (tvDriveFolder != null) {
            tvDriveFolder.setText("Drive App Data → CashLipi_backup.json");
        }

        updateLastSyncLabel();

        // Auto sync toggle — সত্যিকারের Drive sync চালু/বন্ধ করে
        if (switchAutoSync != null) {
            BackupSettings s = backupManager.getSettings();
            switchAutoSync.setChecked(db.isDriveAutoSyncEnabled());
            switchAutoSync.setOnCheckedChangeListener((b, checked) -> {
                if (checked && !signedIn) {
                    Toast.makeText(this,
                        " আগে Google Sign-In করুন", Toast.LENGTH_SHORT).show();
                    b.setChecked(false);
                    return;
                }
                db.setDriveAutoSyncEnabled(checked);
                s.setGoogleDriveEnabled(checked);
                backupManager.saveSettings(s);

                if (checked) {
                    Toast.makeText(this, " অটো সিঙ্ক চালু — এখন সিঙ্ক করা হচ্ছে...", Toast.LENGTH_SHORT).show();
                    performSync();
                } else {
                    Toast.makeText(this, " অটো সিঙ্ক বন্ধ করা হয়েছে", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateLastSyncLabel() {
        if (tvLastSyncTime == null) return;
        long last = db.getLastDriveSyncTime();
        if (last == 0L) {
            tvLastSyncTime.setText("এখনো কোনো সিঙ্ক হয়নি");
        } else {
            String formatted = android.text.format.DateFormat.format("dd MMM yyyy, HH:mm", last).toString();
            tvLastSyncTime.setText("সর্বশেষ সিঙ্ক: " + formatted);
        }
    }

    private void setupClickListeners() {

        // Google Sign In → ProfileActivity-তে পাঠাই (সেখানেই sign-in হয়)
        View btnSignIn = findViewById(R.id.btnGoogleSignIn);
        if (btnSignIn != null) {
            btnSignIn.setOnClickListener(v -> {
                startActivity(new Intent(this, ProfileActivity.class));
            });
        }

        // Sign out
        View btnSignOut = findViewById(R.id.btnGoogleSignOut);
        if (btnSignOut != null) {
            btnSignOut.setOnClickListener(v -> {
                if (!db.isGoogleSignedIn()) {
                    Toast.makeText(this, "কোনো Google Account সংযুক্ত নেই", Toast.LENGTH_SHORT).show();
                    return;
                }
                new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Sign Out")
                    .setMessage("Google Account থেকে সাইন আউট করবেন?")
                    .setPositiveButton("হ্যাঁ", (d, w) -> {
                        db.clearGoogleAccount();
                        db.setDriveAutoSyncEnabled(false);
                        Toast.makeText(this, " সাইন আউট হয়েছে", Toast.LENGTH_SHORT).show();
                        setupViews();
                    })
                    .setNegativeButton("না", null)
                    .show();
            });
        }

        //  Manual upload — এখনই Drive-এ সত্যিকারের sync করে
        View btnUpload = findViewById(R.id.btnManualUpload);
        if (btnUpload != null) {
            btnUpload.setOnClickListener(v -> {
                if (!db.isGoogleSignedIn()) {
                    Toast.makeText(this, " আগে Google Sign-In করুন", Toast.LENGTH_SHORT).show();
                    return;
                }
                performSync();
            });
        }

        // Restore from Drive — সত্যিকারের ডেটা ডাউনলোড করে RestoreConfirmActivity-তে পাঠায়
        View btnRestoreFromDrive = findViewById(R.id.btnRestoreFromDrive);
        if (btnRestoreFromDrive != null) {
            btnRestoreFromDrive.setOnClickListener(v -> performRestoreFromDrive());
        }
    }

    /**  সত্যিকারের Drive sync — DatabaseManager-এর সব ডেটা JSON করে আপলোড করে */
    private void performSync() {
        showProgress(true);
        String json = backupManager.generateJsonBackup(BackupManager.TYPE_ALL);

        driveSync.syncNow(json, new GoogleDriveSyncManager.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                mainHandler.post(() -> {
                    showProgress(false);
                    Toast.makeText(GoogleDriveBackupActivity.this, message, Toast.LENGTH_SHORT).show();
                    updateLastSyncLabel();
                });
            }
            @Override
            public void onFailure(String error) {
                mainHandler.post(() -> {
                    showProgress(false);
                    Toast.makeText(GoogleDriveBackupActivity.this, " " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**  Drive থেকে backup ডাউনলোড করে Restore Confirm স্ক্রিনে পাঠায় */
    private void performRestoreFromDrive() {
        if (!db.isGoogleSignedIn()) {
            Toast.makeText(this, " আগে Google Sign-In করুন", Toast.LENGTH_SHORT).show();
            return;
        }
        showProgress(true);
        driveSync.downloadBackup(new GoogleDriveSyncManager.SyncCallback() {
            @Override
            public void onSuccess(String json) {
                mainHandler.post(() -> {
                    showProgress(false);
                    double[] amounts = backupManager.previewBackupAmounts(json);
                    Intent i = new Intent(GoogleDriveBackupActivity.this, RestoreConfirmActivity.class);
                    i.putExtra("backup_json", json);
                    i.putExtra("preview_income", amounts[0]);
                    i.putExtra("preview_expense", amounts[1]);
                    i.putExtra("source", "google_drive");
                    startActivity(i);
                });
            }
            @Override
            public void onFailure(String error) {
                mainHandler.post(() -> {
                    showProgress(false);
                    Toast.makeText(GoogleDriveBackupActivity.this, " " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showProgress(boolean show) {
        if (progressSync != null) progressSync.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void animateIn() {
        int[] ids = {R.id.cardGDriveAccount, R.id.cardGDriveOptions,
            R.id.cardGDrivePath, R.id.cardGDriveActions};
        long delay = 80;
        for (int id : ids) {
            View v = findViewById(id);
            if (v != null) {
                v.setAlpha(0f);
                v.setTranslationY(30f);
                v.animate().alpha(1f).translationY(0f)
                    .setStartDelay(delay).setDuration(300).start();
                delay += 80;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupViews();
    }
}
