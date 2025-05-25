package com.maramagagriculturalaid.app.Notification;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.maramagagriculturalaid.app.R;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<Notification> notifications;
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public NotificationAdapter(List<Notification> notifications, OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notifications, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        if (notifications != null && position < notifications.size()) {
            Notification notification = notifications.get(position);
            if (notification != null) {
                holder.bind(notification);
            }
        }
    }

    @Override
    public int getItemCount() {
        return notifications != null ? notifications.size() : 0;
    }

    public void updateNotifications(List<Notification> newNotifications) {
        this.notifications = newNotifications;
        notifyDataSetChanged();
    }

    public int getPosition(Notification notification) {
        if (notifications != null && notification != null && notification.getId() != null) {
            for (int i = 0; i < notifications.size(); i++) {
                Notification currentNotification = notifications.get(i);
                if (currentNotification != null && currentNotification.getId() != null &&
                        currentNotification.getId().equals(notification.getId())) {
                    return i;
                }
            }
        }
        return -1;
    }

    public class NotificationViewHolder extends RecyclerView.ViewHolder {
        private TextView tvFarmerName;
        private TextView tvMessage;
        private TextView tvTimestamp;
        private TextView tvStatus;
        private View unreadIndicator; // FIXED: Changed from readIndicator to unreadIndicator

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize views - make sure these IDs exist in your item_notification.xml
            tvFarmerName = itemView.findViewById(R.id.tv_farmer_name);
            tvMessage = itemView.findViewById(R.id.tv_message);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            tvStatus = itemView.findViewById(R.id.tv_status);
            unreadIndicator = itemView.findViewById(R.id.unread_indicator); // FIXED: Using unread_indicator

            // Set click listener
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION && notifications != null &&
                                position < notifications.size()) {
                            Notification notification = notifications.get(position);
                            if (notification != null) {
                                listener.onNotificationClick(notification);
                            }
                        }
                    }
                }
            });
        }

        public void bind(Notification notification) {
            if (notification == null) {
                return;
            }

            try {
                // Set farmer name
                if (tvFarmerName != null) {
                    String farmerName = notification.getFullName();
                    tvFarmerName.setText(farmerName != null ? farmerName : "Unknown Farmer");
                }

                // Set the formatted message for tv_message
                // This will show: "The Subsidy Application of ID: FarmerID, FarmerName has been approved or rejected"
                if (tvMessage != null) {
                    String message = notification.getFormattedMessage();
                    tvMessage.setText(message != null ? message : "No message available");
                }

                // Set timestamp
                if (tvTimestamp != null) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                        String timestampText = sdf.format(notification.getTimestamp());
                        tvTimestamp.setText(timestampText);
                    } catch (Exception e) {
                        tvTimestamp.setText("Unknown time");
                    }
                }

                // Set status
                if (tvStatus != null) {
                    String status = notification.getStatus();
                    tvStatus.setText(status != null ? status : "Unknown");

                    // Update status background color
                    updateStatusBackground(status);
                }

                // FIXED: Show/hide unread indicator (show only when notification is unread)
                if (unreadIndicator != null) {
                    // Show indicator only for UNREAD notifications
                    unreadIndicator.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);
                }

                // Update item background for read/unread
                updateItemBackground(notification.isRead());

            } catch (Exception e) {
                // Handle any binding errors gracefully
                if (tvFarmerName != null) {
                    tvFarmerName.setText("Error loading notification");
                }
                if (tvMessage != null) {
                    tvMessage.setText("Error loading message");
                }
            }
        }

        private void updateStatusBackground(String status) {
            if (tvStatus == null || status == null) {
                return;
            }

            try {
                String statusLower = status.trim().toLowerCase();
                switch (statusLower) {
                    case "approved":
                        tvStatus.setBackgroundResource(R.drawable.status_approved_background);
                        break;
                    case "rejected":
                        tvStatus.setBackgroundResource(R.drawable.status_rejected_background);
                        break;
                    case "pending":
                    default:
                        tvStatus.setBackgroundResource(R.drawable.status_pending_background);
                        break;
                }
            } catch (Exception e) {
                // If background resources don't exist, just skip
            }
        }

        private void updateItemBackground(boolean isRead) {
            try {
                if (isRead) {
                    itemView.setAlpha(0.7f);
                } else {
                    itemView.setAlpha(1.0f);
                }
            } catch (Exception e) {
                // Handle any errors gracefully
            }
        }
    }
}
