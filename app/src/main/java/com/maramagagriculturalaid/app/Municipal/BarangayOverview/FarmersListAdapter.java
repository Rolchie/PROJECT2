package com.maramagagriculturalaid.app.Municipal.BarangayOverview;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.maramagagriculturalaid.app.FarmersData.Farmer;
import com.maramagagriculturalaid.app.R;

import java.util.List;

public class FarmersListAdapter extends RecyclerView.Adapter<FarmersListAdapter.FarmerViewHolder> {

    private static final String TAG = "FarmersListAdapter";
    private List<Farmer> farmersList;
    private OnFarmerClickListener listener;

    public interface OnFarmerClickListener {
        void onFarmerClick(Farmer farmer);
    }

    public FarmersListAdapter(List<Farmer> farmersList) {
        this.farmersList = farmersList;
    }

    public void setOnFarmerClickListener(OnFarmerClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public FarmerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_municipal_farmer_list, parent, false);
        return new FarmerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FarmerViewHolder holder, int position) {
        Farmer farmer = farmersList.get(position);
        holder.bind(farmer);
    }

    @Override
    public int getItemCount() {
        return farmersList != null ? farmersList.size() : 0;
    }

    public class FarmerViewHolder extends RecyclerView.ViewHolder {
        private CardView cardView;
        private TextView tvFarmerId;
        private TextView tvFarmerName;

        public FarmerViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize views
            cardView = (CardView) itemView;
            tvFarmerId = itemView.findViewById(R.id.tvFarmerId);
            tvFarmerName = itemView.findViewById(R.id.tvFarmerName);

            // Set click listener without any visual feedback
            cardView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onFarmerClick(farmersList.get(getAdapterPosition()));
                }
            });

            // Remove any foreground/background effects
            cardView.setForeground(null);
            cardView.setClickable(true);
            cardView.setFocusable(true);
        }

        public void bind(Farmer farmer) {
            if (farmer == null) {
                Log.w(TAG, "Farmer is null in bind method");
                return;
            }

            // DEBUG: Log farmer data being bound
            Log.d(TAG, "=== BINDING FARMER ===");
            Log.d(TAG, "Farmer ID: '" + farmer.getId() + "'");
            Log.d(TAG, "Document ID: '" + farmer.getDocumentId() + "'");
            Log.d(TAG, "Full Name: '" + farmer.getFullName() + "'");
            Log.d(TAG, "First Name: '" + farmer.getFirstName() + "'");
            Log.d(TAG, "Last Name: '" + farmer.getLastName() + "'");
            Log.d(TAG, "=== END BINDING ===");

            // Farmer ID - Display the actual ID, not the name
            String farmerId = farmer.getId();
            Log.d(TAG, "Retrieved farmer ID: '" + farmerId + "'");

            if (farmerId != null && !farmerId.isEmpty()) {
                tvFarmerId.setText("ID: " + farmerId);
                Log.d(TAG, "Set ID field to: 'ID: " + farmerId + "'");
            } else {
                // Try document ID as fallback
                String docId = farmer.getDocumentId();
                if (docId != null && !docId.isEmpty()) {
                    tvFarmerId.setText("ID: " + docId);
                    Log.d(TAG, "Set ID field to document ID: 'ID: " + docId + "'");
                } else {
                    tvFarmerId.setText("ID: Not assigned");
                    Log.d(TAG, "Set ID field to: 'ID: Not assigned'");
                }
            }

            // Farmer Name - Display only the name
            String fullName = farmer.getFullName();
            Log.d(TAG, "Retrieved farmer name: '" + fullName + "'");

            if (fullName != null && !fullName.isEmpty()) {
                // Add farm type to the name display (but keep name separate from ID)
                StringBuilder nameBuilder = new StringBuilder(fullName);

                String farmType = farmer.getFarmType();
                if (farmType != null && !farmType.isEmpty()) {
                    nameBuilder.append(" • ").append(farmType);
                }

                tvFarmerName.setText(nameBuilder.toString());
                Log.d(TAG, "Set name field to: '" + nameBuilder.toString() + "'");
            } else {
                tvFarmerName.setText("Name not available");
                Log.d(TAG, "Set name field to: 'Name not available'");
            }
        }
    }

    // Method to update the list
    public void updateFarmersList(List<Farmer> newFarmersList) {
        if (newFarmersList != null) {
            this.farmersList = newFarmersList;
            notifyDataSetChanged();
        }
    }

    // Method to get farmer at position with bounds checking
    public Farmer getFarmerAt(int position) {
        if (farmersList != null && position >= 0 && position < farmersList.size()) {
            return farmersList.get(position);
        }
        return null;
    }

    // Method to get the current list size
    public int getFarmersCount() {
        return farmersList != null ? farmersList.size() : 0;
    }

    // Method to check if list is empty
    public boolean isEmpty() {
        return farmersList == null || farmersList.isEmpty();
    }
}
