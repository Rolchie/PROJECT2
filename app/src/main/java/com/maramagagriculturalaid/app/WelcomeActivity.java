package com.maramagagriculturalaid.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    Button getStartedButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        getStartedButton = findViewById(R.id.btn_get_started);

        getStartedButton.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }

}