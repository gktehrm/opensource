package com.example.opensource.folder;

import com.example.opensource.RepositoryListAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FolderFilter {

    // 한글 초성 리스트
    private static final char[] CHO = {
            'ㄱ','ㄲ','ㄴ','ㄷ','ㄸ','ㄹ','ㅁ','ㅂ','ㅃ','ㅅ',
            'ㅆ','ㅇ','ㅈ','ㅉ','ㅊ','ㅋ','ㅌ','ㅍ','ㅎ'
    };

    // 문자열을 초성 문자열로 변환
    private static String getChosung(String str) {
        StringBuilder result = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (c >= 0xAC00 && c <= 0xD7A3) { // 한글 완성형 범위
                int uniVal = c - 0xAC00;
                int choIdx = uniVal / (21 * 28);
                result.append(CHO[choIdx]);
            } else {
                result.append(c); // 한글 아니면 그대로 둠
            }
        }
        return result.toString();
    }

    public static List<RepositoryListAdapter.FolderListItem> filter(List<folderInfo> folders, String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.getDefault());

        List<RepositoryListAdapter.FolderListItem> filtered = new ArrayList<>();

        for (folderInfo f : folders) {
            if (f != null) {
                String title = f.getname() == null ? "" : f.getname();
                String date = f.getlastModified() == null ? "" : f.getlastModified();

                // 소문자 변환
                String titleLower = title.toLowerCase(Locale.getDefault());
                String dateLower = date.toLowerCase(Locale.getDefault());

                // 초성 변환
                String cho = getChosung(title);

                // 조건: 검색어가 비었거나 / 제목 포함하거나 / 초성 포함하거나 / 날짜 포함
                if (q.isEmpty() || titleLower.contains(q) || cho.contains(q) || dateLower.contains(q)) {
                    filtered.add(new RepositoryListAdapter.FolderListItem.Row(f));
                }
            }
        }

        // 검색어가 없을 때만 + 버튼 추가
        if (q.isEmpty()) {
            filtered.add(0, new RepositoryListAdapter.FolderListItem.Add());
        }

        return filtered;
    }
}
