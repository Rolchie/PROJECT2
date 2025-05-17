package com.maramagagriculturalaid.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.maramagagriculturalaid.app.FarmersData.ActivityItem;

import java.util.List;

public class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.ViewHolder> {
    private List<ActivityItem> activities;

    public RecentActivityAdapter(List<ActivityItem> activities) {
        this.activities = activities;
    }

    public void updateActivities(List<ActivityItem> newActivities) {
        this.activities = newActivities;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (activities.isEmpty()) {
            holder.activityIcon.setImageResource(R.drawable.baseline_info_24);
            holder.activityTitle.setText("No recent activities");
            holder.activityDescription.setText("Activities will appear here when you add farmers or subsidy applications");
            holder.activityTime.setText("");
            return;
        }

        ActivityItem activity = activities.get(position);

        // Set icon based on activity type
        setActivityIcon(holder.activityIcon, activity.getType());

        // Set title and description
        holder.activityTitle.setText(activity.getTitle());
        holder.activityDescription.setText(activity.getDescription());

        // Set relative time
        holder.activityTime.setText(getRelativeTime(activity.getTimestamp()));
    }

    private void setActivityIcon(ImageView iconView, String activityType) {
        if (activityType == null) {
            iconView.setImageResource(R.drawable.baseline_info_24);
            return;
        }

        switch (activityType) {
            case "farmer_added":
                iconView.setImageResource(R.drawable.baseline_add_24); // Green add person icon
                break;
            case "farmer_edited":
                iconView.setImageResource(R.drawable.baseline_edit_note_24); // Blue edit icon
                break;
            case "farmer_removed":
                iconView.setImageResource(R.drawable.baseline_clear_24); // Red remove person icon
                break;
            case "subsidy_added":
                iconView.setImageResource(R.drawable.baseline_add_24); // Green agriculture/plant icon
                break;
            default:
                iconView.setImageResource(R.drawable.baseline_info_24);
                break;
        }
    }

    private String getRelativeTime(long timestamp) {
        if (timestamp <= 0) {
            return "Unknown time";
        }

        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - timestamp;

        // Convert to seconds
        long seconds = timeDiff / 1000;

        if (seconds < 60) {
            return "Just now";
        }

        // Convert to minutes
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "m ago";
        }

        // Convert to hours
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "h ago";
        }

        // Convert to days
        long days = hours / 24;
        if (days < 7) {
            return days + "d ago";
        }

        // Convert to weeks
        long weeks = days / 7;
        if (weeks < 4) {
            return weeks + "w ago";
        }

        // Convert to months
        long months = days / 30;
        if (months < 12) {
            return months + "mo ago";
        }

        // Convert to years
        long years = days / 365;
        return years + "y ago";
    }

    @Override
    public int getItemCount() {
        // Show exactly 3 items or 1 for empty state
        if (activities.isEmpty()) {
            return 1; // Show empty state message
        }
        return Math.min(3, activities.size()); // Show maximum 3 activities
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView activityIcon;
        TextView activityTitle, activityDescription, activityTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            activityIcon = itemView.findViewById(R.id.activity_icon);
            activityTitle = itemView.findViewById(R.id.activity_title);
            activityDescription = itemView.findViewById(R.id.activity_description);
            activityTime = itemView.findViewById(R.id.activity_time);
        }
    }
}