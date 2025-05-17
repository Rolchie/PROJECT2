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

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.maramagagriculturalaid.app.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddFarmerAcitivity extends AppCompatActivity {

    private EditText etFarmerId, etPhone, etFirstName, etLastName, etMiddleInitial;
    private TextView tvBirthday;
    private ImageButton btnBack;
    private Button btnNext;
    private View progressBar;

    private Calendar calendar;
    private SimpleDateFormat dateFormatter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_farmer_acitivity); // Ensure this XML is named correctly

        db = FirebaseFirestore.getInstance();
        calendar = Calendar.getInstance();
        dateFormatter = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        etFarmerId = findViewById(R.id.et_farmer_id);
        // Set input filter to exactly 5 digits
        etFarmerId.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(5),
                (source, start, end, dest, dstart, dend) -> {
                    // Check if the result would exceed 5 characters
                    if ((dest.length() - (dend - dstart) + (end - start)) > 5) {
                        return "";
                    }

                    // Only allow digits
                    for (int i = start; i < end; i++) {
                        if (!Character.isDigit(source.charAt(i))) return "";
                    }
                    return null;
                }
        });

        etPhone = findViewById(R.id.et_phone);
        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        etMiddleInitial = findViewById(R.id.et_middle_initial);
        tvBirthday = findViewById(R.id.tv_birthday);
        btnBack = findViewById(R.id.btn_back);
        btnNext = findViewById(R.id.btn_next);
        progressBar = findViewById(R.id.progress_bar);

        if (progressBar != null) progressBar.setVisibility(View.GONE);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());
        btnNext.setOnClickListener(v -> {
            if (validateForm()) {
                validateAndCheckFarmerId();
            }
        });
        tvBirthday.setOnClickListener(v -> showDatePickerDialog());

        // Add text change listener to Farmer ID
        etFarmerId.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                // Clear error when text changes
                if (s.length() > 0) {
                    etFarmerId.setError(null);
                }
            }
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

        String farmerId = etFarmerId.getText().toString().trim();
        if (TextUtils.isEmpty(farmerId)) {
            etFarmerId.setError("Farmer ID is required");
            isValid = false;
        } else if (farmerId.length() != 5) {
            etFarmerId.setError("Farmer ID must be exactly 5 digits");
            isValid = false;
        } else {
            try {
                Integer.parseInt(farmerId);
            } catch (NumberFormatException e) {
                etFarmerId.setError("Must be numeric");
                isValid = false;
            }
        }

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

    private void validateAndCheckFarmerId() {
        String farmerId = etFarmerId.getText().toString().trim();

        // Show loading indicator
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        btnNext.setEnabled(false);

        // Check if farmer ID already exists in any barangay
        String[] barangays = {"Anahawon", "Base Camp", "Bayabason", "Bagongsilang", "Camp 1", "Colambugon", "Dagumba-an","Danggawan", "Dologon", "Kisanday", "Kuya", "La Roxas", "Panadtalan", "Panalsalan", "North Poblacion", "South Poblacion", "San Miguel", "San Roque", "Tubigon", "Kiharong"};

        // Counter for completed checks
        final int[] checksCompleted = {0};
        final boolean[] farmerIdExists = {false};

        for (String barangay : barangays) {
            db.collection("Barangays")
                    .document(barangay)
                    .collection("Farmers")
                    .whereEqualTo("farmerId", farmerId)
                    .get()
                    .addOnCompleteListener(task -> {
                        checksCompleted[0]++;

                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            farmerIdExists[0] = true;
                        }

                        // If all checks are completed
                        if (checksCompleted[0] == barangays.length) {
                            if (progressBar != null) {
                                progressBar.setVisibility(View.GONE);
                            }
                            btnNext.setEnabled(true);

                            if (farmerIdExists[0]) {
                                // Farmer ID already exists
                                etFarmerId.setError("This Farmer ID is already in use");
                                Toast.makeText(AddFarmerAcitivity.this,
                                        "A farmer with this ID already exists", Toast.LENGTH_LONG).show();
                            } else {
                                // Continue to check if name exists
                                checkFarmerNameExists();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        checksCompleted[0]++;

                        // If all checks are completed
                        if (checksCompleted[0] == barangays.length) {
                            if (progressBar != null) {
                                progressBar.setVisibility(View.GONE);
                            }
                            btnNext.setEnabled(true);

                            // Continue even if there was an error with some checks
                            if (farmerIdExists[0]) {
                                etFarmerId.setError("This Farmer ID is already in use");
                                Toast.makeText(AddFarmerAcitivity.this,
                                        "A farmer with this ID already exists", Toast.LENGTH_LONG).show();
                            } else {
                                checkFarmerNameExists();
                            }
                        }
                    });
        }
    }

    private void checkFarmerNameExists() {
        String lastName = etLastName.getText().toString().trim();
        String firstName = etFirstName.getText().toString().trim();
        String middleInitial = etMiddleInitial.getText().toString().trim();

        // Format the document ID
        String farmerDocId = lastName + ", " + firstName;
        if (!TextUtils.isEmpty(middleInitial)) {
            farmerDocId += " " + middleInitial + ".";
        }

        // Show loading indicator
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        btnNext.setEnabled(false);

        // Check if farmer name already exists in any barangay
        String[] barangays = {"Anahawon", "Base Camp", "Bayabason", "Bagongsilang", "Camp 1", "Colambugon", "Dagumba-an","Danggawan", "Dologon", "Kisanday", "Kuya", "La Roxas", "Panadtalan", "Panalsalan", "North Poblacion", "South Poblacion", "San Miguel", "San Roque", "Tubigon", "Kiharong"};

        // Counter for completed checks
        final int[] checksCompleted = {0};
        final boolean[] farmerNameExists = {false};

        for (String brgy : barangays) {
            String finalFarmerDocId = farmerDocId;
            db.collection("Barangays")
                    .document(brgy)
                    .collection("Farmers")
                    .document(finalFarmerDocId)
                    .get()
                    .addOnCompleteListener(nameTask -> {
                        checksCompleted[0]++;

                        if (nameTask.isSuccessful() && nameTask.getResult().exists()) {
                            farmerNameExists[0] = true;
                        }

                        // If all checks are completed for farmer name
                        if (checksCompleted[0] == barangays.length) {
                            if (progressBar != null) {
                                progressBar.setVisibility(View.GONE);
                            }
                            btnNext.setEnabled(true);

                            if (farmerNameExists[0]) {
                                // Farmer with this name already exists
                                Toast.makeText(AddFarmerAcitivity.this,
                                        "A farmer with this name already exists", Toast.LENGTH_LONG).show();
                            } else {
                                // Farmer doesn't exist, proceed to next screen
                                navigateToFarmInfo();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        checksCompleted[0]++;

                        // If all checks are completed for farmer name
                        if (checksCompleted[0] == barangays.length) {
                            if (progressBar != null) {
                                progressBar.setVisibility(View.GONE);
                            }
                            btnNext.setEnabled(true);

                            if (farmerNameExists[0]) {
                                // Farmer with this name already exists
                                Toast.makeText(AddFarmerAcitivity.this,
                                        "A farmer with this name already exists", Toast.LENGTH_LONG).show();
                            } else {
                                // Farmer doesn't exist, proceed to next screen
                                navigateToFarmInfo();
                            }
                        }
                    });
        }
    }


    private void showError(String message, boolean highlightIdField) {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        btnNext.setEnabled(true);
        if (highlightIdField) etFarmerId.setError(message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void navigateToFarmInfo() {
        Intent intent = new Intent(this, FarmInfoActivity.class);
        intent.putExtra("farmerId", etFarmerId.getText().toString().trim());
        intent.putExtra("phoneNumber", etPhone.getText().toString().trim());
        intent.putExtra("firstName", etFirstName.getText().toString().trim());
        intent.putExtra("lastName", etLastName.getText().toString().trim());
        intent.putExtra("middleInitial", etMiddleInitial.getText().toString().trim());
        intent.putExtra("birthday", tvBirthday.getText().toString().trim());
        startActivity(intent);
    }
}