package com.example.opensource.model;

public class ReceiptFile {
    private String title;
    private String date;

    public ReceiptFile(String title, String date) {
        this.title = title;
        this.date = date;
    }

    // 폴더 이름 가져오기
    public String getTitle() {
        return title;
    }

    // 폴더 이름 수정하기
    public void setTitle(String title) {
        this.title = title;
    }

    // 날짜 가져오기
    public String getDate() {
        return date;
    }

    // 날짜 수정하기 (필요 시)
    public void setDate(String date) {
        this.date = date;
    }
}
