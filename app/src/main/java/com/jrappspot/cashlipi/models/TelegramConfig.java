package com.jrappspot.cashlipi.models;

/**
 * TelegramConfig — Telegram Bot configuration for backup.
 */
public class TelegramConfig {
    private String botToken;
    private String chatId;
    private boolean enabled;
    private boolean autoBackup;
    private String preferredFormat; // json | pdf | docx | xlsx

    public TelegramConfig() {
        this.preferredFormat = "json";
        this.enabled = false;
        this.autoBackup = false;
    }

    public String getBotToken() { return botToken; }
    public void setBotToken(String botToken) { this.botToken = botToken; }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isAutoBackup() { return autoBackup; }
    public void setAutoBackup(boolean autoBackup) { this.autoBackup = autoBackup; }

    public String getPreferredFormat() { return preferredFormat; }
    public void setPreferredFormat(String preferredFormat) { this.preferredFormat = preferredFormat; }

    public boolean isValid() {
        return botToken != null && !botToken.isEmpty()
            && chatId != null && !chatId.isEmpty();
    }
}
