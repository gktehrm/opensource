package com.example.opensource.file;

public class ReportData {
    private String timestamp;
    private String storeName;
    private String content;
    private int amount;
    private String note;

    public ReportData(String timeStamp, String storeName, String content, int amount, String note) {
        this.timestamp = timeStamp;
        this.storeName = storeName;
        this.content = content;
        this.amount = amount;
        this.note = note;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}