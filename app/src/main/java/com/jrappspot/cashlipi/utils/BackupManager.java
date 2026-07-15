package com.jrappspot.cashlipi.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;

import androidx.core.content.FileProvider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.jrappspot.cashlipi.models.BackupRecord;
import com.jrappspot.cashlipi.models.BackupSettings;
import com.jrappspot.cashlipi.models.TelegramConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * BackupManager — Premium Backup System for CashLipi ক্যাশলিপি.
 *
 * Handles:
 *  - JSON export/import
 *  - Backup history (stored in SharedPreferences)
 *  - Telegram config (bot token & chat id)
 *  - Backup settings (triggers, methods, frequency)
 *  - Local storage backup
 *  - File sharing via FileProvider
 */
public class BackupManager {

    // ── SharedPreferences Keys ─────────────────────────────────────────────
    private static final String PREF_NAME         = "cashlipi_backup_prefs";
    private static final String KEY_HISTORY       = "backup_history";
    private static final String KEY_TELEGRAM      = "telegram_config";
    private static final String KEY_SETTINGS      = "backup_settings";
    private static final String KEY_LAST_BACKUP   = "last_backup_record";

    // ── Backup types ──────────────────────────────────────────────────────
    public static final String TYPE_ALL        = "all";
    public static final String TYPE_INCOME     = "income";
    public static final String TYPE_EXPENSE    = "expense";
    public static final String TYPE_DEBT       = "debt";
    public static final String TYPE_RECEIVABLE = "receivable";

    // ── Backup formats ────────────────────────────────────────────────────
    public static final String FORMAT_JSON = "json";
    public static final String FORMAT_PDF  = "pdf";
    public static final String FORMAT_DOCX = "docx";
    public static final String FORMAT_XLSX = "xlsx";

    // ── Backup methods ────────────────────────────────────────────────────
    public static final String METHOD_LOCAL        = "local";
    public static final String METHOD_TELEGRAM     = "telegram";
    public static final String METHOD_GOOGLE_DRIVE = "google_drive";

    // ── Status ────────────────────────────────────────────────────────────
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_FAILED  = "failed";
    public static final String STATUS_PENDING = "pending";

    private static BackupManager instance;
    private final Context context;
    private final SharedPreferences prefs;
    private final DatabaseManager db;
    private final Gson gson;

    private BackupManager(Context ctx) {
        this.context = ctx.getApplicationContext();
        this.prefs   = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.db      = DatabaseManager.getInstance(context);
        this.gson    = new GsonBuilder().create();
    }

