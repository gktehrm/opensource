package com.example.opensource.receipt.entity;

public class ReceiptItem {
    private String itemName;
    private int quantity;
    private int unitPrice;
    private int subTotal;

    public ReceiptItem(String itemName, int quantity, int unitPrice) {
        this.itemName =  itemName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subTotal = quantity * unitPrice;
}

// Getter/Setter
public String getProductName() { return itemName; }
public void setProductName(String productName) { this.itemName = productName; }

public int getQuantity() { return quantity; }
public void setQuantity(int quantity){ this.quantity = quantity; }

    public int getUnitPrice() { return unitPrice; }
    public void setUnitPrice(int unitPrice) { this.unitPrice = unitPrice; }

    public int getSubTotal() { return subTotal; }
    public void setSubTotal(int subTotal) { this.subTotal = subTotal; }
}
