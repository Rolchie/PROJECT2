package com.maramagagriculturalaid.app.FarmersData;

import android.content.Intent;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.maramagagriculturalaid.app.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditFarmerActivity extends AppCompatActivity {

    private TextView tvFarmerId;
    private EditText etPhone, etFirstName, etLastName, etMiddleInitial;
    private TextView tvBirthday;
    private ImageButton btnBack;
    private Button btnSave, btnDelete;
    private View progressBar;

    private Calendar calendar;
    private SimpleDateFormat dateFormatter;
    private FirebaseFirestore db;

    // Variables to store original farmer data
    private String originalFarmerId;
    private String originalBarangay;
    private String originalDocumentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_farmer); // Make sure to create this layout

        db = FirebaseFirestore.getInstance();
        calendar = Calendar.getInstance();
        dateFormatter = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);

        initViews();
        setupClickListeners();

        // Get farmer data from intent
        Intent intent = getIntent();
        if (intent != null) {
            originalFarmerId = intent.getStringExtra("farmerId");
            originalBarangay = intent.getStringExtra("barangay");
            originalDocumentId = intent.getStringExtra("documentId");

            if (originalFarmerId != null && originalBarangay != null && originalDocumentId != null) {
                loadFarmerData();
            } else {
                Toast.makeText(this, "Error: Missing farmer information", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void initViews() {
        tvFarmerId = findViewById(R.id.tv_farmer_id);
        etPhone = findViewById(R.id.et_phone);
        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        etMiddleInitial = findViewById(R.id.et_middle_initial);
        tvBirthday = findViewById(R.id.tv_birthday);
        btnBack = findViewById(R.id.btn_back);
        btnSave = findViewById(R.id.btn_save);
        btnDelete = findViewById(R.id.btn_delete);
        progressBar = findViewById(R.id.progress_bar);

        if (progressBar != null) progressBar.setVisibility(View.GONE);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());

        btnSave.setOnClickListener(v -> {
            if (validateForm()) {
                updateFarmerInfo();
            }
        });

        btnDelete.setOnClickListener(v -> {
            showDeleteConfirmationDialog();
        });

        tvBirthday.setOnClickListener(v -> showDatePickerDialog());
    }

    private void loadFarmerData() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        db.collection("Barangays")
                .document(originalBarangay)
                .collection("Farmers")
                .document(originalDocumentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);

                    if (documentSnapshot.exists()) {
                        // Set the data to the views
                        tvFarmerId.setText(documentSnapshot.getString("farmerId"));
                        etPhone.setText(documentSnapshot.getString("phoneNumber"));
                        etFirstName.setText(documentSnapshot.getString("firstName"));
                        etLastName.setText(documentSnapshot.getString("lastName"));
                        etMiddleInitial.setText(documentSnapshot.getString("middleInitial"));
                        tvBirthday.setText(documentSnapshot.getString("birthday"));

                        // Set the calendar to the birthday date
                        try {
                            if (documentSnapshot.getString("birthday") != null) {
                                calendar.setTime(dateFormatter.parse(documentSnapshot.getString("birthday")));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        Toast.makeText(this, "Farmer data not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading farmer data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    tvBirthday.setText(dateFormatter.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        Calendar minAgeCalendar = Calendar.getInstance();
        minAgeCalendar.add(Calendar.YEAR, -15);
        datePickerDialog.getDatePicker().setMaxDate(minAgeCalendar.getTimeInMillis());
        datePickerDialog.getDatePicker().setMinDate(0); // Optional: prevent selecting before 1970

        datePickerDialog.show();
    }

    private boolean validateForm() {
        boolean isValid = true;

        if (TextUtils.isEmpty(etFirstName.getText())) {
            etFirstName.setError("First name is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(etLastName.getText())) {
            etLastName.setError("Last name is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(tvBirthday.getText())) {
            Toast.makeText(this, "Please select a birthday", Toast.LENGTH_SHORT).show();
            isValid = false;
        } else {
            Calendar minAgeCalendar = Calendar.getInstance();
            minAgeCalendar.add(Calendar.YEAR, -15);
            if (calendar.after(minAgeCalendar)) {
                Toast.makeText(this, "Must be at least 15 years old", Toast.LENGTH_SHORT).show();
                isValid = false;
            }
        }

        return isValid;
    }

    private void updateFarmerInfo() {
        String lastName = etLastName.getText().toString().trim();
        String firstName = etFirstName.getText().toString().trim();
        String middleInitial = etMiddleInitial.getText().toString().trim();
        String phoneNumber = etPhone.getText().toString().trim();
        String birthday = tvBirthday.getText().toString().trim();

        // Format the document ID as shown in the screenshot: "Sanchez, Rolch Vincent T."
        String newFarmerDocId = lastName + ", " + firstName;
        if (!TextUtils.isEmpty(middleInitial)) {
            newFarmerDocId += " " + middleInitial + ".";
        }

        // Show loading indicator
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        btnSave.setEnabled(false);

        // Check if the new name is different and already exists (only if name changed)
        if (!newFarmerDocId.equals(originalDocumentId)) {
            String finalNewFarmerDocId = newFarmerDocId;
            checkNameExists(newFarmerDocId, () -> {
                // Name doesn't exist, proceed with update
                performUpdate(finalNewFarmerDocId, lastName, firstName, middleInitial, phoneNumber, birthday);
            });
        } else {
            // Name hasn't changed, just update the existing document
            performUpdate(originalDocumentId, lastName, firstName, middleInitial, phoneNumber, birthday);
        }
    }

    private void checkNameExists(String newDocId, Runnable onNameAvailable) {
        String[] barangays = {"Anahawon", "Base Camp", "Bayabason", "Bagongsilang", "Camp 1", "Colambugon", "Dagumba-an","Danggawan", "Dologon", "Kisanday", "Kuya", "La Roxas", "Panadtalan", "Panalsalan", "North Poblacion", "South Poblacion", "San Miguel", "San Roque", "Tubigon", "Kiharong"};

        final int[] nameChecksCompleted = {0};
        final boolean[] farmerNameExists = {false};

        for (String barangay : barangays) {
            db.collection("Barangays")
                    .document(barangay)
                    .collection("Farmers")
                    .document(newDocId)
                    .get()
                    .addOnCompleteListener(task -> {
                        nameChecksCompleted[0]++;

                        if (task.isSuccessful() && task.getResult().exists()) {
                            farmerNameExists[0] = true;
                        }

                        if (nameChecksCompleted[0] == barangays.length) {
                            if (progressBar != null && nameChecksCompleted[0] == barangays.length) {
                                if (farmerNameExists[0]) {
                                    progressBar.setVisibility(View.GONE);
                                    btnSave.setEnabled(true);
                                    Toast.makeText(EditFarmerActivity.this,
                                            "A farmer with this name already exists", Toast.LENGTH_LONG).show();
                                } else {
                                    // Name is available, proceed with update
                                    onNameAvailable.run();
                                }
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        nameChecksCompleted[0]++;

                        if (nameChecksCompleted[0] == barangays.length) {
                            if (!farmerNameExists[0]) {
                                // Name is available, proceed with update
                                onNameAvailable.run();
                            } else {
                                progressBar.setVisibility(View.GONE);
                                btnSave.setEnabled(true);
                            }
                        }
                    });
        }
    }

    private void performUpdate(String documentId, String lastName, String firstName, String middleInitial,
                               String phoneNumber, String birthday) {
        // Create updated farmer data
        Map<String, Object> farmerData = new HashMap<>();
        farmerData.put("farmerId", originalFarmerId); // Keep the original ID
        farmerData.put("firstName", firstName);
        farmerData.put("lastName", lastName);
        farmerData.put("middleInitial", middleInitial);
        farmerData.put("phoneNumber", phoneNumber);
        farmerData.put("birthday", birthday);

        DocumentReference originalDocRef = db.collection("Barangays")
                .document(originalBarangay)
                .collection("Farmers")
                .document(originalDocumentId);

        DocumentReference newDocRef = db.collection("Barangays")
                .document(originalBarangay)
                .collection("Farmers")
                .document(documentId);

        // If document ID hasn't changed, just update the existing document
        if (documentId.equals(originalDocumentId)) {
            originalDocRef.update(farmerData)
                    .addOnSuccessListener(aVoid -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        btnSave.setEnabled(true);
                        Toast.makeText(EditFarmerActivity.this, "Farmer information updated successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        btnSave.setEnabled(true);
                        Toast.makeText(EditFarmerActivity.this, "Error updating farmer: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // If document ID has changed, we need to:
            // 1. Create a new document with the new ID
            // 2. Copy all data from the old document
            // 3. Delete the old document

            // First get all data from the original document
            originalDocRef.get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Create a new map with all the original data
                            Map<String, Object> allData = documentSnapshot.getData();
                            if (allData != null) {
                                // Update with the new farmer info
                                allData.putAll(farmerData);

                                // Create the new document
                                newDocRef.set(allData)
                                        .addOnSuccessListener(aVoid -> {
                                            // Delete the old document
                                            originalDocRef.delete()
                                                    .addOnSuccessListener(aVoid1 -> {
                                                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                                                        btnSave.setEnabled(true);
                                                        Toast.makeText(EditFarmerActivity.this, "Farmer information updated successfully", Toast.LENGTH_SHORT).show();
                                                        finish();
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                                                        btnSave.setEnabled(true);
                                                        Toast.makeText(EditFarmerActivity.this, "Error cleaning up old data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    });
                                        })
                                        .addOnFailureListener(e -> {
                                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                                            btnSave.setEnabled(true);
                                            Toast.makeText(EditFarmerActivity.this, "Error creating updated record: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            }
                        } else {
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                            btnSave.setEnabled(true);
                            Toast.makeText(EditFarmerActivity.this, "Original farmer data not found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        btnSave.setEnabled(true);
                        Toast.makeText(EditFarmerActivity.this, "Error retrieving original data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Farmer")
                .setMessage("Are you sure you want to delete this farmer? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteFarmer();
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteFarmer() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        btnDelete.setEnabled(false);
        btnSave.setEnabled(false);

        db.collection("Barangays")
                .document(originalBarangay)
                .collection("Farmers")
                .document(originalDocumentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(EditFarmerActivity.this, "Farmer deleted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    btnDelete.setEnabled(true);
                    btnSave.setEnabled(true);
                    Toast.makeText(EditFarmerActivity.this, "Error deleting farmer: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}