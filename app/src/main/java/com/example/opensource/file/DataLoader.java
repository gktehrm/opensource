package com.example.opensource.file;

import android.util.Log;

import com.example.opensource.receipt.ReceiptManager;
import com.example.opensource.receipt.entity.Receipt;

import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class DataLoader {
    public static List<ReportData> loadFromJson(ReceiptManager receiptManager) {
        List<ReportData> dataList = new ArrayList<>();
        try {
            List<Receipt> receipts = receiptManager.getAllReceipts();
            for (int i = 0; i < receipts.size(); i++) {
                Receipt receipt = receipts.get(i);
                dataList.add(new ReportData(
                        receipt.getTimestamp(),
                        receipt.getStoreName(),
                        "내용",
                        String.valueOf(receipt.getReceiptTotal()),
                        "비고"
                ));
            }
        } catch (Exception e) {
            Log.e("DataLoader", "JSON 파일 로드 중 오류", e);
        }
        return dataList;
    }

    public static List<ReportData> loadFromJson(String jsonPath) {
        List<ReportData> dataList = new ArrayList<>();
        try {
            String content = new String(Files.readAllBytes(Paths.get(jsonPath)));
            JSONArray array = new JSONArray(content);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                dataList.add(new ReportData(
                        obj.getString("사용날짜"),
                        obj.getString("구입점명"),
                        obj.getString("내용"),
                        obj.getString("사용금액"),
                        obj.optString("비고", "")
                ));
            }
        } catch (Exception e) {
            Log.e("DataLoader", "JSON 파일 로드 중 오류", e);
        }
        return dataList;
    }
}