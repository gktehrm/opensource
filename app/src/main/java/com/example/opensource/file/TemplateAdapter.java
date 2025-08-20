package com.example.opensource.file;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.opensource.R;

import java.util.List;

class TemplateAdapter extends RecyclerView.Adapter<TemplateAdapter.ViewHolder> {
    private int selectedPosition = RecyclerView.NO_POSITION;
    private List<TemplateItem> templates;
    private OnItemClickListener listener;

    interface OnItemClickListener {
        void onItemClick(TemplateItem item);
    }

    public TemplateAdapter(List<TemplateItem> templates, OnItemClickListener listener) {
        this.templates = templates;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_template_preview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TemplateItem item = templates.get(position);

        holder.previewImageView.setImageResource(item.getPreviewImageResId());
        holder.fileNameText.setText(item.getFileName());

        // 선택된 경우 표시
        holder.itemView.setBackgroundResource(
                position == selectedPosition ? R.drawable.selected_border : 0
        );

        holder.itemView.setOnClickListener(v -> {
            selectedPosition = holder.getAdapterPosition();
            notifyDataSetChanged(); // 전체 새로고침 (간단한 방법)
            listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return templates.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView previewImageView;
        TextView fileNameText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            previewImageView = itemView.findViewById(R.id.template_preview_image);
            fileNameText = itemView.findViewById(R.id.template_file_name_text);
        }
    }
}
