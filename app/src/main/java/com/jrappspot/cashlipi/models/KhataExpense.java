package com.jrappspot.cashlipi.models;

import com.google.gson.annotations.SerializedName;

/**
 * বাকির খাতার ব্যবসায়িক খরচ — দোকান ভাড়া, বিদ্যুৎ বিল, পরিবহন ইত্যাদি।
 * এটা মূল একাউন্টের আয়-ব্যয় (Transaction) থেকে সম্পূর্ণ আলাদা — শুধু বিজনেস ওয়ালেট থেকে
 * বিয়োগ হয় (payFromWallet সত্য হলে), নাহলে শুধু হিসাব-রাখার জন্য (বুককিপিং)।
 */
public class KhataExpense {

    @SerializedName("id")
    private String id;

    @SerializedName("category")
    private String category;

    @SerializedName("amount")
    private double amount;

    @SerializedName("date")
    private String date;

    @SerializedName("time")
    private String time;

    @SerializedName("note")
    private String note;

    @SerializedName("payFromWallet")
    private boolean payFromWallet; // সত্য হলে ওয়ালেট ব্যালেন্স থেকে টাকা কমে

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    public KhataExpense() {}

    public String getId() { return id != null ? id : ""; }
    public String getCategory() { return category != null ? category : "অন্যান্য"; }
    public double getAmount() { return amount; }
    public String getDate() { return date != null ? date : ""; }
    public String getTime() { return time != null ? time : ""; }
    public String getNote() { return note != null ? note : ""; }
    public boolean isPayFromWallet() { return payFromWallet; }
    public String getCreatedAt() { return createdAt != null ? createdAt : ""; }
    public String getUpdatedAt() { return updatedAt != null ? updatedAt : ""; }

    public void setId(String id) { this.id = id; }
    public void setCategory(String category) { this.category = category; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setDate(String date) { this.date = date; }
    public void setTime(String time) { this.time = time; }
    public void setNote(String note) { this.note = note; }
    public void setPayFromWallet(boolean payFromWallet) { this.payFromWallet = payFromWallet; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
