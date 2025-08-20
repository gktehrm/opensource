package com.example.opensource.file;

import java.util.List;

public interface ReportDataCallback {
    void onSuccess(List<ReportData> dataList);
    void onFailure(Exception e);
}
