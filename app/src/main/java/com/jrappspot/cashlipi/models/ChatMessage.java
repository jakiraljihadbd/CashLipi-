package com.jrappspot.cashlipi.models;

/**
 * AI চ্যাট মেসেজ মডেল — item_chat_user / item_chat_bot / item_chat_typing
 * তিনটা ভিউ টাইপকে রিপ্রেজেন্ট করে।
 */
public class ChatMessage {

    public enum Role { USER, BOT, TYPING }

    private Role role;
    private String text;
    private long timestamp;

    public ChatMessage(Role role, String text) {
        this.role = role;
        this.text = text;
        this.timestamp = System.currentTimeMillis();
    }

    public Role getRole() { return role; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public long getTimestamp() { return timestamp; }
}
