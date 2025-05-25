package com.maramagagriculturalaid.app.SuccessActivities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.maramagagriculturalaid.app.R;
import com.maramagagriculturalaid.app.MainActivity;

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
                // Navigate back to MainActivity with MunicipalHomeFragment
                Intent intent = new Intent(SuccessSubsidyApplication.this, MainActivity.class);

                // Add extra to specify which fragment to show
                intent.putExtra("navigate_to_fragment", "municipal_home");

                // Clear the activity stack to prevent going back to success screen
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                startActivity(intent);
                finish();
            }
        });
    }
}
