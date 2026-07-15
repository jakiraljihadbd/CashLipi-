# 📦 CashLipi ক্যাশলিপি — Premium Backup Module
## সম্পূর্ণ Integration Guide

---

## 📁 ফাইল কাঠামো

```
java/com/jrappspot/cashlipi/
├── models/
│   ├── BackupRecord.java       ← ব্যাকআপ রেকর্ড মডেল
│   ├── TelegramConfig.java     ← Telegram সেটিংস মডেল
│   └── BackupSettings.java     ← সমস্ত ব্যাকআপ সেটিংস
├── utils/
│   └── BackupManager.java      ← Core ব্যাকআপ সিস্টেম
├── adapters/
│   └── BackupHistoryAdapter.java ← History RecyclerView Adapter
└── activities/
    ├── BackupCenterActivity.java    ← মূল ব্যাকআপ হাব
    ├── CreateBackupActivity.java    ← ব্যাকআপ তৈরি করুন
    ├── TelegramBackupActivity.java  ← Telegram সেটআপ
    ├── GoogleDriveBackupActivity.java ← Google Drive সেটআপ
    ├── BackupHistoryActivity.java   ← ইতিহাস দেখুন
    ├── RestoreBackupActivity.java   ← রিস্টোর করুন
    ├── RestoreConfirmActivity.java  ← রিস্টোর নিশ্চিত করুন
    ├── BackupSettingsActivity.java  ← সেটিংস পরিচালনা
    └── BackupDetailsActivity.java   ← ব্যাকআপ বিস্তারিত

res/layout/
├── activity_backup_center.xml
├── activity_create_backup.xml
├── activity_telegram_backup.xml
├── activity_google_drive_backup.xml
├── activity_backup_history.xml
├── activity_restore_backup.xml
├── activity_restore_confirm.xml
├── activity_backup_settings.xml
├── activity_backup_details.xml
└── item_backup_history.xml

res/drawable/
└── (35+ drawable XML files for gradients, buttons, badges)

res/xml/
└── file_provider_paths.xml
```

---

## 🔧 Step 1: ফাইল কপি করুন

```bash
# সব Java ফাইল কপি করুন
cp -r models/ <your_project>/app/src/main/java/com/jrappspot/cashlipi/
cp -r utils/BackupManager.java <your_project>/app/src/main/java/com/jrappspot/cashlipi/utils/
cp -r adapters/ <your_project>/app/src/main/java/com/jrappspot/cashlipi/
cp -r activities/Backup*.java <your_project>/app/src/main/java/com/jrappspot/cashlipi/activities/
cp -r activities/Restore*.java <your_project>/app/src/main/java/com/jrappspot/cashlipi/activities/
cp -r activities/Telegram*.java <your_project>/app/src/main/java/com/jrappspot/cashlipi/activities/
cp -r activities/GoogleDrive*.java <your_project>/app/src/main/java/com/jrappspot/cashlipi/activities/

# Layout ফাইল কপি করুন
cp -r res/layout/activity_backup*.xml <your_project>/app/src/main/res/layout/
cp -r res/layout/activity_restore*.xml <your_project>/app/src/main/res/layout/
cp -r res/layout/activity_telegram*.xml <your_project>/app/src/main/res/layout/
cp -r res/layout/activity_google*.xml <your_project>/app/src/main/res/layout/
cp -r res/layout/item_backup_history.xml <your_project>/app/src/main/res/layout/

# Drawable ফাইল কপি করুন
cp res/drawable/*.xml <your_project>/app/src/main/res/drawable/

# XML ফাইল কপি করুন
cp res/xml/file_provider_paths.xml <your_project>/app/src/main/res/xml/
```

---

## 🔧 Step 2: AndroidManifest.xml আপডেট করুন

`AndroidManifest_backup_entries.xml` থেকে সব `<activity>` এবং `<provider>` entries আপনার `AndroidManifest.xml`-এর `<application>` ট্যাগের ভেতরে যোগ করুন।

**⚠️ গুরুত্বপূর্ণ:** যদি ইতোমধ্যে FileProvider থাকে, duplicate করবেন না।

---

## 🔧 Step 3: build.gradle আপডেট করুন

`build_gradle_dependencies.txt` থেকে নতুন dependencies যোগ করুন:

```gradle
// Gson — সবচেয়ে গুরুত্বপূর্ণ
implementation 'com.google.code.gson:gson:2.10.1'

// WorkManager (optional)
implementation 'androidx.work:work-runtime:2.9.0'
```

---

## 🔧 Step 4: DatabaseManager-এ method যোগ করুন

আপনার `DatabaseManager.java`-তে এই methods থাকা দরকার:

