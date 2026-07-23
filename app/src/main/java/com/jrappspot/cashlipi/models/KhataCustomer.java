package com.jrappspot.cashlipi.models;

import com.google.gson.annotations.SerializedName;

/**
 * বাকির খাতার একজন গ্রাহক/কাস্টমার।
 * পরিচিতি তথ্যের পাশাপাশি "পূর্বের জের" (opening balance) রাখা যায় — নতুন গ্রাহক
 * যোগ করার সময় যদি আগে থেকেই কিছু বাকি থাকে তাহলে সেটা এখানে সেট হয়।
 */
public class KhataCustomer {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("businessTag")
    private String businessTag;    // যেমন: দোকানদার, পাইকার, নিয়মিত কাস্টমার — ঐচ্ছিক

    @SerializedName("phone")
    private String phone;

    @SerializedName("address")
    private String address;

    @SerializedName("email")
    private String email;

    @SerializedName("photoPath")
    private String photoPath;

    @SerializedName("openingBalance")
    private double openingBalance; // পূর্বের জের — ধনাত্মক মানে গ্রাহক আগে থেকেই বাকি, ঋণাত্মক মানে অগ্রিম জমা

    @SerializedName("date")
    private String date;

    @SerializedName("time")
    private String time;

    @SerializedName("favorite")
    private boolean favorite;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    public KhataCustomer() {}

    public KhataCustomer(String name, String businessTag, String phone) {
        this.name = name;
        this.businessTag = businessTag;
        this.phone = phone;
    }

    public boolean hasPhoto() { return photoPath != null && !photoPath.isEmpty(); }
    public boolean hasPhone() { return phone != null && !phone.trim().isEmpty(); }
    public boolean hasAddress() { return address != null && !address.trim().isEmpty(); }
    public boolean hasBusinessTag() { return businessTag != null && !businessTag.trim().isEmpty(); }
    public boolean hasEmail() { return email != null && !email.trim().isEmpty(); }

    /** তালিকায় বৃত্তাকার ছবির বদলে দেখানোর জন্য নামের প্রথম অক্ষর। */
    public String getInitial() {
        String n = getName().trim();
        return n.isEmpty() ? "?" : n.substring(0, 1).toUpperCase();
    }

    // Getters
    public String getId() { return id != null ? id : ""; }
    public String getName() { return name != null ? name : ""; }
    public String getBusinessTag() { return businessTag != null ? businessTag : ""; }
    public String getPhone() { return phone != null ? phone : ""; }
    public String getAddress() { return address != null ? address : ""; }
    public String getEmail() { return email != null ? email : ""; }
    public String getPhotoPath() { return photoPath != null ? photoPath : ""; }
    public double getOpeningBalance() { return openingBalance; }
    public String getDate() { return date != null ? date : ""; }
    public String getTime() { return time != null ? time : ""; }
    public boolean isFavorite() { return favorite; }
    public String getCreatedAt() { return createdAt != null ? createdAt : ""; }
    public String getUpdatedAt() { return updatedAt != null ? updatedAt : ""; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setBusinessTag(String businessTag) { this.businessTag = businessTag; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setAddress(String address) { this.address = address; }
    public void setEmail(String email) { this.email = email; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
    public void setOpeningBalance(double openingBalance) { this.openingBalance = openingBalance; }
    public void setDate(String date) { this.date = date; }
    public void setTime(String time) { this.time = time; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
