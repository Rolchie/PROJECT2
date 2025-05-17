package com.maramagagriculturalaid.app.SubsidyManagement;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.maramagagriculturalaid.app.R;
import com.maramagagriculturalaid.app.SuccessActivities.SuccessSubsidyApplication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubsidyRequest extends AppCompatActivity {

    private static final String TAG = "SubsidyRequest";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int FILE_PICK_REQUEST_CODE = 1;

    // Firebase services
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    // UI Elements
    private TextInputEditText etFarmerId;
    private EditText etFarmerName;
    private Spinner spinnerSupport;
    private Spinner spinnerLivestockSupport;
    private Spinner spinnerSeedsType;
    private Spinner spinnerFertilizersType;
    private AppCompatTextView tvFarmType;
    private AppCompatTextView tvLocation;
    private AppCompatTextView tvCrops;
    private AppCompatTextView tvLotSize;
    private AppCompatTextView tvLivestock;
    private AppCompatTextView tvLivestockCount;
    private AppCompatButton btnFindFarmer;
    private AppCompatButton btnChooseFile;
    private AppCompatButton btnSubmit;
    private ImageButton btnBack;
    private LinearLayout loadingState;
    private TextView tvSearchStatus;
    private ConstraintLayout farmerDetailsSection;

    // Variables
    private String selectedBarangay;
    private Uri selectedFileUri;
    private TextView loadingText;
    private boolean hasCrops = false;
    private boolean hasLivestock = false;
    private Map<String, Object> currentFarmerData = new HashMap<>();

    // Custom input variables
    private String customLivestockSupport = "";
    private String customSeedType = "";

    // Callback interface for custom input
    private interface CustomInputCallback {
        void onCustomInput(String customText);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subsidy_request);

        // Initialize services
        initializeServices();

        // Initialize UI elements
        initializeViews();
        setupListeners();
        setupSpinners();
    }

    private void initializeServices() {
        try {
            Log.d(TAG, "=== SERVICES INITIALIZATION DEBUG ===");

            // Initialize Firebase services
            try {
                FirebaseApp app = FirebaseApp.getInstance();
                db = FirebaseFirestore.getInstance();
                storage = FirebaseStorage.getInstance();
                Log.d(TAG, "✓ Firebase services initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "✗ Firebase initialization failed: " + e.getMessage());
                Toast.makeText(this, "Firebase initialization failed", Toast.LENGTH_LONG).show();
                return;
            }

            // Check Firebase authentication
            try {
                FirebaseAuth auth = FirebaseAuth.getInstance();
                FirebaseUser user = auth.getCurrentUser();
                if (user != null) {
                    Log.d(TAG, "✓ User authenticated: " + user.getUid());
                } else {
                    Log.w(TAG, "⚠ User not authenticated");
                }
            } catch (Exception e) {
                Log.e(TAG, "✗ Authentication check failed: " + e.getMessage());
            }

            Log.d(TAG, "=== SERVICES INITIALIZATION COMPLETE ===");

        } catch (Exception e) {
            Log.e(TAG, "Fatal error during services initialization: " + e.getMessage());
            Toast.makeText(this, "Fatal initialization error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void initializeViews() {
        etFarmerId = findViewById(R.id.et_farmer_id);
        etFarmerName = findViewById(R.id.et_farmer_name);
        spinnerSupport = findViewById(R.id.spinner_support);
        spinnerLivestockSupport = findViewById(R.id.spinner_livestock_support);
        spinnerSeedsType = findViewById(R.id.spinner_seeds_type);
        spinnerFertilizersType = findViewById(R.id.spinner_fertilizers_type);
        tvFarmType = findViewById(R.id.tv_farm_type);
        tvLocation = findViewById(R.id.tv_location);
        tvCrops = findViewById(R.id.tv_crops);
        tvLotSize = findViewById(R.id.tv_lot_size);
        tvLivestock = findViewById(R.id.tv_livestock);
        tvLivestockCount = findViewById(R.id.tv_livestock_count);
        btnFindFarmer = findViewById(R.id.btn_find_farmer);
        btnChooseFile = findViewById(R.id.btn_choose_file);
        btnSubmit = findViewById(R.id.btn_submit);
        btnBack = findViewById(R.id.btn_back);
        loadingState = findViewById(R.id.loading_state);
        tvSearchStatus = findViewById(R.id.tv_search_status);
        farmerDetailsSection = findViewById(R.id.farmerDetailsSection);
    }

    // Custom ArrayAdapter to ensure black text color
    private class CustomSpinnerAdapter extends ArrayAdapter<String> {
        public CustomSpinnerAdapter(Context context, int resource, String[] objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            TextView textView = (TextView) view;
            textView.setTextColor(Color.BLACK);
            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View view = super.getDropDownView(position, convertView, parent);
            TextView textView = (TextView) view;
            textView.setTextColor(Color.WHITE);

            // Make the first item (prompt) appear grayed out in dropdown
            if (position == 0) {
                textView.setTextColor(Color.GRAY);
            }

            return view;
        }

        @Override
        public boolean isEnabled(int position) {
            // Disable the first item (prompt) from being selectable
            return position != 0;
        }
    }

    private void setupListeners() {
        findViewById(R.id.submit_button_container).setVisibility(View.GONE);
        btnBack.setOnClickListener(v -> finish());

        btnFindFarmer.setOnClickListener(v -> {
            String farmerId = etFarmerId.getText().toString().trim();
            fetchFarmerData(farmerId);
        });

        btnChooseFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(intent, FILE_PICK_REQUEST_CODE);
        });

        btnSubmit.setOnClickListener(v -> {
            if (validateSubmission()) {
                submitSubsidyRequest();
            }
        });

        // Support type spinner listener
        spinnerSupport.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) { // Only update if not the prompt
                    updateSupportTypeSpinners();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Livestock support spinner listener for "Others" option
        spinnerLivestockSupport.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) { // Only process if not the prompt
                    String selectedItem = parent.getItemAtPosition(position).toString();
                    if (selectedItem.equals("Others (Please Specify)")) {
                        showCustomInputDialog(
                                "Specify Livestock Support",
                                "Enter custom livestock support type",
                                new CustomInputCallback() {
                                    @Override
                                    public void onCustomInput(String customText) {
                                        if (customText != null) {
                                            customLivestockSupport = customText;
                                            Toast.makeText(SubsidyRequest.this,
                                                    "Custom livestock support: " + customText,
                                                    Toast.LENGTH_SHORT).show();
                                        } else {
                                            // Reset to prompt if cancelled
                                            spinnerLivestockSupport.setSelection(0);
                                            customLivestockSupport = "";
                                        }
                                    }
                                }
                        );
                    } else {
                        customLivestockSupport = "";
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Seeds type spinner listener for "Others" option
        spinnerSeedsType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) { // Only process if not the prompt
                    String selectedItem = parent.getItemAtPosition(position).toString();
                    if (selectedItem.equals("Others (Please Specify)")) {
                        showCustomInputDialog(
                                "Specify Seed Type",
                                "Enter custom seed type",
                                new CustomInputCallback() {
                                    @Override
                                    public void onCustomInput(String customText) {
                                        if (customText != null) {
                                            customSeedType = customText;
                                            Toast.makeText(SubsidyRequest.this,
                                                    "Custom seed type: " + customText,
                                                    Toast.LENGTH_SHORT).show();
                                        } else {
                                            // Reset to prompt if cancelled
                                            spinnerSeedsType.setSelection(0);
                                            customSeedType = "";
                                        }
                                    }
                                }
                        );
                    } else {
                        customSeedType = "";
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Fertilizers type spinner listener
        spinnerFertilizersType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // No special handling needed for fertilizers, just ensure text is black
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    // Method to show custom input dialog
    private void showCustomInputDialog(String title, String hint, final CustomInputCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        // Create input field
        final EditText input = new EditText(this);
        input.setHint(hint);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setTextColor(Color.BLACK);

        // Add some padding
        LinearLayout container = new LinearLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(50, 20, 50, 20);
        input.setLayoutParams(params);
        container.addView(input);

        builder.setView(container);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String customText = input.getText().toString().trim();
            if (!customText.isEmpty()) {
                callback.onCustomInput(customText);
            } else {
                Toast.makeText(SubsidyRequest.this, "Please enter a value", Toast.LENGTH_SHORT).show();
                // Reset spinner to prompt if cancelled
                callback.onCustomInput(null);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.cancel();
            // Reset spinner to prompt if cancelled
            callback.onCustomInput(null);
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void setupSpinners() {
        // We'll update the main support spinner dynamically based on farm type
        updateMainSupportSpinner();

        // Livestock support spinner with prompt
        String[] livestockSupportTypes = {
                "Select Livestock Support Type",
                "Veterinary Services",
                "Feed and Nutrition Support",
                "Breeding and Genetic Improvement",
                "Infrastructure and Equipment",
                "Training and Extension Services",
                "Marketing and Value Chain Development",
                "Others (Please Specify)"
        };
        CustomSpinnerAdapter livestockAdapter = new CustomSpinnerAdapter(this, android.R.layout.simple_spinner_item, livestockSupportTypes);
        livestockAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLivestockSupport.setAdapter(livestockAdapter);

        // Seeds type spinner with prompt
        String[] seedsTypes = {
                "Select Seed Type",
                "Rice Seeds",
                "Soybean Seeds",
                "Vegetable Seeds",
                "Others (Please Specify)"
        };
        CustomSpinnerAdapter seedsAdapter = new CustomSpinnerAdapter(this, android.R.layout.simple_spinner_item, seedsTypes);
        seedsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSeedsType.setAdapter(seedsAdapter);

        // Fertilizers type spinner with prompt
        String[] fertilizersTypes = {
                "Select Fertilizer Type",
                "Organic Fertilizers",
                "Inorganic Fertilizers"
        };
        CustomSpinnerAdapter fertilizersAdapter = new CustomSpinnerAdapter(this, android.R.layout.simple_spinner_item, fertilizersTypes);
        fertilizersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFertilizersType.setAdapter(fertilizersAdapter);
    }

    private void updateMainSupportSpinner() {
        List<String> supportTypes = new ArrayList<>();
        supportTypes.add("Select Support Type"); // Add prompt

        if (hasCrops) {
            supportTypes.add("Seeds and Fertilizers");
        }
        if (hasLivestock) {
            supportTypes.add("Livestock Support");
        }
        // Monetary support is always available
        supportTypes.add("Monetary Support");

        String[] supportArray = supportTypes.toArray(new String[0]);
        CustomSpinnerAdapter supportAdapter = new CustomSpinnerAdapter(this, android.R.layout.simple_spinner_item, supportArray);
        supportAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSupport.setAdapter(supportAdapter);
    }

    private void updateSupportTypeSpinners() {
        if (spinnerSupport.getSelectedItemPosition() <= 0) {
            return; // Don't update if prompt is selected
        }

        String selectedSupport = spinnerSupport.getSelectedItem().toString();

        // Hide all secondary spinners first
        spinnerLivestockSupport.setVisibility(View.GONE);
        spinnerSeedsType.setVisibility(View.GONE);
        spinnerFertilizersType.setVisibility(View.GONE);

        // Reset secondary spinners to prompt
        spinnerLivestockSupport.setSelection(0);
        spinnerSeedsType.setSelection(0);
        spinnerFertilizersType.setSelection(0);

        // Adjust the farm details label position
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) findViewById(R.id.tv_farm_details_label).getLayoutParams();

        if (selectedSupport.equals("Livestock Support")) {
            spinnerLivestockSupport.setVisibility(View.VISIBLE);
            params.topToBottom = R.id.spinner_livestock_support;
        } else if (selectedSupport.equals("Seeds and Fertilizers")) {
            spinnerSeedsType.setVisibility(View.VISIBLE);
            spinnerFertilizersType.setVisibility(View.VISIBLE);
            params.topToBottom = R.id.spinner_fertilizers_type;
        } else {
            // For Monetary Support or when no specific support is selected
            params.topToBottom = R.id.spinner_support;
        }

        findViewById(R.id.tv_farm_details_label).setLayoutParams(params);
    }

    private boolean validateSubmission() {
        boolean isValid = true;

        String farmerId = etFarmerId.getText().toString().trim();
        if (farmerId.length() != 5) {
            etFarmerId.setError("Farmer ID must be exactly 5 digits");
            isValid = false;
        }
        // Check if farmer data is loaded
        if (farmerDetailsSection.getVisibility() != View.VISIBLE) {
            Toast.makeText(this, "Please find a farmer first", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check if support type is selected (not the prompt)
        if (spinnerSupport.getSelectedItemPosition() <= 0) {
            Toast.makeText(this, "Please select a support type", Toast.LENGTH_SHORT).show();
            return false;
        }

        String selectedSupport = spinnerSupport.getSelectedItem().toString();

        // Validate livestock support selection
        if (selectedSupport.equals("Livestock Support") && hasLivestock) {
            if (spinnerLivestockSupport.getSelectedItemPosition() <= 0) {
                Toast.makeText(this, "Please select a livestock support type", Toast.LENGTH_SHORT).show();
                return false;
            }
            // Check if "Others" is selected but no custom input provided
            String livestockSupportType = spinnerLivestockSupport.getSelectedItem().toString();
            if (livestockSupportType.equals("Others (Please Specify)") && customLivestockSupport.isEmpty()) {
                Toast.makeText(this, "Please specify the livestock support type", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        // Validate seeds and fertilizers selection
        if (selectedSupport.equals("Seeds and Fertilizers") && hasCrops) {
            if (spinnerSeedsType.getSelectedItemPosition() <= 0) {
                Toast.makeText(this, "Please select a seed type", Toast.LENGTH_SHORT).show();
                return false;
            }
            // Check if "Others" is selected but no custom input provided
            String seedType = spinnerSeedsType.getSelectedItem().toString();
            if (seedType.equals("Others (Please Specify)") && customSeedType.isEmpty()) {
                Toast.makeText(this, "Please specify the seed type", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (spinnerFertilizersType.getSelectedItemPosition() <= 0) {
                Toast.makeText(this, "Please select a fertilizer type", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        // Check if file is selected (required)
        if (selectedFileUri == null) {
            Toast.makeText(this, "Please select a file", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check file size if a file is selected
        if (selectedFileUri != null) {
            try {
                android.database.Cursor cursor = getContentResolver().query(selectedFileUri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                    long fileSize = cursor.getLong(sizeIndex);
                    cursor.close();

                    if (fileSize > MAX_FILE_SIZE) {
                        Toast.makeText(this, "File is too large. Please select a file smaller than 10MB.", Toast.LENGTH_LONG).show();
                        return false;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking file size", e);
                Toast.makeText(this, "Error checking file size", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        // Check network connectivity
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection available", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private void fetchFarmerData(String farmerId) {
        tvSearchStatus.setVisibility(View.GONE);
        farmerDetailsSection.setVisibility(View.GONE);
        findViewById(R.id.submit_button_container).setVisibility(View.GONE);

        btnFindFarmer.setVisibility(View.INVISIBLE);
        loadingState.setVisibility(View.VISIBLE);

        if (farmerId == null || farmerId.trim().isEmpty()) {
            showSearchError("Please enter a Farmer ID");
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showSearchError("User not authenticated");
            return;
        }

        db.collection("Users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(userDocument -> {
                    if (userDocument.exists()) {
                        String userRole = userDocument.getString("Role");
                        String barangay = userDocument.getString("Barangay");

                        if (barangay == null || barangay.trim().isEmpty()) {
                            showSearchError("Barangay not assigned to user");
                            return;
                        }

                        selectedBarangay = barangay;

                        db.collection("Barangays").document(barangay)
                                .collection("Farmers")
                                .whereEqualTo("farmerId", farmerId)
                                .get()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        if (task.getResult().isEmpty()) {
                                            showSearchError("No farmer found with ID: " + farmerId);
                                        } else {
                                            DocumentSnapshot farmerDoc = task.getResult().getDocuments().get(0);
                                            String farmerDocId = farmerDoc.getId();
                                            currentFarmerData = new HashMap<>(farmerDoc.getData());

                                            // Reset flags
                                            hasCrops = false;
                                            hasLivestock = false;

                                            // Load farm type data from the nested structure
                                            loadFarmTypeData(barangay, farmerDocId);
                                        }
                                    } else {
                                        showSearchError("Error searching for farmer");
                                        Log.e(TAG, "Error getting farmer documents: ", task.getException());
                                    }
                                });
                    } else {
                        showSearchError("User data not found");
                    }
                })
                .addOnFailureListener(e -> {
                    showSearchError("Error fetching user data");
                    Log.e(TAG, "Error getting user document: ", e);
                });
    }

    private void loadFarmTypeData(String barangay, String farmerDocId) {
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

                            // Extract location data for ALL farm types (moved outside specific type checks)
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
                                    currentFarmerData.put("location", locationBuilder.toString());
                                }
                            }

                            // Set flags and extract specific data based on farm type
                            if (farmTypeId.equalsIgnoreCase("Crop") || farmTypeId.equalsIgnoreCase("Crops")) {
                                hasCrops = true;

                                // Extract crop-specific data
                                if (farmTypeData != null) {
                                    String cropsGrown = getFieldValue(farmTypeData, "cropsGrown", "crops", "crops_grown");
                                    if (cropsGrown != null) {
                                        currentFarmerData.put("crops", cropsGrown);
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
                                        currentFarmerData.put("lotSize", combinedLotSize);
                                    }
                                }

                            } else if (farmTypeId.equalsIgnoreCase("Livestock")) {
                                hasLivestock = true;

                                // Extract livestock-specific data
                                if (farmTypeData != null) {
                                    // Get livestock type - the exact field name is "livestockType"
                                    String livestock = getFieldValue(farmTypeData, "livestockType", "livestock", "animalType", "animal", "livestockName");
                                    if (livestock != null) {
                                        currentFarmerData.put("livestock", livestock);
                                        Log.d(TAG, "Livestock type found: " + livestock);
                                    } else {
                                        Log.d(TAG, "No livestock type found in data");
                                    }

                                    // Get livestock count - the exact field name is "livestockCount"
                                    String livestockCount = getFieldValue(farmTypeData, "livestockCount", "numLivestock", "numberOfLivestock", "livestock_count", "animalCount", "count", "quantity", "livestockNumber", "totalLivestock");
                                    if (livestockCount != null) {
                                        currentFarmerData.put("livestockCount", livestockCount);
                                        Log.d(TAG, "Livestock count found: " + livestockCount);
                                    } else {
                                        Log.d(TAG, "No livestock count found in data");
                                        // Debug: print all available keys
                                        Log.d(TAG, "Available keys in livestock data: " + farmTypeData.keySet().toString());
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
                                        // Fallback to direct lotSize field
                                        combinedLotSize = getFieldValue(farmTypeData, "lotSize", "lot_size", "farmSize");
                                    }

                                    if (combinedLotSize != null && !combinedLotSize.isEmpty()) {
                                        currentFarmerData.put("lotSize", combinedLotSize);
                                        Log.d(TAG, "Livestock farm lot size found: " + combinedLotSize);
                                    }
                                }
                            }
                        }

                        // Set the combined farm type
                        if (farmTypeBuilder.length() > 0) {
                            currentFarmerData.put("farmType", farmTypeBuilder.toString());
                        }
                    } else {
                        Log.d(TAG, "No farm type data found");
                        currentFarmerData.put("farmType", "Not specified");
                    }

                    // Populate the UI with all collected data
                    populateFarmerData(currentFarmerData);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading farm type data", e);
                    // Still populate basic farmer data even if farm type loading fails
                    currentFarmerData.put("farmType", "Error loading farm type");
                    populateFarmerData(currentFarmerData);
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
        if (data != null) {
            Log.d(TAG, "Available keys in data: " + data.keySet().toString());
        }
        return null;
    }

    private void populateFarmerData(Map<String, Object> farmerData) {
        runOnUiThread(() -> {
            try {
                // Set basic farmer information
                String fullName = getFieldValue(farmerData, "fullName", "full_name", "name");
                if (fullName == null || fullName.isEmpty()) {
                    // Construct from parts if fullName doesn't exist
                    String firstName = getFieldValue(farmerData, "firstName", "first_name");
                    String lastName = getFieldValue(farmerData, "lastName", "last_name");
                    String middleInitial = getFieldValue(farmerData, "middleInitial", "middle_initial");

                    StringBuilder nameBuilder = new StringBuilder();
                    if (firstName != null) nameBuilder.append(firstName);
                    if (middleInitial != null) {
                        if (nameBuilder.length() > 0) nameBuilder.append(" ");
                        nameBuilder.append(middleInitial);
                    }
                    if (lastName != null) {
                        if (nameBuilder.length() > 0) nameBuilder.append(" ");
                        nameBuilder.append(lastName);
                    }
                    fullName = nameBuilder.toString();
                }
                etFarmerName.setText(fullName != null ? fullName : "");

                // Set farm details
                tvFarmType.setText(getFieldValue(farmerData, "farmType") != null ? getFieldValue(farmerData, "farmType") : "Not specified");
                tvLocation.setText(getFieldValue(farmerData, "location") != null ? getFieldValue(farmerData, "location") : "Not specified");

                // Handle crops data
                String crops = getFieldValue(farmerData, "crops", "cropsGrown");
                tvCrops.setText(crops != null ? crops : "Not specified");

                // Handle lot size
                String lotSize = getFieldValue(farmerData, "lotSize", "lot_size");
                tvLotSize.setText(lotSize != null ? lotSize : "Not specified");

                // Handle livestock data
                String livestock = getFieldValue(farmerData, "livestock", "livestockType");
                tvLivestock.setText(livestock != null ? livestock : "Not specified");

                // Handle livestock count
                String livestockCount = getFieldValue(farmerData, "livestockCount", "numLivestock", "numberOfLivestock");
                tvLivestockCount.setText(livestockCount != null ? livestockCount : "Not specified");

                // Debug logging
                Log.d(TAG, "=== POPULATED FARMER DATA ===");
                Log.d(TAG, "Farm Type: " + getFieldValue(farmerData, "farmType"));
                Log.d(TAG, "Location: " + getFieldValue(farmerData, "location"));
                Log.d(TAG, "Livestock: " + getFieldValue(farmerData, "livestock"));
                Log.d(TAG, "Livestock Count: " + getFieldValue(farmerData, "livestockCount"));
                Log.d(TAG, "Lot Size: " + getFieldValue(farmerData, "lotSize"));
                Log.d(TAG, "Has Livestock Flag: " + hasLivestock);
                Log.d(TAG, "Has Crops Flag: " + hasCrops);

                // Update the main support spinner based on farm type
                updateMainSupportSpinner();

                // Update the UI based on selected support type
                updateSupportTypeSpinners();

                // Show the farmer details section
                farmerDetailsSection.setVisibility(View.VISIBLE);
                findViewById(R.id.submit_button_container).setVisibility(View.VISIBLE);

                // Hide loading state
                loadingState.setVisibility(View.GONE);
                btnFindFarmer.setVisibility(View.VISIBLE);

                // Show success message
                tvSearchStatus.setText("Farmer found!");
                tvSearchStatus.setTextColor(getResources().getColor(R.color.ButtonGreen));
                tvSearchStatus.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                Log.e(TAG, "Error populating farmer data", e);
                showSearchError("Error displaying farmer data");
            }
        });
    }

    private void showSearchError(String message) {
        runOnUiThread(() -> {
            tvSearchStatus.setText(message);
            tvSearchStatus.setTextColor(getResources().getColor(R.color.red));
            tvSearchStatus.setVisibility(View.VISIBLE);

            loadingState.setVisibility(View.GONE);
            btnFindFarmer.setVisibility(View.VISIBLE);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_PICK_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                selectedFileUri = data.getData();
                if (selectedFileUri != null) {
                    btnChooseFile.setText("File selected: " + getFileName(selectedFileUri));
                }
            }
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting file name", e);
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void submitSubsidyRequest() {
        // Get current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create subsidy request data
        Map<String, Object> subsidyRequest = new HashMap<>();
        subsidyRequest.put("farmerId", etFarmerId.getText().toString().trim());
        subsidyRequest.put("farmerName", etFarmerName.getText().toString().trim());
        subsidyRequest.put("supportType", spinnerSupport.getSelectedItem().toString());
        subsidyRequest.put("status", "Pending");
        subsidyRequest.put("timestamp", System.currentTimeMillis());
        subsidyRequest.put("submittedBy", currentUser.getUid());
        subsidyRequest.put("barangay", selectedBarangay);

        // Add specific support details based on type
        String supportType = spinnerSupport.getSelectedItem().toString();
        if (supportType.equals("Livestock Support") && hasLivestock) {
            String livestockSupportType = spinnerLivestockSupport.getSelectedItem().toString();
            if (livestockSupportType.equals("Others (Please Specify)") && !customLivestockSupport.isEmpty()) {
                subsidyRequest.put("livestockSupportType", "Others: " + customLivestockSupport);
            } else {
                subsidyRequest.put("livestockSupportType", livestockSupportType);
            }
        } else if (supportType.equals("Seeds and Fertilizers") && hasCrops) {
            String seedType = spinnerSeedsType.getSelectedItem().toString();
            if (seedType.equals("Others (Please Specify)") && !customSeedType.isEmpty()) {
                subsidyRequest.put("seedType", "Others: " + customSeedType);
            } else {
                subsidyRequest.put("seedType", seedType);
            }
            subsidyRequest.put("fertilizerType", spinnerFertilizersType.getSelectedItem().toString());
        }

        // Handle file upload if a file is selected
        if (selectedFileUri != null) {
            uploadFileToFirebaseAndSubmit(subsidyRequest);
        } else {
            // This shouldn't happen due to validation, but handle it just in case
            submitToFirestore(subsidyRequest, null);
        }
    }

    private void uploadFileToFirebaseAndSubmit(Map<String, Object> subsidyRequest) {
        // Show loading state
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Uploading file...");

        String farmerId = subsidyRequest.get("farmerId").toString();
        String originalFileName = getFileName(selectedFileUri);

        Log.d(TAG, "=== FIREBASE FILE UPLOAD DEBUG ===");
        Log.d(TAG, "Farmer ID: " + farmerId);
        Log.d(TAG, "Original filename: " + originalFileName);

        try {
            // Get Firebase Storage reference
            StorageReference storageRef = storage.getReference();

            // Create a reference for the file with timestamp to avoid conflicts
            String fileName = "subsidy_documents/" + System.currentTimeMillis() + "_" + farmerId + "_" + originalFileName;
            StorageReference fileRef = storageRef.child(fileName);

            // Upload file
            UploadTask uploadTask = fileRef.putFile(selectedFileUri);

            uploadTask.addOnSuccessListener(taskSnapshot -> {
                Log.d(TAG, "✓ File uploaded successfully to Firebase Storage");

                // Get download URL
                fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    Log.d(TAG, "✓ Download URL obtained: " + uri.toString());

                    runOnUiThread(() -> {
                        btnSubmit.setText("File uploaded, submitting request...");

                        // Add file information to subsidy request
                        subsidyRequest.put("supportingDocumentUrl", uri.toString());
                        subsidyRequest.put("supportingDocumentName", originalFileName);
                        subsidyRequest.put("supportingDocumentPath", fileName);
                        subsidyRequest.put("storageProvider", "firebase");

                        // Now submit to Firestore
                        submitToFirestore(subsidyRequest, originalFileName);
                    });
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Failed to get download URL: " + e.getMessage());
                    runOnUiThread(() -> {
                        Toast.makeText(SubsidyRequest.this, "Failed to get file URL: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        resetSubmitButton();
                    });
                });

            }).addOnFailureListener(e -> {
                Log.e(TAG, "✗ Firebase file upload failed: " + e.getMessage());
                runOnUiThread(() -> {
                    String errorMessage = "File upload failed: " + e.getMessage();
                    Toast.makeText(SubsidyRequest.this, errorMessage, Toast.LENGTH_LONG).show();
                    resetSubmitButton();
                });
            }).addOnProgressListener(taskSnapshot -> {
                // Show upload progress
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                runOnUiThread(() -> {
                    btnSubmit.setText("Uploading... " + (int) progress + "%");
                });
            });

        } catch (Exception e) {
            Log.e(TAG, "✗ Unexpected error during upload: " + e.getMessage());
            e.printStackTrace();
            runOnUiThread(() -> {
                Toast.makeText(SubsidyRequest.this, "Upload error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                resetSubmitButton();
            });
        }
    }

    private void resetSubmitButton() {
        btnSubmit.setEnabled(true);
        btnSubmit.setText("Submit Request");
    }

    private void submitToFirestore(Map<String, Object> subsidyRequest, String fileName) {
        // Show loading state
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Submitting request...");

        // Get reference to the barangay's SubsidyRequests subcollection
        db.collection("Barangays").document(selectedBarangay)
                .collection("SubsidyRequests")
                .add(subsidyRequest)
                .addOnSuccessListener(documentReference -> {
                    // Success - subsidy request submitted
                    if (fileName != null) {
                        Log.d(TAG, "Subsidy request with file submitted with ID: " + documentReference.getId());
                    } else {
                        Log.d(TAG, "Subsidy request submitted with ID: " + documentReference.getId());
                    }

                    // Store the request ID in the document for easier reference
                    db.collection("Barangays").document(selectedBarangay)
                            .collection("SubsidyRequests").document(documentReference.getId())
                            .update("requestId", documentReference.getId())
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Request ID updated in document");
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error updating request ID", e);
                            });

                    // Navigate to success activity
                    Intent successIntent = new Intent(SubsidyRequest.this, SuccessSubsidyApplication.class);
                    successIntent.putExtra("farmerId", subsidyRequest.get("farmerId").toString());
                    successIntent.putExtra("farmerName", subsidyRequest.get("farmerName").toString());
                    successIntent.putExtra("supportType", subsidyRequest.get("supportType").toString());
                    successIntent.putExtra("requestId", documentReference.getId());
                    startActivity(successIntent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Error occurred
                    Log.e(TAG, "Error submitting subsidy request", e);
                    Toast.makeText(SubsidyRequest.this, "Failed to submit request: " + e.getMessage(), Toast.LENGTH_LONG).show();

                    // Reset button state
                    resetSubmitButton();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up any ongoing operations if necessary
    }

    @Override
    public void onBackPressed() {
        // Handle back button press
        super.onBackPressed();
        finish();
    }

    private void resetForm() {
        etFarmerId.setText("");
        etFarmerName.setText("");

        // Reset spinners to prompt positions (index 0)
        spinnerSupport.setSelection(0);
        spinnerLivestockSupport.setSelection(0);
        spinnerSeedsType.setSelection(0);
        spinnerFertilizersType.setSelection(0);

        // Reset custom values
        customLivestockSupport = "";
        customSeedType = "";

        // Hide sections
        farmerDetailsSection.setVisibility(View.GONE);
        findViewById(R.id.submit_button_container).setVisibility(View.GONE);

        // Reset file selection
        selectedFileUri = null;
        btnChooseFile.setText("Choose Supporting Document");

        // Reset status
        tvSearchStatus.setVisibility(View.GONE);

        // Reset flags
        hasCrops = false;
        hasLivestock = false;
        selectedBarangay = null;
        currentFarmerData.clear();

        // Re-enable submit button
        btnSubmit.setEnabled(true);
        btnSubmit.setText("Submit Request");
    }
}