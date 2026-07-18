package com.jrappspot.cashlipi.models;

/**
 * একজন ব্যক্তির নামে থাকা সব দেনা-পাওনা এন্ট্রি (LedgerEntry, person নাম মিলিয়ে) থেকে
 * হিসাব করা সারসংক্ষেপ — দেনা-পাওনা পেজের কার্ড ও উপরের স্লাইড ব্যানারে দেখানোর জন্য।
 * এটা শুধু UI-এর জন্য একটা গণনা করা স্ন্যাপশট, আলাদাভাবে সংরক্ষিত হয় না।
 */
public class PersonStat {

    public int totalCount;        // মোট লেনদেন সংখ্যা
    public int unpaidCount;       // অপরিশোধিত এন্ট্রি সংখ্যা
    public double unpaidDena;     // অপরিশোধিত দেনা (আমি দেব)
    public double unpaidPabona;   // অপরিশোধিত পাওনা (আমি পাব)

    public boolean hasAnyTxn() { return totalCount > 0; }
    public boolean hasUnpaid() { return unpaidCount > 0; }
    public boolean isFullyPaid() { return totalCount > 0 && unpaidCount == 0; }

    /** নেট বকেয়া — ধনাত্মক মানে পাওনা বেশি (আমি পাব), ঋণাত্মক মানে দেনা বেশি (আমি দেব)। */
    public double getNetOutstanding() { return unpaidPabona - unpaidDena; }

    /** ব্যানার/ব্যাজে দেখানোর জন্য — কোন দিকটা বড়: "dena" বা "pabona"। */
    public boolean isNetDena() { return unpaidDena >= unpaidPabona; }

    public double getNetAmount() { return Math.abs(getNetOutstanding()); }
}
