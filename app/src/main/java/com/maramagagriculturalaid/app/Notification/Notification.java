package com.maramagagriculturalaid.app.Notification;

import java.io.Serializable;
import java.util.Date;

public class Notification implements Serializable {
    // Existing fields - keeping all of them
    private String id;
    private String farmerId;
    private String firstName;
    private String lastName;
    private String middleInitial;
    private String fullName;
    private String barangay;
    private String farmType;
    private String status;
    private Date timestamp;
    private boolean read;
    private String crops;
    private String livestock;

    // NEW fields being added - not removing anything
    private String type; // "subsidy_request", "farmer_registration", etc.
    private String subsidyId; // Link to subsidy request
    private String message; // Custom message content
    private String title; // Notification title
    private String priority; // "high", "medium", "low"
    private String category; // Additional categorization
    private String sourceCollection; // Which collection this notification came from
    private String sourceDocument; // Document ID from source collection
    private String actionRequired; // What action is needed
    private String assignedTo; // Who should handle this notification
    private Date dueDate; // When action should be completed
    private boolean archived; // For archiving old notifications
    private String notes; // Additional notes or comments

    // Default constructor required for Firestore
    public Notification() {}

    // Constructor with existing fields
    public Notification(String farmerId, String firstName, String lastName,
                        String middleInitial, String barangay, String status) {
        this.farmerId = farmerId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.middleInitial = middleInitial;
        this.barangay = barangay;
        this.status = status;
        this.timestamp = new Date();
        this.read = false;
        this.fullName = buildFullName();
    }

    // New constructor with additional fields
    public Notification(String type, String farmerId, String firstName, String lastName,
                        String middleInitial, String barangay, String status, String subsidyId) {
        this.type = type;
        this.farmerId = farmerId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.middleInitial = middleInitial;
        this.barangay = barangay;
        this.status = status;
        this.subsidyId = subsidyId;
        this.timestamp = new Date();
        this.read = false;
        this.archived = false;
        this.fullName = buildFullName();
    }

    // ALL EXISTING GETTERS AND SETTERS - keeping everything
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFarmerId() {
        return farmerId;
    }

    public void setFarmerId(String farmerId) {
        this.farmerId = farmerId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
        this.fullName = buildFullName();
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
        this.fullName = buildFullName();
    }

    public String getMiddleInitial() {
        return middleInitial;
    }

    public void setMiddleInitial(String middleInitial) {
        this.middleInitial = middleInitial;
        this.fullName = buildFullName();
    }