```java
// ইতোমধ্যে থাকলে skip করুন
public List<IncomeEntry> getIncomeList() { ... }
public List<ExpenseEntry> getExpenseList() { ... }
public List<LedgerEntry> getLedgerList() { ... }
public List<SavingsEntry> getSavingsList() { ... }
public List<NoteEntry> getNotesList() { ... }
public double getTotalIncome() { ... }
public double getTotalExpense() { ... }
public double getTotalDena() { ... }
public double getTotalPabona() { ... }
public boolean isGoogleSignedIn() { ... }
public String getGoogleAccountEmail() { ... }
public boolean importFromJson(String json) { ... }
public String exportToJson() { ... }

// Static helpers (যোগ করুন যদি না থাকে)
public static String generateId() {
    return java.util.UUID.randomUUID().toString();
}
public static String nowIso() {
    return new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
        java.util.Locale.US).format(new java.util.Date());
}
public static String nowDate() {
    return new java.text.SimpleDateFormat("yyyy-MM-dd",
        java.util.Locale.US).format(new java.util.Date());
}
public static String formatAmount(double amount) {
    if (amount == (long) amount)
        return String.format("%,d", (long) amount);
    return String.format("%,.2f", amount);
}
```

---

## 🔧 Step 5: Backup সেকশনে Navigation যোগ করুন

Dashboard বা Bottom Navigation থেকে Backup Center খুলুন:

```java
// DashboardActivity.java বা যেখানে Backup বোতাম আছে
btnBackup.setOnClickListener(v ->
    startActivity(new Intent(this, BackupCenterActivity.class)));
```

---

## 🔧 Step 6: Auto Backup যোগ করুন (Optional)

যেখানে income/expense যোগ হয়, সেখানে এটি যোগ করুন:

```java
// যেকোনো data save করার পর
BackupSettings settings = BackupManager.getInstance(this).getSettings();
if (settings.isAutoBackupEnabled() && settings.isTriggerOnIncomeAdded()) {
    triggerAutoBackup("income");
}

// Helper method
private void triggerAutoBackup(String trigger) {
    BackupManager bm = BackupManager.getInstance(this);
    String json = bm.generateJsonBackup(BackupManager.TYPE_ALL);
    if (json == null) return;

    String fileName = bm.generateFileName("json");

    // Save locally
    if (bm.getSettings().isLocalStorageEnabled()) {
        File f = bm.saveJsonToCache(json, fileName);
        if (f != null) {
            BackupRecord r = bm.buildRecord(BackupManager.TYPE_ALL,
                BackupManager.FORMAT_JSON, BackupManager.METHOD_LOCAL,
                BackupManager.STATUS_SUCCESS, fileName, f.length());
            bm.addToHistory(r);
        }
    }

    // Send to Telegram (background)
    if (bm.getSettings().isTelegramEnabled()) {
        TelegramConfig cfg = bm.getTelegramConfig();
        if (cfg.isValid()) {
            new Thread(() -> bm.sendFileToTelegram(
                cfg.getBotToken(), cfg.getChatId(), json, fileName
            )).start();
        }
    }
}
```

---

## 🎨 UI Features

| Screen | Feature |
|--------|---------|
| Backup Center | Latest backup, quick actions, methods, summary |
| Create Backup | Data type selector, format picker, method selector |
| Telegram Setup | Bot token, chat ID, test connection, auto backup |
| Google Drive | Sign-in, auto sync, upload, restore |
| Backup History | Filter by method, click for details |
| Restore | Source selector, file preview, confirmation dialog |
| Backup Settings | Triggers, methods, frequency (realtime/daily/weekly/monthly) |
| Backup Details | Full info, share, restore, delete |

---

## 🚀 Telegram Bot Setup

1. Telegram-এ `@BotFather` খুঁজুন
2. `/newbot` কমান্ড দিন
3. Bot নাম এবং username দিন
4. Bot Token কপি করুন (e.g. `1234567890:ABCDefGhIJK...`)
5. Chat ID পেতে `@userinfobot`-এ `/start` পাঠান
6. App-এ Telegram Backup screen-এ এন্টার করুন
7. "Test Connection" চাপুন

---

## ✅ সব ঠিক থাকলে

- ✅ Backup Center-এ সব section দেখা যাবে
- ✅ Create Backup → JSON ফাইল তৈরি হবে
- ✅ Telegram-এ ফাইল পাঠানো যাবে
- ✅ Local storage-এ JSON সেভ হবে
- ✅ Backup History-তে সব record দেখা যাবে
- ✅ Restore করলে data ফিরে আসবে
- ✅ Settings-এ সব toggle কাজ করবে

---

_CashLipi ক্যাশলিপি — Premium Finance Management_
_Backup Module v1.0 — Built with ❤️_
