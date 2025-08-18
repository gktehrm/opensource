package com.example.opensource.file; // 실제 패키지 이름으로 변경해주세요.

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract; // SAF 사용을 위해 추가
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.opensource.R;
import com.example.opensource.file.ReportData; // ReportData 클래스가 필요합니다.
import com.example.opensource.file.WordReportGenerator; // WordReportGenerator 클래스가 필요합니다.

// import java.io.File; // SAF 사용 시 직접적인 File 객체 사용은 줄어들 수 있습니다.
import java.io.InputStream;
import java.io.OutputStream; // 파일 쓰기를 위해 추가
import java.io.IOException; // 입출력 예외 처리를 위해 추가
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FileGeneratorActivity extends AppCompatActivity { // 클래스 이름은 상황에 맞게 변경하세요.

    private static final String TAG = "ReportGenerator";
    private RadioGroup fileTypeRadioGroup;
    private RecyclerView templateRecyclerView;
    private Button importTemplateButton;
    private Button generateReportButton;

    private TemplateAdapter templateAdapter;
    private List<TemplateItem> allTemplates; // 모든 양식 리스트
    private List<TemplateItem> filteredTemplates; // 필터링된 양식 리스트
    private TemplateItem selectedTemplate; // 현재 선택된 양식

    // TODO: 작성자 이름은 SharedPreferences, Intent extras 등에서 가져오도록 수정
    private String authorName = "기본 작성자";

    private ActivityResultLauncher<Intent> filePickerLauncher; // 양식 파일 선택기
    private ActivityResultLauncher<Intent> directoryPickerLauncher; // 저장 위치 선택기

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_generator);

        fileTypeRadioGroup = findViewById(R.id.file_type_radio_group);
        templateRecyclerView = findViewById(R.id.template_recycler_view);
        importTemplateButton = findViewById(R.id.import_template_button);
        generateReportButton = findViewById(R.id.generate_report_button);

        allTemplates = new ArrayList<>();
        filteredTemplates = new ArrayList<>();
        loadInitialTemplates();

        templateAdapter = new TemplateAdapter(filteredTemplates, item -> {
            selectedTemplate = item;
            Toast.makeText(FileGeneratorActivity.this, selectedTemplate.getFileName() + " 선택됨", Toast.LENGTH_SHORT).show();
        });
        templateRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        templateRecyclerView.setAdapter(templateAdapter);

        fileTypeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> filterTemplates());
        filterTemplates(); // 초기 필터링 실행

        // 양식 파일 가져오기 결과 처리
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            // TODO: 가져온 양식 파일을 앱 내부 저장소로 복사하고 'allTemplates'에 추가하는 로직 필요.
                            String fileName = getFileNameFromUri(uri);
                            String fileType = "Unknown";
                            if (fileName != null) {
                                if (fileName.endsWith(".docx")) {
                                    fileType = "Word";
                                } else if (fileName.endsWith(".xlsx")) {
                                    fileType = "Excel";
                                }
                            }
                            TemplateItem newTemplate = new TemplateItem(fileName, fileType, uri.toString(), R.drawable.tamplate);
                            allTemplates.add(newTemplate);
                            filterTemplates(); // 양식 리스트 새로고침
                            Toast.makeText(this, "양식 가져오기 성공: " + fileName, Toast.LENGTH_LONG).show();
                        }
                    }
                });

        // 파일 저장 위치 선택 결과 처리 (SAF - Storage Access Framework 사용)
        directoryPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri treeUri = result.getData().getData();
                        if (treeUri != null) {
                            // 사용자가 선택한 디렉토리의 URI를 받았습니다.
                            // 이제 이 URI를 사용하여 생성된 파일을 저장합니다.
                            generateReportWithSelectedPath(treeUri);
                        }
                    }
                }
        );

        importTemplateButton.setOnClickListener(v -> openFilePicker());

        generateReportButton.setOnClickListener(v -> {
            if (selectedTemplate == null) {
                Toast.makeText(this, "먼저 양식을 선택해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            // 현재는 Word 파일 생성만 지원한다고 가정
            if (!"Word".equals(selectedTemplate.getFileType())) {
                Toast.makeText(this, "현재 Word 파일 생성만 지원합니다.", Toast.LENGTH_SHORT).show();
                // TODO: 필요한 경우 Excel 파일 생성 기능 구현
                return;
            }
            openDirectoryPicker(); // 저장 위치 선택기 실행
        });
    }

    // 선택된 저장 경로에 보고서 파일 생성
    // FileGeneratorActivity.java 내부에 있는 메소드

    // 선택된 저장 경로에 보고서 파일 생성
    private void generateReportWithSelectedPath(Uri directoryUri) {
        if (selectedTemplate == null || !"Word".equals(selectedTemplate.getFileType())) {
            Toast.makeText(this, "선택된 Word 양식이 유효하지 않습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: 실제 앱에서는 다른 곳에서 데이터를 가져옵니다. (예: 사용자 입력, 데이터베이스 등)
        List<ReportData> dataList = Arrays.asList(
                new ReportData("2023-01-15 10:30:00", "가게 A", "사무용품 구매", "150.00", "영수증 #123"),
                new ReportData("2023-01-16 14:45:00", "식당 B", "비즈니스 점심", "75.50", ""),
                new ReportData("2023-01-17 09:00:00", "상점 C", "소프트웨어 구독", "29.99", "연간 갱신")
        );

        Uri reportFileUri = null; // try-catch 블록 외부에서 선언
        try {
            // 생성될 보고서 파일의 고유한 이름 생성
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String reportFileName = "보고서_" + timeStamp + ".docx";

            // SAF를 사용하여 선택된 디렉터리에 문서를 생성하고 URI를 반환받습니다.
            reportFileUri = DocumentsContract.createDocument(getContentResolver(), directoryUri, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", reportFileName);

            if (reportFileUri == null) {
                Toast.makeText(this, "보고서 파일을 생성할 수 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            // try-with-resources를 사용하여 스트림을 안전하게 엽니다. 작업이 끝나면 자동으로 닫힙니다.
            // 1. 템플릿 파일(URI)로부터 InputStream을 엽니다.
            // 2. 새로 생성된 보고서 파일(URI)에 데이터를 쓸 OutputStream을 엽니다.
            try (InputStream templateStream = getContentResolver().openInputStream(Uri.parse(selectedTemplate.getFilePath()));
                 OutputStream outputStream = getContentResolver().openOutputStream(reportFileUri)) {

                if (templateStream == null || outputStream == null) {
                    Toast.makeText(this, "파일 스트림을 열 수 없습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // *** 수정된 WordReportGenerator.generateReport 메소드 호출 ***
                // 경로 대신 InputStream과 OutputStream을 직접 전달합니다.
                WordReportGenerator.generateReport(templateStream, outputStream, dataList, authorName);

                Toast.makeText(this, "보고서 생성 완료: " + reportFileName, Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) { // generateReport에서 던진 예외를 포함한 모든 예외를 여기서 처리합니다.
            Log.e(TAG, "보고서 생성 중 오류 발생", e);
            Toast.makeText(this, "보고서 생성 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();

            // 오류 발생 시, 비어있거나 불완전한 파일을 삭제해주는 것이 좋습니다.
            if (reportFileUri != null) {
                try {
                    DocumentsContract.deleteDocument(getContentResolver(), reportFileUri);
                } catch (Exception deleteException) {
                    Log.e(TAG, "오류 후 파일 삭제 실패", deleteException);
                }
            }
        }
    }


    // 초기 양식 데이터 로드 (예시)
    private void loadInitialTemplates() {
        // TODO: 실제 앱에서는 내부 저장소/assets에서 기본 양식을 로드합니다.
        // 예시:
        // allTemplates.add(new TemplateItem("일반 Word 양식", "Word", "내부저장소/양식/template1.docx", R.drawable.ic_word_preview));
        // allTemplates.add(new TemplateItem("매출 Excel 양식", "Excel", "내부저장소/양식/template2.xlsx", R.drawable.ic_excel_preview));

        // 시뮬레이션 데이터 (실제 로직으로 대체 필요)
        // R.raw.sample_word_template은 res/raw 폴더에 sample_word_template.docx 파일이 있다고 가정합니다.
        // R.drawable.ic_template_placeholder는 적절한 미리보기 이미지로 대체해야 합니다.
//        allTemplates.add(new TemplateItem("워드_양식_1.docx", "Word", "android.resource://" + getPackageName() + "/" + R.raw.sample_word_template, R.drawable.ic_template_placeholder));
//        allTemplates.add(new TemplateItem("엑셀_양식_1.xlsx", "Excel", "엑셀_양식_1_경로", R.drawable.ic_template_placeholder));
        allTemplates.add(new TemplateItem("워드_양식_2.docx", "Word", "워드_양식_2_경로", R.drawable.tamplate));

        // R.drawable.ic_template_placeholder 같은 리소스를 생성하거나 다른 리소스를 사용해야 합니다.
        // "경로..." 부분에는 실제 양식 파일 경로 또는 assets/raw에서 복사하는 메커니즘을 사용해야 합니다.
    }

    // 선택된 파일 형식에 따라 양식 필터링
    private void filterTemplates() {
//        RadioButton selectedRadioButton = findViewById(fileTypeRadioGroup.getCheckedRadioButtonId());
        String selectedType = "";
        if (selectedRadioButton != null) {
            selectedType = selectedRadioButton.getText().toString();
            // 라디오 버튼의 텍스트가 "워드 (Word)" 와 같이 되어있으므로, 실제 타입 ("Word", "Excel")과 비교하기 위한 로직이 필요할 수 있습니다.
            // 여기서는 단순화를 위해 RadioButton의 text가 "Word" 또는 "Excel"이라고 가정합니다.
            // 실제로는 id를 비교하거나, tag를 설정하여 비교하는 것이 더 안전합니다.
            if (selectedRadioButton.getId() == R.id.radio_word) {
                selectedType = "Word";
            } else if (selectedRadioButton.getId() == R.id.radio_excel) {
                selectedType = "Excel";
            }
        }

        filteredTemplates.clear();
        for (TemplateItem item : allTemplates) {
            if (item.getFileType().equalsIgnoreCase(selectedType)) {
                filteredTemplates.add(item);
            }
        }
        if (templateAdapter != null) {
            templateAdapter.notifyDataSetChanged();
        }
        selectedTemplate = null; // 필터 변경 시 선택 해제
    }

    // 파일 선택기 열기 (양식 가져오기)
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // 모든 파일 유형 허용 (초기)
        // 특정 파일 유형만 허용하려면 아래와 같이 MIME 유형을 설정합니다.
        // String[] mimeTypes = {"application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/vnd.ms-excel"};
        // intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        filePickerLauncher.launch(intent);
    }

    // 디렉토리 선택기 열기 (파일 저장 위치 선택)
    private void openDirectoryPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        directoryPickerLauncher.launch(intent);
    }

    // Uri에서 파일 이름 가져오기
    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "URI에서 파일 이름 가져오기 오류(content scheme): ", e);
            }
        }
        if (fileName == null) {
            fileName = uri.getPath();
            if (fileName != null) {
                int cut = fileName.lastIndexOf('/');
                if (cut != -1) {
                    fileName = fileName.substring(cut + 1);
                }
            }
        }
        return fileName;
    }
}
