package com.maramagagriculturalaid.app.SuccessActivities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.maramagagriculturalaid.app.R;
import com.maramagagriculturalaid.app.SubsidyManagement.SubsidyRequest;

public class SuccessSubsidyApplication extends AppCompatActivity {

    private Button btnBackToHome;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success_subsidy_application);

        btnBackToHome = findViewById(R.id.btn_back_to_home);

        btnBackToHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to main activity and clear the stack
                Intent intent = new Intent(SuccessSubsidyApplication.this, SubsidyRequest.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
    }
}
