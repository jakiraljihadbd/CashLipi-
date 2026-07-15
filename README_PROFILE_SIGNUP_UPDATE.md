# CashLipi ক্যাশলিপি — Profile UI Upgrade + Sign Up (Phone/Gmail) আপডেট

## 🔴 নতুন বাগফিক্স (এই আপডেটে)

**সমস্যা:** ফোন নম্বর দিয়ে সাইন-আপ করলে Profile-এর "ইমেইল (Gmail)" বক্সে ভুলবশত ফোন নম্বরটাই দেখাচ্ছিল।

**আসল কারণ:** `EmailLoginActivity`-তে লগইনের সময় `saveGoogleAccount(name, email, null)`-এ ইমেইল না থাকলে ফোন নম্বর পাঠানো হতো — এটা ইচ্ছাকৃতভাবে করা হয়েছিল যাতে *পুরোনো ডাটা-restore ফিচার* (legacy phone-key দিয়ে Firestore backup খোঁজা) কাজ করে। তাই এই মানটা সরিয়ে ফেললে পুরোনো ব্যাকআপ restore ভেঙে যেতে পারত — তাই আমি সেটা স্পর্শ করিনি।

**সমাধান:** `ProfileActivity.java`-তে ইমেইল বক্স পূরণ করার সময় এখন শুধু তখনই এই fallback মান ব্যবহার হয় যখন সেটা আসলেই ইমেইলের মতো দেখতে (`@` আছে) — ফোন নম্বর হলে বক্সটা ফাঁকা থাকবে, ম্যানুয়ালি বসানো যাবে। ব্যাকআপ/restore লজিক অক্ষত রাখা হয়েছে।

**মোবাইল নম্বর বক্স:** Gmail দিয়ে সাইন-আপ করলে এটা এমনিতেই ফাঁকা থাকে (আগে থেকেই সঠিক ছিল, আলাদা কোনো ফিক্স লাগেনি) — টেস্ট করে দেখবেন।

## 🎨 Profile UI আরও friendly করা হয়েছে

- ছবি না থাকলে এখন জেনেরিক ধূসর আইকনের বদলে নামের আদ্যক্ষর (initials) দিয়ে একটা রঙিন গ্র্যাডিয়েন্ট avatar দেখাবে (যেমন "Md Rakib Hossain" → "MR") — বেশিরভাগ modern অ্যাপের মতো
- সেভ/পাসওয়ার্ড বাটনে আইকন যোগ হয়েছে (💾 প্রোফাইল সেভ করুন, 🔑 পাসওয়ার্ড পরিবর্তন করুন)
- সাইন-ইন বাটনেও আইকন (📱) যোগ হয়েছে

## ✅ আগের সেশনে যা করা হয়েছিল

1. **Profile স্ক্রিন — প্রফেশনাল লুক আপগ্রেড**
   - Header-এ subtle ব্র্যান্ডেড ডেকো সার্কেল ও আইকন ব্যাজ
   - সব কার্ড এখন `MaterialCardView` — হালকা বর্ডার (stroke) ও কম elevation দিয়ে ফ্ল্যাট, মডার্ন লুক
   - প্রতিটি সেকশনে রঙিন গোলাকার আইকন ব্যাজ + সাব-টাইটেল

2. **Sign Up স্ক্রিন — Login পেজের মতো Phone/Gmail টগল বাটন**
   - **ফোন মোড ফর্ম:** নাম → ইউজারনেম → মোবাইল নম্বর (country-code সহ) → Password → Confirm Password
   - **Gmail মোড ফর্ম:** নাম → ইউজারনেম → Gmail → Password → Confirm Password
   - ইউজারনেম Firestore-এ ইউনিক কিনা চেক করে

3. **Login বাগফিক্স** — সাইন-আপে দেওয়া আসল ইউজারনেম এখন সঠিকভাবে সেভ হয়

## 📁 পরিবর্তিত / নতুন ফাইল

```
app/src/main/res/layout/activity_profile.xml                          ← REPLACE
app/src/main/res/layout/activity_sign_up.xml                           ← REPLACE
app/src/main/java/com/jrappspot/cashlipi/activities/SignUpActivity.java       ← REPLACE
app/src/main/java/com/jrappspot/cashlipi/activities/EmailLoginActivity.java   ← REPLACE
app/src/main/java/com/jrappspot/cashlipi/activities/ProfileActivity.java      ← REPLACE
app/src/main/res/values/colors.xml                                     ← REPLACE
app/src/main/res/values-night/colors.xml                               ← REPLACE
app/src/main/res/drawable/shape_circle_solid.xml                       ← NEW
app/src/main/res/drawable/bg_profile_initials.xml                      ← NEW
```

## ℹ️ নোট

- Firestore security rules-এ `users` কালেকশনে খোলা `read` না থাকলে ইউজারনেম-uniqueness চেক ব্যর্থ হবে — সেক্ষেত্রে fallback করে সাইন-আপ চালিয়ে যাওয়া হয়, ব্লক করা হয় না।
- ইমেইল-ফিক্সটা শুধু **নতুন লগইনের পর** কার্যকর হবে (refreshUI যখন আবার চলবে) — যাদের আগে থেকেই ভুল মান লোকাল স্টোরেজে সেভ হয়ে আছে, তাদের একবার Profile খুললেই (refreshUI চলার সাথে সাথে) এখন সঠিকভাবে ফাঁকা দেখাবে, কারণ ফিক্সটা display-time-এ প্রয়োগ হয়, পুরোনো ডাটা মুছে ফেলার দরকার নেই।

