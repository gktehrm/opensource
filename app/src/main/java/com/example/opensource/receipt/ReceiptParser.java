// com/example/opensource/receipt/ReceiptParser.java
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
        receipt.setTimestamp(json.optString("timestamp"));

        // 선택 필드 보완
        receipt.setPaymentMethod(json.optString("paymentMethod"));
        receipt.setUserInformation(json.optString("userInformation"));
        receipt.setImageUri(json.optString("imageUri"));

        JSONArray list = json.optJSONArray("itemList");
        int total = 0;
        if (list != null) {
            for (int i = 0; i < list.length(); i++) {
                JSONObject itemObj = list.optJSONObject(i);
                if (itemObj == null) continue;
                String productName = itemObj.optString("productName");
                int quantity = itemObj.optInt("quantity");
                int unitPrice = itemObj.optInt("unitPrice");
                ReceiptItem receiptItem = new ReceiptItem(productName, quantity, unitPrice);
                receipt.addItem(receiptItem);
                total += receiptItem.getSubTotal();
            }
        }
        // JSON에 receiptTotal이 있으면 우선 적용 (OCR결과가 직접 계산해서 넣어준 경우)
        if (json.has("receiptTotal")) {
            receipt.setAmount(json.optInt("receiptTotal", total));
        } else {
            receipt.setAmount(total);
        }
        return receipt;
    }

    public static JSONObject toJson(Receipt receipt) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("storeName", receipt.getStoreName());
        json.put("address", receipt.getAddress());
        json.put("timestamp", receipt.getTimestamp());
        json.put("paymentMethod", receipt.getPaymentMethod());
        json.put("userInformation", receipt.getUserInformation());
        json.put("imageUri", receipt.getImageUri());

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
        json.put("Amount", receipt.getAmount());
        return json;
    }
}
