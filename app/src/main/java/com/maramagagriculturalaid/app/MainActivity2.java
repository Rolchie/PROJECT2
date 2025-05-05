package com.maramagagriculturalaid.app;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.maramagagriculturalaid.app.databinding.ActivityMain2Binding;

public class MainActivity2 extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    ActivityMain2Binding binding;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private TextView txtBarangayName, txtEmail;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMain2Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        drawerLayout = binding.drawerLayout;
        navigationView = binding.navigationView;

        // Set NavigationView listener
        navigationView.setNavigationItemSelectedListener(this);

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

        // FAB click
        binding.fab.setOnClickListener(v -> showBottomDialog());

        // Show Home fragment by default
        replaceFragment(new HomeFragment());

        // Firebase setup
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Get header view for drawer items
        View headerView = navigationView.getHeaderView(0);
        txtBarangayName = headerView.findViewById(R.id.BarangayName);
        txtEmail = headerView.findViewById(R.id.BarangayEmail);
        btnLogout = headerView.findViewById(R.id.logout);

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(MainActivity2.this, "Logged out", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity2.this, MainActivity.class));
            finish();
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
        }

        return false;
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.frameLayout, fragment);
        transaction.commit();
    }

    private void showBottomDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottom_sheet_layout);

        LinearLayout seeds = dialog.findViewById(R.id.SeedsFertilizers);
        LinearLayout livestock = dialog.findViewById(R.id.LivestockSupport);
        LinearLayout money = dialog.findViewById(R.id.MonetarySupport);
        ImageView cancelButton = dialog.findViewById(R.id.cancelButton);

        seeds.setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(MainActivity2.this, "Seeds and Fertilizers clicked", Toast.LENGTH_SHORT).show();
        });

        livestock.setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(MainActivity2.this, "Livestock Support clicked", Toast.LENGTH_SHORT).show();
        });

        money.setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(MainActivity2.this, "Monetary Support clicked", Toast.LENGTH_SHORT).show();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().getAttributes().windowAnimations =
                    com.google.android.material.R.style.MaterialAlertDialog_Material3_Animation;
            dialog.getWindow().setGravity(Gravity.BOTTOM);
        }
    }
}
