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
                        receipt.getAmount(),
                        "비고"
                ));
            }
        } catch (Exception e) {
            Log.e("DataLoader", "JSON 파일 로드 중 오류", e);
        }
        return dataList;
    }

    public static List<ReportData> fromReceipts(List<Receipt> receipts) {
        List<ReportData> list = new ArrayList<>();
        for (Receipt r : receipts) {
            list.add(new ReportData(
                    r.getTimestamp(),
                    r.getStoreName(),
                    "내용",   // 또는 항목 요약 정보
                    r.getAmount(), // 금액
                    "비고"   // 비고
            ));
        }
        return list;
    }

}