    public static synchronized BackupManager getInstance(Context ctx) {
        if (instance == null) instance = new BackupManager(ctx);
        return instance;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  BACKUP SETTINGS
    // ═══════════════════════════════════════════════════════════════════════

    public BackupSettings getSettings() {
        String json = prefs.getString(KEY_SETTINGS, null);
        if (json == null) return new BackupSettings();
        try {
            BackupSettings s = gson.fromJson(json, BackupSettings.class);
            return s != null ? s : new BackupSettings();
        } catch (Exception e) {
            return new BackupSettings();
        }
    }

    public void saveSettings(BackupSettings settings) {
        prefs.edit().putString(KEY_SETTINGS, gson.toJson(settings)).apply();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TELEGRAM CONFIG
    // ═══════════════════════════════════════════════════════════════════════

    public TelegramConfig getTelegramConfig() {
        String json = prefs.getString(KEY_TELEGRAM, null);
        if (json == null) return new TelegramConfig();
        try {
            TelegramConfig cfg = gson.fromJson(json, TelegramConfig.class);
            return cfg != null ? cfg : new TelegramConfig();
        } catch (Exception e) {
            return new TelegramConfig();
        }
    }

    public void saveTelegramConfig(TelegramConfig config) {
        prefs.edit().putString(KEY_TELEGRAM, gson.toJson(config)).apply();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  BACKUP HISTORY
    // ═══════════════════════════════════════════════════════════════════════

    public List<BackupRecord> getBackupHistory() {
        String json = prefs.getString(KEY_HISTORY, null);
        if (json == null) return new ArrayList<>();
        try {
            Type t = new TypeToken<List<BackupRecord>>() {}.getType();
            List<BackupRecord> list = gson.fromJson(json, t);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void addToHistory(BackupRecord record) {
        List<BackupRecord> history = getBackupHistory();
        history.add(0, record); // newest first
        // Keep max 50 records
        if (history.size() > 50) history = history.subList(0, 50);
        prefs.edit().putString(KEY_HISTORY, gson.toJson(history)).apply();

        // Also save as last backup
        if (STATUS_SUCCESS.equals(record.getStatus())) {
            prefs.edit().putString(KEY_LAST_BACKUP, gson.toJson(record)).apply();
        }
    }

    public BackupRecord getLastBackupRecord() {
        String json = prefs.getString(KEY_LAST_BACKUP, null);
        if (json == null) return null;
        try {
            return gson.fromJson(json, BackupRecord.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  JSON BACKUP GENERATION
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Generates JSON backup string for the given data type.
     * @param dataType TYPE_ALL | TYPE_INCOME | TYPE_EXPENSE | etc.
     * @return JSON string or null on failure
     */
    public String generateJsonBackup(String dataType) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"appName\":\"CashLipi ক্যাশলিপি\",");
            sb.append("\"version\":\"1.0\",");
            sb.append("\"dataType\":\"").append(dataType).append("\",");
            sb.append("\"exportedAt\":\"").append(DatabaseManager.nowIso()).append("\",");

            String incomeJson  = prefs_db().getString("income",  "[]");
            String expenseJson = prefs_db().getString("expense", "[]");
            String ledgerJson  = prefs_db().getString("ledger",  "[]");
            String savingsJson = prefs_db().getString("savings", "[]");
            String notesJson   = prefs_db().getString("notes",   "[]");

            if (TYPE_ALL.equals(dataType)) {
                sb.append("\"income\":").append(incomeJson).append(",");
                sb.append("\"expense\":").append(expenseJson).append(",");
                sb.append("\"ledger\":").append(ledgerJson).append(",");
                sb.append("\"savings\":").append(savingsJson).append(",");
                sb.append("\"notes\":").append(notesJson);
            } else if (TYPE_INCOME.equals(dataType)) {
                sb.append("\"income\":").append(incomeJson);
            } else if (TYPE_EXPENSE.equals(dataType)) {
                sb.append("\"expense\":").append(expenseJson);
            } else if (TYPE_DEBT.equals(dataType)) {
                // debt = ledger entries with type "dena"
                sb.append("\"ledger\":").append(ledgerJson);
            } else if (TYPE_RECEIVABLE.equals(dataType)) {
                sb.append("\"ledger\":").append(ledgerJson);
            } else {
                sb.append("\"income\":").append(incomeJson).append(",");
                sb.append("\"expense\":").append(expenseJson).append(",");
                sb.append("\"ledger\":").append(ledgerJson);
            }

            sb.append("}");
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    // Access DatabaseManager's internal prefs for raw JSON
    private SharedPreferences prefs_db() {
        return context.getSharedPreferences("cashlipi_account_db", Context.MODE_PRIVATE);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  LOCAL STORAGE — SAVE TO CACHE
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Saves JSON content to app cache folder.
     * @return File object or null on failure
     */
    public File saveJsonToCache(String jsonContent, String fileName) {
        try {
            File dir = new File(context.getCacheDir(), "CashLipi_BACKUPS");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, fileName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(jsonContent.getBytes("UTF-8"));
            }
            return file;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Saves JSON content to external storage (Downloads/CashLipi/).
     * @return File object or null on failure
     */
    public File saveJsonToExternalStorage(String jsonContent, String fileName) {
        try {
            File dir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "CashLipi"
            );
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, fileName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(jsonContent.getBytes("UTF-8"));
            }
            return file;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns FileProvider URI for a cached file.
     */
    public Uri getFileUri(File file) {
        try {
            return FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".provider",
                file
            );
        } catch (Exception e) {
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TELEGRAM SHARING
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Creates a share Intent for Telegram.
     * Tries Telegram first, falls back to generic chooser.
     */
    public Intent createTelegramShareIntent(Uri fileUri, String caption) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_STREAM, fileUri);
        intent.putExtra(Intent.EXTRA_TEXT, caption);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Check if Telegram is installed
        intent.setPackage("org.telegram.messenger");
        if (context.getPackageManager().resolveActivity(intent, 0) != null) {
            return intent;
        }

        // Try Telegram X
        intent.setPackage("org.thunderdog.challegram");
        if (context.getPackageManager().resolveActivity(intent, 0) != null) {
            return intent;
        }

        // Fallback — generic chooser
        intent.setPackage(null);
        return Intent.createChooser(intent, "ব্যাকআপ শেয়ার করুন");
    }

    /**
     * Tests Telegram Bot API connection via network call.
     * Call from background thread (AsyncTask / Thread).
     * Returns true if connection successful.
     */
    public boolean testTelegramConnection(String botToken, String chatId) {
        try {
            String url = "https://api.telegram.org/bot" + botToken + "/getMe";
            java.net.URL u = new java.net.URL(url);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) u.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            conn.disconnect();
            return code == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Sends a JSON file to Telegram using Bot API.
     * Call from background thread.
     * @return true on success
     */
    public boolean sendFileToTelegram(String botToken, String chatId,
                                      String jsonContent, String fileName) {
        try {
            String boundary = "==CASHLIPI_BACKUP_BOUNDARY==";
            String url = "https://api.telegram.org/bot" + botToken + "/sendDocument";

            byte[] fileBytes = jsonContent.getBytes("UTF-8");

            java.net.URL u = new java.net.URL(url);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) u.openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            java.io.OutputStream os = conn.getOutputStream();

            // chat_id field
            String chatIdPart = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n"
                + chatId + "\r\n";
            os.write(chatIdPart.getBytes("UTF-8"));

            // caption field
            String captionPart = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"caption\"\r\n\r\n"
                + " CashLipi ক্যাশলিপি Backup — " + fileName + "\r\n";
            os.write(captionPart.getBytes("UTF-8"));

            // document field
            String docHeader = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"document\"; filename=\"" + fileName + "\"\r\n"
                + "Content-Type: application/json\r\n\r\n";
            os.write(docHeader.getBytes("UTF-8"));
            os.write(fileBytes);
            os.write(("\r\n--" + boundary + "--\r\n").getBytes("UTF-8"));
            os.flush();
            os.close();

            int code = conn.getResponseCode();
            conn.disconnect();
            return code == 200;

        } catch (Exception e) {
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  BACKUP RECORD BUILDER
    // ═══════════════════════════════════════════════════════════════════════

    /** Creates a new BackupRecord from current DB state */
    public BackupRecord buildRecord(String dataType, String format,
                                    String method, String status,
                                    String fileName, long fileSize) {
        BackupRecord r = new BackupRecord();
        r.setId(DatabaseManager.generateId());
        r.setDate(DatabaseManager.nowDate());
        r.setTime(new SimpleDateFormat("HH:mm", Locale.US).format(new Date()));
        r.setCreatedAt(DatabaseManager.nowIso());
        r.setDataType(dataType);
        r.setFormat(format);
        r.setMethod(method);
        r.setStatus(status);
        r.setFileName(fileName);
        r.setFileSize(fileSize);

        // Populate counts from DB
        r.setIncomeCount(db.getIncomeList().size());
        r.setExpenseCount(db.getExpenseList().size());
        r.setDebtCount((int) db.getLedgerList().stream()
            .filter(e -> "dena".equals(e.getType())).count());
        r.setReceivableCount((int) db.getLedgerList().stream()
            .filter(e -> "pabona".equals(e.getType())).count());
        r.setTotalItems(r.getIncomeCount() + r.getExpenseCount()
            + r.getDebtCount() + r.getReceivableCount());

        // Populate amounts
        r.setIncomeAmount(db.getTotalIncome());
        r.setExpenseAmount(db.getTotalExpense());
        r.setDebtAmount(db.getTotalDena());
        r.setReceivableAmount(db.getTotalPabona());

        return r;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  RESTORE FROM JSON
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Reads a backup JSON file from URI and returns content string.
     */
    public String readJsonFromUri(Uri uri) {
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) return null;
            byte[] buf = new byte[is.available()];
            is.read(buf);
            return new String(buf, "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parses backup JSON to preview data counts.
     * Returns array: [incomeCount, expenseCount, debtCount, receivableCount]
     */
    public int[] previewBackupCounts(String json) {
        try {
            com.google.gson.JsonObject obj = gson.fromJson(json, com.google.gson.JsonObject.class);
            int income = obj.has("income")
                ? obj.getAsJsonArray("income").size() : 0;
            int expense = obj.has("expense")
                ? obj.getAsJsonArray("expense").size() : 0;
            int debtTotal = obj.has("ledger")
                ? obj.getAsJsonArray("ledger").size() : 0;
            return new int[]{income, expense, debtTotal / 2, debtTotal / 2};
        } catch (Exception e) {
            return new int[]{0, 0, 0, 0};
        }
    }

    /**
     * Parses backup JSON to preview amounts.
     * Returns array: [totalIncome, totalExpense]
     */
    public double[] previewBackupAmounts(String json) {
        try {
            com.google.gson.JsonObject obj = gson.fromJson(json, com.google.gson.JsonObject.class);
            double incomeTotal = 0, expenseTotal = 0;

            if (obj.has("income")) {
                com.google.gson.JsonArray arr = obj.getAsJsonArray("income");
                for (int i = 0; i < arr.size(); i++) {
                    try {
                        incomeTotal += arr.get(i).getAsJsonObject()
                            .get("amount").getAsDouble();
                    } catch (Exception ignored) {}
                }
            }
            if (obj.has("expense")) {
                com.google.gson.JsonArray arr = obj.getAsJsonArray("expense");
                for (int i = 0; i < arr.size(); i++) {
                    try {
                        expenseTotal += arr.get(i).getAsJsonObject()
                            .get("amount").getAsDouble();
                    } catch (Exception ignored) {}
                }
            }

            return new double[]{incomeTotal, expenseTotal};
        } catch (Exception e) {
            return new double[]{0, 0};
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  UTILITY
    // ═══════════════════════════════════════════════════════════════════════

    /** Generates a timestamped backup filename */
    public String generateFileName(String format) {
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return "CashLipi_" + ts + "." + format;
    }

    /** Formats date+time for display: "16 Jun 2026 • 08:45 PM" */
    public static String formatDisplayDateTime(String date, String time) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date);
            String dateFmt = new SimpleDateFormat("dd MMM yyyy", Locale.US).format(d);
            // Convert time to 12h
            String[] parts = time.split(":");
            int h = Integer.parseInt(parts[0]);
            String m = parts.length > 1 ? parts[1] : "00";
            String ap = h >= 12 ? "PM" : "AM";
            h = h % 12; if (h == 0) h = 12;
            return dateFmt + " • " + h + ":" + m + " " + ap;
        } catch (Exception e) {
            return date + " • " + time;
        }
    }

    /** Gets backup count by method from history */
    public int getBackupCountByMethod(String method) {
        int count = 0;
        for (BackupRecord r : getBackupHistory()) {
            if (method.equals(r.getMethod()) && STATUS_SUCCESS.equals(r.getStatus()))
                count++;
        }
        return count;
    }

    /** Gets total backups */
    public int getTotalBackupCount() {
        return (int) getBackupHistory().stream()
            .filter(r -> STATUS_SUCCESS.equals(r.getStatus()))
            .count();
    }

    /**
     *  INSTANT AUTO SYNC — Income/Expense/Ledger/Savings এ কোনো পরিবর্তন হলে
     * এই মেথড কল করতে হবে। Google Drive auto-sync চালু থাকলে সাথে সাথেই
     * ব্যাকগ্রাউন্ডে Drive-এ সিঙ্ক হয়ে যাবে — ইউজারকে কিছু করতে হবে না।
     */
    public void triggerAutoGoogleDriveSync() {
        if (!db.isDriveAutoSyncEnabled()) return;
        if (!db.isGoogleSignedIn()) return;

        BackupSettings settings = getSettings();
        if (!settings.isGoogleDriveEnabled()) return;

        String json = generateJsonBackup(TYPE_ALL);
        com.jrappspot.cashlipi.utils.GoogleDriveSyncManager syncManager =
            new com.jrappspot.cashlipi.utils.GoogleDriveSyncManager(context);

        syncManager.syncNow(json, new com.jrappspot.cashlipi.utils.GoogleDriveSyncManager.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                android.util.Log.d("AutoSync", " Instant Google Drive sync সফল");
            }
            @Override
            public void onFailure(String error) {
                android.util.Log.e("AutoSync", " Instant Google Drive sync ব্যর্থ: " + error);
            }
        });
    }
}
