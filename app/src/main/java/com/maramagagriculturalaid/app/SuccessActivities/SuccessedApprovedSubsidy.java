    package com.maramagagriculturalaid.app.SuccessActivities;

    import android.content.Intent;
    import android.os.Bundle;

    import androidx.activity.EdgeToEdge;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.appcompat.widget.AppCompatButton;
    import androidx.core.graphics.Insets;
    import androidx.core.view.ViewCompat;
    import androidx.core.view.WindowInsetsCompat;

    import com.maramagagriculturalaid.app.Municipal.MunicipalActivity;
    import com.maramagagriculturalaid.app.R;

    public class SuccessedApprovedSubsidy extends AppCompatActivity {

        private AppCompatButton btnBackHome;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_successed_approved_subsidy);

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

            initializeViews();
            setupClickListeners();
        }

        private void initializeViews() {
            btnBackHome = findViewById(R.id.btn_back_home);
        }

        private void setupClickListeners() {
            btnBackHome.setOnClickListener(v -> navigateToMunicipalHome());
        }

        private void navigateToMunicipalHome() {
            try {
                Intent intent = new Intent(this, MunicipalActivity.class);

                // Clear the activity stack and start fresh
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

                // Add extra to indicate we want to show the home fragment
                intent.putExtra("navigate_to", "home");

                startActivity(intent);
                finish(); // Close this success activity

            } catch (Exception e) {
                // Fallback: just finish this activity
                finish();
            }
        }

        @Override
        public void onBackPressed() {
            // Override back button to go to municipal home instead of previous activity
            super.onBackPressed();
            navigateToMunicipalHome();
        }
    }