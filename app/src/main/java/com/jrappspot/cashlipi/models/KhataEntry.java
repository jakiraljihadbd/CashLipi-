package com.jrappspot.cashlipi.models;

import com.google.gson.annotations.SerializedName;

/**
 * বাকির খাতার একটি এন্ট্রি — কোনো গ্রাহকের নামে "বাকি" (মাল/সেবা বাকিতে দেওয়া হলো, গ্রাহকের
 * দেনা বাড়লো) অথবা "জমা" (গ্রাহক টাকা পরিশোধ করলো, দেনা কমলো)।
 */
public class KhataEntry {

    @SerializedName("id")
    private String id;

    @SerializedName("type")
    private String type;              // "baki" | "joma"

    @SerializedName("customerId")
    private String customerId;

    @SerializedName("customerName")
    private String customerName;      // দ্রুত দেখানোর জন্য cache করা নাম

    @SerializedName("category")
    private String category;          // ঐচ্ছিক — যেমন: চাল, তেল, নগদ উত্তোলন ইত্যাদি

    @SerializedName("amount")
    private double amount;

    @SerializedName("date")
    private String date;

    @SerializedName("time")
    private String time;

    @SerializedName("note")
    private String note;

    @SerializedName("depositTo")
    private String depositTo;         // "joma" এন্ট্রির টাকা কোথায় জমা হবে: "wallet" | "balance" | "none"

    @SerializedName("favorite")
    private boolean favorite;

    @SerializedName("paid")
    private boolean paid;         // এন্ট্রিটা মীমাংসা/রিকনসাইল করা হয়েছে কিনা (ঐচ্ছিক, ডিফল্ট false)

    @SerializedName("paidDate")
    private String paidDate;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    public KhataEntry() {}

    public KhataEntry(String type, String customerId, String customerName, double amount, String date, String time) {
        this.type = type;
        this.customerId = customerId;
        this.customerName = customerName;
        this.amount = amount;
        this.date = date;
        this.time = time;
    }

    public boolean isBaki() { return "baki".equals(type); }
    public boolean isJoma() { return "joma".equals(type); }

    public String getTypeDisplay() {
        return isBaki() ? "বাকি" : "জমা";
    }

    /** ছয়টা লেনদেন ক্যাটাগরির কোড — গ্রাহক/সাপ্লায়ার দুই ধরনের পার্টির জন্যই ব্যবহৃত হয়। */
    public static final String CAT_SALE = "sale";         // বিক্রয় (গ্রাহক বাকি বাড়ে)
    public static final String CAT_PAYMENT = "payment";   // পেমেন্ট (গ্রাহকের কাছ থেকে জমা)
    public static final String CAT_RECEIVE = "receive";   // গ্রহণ (সাপ্লায়ার থেকে মাল নিলাম, আপনার বাকি বাড়ে)
    public static final String CAT_GIVE = "give";         // প্রদান (সাপ্লায়ারকে টাকা দিলাম, বাকি কমে)
    public static final String CAT_ADVANCE = "advance";   // অগ্রিম পরিশোধ
    public static final String CAT_BALANCE = "balance";   // জের (ওপেনিং/সমন্বয়)

    /** category কোড থেকে বাংলা লেবেল — এন্ট্রি যোগ/এডিট করার সময় ও তালিকায় দেখানোর জন্য। */
    public static String categoryLabel(String cat) {
        if (cat == null) return "";
        switch (cat) {
            case CAT_SALE: return "বিক্রয়";
            case CAT_PAYMENT: return "পেমেন্ট";
            case CAT_RECEIVE: return "গ্রহণ";
            case CAT_GIVE: return "প্রদান";
            case CAT_ADVANCE: return "অগ্রিম পরিশোধ";
            case CAT_BALANCE: return "জের";
            default: return cat;
        }
    }

    /** category অনুযায়ী এন্ট্রির দিক (বাকি বাড়বে নাকি জমা/কমবে) — নতুন এন্ট্রি তৈরির সময় type
     *  স্বয়ংক্রিয়ভাবে এখান থেকে বসানো হয়, বাকি/জমা-ভিত্তিক পুরনো লজিক (রং, ব্যালেন্স হিসাব)
     *  অপরিবর্তিত থাকে। */
    public static String typeForCategory(String cat) {
        if (CAT_SALE.equals(cat) || CAT_RECEIVE.equals(cat)) return "baki";
        return "joma"; // payment, give, advance, balance(ঋণাত্মক না হলে) সবই বাকি কমায়
    }

    public String getCategoryLabel() { return categoryLabel(getCategory()); }

    /** টেবিল/লেজার ভিউতে দেখানোর জন্য — বাকি হলে ধনাত্মক, জমা হলে ঋণাত্মক প্রভাব। */
    public double getSignedAmount() {
        return isBaki() ? amount : -amount;
    }

    // Getters
    public String getId() { return id != null ? id : ""; }
    public String getType() { return type != null ? type : "baki"; }
    public String getCustomerId() { return customerId != null ? customerId : ""; }
    public String getCustomerName() { return customerName != null ? customerName : ""; }
    public String getCategory() { return category != null ? category : ""; }
    public double getAmount() { return amount; }
    public String getDate() { return date != null ? date : ""; }
    public String getTime() { return time != null ? time : ""; }
    public String getNote() { return note != null ? note : ""; }
    public String getDepositTo() { return depositTo != null && !depositTo.isEmpty() ? depositTo : "wallet"; }
    public boolean isFavorite() { return favorite; }
    public boolean isPaid() { return paid; }
    public String getPaidDate() { return paidDate != null ? paidDate : ""; }
    public String getCreatedAt() { return createdAt != null ? createdAt : ""; }
    public String getUpdatedAt() { return updatedAt != null ? updatedAt : ""; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setType(String type) { this.type = type; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public void setCategory(String category) { this.category = category; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setDate(String date) { this.date = date; }
    public void setTime(String time) { this.time = time; }
    public void setNote(String note) { this.note = note; }
    public void setDepositTo(String depositTo) { this.depositTo = depositTo; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
    public void setPaid(boolean paid) { this.paid = paid; }
    public void setPaidDate(String paidDate) { this.paidDate = paidDate; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
