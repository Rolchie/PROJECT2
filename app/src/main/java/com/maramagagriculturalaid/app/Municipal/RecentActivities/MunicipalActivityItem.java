package com.maramagagriculturalaid.app.Municipal.RecentActivities;

import com.google.firebase.Timestamp;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * MunicipalActivityItem - Model class representing a municipal subsidy decision activity
 * Used specifically for displaying subsidy approval/rejection activities
 */
public class MunicipalActivityItem {

    // Activity identification
    private String id;
    private String type; // "subsidy_approved" or "subsidy_rejected"
    private String title; // "Approved an Application" or "Rejected an Application"
    private String description;

    // Subsidy-specific information
    private String barangay;
    private String farmerName;
    private String subsidyType;
    private String applicationId;
    private String status; // "approved" or "rejected"
    private String rejectionReason; // Only for rejected applications

    // Activity metadata
    private Timestamp timestamp;
    private String icon;
    private String priority;

    // Display properties
    private boolean isRead;
    private boolean isExpanded;
    private String formattedTime;
    private String relativeTime;

    // Default constructor required for Firestore
    public MunicipalActivityItem() {
    }

    // Constructor for approved subsidy
    public static MunicipalActivityItem createApprovedActivity(String barangay, String farmerName,
                                                               String subsidyType, String applicationId) {
        MunicipalActivityItem item = new MunicipalActivityItem();
        item.type = "subsidy_approved";
        item.title = "Approved an Application";
        item.description = "Approved " + farmerName + "'s application" +
                (subsidyType != null ? " for " + subsidyType : "");
        item.barangay = barangay;
        item.farmerName = farmerName;
        item.subsidyType = subsidyType;
        item.applicationId = applicationId;
        item.status = "approved";
        item.timestamp = Timestamp.now();
        item.icon = "check_circle";
        item.priority = "high";
        item.isRead = false;
        item.isExpanded = false;
        item.updateFormattedTime();
        return item;
    }

    // Constructor for rejected subsidy
    public static MunicipalActivityItem createRejectedActivity(String barangay, String farmerName,
                                                               String subsidyType, String applicationId,
                                                               String rejectionReason) {
        MunicipalActivityItem item = new MunicipalActivityItem();
        item.type = "subsidy_rejected";
        item.title = "Rejected an Application";
        item.description = "Rejected " + farmerName + "'s application" +
                (subsidyType != null ? " for " + subsidyType : "") +
                (rejectionReason != null ? " - Reason: " + rejectionReason : "");
        item.barangay = barangay;
        item.farmerName = farmerName;
        item.subsidyType = subsidyType;
        item.applicationId = applicationId;
        item.rejectionReason = rejectionReason;
        item.status = "rejected";
        item.timestamp = Timestamp.now();
        item.icon = "cancel";
        item.priority = "high";
        item.isRead = false;
        item.isExpanded = false;
        item.updateFormattedTime();
        return item;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBarangay() {
        return barangay;
    }

    public void setBarangay(String barangay) {
        this.barangay = barangay;
    }

    public String getFarmerName() {
        return farmerName;
    }

    public void setFarmerName(String farmerName) {
        this.farmerName = farmerName;
    }

    public String getSubsidyType() {
        return subsidyType;
    }

    public void setSubsidyType(String subsidyType) {
        this.subsidyType = subsidyType;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
        updateFormattedTime();
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    public String getFormattedTime() {
        return formattedTime;
    }

    public String getRelativeTime() {
        return relativeTime;
    }

    // Utility methods
    private void updateFormattedTime() {
        if (timestamp != null) {
            Date date = timestamp.toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
            this.formattedTime = sdf.format(date);
            this.relativeTime = getRelativeTimeString(date);
        }
    }

    /**
     * Get relative time string (e.g., "2 hours ago", "Yesterday", "3 days ago")
     */
    private String getRelativeTimeString(Date date) {
        if (date == null) return "Unknown time";

        long now = System.currentTimeMillis();
        long activityTime = date.getTime();
        long diff = now - activityTime;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        } else if (hours < 24) {
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        } else if (days == 1) {
            return "Yesterday";
        } else if (days < 7) {
            return days + " days ago";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
            return sdf.format(date);
        }
    }

    /**
     * Check if this is an approval activity
     */
    public boolean isApproval() {
        return "subsidy_approved".equals(type) || "approved".equals(status);
    }

    /**
     * Check if this is a rejection activity
     */
    public boolean isRejection() {
        return "subsidy_rejected".equals(type) || "rejected".equals(status);
    }

    /**
     * Get status color for UI display
     */
    public String getStatusColor() {
        if (isApproval()) {
            return "#4CAF50"; // Green for approved
        } else if (isRejection()) {
            return "#F44336"; // Red for rejected
        } else {
            return "#2196F3"; // Blue for default
        }
    }

    /**
     * Get status text for display
     */
    public String getStatusText() {
        if (isApproval()) {
            return "APPROVED";
        } else if (isRejection()) {
            return "REJECTED";
        } else {
            return "UNKNOWN";
        }
    }

    /**
     * Get appropriate icon resource name
     */
    public String getIconResource() {
        if (icon != null && !icon.isEmpty()) {
            return icon;
        }

        if (isApproval()) {
            return "check_circle";
        } else if (isRejection()) {
            return "cancel";
        } else {
            return "info";
        }
    }

    /**
     * Check if this activity is recent (within last 24 hours)
     */
    public boolean isRecent() {
        if (timestamp == null) return false;

        long now = System.currentTimeMillis();
        long activityTime = timestamp.toDate().getTime();
        long diff = now - activityTime;
        long hours = diff / (1000 * 60 * 60);

        return hours <= 24;
    }

    /**
     * Get a short description for list display
     */
    public String getShortDescription() {
        if (description == null || description.isEmpty()) {
            return title != null ? title : "No description";
        }

        if (description.length() <= 80) {
            return description;
        }

        return description.substring(0, 77) + "...";
    }

    /**
     * Check if activity involves a specific farmer
     */
    public boolean involvesFarmer(String farmerName) {
        return this.farmerName != null && this.farmerName.equalsIgnoreCase(farmerName);
    }

    /**
     * Check if activity is in a specific barangay
     */
    public boolean isInBarangay(String barangay) {
        return this.barangay != null && this.barangay.equalsIgnoreCase(barangay);
    }

    /**
     * Check if activity matches a search query
     */
    public boolean matchesSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }

        String lowerQuery = query.toLowerCase().trim();

        return (title != null && title.toLowerCase().contains(lowerQuery)) ||
                (description != null && description.toLowerCase().contains(lowerQuery)) ||
                (farmerName != null && farmerName.toLowerCase().contains(lowerQuery)) ||
                (subsidyType != null && subsidyType.toLowerCase().contains(lowerQuery)) ||
                (status != null && status.toLowerCase().contains(lowerQuery));
    }

    @Override
    public String toString() {
        return "MunicipalActivityItem{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", title='" + title + '\'' +
                ", farmerName='" + farmerName + '\'' +
                ", subsidyType='" + subsidyType + '\'' +
                ", status='" + status + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        MunicipalActivityItem that = (MunicipalActivityItem) obj;

        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
