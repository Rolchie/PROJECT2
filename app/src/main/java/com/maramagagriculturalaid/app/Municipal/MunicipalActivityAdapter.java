package com.maramagagriculturalaid.app.Municipal;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.maramagagriculturalaid.app.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MunicipalActivityAdapter extends RecyclerView.Adapter<MunicipalActivityAdapter.ViewHolder> {

    private List<MunicipalHomeFragment.ActivityItem> items;
    private Context context;

    public MunicipalActivityAdapter(List<MunicipalHomeFragment.ActivityItem> items, Context context) {
        this.items = items;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_municipal_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MunicipalHomeFragment.ActivityItem item = items.get(position);

        if (holder.titleTextView != null) {
            holder.titleTextView.setText(item.getTitle() != null ? item.getTitle() : "No title");
        }
        if (holder.descriptionTextView != null) {
            holder.descriptionTextView.setText(item.getDescription() != null ? item.getDescription() : "No description");
        }
        if (holder.timeTextView != null) {
            // Format the timestamp string for display
            String timestamp = item.getTimestamp();
            if (timestamp != null && !timestamp.isEmpty()) {
                holder.timeTextView.setText(formatTimestamp(timestamp));
            } else {
                holder.timeTextView.setText("Unknown time");
            }
        }

        // Set icon based on type
        if (holder.iconView != null) {
            String iconType = item.getIconType();
            if (iconType != null) {
                switch (iconType.toLowerCase()) {
                    case "edit":
                        holder.iconView.setImageResource(R.drawable.baseline_edit_note_24);
                        // Reset color filter
                        holder.iconView.clearColorFilter();
                        break;
                    case "approve":
                    case "approved":
                        holder.iconView.setImageResource(R.drawable.baseline_check_circle_24);
                        if (context != null) {
                            holder.iconView.setColorFilter(context.getColor(R.color.ButtonGreen));
                        }
                        break;
                    case "reject":
                    case "rejected":
                        holder.iconView.setImageResource(R.drawable.baseline_clear_24);
                        if (context != null) {
                            holder.iconView.setColorFilter(context.getColor(R.color.red));
                        }
                        break;
                    case "add":
                    case "create":
                        holder.iconView.setImageResource(R.drawable.baseline_add_24);
                        // Reset color filter
                        holder.iconView.clearColorFilter();
                        break;
                    case "update":
                        holder.iconView.setImageResource(R.drawable.baseline_edit_note_24);
                        if (context != null) {
                            holder.iconView.setColorFilter(context.getColor(R.color.ButtonGreen));
                        }
                        break;
                    case "delete":
                        holder.iconView.setImageResource(R.drawable.baseline_clear_24);
                        if (context != null) {
                            holder.iconView.setColorFilter(context.getColor(R.color.red));
                        }
                        break;
                    default:
                        holder.iconView.setImageResource(R.drawable.baseline_edit_note_24);
                        // Reset color filter
                        holder.iconView.clearColorFilter();
                        break;
                }
            } else {
                // Default icon if no type specified
                holder.iconView.setImageResource(R.drawable.baseline_edit_note_24);
                holder.iconView.clearColorFilter();
            }
        }

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (context != null) {
                String title = item.getTitle();
                Toast.makeText(context, title != null ? title : "Activity", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    /**
     * Format timestamp string for display
     * This method handles the timestamp formatting since ActivityItem now uses String
     */
    private String formatTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return "Unknown time";
        }

        try {
            // If the timestamp is already formatted, return as is
            if (timestamp.contains("ago") || timestamp.contains(",")) {
                return timestamp;
            }

            // Try to parse as a date string and format it
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

            try {
                Date date = inputFormat.parse(timestamp);
                if (date != null) {
                    // Calculate time difference
                    long diffInMillis = System.currentTimeMillis() - date.getTime();
                    long diffInMinutes = diffInMillis / (60 * 1000);
                    long diffInHours = diffInMillis / (60 * 60 * 1000);
                    long diffInDays = diffInMillis / (24 * 60 * 60 * 1000);

                    if (diffInMinutes < 60) {
                        return diffInMinutes + "m ago";
                    } else if (diffInHours < 24) {
                        return diffInHours + "h ago";
                    } else if (diffInDays < 7) {
                        return diffInDays + "d ago";
                    } else {
                        return outputFormat.format(date);
                    }
                }
            } catch (Exception e) {
                // If parsing fails, return the original timestamp
                return timestamp;
            }
        } catch (Exception e) {
            // If any error occurs, return the original timestamp
            return timestamp;
        }

        return timestamp;
    }

    /**
     * Update the items list and refresh the adapter
     */
    public void updateItems(List<MunicipalHomeFragment.ActivityItem> newItems) {
        if (newItems != null) {
            this.items = newItems;
            notifyDataSetChanged();
        }
    }

    /**
     * Add a single item to the list
     */
    public void addItem(MunicipalHomeFragment.ActivityItem item) {
        if (item != null && items != null) {
            items.add(0, item); // Add to the beginning of the list
            notifyItemInserted(0);
        }
    }

    /**
     * Clear all items
     */
    public void clearItems() {
        if (items != null) {
            int size = items.size();
            items.clear();
            notifyItemRangeRemoved(0, size);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView iconView;
        TextView titleTextView;
        TextView descriptionTextView;
        TextView timeTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.activityIcon);
            titleTextView = itemView.findViewById(R.id.activityTitle);
            descriptionTextView = itemView.findViewById(R.id.activityDescription);
            timeTextView = itemView.findViewById(R.id.activityTime);
        }
    }
}