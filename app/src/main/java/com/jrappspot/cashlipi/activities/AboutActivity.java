package com.jrappspot.cashlipi.activities;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.jrappspot.cashlipi.R;

public class AboutActivity extends BaseActivity {

    private static final String DEVELOPER_SEARCH_URL =
            "https://www.google.com/search?q=Jakir+Al+Jihad";

    // ডেভেলপার যোগাযোগ নাম্বার (আন্তর্জাতিক ফরম্যাট, কোনো '+' বা '0' ছাড়া)
    private static final String DEVELOPER_PHONE_INTL = "8801737930168"; // WhatsApp/IMO জন্য
    private static final String DEVELOPER_PHONE_DIAL = "+8801737930168"; // ডায়ালার জন্য
    private static final String IMO_PACKAGE = "com.imo.android.imoim";

    @Override protected void onCreate(Bundle s){
        super.onCreate(s);
        setContentView(R.layout.activity_about);

        View.OnClickListener openDeveloperSearch = v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(DEVELOPER_SEARCH_URL)));
            } catch (Exception ignored) {}
        };

        View developerName = findViewById(R.id.developerNameText);
        if (developerName != null) developerName.setOnClickListener(openDeveloperSearch);

        View developerSearchRow = findViewById(R.id.developerSearchRow);
        if (developerSearchRow != null) developerSearchRow.setOnClickListener(openDeveloperSearch);

        // ── WhatsApp সরাসরি চ্যাট ──
        View btnWhatsapp = findViewById(R.id.btnDevWhatsapp);
        if (btnWhatsapp != null) btnWhatsapp.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://wa.me/" + DEVELOPER_PHONE_INTL));
                intent.setPackage("com.whatsapp");
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://wa.me/" + DEVELOPER_PHONE_INTL)));
                } catch (Exception ignored) {
                    Toast.makeText(this, "WhatsApp খুঁজে পাওয়া যায়নি", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // ── IMO — অ্যাপ থাকলে সরাসরি খুলবে, না থাকলে নাম্বার কপি করে Play Store দেখাবে ──
        View btnImo = findViewById(R.id.btnDevImo);
        if (btnImo != null) btnImo.setOnClickListener(v -> {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(IMO_PACKAGE);
            if (launchIntent != null) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Developer Number", DEVELOPER_PHONE_DIAL));
                Toast.makeText(this, "নাম্বার কপি হয়েছে — IMO-তে সার্চ করে চ্যাট করুন", Toast.LENGTH_LONG).show();
                startActivity(launchIntent);
            } else {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=" + IMO_PACKAGE)));
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=" + IMO_PACKAGE)));
                }
            }
        });

        // ── সরাসরি কল (ডায়ালার ওপেন হবে, নাম্বার প্রি-ফিল করা থাকবে) ──
        View btnCall = findViewById(R.id.btnDevCall);
        if (btnCall != null) btnCall.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + DEVELOPER_PHONE_DIAL)));
            } catch (Exception ignored) {
                Toast.makeText(this, "ডায়ালার খোলা যায়নি", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
