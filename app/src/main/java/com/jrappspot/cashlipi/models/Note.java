package com.jrappspot.cashlipi.models;

import com.google.gson.annotations.SerializedName;

public class Note {

    @SerializedName("id")
    private String id;

    @SerializedName("title")
    private String title;

    @SerializedName("content")
    private String content;

    @SerializedName("color")
    private String color;       // "yellow" | "blue" | "green" | "pink" | "purple"

    @SerializedName("pinned")
    private boolean pinned;

    @SerializedName("favorite")
    private boolean favorite;

    @SerializedName("date")
    private String date;

    @SerializedName("time")
    private String time;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    public Note() {}

    public Note(String title, String content, String color) {
        this.title = title;
        this.content = content;
        this.color = color;
    }

    // Getters
    public String getId() { return id != null ? id : ""; }
    public String getTitle() { return title != null ? title : ""; }
    public String getContent() { return content != null ? content : ""; }
    public String getColor() { return color != null ? color : "yellow"; }
    public boolean isPinned() { return pinned; }
    public boolean isFavorite() { return favorite; }
    public String getDate() { return date != null ? date : ""; }
    public String getTime() { return time != null ? time : ""; }
    public String getCreatedAt() { return createdAt != null ? createdAt : ""; }
    public String getUpdatedAt() { return updatedAt != null ? updatedAt : ""; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setColor(String color) { this.color = color; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
    public void setDate(String date) { this.date = date; }
    public void setTime(String time) { this.time = time; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
