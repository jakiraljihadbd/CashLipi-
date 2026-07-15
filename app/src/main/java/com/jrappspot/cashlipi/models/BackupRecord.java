package com.jrappspot.cashlipi.models;

import java.io.Serializable;

/**
 * BackupRecord — একটি ব্যাকআপের সম্পূর্ণ তথ্য রেকর্ড করে।
 * Stores a complete backup record with all metadata.
 */
public class BackupRecord implements Serializable {

    // Unique identifier
    private String id;

    // Date/Time
    private String date;        // yyyy-MM-dd
    private String time;        // HH:mm
    private String createdAt;   // ISO 8601

    // Backup metadata
    private String dataType;    // "all" | "income" | "expense" | "debt" | "receivable"
    private String format;      // "json" | "pdf" | "docx" | "xlsx"
    private String method;      // "local" | "telegram" | "google_drive"
    private String status;      // "success" | "failed" | "pending"
    private long fileSize;      // bytes

    // Counts at time of backup
    private int incomeCount;
    private int expenseCount;
    private int debtCount;
    private int receivableCount;
    private int totalItems;

    // Amounts at time of backup
    private double incomeAmount;
    private double expenseAmount;
    private double debtAmount;
    private double receivableAmount;

    // File info
    private String fileName;
    private String filePath;

    // Notes
    private String note;

    // ─── Getters & Setters ──────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public int getIncomeCount() { return incomeCount; }
    public void setIncomeCount(int incomeCount) { this.incomeCount = incomeCount; }

    public int getExpenseCount() { return expenseCount; }
    public void setExpenseCount(int expenseCount) { this.expenseCount = expenseCount; }

    public int getDebtCount() { return debtCount; }
    public void setDebtCount(int debtCount) { this.debtCount = debtCount; }

    public int getReceivableCount() { return receivableCount; }
    public void setReceivableCount(int receivableCount) { this.receivableCount = receivableCount; }

    public int getTotalItems() { return totalItems; }
    public void setTotalItems(int totalItems) { this.totalItems = totalItems; }

    public double getIncomeAmount() { return incomeAmount; }
    public void setIncomeAmount(double incomeAmount) { this.incomeAmount = incomeAmount; }

    public double getExpenseAmount() { return expenseAmount; }
    public void setExpenseAmount(double expenseAmount) { this.expenseAmount = expenseAmount; }

    public double getDebtAmount() { return debtAmount; }
    public void setDebtAmount(double debtAmount) { this.debtAmount = debtAmount; }

    public double getReceivableAmount() { return receivableAmount; }
    public void setReceivableAmount(double receivableAmount) { this.receivableAmount = receivableAmount; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    // ─── Helper Methods ──────────────────────────────────────────────────────

    /** Returns human-readable file size e.g. "120 KB" */
    public String getFileSizeDisplay() {
        if (fileSize <= 0) return "-- KB";
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return (fileSize / 1024) + " KB";
        return String.format("%.1f MB", fileSize / (1024.0 * 1024));
    }

    /** Returns emoji icon for method */
    public String getMethodIcon() {
        if (method == null) return "";
        switch (method) {
            case "telegram":     return "";
            case "google_drive": return "";
            case "local":        return "";
            default:             return "";
        }
    }

    /** Returns method display name */
    public String getMethodDisplay() {
        if (method == null) return "লোকাল";
        switch (method) {
            case "telegram":     return "Telegram";
            case "google_drive": return "Google Drive";
            case "local":        return "লোকাল স্টোরেজ";
            default:             return "লোকাল";
        }
    }

    /** Returns format display in uppercase */
    public String getFormatDisplay() {
        if (format == null) return "JSON";
        return format.toUpperCase();
    }

    /** Returns data type display */
    public String getDataTypeDisplay() {
        if (dataType == null) return "সব ডেটা";
        switch (dataType) {
            case "all":        return "সব ডেটা";
            case "income":     return "আয়";
            case "expense":    return "ব্যয়";
            case "debt":       return "দেনা";
            case "receivable": return "পাওনা";
            default:           return "সব ডেটা";
        }
    }

    /** Is backup successful */
    public boolean isSuccess() {
        return "success".equals(status);
    }
}
