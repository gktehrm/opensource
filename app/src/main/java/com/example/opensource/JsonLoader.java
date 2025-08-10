package com.example.opensource;

import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class JsonLoader {
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
            e.printStackTrace();
        }
        return dataList;
    }
}