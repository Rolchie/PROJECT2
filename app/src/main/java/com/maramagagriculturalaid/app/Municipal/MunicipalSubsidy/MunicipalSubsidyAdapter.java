package com.maramagagriculturalaid.app.Municipal.MunicipalSubsidy;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.maramagagriculturalaid.app.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MunicipalSubsidyAdapter extends RecyclerView.Adapter<MunicipalSubsidyAdapter.SubsidyViewHolder> {

    private Context context;
    private List<MunicipalSubsidyItem> subsidyItems;
    private boolean isMunicipalMode = false;

    public MunicipalSubsidyAdapter(Context context, List<MunicipalSubsidyItem> subsidyItems) {
        this.context = context;
        this.subsidyItems = subsidyItems;
    }

    public void setMunicipalMode(boolean municipalMode) {
        this.isMunicipalMode = municipalMode;
    }

    public void updateData(List<MunicipalSubsidyItem> newItems) {
        this.subsidyItems = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SubsidyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_municipal_subsidy_request, parent, false);
        return new SubsidyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubsidyViewHolder holder, int position) {
        MunicipalSubsidyItem item = subsidyItems.get(position);
        holder.bind(item, isMunicipalMode);

        // Set click listener for the entire item
        holder.itemView.setOnClickListener(v -> openSubsidyDetails(item));

        // Set click listener for the view details button
        holder.btnViewDetails.setOnClickListener(v -> openSubsidyDetails(item));
    }

    private void openSubsidyDetails(MunicipalSubsidyItem item) {
        Intent intent = new Intent(context, MunicipalSubsidyDetails.class);
        intent.putExtra("subsidyId", item.getId());
        if (item.getBarangay() != null) {
            intent.putExtra("barangay", item.getBarangay());
        }
        intent.putExtra("farmerName", item.getFarmerName());
        intent.putExtra("farmerId", item.getFarmerId());
        intent.putExtra("subsidyType", item.getSubsidyType());
        intent.putExtra("status", item.getStatus());
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return subsidyItems != null ? subsidyItems.size() : 0;
    }

    public static class SubsidyViewHolder extends RecyclerView.ViewHolder {
        private TextView tvFarmerId, tvFarmerName, tvSubsidyType, tvStatus, tvDate, tvBarangay;
        private ImageButton btnViewDetails;

        public SubsidyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFarmerId = itemView.findViewById(R.id.tv_farmer_id);
            tvFarmerName = itemView.findViewById(R.id.tv_farmer_name);
            tvSubsidyType = itemView.findViewById(R.id.tv_subsidy_type);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvBarangay = itemView.findViewById(R.id.tv_barangay);
            btnViewDetails = itemView.findViewById(R.id.btn_view_details);
        }

        public void bind(MunicipalSubsidyItem item, boolean isMunicipalMode) {
            // Set farmer ID
            if (item.getFarmerId() != null && !item.getFarmerId().isEmpty()) {
                tvFarmerId.setText("ID: " + item.getFarmerId());
                tvFarmerId.setVisibility(View.VISIBLE);
            } else {
                tvFarmerId.setText("ID: Not Available");
                tvFarmerId.setVisibility(View.VISIBLE);
            }

            // Set farmer name
            if (item.getFarmerName() != null && !item.getFarmerName().isEmpty()) {
                tvFarmerName.setText(item.getFarmerName());
            } else {
                tvFarmerName.setText("Unknown Farmer");
            }

            // Set barangay info - show in municipal mode or if explicitly available
            if (isMunicipalMode) {
                if (item.getBarangay() != null && !item.getBarangay().isEmpty()) {
                    tvBarangay.setText("From: " + item.getBarangay());
                    tvBarangay.setVisibility(View.VISIBLE);
                } else {
                    tvBarangay.setText("From: Unknown Barangay");
                    tvBarangay.setVisibility(View.VISIBLE);
                }
            } else {
                // Hide barangay info in single barangay mode
                tvBarangay.setVisibility(View.GONE);
            }

            // Set subsidy type
            if (item.getSubsidyType() != null && !item.getSubsidyType().isEmpty()) {
                tvSubsidyType.setText("Subsidy Type: " + item.getSubsidyType());
            } else {
                tvSubsidyType.setText("Subsidy Type: Not Specified");
            }

            // Set date - prioritize dateApplied, fallback to timestamp
            String displayDate = getDisplayDate(item);
            tvDate.setText(displayDate);

            // Set status with appropriate styling
            String status = item.getStatus();
            if (status != null && !status.isEmpty()) {
                tvStatus.setText(status);
                setStatusBackground(status);
            } else {
                tvStatus.setText("Pending");
                setStatusBackground("Pending");
            }
        }

        private String getDisplayDate(MunicipalSubsidyItem item) {
            // Try to use dateApplied first
            if (item.getDateApplied() != null && !item.getDateApplied().isEmpty()) {
                return item.getDateApplied();
            }

            // Try to use dateApproved if status is approved
            if (item.isApproved() && item.getDateApproved() != null && !item.getDateApproved().isEmpty()) {
                return item.getDateApproved();
            }

            // Try to format timestamp
            if (item.getTimestamp() != null) {
                try {
                    Date date;
                    if (item.getTimestamp() instanceof Long) {
                        date = new Date((Long) item.getTimestamp());
                    } else if (item.getTimestamp() instanceof com.google.firebase.Timestamp) {
                        date = ((com.google.firebase.Timestamp) item.getTimestamp()).toDate();
                    } else {
                        return "Unknown Date";
                    }
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    return sdf.format(date);
                } catch (Exception e) {
                    return "Invalid Date";
                }
            }

            return "No Date Available";
        }

        private void setStatusBackground(String status) {
            switch (status.toLowerCase().trim()) {
                case "approved":
                    tvStatus.setBackgroundResource(R.drawable.status_approved_background);
                    tvStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.white));
                    break;
                case "rejected":
                    tvStatus.setBackgroundResource(R.drawable.status_rejected_background);
                    tvStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.white));
                    break;
                case "pending":
                default:
                    tvStatus.setBackgroundResource(R.drawable.status_pending_background);
                    tvStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.black));
                    break;
            }
        }
    }

    // Utility methods for filtering
    public void filterByStatus(String status) {
        // This method can be implemented if you need client-side filtering
        notifyDataSetChanged();
    }

    public void clearData() {
        if (subsidyItems != null) {
            subsidyItems.clear();
            notifyDataSetChanged();
        }
    }

    public int getItemCountByStatus(String status) {
        if (subsidyItems == null) return 0;

        int count = 0;
        for (MunicipalSubsidyItem item : subsidyItems) {
            if (item.getStatus() != null && item.getStatus().equalsIgnoreCase(status)) {
                count++;
            }
        }
        return count;
    }

    // Get counts for different statuses
    public int getPendingCount() {
        return getItemCountByStatus("Pending");
    }

    public int getApprovedCount() {
        return getItemCountByStatus("Approved");
    }

    public int getRejectedCount() {
        return getItemCountByStatus("Rejected");
    }
}