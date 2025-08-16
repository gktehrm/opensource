// com/example/opensource/receipt/entity/Receipt.java
package com.example.opensource.receipt.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Receipt implements Serializable {
    private String storeName;
    private String address;
    private String phoneNumber;
    private String timestamp;

    private List<ReceiptItem> receiptItemList;

    private int receiptTotal;
    private String paymentMethod;
    private String userInformation;

    // 이미지: Intent로 주고받기 위해 Bitmap 대신 Uri String 권장
    private String imageUri; // content://... 또는 file://...

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

    public int getReceiptTotal() { return receiptTotal; }
    public void setReceiptTotal(int receiptTotal) { this.receiptTotal = receiptTotal; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getUserInformation() { return userInformation; }
    public void setUserInformation(String userInformation) { this.userInformation = userInformation; }

    public String getImageUri() { return imageUri; }
    public void setImageUri(String imageUri) { this.imageUri = imageUri; }

    // item 조작 시 총액 갱신
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
