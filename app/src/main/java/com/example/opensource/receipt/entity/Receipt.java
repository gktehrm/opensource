// com/example/opensource/receipt/entity/Receipt.java
package com.example.opensource.receipt.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Receipt implements Serializable {
    private String id;
    private int amount;
    private String storeName;
    private String address;
    private String timestamp;
    private String note;

    private List<ReceiptItem> itemList;

    private int receiptTotal;
    private String paymentMethod;
    private String userInformation;

    //로컬 저장 결로
    private String imageUri;
    //Firebase 업로드 후 다운로드 URI
    private String imageUrl;

    public Receipt() {
        itemList = new ArrayList<>();
        amount = 0;
    }

    // Getter/Setter
    // 🔹 Getter/Setter for id
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public List<ReceiptItem> getItemList() { return itemList; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getUserInformation() { return userInformation; }
    public void setUserInformation(String userInformation) { this.userInformation = userInformation; }

    public String getImageUri() { return imageUri; }
    public void setImageUri(String imageUri) { this.imageUri = imageUri; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    // item 조작 시 총액 갱신
    public void addItem(ReceiptItem receiptItem) {
        itemList.add(receiptItem);
        receiptTotal += receiptItem.getSubTotal();
    }
    public void removeItem(int index) {
        if (index >= 0 && index < itemList.size()) {
            receiptTotal -= itemList.get(index).getSubTotal();
            itemList.remove(index);
        }
    }
    public void updateItem(int index, ReceiptItem newReceiptItem) {
        if (index >= 0 && index < itemList.size()) {
            receiptTotal -= itemList.get(index).getSubTotal();
            itemList.set(index, newReceiptItem);
            receiptTotal += newReceiptItem.getSubTotal();
        }
    }
}
