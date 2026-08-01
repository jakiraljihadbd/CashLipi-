package com.jrappspot.cashlipi.models;

import com.google.gson.annotations.SerializedName;

/** ইনভয়েসের একটা লাইন-আইটেম — পণ্যের নাম, পরিমাণ, দর। */
public class InvoiceItem {

    @SerializedName("productId")
    private String productId; // Product থেকে বেছে নিলে সেট হয়, ফাঁকা থাকলে ফ্রি-টেক্সট আইটেম

    @SerializedName("name")
    private String name;

    @SerializedName("qty")
    private double qty;

    @SerializedName("rate")
    private double rate;

    public InvoiceItem() {}

    public InvoiceItem(String name, double qty, double rate) {
        this.name = name;
        this.qty = qty;
        this.rate = rate;
    }

    public double getAmount() { return qty * rate; }

    public String getProductId() { return productId != null ? productId : ""; }
    public String getName() { return name != null ? name : ""; }
    public double getQty() { return qty; }
    public double getRate() { return rate; }

    public void setProductId(String productId) { this.productId = productId; }
    public void setName(String name) { this.name = name; }
    public void setQty(double qty) { this.qty = qty; }
    public void setRate(double rate) { this.rate = rate; }
}
