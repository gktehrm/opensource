package com.example.opensource.file;

import com.example.opensource.receipt.entity.Receipt;

public class ReportDataMapper {
    public static ReportData map(Receipt receipt) {
        if (receipt == null) return null;

        return new ReportData(
                receipt.getTimestamp(),
                receipt.getStoreName(),
                receipt.getUserInformation(),
                receipt.getAmount(),
                ""
        );
    }
}
