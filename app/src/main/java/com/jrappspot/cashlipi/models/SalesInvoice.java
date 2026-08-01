package com.jrappspot.cashlipi.models;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * পণ্যের ক্রয়/বিক্রয়ের একটা ইনভয়েস — বর্তমান দোকানের (shop-scoped) নিজস্ব ইনভয়েস তালিকার
 * একটা আইটেম। ক্রেতা/বিক্রেতার নাম, লাইন-আইটেমের তালিকা ও মোট টাকা রাখা হয়।
 */
public class SalesInvoice {

    @SerializedName("id")
    private String id;

    @SerializedName("invoiceNo")
    private String invoiceNo;

    @SerializedName("customerId")
    private String customerId; // বাকির খাতার গ্রাহকের সাথে যুক্ত হলে সেট হয়, ঐচ্ছিক

    @SerializedName("customerName")
    private String customerName;

    @SerializedName("customerAddress")
    private String customerAddress;

    @SerializedName("items")
    private List<InvoiceItem> items;

    @SerializedName("note")
    private String note;

    @SerializedName("date")
    private String date;

    @SerializedName("time")
    private String time;

    @SerializedName("createdAt")
    private String createdAt;

    public SalesInvoice() {}

    public double getTotal() {
        double total = 0;
        for (InvoiceItem it : getItems()) total += it.getAmount();
        return total;
    }

    public String getId() { return id != null ? id : ""; }
    public String getInvoiceNo() { return invoiceNo != null ? invoiceNo : ""; }
    public String getCustomerId() { return customerId != null ? customerId : ""; }
    public String getCustomerName() { return customerName != null ? customerName : ""; }
    public String getCustomerAddress() { return customerAddress != null ? customerAddress : ""; }
    public List<InvoiceItem> getItems() { return items != null ? items : new ArrayList<>(); }
    public String getNote() { return note != null ? note : ""; }
    public String getDate() { return date != null ? date : ""; }
    public String getTime() { return time != null ? time : ""; }
    public String getCreatedAt() { return createdAt != null ? createdAt : ""; }

    public void setId(String id) { this.id = id; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public void setCustomerAddress(String customerAddress) { this.customerAddress = customerAddress; }
    public void setItems(List<InvoiceItem> items) { this.items = items; }
    public void setNote(String note) { this.note = note; }
    public void setDate(String date) { this.date = date; }
    public void setTime(String time) { this.time = time; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
