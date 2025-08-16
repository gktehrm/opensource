package com.example.opensource.folder;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.example.opensource.RepositoryListAdapter;
import com.example.opensource.repository.RepositoryInfo;

import java.util.List;

/**
 * 검색창 동작 관리 (검색/뒤로가기 처리)
 */
public class SearchHelper {

    // 🔹 검색창 세팅
    public static void setupSearch(EditText searchBar, List<RepositoryInfo> fileList, RepositoryListAdapter adapter) {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.submitList(FolderFilter.filter(fileList, s.toString()));
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // 🔹 뒤로가기 처리 (검색창 비우기 → 전체 목록 복원)
    public static boolean handleBackPress(EditText searchBar, List<RepositoryInfo> fileList, RepositoryListAdapter adapter) {
        if (searchBar != null && !searchBar.getText().toString().isEmpty()) {
            searchBar.setText("");
            adapter.submitList(FolderFilter.filter(fileList, ""));
            return true; // 뒤로가기 소비
        }
        return false; // 기본 뒤로가기 동작 실행
    }
}
