package com.example.opensource.file;

import com.example.opensource.receipt.entity.Receipt;

import java.util.*;

public class ReportDataMapper {
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
