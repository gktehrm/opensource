package com.example.opensource.file;

public class ReportData {
    private String timestamp;
    private String storeName;
    private String content;  // 항목 설명 (현재 placeholder)
    private int amount;      // 금액
    private String note;     // 비고

    public ReportData(String timestamp, String storeName, String content, int amount, String note) {
        this.timestamp = timestamp;
        this.storeName = storeName;
        this.content = content;
        this.amount = amount;
        this.note = note;
    }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
