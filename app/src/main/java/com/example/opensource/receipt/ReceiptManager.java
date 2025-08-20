package com.example.opensource.receipt;

import com.example.opensource.receipt.entity.Receipt;

import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ReceiptManager {
    private List<Receipt> receipts;

    public ReceiptManager() {
        receipts = new ArrayList<>();
    }

    // 생성 메소드
    public void addReceipt(Receipt receipt) {
        receipts.add(receipt);
    }

    // 읽기 메소드
    public Receipt getReceipt(int index) {
        if (index >= 0 && index < receipts.size()) {
            return receipts.get(index);
        }
        return null;
    }

    public List<Receipt> getAllReceipts() {
        return receipts;
    }

    // 수정 메소드
    public void updateReceipt(int index, Receipt updatedReceipt) {
        if (index >= 0 && index < receipts.size()) {
            receipts.set(index, updatedReceipt);
        }
    }

    // 삭제 메소드
    public void deleteReceipt(int index) {
        if (index >= 0 && index < receipts.size()) {
            receipts.remove(index);
        }
    }

    // JSON 저장
    public JSONArray toJsonArray() throws JSONException {
        JSONArray array = new JSONArray();
        for (Receipt receipt : receipts) {
            array.put(ReceiptParser.toJson(receipt));
        }
        return array;
    }

    // JSON 불러오기
    public void loadFromJsonArray(JSONArray array) {
        receipts.clear();
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj != null) {
                receipts.add(ReceiptParser.parseFromJson(obj));
            }
        }
    }

    // 정렬 (거래일시 오름차순/내림차순)
    public void sortByDate(final boolean ascending) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Collections.sort(receipts, new Comparator<Receipt>() {
            @Override
            public int compare(Receipt r1, Receipt r2) {
                try {
                    Date d1 = sdf.parse(r1.getTimestamp());
                    Date d2 = sdf.parse(r2.getTimestamp());
                    return ascending ? d1.compareTo(d2) : d2.compareTo(d1);
                } catch (Exception e) {
                    return 0;
                }
            }
        });

    }

    // 검색 (가맹점명 포함 여부로)
    public List<Receipt> searchByStore(String keyword) {
        List<Receipt> result = new ArrayList<>();
        for (Receipt r : receipts) {
            if (r.getStoreName().toLowerCase().contains(keyword.toLowerCase())) {
                result.add(r);
            }
        }
        return result;
    }



    // 표시 (콘솔용 디버그 출력)
    public void printAllReceipts() {
        for (int i = 0; i < receipts.size(); i++) {
            Receipt r = receipts.get(i);
            System.out.println("[" + i + "] " + r.getStoreName() + " | " + r.getTimestamp() + " | " + r.getAmount() + "원");
        }
    }
}
