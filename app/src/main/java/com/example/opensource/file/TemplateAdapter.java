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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_template_preview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TemplateItem item = templates.get(position);
        // TODO: 실제 이미지 또는 썸네일 로드 (예: Glide, Picasso 라이브러리 사용 또는 직접 생성)
        holder.previewImageView.setImageResource(item.getPreviewImageResId());
        holder.fileTypeText.setText(item.getFileType());
        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return templates.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView previewImageView;
        TextView fileTypeText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            previewImageView = itemView.findViewById(R.id.template_preview_image);
            fileTypeText = itemView.findViewById(R.id.template_file_type_text);
        }
    }
}