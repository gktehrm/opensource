package com.example.opensource.file;

import com.example.opensource.file.ReportData;
import com.example.opensource.receipt.entity.Receipt;

import java.util.List;

public interface ReportDataCallback {
    void onSuccess(List<ReportData> dataList);

    void onFailure(Exception e);

    public static ReportData map(Receipt receipt) {
        return new ReportData(
                receipt.getTimestamp(),
                receipt.getStoreName(),
                "내용",
                receipt.getAmount(),
                "비고"
        );
    }
}

