package com.jrappspot.cashlipi.models;

import com.google.gson.annotations.SerializedName;

public class LedgerEntry {

    @SerializedName("id")
    private String id;

    @SerializedName("type")
    private String type;        // "dena" | "pabona"

    @SerializedName("person")
    private String person;

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

    @SerializedName("paid")
    private boolean paid;

    @SerializedName("paidDate")
    private String paidDate;

    @SerializedName("settleTo")
    private String settleTo;    // পরিশোধ করার সময় কোথায় হিসাব হবে: "balance" | "savings" | "incomeExpense" | "none"

    @SerializedName("settleTxnId")
    private String settleTxnId; // "incomeExpense"-এ পরিশোধ হলে তৈরি হওয়া আসল আয়/ব্যয় Transaction-এর id

    @SerializedName("favorite")
    private boolean favorite;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    public LedgerEntry() {}

    public LedgerEntry(String type, String person, double amount, String date, String time) {
        this.type = type;
        this.person = person;
        this.amount = amount;
        this.date = date;
        this.time = time;
        this.paid = false;
    }

    public boolean isDena() { return "dena".equals(type); }
    public boolean isPabona() { return "pabona".equals(type); }

    public String getTypeDisplay() {
        return isDena() ? " দেনা" : " পাওনা";
    }

    // Getters
    public String getId() { return id != null ? id : ""; }
    public String getType() { return type != null ? type : "dena"; }
    public String getPerson() { return person != null ? person : ""; }
    public String getCategory() { return category != null ? category : ""; }
    public double getAmount() { return amount; }
    public String getDate() { return date != null ? date : ""; }
    public String getTime() { return time != null ? time : ""; }
    public String getNote() { return note != null ? note : ""; }
    public boolean isPaid() { return paid; }
    public String getPaidDate() { return paidDate != null ? paidDate : ""; }
    public String getSettleTo() { return settleTo != null && !settleTo.isEmpty() ? settleTo : "balance"; }
    public String getSettleTxnId() { return settleTxnId != null ? settleTxnId : ""; }
    public boolean isFavorite() { return favorite; }
    public String getCreatedAt() { return createdAt != null ? createdAt : ""; }
    public String getUpdatedAt() { return updatedAt != null ? updatedAt : ""; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setType(String type) { this.type = type; }
    public void setPerson(String person) { this.person = person; }
    public void setCategory(String category) { this.category = category; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setDate(String date) { this.date = date; }
    public void setTime(String time) { this.time = time; }
    public void setNote(String note) { this.note = note; }
    public void setPaid(boolean paid) { this.paid = paid; }
    public void setPaidDate(String paidDate) { this.paidDate = paidDate; }
    public void setSettleTo(String settleTo) { this.settleTo = settleTo; }
    public void setSettleTxnId(String settleTxnId) { this.settleTxnId = settleTxnId; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
