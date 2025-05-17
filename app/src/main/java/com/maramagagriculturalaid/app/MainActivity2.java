package com.maramagagriculturalaid.app;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.maramagagriculturalaid.app.FarmersData.FarmersDataFragment;
import com.maramagagriculturalaid.app.databinding.ActivityMain2Binding;

public class MainActivity2 extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    ActivityMain2Binding binding;
    private Dialog bottomDrawerDialog;
    private DrawerLayout drawerLayout;
    NavigationView navigationView;
    FirebaseFirestore db;
    private FirebaseAuth mAuth;
    String userId;
    TextView txtBarangayName, txtEmail;
    Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMain2Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        drawerLayout = binding.drawerLayout;
        navigationView = binding.navigationView;

        // Set NavigationView listener
        navigationView.setNavigationItemSelectedListener(MainActivity2.this);

        // Set up bottom nav
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.menu) {
                if (!drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.openDrawer(GravityCompat.END);
                } else {
                    drawerLayout.closeDrawer(GravityCompat.END);
                }
                return true;
            } else if (itemId == R.id.home) {
                replaceFragment(new HomeFragment());
                return true;
            } else if (itemId == R.id.farmersData) {
                replaceFragment(new FarmersDataFragment());
                return true;
            } else if (itemId == R.id.notifications) {
                replaceFragment(new NotificationsFragment());
                return true;
            }

            return false;
        });

        initBottomDrawerDialog();
        binding.fab.setOnClickListener(v -> toggleBottomDrawer());

        replaceFragment(new HomeFragment());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize userId from current authenticated user
        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        } else {
            // Handle case when user is not authenticated
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity2.this, MainActivity.class));
            finish();
            return;
        }

        View headerView = navigationView.getHeaderView(0);
        txtBarangayName = headerView.findViewById(R.id.BarangayName);
        txtEmail = headerView.findViewById(R.id.BarangayEmail);
        btnLogout = headerView.findViewById(R.id.logout);

        DocumentReference docRef = db.collection("Users").document(userId);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    String name = document.getString("Name");
                    String email = document.getString("Email");

                    if (name != null) {
                        txtBarangayName.setText("Barangay" + " " + name);
                    }

                    if (email != null) {
                        txtEmail.setText(email);
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
        drawerLayout.closeDrawer(GravityCompat.END);

        if (itemId == R.id.nav_home) {
            replaceFragment(new HomeFragment());
            return true;
        } else if (itemId == R.id.nav_settings) {
            replaceFragment(new SettingsFragment());
            return true;
        } else if (itemId == R.id.nav_about) {
            replaceFragment(new AboutUsFragment());
            return true;
        } else if (itemId == R.id.logout) {
            Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.logout);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setCancelable(false);

            Button btnYes = dialog.findViewById(R.id.btnYes);
            Button btnNo = dialog.findViewById(R.id.btnNo);

            btnYes.setOnClickListener(v -> {
                dialog.dismiss();
                mAuth.signOut();
                Toast.makeText(MainActivity2.this, "Logged out", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity2.this, MainActivity.class));
                finish();
            });

            btnNo.setOnClickListener(v -> dialog.dismiss());

            dialog.show();
            return true;
        }

        return false;
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.frameLayout, fragment);
        transaction.commit();
    }

    private void toggleBottomDrawer() {
        if (bottomDrawerDialog != null && bottomDrawerDialog.isShowing()) {
            bottomDrawerDialog.dismiss();
        } else {
            bottomDrawerDialog.show();
        }
    }

    private void initBottomDrawerDialog() {
        bottomDrawerDialog = new Dialog(this);
        bottomDrawerDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        bottomDrawerDialog.setContentView(R.layout.bottom_sheet_layout);

        LinearLayout seeds = bottomDrawerDialog.findViewById(R.id.SeedsFertilizers);
        LinearLayout livestock = bottomDrawerDialog.findViewById(R.id.LivestockSupport);
        LinearLayout money = bottomDrawerDialog.findViewById(R.id.MonetarySupport);
        ImageView cancelButton = bottomDrawerDialog.findViewById(R.id.cancelButton);

        seeds.setOnClickListener(v -> {
            bottomDrawerDialog.dismiss();
            Toast.makeText(MainActivity2.this, "Seeds and Fertilizers clicked", Toast.LENGTH_SHORT).show();
        });

        livestock.setOnClickListener(v -> {
            bottomDrawerDialog.dismiss();
            Toast.makeText(MainActivity2.this, "Livestock Support clicked", Toast.LENGTH_SHORT).show();
        });

        money.setOnClickListener(v -> {
            bottomDrawerDialog.dismiss();
            Toast.makeText(MainActivity2.this, "Monetary Support clicked", Toast.LENGTH_SHORT).show();
        });

        cancelButton.setOnClickListener(v -> bottomDrawerDialog.dismiss());

        if (bottomDrawerDialog.getWindow() != null) {
            bottomDrawerDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            bottomDrawerDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            bottomDrawerDialog.getWindow().setWindowAnimations(R.style.BottomDialogAnimation);
            bottomDrawerDialog.getWindow().setGravity(Gravity.BOTTOM);
        }
    }

}