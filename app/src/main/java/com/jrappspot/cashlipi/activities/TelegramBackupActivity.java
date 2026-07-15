package com.jrappspot.cashlipi.activities;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


import androidx.cardview.widget.CardView;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.models.BackupSettings;
import com.jrappspot.cashlipi.models.TelegramConfig;
import com.jrappspot.cashlipi.utils.BackupManager;

/**
 * TelegramBackupActivity — Configure Telegram Bot for backup delivery.
 */
public class TelegramBackupActivity extends BaseActivity {

    private BackupManager backupManager;

    private EditText etBotToken, etChatId;
    private Switch switchEnabled, switchAutoBackup;
    private Spinner spinnerFormat;
    private TextView tvConnectionStatus;
    private CardView cardConnectionStatus;
    private ImageButton btnToggleToken;

    private boolean tokenVisible = false;

    private final String[] FORMATS = {"JSON", "PDF", "DOCX", "XLSX"};
    private final String[] FORMAT_KEYS = {"json", "pdf", "docx", "xlsx"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_telegram_backup);

        backupManager = BackupManager.getInstance(this);

        initViews();
        loadConfig();
        setupClickListeners();
        animateIn();
    }

    private void initViews() {
        etBotToken          = findViewById(R.id.etBotToken);
        etChatId            = findViewById(R.id.etChatId);
        switchEnabled       = findViewById(R.id.switchTelegramEnabled);
        switchAutoBackup    = findViewById(R.id.switchTelegramAutoBackup);
        spinnerFormat       = findViewById(R.id.spinnerTelegramFormat);
        tvConnectionStatus  = findViewById(R.id.tvConnectionStatus);
        cardConnectionStatus= findViewById(R.id.cardConnectionStatus);
        btnToggleToken      = findViewById(R.id.btnToggleBotToken);

        // Format spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, FORMATS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFormat.setAdapter(adapter);
    }

    private void loadConfig() {
        TelegramConfig cfg = backupManager.getTelegramConfig();
        BackupSettings settings = backupManager.getSettings();

        if (cfg.getBotToken() != null) etBotToken.setText(cfg.getBotToken());
        if (cfg.getChatId() != null) etChatId.setText(cfg.getChatId());

        switchEnabled.setChecked(cfg.isEnabled());
        switchAutoBackup.setChecked(cfg.isAutoBackup());

        // Set spinner to saved format
        String savedFormat = cfg.getPreferredFormat() != null ? cfg.getPreferredFormat() : "json";
        for (int i = 0; i < FORMAT_KEYS.length; i++) {
            if (FORMAT_KEYS[i].equals(savedFormat)) {
                spinnerFormat.setSelection(i);
                break;
            }
        }
    }

    private void setupClickListeners() {
        // Back

        // Show/hide bot token
        btnToggleToken.setOnClickListener(v -> {
            tokenVisible = !tokenVisible;
            if (tokenVisible) {
                etBotToken.setTransformationMethod(
                    HideReturnsTransformationMethod.getInstance());
                btnToggleToken.setImageResource(R.drawable.ic_eye_off);
            } else {
                etBotToken.setTransformationMethod(
                    PasswordTransformationMethod.getInstance());
                btnToggleToken.setImageResource(R.drawable.ic_eye);
            }
            // Keep cursor at end
            etBotToken.setSelection(etBotToken.getText().length());
        });

        // Test Connection
        findViewById(R.id.btnTestConnection).setOnClickListener(v -> testConnection());

        // Save Settings
        findViewById(R.id.btnSaveSettings).setOnClickListener(v -> saveSettings());

        // Format spinner selection
        spinnerFormat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                // handled at save time
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void testConnection() {
        String token  = etBotToken.getText().toString().trim();
        String chatId = etChatId.getText().toString().trim();

        if (token.isEmpty()) {
            showConnectionStatus(false, " Bot Token খালি!");
            return;
        }
        if (chatId.isEmpty()) {
            showConnectionStatus(false, " Chat ID খালি!");
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage(" সংযোগ পরীক্ষা হচ্ছে...");
        pd.setCancelable(false);
        pd.show();

        new AsyncTask<String, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(String... params) {
                return backupManager.testTelegramConnection(params[0], params[1]);
            }

            @Override
            protected void onPostExecute(Boolean success) {
                pd.dismiss();
                if (success) {
                    showConnectionStatus(true,
                        " সংযোগ সফল! Telegram Bot সঠিকভাবে কাজ করছে।");
                } else {
                    showConnectionStatus(false,
                        " সংযোগ ব্যর্থ। Bot Token সঠিক কিনা পরীক্ষা করুন।");
                }
            }
        }.execute(token, chatId);
    }

    private void showConnectionStatus(boolean success, String message) {
        if (cardConnectionStatus != null) cardConnectionStatus.setVisibility(View.VISIBLE);
        if (tvConnectionStatus != null) {
            tvConnectionStatus.setText(message);
            tvConnectionStatus.setTextColor(success ? ContextCompat.getColor(this, R.color.successColor)
                : ContextCompat.getColor(this, R.color.errorColor));
        }
    }

    private void saveSettings() {
        String token  = etBotToken.getText().toString().trim();
        String chatId = etChatId.getText().toString().trim();

        if (switchEnabled.isChecked() && (token.isEmpty() || chatId.isEmpty())) {
            Toast.makeText(this,
                " Telegram চালু করতে Bot Token ও Chat ID দিন",
                Toast.LENGTH_LONG).show();
            return;
        }

        // Build config
        TelegramConfig cfg = new TelegramConfig();
        cfg.setBotToken(token);
        cfg.setChatId(chatId);
        cfg.setEnabled(switchEnabled.isChecked());
        cfg.setAutoBackup(switchAutoBackup.isChecked());
        cfg.setPreferredFormat(FORMAT_KEYS[spinnerFormat.getSelectedItemPosition()]);

        backupManager.saveTelegramConfig(cfg);

        // Update global settings
        BackupSettings settings = backupManager.getSettings();
        settings.setTelegramEnabled(switchEnabled.isChecked());
        if (switchEnabled.isChecked() && switchAutoBackup.isChecked()) {
            settings.setAutoBackupEnabled(true);
        }
        backupManager.saveSettings(settings);

        Toast.makeText(this, " Telegram সেটিংস সেভ হয়েছে!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void animateIn() {
        int[] ids = {R.id.cardTelegramConfig, R.id.cardTelegramToggles,
            R.id.cardTelegramFormat, R.id.cardTelegramInfo};
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
}
