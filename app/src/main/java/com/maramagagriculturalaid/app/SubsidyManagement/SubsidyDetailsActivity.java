package com.maramagagriculturalaid.app.SubsidyManagement;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.maramagagriculturalaid.app.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SubsidyDetailsActivity extends AppCompatActivity {

    private static final String TAG = "SubsidyDetailActivity";

    // UI Components - matching XML layout
    private TextView tvStatus, tvDate, tvSubsidyType, tvFarmerId, tvFarmerName;
    private TextView tvFarmType, tvLocation, tvCrops, tvLotSize;
    private TextView tvNoFiles, tvFileName;
    private LinearLayout fileItem;
    private CardView farmerDetailsCard;
    private Button btnDeleteApplication;
    private ImageButton btnDownloadFile;
    private ImageButton btnBack;

    // Labels for dynamic hiding/showing
    private TextView cropsLabel, lotSizeLabel;

    // Subsidy details CardView and components
    private CardView subsidyDetailsContainer;
    private TextView tvSubsidyDetailsTitle;
    private TextView tvSeedType, tvFertilizerType, tvLivestockSupportType;
    private TextView seedTypeLabel, fertilizerTypeLabel, livestockSupportLabel;

    // Firebase
    private FirebaseFirestore db;
    private String subsidyId;
    private String barangay;
    private String attachmentUrl;

    // Data storage
    private DocumentSnapshot subsidyDocument;
    private Map<String, Object> farmerData = new HashMap<>();
    private boolean hasCrops = false;
    private boolean hasLivestock = false;
    private boolean hasEmbeddedFarmerData = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subsidy_details);

        // Validate intent data first
        if (!validateIntentData()) {
            return;
        }

        db = FirebaseFirestore.getInstance();
        subsidyId = getIntent().getStringExtra("subsidyId");
        barangay = getIntent().getStringExtra("barangay");

        initializeViews();
        setupListeners();
        loadSubsidyData();
    }

    private boolean validateIntentData() {
        subsidyId = getIntent().getStringExtra("subsidyId");
        barangay = getIntent().getStringExtra("barangay");

        if (subsidyId == null || subsidyId.isEmpty() || barangay == null || barangay.isEmpty()) {
            Toast.makeText(this, "Error: Missing required data", Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }
        return true;
    }

    private void initializeViews() {
        try {
            // Basic info views (matching XML IDs exactly)
            tvStatus = findViewById(R.id.tv_status);
            tvDate = findViewById(R.id.tv_date);
            tvSubsidyType = findViewById(R.id.tv_subsidy_type);
            tvFarmerId = findViewById(R.id.tv_farmer_id);
            tvFarmerName = findViewById(R.id.tv_farmer_name);
            tvFarmType = findViewById(R.id.tv_farm_type);
            tvLocation = findViewById(R.id.tv_location);

            // Crop details views (these exist in XML)
            tvCrops = findViewById(R.id.tv_crops);
            tvLotSize = findViewById(R.id.tv_lot_size);
            cropsLabel = findViewById(R.id.crops_label);
            lotSizeLabel = findViewById(R.id.lot_size_label);

            // File related views
            tvNoFiles = findViewById(R.id.tv_no_files);
            tvFileName = findViewById(R.id.tv_file_name);
            fileItem = findViewById(R.id.file_item);

            // Container views
            farmerDetailsCard = findViewById(R.id.farmer_details_card);

            // Button views
            btnDeleteApplication = findViewById(R.id.btn_delete_application);
            btnBack = findViewById(R.id.btn_back);
            btnDownloadFile = findViewById(R.id.btn_download_file);

            // Initialize subsidy details views from XML
            initializeSubsidyDetailsViews();

            // Validate critical views
            if (tvStatus == null || tvDate == null || tvSubsidyType == null ||
                    tvFarmerId == null || tvFarmerName == null || btnBack == null) {
                throw new RuntimeException("Critical UI components not found in layout");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            Toast.makeText(this, "Error initializing interface", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeSubsidyDetailsViews() {
        // Get subsidy details views from XML (now CardView)
        subsidyDetailsContainer = findViewById(R.id.subsidy_details_container);
        tvSubsidyDetailsTitle = findViewById(R.id.tv_subsidy_details_title);

        // Seed and Fertilizer views
        tvSeedType = findViewById(R.id.tv_seed_type);
        tvFertilizerType = findViewById(R.id.tv_fertilizer_type);
        seedTypeLabel = findViewById(R.id.seed_type_label);
        fertilizerTypeLabel = findViewById(R.id.fertilizer_type_label);

        // Livestock support views
        tvLivestockSupportType = findViewById(R.id.tv_livestock_support_type);
        livestockSupportLabel = findViewById(R.id.livestock_support_label);

        // Log if views are found
        Log.d(TAG, "Subsidy details container found: " + (subsidyDetailsContainer != null));
        Log.d(TAG, "Seed type views found: " + (tvSeedType != null && seedTypeLabel != null));
        Log.d(TAG, "Fertilizer type views found: " + (tvFertilizerType != null && fertilizerTypeLabel != null));
        Log.d(TAG, "Livestock support views found: " + (tvLivestockSupportType != null && livestockSupportLabel != null));
    }

    private void setupListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnDeleteApplication != null) {
            btnDeleteApplication.setOnClickListener(v -> confirmDelete());
        }

        if (btnDownloadFile != null) {
            btnDownloadFile.setOnClickListener(v -> downloadFile());
        }
    }

    private void downloadFile() {
        if (attachmentUrl != null && !attachmentUrl.isEmpty()) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(attachmentUrl));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error opening file", e);
                Toast.makeText(this, "Unable to open file", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No file to download", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadSubsidyData() {
        if (db == null || barangay == null || subsidyId == null) {
            Toast.makeText(this, "Error: Invalid data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Loading subsidy data for ID: " + subsidyId + " in barangay: " + barangay);

        db.collection("Barangays").document(barangay)
                .collection("SubsidyRequests").document(subsidyId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Log.d(TAG, "Subsidy document found");
                        subsidyDocument = documentSnapshot;

                        // First update UI with subsidy data
                        updateSubsidyUI(documentSnapshot);

                        // Check if farmer data is embedded in the subsidy document
                        checkForEmbeddedFarmerData(documentSnapshot);

                    } else {
                        Toast.makeText(this, "Subsidy request not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading subsidy data", e);
                    Toast.makeText(this, "Error loading data", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void checkForEmbeddedFarmerData(DocumentSnapshot document) {
        // Check if farmer details are embedded directly in the subsidy document
        String embeddedFarmType = document.getString("farmType");
        String embeddedLocation = document.getString("location");
        String embeddedLivestock = document.getString("livestock");
        String embeddedLivestockCount = document.getString("livestockCount");
        String embeddedCrops = document.getString("crops");
        String embeddedLotSize = document.getString("lotSize");

        Log.d(TAG, "=== CHECKING FOR EMBEDDED FARMER DATA ===");
        Log.d(TAG, "Embedded Farm Type: " + embeddedFarmType);
        Log.d(TAG, "Embedded Location: " + embeddedLocation);
        Log.d(TAG, "Embedded Livestock: " + embeddedLivestock);
        Log.d(TAG, "Embedded Livestock Count: " + embeddedLivestockCount);
        Log.d(TAG, "Embedded Crops: " + embeddedCrops);
        Log.d(TAG, "Embedded Lot Size: " + embeddedLotSize);

        // If any farmer-specific data is found in the subsidy document, use it
        if ((embeddedFarmType != null && !embeddedFarmType.isEmpty()) ||
                (embeddedLocation != null && !embeddedLocation.isEmpty()) ||
                (embeddedLivestock != null && !embeddedLivestock.isEmpty()) ||
                (embeddedCrops != null && !embeddedCrops.isEmpty())) {

            Log.d(TAG, "Found embedded farmer data, using it directly");
            hasEmbeddedFarmerData = true;

            // Store embedded data in farmerData map
            if (embeddedFarmType != null && !embeddedFarmType.isEmpty()) {
                farmerData.put("farmType", embeddedFarmType);

                // Set flags based on farm type
                if (embeddedFarmType.equalsIgnoreCase("Livestock")) {
                    hasLivestock = true;
                } else if (embeddedFarmType.equalsIgnoreCase("Crop") || embeddedFarmType.equalsIgnoreCase("Crops")) {
                    hasCrops = true;
                }
            }

            if (embeddedLocation != null && !embeddedLocation.isEmpty()) {
                farmerData.put("location", embeddedLocation);
            }

            if (embeddedLivestock != null && !embeddedLivestock.isEmpty()) {
                farmerData.put("livestock", embeddedLivestock);
                hasLivestock = true;
            }

            if (embeddedLivestockCount != null && !embeddedLivestockCount.isEmpty()) {
                farmerData.put("livestockCount", embeddedLivestockCount);
            }

            if (embeddedCrops != null && !embeddedCrops.isEmpty()) {
                farmerData.put("crops", embeddedCrops);
                hasCrops = true;
            }

            if (embeddedLotSize != null && !embeddedLotSize.isEmpty()) {
                farmerData.put("lotSize", embeddedLotSize);
            }

            // Update UI immediately with embedded data
            updateFarmerDetailsUI();

        } else {
            Log.d(TAG, "No embedded farmer data found, loading from Farmers collection");
            hasEmbeddedFarmerData = false;

            // Load farmer details from separate collection
            String farmerId = document.getString("farmerId");
            if (farmerId != null && !farmerId.isEmpty()) {
                loadFarmerData(farmerId);
            } else {
                Log.e(TAG, "No farmerId found in subsidy document");
                setDefaultFarmerData();
            }
        }
    }

    private void updateSubsidyUI(DocumentSnapshot document) {
        if (document == null) return;

        try {
            Log.d(TAG, "=== UPDATING SUBSIDY UI ===");

            // Set common fields from subsidy document
            setStatus(document.getString("status"));
            setDate(document.getLong("timestamp"));

            safeSetText(tvFarmerId, document.getString("farmerId"));
            safeSetText(tvFarmerName, document.getString("farmerName"));

            // Handle subsidy type specific display
            String subsidyType = document.getString("supportType");
            safeSetText(tvSubsidyType, subsidyType);

            Log.d(TAG, "Subsidy Type: " + subsidyType);

            // Handle different subsidy types and their specific details
            handleSubsidyTypeDisplay(document, subsidyType);

            // Handle attachment
            updateAttachmentDisplay(document);

        } catch (Exception e) {
            Log.e(TAG, "Error updating subsidy UI", e);
            Toast.makeText(this, "Error displaying subsidy data", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadFarmerData(String farmerId) {
        Log.d(TAG, "Loading farmer data for ID: " + farmerId);

        db.collection("Barangays").document(barangay)
                .collection("Farmers")
                .whereEqualTo("farmerId", farmerId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            DocumentSnapshot farmerDoc = task.getResult().getDocuments().get(0);
                            String farmerDocId = farmerDoc.getId();
                            farmerData = new HashMap<>(farmerDoc.getData());

                            Log.d(TAG, "Farmer document found: " + farmerDocId);

                            // Reset flags
                            hasCrops = false;
                            hasLivestock = false;

                            // Load farm type data from the nested structure
                            loadFarmTypeData(farmerDocId);
                        } else {
                            Log.e(TAG, "No farmer found with ID: " + farmerId);
                            // Set default values for farmer details
                            setDefaultFarmerData();
                        }
                    } else {
                        Log.e(TAG, "Error searching for farmer", task.getException());
                        setDefaultFarmerData();
                    }
                });
    }

    private void loadFarmTypeData(String farmerDocId) {
        Log.d(TAG, "Loading farm type data for farmer: " + farmerDocId);

        // Load FarmType subcollection data
        db.collection("Barangays").document(barangay)
                .collection("Farmers").document(farmerDocId)
                .collection("FarmType")
                .get()
                .addOnCompleteListener(farmTypeTask -> {
                    if (farmTypeTask.isSuccessful() && !farmTypeTask.getResult().isEmpty()) {
                        StringBuilder farmTypeBuilder = new StringBuilder();

                        for (DocumentSnapshot farmTypeDoc : farmTypeTask.getResult().getDocuments()) {
                            String farmTypeId = farmTypeDoc.getId(); // This will be "Crop", "Livestock", etc.
                            Map<String, Object> farmTypeData = farmTypeDoc.getData();

                            Log.d(TAG, "Found farm type: " + farmTypeId);
                            Log.d(TAG, "Farm type data: " + farmTypeData);

                            // Add to farm type display
                            if (farmTypeBuilder.length() > 0) {
                                farmTypeBuilder.append(", ");
                            }
                            farmTypeBuilder.append(farmTypeId);

                            // Extract location data for ALL farm types
                            if (farmTypeData != null) {
                                String street = getFieldValue(farmTypeData, "street", "streetAddress");
                                String barangayName = getFieldValue(farmTypeData, "barangay", "brgy");
                                String municipal = getFieldValue(farmTypeData, "municipal", "municipality");

                                StringBuilder locationBuilder = new StringBuilder();
                                if (street != null && !street.isEmpty()) {
                                    locationBuilder.append(street);
                                }
                                if (barangayName != null && !barangayName.isEmpty()) {
                                    if (locationBuilder.length() > 0) locationBuilder.append(", ");
                                    locationBuilder.append(barangayName);
                                }
                                if (municipal != null && !municipal.isEmpty()) {
                                    if (locationBuilder.length() > 0) locationBuilder.append(", ");
                                    locationBuilder.append(municipal);
                                }

                                if (locationBuilder.length() > 0) {
                                    farmerData.put("location", locationBuilder.toString());
                                }
                            }

                            // Set flags and extract specific data based on farm type
                            if (farmTypeId.equalsIgnoreCase("Crop") || farmTypeId.equalsIgnoreCase("Crops")) {
                                hasCrops = true;

                                // Extract crop-specific data
                                if (farmTypeData != null) {
                                    String cropsGrown = getFieldValue(farmTypeData, "cropsGrown", "crops", "crops_grown");
                                    if (cropsGrown != null) {
                                        farmerData.put("crops", cropsGrown);
                                    }

                                    // Handle lot size - combine value and unit
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
                                        combinedLotSize = getFieldValue(farmTypeData, "lotSize", "lot_size");
                                    }

                                    if (combinedLotSize != null && !combinedLotSize.isEmpty()) {
                                        farmerData.put("lotSize", combinedLotSize);
                                    }
                                }

                            } else if (farmTypeId.equalsIgnoreCase("Livestock")) {
                                hasLivestock = true;

                                // Extract livestock-specific data
                                if (farmTypeData != null) {
                                    String livestock = getFieldValue(farmTypeData, "livestockType", "livestock", "animalType", "animal", "livestockName");
                                    if (livestock != null) {
                                        farmerData.put("livestock", livestock);
                                        Log.d(TAG, "Livestock type found: " + livestock);
                                    }

                                    String livestockCount = getFieldValue(farmTypeData, "livestockCount", "numLivestock", "numberOfLivestock", "livestock_count", "animalCount", "count", "quantity", "livestockNumber", "totalLivestock");
                                    if (livestockCount != null) {
                                        farmerData.put("livestockCount", livestockCount);
                                        Log.d(TAG, "Livestock count found: " + livestockCount);
                                    }

                                    // Also try to get lot size for livestock farms
                                    String lotSizeValue = getFieldValue(farmTypeData, "lotSizeValue", "lotSize", "lot_size", "farmSize");
                                    String lotSizeUnit = getFieldValue(farmTypeData, "lotSizeUnit", "unit", "sizeUnit");
                                    String combinedLotSize = "";

                                    if (lotSizeValue != null && !lotSizeValue.isEmpty()) {
                                        combinedLotSize = lotSizeValue;
                                        if (lotSizeUnit != null && !lotSizeUnit.isEmpty()) {
                                            combinedLotSize += " " + lotSizeUnit;
                                        }
                                    } else {
                                        combinedLotSize = getFieldValue(farmTypeData, "lotSize", "lot_size", "farmSize");
                                    }

                                    if (combinedLotSize != null && !combinedLotSize.isEmpty()) {
                                        farmerData.put("lotSize", combinedLotSize);
                                    }
                                }
                            }
                        }

                        // Set the combined farm type
                        if (farmTypeBuilder.length() > 0) {
                            farmerData.put("farmType", farmTypeBuilder.toString());
                        }
                    } else {
                        Log.d(TAG, "No farm type data found");
                        farmerData.put("farmType", "Not specified");
                    }

                    // Update the farmer details UI
                    updateFarmerDetailsUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading farm type data", e);
                    farmerData.put("farmType", "Error loading farm type");
                    updateFarmerDetailsUI();
                });
    }

    private String getFieldValue(Map<String, Object> data, String... possibleKeys) {
        if (data == null) return null;

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
        Log.d(TAG, "No value found for any of the keys: " + java.util.Arrays.toString(possibleKeys));
        return null;
    }

    private void setDefaultFarmerData() {
        farmerData.put("farmType", "Not specified");
        farmerData.put("location", "Not specified");
        farmerData.put("crops", "Not specified");
        farmerData.put("lotSize", "Not specified");
        farmerData.put("livestock", "Not specified");
        farmerData.put("livestockCount", "Not specified");
        updateFarmerDetailsUI();
    }

    private void updateFarmerDetailsUI() {
        runOnUiThread(() -> {
            try {
                Log.d(TAG, "=== UPDATING FARMER DETAILS UI ===");
                Log.d(TAG, "Has Embedded Data: " + hasEmbeddedFarmerData);
                Log.d(TAG, "Has Livestock Flag: " + hasLivestock);
                Log.d(TAG, "Has Crops Flag: " + hasCrops);

                // Set farm details
                String farmType = getFieldValue(farmerData, "farmType");
                String location = getFieldValue(farmerData, "location");

                safeSetText(tvFarmType, farmType);
                safeSetText(tvLocation, location);

                // Handle crops data
                String crops = getFieldValue(farmerData, "crops", "cropsGrown");
                safeSetText(tvCrops, crops);

                // Handle lot size
                String lotSize = getFieldValue(farmerData, "lotSize", "lot_size");
                safeSetText(tvLotSize, lotSize);

                Log.d(TAG, "Farm Type: " + farmType);
                Log.d(TAG, "Location: " + location);
                Log.d(TAG, "Crops: " + crops);
                Log.d(TAG, "Lot Size: " + lotSize);

                // Handle livestock data
                String livestock = getFieldValue(farmerData, "livestock", "livestockType");
                String livestockCount = getFieldValue(farmerData, "livestockCount", "numLivestock");

                Log.d(TAG, "Livestock: " + livestock);
                Log.d(TAG, "Livestock Count: " + livestockCount);

                // If we have embedded data and it's livestock type, update the display accordingly
                if (hasEmbeddedFarmerData && hasLivestock) {
                    Log.d(TAG, "Updating livestock display for embedded data");
                    // The handleSubsidyTypeDisplay will be called again to properly show livestock data
                    if (subsidyDocument != null) {
                        String subsidyType = subsidyDocument.getString("supportType");
                        // Force livestock support display if we have livestock data
                        if (livestock != null && !livestock.isEmpty()) {
                            handleSubsidyTypeDisplay(subsidyDocument, "Livestock Support");
                        } else {
                            handleSubsidyTypeDisplay(subsidyDocument, subsidyType);
                        }
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error updating farmer details UI", e);
                Toast.makeText(SubsidyDetailsActivity.this, "Error displaying farmer data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleSubsidyTypeDisplay(DocumentSnapshot document, String subsidyType) {
        // Hide all subsidy-specific details first
        hideAllSubsidyDetails();

        if (subsidyType == null) {
            hideFarmerDetails();
            return;
        }

        // Check if we have livestock data regardless of the stated subsidy type
        String livestock = getFieldValue(farmerData, "livestock", "livestockType");
        String livestockCount = getFieldValue(farmerData, "livestockCount", "numLivestock");

        Log.d(TAG, "=== HANDLING SUBSIDY TYPE DISPLAY ===");
        Log.d(TAG, "Stated Subsidy Type: " + subsidyType);
        Log.d(TAG, "Has Livestock Data: " + (livestock != null && !livestock.isEmpty()));
        Log.d(TAG, "Livestock: " + livestock);
        Log.d(TAG, "Livestock Count: " + livestockCount);

        // If we have livestock data, treat it as livestock support regardless of stated type
        if (livestock != null && !livestock.isEmpty()) {
            Log.d(TAG, "Displaying as livestock support due to livestock data");
            handleLivestockSupport(document);
            showLivestockSupportDetails(document);
        } else {
            switch (subsidyType) {
                case "Livestock Support":
                    handleLivestockSupport(document);
                    showLivestockSupportDetails(document);
                    break;
                case "Seeds and Fertilizers":
                    handleCropSupport(document);
                    showSeedsAndFertilizersDetails(document);
                    break;
                case "Monetary Support":
                default:
                    // For monetary support, show basic farmer details but hide crop-specific fields
                    showBasicFarmerDetails();
                    break;
            }
        }
    }

    private void showSeedsAndFertilizersDetails(DocumentSnapshot document) {
        // Show subsidy details container (CardView)
        safeSetVisibility(subsidyDetailsContainer, View.VISIBLE);

        boolean hasDetails = false;

        // Show seed type
        String seedType = document.getString("seedType");
        if (seedType != null && !seedType.isEmpty()) {
            safeSetVisibility(seedTypeLabel, View.VISIBLE);
            safeSetVisibility(tvSeedType, View.VISIBLE);
            safeSetText(tvSeedType, seedType);
            hasDetails = true;
            Log.d(TAG, "Showing seed type: " + seedType);
        }

        // Show fertilizer type
        String fertilizerType = document.getString("fertilizerType");
        if (fertilizerType != null && !fertilizerType.isEmpty()) {
            safeSetVisibility(fertilizerTypeLabel, View.VISIBLE);
            safeSetVisibility(tvFertilizerType, View.VISIBLE);
            safeSetText(tvFertilizerType, fertilizerType);
            hasDetails = true;
            Log.d(TAG, "Showing fertilizer type: " + fertilizerType);
        }

        // Hide container if no details to show
        if (!hasDetails) {
            safeSetVisibility(subsidyDetailsContainer, View.GONE);
        }

        Log.d(TAG, "Seeds and Fertilizers - Seed Type: " + seedType + ", Fertilizer Type: " + fertilizerType);
    }

    private void showLivestockSupportDetails(DocumentSnapshot document) {
        // Show subsidy details container (CardView)
        safeSetVisibility(subsidyDetailsContainer, View.VISIBLE);

        // Show livestock support type
        String livestockSupportType = document.getString("livestockSupportType");
        if (livestockSupportType != null && !livestockSupportType.isEmpty()) {
            safeSetVisibility(livestockSupportLabel, View.VISIBLE);
            safeSetVisibility(tvLivestockSupportType, View.VISIBLE);
            safeSetText(tvLivestockSupportType, livestockSupportType);
            Log.d(TAG, "Showing livestock support type: " + livestockSupportType);
        } else {
            // Hide container if no livestock support type
            safeSetVisibility(subsidyDetailsContainer, View.GONE);
        }

        Log.d(TAG, "Livestock Support - Support Type: " + livestockSupportType);
    }

    private void hideAllSubsidyDetails() {
        // Hide seed and fertilizer details
        safeSetVisibility(seedTypeLabel, View.GONE);
        safeSetVisibility(tvSeedType, View.GONE);
        safeSetVisibility(fertilizerTypeLabel, View.GONE);
        safeSetVisibility(tvFertilizerType, View.GONE);

        // Hide livestock support details
        safeSetVisibility(livestockSupportLabel, View.GONE);
        safeSetVisibility(tvLivestockSupportType, View.GONE);

        // Hide the entire container (CardView)
        safeSetVisibility(subsidyDetailsContainer, View.GONE);
    }

    private void handleLivestockSupport(DocumentSnapshot document) {
        // Show farmer details but adapt labels for livestock
        showBasicFarmerDetails();

        // Use the crops field to show livestock type from farmer data
        String livestock = getFieldValue(farmerData, "livestock", "livestockType");
        if (livestock != null && !livestock.isEmpty()) {
            safeSetVisibility(cropsLabel, View.VISIBLE);
            safeSetVisibility(tvCrops, View.VISIBLE);
            if (cropsLabel != null) cropsLabel.setText("Livestock Type");
            safeSetText(tvCrops, livestock);
            Log.d(TAG, "Displaying livestock type: " + livestock);
        }

        // Use the lot size field to show livestock count from farmer data
        String livestockCount = getFieldValue(farmerData, "livestockCount", "numLivestock");
        if (livestockCount != null && !livestockCount.isEmpty()) {
            safeSetVisibility(lotSizeLabel, View.VISIBLE);
            safeSetVisibility(tvLotSize, View.VISIBLE);
            if (lotSizeLabel != null) lotSizeLabel.setText("Livestock Count");
            safeSetText(tvLotSize, livestockCount);
            Log.d(TAG, "Displaying livestock count: " + livestockCount);
        }
    }

    private void handleCropSupport(DocumentSnapshot document) {
        // Show crop-specific fields with original labels
        showCropFields();

        // Set crops data from farmer data
        String crops = getFieldValue(farmerData, "crops", "cropsGrown");
        safeSetText(tvCrops, crops);

        String lotSize = getFieldValue(farmerData, "lotSize", "lot_size");
        safeSetText(tvLotSize, lotSize);
    }

    private void showBasicFarmerDetails() {
        // Show basic farmer info but hide crop-specific fields
        hideFarmerDetails();
    }

    private void showCropFields() {
        safeSetVisibility(cropsLabel, View.VISIBLE);
        safeSetVisibility(tvCrops, View.VISIBLE);
        safeSetVisibility(lotSizeLabel, View.VISIBLE);
        safeSetVisibility(tvLotSize, View.VISIBLE);

        // Reset labels to original text
        if (cropsLabel != null) cropsLabel.setText("Crops");
        if (lotSizeLabel != null) lotSizeLabel.setText("Lot Size");
    }

    private void hideFarmerDetails() {
        safeSetVisibility(cropsLabel, View.GONE);
        safeSetVisibility(tvCrops, View.GONE);
        safeSetVisibility(lotSizeLabel, View.GONE);
        safeSetVisibility(tvLotSize, View.GONE);
    }

    private void safeSetText(TextView textView, String text) {
        if (textView != null) {
            textView.setText(text != null && !text.isEmpty() ? text : "Not specified");
        }
    }

    private void safeSetVisibility(View view, int visibility) {
        if (view != null) {
            view.setVisibility(visibility);
        }
    }

    private void updateAttachmentDisplay(DocumentSnapshot document) {
        // Try multiple possible field names for attachment URL
        attachmentUrl = document.getString("supportingDocumentUrl");
        if (attachmentUrl == null || attachmentUrl.isEmpty()) {
            attachmentUrl = document.getString("attachmentUrl");
        }

        if (attachmentUrl != null && !attachmentUrl.isEmpty()) {
            safeSetVisibility(fileItem, View.VISIBLE);
            safeSetVisibility(tvNoFiles, View.GONE);

            // Try to get the original filename
            String fileName = document.getString("supportingDocumentName");
            if (fileName == null || fileName.isEmpty()) {
                fileName = getFileNameFromUrl(attachmentUrl);
            }
            safeSetText(tvFileName, fileName);
        } else {
            safeSetVisibility(fileItem, View.GONE);
            safeSetVisibility(tvNoFiles, View.VISIBLE);
        }
    }

    private void setStatus(String status) {
        if (status == null || tvStatus == null) return;

        tvStatus.setText(status);
        int backgroundResId;
        int textColorResId;

        switch (status.toLowerCase().trim()) {
            case "approved":
                backgroundResId = R.drawable.status_approved_background;
                textColorResId = R.color.ButtonGreen;
                safeSetVisibility(btnDeleteApplication, View.GONE);
                break;
            case "rejected":
                backgroundResId = R.drawable.status_rejected_background;
                textColorResId = R.color.red;
                safeSetVisibility(btnDeleteApplication, View.GONE);
                break;
            default: // Pending
                backgroundResId = R.drawable.status_pending_background;
                textColorResId = android.R.color.black;
                safeSetVisibility(btnDeleteApplication, View.VISIBLE);
                break;
        }

        try {
            tvStatus.setBackgroundResource(backgroundResId);
            tvStatus.setTextColor(getResources().getColor(textColorResId));
        } catch (Exception e) {
            Log.e(TAG, "Error setting status appearance", e);
        }
    }

    private void setDate(Long timestamp) {
        if (timestamp == null || tvDate == null) return;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
            tvDate.setText(sdf.format(new Date(timestamp)));
        } catch (Exception e) {
            Log.e(TAG, "Error formatting date", e);
            tvDate.setText("Date not available");
        }
    }

    private String getFileNameFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "Unknown file";
        }

        try {
            int lastSlashIndex = url.lastIndexOf('/');
            if (lastSlashIndex != -1 && lastSlashIndex < url.length() - 1) {
                String fileName = url.substring(lastSlashIndex + 1);
                // Remove URL parameters if any
                int questionMarkIndex = fileName.indexOf('?');
                if (questionMarkIndex != -1) {
                    fileName = fileName.substring(0, questionMarkIndex);
                }
                return fileName;
            }
            return "Unknown file";
        } catch (Exception e) {
            Log.e(TAG, "Error extracting filename from URL", e);
            return "Unknown file";
        }
    }

    private void confirmDelete() {
        try {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Application")
                    .setMessage("Are you sure you want to delete this subsidy application? This action cannot be undone.")
                    .setPositiveButton("Delete", (dialog, which) -> deleteApplication())
                    .setNegativeButton("Cancel", null)
                    .setCancelable(true)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing delete confirmation", e);
            Toast.makeText(this, "Error showing confirmation dialog", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteApplication() {
        if (db == null || barangay == null || subsidyId == null) {
            Toast.makeText(this, "Error: Invalid data for deletion", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable delete button to prevent multiple clicks
        if (btnDeleteApplication != null) {
            btnDeleteApplication.setEnabled(false);
            btnDeleteApplication.setText("Deleting...");
        }

        db.collection("Barangays").document(barangay)
                .collection("SubsidyRequests").document(subsidyId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Application deleted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting application", e);
                    Toast.makeText(this, "Error deleting application. Please try again.", Toast.LENGTH_SHORT).show();

                    // Re-enable delete button on failure
                    if (btnDeleteApplication != null) {
                        btnDeleteApplication.setEnabled(true);
                        btnDeleteApplication.setText("Delete Application");
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up references to prevent memory leaks
        db = null;
        subsidyId = null;
        barangay = null;
        attachmentUrl = null;
        farmerData.clear();
        subsidyDocument = null;
    }
}