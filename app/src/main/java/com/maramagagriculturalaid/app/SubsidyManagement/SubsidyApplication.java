package com.maramagagriculturalaid.app.SubsidyManagement;

public class SubsidyApplication {
    private String farmerId;
    private String farmerName;
    private String supportType;
    private String farmType;
    private String location;
    private String crops;
    private String lotSize;
    private String livestock;
    private String livestockCount;
    private long timestamp;
    private String status;
    private String submissionDate;

    // Required empty constructor for Firestore
    public SubsidyApplication() {
    }

    public SubsidyApplication(String farmerId, String farmerName, String supportType,
                              String farmType, String location, String crops,
                              String lotSize, String livestock, String livestockCount,
                              long timestamp, String status) {
        this.farmerId = farmerId;
        this.farmerName = farmerName;
        this.supportType = supportType;
        this.farmType = farmType;
        this.location = location;
        this.crops = crops;
        this.lotSize = lotSize;
        this.livestock = livestock;
        this.livestockCount = livestockCount;
        this.timestamp = timestamp;
        this.status = status;
    }

    // Getters and setters
    public String getFarmerId() {
        return farmerId;
    }

    public void setFarmerId(String farmerId) {
        this.farmerId = farmerId;
    }

    public String getFarmerName() {
        return farmerName;
    }

    public void setFarmerName(String farmerName) {
        this.farmerName = farmerName;
    }

    public String getSupportType() {
        return supportType;
    }

    public void setSupportType(String supportType) {
        this.supportType = supportType;
    }

    public String getFarmType() {
        return farmType;
    }

    public void setFarmType(String farmType) {
        this.farmType = farmType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCrops() {
        return crops;
    }

    public void setCrops(String crops) {
        this.crops = crops;
    }

    public String getLotSize() {
        return lotSize;
    }

    public String getSubmissionDate() { return submissionDate; }
    public void setSubmissionDate(String submissionDate) { this.submissionDate = submissionDate; }

    public void setLotSize(String lotSize) {
        this.lotSize = lotSize;
    }

    public String getLivestock() {
        return livestock;
    }

    public void setLivestock(String livestock) {
        this.livestock = livestock;
    }

    public String getLivestockCount() {
        return livestockCount;
    }

    public void setLivestockCount(String livestockCount) {
        this.livestockCount = livestockCount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
