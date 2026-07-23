package com.jrappspot.cashlipi.models;

/**
 * একজন গ্রাহকের নামে থাকা সব বাকির খাতা এন্ট্রি (KhataEntry, customerId মিলিয়ে) থেকে
 * হিসাব করা সারসংক্ষেপ — বাকির খাতা পেজের কার্ড ও উপরের ব্যানারে দেখানোর জন্য।
 * এটা শুধু UI-এর জন্য একটা গণনা করা স্ন্যাপশট, আলাদাভাবে সংরক্ষিত হয় না।
 */
public class KhataCustomerStat {

    public int totalCount;        // মোট লেনদেন সংখ্যা
    public int unpaidCount;       // (রিজার্ভড — ভবিষ্যতে কিস্তি-ভিত্তিক পরিশোধের জন্য)
    public double totalBaki;      // মোট বাকি দেওয়া (customer এর কাছে পাওনা তৈরি হওয়া টাকা)
    public double totalJoma;      // মোট জমা নেওয়া (customer এর পরিশোধ)

    public boolean hasAnyTxn() { return totalCount > 0; }

    /** নেট বকেয়া — ধনাত্মক মানে গ্রাহক বাকি আছেন (আপনি পাবেন), ঋণাত্মক মানে গ্রাহক অগ্রিম/জমা বেশি দিয়েছেন। */
    public double getNetOutstanding() { return totalBaki - totalJoma; }

    /** কার্ড/ব্যাজে দেখানোর জন্য — গ্রাহক বাকি আছেন কিনা। */
    public boolean isDue() { return getNetOutstanding() > 0.004; }

    public boolean isAdvance() { return getNetOutstanding() < -0.004; }

    public boolean isSettled() { return !isDue() && !isAdvance(); }

    public double getNetAmount() { return Math.abs(getNetOutstanding()); }
}
