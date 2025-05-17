package com.maramagagriculturalaid.app.FarmersData;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatSpinner;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.maramagagriculturalaid.app.R;
import com.maramagagriculturalaid.app.FarmersData.FarmersDetailsActivity;

import java.util.HashMap;
import java.util.Map;

public class EditFarmInformationActivity extends AppCompatActivity {

    private static final String TAG = "EditFarmInfo";

    // UI Components
    private ImageButton btnBack;
    private TextView tvFarmerName;
    private RadioGroup rgFarmType;
    private RadioButton rbCrop, rbLivestock, rbMixed;
    private ViewFlipper viewFlipper;
    private AppCompatButton btnSave;
    private View progressOverlay;

    // Crop Form Components
    private EditText etMunicipalCrop, etStreetCrop, etOtherCrop;
    private TextView tvBarangayCrop;
    private AppCompatSpinner spinnerCropsGrown;
    private LinearLayout layoutOtherCrop;
    private EditText etLotSizeValue;
    private Spinner spinnerUnit;

    // Livestock Form Components
    private EditText etMunicipalLivestock, etStreetLivestock, etOtherLivestock, etAnimalCount;
    private TextView tvBarangayLivestock;
    private AppCompatSpinner spinnerLivestockType;
    private LinearLayout layoutOtherLivestock;

    // Mixed Form Components
    private EditText etMunicipalMixed, etStreetMixed, etOtherCropMixed, etOtherLivestockMixed;
    private TextView tvBarangayMixed;
    private AppCompatSpinner spinnerCropsGrownMixed, spinnerLivestockTypeMixed;
    private LinearLayout layoutOtherCropMixed, layoutOtherLivestockMixed;
    private EditText etLotSizeValueMixed, etAnimalCountMixed;
    private Spinner spinnerUnitMixed;

    // Data
    private FirebaseFirestore db;
    private String farmerId, farmerName, barangay, farmerDocumentId;
    private String currentFarmType = "";

    // Basic information from EditFarmerActivity
    private String firstName, middleName, lastName, contactNumber;

    // Farm type data storage
    private Map<String, Object> cropData = new HashMap<>();
    private Map<String, Object> livestockData = new HashMap<>();

    // Shared data across farm types
    private String sharedStreet = "";
    private String sharedMunicipal = "";

    // Unsaved changes tracking
    private boolean hasUnsavedChanges = false;
    private Map<String, String> originalValues = new HashMap<>();
    private String originalFarmType = "";

    // Spinner Data
    private String[] cropOptions = {"Select Crop", "Rice", "Corn", "Vegetables", "Fruits", "Root Crops", "Pineapple", "Other"};
    private String[] livestockOptions = {"Select Livestock", "Cattle", "Carabao", "Goat", "Goats", "Pig", "Chicken", "Duck", "Other"};
    private String[] unitOptions = {"Hectares(ha)", "Square Meters"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_farm_information);

        db = FirebaseFirestore.getInstance();

        // Get data from intent
        farmerId = getIntent().getStringExtra("farmerId");
        farmerName = getIntent().getStringExtra("farmerName");
        barangay = getIntent().getStringExtra("barangay");
        farmerDocumentId = getIntent().getStringExtra("farmerDocumentId");

        // Get basic information from EditFarmerActivity
        firstName = getIntent().getStringExtra("firstName");
        middleName = getIntent().getStringExtra("middleName");
        lastName = getIntent().getStringExtra("lastName");
        contactNumber = getIntent().getStringExtra("contactNumber");

        // Debug logging
        Log.d(TAG, "Received data - farmerId: " + farmerId + ", farmerName: " + farmerName +
                ", barangay: " + barangay + ", farmerDocumentId: " + farmerDocumentId);
        Log.d(TAG, "Basic info - firstName: " + firstName + ", lastName: " + lastName);

        initViews();
        setupSpinners();
        setupClickListeners();
        setupChangeListeners();
        loadFarmerData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvFarmerName = findViewById(R.id.tv_farmer_name);
        rgFarmType = findViewById(R.id.rg_farm_type);
        rbCrop = findViewById(R.id.rb_crop);
        rbLivestock = findViewById(R.id.rb_livestock);
        rbMixed = findViewById(R.id.rb_mixed);
        viewFlipper = findViewById(R.id.view_flipper);
        btnSave = findViewById(R.id.btn_save);
        progressOverlay = findViewById(R.id.progress_overlay);

        // Crop form components
        etMunicipalCrop = findViewById(R.id.et_municipal_crop);
        tvBarangayCrop = findViewById(R.id.tv_barangay_crop);
        etStreetCrop = findViewById(R.id.et_street_crop);
        spinnerCropsGrown = findViewById(R.id.spinner_crops_grown);
        layoutOtherCrop = findViewById(R.id.layout_other_crop);
        etOtherCrop = findViewById(R.id.et_other_crop);
        etLotSizeValue = findViewById(R.id.et_lot_size_value);
        spinnerUnit = findViewById(R.id.spinner_unit);

