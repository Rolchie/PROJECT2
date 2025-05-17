package com.maramagagriculturalaid.app.FarmersData;

import com.google.firebase.firestore.DocumentId;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Farmer implements Serializable {
    @DocumentId
    private String documentId;

    private String id;
    private String firstName;
    private String lastName;
    private String middleInitial;
    private String phoneNumber;
    private Date birthday;
    private String address;
    private double annualIncome;

    // Farm information
    private String farmType; // "Crop", "Livestock", "Mixed"
    private String location;
    private String barangay;
    private String barangayId;
    private String exactLocation;
    private String cropsGrown;
    private double lotSize;
    private String livestock;
    private int livestockCount;

    public Farmer() {
        // Required empty constructor for Firestore
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getMiddleInitial() {
        return middleInitial;
    }

    public void setMiddleInitial(String middleInitial) {
        this.middleInitial = middleInitial;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public String getBarangay() {
        return barangay;
    }

    public void setBarangay(String barangay) {
        this.barangay = barangay;
    }

    public String getBarangayId() {
        return barangayId;
    }

    public void setBarangayId(String barangayId) {
        this.barangayId = barangayId;
    }

    public String getExactLocation() {
        return exactLocation;
    }

    public void setExactLocation(String exactLocation) {
        this.exactLocation = exactLocation;
    }

    public String getCropsGrown() {
        return cropsGrown;
    }

    public void setCropsGrown(String cropsGrown) {
        this.cropsGrown = cropsGrown;
    }

    public double getLotSize() {
        return lotSize;
    }

    public void setLotSize(double lotSize) {
        this.lotSize = lotSize;
    }

    public String getLivestock() {
        return livestock;
    }

    public void setLivestock(String livestock) {
        this.livestock = livestock;
    }

    public int getLivestockCount() {
        return livestockCount;
    }

    public void setLivestockCount(int livestockCount) {
        this.livestockCount = livestockCount;
    }

    // Add these to your Farmer class
    private String dateAdded; // Add this field

    // Add these getter and setter methods
    public String getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(String dateAdded) {
        this.dateAdded = dateAdded;
    }

    // Add this method if you need to set the barangay name
    public void setBarangayName(String barangayName) {
        this.barangay = barangayName;
    }

    // Add this method if you need to get the barangay name
    public String getBarangayName() {
        return barangay;
    }

    // Helper method to get full name
    public String getFullName() {
        if (middleInitial != null && !middleInitial.isEmpty()) {
            return lastName + ", " + firstName + " " + middleInitial + ".";
        } else {
            return lastName + ", " + firstName;
        }
    }

    // Convert Farmer object to Firestore-compatible Map
    public static Farmer fromMap(Map<String, Object> map) {
        Farmer farmer = new Farmer();
        farmer.setId((String) map.get("id"));
        farmer.setFirstName((String) map.get("firstName"));
        farmer.setLastName((String) map.get("lastName"));
        farmer.setMiddleInitial((String) map.get("middleInitial"));
        farmer.setPhoneNumber((String) map.get("phoneNumber"));
        farmer.setBirthday((Date) map.get("birthday")); // Ensure casting is safe
        farmer.setAddress((String) map.get("address"));
        farmer.setFarmType((String) map.get("farmType"));
        farmer.setLocation((String) map.get("location"));
        farmer.setBarangay((String) map.get("barangay"));
        farmer.setBarangayId((String) map.get("barangayId"));
        farmer.setExactLocation((String) map.get("exactLocation"));
        farmer.setCropsGrown((String) map.get("cropsGrown"));
        farmer.setLotSize((double) map.get("lotSize")); // Might need casting from Long or Double
        farmer.setLivestock((String) map.get("livestock"));
        farmer.setLivestockCount(((Number) map.get("livestockCount")).intValue()); // Safe casting
        farmer.setDateAdded((String) map.get("dateAdded"));
        return farmer;
    }

}
