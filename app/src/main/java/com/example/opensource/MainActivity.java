package com.example.opensource;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.opensource.adapter.FileAdapter;
import com.example.opensource.model.ReceiptFile;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FileAdapter adapter;
    private List<ReceiptFile> fileList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton myPageButton = findViewById(R.id.btnMypage);
        myPageButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, MyPageActivity.class);
            intent.putExtra("nickname", "소프트웨어공학과"); // 실제 닉네임 변수로 교체 가능
            startActivity(intent);
        });

        recyclerView = findViewById(R.id.recyclerView);

        fileList = new ArrayList<>();

        // position 0번에 항상 플러스 카드가 고정되도록 null 추가
        fileList.add(null);

        adapter = new FileAdapter(MainActivity.this, fileList, new FileAdapter.OnAddFolderClickListener() {
            @Override
            public void onAddFolderClick() {
                showAddFolderDialog();
            }
        });


        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void showAddFolderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("폴더 이름을 입력하세요");

        final EditText input = new EditText(MainActivity.this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("확인", (dialog, which) -> {
            String folderName = input.getText().toString().trim();

            if (!folderName.isEmpty()) {
                String date = new SimpleDateFormat("yyyy.MM.dd a h:mm", Locale.getDefault()).format(new Date());
                ReceiptFile newFolder = new ReceiptFile(folderName, "마지막 수정 " + date);
                fileList.add(newFolder);
                adapter.notifyItemInserted(fileList.size() - 1);
            }
        });

        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());

        builder.show();
    }
}
