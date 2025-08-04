package com.example.opensource;

public class ReportData {
    private String timestamp;
    private String storeName;
    private String content;
    private String amount;
    private String note;

    public ReportData(String timeStamp, String storeName, String content, String amount, String note) {
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

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}