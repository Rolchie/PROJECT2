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

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.maramagagriculturalaid.app.R;

public class EditFarmerActivity extends AppCompatActivity {

    private static final String TAG = "EditFarmerActivity";

    // UI Components
    private ImageButton btnBack;
    private TextView tvTitle, tvBirthday, tvFarmerId;
    private EditText etFirstName, etMiddleName, etLastName, etContactNumber;
    private AppCompatButton btnNext;
    private View progressOverlay;

    // Data
    private FirebaseFirestore db;
    private String farmerId, farmerName, barangay, farmerDocumentId;

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
        loadFarmerData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvTitle = findViewById(R.id.tv_title);
        tvFarmerId = findViewById(R.id.tv_farmer_id); // Add this TextView for farmer ID
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
        if (barangay == null) {
            Toast.makeText(this, "Missing barangay information", Toast.LENGTH_SHORT).show();
            finish();
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
                    Log.d(TAG, "Document data: " + document.getData()); // Debug log to see all fields
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
                            Log.d(TAG, "Document data: " + document.getData()); // Debug log
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
            String middleInitial = document.getString("middleInitial"); // Note: using middleInitial, not middleName
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

        // Pass original identifiers
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

        Log.d(TAG, "Proceeding to EditFarmInformationActivity with updated data");
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100) {
            if (resultCode == RESULT_OK) {
                // Farm information was saved successfully
                // Return to the previous activity with success result
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