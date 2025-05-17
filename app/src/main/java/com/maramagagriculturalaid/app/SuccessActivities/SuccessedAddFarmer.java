package com.maramagagriculturalaid.app.SuccessActivities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.maramagagriculturalaid.app.MainActivity;
import com.maramagagriculturalaid.app.R;

public class SuccessedAddFarmer extends AppCompatActivity {

    private TextView tvSuccessMessage;
    private ImageView ivSuccessIcon;
    private Button btnBackHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_successed_add_farmer);

        // Initialize views
        tvSuccessMessage = findViewById(R.id.tv_success_message);
        ivSuccessIcon = findViewById(R.id.iv_success_icon);
        btnBackHome = findViewById(R.id.btn_back_home);

        // Get custom message if provided
        String successMessage = getIntent().getStringExtra("success_message");
        if (successMessage != null && !successMessage.isEmpty()) {
            tvSuccessMessage.setText(successMessage);
        }

        // Set up click listener for the back to home button
        btnBackHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to the home/main activity and clear the back stack
                Intent intent = new Intent(SuccessedAddFarmer.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        // Override back button to go to home instead of previous screen
        super.onBackPressed();
        Intent intent = new Intent(SuccessedAddFarmer.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}