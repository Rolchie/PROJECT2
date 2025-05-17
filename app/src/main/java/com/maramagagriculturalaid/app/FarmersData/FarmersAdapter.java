package com.maramagagriculturalaid.app.FarmersData;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.maramagagriculturalaid.app.R;

import java.util.List;

public class FarmersAdapter extends RecyclerView.Adapter<FarmersAdapter.FarmerViewHolder> {

    private List<Farmer> farmersList;
    private final OnFarmerClickListener listener;

    public interface OnFarmerClickListener {
        void onFarmerClick(Farmer farmer);
    }

    public FarmersAdapter(List<Farmer> farmersList, OnFarmerClickListener listener) {
        this.farmersList = farmersList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FarmerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_farmer, parent, false);
        return new FarmerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FarmerViewHolder holder, int position) {
        Farmer farmer = farmersList.get(position);

        holder.textFarmerId.setText(farmer.getId());
        holder.textFarmerName.setText(farmer.getFullName());

        // Set click listener for the entire item
        holder.itemView.setOnClickListener(v -> listener.onFarmerClick(farmer));
    }

    @Override
    public int getItemCount() {
        return farmersList.size();
    }

    public void updateList(List<Farmer> newList) {
        this.farmersList = newList;
        notifyDataSetChanged();
    }

    static class FarmerViewHolder extends RecyclerView.ViewHolder {
        TextView textFarmerId;
        TextView textFarmerName;

        public FarmerViewHolder(@NonNull View itemView) {
            super(itemView);
            textFarmerId = itemView.findViewById(R.id.txtFarmerId);
            textFarmerName = itemView.findViewById(R.id.txtFarmerName);
        }
    }
}