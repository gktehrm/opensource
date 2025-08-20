package com.example.opensource.file;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.opensource.R;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class FileGeneratorActivity extends AppCompatActivity {
    private static final String TAG = "ReportGenerator";

    private RecyclerView templateRecyclerView;
    private Button importTemplateButton, generateReportButton;

    private List<TemplateItem> allTemplates;
    private TemplateItem selectedTemplate;
    private TemplateAdapter templateAdapter;

    private String authorName = "기본 작성자";

    private ActivityResultLauncher<Intent> filePickerLauncher;
    private ActivityResultLauncher<Intent> directoryPickerLauncher;

    private Uri selectedDirectoryUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_generator);

        initViews();
        initLaunchers();
        initTemplateList();
        setupListeners();
    }

    private void initViews() {
        templateRecyclerView = findViewById(R.id.template_recycler_view);
        importTemplateButton = findViewById(R.id.import_template_button);
        generateReportButton = findViewById(R.id.generate_report_button);
    }

    private void initLaunchers() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        handleTemplateImport(uri);
                    }
                });

        directoryPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedDirectoryUri = result.getData().getData();
                        if (selectedDirectoryUri != null) {
                            fetchDataFromFirestoreAndGenerateReport();
                        }
                    }
                });
    }

    private void initTemplateList() {
        allTemplates = new ArrayList<>();
        loadInitialTemplates();

        templateAdapter = new TemplateAdapter(allTemplates, item -> {
            selectedTemplate = item;
        });

        templateRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        templateRecyclerView.setAdapter(templateAdapter);
    }

    private void setupListeners() {
        importTemplateButton.setOnClickListener(v -> openFilePicker());
        generateReportButton.setOnClickListener(v -> {
            if (selectedTemplate == null || !"Word".equals(selectedTemplate.getFileType())) {
                Toast.makeText(this, "Word 양식을 선택해주세요.", Toast.LENGTH_SHORT).show();
            } else {
                openDirectoryPicker();
            }
        });
    }

    private void handleTemplateImport(Uri uri) {
        if (uri == null) return;

        String fileName = getFileNameFromUri(uri);
        String fileType = fileName != null && fileName.endsWith(".docx") ? "Word" : "Unknown";

        TemplateItem newTemplate = new TemplateItem(fileName, fileType, uri.toString(), R.drawable.docs);
        allTemplates.add(newTemplate);
        templateAdapter.notifyDataSetChanged();

        Toast.makeText(this, "양식 가져오기 성공: " + fileName, Toast.LENGTH_LONG).show();
    }

    // 🔹 Firestore에서 데이터 불러오고 보고서 생성
    private void fetchDataFromFirestoreAndGenerateReport() {
        String repositoryId = getIntent().getStringExtra("repositoryId");
        if (repositoryId == null) {
            Toast.makeText(this, "저장소 ID가 누락되었습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        ReportService reportService = new ReportService();
        reportService.loadReportData(repositoryId, new ReportDataCallback() {
            @Override
            public void onSuccess(List<ReportData> dataList) {
                createReportFile(selectedDirectoryUri, dataList);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Firestore에서 데이터 로드 실패", e);
                Toast.makeText(FileGeneratorActivity.this, "데이터 로드 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // 🔹 Word 보고서 생성
    private void createReportFile(Uri directoryUri, List<ReportData> dataList) {
        Uri reportFileUri = null;
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String reportFileName = "보고서_" + timeStamp + ".docx";

            String docId = DocumentsContract.getTreeDocumentId(directoryUri);
            Uri dirDocumentUri = DocumentsContract.buildDocumentUriUsingTree(directoryUri, docId);

            reportFileUri = DocumentsContract.createDocument(
                    getContentResolver(),
                    dirDocumentUri,
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    reportFileName
            );

            if (reportFileUri == null) {
                Toast.makeText(this, "보고서 파일 생성 실패", Toast.LENGTH_SHORT).show();
                return;
            }

            try (
                    InputStream templateStream = getContentResolver().openInputStream(Uri.parse(selectedTemplate.getFilePath()));
                    OutputStream outputStream = getContentResolver().openOutputStream(reportFileUri)
            ) {
                if (templateStream == null || outputStream == null) {
                    Toast.makeText(this, "파일 스트림을 열 수 없습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                WordReportGenerator.generateReport(templateStream, outputStream, dataList, authorName);
                Toast.makeText(this, "보고서 생성 완료", Toast.LENGTH_LONG).show();

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(reportFileUri, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                shareIntent.putExtra(Intent.EXTRA_STREAM, reportFileUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                Intent chooser = Intent.createChooser(shareIntent, "보고서 열기 또는 공유");
                chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { intent });
                startActivity(chooser);

            }

        } catch (Exception e) {
            Log.e(TAG, "보고서 생성 중 오류", e);
            Toast.makeText(this, "보고서 생성 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();

            if (reportFileUri != null) {
                try {
                    DocumentsContract.deleteDocument(getContentResolver(), reportFileUri);
                } catch (Exception deleteException) {
                    Log.e(TAG, "오류 발생 후 임시 파일 삭제 실패", deleteException);
                }
            }
        }
    }

    // 🔸 샘플 템플릿 불러오기
    private void loadInitialTemplates() {
        String uri = "android.resource://" + getPackageName() + "/" + R.raw.report_template_1;
        allTemplates.add(new TemplateItem("워드_양식_1.docx", "Word", uri, R.drawable.tamplate));

        uri = "android.resource://" + getPackageName() + "/" + R.raw.report_template_2;
        allTemplates.add(new TemplateItem("워드_양식_1.docx", "Word", uri, R.drawable.tamplate2));
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        filePickerLauncher.launch(intent);
    }

    private void openDirectoryPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        directoryPickerLauncher.launch(intent);
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "파일 이름 가져오기 오류", e);
            }
        }

        if (fileName == null && uri.getPath() != null) {
            int cut = uri.getPath().lastIndexOf('/');
            if (cut != -1) {
                fileName = uri.getPath().substring(cut + 1);
            }
        }

        return fileName;
    }
}
