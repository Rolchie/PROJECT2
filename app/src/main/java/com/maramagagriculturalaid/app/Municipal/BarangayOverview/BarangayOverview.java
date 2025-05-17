package com.maramagagriculturalaid.app.Municipal.BarangayOverview;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.maramagagriculturalaid.app.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class BarangayOverview extends Fragment {

    private static final String TAG = "BarangayOverview";

    // UI Components
    private TextView tvTotalBarangaysValue;
    private TextView tvTotalFarmersValue;
    private AppCompatButton buttonViewAllFarmers;

    // Firebase
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_barangay_overview, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        tvTotalBarangaysValue = view.findViewById(R.id.tv_total_barangays_value);
        tvTotalFarmersValue = view.findViewById(R.id.tv_total_farmers_value);
        buttonViewAllFarmers = view.findViewById(R.id.buttonViewAllFarmers);

        // Load data
        loadBarangayStatistics();

        // Set up button click listeners
        setupButtonListeners();

        return view;
    }

    private void loadBarangayStatistics() {
        Log.d(TAG, "Loading barangay statistics");

        // Count documents in the Barangays collection to get total barangays
        db.collection("Barangays")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalBarangays = queryDocumentSnapshots.size();
                    Log.d(TAG, "Found " + totalBarangays + " barangays");

                    if (tvTotalBarangaysValue != null) {
                        tvTotalBarangaysValue.setText(String.valueOf(totalBarangays));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load barangay data", e);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load barangay data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        // Count total farmers across all barangays
        countTotalFarmers();
    }

    private void countTotalFarmers() {
        Log.d(TAG, "Starting to count total farmers");

        final AtomicInteger totalFarmers = new AtomicInteger(0);
        final AtomicInteger barangaysProcessed = new AtomicInteger(0);

        // Get all barangays first
        db.collection("Barangays")
                .get()
                .addOnSuccessListener(barangaySnapshots -> {
                    int totalBarangays = barangaySnapshots.size();
                    Log.d(TAG, "Found " + totalBarangays + " barangays to process for farmer count");

                    if (totalBarangays == 0) {
                        updateFarmersCountUI(0);
                        return;
                    }

                    // For each barangay, count farmers in the "Farmers" subcollection
                    for (QueryDocumentSnapshot barangayDoc : barangaySnapshots) {
                        String barangayId = barangayDoc.getId();
                        Log.d(TAG, "Counting farmers in barangay: " + barangayId);

                        db.collection("Barangays")
                                .document(barangayId)
                                .collection("Farmers") // Fixed: Changed from "farmers" to "Farmers"
                                .get()
                                .addOnSuccessListener(farmerSnapshots -> {
                                    int farmersInBarangay = farmerSnapshots.size();
                                    totalFarmers.addAndGet(farmersInBarangay);

                                    Log.d(TAG, "Found " + farmersInBarangay + " farmers in " + barangayId);

                                    // Check if we've processed all barangays
                                    if (barangaysProcessed.incrementAndGet() == totalBarangays) {
                                        int finalCount = totalFarmers.get();
                                        Log.d(TAG, "Final total farmers count: " + finalCount);
                                        updateFarmersCountUI(finalCount);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error getting farmers for barangay " + barangayId, e);

                                    // Still increment processed count even on failure
                                    if (barangaysProcessed.incrementAndGet() == totalBarangays) {
                                        int finalCount = totalFarmers.get();
                                        Log.d(TAG, "Final total farmers count (with errors): " + finalCount);
                                        updateFarmersCountUI(finalCount);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load barangays for farmer count", e);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load farmer data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    updateFarmersCountUI(0);
                });
    }

    private void updateFarmersCountUI(int totalFarmers) {
        Log.d(TAG, "Updating farmers count UI with: " + totalFarmers);

        if (tvTotalFarmersValue != null && isAdded()) {
            // Format the number with commas for better readability
            String formattedCount = String.format(Locale.getDefault(), "%,d", totalFarmers);
            tvTotalFarmersValue.setText(formattedCount);
            Log.d(TAG, "Set farmers count to: " + formattedCount);
        } else {
            Log.w(TAG, "Cannot update farmers count - view not available or fragment not added");
        }
    }

    private void setupButtonListeners() {
        // View All Farmers button
        if (buttonViewAllFarmers != null) {
            buttonViewAllFarmers.setOnClickListener(v -> {
                Log.d(TAG, "View All Farmers button clicked");
                navigateToAllFarmers();
            });
        }
    }

    private void navigateToAllFarmers() {
        Log.d(TAG, "Navigating to all farmers view");

        try {
            // Create an intent to start the BarangayList activity
            Intent intent = new Intent(getActivity(), BarangayList.class);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to all farmers", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error opening farmers list", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "Fragment resumed, refreshing data");
        // Refresh data when fragment resumes
        loadBarangayStatistics();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "Fragment view destroyed");
    }
}