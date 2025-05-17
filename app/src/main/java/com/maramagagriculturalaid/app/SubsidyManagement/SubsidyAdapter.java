package com.maramagagriculturalaid.app.SubsidyManagement;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.maramagagriculturalaid.app.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SubsidyAdapter extends RecyclerView.Adapter<SubsidyAdapter.ViewHolder> {

    private static final String TAG = "SubsidyAdapter";
    private List<SubsidyApplication> applications;
    private List<String> documentIds; // Store document IDs in a list
    private Context context;
    private String currentBarangay;

    public SubsidyAdapter(Context context, List<SubsidyApplication> applications) {
        this.context = context;
        this.applications = applications;
        this.currentBarangay = "Anahawon"; // Default value
    }

    // Method to set the current barangay
    public void setCurrentBarangay(String barangay) {
        this.currentBarangay = barangay;
        Log.d(TAG, "Current barangay set to: " + barangay);
    }

    // Method to set document IDs for the applications
    public void setDocumentIds(List<String> documentIds) {
        this.documentIds = documentIds;

        // Log the document IDs for debugging
        if (documentIds != null) {
            Log.d(TAG, "Setting " + documentIds.size() + " document IDs");
            for (int i = 0; i < Math.min(documentIds.size(), 5); i++) { // Log first 5 for brevity
                Log.d(TAG, "Position " + i + " has document ID: " + documentIds.get(i));
            }
        } else {
            Log.e(TAG, "Document IDs list is null");
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subsidy_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SubsidyApplication application = applications.get(position);

        // Get document ID for this position
        String documentId = null;
        if (documentIds != null && position < documentIds.size()) {
            documentId = documentIds.get(position);
        }

        // Log binding for debugging
        Log.d(TAG, "Binding position " + position +
                " with application: " + (application != null ? application.getFarmerName() : "null") +
                " and document ID: " + documentId);

        holder.bind(application, documentId);
    }

    @Override
    public int getItemCount() {
        return applications != null ? applications.size() : 0;
    }

    public void updateData(List<SubsidyApplication> newApplications) {
        this.applications = newApplications;
        notifyDataSetChanged();
        Log.d(TAG, "Data updated with " + (newApplications != null ? newApplications.size() : 0) + " applications");
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFarmerName, tvFarmerId, tvSubsidyType, tvDate, tvStatus;
        ImageButton btnViewDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFarmerName = itemView.findViewById(R.id.tv_farmer_name);
            tvFarmerId = itemView.findViewById(R.id.tv_farmer_id);
            tvSubsidyType = itemView.findViewById(R.id.tv_subsidy_type);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvStatus = itemView.findViewById(R.id.tv_status);
            btnViewDetails = itemView.findViewById(R.id.btn_view_details);
        }

        public void bind(SubsidyApplication application, final String documentId) {
            if (application == null) {
                Log.e(TAG, "Cannot bind null application at position " + getAdapterPosition());
                return;
            }

            // Set farmer name
            String farmerName = application.getFarmerName();
            tvFarmerName.setText(farmerName != null ? farmerName : "N/A");

            // Set farmer ID
            String farmerId = application.getFarmerId();
            tvFarmerId.setText(String.format("ID: %s", farmerId != null ? farmerId : "N/A"));

            // Set subsidy type
            String supportType = application.getSupportType();
            tvSubsidyType.setText(String.format("Subsidy Type: %s", supportType != null ? supportType : "N/A"));

            // Format and set date
            long timestamp = application.getTimestamp();
            if (timestamp > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                String formattedDate = sdf.format(new Date(timestamp));
                tvDate.setText(formattedDate);
            } else {
                tvDate.setText("N/A");
            }

            // Set status with appropriate background
            String status = application.getStatus();
            if (status == null) {
                status = "Pending";
            }
            tvStatus.setText(status);

            // Set status background based on status
            int backgroundResId;
            switch (status.toLowerCase()) {
                case "approved":
                    backgroundResId = R.drawable.status_approved_background;
                    break;
                case "rejected":
                    backgroundResId = R.drawable.status_rejected_background;
                    break;
                case "pending":
                default:
                    backgroundResId = R.drawable.status_pending_background;
                    break;
            }
            tvStatus.setBackgroundResource(backgroundResId);

            // Set click listeners
            View.OnClickListener clickListener = v -> {
                if (documentId != null && !documentId.isEmpty()) {
                    Log.d(TAG, "Navigating to detail screen for document ID: " + documentId);

                    // Create intent to navigate to SubsidyDetailActivity
                    Intent intent = new Intent(context, SubsidyDetailsActivity.class);

                    // Pass necessary data
                    intent.putExtra("subsidyId", documentId);
                    intent.putExtra("barangay", currentBarangay);

                    // Start the activity
                    context.startActivity(intent);
                } else {
                    Log.e(TAG, "Cannot navigate: Document ID is null or empty for position " + getAdapterPosition());
                    Toast.makeText(context,
                            "Error: Could not find document ID for this application",
                            Toast.LENGTH_SHORT).show();
                }
            };

            btnViewDetails.setOnClickListener(clickListener);
            itemView.setOnClickListener(clickListener);
        }
    }
}