package com.maramagagriculturalaid.app.Notification;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.maramagagriculturalaid.app.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private static final String TAG = "NotificationAdapter";
    private List<Notification> notifications;
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public NotificationAdapter(List<Notification> notifications, OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
        Log.d(TAG, "Adapter created with " + (notifications != null ? notifications.size() : 0) + " notifications");
    }

    public void updateNotifications(List<Notification> newNotifications) {
        if (newNotifications == null) {
            Log.w(TAG, "Attempting to update with null notifications list");
            return;
        }

        this.notifications = newNotifications;
        Log.d(TAG, "Notifications updated with " + newNotifications.size() + " items");
        notifyDataSetChanged(); // Make sure UI updates
    }

    public int getPosition(Notification notification) {
        if (notifications == null || notification == null) {
            return -1;
        }
        return notifications.indexOf(notification);
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "Creating view holder");
        View view;
        try {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notifications, parent, false);
            Log.d(TAG, "Successfully inflated item_notifications layout");
        } catch (Exception e) {
            Log.e(TAG, "Error inflating item_notifications layout", e);
            try {
                // Try singular version
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_notifications, parent, false);
                Log.d(TAG, "Successfully inflated item_notification layout");
            } catch (Exception e2) {
                Log.e(TAG, "Error inflating item_notification layout", e2);
                // Create a simple fallback view
                TextView textView = new TextView(parent.getContext());
                textView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                textView.setPadding(20, 20, 20, 20);
                textView.setText("Error loading notification view");
                textView.setTextSize(16);
                view = textView;
            }
        }
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Log.d(TAG, "Binding view holder at position " + position);

        if (notifications == null || position >= notifications.size()) {
            Log.w(TAG, "Invalid position or null notifications list");
            return;
        }

        Notification notification = notifications.get(position);
        if (notification == null) {
            Log.w(TAG, "Null notification at position " + position);
            return;
        }

        Log.d(TAG, "Binding notification: " + notification.getFullName() + " - " + notification.getStatus());

        // Set farmer name
        try {
            String farmerName = notification.getFullName();
            String displayName = (farmerName != null && !farmerName.trim().isEmpty()) ?
                    farmerName : "Unknown Farmer";

            if (holder.tvFarmerName != null) {
                holder.tvFarmerName.setText(displayName);
                Log.d(TAG, "Set farmer name: " + displayName);
            } else {
                Log.w(TAG, "tvFarmerName is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting farmer name", e);
            if (holder.tvFarmerName != null) {
                holder.tvFarmerName.setText("Unknown Farmer");
            }
        }

        // Set status with color coding
        try {
            String status = notification.getStatus();
            String displayStatus = (status != null && !status.trim().isEmpty()) ? status : "Unknown";

            if (holder.tvStatus != null) {
                holder.tvStatus.setText(displayStatus);

                int statusColor;
                if (status != null) {
                    switch (status.toLowerCase().trim()) {
                        case "approved":
                            statusColor = holder.itemView.getContext().getResources()
                                    .getColor(android.R.color.holo_green_dark);
                            break;
                        case "rejected":
                            statusColor = holder.itemView.getContext().getResources()
                                    .getColor(android.R.color.holo_red_dark);
                            break;
                        case "pending":
                            statusColor = holder.itemView.getContext().getResources()
                                    .getColor(android.R.color.holo_orange_dark);
                            break;
                        default:
                            statusColor = holder.itemView.getContext().getResources()
                                    .getColor(android.R.color.darker_gray);
                            break;
                    }
                    holder.tvStatus.setTextColor(statusColor);
                }
                Log.d(TAG, "Set status: " + displayStatus);
            } else {
                Log.w(TAG, "tvStatus is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting status", e);
            if (holder.tvStatus != null) {
                holder.tvStatus.setText("Unknown");
            }
        }

        // Set date
        try {
            if (holder.tvDate != null) {
                if (notification.getTimestamp() != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                    holder.tvDate.setText(sdf.format(notification.getTimestamp()));
                    Log.d(TAG, "Set timestamp: " + notification.getTimestamp());
                } else {
                    holder.tvDate.setText("Date not available");
                    Log.d(TAG, "No timestamp available");
                }
            } else {
                Log.w(TAG, "tvDate is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting date", e);
            if (holder.tvDate != null) {
                holder.tvDate.setText("Date not available");
            }
        }

        // Set read/unread state
        try {
            if (holder.cardView != null) {
                if (notification.isRead()) {
                    holder.cardView.setCardBackgroundColor(
                            holder.itemView.getContext().getResources()
                                    .getColor(android.R.color.white));
                } else {
                    // Light blue/gray for unread
                    holder.cardView.setCardBackgroundColor(
                            holder.itemView.getContext().getResources()
                                    .getColor(android.R.color.background_light));
                }
            }

            if (holder.unreadIndicator != null) {
                holder.unreadIndicator.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);
            }

            Log.d(TAG, "Set read state: " + notification.isRead());
        } catch (Exception e) {
            Log.e(TAG, "Error setting read/unread state", e);
        }

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            Log.d(TAG, "Notification clicked at position " + position);
            if (listener != null) {
                listener.onNotificationClick(notification);
            }
        });
    }

    @Override
    public int getItemCount() {
        int count = notifications != null ? notifications.size() : 0;
        Log.d(TAG, "getItemCount: " + count);
        return count;
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvFarmerName, tvStatus, tvDate;
        View unreadIndicator;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);

            Log.d(TAG, "Initializing ViewHolder");

            try {
                // Try to find all the views
                cardView = itemView.findViewById(R.id.card_view);
                tvFarmerName = itemView.findViewById(R.id.tv_farmer_name);
                tvStatus = itemView.findViewById(R.id.tv_status);
                tvDate = itemView.findViewById(R.id.tv_date);
                unreadIndicator = itemView.findViewById(R.id.unread_indicator);

                // Log which views were found
                Log.d(TAG, "cardView found: " + (cardView != null));
                Log.d(TAG, "tvFarmerName found: " + (tvFarmerName != null));
                Log.d(TAG, "tvStatus found: " + (tvStatus != null));
                Log.d(TAG, "tvDate found: " + (tvDate != null));
                Log.d(TAG, "unreadIndicator found: " + (unreadIndicator != null));

                // If we're using a fallback TextView, use it as the farmer name display
                if (tvFarmerName == null && itemView instanceof TextView) {
                    tvFarmerName = (TextView) itemView;
                    Log.d(TAG, "Using fallback TextView as farmer name display");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error finding views in ViewHolder", e);
            }
        }
    }
}