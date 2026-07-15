package com.jrappspot.cashlipi.models;

import com.google.gson.annotations.SerializedName;

public class TrashItem {

    @SerializedName("_trashId")
    private String trashId;

    @SerializedName("_trashKey")
    private String trashKey;        // "income" | "expense" | "ledger" | "savings" | "notes"

    @SerializedName("_trashedAt")
    private String trashedAt;

    // Common fields (merged from all types)
    @SerializedName("id")
    private String id;

    @SerializedName("amount")
    private double amount;

    @SerializedName("source")
    private String source;

    @SerializedName("category")
    private String category;

    @SerializedName("person")
    private String person;

    @SerializedName("method")
    private String method;

    @SerializedName("type")
    private String type;

    @SerializedName("date")
    private String date;

    @SerializedName("time")
    private String time;

    @SerializedName("note")
    private String note;

    @SerializedName("title")
    private String title;

    @SerializedName("content")
    private String content;

    @SerializedName("paid")
    private boolean paid;

    public TrashItem() {}

    public String getDisplayTitle() {
        if (source != null && !source.isEmpty()) return source;
        if (category != null && !category.isEmpty()) return category;
        if (person != null && !person.isEmpty()) return person;
        if (title != null && !title.isEmpty()) return title;
        if (method != null && !method.isEmpty()) return method;
        return "এন্ট্রি";
    }

    public String getTypeIcon() {
        if ("income".equals(trashKey)) return "";
        if ("expense".equals(trashKey)) return "";
        if ("ledger".equals(trashKey)) return "";
        if ("savings".equals(trashKey)) return "";
        if ("notes".equals(trashKey)) return "";
        return "";
    }

    public String getTypeLabel() {
        if ("income".equals(trashKey)) return "আয়";
        if ("expense".equals(trashKey)) return "ব্যয়";
        if ("ledger".equals(trashKey)) return "দেনা/পাওনা";
        if ("savings".equals(trashKey)) return "সঞ্চয়";
        if ("notes".equals(trashKey)) return "নোট";
        return "অজানা";
    }

    // Getters
    public String getTrashId() { return trashId != null ? trashId : ""; }
    public String getTrashKey() { return trashKey != null ? trashKey : ""; }
    public String getTrashedAt() { return trashedAt != null ? trashedAt : ""; }
    public String getId() { return id != null ? id : ""; }
    public double getAmount() { return amount; }
    public String getSource() { return source != null ? source : ""; }
    public String getCategory() { return category != null ? category : ""; }
    public String getPerson() { return person != null ? person : ""; }
    public String getMethod() { return method != null ? method : ""; }
    public String getType() { return type != null ? type : ""; }
    public String getDate() { return date != null ? date : ""; }
    public String getTime() { return time != null ? time : ""; }
    public String getNote() { return note != null ? note : ""; }
    public String getTitle() { return title != null ? title : ""; }
    public String getContent() { return content != null ? content : ""; }
    public boolean isPaid() { return paid; }

    // Setters
    public void setTrashId(String trashId) { this.trashId = trashId; }
    public void setTrashKey(String trashKey) { this.trashKey = trashKey; }
    public void setTrashedAt(String trashedAt) { this.trashedAt = trashedAt; }
    public void setId(String id) { this.id = id; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setSource(String source) { this.source = source; }
    public void setCategory(String category) { this.category = category; }
    public void setPerson(String person) { this.person = person; }
    public void setMethod(String method) { this.method = method; }
    public void setType(String type) { this.type = type; }
    public void setDate(String date) { this.date = date; }
    public void setTime(String time) { this.time = time; }
    public void setNote(String note) { this.note = note; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setPaid(boolean paid) { this.paid = paid; }
}
