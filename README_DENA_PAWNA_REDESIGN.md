# CashLipi — দেনা-পাওনা পেজ রিডিজাইন

## যা যোগ হলো

1. **উপরে অটো-স্লাইড ব্যানার** — যাদের টাকা এখনও অপরিশোধিত (LedgerEntry এন্ট্রি
   অনুযায়ী, নাম মিলিয়ে), তাদের প্রতি ৩ সেকেন্ড পরপর রঙিনভাবে দেখায়:
   - দেনা (আপনি দেবেন) → কমলা গ্র্যাডিয়েন্ট (`bg_type_active_dena`)
   - পাওনা (আপনি পাবেন) → নীল গ্র্যাডিয়েন্ট (`bg_type_active_pabona`)
   - সবচেয়ে বড় বকেয়াওয়ালা ব্যক্তি আগে দেখায়, নিচে ডট ইন্ডিকেটর।
   - কোনো অপরিশোধিত এন্ট্রি না থাকলে ব্যানারটাই লুকিয়ে যায়।

2. **সার্চ + ফিল্টার** — নাম/ফোন/সম্পর্ক দিয়ে সার্চ, এবং সব / অপরিশোধিত / পরিশোধিত
   চিপ (বিদ্যমান `bg_search_premium`, `bg_chip_selected/unselected` স্টাইল পুনর্ব্যবহার করা হয়েছে)।

3. **ব্যক্তি কার্ড রিডিজাইন** — ছবি/নাম/সম্পর্ক, ফোন, ছোট করে ঠিকানা, এবং নিচে দুটো
   ব্যাজ: "N টি লেনদেন" ও "M টি অপরিশোধিত" (বা "সব পরিশোধিত")।

4. **ব্যক্তি বিস্তারিত পেজ (PersonDetailActivity)** —
   - হেডার আরও কমপ্যাক্ট, ছবি বাম কিনারায়, নাম ছবির উপরের কিনারার সাথে মিলিয়ে
   - ডানে হোয়াটসঅ্যাপ / কল / মেইল বাটন, এডিট-মুছুন এখন ⋮ (তিন-ডট) মেনুতে

## নতুন ডেটা ফিল্ড

- `Person.email` (ঐচ্ছিক) — মেইল বাটনের জন্য যোগ করা হয়েছে, `activity_add_person.xml`-এ
  ইমেইল ইনপুট যোগ হয়েছে। পুরোনো ব্যক্তিদের জন্য এটা খালি থাকবে, তখন মেইল বাটন লুকানো থাকবে।

## গুরুত্বপূর্ণ স্থাপত্যগত নোট

`Person` ও `LedgerEntry` এখনও আলাদা মডেল — `LedgerEntry.person` একটা ফ্রি-টেক্সট নাম
ফিল্ড (কোনো person.id সংযোগ নেই)। তাই সব হিসাব নাম মিলিয়ে (case-insensitive) করা
হয়েছে (`DatabaseManager.getLedgerForPersonName()`, `PersonStat` মডেল)। ভবিষ্যতে
person.id দিয়ে সরাসরি সংযোগ করা হলে এই জোড়া-লাগানো লজিক সরিয়ে ফেলা যাবে।

## পরিবর্তিত/নতুন ফাইল

```
models/Person.java                          ← email ফিল্ড যোগ
models/PersonStat.java                       ← NEW
utils/DatabaseManager.java                   ← getLedgerForPersonName() যোগ
fragments/DenaPawnaFragment.java             ← REPLACE (ব্যানার/সার্চ/ফিল্টার লজিক)
adapters/PersonAdapter.java                  ← REPLACE (স্ট্যাটাস ব্যাজ)
activities/AddPersonActivity.java            ← REPLACE (email সংযোজন)
activities/PersonDetailActivity.java         ← REPLACE (whatsapp/mail/⋮ মেনু)
layout/fragment_dena_pawna.xml               ← REPLACE
layout/item_person.xml                       ← REPLACE
layout/item_debt_banner.xml                  ← NEW
layout/activity_person_detail.xml            ← REPLACE
layout/activity_add_person.xml               ← email ফিল্ড যোগ
drawable/ic_mail.xml                         ← NEW
drawable/ic_whatsapp.xml                     ← NEW
drawable/ic_more_vert.xml                    ← NEW
drawable/bg_action_circle_whatsapp.xml       ← NEW
drawable/bg_action_circle_call.xml           ← NEW
drawable/bg_action_circle_mail.xml           ← NEW
drawable/bg_badge_unpaid.xml                 ← NEW
drawable/bg_badge_paid.xml                   ← NEW
```

## টেস্ট করার আগে

- বিল্ড করে দেখুন কোনো unresolved resource/id নেই (সব XML/Java এখানে ভ্যালিডেট
  করা হয়েছে, কিন্তু পূর্ণ Gradle বিল্ড এই পরিবেশে সম্ভব হয়নি — Android Studio/AIDE-তে
  একবার Build চালিয়ে নিশ্চিত হবেন)।
- অন্তত একটা `Person` ও তার নামে ২-৩টা `LedgerEntry` (কিছু paid, কিছু unpaid) দিয়ে
  ব্যানার ও ব্যাজ ঠিকমতো দেখাচ্ছে কিনা যাচাই করুন।
