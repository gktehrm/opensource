package com.example.opensource.receipt.entity;

import java.util.ArrayList;
import java.util.List;

public class Receipt {
    private String storeName;
    private String address;
    private String phoneNumber;
    private String timestamp;
    private List<ReceiptItem> receiptItemList;
    private int receiptTotal;
    private String imageUrl; // 이미지 경로(URL)

    public Receipt() {
        receiptItemList = new ArrayList<>();
        receiptTotal = 0;
    }

    // Getter/Setter
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public List<ReceiptItem> getItemList() { return receiptItemList; }
    public void setItemList(List<ReceiptItem> list) {
        this.receiptItemList = list;
    }


    public int getReceiptTotal() { return receiptTotal; }
    public void setReceiptTotal(int receiptTotal) { this.receiptTotal = receiptTotal; }  // 파이어베이스 추가

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }  // 파이어베이스 추가

    // method
    public void addItem(ReceiptItem receiptItem) {
        receiptItemList.add(receiptItem);
        receiptTotal += receiptItem.getSubTotal();
    }

    public void removeItem(int index) {
        if (index >= 0 && index < receiptItemList.size()) {
            receiptTotal -= receiptItemList.get(index).getSubTotal();
            receiptItemList.remove(index);
        }
    }

    public void updateItem(int index, ReceiptItem newReceiptItem) {
        if (index >= 0 && index < receiptItemList.size()) {
            receiptTotal -= receiptItemList.get(index).getSubTotal();
            receiptItemList.set(index, newReceiptItem);
            receiptTotal += newReceiptItem.getSubTotal();
        }
    }
}
