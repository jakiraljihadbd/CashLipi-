package com.jrappspot.cashlipi.models;

import com.google.gson.annotations.SerializedName;

/**
 * একটা দোকান/ব্যবসা প্রোফাইল — মাল্টি-শপ সিস্টেমের ভিত্তি। বাকির খাতা, স্টক, ইনভয়েস — এই
 * তিনটা মডিউলের সব ডেটা shopId দিয়ে স্কোপ করা থাকে, যাতে একই একাউন্টে একাধিক দোকানের হিসাব
 * আলাদা আলাদাভাবে রাখা যায়।
 */
public class Shop {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("branchTag")
    private String branchTag; // যেমন: "Main Branch", "Mirpur Branch" — ঐচ্ছিক

    @SerializedName("createdAt")
    private String createdAt;

    public Shop() {}

    public Shop(String name, String branchTag) {
        this.name = name;
        this.branchTag = branchTag;
    }

    public String getId() { return id != null ? id : ""; }
    public String getName() { return name != null ? name : ""; }
    public String getBranchTag() { return branchTag != null ? branchTag : ""; }
    public String getCreatedAt() { return createdAt != null ? createdAt : ""; }

    public String getDisplayName() {
        return getBranchTag().isEmpty() ? getName() : getName() + " (" + getBranchTag() + ")";
    }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setBranchTag(String branchTag) { this.branchTag = branchTag; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
