package com.maramagagriculturalaid.app.FarmersData;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.maramagagriculturalaid.app.R;

import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.google.firebase.Timestamp;

public class FarmersDetailsActivity extends AppCompatActivity {

    private static final String TAG = "FarmersDetailsActivity";
    private static final int EDIT_FARMER_REQUEST_CODE = 1001;

    private Button btnDelete, btnEdit;
    private ProgressBar progressBar;
    private String documentId, currentBarangay;

    // UI Components for farmer details
    private TextView tvFarmerId, tvFullName, tvPhoneNumber, tvBirthday, tvAddress;
    private TextView tvFarmType, tvCropsGrown, tvLotSize, tvLivestock, tvNumLivestock, tvLastUpdated;
    private LinearLayout cropDetails, livestockDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farmers_details);

        // Initialize views
        btnDelete = findViewById(R.id.btn_delete);
        btnEdit = findViewById(R.id.btn_edit);
        progressBar = findViewById(R.id.progress_bar);

        // Initialize farmer detail TextViews
        tvFarmerId = findViewById(R.id.tv_farmer_id);
        tvFullName = findViewById(R.id.tv_full_name);
        tvPhoneNumber = findViewById(R.id.tv_phone_number);
        tvBirthday = findViewById(R.id.tv_birthday);
        tvAddress = findViewById(R.id.tv_address);
        tvFarmType = findViewById(R.id.tv_farm_type);
        tvCropsGrown = findViewById(R.id.tv_crops_grown);
        tvLotSize = findViewById(R.id.tv_lot_size);
        tvLivestock = findViewById(R.id.tv_livestock);
        tvNumLivestock = findViewById(R.id.tv_num_livestock);
        tvLastUpdated = findViewById(R.id.tv_last_updated);
        cropDetails = findViewById(R.id.crop_details);
        livestockDetails = findViewById(R.id.livestock_details);

        // Initialize back button
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // Get data from intent
        documentId = getIntent().getStringExtra("documentId");
        currentBarangay = getIntent().getStringExtra("barangay");

        // Validate required data
        if (documentId == null || documentId.isEmpty()) {
            Toast.makeText(this, "Error: No farmer ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set button click listeners
        btnDelete.setOnClickListener(v -> deleteFarmer());
        btnEdit.setOnClickListener(v -> editFarmer());

        // Load farmer data
        loadFarmerData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data
        loadFarmerData();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_FARMER_REQUEST_CODE && resultCode == RESULT_OK) {
            // Farmer was successfully edited, refresh the data
            loadFarmerData();
            Toast.makeText(this, "Farmer information updated successfully", Toast.LENGTH_SHORT).show();
        }
    }

    private void editFarmer() {
        Intent intent = new Intent(this, EditFarmerActivity.class);

        // Pass the document ID and barangay to the edit activity
        intent.putExtra("documentId", documentId);
        intent.putExtra("barangay", currentBarangay);

        // Optionally pass current farmer data to pre-populate the edit form
        intent.putExtra("farmerId", tvFarmerId.getText().toString());
        intent.putExtra("fullName", tvFullName.getText().toString());
        intent.putExtra("phoneNumber", tvPhoneNumber.getText().toString());
        intent.putExtra("birthday", tvBirthday.getText().toString());
        intent.putExtra("address", tvAddress.getText().toString());
        intent.putExtra("farmType", tvFarmType.getText().toString());
        intent.putExtra("cropsGrown", tvCropsGrown.getText().toString());
        intent.putExtra("lotSize", tvLotSize.getText().toString());
        intent.putExtra("livestock", tvLivestock != null ? tvLivestock.getText().toString() : "");
        intent.putExtra("numLivestock", tvNumLivestock != null ? tvNumLivestock.getText().toString() : "");

        startActivityForResult(intent, EDIT_FARMER_REQUEST_CODE);
    }

    private void loadFarmerData() {
        showLoading(true);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Try loading from Barangay collection first
        if (currentBarangay != null && !currentBarangay.isEmpty()) {
            db.collection("Barangays").document(currentBarangay)
                    .collection("Farmers").document(documentId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            displayFarmerData(documentSnapshot);
                            // Load farm type data separately
                            loadFarmTypeData(db, "Barangays", currentBarangay, "Farmers", documentId);
                        } else {
                            // Fallback to root collection
                            loadFromRootCollection(db);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Failed to load from barangay collection, trying root", e);
                        // Fallback to root collection
                        loadFromRootCollection(db);
                    });
        } else {
            // Directly query root collection
            loadFromRootCollection(db);
        }
    }

    private void loadFromRootCollection(FirebaseFirestore db) {
        db.collection("Farmers").document(documentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        displayFarmerData(documentSnapshot);
                        // Load farm type data separately
                        loadFarmTypeData(db, "Farmers", null, null, documentId);
                    } else {
                        showLoading(false);
                        Log.e(TAG, "Document does not exist: " + documentId);
                        Toast.makeText(this, "Farmer not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(this::handleLoadError);
    }

    private void loadFarmTypeData(FirebaseFirestore db, String rootCollection, String barangayDoc, String farmersCollection, String farmerDoc) {
        DocumentReference farmerRef;

        if (barangayDoc != null && farmersCollection != null) {
            // Barangay collection path
            farmerRef = db.collection(rootCollection).document(barangayDoc)
                    .collection(farmersCollection).document(farmerDoc);
        } else {
            // Root collection path
            farmerRef = db.collection(rootCollection).document(farmerDoc);
        }

        Log.d(TAG, "Loading FarmType data from: " + farmerRef.getPath());

        // Load FarmType subcollection
        farmerRef.collection("FarmType")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);

                    if (!queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " farm type documents");

                        // Process all farm type documents
                        StringBuilder farmTypeBuilder = new StringBuilder();
                        boolean hasLivestockData = false;
                        boolean hasCropData = false;

                        for (DocumentSnapshot farmTypeDoc : queryDocumentSnapshots.getDocuments()) {
                            String farmTypeId = farmTypeDoc.getId();
                            Map<String, Object> farmTypeData = farmTypeDoc.getData();

                            Log.d(TAG, "=== PROCESSING FARM TYPE: " + farmTypeId + " ===");
                            Log.d(TAG, "Farm type data: " + farmTypeData);

                            // Add to farm type display
                            if (farmTypeBuilder.length() > 0) {
                                farmTypeBuilder.append(", ");
                            }
                            farmTypeBuilder.append(farmTypeId);

                            // Process based on farm type
                            String normalizedFarmType = farmTypeId.toLowerCase().trim();

                            if (normalizedFarmType.contains("livestock") || normalizedFarmType.equals("livestock")) {
                                Log.d(TAG, "Processing livestock farm type");
                                hasLivestockData = displayLivestockData(farmTypeData);
                            } else if (normalizedFarmType.contains("crop") || normalizedFarmType.equals("crop")) {
                                Log.d(TAG, "Processing crop farm type");
                                hasCropData = displayCropData(farmTypeData);
                            } else {
                                Log.d(TAG, "Unknown farm type, checking for any data");
                                // Try both livestock and crop data
                                hasLivestockData = displayLivestockData(farmTypeData);
                                if (!hasLivestockData) {
                                    hasCropData = displayCropData(farmTypeData);
                                }
                            }
                        }

                        // Set the combined farm type
                        if (farmTypeBuilder.length() > 0) {
                            tvFarmType.setText(farmTypeBuilder.toString());
                        } else {
                            tvFarmType.setText("N/A");
                        }

                        // Show/hide sections based on data availability
                        if (hasLivestockData) {
                            if (livestockDetails != null) {
                                livestockDetails.setVisibility(View.VISIBLE);
                                Log.d(TAG, "Showing livestock details section");
                            }
                        } else {
                            if (livestockDetails != null) {
                                livestockDetails.setVisibility(View.GONE);
                            }
                        }

                        if (hasCropData) {
                            if (cropDetails != null) {
                                cropDetails.setVisibility(View.VISIBLE);
                                Log.d(TAG, "Showing crop details section");
                            }
                        } else {
                            if (cropDetails != null) {
                                cropDetails.setVisibility(View.GONE);
                            }
                        }

                        // If no specific data found, hide both sections
                        if (!hasLivestockData && !hasCropData) {
                            Log.d(TAG, "No specific farm data found, hiding both sections");
                            if (cropDetails != null) cropDetails.setVisibility(View.GONE);
                            if (livestockDetails != null) livestockDetails.setVisibility(View.GONE);
                        }

                    } else {
                        Log.d(TAG, "No farm type data found");
                        tvFarmType.setText("N/A");
                        // Hide both farm detail sections
                        if (cropDetails != null) cropDetails.setVisibility(View.GONE);
                        if (livestockDetails != null) livestockDetails.setVisibility(View.GONE);
                    }

                    // Make buttons visible
                    btnDelete.setVisibility(View.VISIBLE);
                    btnEdit.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error loading farm type data", e);
                    tvFarmType.setText("Error loading farm type");
                    // Still show buttons even if farm type loading fails
                    btnDelete.setVisibility(View.VISIBLE);
                    btnEdit.setVisibility(View.VISIBLE);
                });
    }

    private boolean displayLivestockData(Map<String, Object> farmTypeData) {
        if (farmTypeData == null) {
            Log.d(TAG, "No farm type data for livestock");
            return false;
        }

        boolean hasData = false;

        // Get livestock type - try multiple field names
        String livestockType = getFieldValue(farmTypeData, "livestockType", "livestock", "livestock_type", "animalType", "animals", "animal");
        Log.d(TAG, "Livestock type found: " + livestockType);

        if (livestockType != null && !livestockType.isEmpty()) {
            if (tvLivestock != null) {
                tvLivestock.setText(livestockType);
                Log.d(TAG, "Set livestock type to: " + livestockType);
            }
            hasData = true;
        } else {
            if (tvLivestock != null) {
                tvLivestock.setText("N/A");
                Log.d(TAG, "Set livestock type to: N/A");
            }
        }

        // Get livestock count - try multiple field names including the exact one from database
        String livestockCount = getFieldValue(farmTypeData, "livestockCount", "numLivestock", "numberOfLivestock", "livestock_count", "animalCount", "count", "quantity", "livestockNumber", "totalLivestock");
        Log.d(TAG, "Livestock count found: " + livestockCount);

        if (livestockCount != null && !livestockCount.isEmpty()) {
            if (tvNumLivestock != null) {
                tvNumLivestock.setText(livestockCount);
                Log.d(TAG, "Set livestock count to: " + livestockCount);
            }
            hasData = true;
        } else {
            if (tvNumLivestock != null) {
                tvNumLivestock.setText("N/A");
                Log.d(TAG, "Set livestock count to: N/A");
            }
        }

        Log.d(TAG, "Livestock data processing complete. Has data: " + hasData);
        return hasData;
    }

    private boolean displayCropData(Map<String, Object> farmTypeData) {
        if (farmTypeData == null) {
            Log.d(TAG, "No farm type data for crops");
            return false;
        }

        boolean hasData = false;

        // Get crops grown
        String cropsGrown = getFieldValue(farmTypeData, "cropsGrown", "crops", "crops_grown", "cropType", "cropTypes");
        Log.d(TAG, "Crops grown found: " + cropsGrown);

        if (cropsGrown != null && !cropsGrown.isEmpty()) {
            if (tvCropsGrown != null) {
                tvCropsGrown.setText(cropsGrown);
                Log.d(TAG, "Set crops grown to: " + cropsGrown);
            }
            hasData = true;
        } else {
            if (tvCropsGrown != null) {
                tvCropsGrown.setText("N/A");
                Log.d(TAG, "Set crops grown to: N/A");
            }
        }

        // Get lot size - combine value and unit if available
        String lotSizeValue = getFieldValue(farmTypeData, "lotSizeValue", "lotSize", "lot_size");
        String lotSizeUnit = getFieldValue(farmTypeData, "lotSizeUnit", "unit", "sizeUnit");
        String combinedLotSize = "";

        if (lotSizeValue != null && !lotSizeValue.isEmpty()) {
            combinedLotSize = lotSizeValue;
            if (lotSizeUnit != null && !lotSizeUnit.isEmpty()) {
                combinedLotSize += " " + lotSizeUnit;
            }
        } else {
            // Fallback to direct lotSize field
            combinedLotSize = getFieldValue(farmTypeData, "lotSize", "lot_size", "farmSize", "landSize");
        }

        Log.d(TAG, "Lot size found: " + combinedLotSize);

        if (combinedLotSize != null && !combinedLotSize.isEmpty()) {
            if (tvLotSize != null) {
                tvLotSize.setText(combinedLotSize);
                Log.d(TAG, "Set lot size to: " + combinedLotSize);
            }
            hasData = true;
        } else {
            if (tvLotSize != null) {
                tvLotSize.setText("N/A");
                Log.d(TAG, "Set lot size to: N/A");
            }
        }

        Log.d(TAG, "Crop data processing complete. Has data: " + hasData);
        return hasData;
    }

    private void displayFarmerData(DocumentSnapshot farmerDoc) {
        if (farmerDoc == null || !farmerDoc.exists()) {
            Toast.makeText(this, "Farmer not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get data from document
        Map<String, Object> data = farmerDoc.getData();
        if (data != null) {
            // Log all available fields for debugging
            Log.d(TAG, "Available fields in document: " + data.keySet().toString());

            // Populate basic information based on your database structure
            populateField(tvFarmerId, data, "farmerId", "id", "farmer_id");

            // Construct full name from available fields
            String fullName = constructFullName(data);
            tvFullName.setText(fullName != null && !fullName.isEmpty() ? fullName : "N/A");

            populateField(tvPhoneNumber, data, "phoneNumber", "phone", "contactNumber", "mobileNumber");
            populateField(tvBirthday, data, "birthday", "birthdate", "dateOfBirth", "birth_date");

            // Construct address from available fields
            String address = constructAddress(data);
            tvAddress.setText(address != null && !address.isEmpty() ? address : "N/A");
            Log.d(TAG, "Constructed address: " + address);

            // Set last updated - handle both Timestamp and String formats
            setLastUpdated(data);

        } else {
            Toast.makeText(this, "No farmer data available", Toast.LENGTH_SHORT).show();
        }
    }

    private String constructFullName(Map<String, Object> data) {
        String firstName = getFieldValue(data, "firstName", "first_name", "fname");
        String middleInitial = getFieldValue(data, "middleInitial", "middle_initial", "middleName", "mname");
        String lastName = getFieldValue(data, "lastName", "last_name", "surname", "lname");
        String fullName = getFieldValue(data, "fullName", "full_name", "name");

        // If fullName exists, use it
        if (fullName != null && !fullName.isEmpty()) {
            return fullName;
        }

        // Otherwise construct from parts
        StringBuilder nameBuilder = new StringBuilder();
        if (firstName != null && !firstName.isEmpty()) {
            nameBuilder.append(firstName);
        }
        if (middleInitial != null && !middleInitial.isEmpty()) {
            if (nameBuilder.length() > 0) nameBuilder.append(" ");
            nameBuilder.append(middleInitial);
        }
        if (lastName != null && !lastName.isEmpty()) {
            if (nameBuilder.length() > 0) nameBuilder.append(" ");
            nameBuilder.append(lastName);
        }

        return nameBuilder.toString();
    }

    private String constructAddress(Map<String, Object> data) {
        // Only include street, barangay, and municipal as requested
        String street = getFieldValue(data, "street", "streetAddress", "street_address", "houseNumber");
        String barangay = getFieldValue(data, "barangay", "brgy", "barangayName");
        String municipal = getFieldValue(data, "municipal", "municipality", "city", "town");

        StringBuilder addressBuilder = new StringBuilder();

        if (street != null && !street.isEmpty()) {
            addressBuilder.append(street);
        }

        if (barangay != null && !barangay.isEmpty()) {
            if (addressBuilder.length() > 0) addressBuilder.append(", ");
            addressBuilder.append(barangay);
        }

        if (municipal != null && !municipal.isEmpty()) {
            if (addressBuilder.length() > 0) addressBuilder.append(", ");
            addressBuilder.append(municipal);
        }

        String result = addressBuilder.toString();
        Log.d(TAG, "Address components - Street: " + street + ", Barangay: " + barangay +
                ", Municipal: " + municipal + ", Result: " + result);

        return result;
    }

    private void setLastUpdated(Map<String, Object> data) {
        Object lastUpdatedObj = data.get("lastUpdated");

        if (lastUpdatedObj instanceof Timestamp) {
            Timestamp timestamp = (Timestamp) lastUpdatedObj;
            Date date = timestamp.toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm:ss a", Locale.getDefault());
            tvLastUpdated.setText("Last updated: " + sdf.format(date));
        } else if (lastUpdatedObj instanceof String) {
            tvLastUpdated.setText("Last updated: " + lastUpdatedObj.toString());
        } else if (data.containsKey("createdAt")) {
            Object createdAtObj = data.get("createdAt");
            if (createdAtObj instanceof Timestamp) {
                Timestamp timestamp = (Timestamp) createdAtObj;
                Date date = timestamp.toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm:ss a", Locale.getDefault());
                tvLastUpdated.setText("Created: " + sdf.format(date));
            } else {
                tvLastUpdated.setText("Created: " + createdAtObj.toString());
            }
        } else {
            tvLastUpdated.setText("Last updated: N/A");
        }
    }

    private void populateField(TextView textView, Map<String, Object> data, String... possibleKeys) {
        String value = getFieldValue(data, possibleKeys);
        String fieldName = textView != null ? getResourceName(textView.getId()) : "unknown";

        if (value != null && !value.isEmpty()) {
            textView.setText(value);
            Log.d(TAG, "Set " + fieldName + " to: " + value);
        } else {
            textView.setText("N/A");
            Log.d(TAG, "Set " + fieldName + " to: N/A (no value found for keys: " +
                    java.util.Arrays.toString(possibleKeys) + ")");
        }
    }

    private String getResourceName(int resourceId) {
        try {
            return getResources().getResourceEntryName(resourceId);
        } catch (Exception e) {
            return "unknown_resource";
        }
    }

    private String getFieldValue(Map<String, Object> data, String... possibleKeys) {
        if (data == null) {
            Log.d(TAG, "Data map is null");
            return null;
        }

        for (String key : possibleKeys) {
            if (data.containsKey(key) && data.get(key) != null) {
                Object value = data.get(key);
                String stringValue = value.toString().trim();
                if (!stringValue.isEmpty() && !stringValue.equalsIgnoreCase("null")) {
                    Log.d(TAG, "Found value for key '" + key + "': " + stringValue);
                    return stringValue;
                }
            }
        }
        Log.d(TAG, "No value found for any of these keys: " + java.util.Arrays.toString(possibleKeys));
        return null;
    }

    private void handleLoadError(Exception e) {
        showLoading(false);
        Log.e(TAG, "Error loading farmer data", e);
        Toast.makeText(this, "Failed to load farmer data: " + e.getMessage(), Toast.LENGTH_LONG).show();

        // Show retry option
        new AlertDialog.Builder(this)
                .setTitle("Error Loading Data")
                .setMessage("Failed to load farmer information. Would you like to try again?")
                .setPositiveButton("Retry", (dialog, which) -> loadFarmerData())
                .setNegativeButton("Close", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void deleteFarmer() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete this farmer? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> performDeletion())
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void performDeletion() {
        showLoading(true);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Determine which collection to delete from
        DocumentReference docRef;
        if (currentBarangay != null && !currentBarangay.isEmpty()) {
            docRef = db.collection("Barangays").document(currentBarangay)
                    .collection("Farmers").document(documentId);
        } else {
            docRef = db.collection("Farmers").document(documentId);
        }

        docRef.delete()
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this, "Farmer deleted successfully", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Farmer successfully deleted");

                    // Set result and finish activity
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error deleting farmer", e);
                    Toast.makeText(this, "Failed to delete farmer: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();

                    // Show retry option
                    new AlertDialog.Builder(this)
                            .setTitle("Delete Failed")
                            .setMessage("Failed to delete farmer. Would you like to try again?")
                            .setPositiveButton("Retry", (dialog, which) -> performDeletion())
                            .setNegativeButton("Cancel", null)
                            .show();
                });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        btnDelete.setEnabled(!show);
        btnEdit.setEnabled(!show);
    }
}