    public String getFullName() {
        if (fullName == null || fullName.isEmpty()) {
            fullName = buildFullName();
        }
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getBarangay() {
        return barangay;
    }

    public void setBarangay(String barangay) {
        this.barangay = barangay;
    }

    public String getFarmType() {
        return farmType;
    }

    public void setFarmType(String farmType) {
        this.farmType = farmType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getCrops() {
        return crops;
    }

    public void setCrops(String crops) {
        this.crops = crops;
    }

    public String getLivestock() {
        return livestock;
    }

    public void setLivestock(String livestock) {
        this.livestock = livestock;
    }

    // EXISTING METHOD - keeping this exactly as it was
    public String getFormattedMessage() {
        String fullName = getFullName();
        String barangayName = getBarangay();
        String status = getStatus();
        String type = getType();

        StringBuilder message = new StringBuilder();
        message.append("From: ").append(barangayName != null ? barangayName : "Unknown").append(" - ");

        if ("subsidy_request".equals(type)) {
            message.append("Subsidy application from ").append(fullName != null ? fullName : "Unknown farmer");

            if (farmType != null && !farmType.isEmpty()) {
                message.append(" (").append(farmType).append(" farming)");
            }

            // Add status context for approved/rejected applications
            if ("Approved".equalsIgnoreCase(status)) {
                message.append(" - Ready for claim");
            } else if ("Rejected".equalsIgnoreCase(status)) {
                message.append(" - Requires review");
            }
        } else if ("Added".equalsIgnoreCase(status)) {
            message.append("New farmer ").append(fullName != null ? fullName : "Unknown").append(" registered");
        } else if ("Updated".equalsIgnoreCase(status)) {
            message.append("Farmer ").append(fullName != null ? fullName : "Unknown").append(" information updated");
        } else {
            message.append("Update for ").append(fullName != null ? fullName : "Unknown");
        }

        return message.toString();
    }

    // NEW GETTERS AND SETTERS for added fields
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSubsidyId() {
        return subsidyId;
    }

    public void setSubsidyId(String subsidyId) {
        this.subsidyId = subsidyId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSourceCollection() {
        return sourceCollection;
    }

    public void setSourceCollection(String sourceCollection) {
        this.sourceCollection = sourceCollection;
    }

    public String getSourceDocument() {
        return sourceDocument;
    }

    public void setSourceDocument(String sourceDocument) {
        this.sourceDocument = sourceDocument;
    }

    public String getActionRequired() {
        return actionRequired;
    }

    public void setActionRequired(String actionRequired) {
        this.actionRequired = actionRequired;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // Helper method to build full name - keeping existing logic
    private String buildFullName() {
        StringBuilder fullName = new StringBuilder();

        if (firstName != null && !firstName.trim().isEmpty()) {
            fullName.append(firstName.trim());
        }

        if (middleInitial != null && !middleInitial.trim().isEmpty()) {
            if (fullName.length() > 0) fullName.append(" ");
            fullName.append(middleInitial.trim());
            if (!middleInitial.endsWith(".")) {
                fullName.append(".");
            }
        }

        if (lastName != null && !lastName.trim().isEmpty()) {
            if (fullName.length() > 0) fullName.append(" ");
            fullName.append(lastName.trim());
        }

        return fullName.toString();
    }

    // Helper methods for new functionality
    public boolean isPending() {
        return "pending".equalsIgnoreCase(status);
    }

    public boolean isApproved() {
        return "approved".equalsIgnoreCase(status);
    }

    public boolean isRejected() {
        return "rejected".equalsIgnoreCase(status);
    }

    public boolean isSubsidyRequest() {
        return "subsidy_request".equals(type);
    }

    public boolean isFarmerRegistration() {
        return "farmer_registration".equals(type);
    }

    public boolean isHighPriority() {
        return "high".equalsIgnoreCase(priority);
    }

    public boolean requiresAction() {
        return actionRequired != null && !actionRequired.trim().isEmpty();
    }

    public boolean isOverdue() {
        if (dueDate == null) return false;
        return new Date().after(dueDate) && !isRead();
    }

    // Enhanced toString method including new fields
    @Override
    public String toString() {
        return "Notification{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", subsidyId='" + subsidyId + '\'' +
                ", farmerId='" + farmerId + '\'' +
                ", fullName='" + getFullName() + '\'' +
                ", barangay='" + barangay + '\'' +
                ", farmType='" + farmType + '\'' +
                ", status='" + status + '\'' +
                ", timestamp=" + timestamp +
                ", read=" + read +
                ", priority='" + priority + '\'' +
                ", actionRequired='" + actionRequired + '\'' +
                ", archived=" + archived +
                '}';
    }

    // Method to create a copy of notification (useful for updates)
    public Notification copy() {
        Notification copy = new Notification();
        copy.setId(this.id);
        copy.setType(this.type);
        copy.setSubsidyId(this.subsidyId);
        copy.setFarmerId(this.farmerId);
        copy.setFirstName(this.firstName);
        copy.setLastName(this.lastName);
        copy.setMiddleInitial(this.middleInitial);
        copy.setFullName(this.fullName);
        copy.setBarangay(this.barangay);
        copy.setFarmType(this.farmType);
        copy.setStatus(this.status);
        copy.setTimestamp(this.timestamp);
        copy.setRead(this.read);
        copy.setCrops(this.crops);
        copy.setLivestock(this.livestock);
        copy.setMessage(this.message);
        copy.setTitle(this.title);
        copy.setPriority(this.priority);
        copy.setCategory(this.category);
        copy.setSourceCollection(this.sourceCollection);
        copy.setSourceDocument(this.sourceDocument);
        copy.setActionRequired(this.actionRequired);
        copy.setAssignedTo(this.assignedTo);
        copy.setDueDate(this.dueDate);
        copy.setArchived(this.archived);
        copy.setNotes(this.notes);
        return copy;
    }
}
