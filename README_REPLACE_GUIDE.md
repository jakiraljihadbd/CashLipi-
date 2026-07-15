# CashLipi ক্যাশলিপি — Backup সম্পূর্ণ ফিক্স
## Path সহ Replace গাইড

---

## ✅ যা ফিক্স হয়েছে

| সমস্যা | সমাধান |
|--------|--------|
| PDF/Word/Excel কাজ করছিল না | ✅ এখন ৩টিই কাজ করে (PDF=PdfDocument, Word=HTML৭.doc, Excel=CSV) |
| Local backup হচ্ছিল না | ✅ Downloads/CashLipi/ এ Auto-save (MediaStore API for Android 10+) |
| Dark/Light কালার সমস্যা | ✅ সব @color/ resource ব্যবহার, light+dark এ ভালো দেখাবে |
| FileProvider missing | ✅ Manifest এ যোগ করা হয়েছে |
| Vector icons missing | ✅ ১৩টি নতুন icon তৈরি করা হয়েছে |

---

## 📁 ফাইল কপি করুন (এই path গুলোতে replace করুন)

```
app/src/main/AndroidManifest.xml                                          ← REPLACE
app/src/main/java/com/jrappspot/cashlipi/activities/BackupActivity.java          ← REPLACE
app/src/main/res/layout/activity_backup.xml                               ← REPLACE
app/src/main/res/xml/file_provider_paths.xml                              ← NEW (যদি না থাকে)
app/src/main/res/drawable/circle_telegram_bg.xml                          ← NEW
app/src/main/res/drawable/circle_gdrive_bg.xml                            ← NEW
app/src/main/res/drawable/circle_local_bg.xml                             ← NEW
app/src/main/res/drawable/ic_home.xml                                     ← NEW
app/src/main/res/drawable/ic_backup_cloud.xml                             ← NEW
app/src/main/res/drawable/ic_restore.xml                                  ← NEW
app/src/main/res/drawable/ic_telegram.xml                                 ← NEW
app/src/main/res/drawable/ic_history.xml                                  ← NEW
app/src/main/res/drawable/ic_settings.xml                                 ← NEW
app/src/main/res/drawable/ic_shield.xml                                   ← NEW
app/src/main/res/drawable/ic_cloud_upload.xml                             ← NEW
app/src/main/res/drawable/ic_folder.xml                                   ← NEW
app/src/main/res/drawable/ic_analysis.xml                                 ← NEW
app/src/main/res/drawable/ic_database.xml                                 ← NEW
app/src/main/res/drawable/ic_file_export.xml                              ← NEW
```

ZIP-এর ভেতরের ফোল্ডার স্ট্রাকচার অনুযায়ী কপি করলেই সরাসরি সঠিক জায়গায় বসে যাবে।

---

## ⚠️ গুরুত্বপূর্ণ — AndroidManifest.xml

আমি সম্পূর্ণ Manifest replace করেছি (FileProvider + permissions যোগ করে)। যদি আপনার project এর Manifest-এ অন্য কিছু (যেমন নতুন Activity) থাকে যা এই ভার্সনে নেই, তাহলে manually merge করুন — কিন্তু মূল project থেকেই এটা বানানো, তাই সবকিছু থাকা উচিত।

---

## 🚀 যেভাবে কাজ করে

### Local Backup (JSON/PDF/Word/Excel)
1. "তৈরি" ট্যাব → Format + Local সিলেক্ট করুন
2. "START BACKUP" চাপুন
3. ফাইল সরাসরি **Downloads/CashLipi/** ফোল্ডারে সেভ হবে
4. Android 10+ এ MediaStore API ব্যবহার হয় (permission লাগে না)
5. Android 9 ও নিচে সরাসরি ফাইল সিস্টেমে লেখা হয়

### PDF ব্যাকআপ
- Android এর built-in `PdfDocument` API ব্যবহার করে
- Summary + Income list থাকে রিপোর্টে

### Word (.doc) ব্যাকআপ
- HTML কন্টেন্ট তৈরি করে `.doc` extension দিয়ে সেভ হয়
- Microsoft Word/WPS এ খোলে এবং ফরম্যাটিং দেখায়

### Excel ব্যাকআপ
- CSV ফরম্যাটে (UTF-8 BOM সহ, যাতে বাংলা টেক্সট সঠিক দেখায়)
- Excel/Google Sheets/WPS এ খোলে

### Auto Backup
অন্য Activity থেকে call করুন:
```java
BackupActivity.triggerAutoBackup(this, "income");   // income যোগের পর
BackupActivity.triggerAutoBackup(this, "expense");  // expense যোগের পর
BackupActivity.triggerAutoBackup(this, "debt");     // ledger যোগের পর
BackupActivity.triggerAutoBackup(this, "update");   // edit এর পর
BackupActivity.triggerAutoBackup(this, "delete");   // delete এর পর
```
এটা স্বয়ংক্রিয়ভাবে Downloads/CashLipi/ এ JSON সেভ করবে এবং Telegram enabled থাকলে পাঠাবে।

---

## 🎨 কালার থিম

সব UI element `@color/colorPrimary`, `@color/cardBg`, `@color/textPrimary` ইত্যাদি resource ব্যবহার করে যা আপনার project এ আগে থেকেই আছে — তাই Dark/Light mode অনুযায়ী automatically ঠিক রঙ দেখাবে।

---

## 🧪 টেস্ট চেকলিস্ট

- [ ] তৈরি → JSON → Local → START BACKUP → Downloads/CashLipi/ চেক করুন
- [ ] তৈরি → PDF → Local → START BACKUP → PDF ফাইল খুলে দেখুন
- [ ] তৈরি → Word → Local → START BACKUP → .doc ফাইল Word এ খুলুন
- [ ] তৈরি → Excel → Local → START BACKUP → .csv ফাইল Excel এ খুলুন
- [ ] Telegram ট্যাব → Bot Token/Chat ID → Test Connection
- [ ] রিস্টোর → ফাইল ব্রাউজ → আগের JSON সিলেক্ট → Restore
- [ ] সেটিংস → Trigger toggles → Save
- [ ] ইতিহাস ট্যাবে ব্যাকআপ লিস্ট দেখুন
