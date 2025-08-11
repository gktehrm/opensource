package com.example.opensource.file;

import org.apache.poi.xwpf.usermodel.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class WordReportGenerator {
    public static void generateReport(String templatePath, String savePath, List<ReportData> dataList, String authorName) {
        try {
            XWPFDocument doc = new XWPFDocument(new FileInputStream(templatePath));
            XWPFTable table = doc.getTables().get(0);

            for (int i = 0; i < dataList.size(); i++) {
                ReportData d = dataList.get(i);
                XWPFTableRow row = (i + 1 < table.getNumberOfRows()) ? table.getRow(i + 1) : table.createRow();
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy년 MM월 dd일 HH시 mm분");

                String formattedDate = "";
                try {
                    Date parsedDate = inputFormat.parse(d.getTimestamp());
                    formattedDate = outputFormat.format(parsedDate);
                } catch (Exception e) {
                    formattedDate = d.getTimestamp();  // 파싱 실패 시 원본 그대로
                }
                while (row.getTableCells().size() < 5) {
                    row.addNewTableCell();
                }

                row.getCell(0).setText(formattedDate);
                row.getCell(1).setText(d.getStoreName());
                row.getCell(2).setText(d.getContent());
                row.getCell(3).setText(d.getAmount());
                row.getCell(4).setText(d.getNote());
            }

            // 하단 날짜 및 작성자
            for (XWPFParagraph para : doc.getParagraphs()) {
                String text = para.getText();
                if (text.contains("0000년 00월 00일")) {
                    Calendar calendar = Calendar.getInstance();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일");
                    String today = sdf.format(calendar.getTime());

                    List<XWPFRun> runs = para.getRuns();
                    if (!runs.isEmpty()) {
                        runs.get(0).setText(today, 0);
                    }
                }
            }

            FileOutputStream out = new FileOutputStream(savePath);
            doc.write(out);
            out.close();
            doc.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
