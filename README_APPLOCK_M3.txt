App Lock — Material 3 রিডিজাইন (Hotfix)
==========================================

এই zip-এ শুধু নতুন/পরিবর্তিত ফাইলগুলো আছে — পুরো প্রজেক্ট না। AndroidIDE-তে
একই পাথে কপি করে replace করে দিন (folder structure zip-এর মতোই রাখা)।

কী পরিবর্তন হয়েছে
------------------
1. AppLockActivity — সম্পূর্ণ নতুন করে লেখা হয়েছে। এখন একটাই multi-step
   wizard: Intro → Create PIN → Confirm PIN → Security Question (ONE, optional)
   → Fingerprint (optional) → Success. Material 3 পার্পল/ইনডিগো থিম, আপনার
   পাঠানো ইমেজ দুটোর ডিজাইন অনুসরণ করে।

2. ResetPinActivity (নতুন ফাইল) — "Forgot PIN" চাপলে এখন এই নতুন স্ক্রিন
   খোলে: Verify Identity (শুধু ১টা সেভ করা প্রশ্ন) → Set New PIN → Confirm
   New PIN → Done। আগের মতো একসাথে ৩টা প্রশ্ন আর জিজ্ঞাসা করে না।

3. LockScreenActivity — "Forgot PIN" বাটনের লজিক এখন ResetPinActivity চালু
   করে, আগের 3-question AlertDialog সরিয়ে দেওয়া হয়েছে।

4. SecureLockStore + DatabaseManager — নতুন মেথড যোগ হয়েছে
   (saveSecurityQuestion, hasSecurityQuestion, getSecurityQuestionIndex,
   verifySecurityAnswer, clearSecurityQuestion) যেটা মাত্র ONE প্রশ্ন-উত্তর
   সেভ/ভেরিফাই করে। পুরনো ৫-প্রশ্নের মেথডগুলো এখনো আছে (কোথাও ব্যবহার হচ্ছে
   না, ভবিষ্যতে দরকার হলে থাকবে) — তাই পুরনো ডাটার সাথে সংঘর্ষ হবে না।

5. colors.xml — নিচে নতুন "applock*" নামের একটা সেকশন যোগ হয়েছে
   (Primary #6750A4, Secondary #7F67BE, Background #F7F6FC ইত্যাদি)। এই
   রঙগুলো বদলালে পুরো App Lock wizard-এর থিম বদলে যাবে, Java কোড ছুঁতে
   হবে না।

6. themes.xml — 3টা নতুন style: AppLockKeypadKey, AppLockQuestionRadio,
   AppLockDivider।

7. নতুন drawable রিসোর্স (bg_applock_*, shape_applock_*, ic_applock_*) —
   পুরনো কোনো drawable-এর নাম ওভাররাইট করা হয়নি, তাই বাকি অ্যাপে প্রভাব
   পড়বে না।

8. AndroidManifest.xml — ResetPinActivity রেজিস্টার করা হয়েছে।

গুরুত্বপূর্ণ নোট
-----------------
- PIN mandatory, Security Question ও Fingerprint দুটোই optional/skip করা যায়
  (spec অনুযায়ী)।
- "Never ask 3 questions" — রিসেটের সময় শুধু সেটআপে বাছাই করা ১টা প্রশ্নই
  জিজ্ঞাসা করা হয়।
- যদি কোনো ইউজার আগে থেকেই App Lock চালু করা থাকে (পুরনো ভার্সনে সেট করা),
  তাদের security question ডাটা নতুন সিস্টেমে মাইগ্রেট হয়নি (আলাদা key ব্যবহার
  করা হয়েছে) — তারা App Lock খুলে PIN আবার সেট করার সময় নতুন করে একটা প্রশ্ন
  বেছে নিলেই নতুন সিস্টেমে চলে আসবে।
