package com.maramagagriculturalaid.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FarmersDataFragment extends Fragment {

    private static final String TAG = "FarmersDataFragment";

    private TextInputEditText editTextFarmerId;
    private MaterialButton buttonSearch, buttonViewAllFarmers, buttonAddFarmer;
    private ProgressBar progressBar;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_farmers_data, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize views
        editTextFarmerId = view.findViewById(R.id.editTextFarmerId);
        buttonSearch = view.findViewById(R.id.buttonSearch);
        buttonViewAllFarmers = view.findViewById(R.id.buttonViewAllFarmers);
        buttonAddFarmer = view.findViewById(R.id.buttonAddFarmer);
        progressBar = view.findViewById(R.id.progressBar);

        // Set up click listeners
        setupClickListeners();

        return view;
    }

    private void setupClickListeners() {
        // Search for a specific farmer by ID
        buttonSearch.setOnClickListener(v -> {
            String farmerId = editTextFarmerId.getText().toString().trim();
            if (TextUtils.isEmpty(farmerId)) {
                editTextFarmerId.setError("Please enter a Farmer ID");
                return;
            }

            searchFarmerById(farmerId);
        });

        // View all farmers
        buttonViewAllFarmers.setOnClickListener(v -> {
            loadAllFarmers();
        });

        // Add a new farmer
        buttonAddFarmer.setOnClickListener(v -> {
            // Navigate to add farmer screen
            navigateToAddFarmer();
        });
    }

    private void searchFarmerById(String farmerId) {
        showProgressBar(true);

        db.collection("farmers")
                .whereEqualTo("farmerId", farmerId)
                .get()
                .addOnCompleteListener(task -> {
                    showProgressBar(false);

                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            Toast.makeText(getContext(), "No farmer found with ID: " + farmerId, Toast.LENGTH_LONG).show();
                        } else {
                            // Farmer found, navigate to details screen
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String documentId = document.getId();
                                navigateToFarmerDetails(documentId);
                                break; // Should only be one result
                            }
                        }
                    } else {
                        Log.e(TAG, "Error searching for farmer", task.getException());
                        Toast.makeText(getContext(), "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadAllFarmers() {
        showProgressBar(true);

        db.collection("farmers")
                .get()
                .addOnCompleteListener(task -> {
                    showProgressBar(false);

                    if (task.isSuccessful()) {
                        List<String> farmerIds = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            farmerIds.add(document.getId());
                        }

                        if (farmerIds.isEmpty()) {
                            Toast.makeText(getContext(), "No farmers found in the database", Toast.LENGTH_LONG).show();
                        } else {
                            // Navigate to list screen with all farmers
                            navigateToFarmersList(farmerIds);
                        }
                    } else {
                        Log.e(TAG, "Error loading farmers", task.getException());
                        Toast.makeText(getContext(), "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToFarmerDetails(String farmerId) {
        Intent intent = new Intent(requireContext(), FarmersDetialsFragment.class);
        intent.putExtra("FARMER_ID", farmerId);
        startActivity(intent);
    }

    private void navigateToFarmersList(List<String> farmerIds) {
        Intent intent = new Intent(requireContext(), FarmersListFragment.class);
        intent.putStringArrayListExtra("FARMER_IDS", new ArrayList<>(farmerIds));
        startActivity(intent);
    }

    private void navigateToAddFarmer() {
        Intent intent = new Intent(requireContext(), AddFarmerFragment.class);
        startActivity(intent);
    }

    private void showProgressBar(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        buttonSearch.setEnabled(!show);
        buttonViewAllFarmers.setEnabled(!show);
        buttonAddFarmer.setEnabled(!show);
    }
}