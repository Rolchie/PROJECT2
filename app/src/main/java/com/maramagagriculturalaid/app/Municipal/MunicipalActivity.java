package com.maramagagriculturalaid.app.Municipal;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.maramagagriculturalaid.app.MainActivity;
import com.maramagagriculturalaid.app.Municipal.BarangayOverview.BarangayOverview;
import com.maramagagriculturalaid.app.Municipal.MunicipalNotifications.MunicipalNotificationsFragment;
import com.maramagagriculturalaid.app.Municipal.MunicipalSubsidy.MunicipalSubsidyManagement;
import com.maramagagriculturalaid.app.R;
import com.maramagagriculturalaid.app.databinding.ActivityMunicipalBinding;

public class MunicipalActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ActivityMunicipalBinding binding;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;
    private TextView txtMunicipalName, txtMunicipalEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMunicipalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize views from binding (matching XML IDs)
        drawerLayout = binding.drawerLayout;
        navigationView = binding.navigationView;

        // Set NavigationView listener
        navigationView.setNavigationItemSelectedListener(this);

        // Set up bottom navigation (matching your bottom menu XML)
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.menu) {
                // Open drawer to END (right side) to match XML layout_gravity="end"
                if (!drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.openDrawer(GravityCompat.END);
                } else {
                    drawerLayout.closeDrawer(GravityCompat.END);
                }
                return true;
            } else if (itemId == R.id.home) {
                replaceFragment(new MunicipalHomeFragment());
                return true;
            } else if (itemId == R.id.BarangayOverview) {
                replaceFragment(new BarangayOverview());
                return true;
            } else if (itemId == R.id.notifications) {
                replaceFragment(new MunicipalNotificationsFragment());
                return true;
            } else if (itemId == R.id.placeholder) {
                // Placeholder item - do nothing
                return false;
            }

            return false;
        });

        // FAB click listener (matching XML fab ID)
        binding.fab.setOnClickListener(v -> {
            startActivity(new Intent(MunicipalActivity.this, MunicipalSubsidyManagement.class));
        });

        // Start with MunicipalHomeFragment in frameLayout
        replaceFragment(new MunicipalHomeFragment());

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize userId from current authenticated user
        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
            loadUserData();
        } else {
            // Handle case when user is not authenticated
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MunicipalActivity.this, MainActivity.class));
            finish();
        }
    }

    private void loadUserData() {
        // Get header view from navigationView (matching your header XML)
        View headerView = navigationView.getHeaderView(0);

        // Find views in the header layout using CORRECT IDs from your XML
        txtMunicipalName = headerView.findViewById(R.id.MunicipalName);
        txtMunicipalEmail = headerView.findViewById(R.id.MunicipalEmail);

        // Load user data from Firestore
        DocumentReference docRef = db.collection("Users").document(userId);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    String name = document.getString("Municipal");
                    String email = document.getString("Email");

                    // Update UI elements if they exist (using correct variable names)
                    if (name != null && txtMunicipalName != null) {
                        txtMunicipalName.setText(name + " Municipality");
                    }

                    if (email != null && txtMunicipalEmail != null) {
                        txtMunicipalEmail.setText(email);
                    }
                } else {
                    Log.d("FirestoreDebug", "Document not found");
                    Toast.makeText(this, "User document not found", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e("FirestoreDebug", "Fetch failed: ", task.getException());
                Toast.makeText(this, "Failed to load user info", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        // Close drawer to END (matching XML layout_gravity="end")
        drawerLayout.closeDrawer(GravityCompat.END);

        if (itemId == R.id.nav_home) {
            replaceFragment(new MunicipalHomeFragment());
            return true;
        } else if (itemId == R.id.logout) {
            showLogoutDialog();
            return true;
        }

        return false;
    }

    private void showLogoutDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.logout);

        // Set transparent background if window exists
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setCancelable(false);

        Button btnYes = dialog.findViewById(R.id.btnYes);
        Button btnNo = dialog.findViewById(R.id.btnNo);

        if (btnYes != null) {
            btnYes.setOnClickListener(v -> {
                dialog.dismiss();
                mAuth.signOut();
                Toast.makeText(MunicipalActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MunicipalActivity.this, MainActivity.class));
                finish();
            });
        }

        if (btnNo != null) {
            btnNo.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void replaceFragment(Fragment fragment) {
        // Use frameLayout ID from XML
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.frameLayout, fragment);
        transaction.commit();
    }

    @Override
    public void onBackPressed() {
        // Handle back press for drawer (matching XML layout_gravity="end")
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }
}