package com.maramagagriculturalaid.app.FarmersData;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.maramagagriculturalaid.app.R;

public class EditFarmerActivity extends AppCompatActivity {

    private static final String TAG = "EditFarmerActivity";
    private static final int EDIT_FARM_INFO_REQUEST_CODE = 100;

    // UI Components
    private ImageButton btnBack;
    private TextView tvTitle, tvBirthday, tvFarmerId;
    private EditText etFirstName, etMiddleName, etLastName, etContactNumber;
    private AppCompatButton btnNext;
    private View progressOverlay;

    // Data
    private FirebaseFirestore db;
    private String farmerId, farmerName, barangay, farmerDocumentId;

    // Predefined barangays in Maramag Municipality
    private final String[] availableBarangays = {
            "Anahawon", "Base Camp", "Bayabason", "Camp 1", "Colambugon",
            "Dagumba-an", "Danggawan", "Dologon", "Kisanday", "Kuya",
            "La Roxas", "Panadtalan", "Panalsalan", "North Poblacion", "South Poblacion",
            "San Miguel", "San Roque", "Tubigon", "Kiharong", "Bagongsilang"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_farmer);

        db = FirebaseFirestore.getInstance();

        // Get data from intent
        farmerId = getIntent().getStringExtra("farmerId");
        farmerName = getIntent().getStringExtra("farmerName");
        barangay = getIntent().getStringExtra("barangay");
        farmerDocumentId = getIntent().getStringExtra("farmerDocumentId");

        // Debug logging
        Log.d(TAG, "Received data - farmerId: " + farmerId + ", farmerName: " + farmerName +
                ", barangay: " + barangay + ", farmerDocumentId: " + farmerDocumentId);

        initViews();
        setupClickListeners();

