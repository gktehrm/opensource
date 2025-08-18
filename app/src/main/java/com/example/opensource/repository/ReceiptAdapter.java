package com.example.opensource.repository;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.opensource.R;
import com.example.opensource.receipt.entity.Receipt;

import java.util.List;

public class ReceiptAdapter extends RecyclerView.Adapter<ReceiptAdapter.VH> {

    public interface OnItemClickListener {
        void onClick(int position, Receipt receipt);
    }

    private List<Receipt> data;
    private OnItemClickListener listener;

    public ReceiptAdapter(List<Receipt> data, OnItemClickListener listener) {
        this.data = data;
        this.listener = listener;
    }

    public void setReceipts(List<Receipt> newData) {
        this.data = newData;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_receipt, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Receipt r = data.get(pos);
        h.title.setText(r.getStoreName());
        h.date.setText(r.getTimestamp());
        h.sub.setText(r.getReceiptTotal() + "원");
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(pos, r);
        });
    }

    @Override
    public int getItemCount() { return data == null ? 0 : data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, date, sub;
        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvStoreName);
            date = itemView.findViewById(R.id.tvReceiptDate);
            sub = itemView.findViewById(R.id.tvReceiptTotal);
        }
    }
}
