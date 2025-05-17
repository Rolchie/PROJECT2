package com.maramagagriculturalaid.app.FarmersData;

public class ActivityItem {
    private String type; // "farmer_added", "farmer_edited", "farmer_removed", "subsidy_added"
    private String title; // "Added farmer", "Edited farmer", etc.
    private String description; // Details about the activity
    private String farmerName;
    private String cropsGrown;
    private long timestamp;

    public ActivityItem() {
        // Default constructor
    }

    public ActivityItem(String type, String title, String description, String farmerName, String cropsGrown, long timestamp) {
        this.type = type;
        this.title = title;
        this.description = description;
        this.farmerName = farmerName;
        this.cropsGrown = cropsGrown;
        this.timestamp = timestamp;
    }

    // Getters
    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getFarmerName() {
        return farmerName;
    }

    public String getCropsGrown() {
        return cropsGrown;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // Setters
    public void setType(String type) {
        this.type = type;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setFarmerName(String farmerName) {
        this.farmerName = farmerName;
    }

    public void setCropsGrown(String cropsGrown) {
        this.cropsGrown = cropsGrown;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