        // Handle missing barangay information gracefully
        if (barangay == null || barangay.isEmpty()) {
            handleMissingBarangay();
        } else {
            // Validate barangay exists in our predefined list
            boolean isValidBarangay = false;
            for (String validBarangay : availableBarangays) {
                if (validBarangay.equalsIgnoreCase(barangay)) {
                    barangay = validBarangay; // Use correct case
                    isValidBarangay = true;
                    break;
                }
            }

            if (isValidBarangay) {
                loadFarmerData();
            } else {
                Log.w(TAG, "Invalid barangay: " + barangay);
                handleMissingBarangay();
            }
        }
    }

    private void handleMissingBarangay() {
        Log.w(TAG, "Missing or invalid barangay information, attempting to resolve...");

        // Try to get barangay from user's profile or farmer data
        if (farmerId != null || farmerDocumentId != null) {
            attemptBarangayRecovery();
        } else {
            showBarangaySelectionDialog();
        }
    }

    private void attemptBarangayRecovery() {
        Log.d(TAG, "Attempting to recover barangay information...");

        // First, try to get user's barangay from their profile
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            if (progressOverlay != null) {
                progressOverlay.setVisibility(View.VISIBLE);
            }

            db.collection("Users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(userDocument -> {
                        if (userDocument.exists()) {
                            String userBarangay = userDocument.getString("Barangay");
                            if (userBarangay == null || userBarangay.isEmpty()) {
                                userBarangay = userDocument.getString("barangay");
                            }

                            if (userBarangay != null && !userBarangay.isEmpty()) {
                                // Validate the barangay
                                for (String validBarangay : availableBarangays) {
                                    if (validBarangay.equalsIgnoreCase(userBarangay)) {
                                        barangay = validBarangay;
                                        Log.d(TAG, "Recovered barangay from user profile: " + barangay);
                                        if (progressOverlay != null) {
                                            progressOverlay.setVisibility(View.GONE);
                                        }
                                        loadFarmerData();
                                        return;
                                    }
                                }
                            }
                        }

                        // If user barangay not found, try searching all barangays for the farmer
                        searchFarmerInAllBarangays();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error getting user barangay", e);
                        searchFarmerInAllBarangays();
                    });
        } else {
            searchFarmerInAllBarangays();
        }
    }

    private void searchFarmerInAllBarangays() {
        Log.d(TAG, "Searching for farmer in all barangays...");

        if (progressOverlay != null) {
            progressOverlay.setVisibility(View.VISIBLE);
        }

        // Search for the farmer in all available barangays
        searchInBarangayList(0);
    }

    private void searchInBarangayList(int barangayIndex) {
        if (barangayIndex >= availableBarangays.length) {
            // Farmer not found in any barangay
            if (progressOverlay != null) {
                progressOverlay.setVisibility(View.GONE);
            }
            Log.w(TAG, "Farmer not found in any barangay");
            showBarangaySelectionDialog();
            return;
        }

        String searchBarangay = availableBarangays[barangayIndex];
        Log.d(TAG, "Searching in barangay: " + searchBarangay);

        // Search by farmer document ID first
        if (farmerDocumentId != null && !farmerDocumentId.isEmpty()) {
            db.collection("Barangays")
                    .document(searchBarangay)
                    .collection("Farmers")
                    .document(farmerDocumentId)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            barangay = searchBarangay;
                            Log.d(TAG, "Found farmer by document ID in barangay: " + barangay);
                            if (progressOverlay != null) {
                                progressOverlay.setVisibility(View.GONE);
                            }
                            loadFarmerData();
                        } else {
                            // Try searching by farmer ID in this barangay
                            searchByFarmerIdInBarangay(searchBarangay, barangayIndex);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error searching in barangay: " + searchBarangay, e);
                        searchByFarmerIdInBarangay(searchBarangay, barangayIndex);
                    });
        } else {
            searchByFarmerIdInBarangay(searchBarangay, barangayIndex);
        }
    }

    private void searchByFarmerIdInBarangay(String searchBarangay, int barangayIndex) {
        if (farmerId != null && !farmerId.isEmpty()) {
            db.collection("Barangays")
                    .document(searchBarangay)
                    .collection("Farmers")
                    .whereEqualTo("farmerId", farmerId)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            barangay = searchBarangay;
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                farmerDocumentId = document.getId();
                                break;
                            }
                            Log.d(TAG, "Found farmer by farmer ID in barangay: " + barangay);
                            if (progressOverlay != null) {
                                progressOverlay.setVisibility(View.GONE);
                            }
                            loadFarmerData();
                        } else {
                            // Continue searching in next barangay
                            searchInBarangayList(barangayIndex + 1);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error searching by farmer ID in barangay: " + searchBarangay, e);
                        searchInBarangayList(barangayIndex + 1);
                    });
        } else {
            // No farmer ID to search with, continue to next barangay
            searchInBarangayList(barangayIndex + 1);
        }
    }

    private void showBarangaySelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Barangay");
        builder.setMessage("Please select the barangay where this farmer is located:");

        builder.setItems(availableBarangays, (dialog, which) -> {
            barangay = availableBarangays[which];
            Log.d(TAG, "User selected barangay: " + barangay);
            loadFarmerData();
            dialog.dismiss();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            Toast.makeText(this, "Cannot proceed without barangay information", Toast.LENGTH_LONG).show();
            finish();
        });

        builder.setCancelable(false);
        builder.show();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvTitle = findViewById(R.id.tv_title);
        tvFarmerId = findViewById(R.id.tv_farmer_id);
        etFirstName = findViewById(R.id.et_first_name);
        etMiddleName = findViewById(R.id.et_middle_initial);
        etLastName = findViewById(R.id.et_last_name);
        tvBirthday = findViewById(R.id.tv_birthday);
        etContactNumber = findViewById(R.id.et_phone);
        btnNext = findViewById(R.id.btn_next);
        progressOverlay = findViewById(R.id.progress_overlay);

        // Set title
        if (tvTitle != null) {
            tvTitle.setText("Edit Farmer Information");
        }

        // Set hints for EditText fields to make them visible
        if (etFirstName != null) {
            etFirstName.setHint("Enter first name");
        }

        if (etMiddleName != null) {
            etMiddleName.setHint("Enter middle initial");
        }

        if (etLastName != null) {
            etLastName.setHint("Enter last name");
        }

        if (etContactNumber != null) {
            etContactNumber.setHint("Enter contact number (e.g., 09123456789)");
        }

        // Hide progress overlay initially
        if (progressOverlay != null) {
            progressOverlay.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        } else {
            Log.e(TAG, "btnBack is null - check if R.id.btn_back exists in layout");
        }

        if (btnNext != null) {
            btnNext.setOnClickListener(v -> {
                if (validateForm()) {
                    proceedToFarmInformation();
                }
            });
        } else {
            Log.e(TAG, "btnNext is null - check if R.id.btn_next exists in layout");
            Toast.makeText(this, "UI initialization error", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadFarmerData() {
        if (barangay == null || barangay.isEmpty()) {
            Toast.makeText(this, "Cannot load farmer data: Missing barangay information", Toast.LENGTH_SHORT).show();
            handleMissingBarangay();
            return;
        }

        if (progressOverlay != null) {
            progressOverlay.setVisibility(View.VISIBLE);
        }

        // Try multiple approaches to find the farmer
        if (farmerDocumentId != null && !farmerDocumentId.isEmpty()) {
            // First try: Use the provided document ID
            loadFarmerByDocumentId(farmerDocumentId);
        } else if (farmerId != null && !farmerId.isEmpty()) {
            // Second try: Search by farmer ID
            loadFarmerByFarmerId(farmerId);
        } else if (farmerName != null && !farmerName.isEmpty()) {
            // Third try: Use farmer name as document ID
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
                    Log.d(TAG, "Document data: " + document.getData());
                    if (progressOverlay != null) {
                        progressOverlay.setVisibility(View.GONE);
                    }
                    farmerDocumentId = documentId; // Store the working document ID
                    populateFormWithData(document);
                } else {
                    Log.d(TAG, "Farmer not found by document ID: " + documentId);
                    // If document ID didn't work, try searching by farmer ID
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
                // Try alternative method
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
                        for (com.google.firebase.firestore.QueryDocumentSnapshot document : task.getResult()) {
                            Log.d(TAG, "Farmer found by farmer ID: " + searchFarmerId + ", Document ID: " + document.getId());
                            Log.d(TAG, "Document data: " + document.getData());
                            if (progressOverlay != null) {
                                progressOverlay.setVisibility(View.GONE);
                            }
                            farmerDocumentId = document.getId(); // Store the actual document ID
                            populateFormWithData(document);
                            return;
                        }
                    }

                    // If we reach here, farmer was not found
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

    private void populateFormWithData(DocumentSnapshot document) {
        try {
            // Get farmer ID and display it
            String farmerIdFromDb = document.getString("farmerId");
            String firstName = document.getString("firstName");
            String middleInitial = document.getString("middleInitial");
            String lastName = document.getString("lastName");
            String birthday = document.getString("birthday");

            // Try different possible field names for phone/contact
            String contactNumber = document.getString("contactNumber");
            if (contactNumber == null) {
                contactNumber = document.getString("phoneNumber");
            }
            if (contactNumber == null) {
                contactNumber = document.getString("phone");
            }
            if (contactNumber == null) {
                contactNumber = document.getString("mobile");
            }

            Log.d(TAG, "Retrieved data - farmerId: " + farmerIdFromDb +
                    ", firstName: " + firstName +
                    ", lastName: " + lastName +
                    ", middleInitial: " + middleInitial +
                    ", birthday: " + birthday +
                    ", contactNumber: " + contactNumber);

            // Populate the farmer ID
            if (farmerIdFromDb != null && tvFarmerId != null) {
                tvFarmerId.setText(farmerIdFromDb);
                farmerId = farmerIdFromDb; // Update the farmerId variable
            }

            // Populate the form fields
            if (firstName != null && etFirstName != null) {
                etFirstName.setText(firstName);
            }

            if (middleInitial != null && etMiddleName != null) {
                etMiddleName.setText(middleInitial);
            }

            if (lastName != null && etLastName != null) {
                etLastName.setText(lastName);
            }

            if (contactNumber != null && etContactNumber != null) {
                etContactNumber.setText(contactNumber);
            } else {
                Log.w(TAG, "Contact number not found in database");
            }

            if (birthday != null && tvBirthday != null) {
                tvBirthday.setText(birthday);
            }

            Log.d(TAG, "Form populated with farmer data");

        } catch (Exception e) {
            Log.e(TAG, "Error populating form data", e);
            Toast.makeText(this, "Error loading form data", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateForm() {
        boolean isValid = true;

        // Validate first name
        if (etFirstName != null && TextUtils.isEmpty(etFirstName.getText())) {
            etFirstName.setError("First name is required");
            isValid = false;
        }

        // Validate last name
        if (etLastName != null && TextUtils.isEmpty(etLastName.getText())) {
            etLastName.setError("Last name is required");
            isValid = false;
        }

        // Validate contact number
        if (etContactNumber != null && TextUtils.isEmpty(etContactNumber.getText())) {
            etContactNumber.setError("Contact number is required");
            isValid = false;
        } else if (etContactNumber != null) {
            String contact = etContactNumber.getText().toString();
            if (contact.length() < 10) {
                etContactNumber.setError("Contact number must be at least 10 digits");
                isValid = false;
            }
        }

        return isValid;
    }

    // FIXED: Updated to use startActivityForResult and handle the result properly
    private void proceedToFarmInformation() {
        // Create the updated farmer name
        String firstName = etFirstName != null ? etFirstName.getText().toString().trim() : "";
        String middleName = etMiddleName != null ? etMiddleName.getText().toString().trim() : "";
        String lastName = etLastName != null ? etLastName.getText().toString().trim() : "";

        String updatedFarmerName;
        if (!middleName.isEmpty()) {
            updatedFarmerName = lastName + ", " + firstName + " " + middleName.charAt(0) + ".";
        } else {
            updatedFarmerName = lastName + ", " + firstName;
        }

        // Pass all the data to EditFarmInformationActivity
        Intent intent = new Intent(this, EditFarmInformationActivity.class);

        // Pass original identifiers - IMPORTANT: Make sure barangay is passed!
        intent.putExtra("farmerId", farmerId);
        intent.putExtra("farmerName", updatedFarmerName);
        intent.putExtra("barangay", barangay);
        intent.putExtra("farmerDocumentId", farmerDocumentId);

        // Pass the updated basic information
        intent.putExtra("firstName", firstName);
        intent.putExtra("middleName", middleName);
        intent.putExtra("lastName", lastName);

        String contactNumber = etContactNumber != null ? etContactNumber.getText().toString().trim() : "";
        intent.putExtra("contactNumber", contactNumber);

        Log.d(TAG, "Proceeding to EditFarmInformationActivity with barangay: " + barangay);
        startActivityForResult(intent, EDIT_FARM_INFO_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_FARM_INFO_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // Farm information was saved successfully
                // Return to the previous activity (FarmersDetailsActivity) with success result
                Intent resultIntent = new Intent();
                resultIntent.putExtra("updated", true);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
            // If RESULT_CANCELED, user came back without saving, so stay in this activity
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
