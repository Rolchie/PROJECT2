package com.maramagagriculturalaid.app.Municipal.BarangayOverview;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.maramagagriculturalaid.app.R;

import java.util.List;
public class BarangayAdapter extends RecyclerView.Adapter<BarangayAdapter.BarangayViewHolder> {

    private List<Barangay> barangayList;
    private OnBarangayClickListener listener;

    public interface OnBarangayClickListener {
        void onBarangayClick(int position);
    }

    public BarangayAdapter(List<Barangay> barangayList, OnBarangayClickListener listener) {
        this.barangayList = barangayList;
        this.listener = listener;
    }

    public void updateList(List<Barangay> newList) {
        this.barangayList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BarangayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_barangay_list, parent, false);
        return new BarangayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BarangayViewHolder holder, int position) {
        Barangay barangay = barangayList.get(position);
        holder.tvBarangayName.setText(barangay.getName());
        holder.tvFarmersCount.setText(String.valueOf(barangay.getFarmersCount()));
    }

    @Override
    public int getItemCount() {
        return barangayList.size();
    }

    class BarangayViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView tvBarangayName, tvFarmersCount;

        public BarangayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBarangayName = itemView.findViewById(R.id.tvBarangayName);
            tvFarmersCount = itemView.findViewById(R.id.tvFarmersCount);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (listener != null) {
                listener.onBarangayClick(getAdapterPosition());
            }
        }
    }
}
