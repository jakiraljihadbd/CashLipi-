package com.jrappspot.cashlipi.models;

import com.google.gson.annotations.SerializedName;

/**
 * বিজনেস ওয়ালেটের একটা লেনদেন — মূল ব্যালেন্স থেকে জমা/উত্তোলন, গ্রাহকের জমা ওয়ালেটে
 * জমা হওয়া, বা খরচ ওয়ালেট থেকে কাটা হওয়া। শুধু হিস্ট্রি দেখানোর জন্য।
 */
public class KhataWalletTxn {

    @SerializedName("id")
    private String id;

    @SerializedName("type")
    private String type; // "fund_in" | "fund_out" | "joma_in" | "expense_out"

    @SerializedName("amount")
    private double amount;

    @SerializedName("note")
    private String note;

    @SerializedName("date")
    private String date;

    @SerializedName("time")
    private String time;

    @SerializedName("createdAt")
    private String createdAt;

    public KhataWalletTxn() {}

    public KhataWalletTxn(String type, double amount, String note) {
        this.type = type;
        this.amount = amount;
        this.note = note;
    }

    public boolean isCredit() { return "fund_in".equals(type) || "joma_in".equals(type); }

    public String getId() { return id != null ? id : ""; }
    public String getType() { return type != null ? type : ""; }
    public double getAmount() { return amount; }
    public String getNote() { return note != null ? note : ""; }
    public String getDate() { return date != null ? date : ""; }
    public String getTime() { return time != null ? time : ""; }
    public String getCreatedAt() { return createdAt != null ? createdAt : ""; }

    public String getTypeLabel() {
        switch (getType()) {
            case "fund_in": return "মূল ব্যালেন্স থেকে জমা";
            case "fund_out": return "মূল ব্যালেন্সে ফেরত";
            case "joma_in": return "গ্রাহকের জমা";
            case "expense_out": return "খরচ পরিশোধ";
            default: return "";
        }
    }

    public void setId(String id) { this.id = id; }
    public void setType(String type) { this.type = type; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setNote(String note) { this.note = note; }
    public void setDate(String date) { this.date = date; }
    public void setTime(String time) { this.time = time; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
