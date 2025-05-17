package com.maramagagriculturalaid.app.FarmersData;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
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

    public void updateData(List<Farmer> newFarmers) {
        this.farmersList = newFarmers;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FarmerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_farmer, parent, false);
        return new FarmerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FarmerViewHolder holder, int position) {
        Farmer farmer = farmersList.get(position);
        holder.bind(farmer, listener);
    }

    @Override
    public int getItemCount() {
        return farmersList != null ? farmersList.size() : 0;
    }

    static class FarmerViewHolder extends RecyclerView.ViewHolder {
        private final CardView cardView;
        private final TextView textViewFarmerId;
        private final TextView textViewFarmerName;

        FarmerViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardViewFarmer);
            textViewFarmerId = itemView.findViewById(R.id.textViewFarmerId);
            textViewFarmerName = itemView.findViewById(R.id.textViewFarmerName);
        }

        void bind(final Farmer farmer, final OnFarmerClickListener listener) {
            // Set farmer ID with null check
            textViewFarmerId.setText(farmer.getId() != null ? farmer.getId() : "N/A");

            // Set farmer name with null check
            textViewFarmerName.setText(farmer.getFullName() != null ?
                    farmer.getFullName() : "Unknown Farmer");

            // Set click listener
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFarmerClick(farmer);
                }
            });
        }
    }

    // Helper method to get farmer at position
    public Farmer getFarmerAt(int position) {
        if (position >= 0 && position < getItemCount()) {
            return farmersList.get(position);
        }
        return null;
    }
}