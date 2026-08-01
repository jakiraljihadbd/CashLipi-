package com.jrappspot.cashlipi.models;

import com.google.gson.annotations.SerializedName;

/**
 * স্টক/ইনভেন্টরির একটা পণ্য — বর্তমান দোকানের (shop-scoped) নিজস্ব পণ্য তালিকার একটা আইটেম।
 * ক্রয়মূল্য, বিক্রয়মূল্য ও বর্তমান মজুদ রাখা হয়, যা থেকে লাভ ও কম-স্টক অ্যালার্ট হিসাব হয়।
 */
public class Product {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("photoPath")
    private String photoPath;

    @SerializedName("barcode")
    private String barcode;

    @SerializedName("category")
    private String category;

    @SerializedName("buyPrice")
    private double buyPrice;

    @SerializedName("sellPrice")
    private double sellPrice;

    @SerializedName("stockQty")
    private double stockQty;

    @SerializedName("lowStockAlert")
    private double lowStockAlert; // এই সংখ্যার নিচে নামলে "কম স্টক" হিসেবে গণ্য হবে

    @SerializedName("unit")
    private String unit; // যেমন: পিস, কেজি, লিটার — ঐচ্ছিক

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    public Product() {}

    public boolean hasPhoto() { return photoPath != null && !photoPath.isEmpty(); }
    public boolean isLowStock() { return stockQty <= lowStockAlert; }
    public double getProfitPerUnit() { return sellPrice - buyPrice; }
    public double getTotalBuyValue() { return buyPrice * stockQty; }
    public double getTotalSellValue() { return sellPrice * stockQty; }
    public double getTotalProfit() { return getProfitPerUnit() * stockQty; }

    public String getInitial() {
        String n = getName().trim();
        return n.isEmpty() ? "?" : n.substring(0, 1).toUpperCase();
    }

    public String getId() { return id != null ? id : ""; }
    public String getName() { return name != null ? name : ""; }
    public String getPhotoPath() { return photoPath != null ? photoPath : ""; }
    public String getBarcode() { return barcode != null ? barcode : ""; }
    public String getCategory() { return category != null ? category : ""; }
    public double getBuyPrice() { return buyPrice; }
    public double getSellPrice() { return sellPrice; }
    public double getStockQty() { return stockQty; }
    public double getLowStockAlert() { return lowStockAlert; }
    public String getUnit() { return unit != null && !unit.isEmpty() ? unit : "পিস"; }
    public String getCreatedAt() { return createdAt != null ? createdAt : ""; }
    public String getUpdatedAt() { return updatedAt != null ? updatedAt : ""; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public void setCategory(String category) { this.category = category; }
    public void setBuyPrice(double buyPrice) { this.buyPrice = buyPrice; }
    public void setSellPrice(double sellPrice) { this.sellPrice = sellPrice; }
    public void setStockQty(double stockQty) { this.stockQty = stockQty; }
    public void setLowStockAlert(double lowStockAlert) { this.lowStockAlert = lowStockAlert; }
    public void setUnit(String unit) { this.unit = unit; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
