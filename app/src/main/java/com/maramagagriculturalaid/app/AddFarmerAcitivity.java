package com.maramagagriculturalaid.app;

import android.content.Intent;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
public class AddFarmerAcitivity extends AppCompatActivity {

    // UI Components
    private EditText etFarmerId, etPhone, etFirstName, etLastName, etMiddleInitial;
    private TextView tvBirthday;
    private ImageButton btnBack;
    private Button btnNext;

    private Calendar calendar;
    private SimpleDateFormat dateFormatter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_farmer_acitivity);

        // Initialize date formatter
        calendar = Calendar.getInstance();
        dateFormatter = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);

        // Initialize views
        initViews();

        // Set up click listeners
        setupClickListeners();
    }

    private void initViews() {
        etFarmerId = findViewById(R.id.et_farmer_id);
        etPhone = findViewById(R.id.et_phone);
        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        etMiddleInitial = findViewById(R.id.et_middle_initial);
        tvBirthday = findViewById(R.id.tv_birthday);
        btnBack = findViewById(R.id.btn_back);
        btnNext = findViewById(R.id.btn_next);
    }

    private void setupClickListeners() {
        // Back button
        btnBack.setOnClickListener(v -> onBackPressed());

        // Next button
        btnNext.setOnClickListener(v -> {
            if (validateForm()) {
                navigateToFarmInfo();
            }
        });

        // Birthday field
        tvBirthday.setOnClickListener(v -> showDatePickerDialog());
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

        // Set maximum date to current date (no future birthdays)
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

        datePickerDialog.show();
    }

    private boolean validateForm() {
        boolean isValid = true;

        // Validate Farmer ID
        if (TextUtils.isEmpty(etFarmerId.getText())) {
            etFarmerId.setError("Farmer ID is required");
            isValid = false;
        }

        // Validate Phone Number (optional in screenshot)
        // No validation needed as it's marked optional

        // Validate First Name
        if (TextUtils.isEmpty(etFirstName.getText())) {
            etFirstName.setError("First name is required");
            isValid = false;
        }

        // Validate Last Name
        if (TextUtils.isEmpty(etLastName.getText())) {
            etLastName.setError("Last name is required");
            isValid = false;
        }

        // Validate Birthday
        if (TextUtils.isEmpty(tvBirthday.getText())) {
            Toast.makeText(this, "Please select a birthday", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private void navigateToFarmInfo() {
        // Create intent to navigate to FarmInformationActivity
        Intent intent = new Intent(this, FarmInfoActivity.class);

        // Pass farmer data to the next activity
        intent.putExtra("farmerId", etFarmerId.getText().toString().trim());
        intent.putExtra("phoneNumber", etPhone.getText().toString().trim());
        intent.putExtra("firstName", etFirstName.getText().toString().trim());
        intent.putExtra("lastName", etLastName.getText().toString().trim());
        intent.putExtra("middleInitial", etMiddleInitial.getText().toString().trim());
        intent.putExtra("birthday", tvBirthday.getText().toString().trim());

        // Start the activity
        startActivity(intent);
    }
}