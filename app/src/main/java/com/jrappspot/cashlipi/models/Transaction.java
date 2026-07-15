package com.jrappspot.cashlipi.models;

import com.google.gson.annotations.SerializedName;

public class Transaction {

    @SerializedName("id")
    private String id;

    @SerializedName("amount")
    private double amount;

    @SerializedName("source")
    private String source;          // income source

    @SerializedName("category")
    private String category;        // expense category

    @SerializedName("method")
    private String method;          // savings method

    @SerializedName("bankName")
    private String bankName;

    @SerializedName("accountNumber")
    private String accountNumber;

    @SerializedName("sourceType")
    private String sourceType;      // savings: "balance" or "direct"

    @SerializedName("date")
    private String date;

    @SerializedName("time")
    private String time;

    @SerializedName("note")
    private String note;

    @SerializedName("type")
    private String type;            // "income" | "expense" | "savings"

    @SerializedName("favorite")
    private boolean favorite;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    public Transaction() {}

    public Transaction(String type, double amount, String date, String time) {
        this.type = type;
        this.amount = amount;
        this.date = date;
        this.time = time;
    }

    // Getters
    public String getId() { return id; }
    public double getAmount() { return amount; }
    public String getSource() { return source != null ? source : ""; }
    public String getCategory() { return category != null ? category : ""; }
    public String getMethod() { return method != null ? method : ""; }
    public String getBankName() { return bankName != null ? bankName : ""; }
    public String getAccountNumber() { return accountNumber != null ? accountNumber : ""; }
    public String getSourceType() { return sourceType != null ? sourceType : "balance"; }
    public String getDate() { return date != null ? date : ""; }
    public String getTime() { return time != null ? time : ""; }
    public String getNote() { return note != null ? note : ""; }
    public String getType() { return type != null ? type : ""; }
    public boolean isFavorite() { return favorite; }
    public String getCreatedAt() { return createdAt != null ? createdAt : ""; }
    public String getUpdatedAt() { return updatedAt != null ? updatedAt : ""; }

    // Display title: income → source, expense → category, savings → method
    public String getDisplayTitle() {
        if ("income".equals(type)) {
            return source != null && !source.isEmpty() ? source : "আয়";
        } else if ("expense".equals(type)) {
            return category != null && !category.isEmpty() ? category : "ব্যয়";
        } else if ("savings".equals(type)) {
            return getMethodDisplay();
        }
        return "";
    }

    public String getMethodDisplay() {
        if (method == null) return "সঞ্চয়";
        switch (method) {
            case "bank":   return " ব্যাংক" + (bankName != null && !bankName.isEmpty() ? " - " + bankName : "");
            case "bkash":  return " বিকাশ";
            case "nagad":  return " নগদ";
            case "rocket": return " রকেট";
            case "cash":   return " নগদ টাকা";
            default:       return " অন্যান্য";
        }
    }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setSource(String source) { this.source = source; }
    public void setCategory(String category) { this.category = category; }
    public void setMethod(String method) { this.method = method; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public void setDate(String date) { this.date = date; }
    public void setTime(String time) { this.time = time; }
    public void setNote(String note) { this.note = note; }
    public void setType(String type) { this.type = type; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
