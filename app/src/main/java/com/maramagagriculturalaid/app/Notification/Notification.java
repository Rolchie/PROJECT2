package com.maramagagriculturalaid.app.Notification;

import java.util.Date;

public class Notification {
    private String id;
    String farmerId;
    public String FirstName;
    public String LastName;
    public String MiddleInitial;
    String subsidyId;
    String status; // "Approved" or "Rejected"
    Date timestamp;
    private boolean isRead;

    private String barangay;
    private String farmType; // "Crops", "Livestock", or "Mixed"
    private String crops;
    private String livestock;


    // Empty constructor for Firestore
    public Notification() {}

    public Notification(String farmerId, String FirstName, String Lastname, String MiddleInitial,  String subsidyId, String status) {
        this.farmerId = farmerId;
        this.FirstName = FirstName;
        this.LastName = Lastname;
        this.MiddleInitial = MiddleInitial;
        this.subsidyId = subsidyId;
        this.status = status;
        this.timestamp = new Date();
        this.isRead = false;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFarmerId() { return farmerId; }

    public String getFirstName() {return FirstName;}
    public String getLastName() {return LastName;}
    public String getMiddleInitial() {return MiddleInitial;}
    public String getFullName() { return getLastName() + ", " + getFirstName() + " " + getMiddleInitial() + "."; }
    public String getSubsidyId() { return subsidyId; }
    public String getStatus() { return status; }
    public Date getTimestamp() { return timestamp; }
    public boolean isRead() { return isRead; }

    public String getBarangay() {
        return barangay != null ? barangay : "Unknown Barangay";
    }

    public String getFarmType() {
        return farmType != null ? farmType : "Not Specified";
    }

    public String getCropsList() {
        return crops != null ? crops : "No crops specified";
    }

    public String getLivestockList() {
        return livestock != null ? livestock : "No livestock specified";
    }

    // Setters
    public void setRead(boolean read) { isRead = read; }
    public void setBarangay(String barangay) { this.barangay = barangay; }
    public void setFarmType(String farmType) { this.farmType = farmType; }
    public void setCrops(String crops) { this.crops = crops; }
    public void setLivestock(String livestock) { this.livestock = livestock; }

    public String getFormattedMessage() {
        return String.format("The subsidy application of %s (%s) has been %s",
                getFullName(), getFarmerId(), getStatus().toLowerCase());
    }
}
