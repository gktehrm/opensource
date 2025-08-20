package com.example.opensource.file;

import org.apache.poi.xwpf.usermodel.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class WordReportGenerator {

    /**
     * 템플릿 기반 Word 보고서를 생성합니다.
     *
     * @param templateInputStream 템플릿 Word(docx) InputStream
     * @param outputStream 출력할 OutputStream
     * @param dataList 보고서 데이터
     * @param authorName 작성자 이름
     */
    public static void generateReport(InputStream templateInputStream, OutputStream outputStream,
                                      List<ReportData> dataList, String authorName) throws Exception {

        try (XWPFDocument doc = new XWPFDocument(templateInputStream)) {
            XWPFTable table = doc.getTables().get(0);

            for (int i = 0; i < dataList.size(); i++) {
                ReportData d = dataList.get(i);

                // 행 가져오기 또는 새로 생성
                XWPFTableRow row;
                if (i + 1 < table.getNumberOfRows()) {
                    row = table.getRow(i + 1);
                } else {
                    row = table.createRow();
                }

                // 셀 개수 보장
                while (row.getTableCells().size() < 5) {
                    row.addNewTableCell();
                }

                // 날짜 포맷 변환
                String formattedDate;
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy년 MM월 dd일 HH시 mm분");
                    Date parsedDate = inputFormat.parse(d.getTimestamp());
                    formattedDate = outputFormat.format(parsedDate);
                } catch (Exception e) {
                    formattedDate = d.getTimestamp(); // 변환 실패 시 원본 사용
                }

                // 데이터 삽입
                row.getCell(0).setText(formattedDate);
                row.getCell(1).setText(d.getStoreName());
                row.getCell(2).setText(d.getContent());
                row.getCell(3).setText(String.valueOf(d.getAmount()));
                row.getCell(4).setText(d.getNote());
            }

            // 텍스트 치환 (예: "0000년 00월 00일" → 오늘 날짜)
            for (XWPFParagraph para : doc.getParagraphs()) {
                String text = para.getText();
                if (text.contains("0000년 00월 00일")) {
                    String today = new SimpleDateFormat("yyyy년 MM월 dd일").format(new Date());

                    List<XWPFRun> runs = para.getRuns();
                    for (int i = runs.size() - 1; i >= 0; i--) {
                        para.removeRun(i);
                    }
                    para.createRun().setText(today);
                }
            }

            doc.write(outputStream);
        }
    }
}