        // Livestock form components
        etMunicipalLivestock = findViewById(R.id.et_municipal_livestock);
        tvBarangayLivestock = findViewById(R.id.tv_barangay_livestock);
        etStreetLivestock = findViewById(R.id.et_street_livestock);
        spinnerLivestockType = findViewById(R.id.spinner_livestock_type);
        layoutOtherLivestock = findViewById(R.id.layout_other_livestock);
        etOtherLivestock = findViewById(R.id.et_other_livestock);
        etAnimalCount = findViewById(R.id.et_animal_count);

        // Mixed form components
        etMunicipalMixed = findViewById(R.id.et_municipal_mixed);
        tvBarangayMixed = findViewById(R.id.tv_barangay_mixed);
        etStreetMixed = findViewById(R.id.et_street_mixed);
        spinnerCropsGrownMixed = findViewById(R.id.spinner_crops_grown_mixed);
        layoutOtherCropMixed = findViewById(R.id.layout_other_crop_mixed);
        etOtherCropMixed = findViewById(R.id.et_other_crop_mixed);
        etLotSizeValueMixed = findViewById(R.id.et_lot_size_value_mixed);
        spinnerUnitMixed = findViewById(R.id.spinner_unit_mixed);
        spinnerLivestockTypeMixed = findViewById(R.id.spinner_livestock_type_mixed);
        layoutOtherLivestockMixed = findViewById(R.id.layout_other_livestock_mixed);
        etOtherLivestockMixed = findViewById(R.id.et_other_livestock_mixed);
        etAnimalCountMixed = findViewById(R.id.et_animal_count_mixed);

        // Set farmer name
        if (farmerName != null) {
            tvFarmerName.setText(farmerName);
        }

        // Set barangay in all forms
        if (barangay != null) {
            tvBarangayCrop.setText(barangay);
            tvBarangayLivestock.setText(barangay);
            tvBarangayMixed.setText(barangay);
        }

