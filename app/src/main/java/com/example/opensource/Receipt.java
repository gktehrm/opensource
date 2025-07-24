package com.example.opensource;

import java.util.ArrayList;
import java.util.List;

public class Receipt {
    private String storeName;
    private String address;
    private String phoneNumber;
    private String timestamp;
    private List<Item> itemList;
    private int receiptTotal;

    public Receipt() {
        itemList = new ArrayList<>();
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

    public List<Item> getItemList() { return itemList; }

    public int getReceiptTotal() { return receiptTotal; }

    // method
    public void addItem(Item item) {
        itemList.add(item);
        receiptTotal += item.getSubTotal();
    }

    public void removeItem(int index) {
        if (index >= 0 && index < itemList.size()) {
            receiptTotal -= itemList.get(index).getSubTotal();
            itemList.remove(index);
        }
    }

    public void updateItem(int index, Item newItem) {
        if (index >= 0 && index < itemList.size()) {
            receiptTotal -= itemList.get(index).getSubTotal();
            itemList.set(index, newItem);
            receiptTotal += newItem.getSubTotal();
        }
    }
}
