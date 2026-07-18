package com.jrappspot.cashlipi.models;

import com.google.gson.annotations.SerializedName;

/**
 * দেনা-পাওনা পেজের একটি ব্যক্তি বা প্রতিষ্ঠান।
 * এই মুহূর্তে শুধু পরিচিতি (নাম, ছবি, সম্পর্ক, ফোন, ঠিকানা) সংরক্ষিত হয়।
 * প্রতিটা ব্যক্তির লেনদেন (দিলাম/পেলাম) ভবিষ্যতে এই id-কে key করে যুক্ত হবে।
 */
public class Person {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("relation")
    private String relation;      // যেমন: ভাই, বন্ধু, দোকানদার — ঐচ্ছিক

    @SerializedName("phone")
    private String phone;         // ঐচ্ছিক

    @SerializedName("address")
    private String address;       // ঐচ্ছিক

    @SerializedName("email")
    private String email;         // ঐচ্ছিক — মেইল বাটনের জন্য ব্যবহৃত হয়

    @SerializedName("photoPath")
    private String photoPath;     // internal storage-এ ক্রপ করা ছবির path — ঐচ্ছিক

    @SerializedName("date")
    private String date;          // যোগ করার তারিখ (yyyy-MM-dd)

    @SerializedName("time")
    private String time;          // যোগ করার সময় (HH:mm)

    @SerializedName("favorite")
    private boolean favorite;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    public Person() {}

    public Person(String name, String relation, String phone) {
        this.name = name;
        this.relation = relation;
        this.phone = phone;
    }

    public boolean hasPhoto() { return photoPath != null && !photoPath.isEmpty(); }
    public boolean hasPhone() { return phone != null && !phone.trim().isEmpty(); }
    public boolean hasAddress() { return address != null && !address.trim().isEmpty(); }
    public boolean hasRelation() { return relation != null && !relation.trim().isEmpty(); }
    public boolean hasEmail() { return email != null && !email.trim().isEmpty(); }

    /** তালিকায় বৃত্তাকার ছবির বদলে দেখানোর জন্য নামের প্রথম অক্ষর। */
    public String getInitial() {
        String n = getName().trim();
        return n.isEmpty() ? "?" : n.substring(0, 1).toUpperCase();
    }

    // Getters
    public String getId() { return id != null ? id : ""; }
    public String getName() { return name != null ? name : ""; }
    public String getRelation() { return relation != null ? relation : ""; }
    public String getPhone() { return phone != null ? phone : ""; }
    public String getAddress() { return address != null ? address : ""; }
    public String getEmail() { return email != null ? email : ""; }
    public String getPhotoPath() { return photoPath != null ? photoPath : ""; }
    public String getDate() { return date != null ? date : ""; }
    public String getTime() { return time != null ? time : ""; }
    public boolean isFavorite() { return favorite; }
    public String getCreatedAt() { return createdAt != null ? createdAt : ""; }
    public String getUpdatedAt() { return updatedAt != null ? updatedAt : ""; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setRelation(String relation) { this.relation = relation; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setAddress(String address) { this.address = address; }
    public void setEmail(String email) { this.email = email; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
    public void setDate(String date) { this.date = date; }
    public void setTime(String time) { this.time = time; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
