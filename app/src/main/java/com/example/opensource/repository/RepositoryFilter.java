package com.example.opensource.repository;

import com.example.opensource.RepositoryListAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 저장소 검색 필터
 * - 제목, 초성, 날짜 기준으로 검색어 필터링
 */
public class RepositoryFilter {

    private static final char[] CHO = {
            'ㄱ','ㄲ','ㄴ','ㄷ','ㄸ','ㄹ','ㅁ','ㅂ','ㅃ','ㅅ',
            'ㅆ','ㅇ','ㅈ','ㅉ','ㅊ','ㅋ','ㅌ','ㅍ','ㅎ'
    };

    /**
     * 문자열을 초성 문자열로 변환
     */
    private static String getChosung(String str) {
        StringBuilder result = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (c >= 0xAC00 && c <= 0xD7A3) {
                int uniVal = c - 0xAC00;
                int choIdx = uniVal / (21 * 28);
                result.append(CHO[choIdx]);
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * 검색어를 기반으로 폴더 목록을 필터링
     *
     * @param folders 원본 폴더 리스트
     * @param query   검색어
     * @return 필터링된 폴더 리스트
     */
    public static List<RepositoryListAdapter.FolderListItem> filter(List<RepositoryInfo> folders, String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.getDefault());
        List<RepositoryListAdapter.FolderListItem> filtered = new ArrayList<>();

        for (RepositoryInfo f : folders) {
            if (f != null) {
                String title = f.getname() == null ? "" : f.getname();
                String date = f.getlastModified() == null ? "" : f.getlastModified();

                String titleLower = title.toLowerCase(Locale.getDefault());
                String dateLower = date.toLowerCase(Locale.getDefault());
                String cho = getChosung(title);

                if (q.isEmpty() || titleLower.contains(q) || cho.contains(q) || dateLower.contains(q)) {
                    filtered.add(new RepositoryListAdapter.FolderListItem.Row(f));
                }
            }
        }

        if (q.isEmpty()) {
            filtered.add(0, new RepositoryListAdapter.FolderListItem.Add());
        }
        return filtered;
    }
}
