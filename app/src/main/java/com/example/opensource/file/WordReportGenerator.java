package com.example.opensource.file;

import org.apache.poi.xwpf.usermodel.*;

import java.io.InputStream;  // FileInputStream 대신 사용
import java.io.OutputStream; // FileOutputStream 대신 사용
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.io.IOException; // 예외 처리를 위해 추가

public class WordReportGenerator {

    /**
     * 파일 경로 대신 InputStream과 OutputStream을 직접 받아 보고서를 생성합니다.
     * @param templateInputStream 템플릿 파일의 InputStream
     * @param outputStream 결과 보고서를 쓸 OutputStream
     * @param dataList 보고서에 채울 데이터 리스트
     * @param authorName 작성자 이름
     * @throws IOException 파일 I/O 작업 중 발생할 수 있는 예외
     */
    public static void generateReport(InputStream templateInputStream, OutputStream outputStream, List<ReportData> dataList, String authorName) throws Exception {

        try (XWPFDocument doc = new XWPFDocument(templateInputStream)) {
            XWPFTable table = doc.getTables().get(0);

// 기존 데이터 행 모두 삭제 (헤더 제외)
            int existingRows = table.getNumberOfRows();
            for (int i = 0; i < dataList.size(); i++) {
                ReportData d = dataList.get(i);

                // 행 가져오거나 새로 생성
                XWPFTableRow row;
                if (i + 1 < table.getNumberOfRows()) {
                    row = table.getRow(i + 1);
                } else {
                    row = table.createRow();
                }

                // 셀 수가 부족할 경우 추가
                while (row.getTableCells().size() < 5) {
                    row.addNewTableCell();
                }

                // 날짜 포맷 처리
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy년 MM월 dd일 HH시 mm분");

                String formattedDate;
                try {
                    Date parsedDate = inputFormat.parse(d.getTimestamp());
                    formattedDate = outputFormat.format(parsedDate);
                } catch (Exception e) {
                    formattedDate = d.getTimestamp();
                }

                // 데이터 삽입
                row.getCell(0).setText(formattedDate);
                row.getCell(1).setText(d.getStoreName());
                row.getCell(2).setText(d.getContent());
                row.getCell(3).setText(String.valueOf(d.getAmount()));
                row.getCell(4).setText(d.getNote());
            }




            for (XWPFParagraph para : doc.getParagraphs()) {
                String text = para.getText();
                if (text.contains("0000년 00월 00일")) {
                    Calendar calendar = Calendar.getInstance();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일");
                    String today = sdf.format(calendar.getTime());

                    // 단락의 텍스트를 직접 교체하는 것이 더 안정적일 수 있습니다.
                    // 기존 Run을 지우고 새 Run을 추가하거나, Run의 텍스트를 교체합니다.
                    List<XWPFRun> runs = para.getRuns();
                    if (!runs.isEmpty()) {
                        // 기존 텍스트를 지우고 새로 씁니다.
                        for (int i = runs.size() - 1; i >= 0; i--) {
                            para.removeRun(i);
                        }
                        para.createRun().setText(today);
                    } else {
                        para.createRun().setText(today);
                    }
                }
            }

            // 전달받은 OutputStream에 문서를 씁니다.
            doc.write(outputStream);
            // try-with-resources가 outputStream을 닫지 않도록 doc만 닫습니다.
            // outputStream은 이 메소드를 호출한 Activity에서 닫아야 합니다.

        } // XWPFDocument는 여기서 자동으로 닫힙니다.
        // catch 블록을 제거하고, 호출한 쪽(Activity)에서 예외를 처리하도록 throws를 사용합니다.
    }

}