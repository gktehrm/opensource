package com.example.opensource.repository;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.example.opensource.RepositoryListAdapter;

import java.util.List;

/**
 * 검색창 동작 관리 클래스
 * - 검색어 입력 시 필터링 처리
 * - 뒤로가기 시 검색창 초기화
 */
public class RepositorySearchHelper {

    /**
     * 검색창에 TextWatcher를 등록하여 실시간 필터링을 적용
     *
     * @param searchBar 검색창(EditText)
     * @param fileList  전체 폴더 리스트
     * @param adapter   어댑터
     */
    public static void setupSearch(EditText searchBar, List<RepositoryInfo> fileList, RepositoryListAdapter adapter) {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.submitList(RepositoryFilter.filter(fileList, s.toString()));
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * 뒤로가기 처리
     * - 검색창에 입력이 있으면 초기화 후 전체 목록 복원
     *
     * @return true: 뒤로가기 이벤트 소비 / false: 기본 동작 실행
     */
    public static boolean handleBackPress(EditText searchBar, List<RepositoryInfo> fileList, RepositoryListAdapter adapter) {
        if (searchBar != null && !searchBar.getText().toString().isEmpty()) {
            searchBar.setText("");
            adapter.submitList(RepositoryFilter.filter(fileList, ""));
            return true;
        }
        return false;
    }
}
