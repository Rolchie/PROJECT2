package com.maramagagriculturalaid.app.Municipal.MunicipalNotifications;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.maramagagriculturalaid.app.Notification.Notification;
import com.maramagagriculturalaid.app.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MunicipalNotificationAdapter extends RecyclerView.Adapter<MunicipalNotificationAdapter.NotificationViewHolder> {

    private Context context;
    private List<Notification> notifications;
    private OnNotificationClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification, int position);
    }

    public MunicipalNotificationAdapter(Context context, List<Notification> notifications) {
        this.context = context;
        this.notifications = notifications;
    }

    public void setOnNotificationClickListener(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_municipal_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);

        // Set title - use "New Subsidy Application" for subsidy notifications
        String title = notification.getTitle();
        if (title == null || title.isEmpty()) {
            if ("subsidy_application".equals(notification.getType()) || notification.getSubsidyId() != null) {
                title = "New Subsidy Application";
            } else {
                title = "Notification";
            }
        }
        holder.tvTitle.setText(title);

        // Set message - use "From: BarangayName" format
        String message = notification.getMessage();
        if (message == null || message.isEmpty()) {
            String barangay = notification.getBarangay();
            if (barangay != null && !barangay.isEmpty()) {
                message = "From: " + barangay;
            } else {
                message = "New notification";
            }
        }
        holder.tvMessage.setText(message);

        // Format timestamp
        if (notification.getTimestamp() != null) {
            Date date = notification.getTimestamp();
            holder.tvTimestamp.setText(dateFormat.format(date));
        } else {
            holder.tvTimestamp.setText("No date");
        }

        // Show/hide unread indicator and set card appearance
        if (notification.isRead()) {
            holder.unreadIndicator.setVisibility(View.GONE);
            holder.cardView.setAlpha(0.7f); // Slightly fade read notifications
        } else {
            holder.unreadIndicator.setVisibility(View.VISIBLE);
            holder.cardView.setAlpha(1.0f);
        }

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(notification, position);
            } else {
                // Default behavior: open notification detail activity
                openNotificationDetail(notification);
            }
        });
    }

    private void openNotificationDetail(Notification notification) {
        Intent intent = new Intent(context, MunicipalNotificationDetail.class);
        intent.putExtra("notificationId", notification.getId());
        intent.putExtra("barangay", notification.getBarangay());

        // Pass the notification data
        intent.putExtra("farmerId", notification.getFarmerId());
        intent.putExtra("firstName", notification.getFirstName());
        intent.putExtra("lastName", notification.getLastName());
        intent.putExtra("middleInitial", notification.getMiddleInitial());
        intent.putExtra("status", notification.getStatus());
        intent.putExtra("subsidyId", notification.getSubsidyId());
        intent.putExtra("farmType", notification.getFarmType());
        intent.putExtra("read", notification.isRead());

        if (notification.getTimestamp() != null) {
            intent.putExtra("timestamp", notification.getTimestamp().getTime());
        }

        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return notifications != null ? notifications.size() : 0;
    }

    public void updateNotifications(List<Notification> newNotifications) {
        this.notifications = newNotifications;
        notifyDataSetChanged();
    }

    public void markAsRead(int position) {
        if (position >= 0 && position < notifications.size()) {
            notifications.get(position).setRead(true);
            notifyItemChanged(position);
        }
    }

    public void addNotification(Notification notification) {
        if (notifications != null) {
            notifications.add(0, notification); // Add to top
            notifyItemInserted(0);
        }
    }

    public void removeNotification(int position) {
        if (position >= 0 && position < notifications.size()) {
            notifications.remove(position);
            notifyItemRemoved(position);
        }
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvTitle, tvMessage, tvTimestamp;
        View unreadIndicator;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_view);
            tvTitle = itemView.findViewById(R.id.tv_notification_title);
            tvMessage = itemView.findViewById(R.id.tv_message);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            unreadIndicator = itemView.findViewById(R.id.unread_indicator);
        }
    }
}