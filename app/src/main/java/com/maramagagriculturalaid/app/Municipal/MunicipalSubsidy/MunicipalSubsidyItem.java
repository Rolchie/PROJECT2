package com.maramagagriculturalaid.app.Municipal.MunicipalSubsidy;

public class MunicipalSubsidyItem {
    private String id;
    private String farmerName;
    private String farmerId;
    private String barangay;
    private String subsidyType;
    private String status;
    private String dateApplied;
    private String dateApproved;
    private Object timestamp;

    // Default constructor (required for Firebase)
    public MunicipalSubsidyItem() {
    }

    // Constructor with essential fields
    public MunicipalSubsidyItem(String id, String farmerName, String subsidyType, String status) {
        this.id = id;
        this.farmerName = farmerName;
        this.subsidyType = subsidyType;
        this.status = status;
    }

    // Full constructor
    public MunicipalSubsidyItem(String id, String farmerName, String farmerId, String barangay,
                                String subsidyType, String status, String dateApplied, String dateApproved) {
        this.id = id;
        this.farmerName = farmerName;
        this.farmerId = farmerId;
        this.barangay = barangay;
        this.subsidyType = subsidyType;
        this.status = status;
        this.dateApplied = dateApplied;
        this.dateApproved = dateApproved;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFarmerName() {
        return farmerName;
    }

    public void setFarmerName(String farmerName) {
        this.farmerName = farmerName;
    }

    public String getFarmerId() {
        return farmerId;
    }

    public void setFarmerId(String farmerId) {
        this.farmerId = farmerId;
    }

    public String getBarangay() {
        return barangay;
    }

    public void setBarangay(String barangay) {
        this.barangay = barangay;
    }

    public String getSubsidyType() {
        return subsidyType;
    }

    public void setSubsidyType(String subsidyType) {
        this.subsidyType = subsidyType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDateApplied() {
        return dateApplied;
    }

    public void setDateApplied(String dateApplied) {
        this.dateApplied = dateApplied;
    }

    public String getDateApproved() {
        return dateApproved;
    }

    public void setDateApproved(String dateApproved) {
        this.dateApproved = dateApproved;
    }

    public Object getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Object timestamp) {
        this.timestamp = timestamp;
    }

    // Utility methods
    public boolean isPending() {
        return "Pending".equalsIgnoreCase(status);
    }

    public boolean isApproved() {
        return "Approved".equalsIgnoreCase(status);
    }

    public boolean isRejected() {
        return "Rejected".equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return "MunicipalSubsidyItem{" +
                "id='" + id + '\'' +
                ", farmerName='" + farmerName + '\'' +
                ", farmerId='" + farmerId + '\'' +
                ", barangay='" + barangay + '\'' +
                ", subsidyType='" + subsidyType + '\'' +
                ", status='" + status + '\'' +
                ", dateApplied='" + dateApplied + '\'' +
                ", dateApproved='" + dateApproved + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        MunicipalSubsidyItem that = (MunicipalSubsidyItem) obj;
        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}