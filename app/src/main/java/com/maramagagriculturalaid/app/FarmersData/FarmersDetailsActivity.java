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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.maramagagriculturalaid.app.R;
import com.maramagagriculturalaid.app.ActivityLogger;

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
    private String documentId, currentBarangay, farmerId;

    // UI Components for farmer details
    private TextView tvFarmerId, tvFullName, tvPhoneNumber, tvBirthday, tvAddress;
    private TextView tvFarmType, tvCropsGrown, tvLotSize, tvLivestock, tvNumLivestock, tvLastUpdated;
    private LinearLayout cropDetails, livestockDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farmers_details);

        // Initialize views
        initializeViews();

        // Get data from intent
        extractIntentData();

        // Validate required data
        if (documentId == null || documentId.isEmpty()) {
            Toast.makeText(this, "Error: No farmer ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set button click listeners
        setupClickListeners();

        // Load farmer data
        loadFarmerData();
    }

    private void initializeViews() {
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
    }

    private void extractIntentData() {
        documentId = getIntent().getStringExtra("documentId");
        currentBarangay = getIntent().getStringExtra("barangayId");
        farmerId = getIntent().getStringExtra("farmerId");

        // Fallback for legacy intent extras
        if (currentBarangay == null) {
            currentBarangay = getIntent().getStringExtra("barangay");
        }

        Log.d(TAG, "Intent data - documentId: " + documentId + ", barangayId: " + currentBarangay + ", farmerId: " + farmerId);
    }

    private void setupClickListeners() {
        btnDelete.setOnClickListener(v -> deleteFarmer());
        btnEdit.setOnClickListener(v -> editFarmer());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Force refresh data when returning from edit
        Log.d(TAG, "onResume called - refreshing farmer data");
        loadFarmerData();
    }

    // FIXED: Enhanced onActivityResult
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_FARMER_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // Farmer was successfully edited, refresh the data
                Log.d(TAG, "Farmer edit completed successfully, refreshing data");
                loadFarmerData();
                Toast.makeText(this, "Farmer information updated successfully", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "Farmer edit was cancelled");
            }
        }
    }

    // FIXED: Enhanced editFarmer method
    private void editFarmer() {
        try {
            Intent intent = new Intent(this, EditFarmerActivity.class);

            // Pass the correct data to EditFarmerActivity
            intent.putExtra("farmerId", farmerId);
            intent.putExtra("farmerName", tvFullName.getText().toString());
            intent.putExtra("barangay", currentBarangay);
            intent.putExtra("farmerDocumentId", documentId);

            Log.d(TAG, "Starting EditFarmerActivity with - farmerId: " + farmerId +
                    ", barangay: " + currentBarangay + ", documentId: " + documentId);

            startActivityForResult(intent, EDIT_FARMER_REQUEST_CODE);
        } catch (Exception e) {
            Log.e(TAG, "Error starting EditFarmerActivity", e);
            Toast.makeText(this, "Error opening edit screen", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadFarmerData() {
        showLoading(true);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Load from Barangay collection structure
        if (currentBarangay != null && !currentBarangay.isEmpty()) {
            Log.d(TAG, "Loading farmer from barangay: " + currentBarangay + ", document: " + documentId);

            db.collection("Barangays").document(currentBarangay)
                    .collection("Farmers").document(documentId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Log.d(TAG, "Farmer document found in barangay collection");
                            displayFarmerData(documentSnapshot);
                            // Load farm type data separately
                            loadFarmTypeData(db, currentBarangay, documentId);
                        } else {
                            Log.w(TAG, "Farmer document not found in barangay collection");
                            showLoading(false);
                            Toast.makeText(this, "Farmer not found", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to load farmer from barangay collection", e);
                        handleLoadError(e);
                    });
        } else {
            Log.e(TAG, "No barangay ID provided");
            showLoading(false);
            Toast.makeText(this, "Error: No barangay information available", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // FIXED: Enhanced farm type data loading with better mixed farm detection
    private void loadFarmTypeData(FirebaseFirestore db, String barangayId, String farmerDocId) {
        DocumentReference farmerRef = db.collection("Barangays").document(barangayId)
                .collection("Farmers").document(farmerDocId);

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

                            Log.d(TAG, "Processing farm type: " + farmTypeId + " with data: " + farmTypeData);

                            // Add to farm type display
                            if (farmTypeBuilder.length() > 0) {
                                farmTypeBuilder.append(", ");
                            }

                            // Check if this is a mixed farm (has both crop and livestock documents)
                            if ("Crop".equals(farmTypeId)) {
                                hasCropData = displayCropData(farmTypeData) || hasCropData;
                                farmTypeBuilder.append("Crop");
                            } else if ("Livestock".equals(farmTypeId)) {
                                hasLivestockData = displayLivestockData(farmTypeData) || hasLivestockData;
                                farmTypeBuilder.append("Livestock");
                            }
                        }

                        // Determine if this is a mixed farm
                        String finalFarmType;
                        if (hasCropData && hasLivestockData) {
                            finalFarmType = "Mixed";
                            Log.d(TAG, "Detected Mixed farm type with both crop and livestock data");
                        } else {
                            finalFarmType = farmTypeBuilder.toString();
                        }

                        // Set the farm type
                        tvFarmType.setText(!finalFarmType.isEmpty() ? finalFarmType : "N/A");

                        // Show/hide sections based on data availability
                        updateSectionVisibility(hasCropData, hasLivestockData);

                    } else {
                        Log.d(TAG, "No farm type data found");
                        tvFarmType.setText("N/A");
                        updateSectionVisibility(false, false);
                    }

                    // Make buttons visible
                    btnDelete.setVisibility(View.VISIBLE);
                    btnEdit.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error loading farm type data", e);
                    tvFarmType.setText("Error loading farm type");
                    updateSectionVisibility(false, false);

                    // Still show buttons even if farm type loading fails
                    btnDelete.setVisibility(View.VISIBLE);
                    btnEdit.setVisibility(View.VISIBLE);
                });
    }

    private void updateSectionVisibility(boolean hasCropData, boolean hasLivestockData) {
        if (cropDetails != null) {
            cropDetails.setVisibility(hasCropData ? View.VISIBLE : View.GONE);
        }
        if (livestockDetails != null) {
            livestockDetails.setVisibility(hasLivestockData ? View.VISIBLE : View.GONE);
        }

        Log.d(TAG, "Section visibility - Crops: " + hasCropData + ", Livestock: " + hasLivestockData);
    }

    private boolean displayLivestockData(Map<String, Object> farmTypeData) {
        if (farmTypeData == null) {
            return false;
        }

        boolean hasData = false;

        // Get livestock type
        String livestockType = getFieldValue(farmTypeData, "livestockType", "livestock", "animalType");
        if (livestockType != null && !livestockType.isEmpty() && tvLivestock != null) {
            tvLivestock.setText(livestockType);
            hasData = true;
            Log.d(TAG, "Set livestock type: " + livestockType);
        } else if (tvLivestock != null) {
            tvLivestock.setText("N/A");
        }

        // Get livestock count
        String livestockCount = getFieldValue(farmTypeData, "livestockCount", "animalCount", "count");
        if (livestockCount != null && !livestockCount.isEmpty() && tvNumLivestock != null) {
            tvNumLivestock.setText(livestockCount);
            hasData = true;
            Log.d(TAG, "Set livestock count: " + livestockCount);
        } else if (tvNumLivestock != null) {
            tvNumLivestock.setText("N/A");
        }

        return hasData;
    }

    private boolean displayCropData(Map<String, Object> farmTypeData) {
        if (farmTypeData == null) {
            return false;
        }

        boolean hasData = false;

        // Get crops grown
        String cropsGrown = getFieldValue(farmTypeData, "cropsGrown", "crops", "cropType");
        if (cropsGrown != null && !cropsGrown.isEmpty() && tvCropsGrown != null) {
            tvCropsGrown.setText(cropsGrown);
            hasData = true;
            Log.d(TAG, "Set crops grown: " + cropsGrown);
        } else if (tvCropsGrown != null) {
            tvCropsGrown.setText("N/A");
        }

        // Get lot size - try to get complete lot size first, then construct from parts
        String lotSize = getFieldValue(farmTypeData, "lotSize");
        if (lotSize == null || lotSize.isEmpty()) {
            String lotSizeValue = getFieldValue(farmTypeData, "lotSizeValue");
            String lotSizeUnit = getFieldValue(farmTypeData, "lotSizeUnit");

            if (lotSizeValue != null && !lotSizeValue.isEmpty()) {
                lotSize = lotSizeValue;
                if (lotSizeUnit != null && !lotSizeUnit.isEmpty()) {
                    lotSize += " " + lotSizeUnit;
                }
            }
        }

        if (lotSize != null && !lotSize.isEmpty() && tvLotSize != null) {
            tvLotSize.setText(lotSize);
            hasData = true;
            Log.d(TAG, "Set lot size: " + lotSize);
        } else if (tvLotSize != null) {
            tvLotSize.setText("N/A");
        }

        return hasData;
    }

    private void displayFarmerData(DocumentSnapshot farmerDoc) {
        if (farmerDoc == null || !farmerDoc.exists()) {
            Toast.makeText(this, "Farmer not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Map<String, Object> data = farmerDoc.getData();
        if (data != null) {
            Log.d(TAG, "Available fields: " + data.keySet().toString());

            // Populate basic information
            populateField(tvFarmerId, data, "farmerId", "id");

            // Construct full name
            String fullName = constructFullName(data);
            if (tvFullName != null) {
                tvFullName.setText(fullName != null && !fullName.isEmpty() ? fullName : "N/A");
            }

            populateField(tvPhoneNumber, data, "phoneNumber", "phone", "contactNumber");

            // Handle birthday formatting
            setBirthday(data);

            // Construct address
            String address = constructAddress(data);
            if (tvAddress != null) {
                tvAddress.setText(address != null && !address.isEmpty() ? address : "N/A");
            }

            // Set last updated
            setLastUpdated(data);

        } else {
            Toast.makeText(this, "No farmer data available", Toast.LENGTH_SHORT).show();
        }
    }

    private void setBirthday(Map<String, Object> data) {
        if (tvBirthday == null) return;

        Object birthdayObj = data.get("birthday");
        if (birthdayObj instanceof Timestamp) {
            Timestamp timestamp = (Timestamp) birthdayObj;
            Date date = timestamp.toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            tvBirthday.setText(sdf.format(date));
        } else if (birthdayObj instanceof String) {
            tvBirthday.setText(birthdayObj.toString());
        } else {
            tvBirthday.setText("N/A");
        }
    }

    private String constructFullName(Map<String, Object> data) {
        // Try to get fullName first
        String fullName = getFieldValue(data, "fullName", "name");
        if (fullName != null && !fullName.isEmpty()) {
            return fullName;
        }

        // Otherwise construct from parts
        String firstName = getFieldValue(data, "firstName");
        String middleInitial = getFieldValue(data, "middleInitial");
        String lastName = getFieldValue(data, "lastName");

        StringBuilder nameBuilder = new StringBuilder();
        if (firstName != null && !firstName.isEmpty()) {
            nameBuilder.append(firstName);
        }
        if (middleInitial != null && !middleInitial.isEmpty()) {
            if (nameBuilder.length() > 0) nameBuilder.append(" ");
            nameBuilder.append(middleInitial);
            if (!middleInitial.endsWith(".")) {
                nameBuilder.append(".");
            }
        }
        if (lastName != null && !lastName.isEmpty()) {
            if (nameBuilder.length() > 0) nameBuilder.append(" ");
            nameBuilder.append(lastName);
        }

        return nameBuilder.toString();
    }

    private String constructAddress(Map<String, Object> data) {
        String street = getFieldValue(data, "street", "streetAddress");
        String barangay = getFieldValue(data, "barangay");
        String municipal = getFieldValue(data, "municipal", "municipality");

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

        return addressBuilder.toString();
    }

    private void setLastUpdated(Map<String, Object> data) {
        if (tvLastUpdated == null) return;

        Object lastUpdatedObj = data.get("lastUpdated");
        if (lastUpdatedObj instanceof Timestamp) {
            Timestamp timestamp = (Timestamp) lastUpdatedObj;
            Date date = timestamp.toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
            tvLastUpdated.setText("Last updated: " + sdf.format(date));
        } else if (lastUpdatedObj instanceof String) {
            tvLastUpdated.setText("Last updated: " + lastUpdatedObj.toString());
        } else {
            Object createdAtObj = data.get("createdAt");
            if (createdAtObj instanceof Timestamp) {
                Timestamp timestamp = (Timestamp) createdAtObj;
                Date date = timestamp.toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
                tvLastUpdated.setText("Created: " + sdf.format(date));
            } else {
                tvLastUpdated.setText("Last updated: N/A");
            }
        }
    }

    private void populateField(TextView textView, Map<String, Object> data, String... possibleKeys) {
        if (textView == null) return;

        String value = getFieldValue(data, possibleKeys);
        textView.setText(value != null && !value.isEmpty() ? value : "N/A");
    }

    private String getFieldValue(Map<String, Object> data, String... possibleKeys) {
        if (data == null) return null;

        for (String key : possibleKeys) {
            if (data.containsKey(key) && data.get(key) != null) {
                Object value = data.get(key);
                String stringValue = value.toString().trim();
                if (!stringValue.isEmpty() && !stringValue.equalsIgnoreCase("null")) {
                    return stringValue;
                }
            }
        }
        return null;
    }

    private void handleLoadError(Exception e) {
        showLoading(false);
        Log.e(TAG, "Error loading farmer data", e);
        Toast.makeText(this, "Failed to load farmer data", Toast.LENGTH_SHORT).show();

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

        // Get farmer name for logging before deletion
        String farmerName = tvFullName.getText().toString();
        if (farmerName.equals("N/A") || farmerName.isEmpty()) {
            farmerName = "Unknown Farmer";
        }

        String barangayForLogging = currentBarangay != null ? currentBarangay : "Unknown Barangay";

        // Delete from the correct collection structure
        DocumentReference docRef = db.collection("Barangays").document(currentBarangay)
                .collection("Farmers").document(documentId);

        Log.d(TAG, "Deleting farmer from: " + docRef.getPath());

        String finalFarmerName = farmerName;
        docRef.delete()
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);

                    // Log the farmer removal activity
                    Log.d(TAG, "Logging farmer removal: " + finalFarmerName + " from " + barangayForLogging);
                    ActivityLogger.logFarmerRemoved(barangayForLogging, finalFarmerName);

                    Toast.makeText(this, "Farmer deleted successfully", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Farmer successfully deleted");

                    // Set result and finish activity
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error deleting farmer", e);
                    Toast.makeText(this, "Failed to delete farmer: " + e.getMessage(), Toast.LENGTH_LONG).show();

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
        if (btnDelete != null) {
            btnDelete.setEnabled(!show);
        }
        if (btnEdit != null) {
            btnEdit.setEnabled(!show);
        }
    }
}
