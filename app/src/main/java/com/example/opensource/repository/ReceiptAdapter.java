package com.example.opensource.receipt;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.opensource.R;
import com.example.opensource.receipt.entity.Receipt;

import java.util.List;

public class ReceiptAdapter extends RecyclerView.Adapter<ReceiptAdapter.ReceiptViewHolder> {
    private List<Receipt> receiptList;

    public ReceiptAdapter(List<Receipt> receiptList) {
        this.receiptList = receiptList;
    }

    @NonNull
    @Override
    public ReceiptViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_receipt, parent, false);
        return new ReceiptViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ReceiptViewHolder holder, int position) {
        Receipt receipt = receiptList.get(position);
        holder.tvStore.setText(receipt.getStoreName());
        holder.tvDate.setText(receipt.getTimestamp());
        holder.tvTotal.setText(receipt.getReceiptTotal() + "원");
    }

    @Override
    public int getItemCount() {
        return receiptList.size();
    }

    public void setReceipts(List<Receipt> newReceipts) {
        receiptList = newReceipts;
        notifyDataSetChanged();
    }

    static class ReceiptViewHolder extends RecyclerView.ViewHolder {
        TextView tvStore, tvDate, tvTotal;

        ReceiptViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStore = itemView.findViewById(R.id.tvStoreName);
            tvDate = itemView.findViewById(R.id.tvReceiptDate);
            tvTotal = itemView.findViewById(R.id.tvReceiptTotal);
        }
    }
}
