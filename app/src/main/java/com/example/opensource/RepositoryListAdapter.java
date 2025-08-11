package com.example.opensource;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.opensource.repository.RepositoryActivity;
import com.example.opensource.repository.RepositoryInfo;

import java.util.List;

public class RepositoryListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ADD = 0;
    private static final int TYPE_FOLDER = 1;

    private List<RepositoryInfo> fileList;
    private OnAddFolderClickListener addFolderClickListener;
    private Context context;

    public RepositoryListAdapter(Context context, List<RepositoryInfo> fileList, OnAddFolderClickListener listener) {
        this.context = context;
        this.fileList = fileList;
        this.addFolderClickListener = listener;
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return fileList.get(position) == null ? TYPE_ADD : TYPE_FOLDER;
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
        if (getItemViewType(position) == TYPE_FOLDER) {
            FolderViewHolder folderHolder = (FolderViewHolder) holder;
            RepositoryInfo file = fileList.get(position);

            folderHolder.textTitle.setText(file.getTitle());
            folderHolder.textDate.setText(file.getDate());

            folderHolder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, RepositoryActivity.class);
                intent.putExtra("fileName", file.getTitle());  // 파일명 전달
                context.startActivity(intent);
            });

            folderHolder.menuButton.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(v.getContext(), v);
                popup.getMenuInflater().inflate(R.menu.folder_menu, popup.getMenu());

                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.menu_edit) {
                        showEditDialog(v.getContext(), holder.getAdapterPosition());
                        return true;
                    } else if (item.getItemId() == R.id.menu_delete) {
                        fileList.remove(holder.getAdapterPosition());
                        notifyItemRemoved(holder.getAdapterPosition());
                        return true;
                    }
                    return false;
                });

                popup.show();
            });
        }
    }

    // 폴더 수정 다이얼로그
    private void showEditDialog(Context context, int position) {
        RepositoryInfo file = fileList.get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("폴더 이름 수정");

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(file.getTitle());
        builder.setView(input);

        builder.setPositiveButton("확인", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                file.setTitle(newName);
                notifyItemChanged(position);
            }
        });

        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    // 추가 버튼 뷰홀더
    public class AddViewHolder extends RecyclerView.ViewHolder {
        public AddViewHolder(@NonNull View itemView) {
            super(itemView);
            itemView.setOnClickListener(v -> {
                if (addFolderClickListener != null) {
                    addFolderClickListener.onAddFolderClick();
                }
            });
        }
    }

    // 폴더 카드 뷰홀더
    public class FolderViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textDate;
        ImageButton menuButton;

        public FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textDate = itemView.findViewById(R.id.textDate);
            menuButton = itemView.findViewById(R.id.menuButton);
        }
    }

    // 인터페이스 정의
    public interface OnAddFolderClickListener {
        void onAddFolderClick();
    }
}
