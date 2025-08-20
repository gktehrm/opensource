package com.example.opensource;

import android.app.AlertDialog;
import android.content.Intent;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.opensource.firebase.RepositoryManager;
import com.example.opensource.repository.RepositoryInfo;
import com.example.opensource.repository.RepositoryActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RepositoryListAdapter
 * <p>
 * 저장소(폴더) 리스트와 "추가(Add)" 버튼을 관리하는 RecyclerView 어댑터.
 * - {@link DiffUtil} + {@link ListAdapter} 적용
 * - 날짜 포맷 유틸 제공
 * - Context 직접 의존성을 최소화
 */
public class RepositoryListAdapter extends ListAdapter<RepositoryListAdapter.FolderListItem, RecyclerView.ViewHolder> {

    private static final int TYPE_ADD = 0;
    private static final int TYPE_FOLDER = 1;
    private final FolderActionListener actionListener;

    /**
     * RepositoryListAdapter 생성자
     *
     * @param listener 폴더 추가/삭제/수정 이벤트 리스너
     */
    public RepositoryListAdapter(FolderActionListener listener) {
        super(DIFF_CALLBACK);
        this.actionListener = listener;
    }

    /**
     * DiffUtil 구현체
     * 리스트의 변경사항을 효율적으로 감지
     */
    private static final DiffUtil.ItemCallback<FolderListItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull FolderListItem oldItem, @NonNull FolderListItem newItem) {
                    if (oldItem instanceof FolderListItem.Add && newItem instanceof FolderListItem.Add) {
                        return true;
                    } else if (oldItem instanceof FolderListItem.Row && newItem instanceof FolderListItem.Row) {
                        return ((FolderListItem.Row) oldItem).folder.getId()
                                .equals(((FolderListItem.Row) newItem).folder.getId());
                    }
                    return false;
                }

                @Override
                public boolean areContentsTheSame(@NonNull FolderListItem oldItem, @NonNull FolderListItem newItem) {
                    if (oldItem instanceof FolderListItem.Row && newItem instanceof FolderListItem.Row) {
                        RepositoryInfo f1 = ((FolderListItem.Row) oldItem).folder;
                        RepositoryInfo f2 = ((FolderListItem.Row) newItem).folder;
                        return f1.getId().equals(f2.getId())
                                && f1.getname().equals(f2.getname())
                                && f1.getlastModified().equals(f2.getlastModified());
                    }
                    return true; // Add 항목은 변동 없음
                }
            };

    @Override
    public int getItemViewType(int position) {
        FolderListItem item = getItem(position);
        if (item instanceof FolderListItem.Add) return TYPE_ADD;
        else return TYPE_FOLDER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ADD) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.add_card_bg, parent, false);
            return new AddViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_card, parent, false);
            return new FolderViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        FolderListItem item = getItem(position);

        if (holder instanceof AddViewHolder && item instanceof FolderListItem.Add) {
            ((AddViewHolder) holder).bind(actionListener);
        } else if (holder instanceof FolderViewHolder && item instanceof FolderListItem.Row) {
            ((FolderViewHolder) holder).bind(((FolderListItem.Row) item).folder, this, actionListener);
        }
    }

    // ---------- ViewHolder ----------

    /**
     * "Add" 버튼을 표시하는 ViewHolder
     */
    public static class AddViewHolder extends RecyclerView.ViewHolder {
        public AddViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        /**
         * Add 버튼 클릭 이벤트 바인딩
         *
         * @param listener 폴더 추가 리스너
         */
        public void bind(FolderActionListener listener) {
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onAddFolder();
            });
        }
    }

    /**
     * 실제 폴더 항목을 표시하는 ViewHolder
     */
    public static class FolderViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textDate;
        ImageButton menuButton;

        public FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textDate = itemView.findViewById(R.id.textDate);
            menuButton = itemView.findViewById(R.id.menuButton);
        }

        /**
         * 폴더 데이터를 ViewHolder에 바인딩
         *
         * @param file    저장소 정보
         * @param adapter 어댑터 인스턴스
         * @param listener 이벤트 리스너
         */
        public void bind(RepositoryInfo file, RepositoryListAdapter adapter, FolderActionListener listener) {
            textTitle.setText(file.getname());
            textDate.setText("마지막 수정 " + file.getlastModified());

            // 클릭 → RepositoryActivity 열기
            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(itemView.getContext(), RepositoryActivity.class);
                intent.putExtra("fileName", file.getname());
                intent.putExtra("repositoryId", file.getId()); //저장소 ID 추가
                itemView.getContext().startActivity(intent);
            });

            // 메뉴 버튼 클릭 이벤트
            menuButton.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(v.getContext(), v);
                popup.getMenuInflater().inflate(R.menu.folder_menu, popup.getMenu());

                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.action_rename) {
                        showEditDialog(file, adapter, listener);
                        return true;
                    } else if (item.getItemId() == R.id.action_delete) {
                        if (listener != null) listener.onDeleteFolder(file);
                        return true;
                    }
                    return false;
                });

                popup.show();
            });
        }

        /**
         * 폴더 이름 수정 다이얼로그 표시
         *
         * @param file     수정할 RepositoryInfo
         * @param adapter  어댑터 (리스트 갱신용)
         * @param listener 수정 이벤트 리스너
         */
        private void showEditDialog(RepositoryInfo file, RepositoryListAdapter adapter, FolderActionListener listener) {
            AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());
            builder.setTitle("폴더 이름 수정");

            final EditText input = new EditText(itemView.getContext());
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setText(file.getname());
            builder.setView(input);

            builder.setPositiveButton("확인", (dialog, which) -> {
                String newName = input.getText().toString().trim();
                if (!newName.isEmpty()) {
                    RepositoryManager.renameFolder(file.getId(), newName, new RepositoryManager.OnFolderActionListener() {
                        @Override
                        public void onSuccess() {
                            String date = RepositoryListAdapter.DateUtils.now();

                            // 🔥 새로운 folderInfo 객체 생성
                            RepositoryInfo updated = new RepositoryInfo(file.getId(), newName, date);

                            // 🔥 새로운 리스트 만들어서 교체
                            List<RepositoryListAdapter.FolderListItem> newList = new ArrayList<>();
                            for (RepositoryListAdapter.FolderListItem item : adapter.getCurrentList()) {
                                if (item instanceof RepositoryListAdapter.FolderListItem.Row) {
                                    RepositoryInfo f = ((FolderListItem.Row) item).folder;
                                    if (f.getId().equals(file.getId())) {
                                        newList.add(new RepositoryListAdapter.FolderListItem.Row(updated));
                                    } else {
                                        newList.add(item);
                                    }
                                } else {
                                    newList.add(item); // Add 버튼
                                }
                            }
                            adapter.submitList(newList);

                            if (listener != null) listener.onRenameFolder(updated, newName);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(itemView.getContext(), "이름 변경 실패", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

            builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());
            builder.show();
        }
    }

    // ---------- Item 타입 ----------

    /**
     * RecyclerView에 표시될 아이템 유형 (Add 버튼 / Folder 항목)
     */
    public static abstract class FolderListItem {
        /** "Add" 버튼 항목 */
        public static class Add extends FolderListItem { }

        /** 폴더 항목 */
        public static class Row extends FolderListItem {
            public final RepositoryInfo folder;

            public Row(RepositoryInfo folder) {
                this.folder = folder;
            }
        }
    }

    /**
     * 폴더 동작 이벤트 리스너
     */
    public interface FolderActionListener {
        /** 새로운 폴더 추가 이벤트 */
        void onAddFolder();

        /** 폴더 삭제 이벤트 */
        void onDeleteFolder(RepositoryInfo file);

        /** 폴더 이름 변경 이벤트 */
        void onRenameFolder(RepositoryInfo file, String newName);
    }

    // ---------- 날짜 유틸 ----------

    /**
     * 날짜 관련 유틸 클래스
     */
    public static class DateUtils {
        private static final SimpleDateFormat FORMAT =
                new SimpleDateFormat("yyyy.MM.dd a h:mm", Locale.getDefault());

        /**
         * 현재 시간을 지정된 포맷으로 반환
         *
         * @return 현재 시각 문자열
         */
        public static String now() {
            return FORMAT.format(new Date());
        }
    }
}
