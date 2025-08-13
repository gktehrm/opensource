package com.example.opensource.receipt;

import com.example.opensource.receipt.entity.Receipt;
import com.example.opensource.receipt.entity.ReceiptItem;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

public class ReceiptParser {

    public static Receipt parseFromJson(JSONObject json) {
        Receipt receipt = new Receipt();
        receipt.setStoreName(json.optString("storeName"));
        receipt.setAddress(json.optString("address"));
        receipt.setPhoneNumber(json.optString("phoneNumber"));
        receipt.setTimestamp(json.optString("timestamp"));

        JSONArray list = json.optJSONArray("itemList");
        if (list != null) {
            for (int i = 0; i < list.length(); i++) {
                JSONObject itemObj = list.optJSONObject(i);
                String productName = itemObj.optString("productName");
                int quantity = itemObj.optInt("quantity");
                int unitPrice = itemObj.optInt("unitPrice");

                ReceiptItem receiptItem = new ReceiptItem(productName, quantity, unitPrice);
                receipt.addItem(receiptItem);
            }
        }

        return receipt;
    }

    public static JSONObject toJson(Receipt receipt) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("storeName", receipt.getStoreName());
        json.put("address", receipt.getAddress());
        json.put("phoneNumber", receipt.getPhoneNumber());
        json.put("timestamp", receipt.getTimestamp());

        JSONArray list = new JSONArray();
        for (ReceiptItem receiptItem : receipt.getItemList()) {
            JSONObject itemJson = new JSONObject();
            itemJson.put("productName", receiptItem.getProductName());
            itemJson.put("quantity", receiptItem.getQuantity());
            itemJson.put("unitPrice", receiptItem.getUnitPrice());
            itemJson.put("subTotal", receiptItem.getSubTotal());
            list.put(itemJson);
        }

        json.put("itemList", list);
        json.put("receiptTotal", receipt.getReceiptTotal());

        return json;
    }
}
