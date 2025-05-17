package com.maramagagriculturalaid.app.Municipal.BarangayOverview;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.maramagagriculturalaid.app.FarmersData.Farmer;
import com.maramagagriculturalaid.app.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MunicipalFarmersDetails extends AppCompatActivity {

    private static final String TAG = "MunicipalFarmerDetails";

    // UI Components
    private ImageButton btnBack;
    private TextView tvTitle;
    private TextView tvFarmerId;
    private TextView tvFullName;
    private TextView tvPhoneNumber;
    private TextView tvBirthday;
    private TextView tvAddress;
    private TextView tvFarmType;
    private TextView tvCropsGrown;
    private TextView tvLotSize;
    private TextView tvLivestock;
    private TextView tvNumLivestock;
    private TextView tvLastUpdated;
    private ProgressBar progressBar;
    private LinearLayout cropDetails;
    private LinearLayout livestockDetails;

    // Data
    private String farmerId;
    private String barangayName;
    private FirebaseFirestore db;
    private List<DocumentSnapshot> farmTypeDocuments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_municipal_farmers_details);

        // Get data from intent
        farmerId = getIntent().getStringExtra("farmerId");
        barangayName = getIntent().getStringExtra("barangayName");

        if (farmerId == null || barangayName == null) {
            Log.e(TAG, "Missing required data: farmerId or barangayName");
            Toast.makeText(this, "Error: Missing farmer information", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Log.d(TAG, "Loading farmer details for ID: " + farmerId + " in barangay: " + barangayName);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        farmTypeDocuments = new ArrayList<>();

        // Initialize views
        initializeViews();

        // Setup click listeners
        setupClickListeners();

        // Load farmer data
        loadFarmerDetails();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btn_back);
        tvTitle = findViewById(R.id.tv_title);
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
        progressBar = findViewById(R.id.progress_bar);
        cropDetails = findViewById(R.id.crop_details);
        livestockDetails = findViewById(R.id.livestock_details);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            Log.d(TAG, "Back button clicked");
            finish();
        });
    }

    private void loadFarmerDetails() {
        Log.d(TAG, "Loading farmer details from Firestore");
        showLoading(true);

        // Query: Barangays/{barangayName}/Farmers/{farmerId}
        db.collection("Barangays")
                .document(barangayName)
                .collection("Farmers")
                .document(farmerId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Log.d(TAG, "Farmer document found");
                            displayFarmerDetails(document);
                            // Load farm type details from subcollection
                            loadFarmTypeDetails();
                        } else {
                            Log.w(TAG, "Farmer document not found");
                            showLoading(false);
                            Toast.makeText(this, "Farmer not found", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    } else {
                        Log.e(TAG, "Error loading farmer details", task.getException());
                        showLoading(false);
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Unknown error";
                        Toast.makeText(this, "Error loading farmer: " + errorMessage, Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load farmer details", e);
                    showLoading(false);
                    Toast.makeText(this, "Failed to load farmer: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void loadFarmTypeDetails() {
        Log.d(TAG, "Loading farm type details from subcollection");

        // Query: Barangays/{barangayName}/Farmers/{farmerId}/FarmType
        db.collection("Barangays")
                .document(barangayName)
                .collection("Farmers")
                .document(farmerId)
                .collection("FarmType")
                .get()
                .addOnCompleteListener(task -> {
                    showLoading(false);

                    if (task.isSuccessful()) {
                        farmTypeDocuments.clear();

                        if (!task.getResult().isEmpty()) {
                            Log.d(TAG, "Found " + task.getResult().size() + " farm type documents");

                            // Collect all farm type documents
                            for (QueryDocumentSnapshot farmTypeDoc : task.getResult()) {
                                farmTypeDocuments.add(farmTypeDoc);
                                Log.d(TAG, "Farm type document ID: " + farmTypeDoc.getId());
                            }

                            // Process all farm type documents
                            processFarmTypeDocuments();

                        } else {
                            Log.d(TAG, "No farm type documents found");
                            // Hide both sections if no farm type data
                            cropDetails.setVisibility(View.GONE);
                            livestockDetails.setVisibility(View.GONE);
                            tvFarmType.setText("Not specified");
                        }
                    } else {
                        Log.e(TAG, "Error loading farm type details", task.getException());
                        cropDetails.setVisibility(View.GONE);
                        livestockDetails.setVisibility(View.GONE);
                        tvFarmType.setText("Not specified");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load farm type details", e);
                    showLoading(false);
                    cropDetails.setVisibility(View.GONE);
                    livestockDetails.setVisibility(View.GONE);
                    tvFarmType.setText("Not specified");
                });
    }

    private void processFarmTypeDocuments() {
        if (farmTypeDocuments.isEmpty()) {
            cropDetails.setVisibility(View.GONE);
            livestockDetails.setVisibility(View.GONE);
            tvFarmType.setText("Not specified");
            return;
        }

        Log.d(TAG, "Processing " + farmTypeDocuments.size() + " farm type documents");

        // Initialize variables to collect data
        List<String> farmTypes = new ArrayList<>();
        boolean hasCropData = false;
        boolean hasLivestockData = false;

        // Data holders
        String cropsGrown = null;
        Object lotSizeObj = null;
        String lotSizeUnit = null;
        String livestock = null;
        Object livestockCountObj = null;
        Timestamp lastUpdatedTimestamp = null;

        // Process each farm type document
        for (DocumentSnapshot farmTypeDoc : farmTypeDocuments) {
            Log.d(TAG, "=== PROCESSING FARM TYPE DOCUMENT: " + farmTypeDoc.getId() + " ===");

            // Debug: Log all available fields in farm type document
            for (String key : farmTypeDoc.getData().keySet()) {
                Object value = farmTypeDoc.get(key);
                Log.d(TAG, "FarmType Field '" + key + "': " + (value != null ? value.toString() : "null"));
            }

            // Get farm type from document ID (Crop, Livestock, etc.)
            String farmType = farmTypeDoc.getId();
            if (farmType != null && !farmType.isEmpty()) {
                farmTypes.add(farmType);
                Log.d(TAG, "Found farm type from document ID: " + farmType);
            }

            // Get lastUpdated timestamp
            Object lastUpdatedObj = farmTypeDoc.get("lastUpdated");
            if (lastUpdatedObj instanceof Timestamp) {
                lastUpdatedTimestamp = (Timestamp) lastUpdatedObj;
                Log.d(TAG, "Found lastUpdated timestamp: " + lastUpdatedTimestamp);
            }

            // Check for crop data
            String docCropsGrown = getStringFromDocument(farmTypeDoc, "cropsGrown");
            Object docLotSizeObj = farmTypeDoc.get("lotSizeValue");
            if (docLotSizeObj == null) {
                docLotSizeObj = farmTypeDoc.get("lotSize");
            }
            String docLotSizeUnit = getStringFromDocument(farmTypeDoc, "lotSizeUnit");

            if (docCropsGrown != null || docLotSizeObj != null) {
                hasCropData = true;
                if (docCropsGrown != null) cropsGrown = docCropsGrown;
                if (docLotSizeObj != null) {
                    lotSizeObj = docLotSizeObj;
                    if (docLotSizeUnit != null) lotSizeUnit = docLotSizeUnit;
                }
                Log.d(TAG, "Found crop data - Crops: " + docCropsGrown + ", Lot size: " + docLotSizeObj);
            }

            // Check for livestock data - FIXED FIELD NAMES
            String docLivestock = getStringFromDocument(farmTypeDoc, "livestockType"); // Changed from "livestock"
            Object docLivestockCountObj = farmTypeDoc.get("livestockCount");

            if (docLivestock != null || docLivestockCountObj != null) {
                hasLivestockData = true;
                if (docLivestock != null) livestock = docLivestock;
                if (docLivestockCountObj != null) livestockCountObj = docLivestockCountObj;
                Log.d(TAG, "Found livestock data - Type: " + docLivestock + ", Count: " + docLivestockCountObj);
            }
        }

        // Set farm type display
        if (!farmTypes.isEmpty()) {
            String farmTypeDisplay = String.join(", ", farmTypes);
            tvFarmType.setText(farmTypeDisplay);
            Log.d(TAG, "Set farm type display: " + farmTypeDisplay);
        } else {
            tvFarmType.setText("Not specified");
        }

        // Update last updated with timestamp from farm type documents
        if (lastUpdatedTimestamp != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
                String formattedDate = sdf.format(lastUpdatedTimestamp.toDate());
                tvLastUpdated.setText("Last updated: " + formattedDate);
                Log.d(TAG, "Set last updated: " + formattedDate);
            } catch (Exception e) {
                Log.e(TAG, "Error formatting timestamp", e);
                tvLastUpdated.setText("Last updated: " + new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date()));
            }
        }

        // Show appropriate sections based on available data
        Log.d(TAG, "Has crop data: " + hasCropData + ", Has livestock data: " + hasLivestockData);

        if (hasCropData) {
            showCropDetailsWithData(cropsGrown, lotSizeObj, lotSizeUnit);
        } else {
            cropDetails.setVisibility(View.GONE);
            Log.d(TAG, "No crop data found, hiding crop section");
        }

        if (hasLivestockData) {
            showLivestockDetailsWithData(livestock, livestockCountObj);
        } else {
            livestockDetails.setVisibility(View.GONE);
            Log.d(TAG, "No livestock data found, hiding livestock section");
        }

        // If no data found at all
        if (!hasCropData && !hasLivestockData) {
            Log.d(TAG, "No farm data found in any document");
        }
    }

    private void showCropDetailsWithData(String cropsGrown, Object lotSizeObj, String lotSizeUnit) {
        Log.d(TAG, "Showing crop details with data");
        cropDetails.setVisibility(View.VISIBLE);

        // Set crops grown
        tvCropsGrown.setText(cropsGrown != null ? cropsGrown : "Not specified");

        // Set lot size
        if (lotSizeObj != null) {
            try {
                if (lotSizeObj instanceof Number) {
                    double lotSize = ((Number) lotSizeObj).doubleValue();
                    String unit = lotSizeUnit != null ? lotSizeUnit : "hectares";
                    tvLotSize.setText(String.format(Locale.getDefault(), "%.2f %s", lotSize, unit));
                } else {
                    // If it's a string, try to parse it
                    String lotSizeStr = lotSizeObj.toString();
                    try {
                        double lotSize = Double.parseDouble(lotSizeStr);
                        String unit = lotSizeUnit != null ? lotSizeUnit : "hectares";
                        tvLotSize.setText(String.format(Locale.getDefault(), "%.2f %s", lotSize, unit));
                    } catch (NumberFormatException e) {
                        tvLotSize.setText(lotSizeStr);
                    }
                }
            } catch (Exception e) {
                tvLotSize.setText("Not specified");
                Log.e(TAG, "Error formatting lot size", e);
            }
        } else {
            tvLotSize.setText("Not specified");
        }

        Log.d(TAG, "Crop details displayed - Crops: " + cropsGrown + ", Lot size: " + lotSizeObj);
    }

    private void showLivestockDetailsWithData(String livestock, Object livestockCountObj) {
        Log.d(TAG, "Showing livestock details with data");
        livestockDetails.setVisibility(View.VISIBLE);

        // Set livestock type
        tvLivestock.setText(livestock != null ? livestock : "Not specified");

        // Set livestock count
        if (livestockCountObj instanceof Number) {
            int livestockCount = ((Number) livestockCountObj).intValue();
            tvNumLivestock.setText(String.valueOf(livestockCount));
        } else if (livestockCountObj != null) {
            tvNumLivestock.setText(livestockCountObj.toString());
        } else {
            tvNumLivestock.setText("Not specified");
        }

        Log.d(TAG, "Livestock details displayed - Type: " + livestock + ", Count: " + livestockCountObj);
    }

    private void displayFarmerDetails(DocumentSnapshot document) {
        try {
            Log.d(TAG, "=== DISPLAYING FARMER DETAILS ===");

            // Debug: Log all available fields
            for (String key : document.getData().keySet()) {
                Object value = document.get(key);
                Log.d(TAG, "Field '" + key + "': " + (value != null ? value.toString() : "null"));
            }

            // Farmer ID - try multiple field names
            String id = getStringFromDocument(document, "farmerId");
            if (id == null || id.isEmpty()) {
                id = getStringFromDocument(document, "id");
            }
            if (id == null || id.isEmpty()) {
                id = document.getId(); // Use document ID as fallback
            }
            tvFarmerId.setText(id != null ? id : "Not assigned");
            Log.d(TAG, "Set farmer ID: " + id);

            // Full Name - try fullName field first, then build from parts
            String fullName = getStringFromDocument(document, "fullName");
            if (fullName == null || fullName.isEmpty()) {
                String firstName = getStringFromDocument(document, "firstName");
                String lastName = getStringFromDocument(document, "lastName");
                String middleInitial = getStringFromDocument(document, "middleInitial");
                fullName = buildFullName(firstName, middleInitial, lastName);
            }
            tvFullName.setText(fullName);
            Log.d(TAG, "Set full name: " + fullName);

            // Phone Number
            String phoneNumber = getStringFromDocument(document, "phoneNumber");
            tvPhoneNumber.setText(phoneNumber != null ? phoneNumber : "Not provided");

            // Handle birthday
            Object birthdayObj = document.get("birthday");
            if (birthdayObj != null) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
                    if (birthdayObj instanceof Timestamp) {
                        tvBirthday.setText(sdf.format(((Timestamp) birthdayObj).toDate()));
                    } else if (birthdayObj instanceof String) {
                        tvBirthday.setText((String) birthdayObj);
                    } else {
                        tvBirthday.setText(birthdayObj.toString());
                    }
                } catch (Exception e) {
                    tvBirthday.setText("Not provided");
                }
            } else {
                tvBirthday.setText("Not provided");
            }

            // Address - try multiple field names and build comprehensive address
            String address = getStringFromDocument(document, "address");
            if (address == null || address.isEmpty()) {
                address = getStringFromDocument(document, "street");
            }
            if (address == null || address.isEmpty()) {
                // Try to build address from multiple fields
                String street = getStringFromDocument(document, "street");
                String municipal = getStringFromDocument(document, "municipal");
                String barangay = getStringFromDocument(document, "barangay");

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
                address = addressBuilder.length() > 0 ? addressBuilder.toString() : null;
            }
            tvAddress.setText(address != null ? address : "Not provided");
            Log.d(TAG, "Set address: " + address);

            // Last updated - will be updated from farm type documents if available
            String dateAdded = getStringFromDocument(document, "dateAdded");
            if (dateAdded == null || dateAdded.isEmpty()) {
                // Try lastUpdated from main document
                Object lastUpdatedObj = document.get("lastUpdated");
                if (lastUpdatedObj instanceof Timestamp) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
                        String formattedDate = sdf.format(((Timestamp) lastUpdatedObj).toDate());
                        tvLastUpdated.setText("Last updated: " + formattedDate);
                    } catch (Exception e) {
                        tvLastUpdated.setText("Last updated: " + new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date()));
                    }
                } else if (lastUpdatedObj != null) {
                    tvLastUpdated.setText("Last updated: " + lastUpdatedObj.toString());
                } else {
                    tvLastUpdated.setText("Last updated: " + new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date()));
                }
            } else {
                tvLastUpdated.setText("Added: " + dateAdded);
            }

            Log.d(TAG, "Basic farmer details displayed successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error displaying farmer details", e);
            Toast.makeText(this, "Error displaying farmer information", Toast.LENGTH_LONG).show();
        }
    }

    private String buildFullName(String firstName, String middleInitial, String lastName) {
        StringBuilder fullName = new StringBuilder();

        if (firstName != null && !firstName.isEmpty()) {
            fullName.append(firstName);
        }

        if (middleInitial != null && !middleInitial.isEmpty()) {
            if (fullName.length() > 0) fullName.append(" ");
            fullName.append(middleInitial);
            if (!middleInitial.endsWith(".")) fullName.append(".");
        }

        if (lastName != null && !lastName.isEmpty()) {
            if (fullName.length() > 0) fullName.append(" ");
            fullName.append(lastName);
        }

        return fullName.length() > 0 ? fullName.toString() : "Name not available";
    }

    private String getStringFromDocument(DocumentSnapshot document, String field) {
        Object value = document.get(field);
        return value != null ? value.toString() : null;
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}