        // Hide progress overlay initially
        if (progressOverlay != null) {
            progressOverlay.setVisibility(View.GONE);
        }
    }

    private void setupChangeListeners() {
        // Text change listeners for EditText fields
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkForChanges();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        // Add text watchers to all EditText fields
        if (etMunicipalCrop != null) etMunicipalCrop.addTextChangedListener(textWatcher);
        if (etStreetCrop != null) etStreetCrop.addTextChangedListener(textWatcher);
        if (etOtherCrop != null) etOtherCrop.addTextChangedListener(textWatcher);
        if (etLotSizeValue != null) etLotSizeValue.addTextChangedListener(textWatcher);

        if (etMunicipalLivestock != null) etMunicipalLivestock.addTextChangedListener(textWatcher);
        if (etStreetLivestock != null) etStreetLivestock.addTextChangedListener(textWatcher);
        if (etOtherLivestock != null) etOtherLivestock.addTextChangedListener(textWatcher);
        if (etAnimalCount != null) etAnimalCount.addTextChangedListener(textWatcher);

        if (etMunicipalMixed != null) etMunicipalMixed.addTextChangedListener(textWatcher);
        if (etStreetMixed != null) etStreetMixed.addTextChangedListener(textWatcher);
        if (etOtherCropMixed != null) etOtherCropMixed.addTextChangedListener(textWatcher);
        if (etOtherLivestockMixed != null) etOtherLivestockMixed.addTextChangedListener(textWatcher);
        if (etLotSizeValueMixed != null) etLotSizeValueMixed.addTextChangedListener(textWatcher);
        if (etAnimalCountMixed != null) etAnimalCountMixed.addTextChangedListener(textWatcher);
    }

    private void checkForChanges() {
        hasUnsavedChanges = true;
        Log.d(TAG, "Changes detected - hasUnsavedChanges set to true");
    }

    private void saveOriginalValues() {
        // Save original values after data is loaded
        originalValues.clear();
        originalFarmType = currentFarmType;

        // Save crop form values
        originalValues.put("etMunicipalCrop", getTextSafely(etMunicipalCrop));
        originalValues.put("etStreetCrop", getTextSafely(etStreetCrop));
        originalValues.put("etOtherCrop", getTextSafely(etOtherCrop));
        originalValues.put("etLotSizeValue", getTextSafely(etLotSizeValue));
        originalValues.put("spinnerCropsGrown", String.valueOf(spinnerCropsGrown.getSelectedItemPosition()));
        originalValues.put("spinnerUnit", String.valueOf(spinnerUnit.getSelectedItemPosition()));

        // Save livestock form values
        originalValues.put("etMunicipalLivestock", getTextSafely(etMunicipalLivestock));
        originalValues.put("etStreetLivestock", getTextSafely(etStreetLivestock));
        originalValues.put("etOtherLivestock", getTextSafely(etOtherLivestock));
        originalValues.put("etAnimalCount", getTextSafely(etAnimalCount));
        originalValues.put("spinnerLivestockType", String.valueOf(spinnerLivestockType.getSelectedItemPosition()));

        // Save mixed form values
        originalValues.put("etMunicipalMixed", getTextSafely(etMunicipalMixed));
        originalValues.put("etStreetMixed", getTextSafely(etStreetMixed));
        originalValues.put("etOtherCropMixed", getTextSafely(etOtherCropMixed));
        originalValues.put("etOtherLivestockMixed", getTextSafely(etOtherLivestockMixed));
        originalValues.put("etLotSizeValueMixed", getTextSafely(etLotSizeValueMixed));
        originalValues.put("etAnimalCountMixed", getTextSafely(etAnimalCountMixed));
        originalValues.put("spinnerCropsGrownMixed", String.valueOf(spinnerCropsGrownMixed.getSelectedItemPosition()));
        originalValues.put("spinnerLivestockTypeMixed", String.valueOf(spinnerLivestockTypeMixed.getSelectedItemPosition()));
        originalValues.put("spinnerUnitMixed", String.valueOf(spinnerUnitMixed.getSelectedItemPosition()));

        hasUnsavedChanges = false;
        Log.d(TAG, "Original values saved, hasUnsavedChanges reset to false");
    }

    private String getTextSafely(EditText editText) {
        return editText != null && editText.getText() != null ? editText.getText().toString() : "";
    }

    private void setupSpinners() {
        // Create custom adapters with black text
        ArrayAdapter<String> cropAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, cropOptions) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                textView.setTextColor(getResources().getColor(android.R.color.black));
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;
                textView.setTextColor(getResources().getColor(android.R.color.black));
                return view;
            }
        };
        cropAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter<String> livestockAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, livestockOptions) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                textView.setTextColor(getResources().getColor(android.R.color.black));
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;
                textView.setTextColor(getResources().getColor(android.R.color.black));
                return view;
            }
        };
        livestockAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter<String> unitAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, unitOptions) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                textView.setTextColor(getResources().getColor(android.R.color.black));
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;
                textView.setTextColor(getResources().getColor(android.R.color.black));
                return view;
            }
        };
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Set adapters
        spinnerCropsGrown.setAdapter(cropAdapter);
        spinnerCropsGrownMixed.setAdapter(cropAdapter);
        spinnerLivestockType.setAdapter(livestockAdapter);
        spinnerLivestockTypeMixed.setAdapter(livestockAdapter);
        spinnerUnit.setAdapter(unitAdapter);
        spinnerUnitMixed.setAdapter(unitAdapter);

        // Setup spinner listeners
        setupSpinnerListeners();
    }

    private void setupSpinnerListeners() {
        // Crop spinner listener
        spinnerCropsGrown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == cropOptions.length - 1) { // "Other" selected
                    layoutOtherCrop.setVisibility(View.VISIBLE);
                } else {
                    layoutOtherCrop.setVisibility(View.GONE);
                    etOtherCrop.setText("");
                }
                checkForChanges();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Livestock spinner listener
        spinnerLivestockType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == livestockOptions.length - 1) { // "Other" selected
                    layoutOtherLivestock.setVisibility(View.VISIBLE);
                } else {
                    layoutOtherLivestock.setVisibility(View.GONE);
                    etOtherLivestock.setText("");
                }
                checkForChanges();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Mixed form crop spinner listener
        spinnerCropsGrownMixed.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == cropOptions.length - 1) { // "Other" selected
                    layoutOtherCropMixed.setVisibility(View.VISIBLE);
                } else {
                    layoutOtherCropMixed.setVisibility(View.GONE);
                    etOtherCropMixed.setText("");
                }
                checkForChanges();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Mixed form livestock spinner listener
        spinnerLivestockTypeMixed.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == livestockOptions.length - 1) { // "Other" selected
                    layoutOtherLivestockMixed.setVisibility(View.VISIBLE);
                } else {
                    layoutOtherLivestockMixed.setVisibility(View.GONE);
                    etOtherLivestockMixed.setText("");
                }
                checkForChanges();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Unit spinner listeners
        spinnerUnit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                checkForChanges();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerUnitMixed.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                checkForChanges();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupClickListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> handleBackPress());
        }

        // Farm type radio group listener
        rgFarmType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_crop) {
                viewFlipper.setDisplayedChild(0);
                currentFarmType = "Crop";
                // Auto-fill shared data when switching to crop
                autoFillSharedData("Crop");
            } else if (checkedId == R.id.rb_livestock) {
                viewFlipper.setDisplayedChild(1);
                currentFarmType = "Livestock";
                // Auto-fill shared data when switching to livestock
                autoFillSharedData("Livestock");
            } else if (checkedId == R.id.rb_mixed) {
                viewFlipper.setDisplayedChild(2);
                currentFarmType = "Mixed";
                // Auto-fill shared data when switching to mixed
                autoFillSharedData("Mixed");
            }
            checkForChanges();
        });

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                if (validateForm()) {
                    saveAllFarmerInformation();
                }
            });
        }
    }

    private void handleBackPress() {
        if (hasUnsavedChanges) {
            showUnsavedChangesDialog();
        } else {
            navigateToFarmersDetailsActivity();
        }
    }

    private void showUnsavedChangesDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Unsaved Changes")
                .setMessage("You have unsaved changes. Do you want to save them before leaving?")
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (validateForm()) {
                            saveAllFarmerInformation();
                        }
                    }
                })
                .setNegativeButton("Discard", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        hasUnsavedChanges = false;
                        navigateToFarmersDetailsActivity();
                    }
                })
                .setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void navigateToFarmersDetailsActivity() {
        Intent intent = new Intent(this, FarmersDetailsActivity.class);

        // Pass all the necessary data to FarmersDetailsActivity
        intent.putExtra("farmerId", farmerId);
        intent.putExtra("farmerName", farmerName);
        intent.putExtra("barangay", barangay);
        intent.putExtra("farmerDocumentId", farmerDocumentId);

        // Pass basic information if available
        if (firstName != null) intent.putExtra("firstName", firstName);
        if (middleName != null) intent.putExtra("middleName", middleName);
        if (lastName != null) intent.putExtra("lastName", lastName);
        if (contactNumber != null) intent.putExtra("contactNumber", contactNumber);

        // Clear the activity stack and start FarmersDetailsActivity
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        startActivity(intent);
        finish();
    }

    private void autoFillSharedData(String targetFarmType) {
        // Auto-fill street and municipal data across farm types
        switch (targetFarmType) {
            case "Crop":
                if (!TextUtils.isEmpty(sharedStreet) && TextUtils.isEmpty(etStreetCrop.getText())) {
                    etStreetCrop.setText(sharedStreet);
                }
                if (!TextUtils.isEmpty(sharedMunicipal) && TextUtils.isEmpty(etMunicipalCrop.getText())) {
                    etMunicipalCrop.setText(sharedMunicipal);
                }
                break;
            case "Livestock":
                if (!TextUtils.isEmpty(sharedStreet) && TextUtils.isEmpty(etStreetLivestock.getText())) {
                    etStreetLivestock.setText(sharedStreet);
                }
                if (!TextUtils.isEmpty(sharedMunicipal) && TextUtils.isEmpty(etMunicipalLivestock.getText())) {
                    etMunicipalLivestock.setText(sharedMunicipal);
                }
                break;
            case "Mixed":
                if (!TextUtils.isEmpty(sharedStreet) && TextUtils.isEmpty(etStreetMixed.getText())) {
                    etStreetMixed.setText(sharedStreet);
                }
                if (!TextUtils.isEmpty(sharedMunicipal) && TextUtils.isEmpty(etMunicipalMixed.getText())) {
                    etMunicipalMixed.setText(sharedMunicipal);
                }
                break;
        }
    }

    private void loadFarmerData() {
        if (barangay == null) {
            Toast.makeText(this, "Missing barangay information", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (progressOverlay != null) {
            progressOverlay.setVisibility(View.VISIBLE);
        }

        // Load farmer basic data first, then farm type data
        if (farmerDocumentId != null && !farmerDocumentId.isEmpty()) {
            loadFarmerByDocumentId(farmerDocumentId);
        } else if (farmerId != null && !farmerId.isEmpty()) {
            loadFarmerByFarmerId(farmerId);
        } else if (farmerName != null && !farmerName.isEmpty()) {
            loadFarmerByDocumentId(farmerName);
        } else {
            if (progressOverlay != null) {
                progressOverlay.setVisibility(View.GONE);
            }
            Toast.makeText(this, "Missing farmer information", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadFarmerByDocumentId(String documentId) {
        Log.d(TAG, "Trying to load farmer by document ID: " + documentId);

        DocumentReference farmerRef = db.collection("Barangays")
                .document(barangay)
                .collection("Farmers")
                .document(documentId);

        farmerRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    Log.d(TAG, "Farmer found by document ID: " + documentId);
                    farmerDocumentId = documentId;
                    // Load farm type data from subcollection
                    loadFarmTypeData(documentId);
                } else {
                    Log.d(TAG, "Farmer not found by document ID: " + documentId);
                    if (farmerId != null && !farmerId.isEmpty()) {
                        loadFarmerByFarmerId(farmerId);
                    } else {
                        if (progressOverlay != null) {
                            progressOverlay.setVisibility(View.GONE);
                        }
                        Toast.makeText(this, "Farmer data not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
            } else {
                Log.e(TAG, "Error loading farmer by document ID", task.getException());
                if (farmerId != null && !farmerId.isEmpty()) {
                    loadFarmerByFarmerId(farmerId);
                } else {
                    if (progressOverlay != null) {
                        progressOverlay.setVisibility(View.GONE);
                    }
                    Toast.makeText(this, "Error loading farmer data", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
    }

    private void loadFarmerByFarmerId(String searchFarmerId) {
        Log.d(TAG, "Trying to load farmer by farmer ID: " + searchFarmerId);

        db.collection("Barangays")
                .document(barangay)
                .collection("Farmers")
                .whereEqualTo("farmerId", searchFarmerId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d(TAG, "Farmer found by farmer ID: " + searchFarmerId + ", Document ID: " + document.getId());
                            farmerDocumentId = document.getId();
                            // Load farm type data from subcollection
                            loadFarmTypeData(document.getId());
                            return;
                        }
                    }

                    Log.d(TAG, "Farmer not found by farmer ID: " + searchFarmerId);
                    if (progressOverlay != null) {
                        progressOverlay.setVisibility(View.GONE);
                    }
                    Toast.makeText(this, "Farmer data not found in " + barangay, Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error searching farmer by ID", e);
                    if (progressOverlay != null) {
                        progressOverlay.setVisibility(View.GONE);
                    }
                    Toast.makeText(this, "Error loading farmer data", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void loadFarmTypeData(String farmerDocId) {
        Log.d(TAG, "Loading farm type data for farmer: " + farmerDocId);

        db.collection("Barangays")
                .document(barangay)
                .collection("Farmers")
                .document(farmerDocId)
                .collection("FarmType")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean hasData = false;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String docId = document.getId();
                            Log.d(TAG, "Found farm type document: " + docId);
                            Log.d(TAG, "Document data: " + document.getData());

                            // Extract shared data (street and municipal)
                            String street = (String) document.getData().get("street");
                            String municipal = (String) document.getData().get("municipal");

                            if (!TextUtils.isEmpty(street)) {
                                sharedStreet = street;
                            }
                            if (!TextUtils.isEmpty(municipal)) {
                                sharedMunicipal = municipal;
                            }

                            if ("Crop".equals(docId)) {
                                cropData = document.getData();
                                currentFarmType = "Crop";
                                hasData = true;
                                populateCropFormFromData();
                            } else if ("Livestock".equals(docId)) {
                                livestockData = document.getData();
                                currentFarmType = "Livestock";
                                hasData = true;
                                populateLivestockFormFromData();
                            }
                        }

                        // Auto-fill shared data to all forms
                        autoFillAllForms();

                        if (!hasData) {
                            // No farm type data found, default to Crop
                            Log.d(TAG, "No farm type data found, defaulting to Crop");
                            currentFarmType = "Crop";
                            rbCrop.setChecked(true);
                            viewFlipper.setDisplayedChild(0);
                        }

                        // Save original values after data is loaded
                        saveOriginalValues();

                        if (progressOverlay != null) {
                            progressOverlay.setVisibility(View.GONE);
                        }
                    } else {
                        Log.e(TAG, "Error loading farm type data", task.getException());
                        if (progressOverlay != null) {
                            progressOverlay.setVisibility(View.GONE);
                        }
                        // Default to Crop form
                        currentFarmType = "Crop";
                        rbCrop.setChecked(true);
                        viewFlipper.setDisplayedChild(0);
                        saveOriginalValues();
                    }
                });
    }

    private void autoFillAllForms() {
        // Auto-fill street and municipal to all forms if they're empty
        if (!TextUtils.isEmpty(sharedStreet)) {
            if (TextUtils.isEmpty(etStreetCrop.getText())) {
                etStreetCrop.setText(sharedStreet);
            }
            if (TextUtils.isEmpty(etStreetLivestock.getText())) {
                etStreetLivestock.setText(sharedStreet);
            }
            if (TextUtils.isEmpty(etStreetMixed.getText())) {
                etStreetMixed.setText(sharedStreet);
            }
        }

        if (!TextUtils.isEmpty(sharedMunicipal)) {
            if (TextUtils.isEmpty(etMunicipalCrop.getText())) {
                etMunicipalCrop.setText(sharedMunicipal);
            }
            if (TextUtils.isEmpty(etMunicipalLivestock.getText())) {
                etMunicipalLivestock.setText(sharedMunicipal);
            }
            if (TextUtils.isEmpty(etMunicipalMixed.getText())) {
                etMunicipalMixed.setText(sharedMunicipal);
            }
        }

        Log.d(TAG, "Auto-filled shared data - Street: " + sharedStreet + ", Municipal: " + sharedMunicipal);
    }

    private void populateCropFormFromData() {
        rbCrop.setChecked(true);
        viewFlipper.setDisplayedChild(0);

        String municipal = (String) cropData.get("municipal");
        String street = (String) cropData.get("street");
        String cropsGrown = (String) cropData.get("cropsGrown");
        String lotSizeValue = (String) cropData.get("lotSizeValue");
        String lotSizeUnit = (String) cropData.get("lotSizeUnit");

        if (municipal != null) etMunicipalCrop.setText(municipal);
        if (street != null) etStreetCrop.setText(street);
        if (lotSizeValue != null) etLotSizeValue.setText(lotSizeValue);

        // Set unit spinner
        if (lotSizeUnit != null) {
            for (int i = 0; i < unitOptions.length; i++) {
                if (unitOptions[i].equals(lotSizeUnit)) {
                    spinnerUnit.setSelection(i);
                    break;
                }
            }
        }

        // Set crop spinner
        if (cropsGrown != null && !cropsGrown.isEmpty()) {
            boolean found = false;
            for (int i = 1; i < cropOptions.length - 1; i++) { // Skip "Select Crop" and "Other"
                if (cropOptions[i].equalsIgnoreCase(cropsGrown)) {
                    spinnerCropsGrown.setSelection(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                // Set to "Other" and show custom field
                spinnerCropsGrown.setSelection(cropOptions.length - 1);
                layoutOtherCrop.setVisibility(View.VISIBLE);
                etOtherCrop.setText(cropsGrown);
            }
        }

        Log.d(TAG, "Populated crop form with data");
    }

    private void populateLivestockFormFromData() {
        rbLivestock.setChecked(true);
        viewFlipper.setDisplayedChild(1);

        String municipal = (String) livestockData.get("municipal");
        String street = (String) livestockData.get("street");
        String livestockType = (String) livestockData.get("livestockType");
        Object livestockCountObj = livestockData.get("livestockCount");

        if (municipal != null) etMunicipalLivestock.setText(municipal);
        if (street != null) etStreetLivestock.setText(street);

        if (livestockCountObj != null) {
            etAnimalCount.setText(String.valueOf(livestockCountObj));
        }

        // Set livestock spinner
        if (livestockType != null && !livestockType.isEmpty()) {
            boolean found = false;
            for (int i = 1; i < livestockOptions.length - 1; i++) { // Skip "Select Livestock" and "Other"
                if (livestockOptions[i].equalsIgnoreCase(livestockType)) {
                    spinnerLivestockType.setSelection(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                // Set to "Other" and show custom field
                spinnerLivestockType.setSelection(livestockOptions.length - 1);
                layoutOtherLivestock.setVisibility(View.VISIBLE);
                etOtherLivestock.setText(livestockType);
            }
        }

        Log.d(TAG, "Populated livestock form with data");
    }

    // Rest of the validation and saving methods remain the same...
    private boolean validateForm() {
        switch (currentFarmType) {
            case "Crop":
                return validateCropForm();
            case "Livestock":
                return validateLivestockForm();
            case "Mixed":
                return validateMixedForm();
            default:
                Toast.makeText(this, "Please select a farm type", Toast.LENGTH_SHORT).show();
                return false;
        }
    }

    private boolean validateCropForm() {
        if (TextUtils.isEmpty(etMunicipalCrop.getText())) {
            etMunicipalCrop.setError("Municipal is required");
            return false;
        }

        if (TextUtils.isEmpty(etStreetCrop.getText())) {
            etStreetCrop.setError("Street address is required");
            return false;
        }

        if (spinnerCropsGrown.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select crops grown", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (spinnerCropsGrown.getSelectedItemPosition() == cropOptions.length - 1 &&
                TextUtils.isEmpty(etOtherCrop.getText())) {
            etOtherCrop.setError("Please specify other crop");
            return false;
        }

        if (TextUtils.isEmpty(etLotSizeValue.getText())) {
            etLotSizeValue.setError("Lot size is required");
            return false;
        }

        try {
            double lotSize = Double.parseDouble(etLotSizeValue.getText().toString());
            if (lotSize <= 0) {
                etLotSizeValue.setError("Lot size must be greater than 0");
                return false;
            }
        } catch (NumberFormatException e) {
            etLotSizeValue.setError("Invalid lot size");
            return false;
        }

        return true;
    }

    private boolean validateLivestockForm() {
        if (TextUtils.isEmpty(etMunicipalLivestock.getText())) {
            etMunicipalLivestock.setError("Municipal is required");
            return false;
        }

        if (TextUtils.isEmpty(etStreetLivestock.getText())) {
            etStreetLivestock.setError("Street address is required");
            return false;
        }

        if (spinnerLivestockType.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select livestock type", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (spinnerLivestockType.getSelectedItemPosition() == livestockOptions.length - 1 &&
                TextUtils.isEmpty(etOtherLivestock.getText())) {
            etOtherLivestock.setError("Please specify other livestock");
            return false;
        }

        if (TextUtils.isEmpty(etAnimalCount.getText())) {
            etAnimalCount.setError("Number of animals is required");
            return false;
        }

        try {
            int count = Integer.parseInt(etAnimalCount.getText().toString());
            if (count <= 0) {
                etAnimalCount.setError("Number of animals must be greater than 0");
                return false;
            }
        } catch (NumberFormatException e) {
            etAnimalCount.setError("Invalid number");
            return false;
        }

        return true;
    }

    private boolean validateMixedForm() {
        if (TextUtils.isEmpty(etMunicipalMixed.getText())) {
            etMunicipalMixed.setError("Municipal is required");
            return false;
        }

        if (TextUtils.isEmpty(etStreetMixed.getText())) {
            etStreetMixed.setError("Street address is required");
            return false;
        }

        if (spinnerCropsGrownMixed.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select crops grown", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (spinnerCropsGrownMixed.getSelectedItemPosition() == cropOptions.length - 1 &&
                TextUtils.isEmpty(etOtherCropMixed.getText())) {
            etOtherCropMixed.setError("Please specify other crop");
            return false;
        }

        if (TextUtils.isEmpty(etLotSizeValueMixed.getText())) {
            etLotSizeValueMixed.setError("Lot size is required");
            return false;
        }

        try {
            double lotSize = Double.parseDouble(etLotSizeValueMixed.getText().toString());
            if (lotSize <= 0) {
                etLotSizeValueMixed.setError("Lot size must be greater than 0");
                return false;
            }
        } catch (NumberFormatException e) {
            etLotSizeValueMixed.setError("Invalid lot size");
            return false;
        }

        if (spinnerLivestockTypeMixed.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select livestock type", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (spinnerLivestockTypeMixed.getSelectedItemPosition() == livestockOptions.length - 1 &&
                TextUtils.isEmpty(etOtherLivestockMixed.getText())) {
            etOtherLivestockMixed.setError("Please specify other livestock");
            return false;
        }

        if (TextUtils.isEmpty(etAnimalCountMixed.getText())) {
            etAnimalCountMixed.setError("Number of animals is required");
            return false;
        }

        try {
            int count = Integer.parseInt(etAnimalCountMixed.getText().toString());
            if (count <= 0) {
                etAnimalCountMixed.setError("Number of animals must be greater than 0");
                return false;
            }
        } catch (NumberFormatException e) {
            etAnimalCountMixed.setError("Invalid number");
            return false;
        }

        return true;
    }

    private void saveAllFarmerInformation() {
        if (farmerDocumentId == null || farmerDocumentId.isEmpty()) {
            Toast.makeText(this, "Cannot save: Farmer document not found", Toast.LENGTH_SHORT).show();
            return;
        }

        if (progressOverlay != null) {
            progressOverlay.setVisibility(View.VISIBLE);
        }
        if (btnSave != null) {
            btnSave.setEnabled(false);
        }

        // Save to FarmType subcollection based on current farm type
        switch (currentFarmType) {
            case "Crop":
                saveCropDataToSubcollection();
                break;
            case "Livestock":
                saveLivestockDataToSubcollection();
                break;
            case "Mixed":
                saveMixedDataToSubcollection();
                break;
        }
    }

    private void saveCropDataToSubcollection() {
        Map<String, Object> cropData = new HashMap<>();
        cropData.put("barangay", barangay);
        cropData.put("farmType", "Crop");
        cropData.put("farmerId", farmerId);
        cropData.put("farmerName", farmerName);
        cropData.put("municipal", etMunicipalCrop.getText().toString().trim());
        cropData.put("street", etStreetCrop.getText().toString().trim());
        cropData.put("lotSizeValue", etLotSizeValue.getText().toString().trim());
        cropData.put("lotSizeUnit", unitOptions[spinnerUnit.getSelectedItemPosition()]);
        cropData.put("lotSize", etLotSizeValue.getText().toString().trim() + " " + unitOptions[spinnerUnit.getSelectedItemPosition()]);

        // Get crop value
        String cropValue;
        if (spinnerCropsGrown.getSelectedItemPosition() == cropOptions.length - 1) {
            cropValue = etOtherCrop.getText().toString().trim();
        } else {
            cropValue = cropOptions[spinnerCropsGrown.getSelectedItemPosition()];
        }
        cropData.put("cropsGrown", cropValue);

        DocumentReference cropRef = db.collection("Barangays")
                .document(barangay)
                .collection("Farmers")
                .document(farmerDocumentId)
                .collection("FarmType")
                .document("Crop");

        cropRef.set(cropData)
                .addOnSuccessListener(aVoid -> {
                    // Delete Livestock document if it exists
                    deleteLivestockDocument();
                })
                .addOnFailureListener(e -> {
                    if (progressOverlay != null) {
                        progressOverlay.setVisibility(View.GONE);
                    }
                    if (btnSave != null) {
                        btnSave.setEnabled(true);
                    }
                    Log.e(TAG, "Error saving crop data", e);
                    Toast.makeText(this, "Error saving crop information", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveLivestockDataToSubcollection() {
        Map<String, Object> livestockData = new HashMap<>();
        livestockData.put("barangay", barangay);
        livestockData.put("farmerId", farmerId);
        livestockData.put("farmerName", farmerName);
        livestockData.put("municipal", etMunicipalLivestock.getText().toString().trim());
        livestockData.put("street", etStreetLivestock.getText().toString().trim());
        livestockData.put("livestockCount", Integer.parseInt(etAnimalCount.getText().toString().trim()));

        // Get livestock value
        String livestockValue;
        if (spinnerLivestockType.getSelectedItemPosition() == livestockOptions.length - 1) {
            livestockValue = etOtherLivestock.getText().toString().trim();
        } else {
            livestockValue = livestockOptions[spinnerLivestockType.getSelectedItemPosition()];
        }
        livestockData.put("livestockType", livestockValue);

        DocumentReference livestockRef = db.collection("Barangays")
                .document(barangay)
                .collection("Farmers")
                .document(farmerDocumentId)
                .collection("FarmType")
                .document("Livestock");

        livestockRef.set(livestockData)
                .addOnSuccessListener(aVoid -> {
                    // Delete Crop document if it exists
                    deleteCropDocument();
                })
                .addOnFailureListener(e -> {
                    if (progressOverlay != null) {
                        progressOverlay.setVisibility(View.GONE);
                    }
                    if (btnSave != null) {
                        btnSave.setEnabled(true);
                    }
                    Log.e(TAG, "Error saving livestock data", e);
                    Toast.makeText(this, "Error saving livestock information", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveMixedDataToSubcollection() {
        // For mixed farming, save both crop and livestock data
        Map<String, Object> cropData = new HashMap<>();
        cropData.put("barangay", barangay);
        cropData.put("farmType", "Mixed");
        cropData.put("farmerId", farmerId);
        cropData.put("farmerName", farmerName);
        cropData.put("municipal", etMunicipalMixed.getText().toString().trim());
        cropData.put("street", etStreetMixed.getText().toString().trim());
        cropData.put("lotSizeValue", etLotSizeValueMixed.getText().toString().trim());
        cropData.put("lotSizeUnit", unitOptions[spinnerUnitMixed.getSelectedItemPosition()]);

        String cropValue;
        if (spinnerCropsGrownMixed.getSelectedItemPosition() == cropOptions.length - 1) {
            cropValue = etOtherCropMixed.getText().toString().trim();
        } else {
            cropValue = cropOptions[spinnerCropsGrownMixed.getSelectedItemPosition()];
        }
        cropData.put("cropsGrown", cropValue);

        Map<String, Object> livestockData = new HashMap<>();
        livestockData.put("barangay", barangay);
        livestockData.put("farmType", "Mixed");
        livestockData.put("farmerId", farmerId);
        livestockData.put("farmerName", farmerName);
        livestockData.put("municipal", etMunicipalMixed.getText().toString().trim());
        livestockData.put("street", etStreetMixed.getText().toString().trim());
        livestockData.put("livestockCount", Integer.parseInt(etAnimalCountMixed.getText().toString().trim()));

        String livestockValue;
        if (spinnerLivestockTypeMixed.getSelectedItemPosition() == livestockOptions.length - 1) {
            livestockValue = etOtherLivestockMixed.getText().toString().trim();
        } else {
            livestockValue = livestockOptions[spinnerLivestockTypeMixed.getSelectedItemPosition()];
        }
        livestockData.put("livestockType", livestockValue);

        // Save both documents
        DocumentReference cropRef = db.collection("Barangays")
                .document(barangay)
                .collection("Farmers")
                .document(farmerDocumentId)
                .collection("FarmType")
                .document("Crop");

        DocumentReference livestockRef = db.collection("Barangays")
                .document(barangay)
                .collection("Farmers")
                .document(farmerDocumentId)
                .collection("FarmType")
                .document("Livestock");

        cropRef.set(cropData)
                .addOnSuccessListener(aVoid -> {
                    livestockRef.set(livestockData)
                            .addOnSuccessListener(aVoid2 -> {
                                onSaveSuccess();
                            })
                            .addOnFailureListener(e -> {
                                if (progressOverlay != null) {
                                    progressOverlay.setVisibility(View.GONE);
                                }
                                if (btnSave != null) {
                                    btnSave.setEnabled(true);
                                }
                                Log.e(TAG, "Error saving livestock data for mixed farm", e);
                                Toast.makeText(this, "Error saving livestock information", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    if (progressOverlay != null) {
                        progressOverlay.setVisibility(View.GONE);
                    }
                    if (btnSave != null) {
                        btnSave.setEnabled(true);
                    }
                    Log.e(TAG, "Error saving crop data for mixed farm", e);
                    Toast.makeText(this, "Error saving crop information", Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteCropDocument() {
        db.collection("Barangays")
                .document(barangay)
                .collection("Farmers")
                .document(farmerDocumentId)
                .collection("FarmType")
                .document("Crop")
                .delete()
                .addOnCompleteListener(task -> {
                    onSaveSuccess();
                });
    }

    private void deleteLivestockDocument() {
        db.collection("Barangays")
                .document(barangay)
                .collection("Farmers")
                .document(farmerDocumentId)
                .collection("FarmType")
                .document("Livestock")
                .delete()
                .addOnCompleteListener(task -> {
                    onSaveSuccess();
                });
    }

    private void onSaveSuccess() {
        if (progressOverlay != null) {
            progressOverlay.setVisibility(View.GONE);
        }
        if (btnSave != null) {
            btnSave.setEnabled(true);
        }

        // Reset unsaved changes flag after successful save
        hasUnsavedChanges = false;

        Toast.makeText(this, "Farm information updated successfully", Toast.LENGTH_SHORT).show();

        // Navigate to FarmersDetailsActivity after successful save
        navigateToFarmersDetailsActivity();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        handleBackPress();
    }
}