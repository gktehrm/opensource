package com.example.opensource.receipt.entity;
// 영수증 Firestore에 넣을 Map 변환

import com.example.opensource.receipt.entity.Receipt;
import com.example.opensource.receipt.entity.ReceiptItem;

import org.json.JSONObject;
import java.util.*;

public class ReceiptMapper {
    public static Map<String, Object> toMap(Receipt r) {
        Map<String, Object> map = new HashMap<>();
        map.put("storeName", r.getStoreName());
        map.put("address", r.getAddress());
        map.put("phoneNumber", r.getPhoneNumber());
        map.put("timestamp", r.getTimestamp());          // "yyyy-MM-dd HH:mm:ss" 가정
        map.put("paymentMethod", r.getPaymentMethod());
        map.put("userInformation", r.getUserInformation());
        map.put("receiptTotal", r.getReceiptTotal());

        // itemList -> List<Map>
        List<Map<String, Object>> items = new ArrayList<>();
        if (r.getItemList() != null) {
            for (ReceiptItem it : r.getItemList()) {
                Map<String, Object> m = new HashMap<>();
                m.put("productName", it.getProductName());
                m.put("quantity", it.getQuantity());
                m.put("unitPrice", it.getUnitPrice());
                m.put("subTotal", it.getSubTotal());
                items.add(m);
            }
        }
        map.put("itemList", items);

        // 이미지 URL(업로드 후 채움)
        map.put("imageUrl", r.getImageUrl());  // Receipt에 imageUrl 필드가 없다면 생략 or getter 추가

        return map;
    }

    // Map → Receipt (Firestore 읽기용)
    public static Receipt fromMap(Map<String, Object> map) {
        Receipt r = new Receipt();
        r.setStoreName((String) map.get("storeName"));
        r.setAddress((String) map.get("address"));
        r.setPhoneNumber((String) map.get("phoneNumber"));
        r.setTimestamp((String) map.get("timestamp"));
        r.setPaymentMethod((String) map.get("paymentMethod"));
        r.setUserInformation((String) map.get("userInformation"));
        if (map.get("receiptTotal") != null) {
            r.setReceiptTotal(((Long) map.get("receiptTotal")).intValue());
        }

        // Firestore에서 imageUrl도 읽어오기
        r.setImageUrl((String) map.get("imageUrl"));

        // 로컬 저장용 imageUri가 필요하다면 그대로 둠
        r.setImageUri((String) map.get("imageUri"));

        // itemList 복원
        List<Map<String, Object>> items = (List<Map<String, Object>>) map.get("itemList");
        if (items != null) {
            for (Map<String, Object> m : items) {
                ReceiptItem it = new ReceiptItem(
                        (String) m.get("productName"),
                        ((Long) m.get("quantity")).intValue(),
                        ((Long) m.get("unitPrice")).intValue()
                );
                r.addItem(it);
            }
        }
        return r;
    }
}
