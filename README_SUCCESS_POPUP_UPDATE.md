# সাকসেস পপআপ (কালার-কোডেড) — আপডেট গাইড

## যা যোগ করা হলো

আয়, ব্যয়, দেনা, পাওনা এবং সঞ্চয় — এই ৫টি ফর্মের যেকোনো একটি সেভ করার পর এখন একটা সুন্দর সাকসেস পপআপ দেখাবে (আগে শুধু ছোট্ট Toast মেসেজ দেখাতো)। প্রতিটা ক্যাটাগরির নিজস্ব থিম কালার অনুযায়ী পপআপের আইকন, টাইটেল ও প্রাইমারি বাটনের রঙ বদলে যায়:

| ক্যাটাগরি | কালার |
|---|---|
| আয় (Income) | সবুজ `#059669` |
| ব্যয় (Expense) | লাল `#DC2626` |
| দেনা (Dena) | কমলা `#F59E0B` |
| পাওনা (Pabona) | নীল `#3B82F6` |
| সঞ্চয় (Savings) | বেগুনি `#7C3AED` |

## পপআপে যা থাকছে
- উপরে কনফেটি ডেকোরেশনসহ রঙিন চেকমার্ক আইকন (ক্যাটাগরি অনুযায়ী কালার)
- বড় টাইটেল, যেমন "আয় যোগ সফল হয়েছে!" (কালার-কোডেড)
- ছোট বিবরণ + অ্যাপের নাম
- দুইটা বাটন:
  - **আবার যোগ করুন** (ক্যাটাগরির কালারে) — পপআপ বন্ধ হয়ে একই ফর্মে ফোকাস চলে যায় (ফর্ম আগেই খালি হয়ে যায়, তাই সাথে সাথে আরেকটা এন্ট্রি দেওয়া যায়)
  - **তালিকা দেখুন** (গাঢ় কালো/নেভি) — সংশ্লিষ্ট তালিকা স্ক্রিনে (IncomeListActivity / ExpenseListActivity / LedgerListActivity / SavingsListActivity) নিয়ে যায়
  - উপরে ডানে ছোট ✕ বাটন দিয়ে সরাসরি বন্ধ করা যায়

## নতুন/পরিবর্তিত ফাইল

**নতুন যোগ হয়েছে:**
- `app/src/main/java/com/jrappspot/cashlipi/utils/SuccessPopup.java` — মূল রিইউজেবল হেল্পার ক্লাস
- `app/src/main/res/layout/dialog_success_popup.xml` — পপআপের UI
- `app/src/main/res/drawable/bg_success_popup_card.xml` — সাদা রাউন্ড কার্ড ব্যাকগ্রাউন্ড
- `app/src/main/res/drawable/bg_circle_dynamic.xml` — আইকন সার্কেল (কালার প্রোগ্রামে সেট হয়)
- `app/src/main/res/drawable/bg_btn_dynamic_fill.xml` — প্রাইমারি বাটন (কালার প্রোগ্রামে সেট হয়)
- `app/src/main/res/drawable/bg_btn_dark_fill.xml` — সেকেন্ডারি (তালিকা দেখুন) বাটন
- `app/src/main/res/drawable/ic_checkmark_plain.xml` — চেকমার্ক আইকন
- `app/src/main/res/drawable/ic_confetti_piece.xml` — কনফেটি সাজসজ্জা

**পরিবর্তিত হয়েছে:**
- `app/src/main/res/values/colors.xml` — নতুন `denaLight` ও `pabonaLight` কালার যোগ হয়েছে
- `AddIncomeActivity.java`, `AddExpenseActivity.java`, `AddLedgerActivity.java`, `AddSavingsActivity.java` — Toast সরিয়ে `SuccessPopup.show(...)` বসানো হয়েছে

## পাসওয়ার্ড/কালার পরিবর্তন করতে চাইলে
`SuccessPopup.java` ফাইলে `getAccentColorRes()` মেথডে গিয়ে যেকোনো ক্যাটাগরির কালার বদলে দিতে পারবেন।

## যেভাবে বসাবেন
পুরনো প্রজেক্টের উপর এই ZIP এর `app/` ফোল্ডারটা replace করে দিন, তারপর Android Studio-তে Sync + Rebuild করলেই কাজ শেষ।
