package com.jrappspot.cashlipi.models;

/**
 * BackupSettings — All backup configuration in one model.
 */
public class BackupSettings {

    // Auto backup triggers
    private boolean triggerOnIncomeAdded    = true;
    private boolean triggerOnExpenseAdded   = true;
    private boolean triggerOnDebtAdded      = true;
    private boolean triggerOnReceivableAdded = true;
    private boolean triggerOnDataUpdated    = true;
    private boolean triggerOnDataDeleted    = true;

    // Backup methods
    private boolean telegramEnabled      = false;
    private boolean googleDriveEnabled   = true;
    private boolean localStorageEnabled  = true;

    // Auto backup global toggle
    private boolean autoBackupEnabled = true;

    // Frequency: "realtime" | "daily" | "weekly" | "monthly"
    private String backupFrequency = "realtime";

    // Default format for auto backup
    private String defaultFormat = "json";

    // Last backup timestamp
    private String lastBackupAt = "";
    private String lastBackupStatus = "";

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public boolean isTriggerOnIncomeAdded() { return triggerOnIncomeAdded; }
    public void setTriggerOnIncomeAdded(boolean v) { this.triggerOnIncomeAdded = v; }

    public boolean isTriggerOnExpenseAdded() { return triggerOnExpenseAdded; }
    public void setTriggerOnExpenseAdded(boolean v) { this.triggerOnExpenseAdded = v; }

    public boolean isTriggerOnDebtAdded() { return triggerOnDebtAdded; }
    public void setTriggerOnDebtAdded(boolean v) { this.triggerOnDebtAdded = v; }

    public boolean isTriggerOnReceivableAdded() { return triggerOnReceivableAdded; }
    public void setTriggerOnReceivableAdded(boolean v) { this.triggerOnReceivableAdded = v; }

    public boolean isTriggerOnDataUpdated() { return triggerOnDataUpdated; }
    public void setTriggerOnDataUpdated(boolean v) { this.triggerOnDataUpdated = v; }

    public boolean isTriggerOnDataDeleted() { return triggerOnDataDeleted; }
    public void setTriggerOnDataDeleted(boolean v) { this.triggerOnDataDeleted = v; }

    public boolean isTelegramEnabled() { return telegramEnabled; }
    public void setTelegramEnabled(boolean v) { this.telegramEnabled = v; }

    public boolean isGoogleDriveEnabled() { return googleDriveEnabled; }
    public void setGoogleDriveEnabled(boolean v) { this.googleDriveEnabled = v; }

    public boolean isLocalStorageEnabled() { return localStorageEnabled; }
    public void setLocalStorageEnabled(boolean v) { this.localStorageEnabled = v; }

    public boolean isAutoBackupEnabled() { return autoBackupEnabled; }
    public void setAutoBackupEnabled(boolean v) { this.autoBackupEnabled = v; }

    public String getBackupFrequency() { return backupFrequency; }
    public void setBackupFrequency(String v) { this.backupFrequency = v; }

    public String getDefaultFormat() { return defaultFormat; }
    public void setDefaultFormat(String v) { this.defaultFormat = v; }

    public String getLastBackupAt() { return lastBackupAt; }
    public void setLastBackupAt(String v) { this.lastBackupAt = v; }

    public String getLastBackupStatus() { return lastBackupStatus; }
    public void setLastBackupStatus(String v) { this.lastBackupStatus = v; }
}
