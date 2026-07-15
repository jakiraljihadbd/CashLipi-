# CashLipi ক্যাশলিপি — Login Popup Upgrade + Firestore Auto-Restore

## 🔴 আসল বাগ যেটা এবার ফিক্স হলো

আগের ভার্সনে backup data সেভ হতো phone/email দিয়ে বানানো একটা key দিয়ে
(`users/01300168630/backup/data`) — কিন্তু আপনি Email/Phone দিয়ে Login করলে
Firebase Auth একটা আলাদা random UID (যেমন `aXy93kP...`) তৈরি করে। বেশিরভাগ
Firestore security rule চেক করে `request.auth.uid == path-এর id` — তাই phone
number কে path এ ব্যবহার করায় rules-এর সাথে না মেলায় **write পুরোপুরি silently
ব্যর্থ হচ্ছিল** (কোনো error popup/log caller পর্যন্ত পৌঁছাচ্ছিল না, কারণ প্রতিটা
Add Income/Expense এ `uploadAllData(null)` কল হতো — কলব্যাক null মানে error
ধরাই পড়ছিল না)।

**এখন:** Login করা থাকলে (Email/Phone দিয়ে signInWithEmailAndPassword করা)
সরাসরি Firebase Auth-এর UID দিয়ে save/restore হয় — এটাই security rules-এর
সাথে মেলে। পুরোনো Google Sign-In flow (যেখানে real Firebase Auth session নেই)
আগের মতোই email-key ব্যবহার করবে। Restore করার সময় uid-key তে কিছু না পেলে
পুরোনো email-key ও একবার চেক করা হবে (fallback)।

## 🩺 Debug Toast যোগ করা হয়েছে

যেহেতু auto-save (`uploadAllData(null)`) কল হয় প্রতিটা লেনদেনের পরে, এবং সেটার
error আগে কোথাও দেখা যেত না — এখন সেভ ব্যর্থ হলে একটা ছোট Toast দেখাবে
("⚠️ Cloud sync ব্যর্থ: ...") যাতে আসল কারণ (যেমন `PERMISSION_DENIED`) সরাসরি
স্ক্রিনে দেখা যায়। টেস্ট করার সময় নতুন একটা Income/Expense যোগ করে দেখুন —
Toast আসলে সেই মেসেজ আমাকে পাঠাবেন, তাহলে rules-এর ঠিক কোন অংশ ব্লক করছে সেটা
নিশ্চিত হওয়া যাবে। সব ঠিকমতো কাজ করলে ভবিষ্যতে এই Toast টা চাইলে সরিয়ে দেওয়া
যাবে।

## ⚠️ Firestore Security Rules চেক করুন

`users/{uid}` এবং তার নিচের `backup`, `profile` sub-collection এ authenticated
user নিজের uid দিয়ে read/write করতে পারবে — rules এ এমন কিছু থাকা দরকার:

```
match /users/{userId} {
  allow read, write: if request.auth != null && request.auth.uid == userId;
  match /{sub=**} {
    allow read, write: if request.auth != null && request.auth.uid == userId;
  }
}
```

`checkAccountRegistered()` ফিচারটার জন্য (login popup সঠিক দেখানোর জন্য)
unauthenticated অবস্থায় `email`/`phone` field দিয়ে **query** করার অনুমতিও
লাগবে (existence check শুধু, পুরো ডাটা read না) — rules না থাকলে সেটা
automatically generic message এ fallback করবে, অ্যাপ ভাঙবে না।

## ✅ যা করা হয়েছে

| চাহিদা | অবস্থা |
|---|---|
| প্রতিটি লেনদেন নেট থাকলে live Firestore-এ save হবে | ✅ key mismatch bug ফিক্স করার পর এখন আসলেই save হবে |
| Login করলেই আগের ডাটা automatic restore হবে | ✅ uid-key দিয়ে restore, না পেলে legacy email-key ও চেক করে |
| পপআপ আরো সুন্দর/আপগ্রেড করা | ✅ কাস্টম ডিজাইনের পপআপ |
| Password ভুল দিলে popup | ✅ আলাদা popup |
| Firestore-এ অ্যাকাউন্ট না পেলে "User Not Registered" popup | ✅ আলাদা popup |

## 📁 নতুন/পরিবর্তিত ফাইল

```
app/src/main/java/com/jrappspot/cashlipi/activities/EmailLoginActivity.java      ← REPLACE
app/src/main/java/com/jrappspot/cashlipi/activities/SignUpActivity.java          ← REPLACE
app/src/main/java/com/jrappspot/cashlipi/utils/FirestoreSyncManager.java         ← REPLACE
app/src/main/java/com/jrappspot/cashlipi/utils/AuthDialogHelper.java             ← NEW
app/src/main/res/layout/dialog_auth_message.xml                          ← NEW
app/src/main/res/drawable/bg_dialog_card.xml                             ← NEW
app/src/main/res/drawable/bg_dialog_icon_circle.xml                      ← NEW
app/src/main/res/drawable/ic_dialog_warning.xml                          ← NEW
app/src/main/res/drawable/ic_dialog_lock.xml                             ← NEW
app/src/main/res/drawable/ic_dialog_person_off.xml                       ← NEW
app/src/main/res/drawable/ic_dialog_wifi_off.xml                         ← NEW
```

মোবাইলে ফাইল কপি করলে merge না হয়ে পুরোপুরি replace হচ্ছে কিনা অবশ্যই চেক করবেন।

## বোনাস ফিক্স

আগে প্রতিবার sync হওয়ার সময় admin panel এর `blocked` flag জোর করে `false`
করে দেওয়া হতো (`profileData.put("blocked", false)` প্রতি sync এ পাঠানো হতো) —
মানে Admin কাউকে ব্লক করলেও পরের যেকোনো transaction sync এ সেটা আবার আনব্লক
হয়ে যেত। এটাও ফিক্স করে দিয়েছি — এখন sync `blocked` field স্পর্শ করে না।

