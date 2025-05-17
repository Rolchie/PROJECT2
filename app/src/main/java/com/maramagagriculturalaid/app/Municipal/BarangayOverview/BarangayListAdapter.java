package com.maramagagriculturalaid.app.Municipal.BarangayOverview;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.maramagagriculturalaid.app.R;

import java.util.List;

public class BarangayListAdapter extends RecyclerView.Adapter<BarangayListAdapter.BarangayListViewHolder> {

    private List<Barangay> barangayList;
    private OnBarangayClickListener listener;

    public interface OnBarangayClickListener {
        void onBarangayClick(int position);
    }

    public BarangayListAdapter(List<Barangay> barangayList, OnBarangayClickListener listener) {
        this.barangayList = barangayList;
        this.listener = listener;
    }

    public void updateList(List<Barangay> newList) {
        this.barangayList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BarangayListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_barangay_list, parent, false);
        return new BarangayListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BarangayListViewHolder holder, int position) {
        Barangay barangay = barangayList.get(position);

        // Set barangay data
        holder.tvBarangayName.setText(barangay.getName());
        holder.tvFarmersCount.setText(String.valueOf(barangay.getFarmersCount()));
        holder.tvLocation.setText(barangay.getLocation());

        // Make the entire card clickable
        holder.itemView.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (listener != null && adapterPosition != RecyclerView.NO_POSITION) {
                listener.onBarangayClick(adapterPosition);
            }
        });

        // Make the farmers info section clickable as well for better UX
        holder.farmersInfoSection.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (listener != null && adapterPosition != RecyclerView.NO_POSITION) {
                listener.onBarangayClick(adapterPosition);
            }
        });

        // Set clickable properties
        holder.itemView.setClickable(true);
        holder.itemView.setFocusable(true);
    }

    @Override
    public int getItemCount() {
        return barangayList != null ? barangayList.size() : 0;
    }

    static class BarangayListViewHolder extends RecyclerView.ViewHolder {
        TextView tvBarangayName, tvFarmersCount, tvLocation;
        ImageView ivBarangayIcon, ivLocationIcon;
        LinearLayout farmersInfoSection;
        View accentBar, divider;

        public BarangayListViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize all views from your layout
            tvBarangayName = itemView.findViewById(R.id.tvBarangayName);
            tvFarmersCount = itemView.findViewById(R.id.tvFarmersCount);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            ivBarangayIcon = itemView.findViewById(R.id.ivBarangayIcon);
            ivLocationIcon = itemView.findViewById(R.id.ivLocationIcon);
            farmersInfoSection = itemView.findViewById(R.id.farmersInfoSection);
            accentBar = itemView.findViewById(R.id.accentBar);
            divider = itemView.findViewById(R.id.divider);
        }
    }
}