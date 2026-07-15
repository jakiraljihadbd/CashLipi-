package com.jrappspot.cashlipi.activities;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.jrappspot.cashlipi.R;

public class AboutActivity extends BaseActivity {

    private static final String DEVELOPER_SEARCH_URL =
            "https://www.google.com/search?q=Jakir+Al+Jihad";

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
    }
}
