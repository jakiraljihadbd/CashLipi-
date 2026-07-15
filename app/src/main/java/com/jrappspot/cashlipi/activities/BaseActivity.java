package com.jrappspot.cashlipi.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.jrappspot.cashlipi.utils.FontUtils;
import com.jrappspot.cashlipi.utils.FirestoreSyncManager;

/**
 * BaseActivity — সব Activity এটাকে extend করে।
 *
 * onResume() এ FontUtils.applyToView() call করা হয়,
 * ফলে font change হলে পরের screen এ সাথে সাথে কার্যকর হয়।
 *
 * এখন onResume()/onPause() এ live online/offline presence আর
 * device info (নাম/id/IP) ও sync হয় — admin panel এ real-time দেখা যায়।
 */
public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onResume() {
        super.onResume();
        // Font change instantly apply — কোনো restart লাগবে না
        FontUtils.applyToView(this, findViewById(android.R.id.content));

        FirestoreSyncManager sync = FirestoreSyncManager.getInstance(this);
        sync.updateOnlineStatus(true);
        sync.saveDeviceInfoIfNeeded();
    }

    @Override
    protected void onPause() {
        super.onPause();
        FirestoreSyncManager.getInstance(this).updateOnlineStatus(false);
    }
}
