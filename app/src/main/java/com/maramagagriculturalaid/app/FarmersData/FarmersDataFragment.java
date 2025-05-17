package com.maramagagriculturalaid.app.FarmersData;

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
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.maramagagriculturalaid.app.R;

import java.util.HashMap;
import java.util.Map;

public class FarmersDataFragment extends Fragment {

    private static final String TAG = "FarmersDataFragment";

    private AppCompatEditText editTextFarmerId;
    private AppCompatButton buttonSearch, buttonViewAllFarmers, buttonAddFarmer;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String selectedBarangay;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_farmers_data, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        editTextFarmerId = view.findViewById(R.id.editTextFarmerId);
        buttonSearch = view.findViewById(R.id.buttonSearch);
        buttonViewAllFarmers = view.findViewById(R.id.buttonViewAllFarmers);
        buttonAddFarmer = view.findViewById(R.id.buttonAddFarmer);
        progressBar = view.findViewById(R.id.progressBar);

        setupClickListeners();

        return view;
    }

    private void setupClickListeners() {
        buttonSearch.setOnClickListener(v -> {
            String farmerId = editTextFarmerId.getText().toString().trim();
            if (TextUtils.isEmpty(farmerId)) {
                editTextFarmerId.setError("Please enter a Farmer ID");
                return;
            }
            if (farmerId.length() != 5) {
                editTextFarmerId.setError("Farmer ID must be 5 digits");
                return;
            }
            searchFarmerById(farmerId);
        });

        buttonViewAllFarmers.setOnClickListener(v -> loadAllFarmers());
        buttonAddFarmer.setOnClickListener(v -> navigateToAddFarmer());
    }

    private void searchFarmerById(String farmerId) {
        showProgressBar(true);

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            showProgressBar(false);
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("Users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(userDocument -> {
                    if (userDocument.exists()) {
                        String barangay = userDocument.getString("Barangay");
                        selectedBarangay = barangay;

                        if (barangay == null || barangay.isEmpty()) {
                            searchInRootCollection(farmerId);
                        } else {
                            searchInBarangayCollection(farmerId, barangay);
                        }
                    } else {
                        showProgressBar(false);
                        Toast.makeText(getContext(), "User document not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    showProgressBar(false);
                    Toast.makeText(getContext(), "Error getting user data", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error getting user document", e);
                });
    }

    private void searchInBarangayCollection(String farmerId, String barangay) {
        db.collection("Barangays").document(barangay)
                .collection("Farmers")
                .whereEqualTo("farmerId", farmerId)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            DocumentSnapshot farmerDoc = task.getResult().getDocuments().get(0);
                            navigateToFarmerDetails(farmerDoc, barangay);
                        } else {
                            searchInRootCollection(farmerId);
                        }
                    } else {
                        showProgressBar(false);
                        Toast.makeText(getContext(), "Search error in barangay", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error searching barangay collection", task.getException());
                    }
                });
    }

    private void searchInRootCollection(String farmerId) {
        db.collection("Farmers")
                .whereEqualTo("farmerId", farmerId)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    showProgressBar(false);
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            DocumentSnapshot farmerDoc = task.getResult().getDocuments().get(0);
                            String barangay = farmerDoc.getString("barangay");
                            navigateToFarmerDetails(farmerDoc, barangay);
                        } else {
                            Toast.makeText(getContext(), "No farmer found with ID: " + farmerId, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Search error in root collection", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error searching root collection", task.getException());
                    }
                });
    }

    private void navigateToFarmerDetails(DocumentSnapshot farmerDoc, String barangay) {
        showProgressBar(false);

        if (!farmerDoc.exists()) {
            Toast.makeText(getContext(), "Farmer document not found", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent intent = new Intent(getContext(), FarmersDetailsActivity.class);

            // Pass all farmer data
            Map<String, Object> farmerData = farmerDoc.getData();
            if (farmerData != null) {
                for (Map.Entry<String, Object> entry : farmerData.entrySet()) {
                    if (entry.getValue() instanceof String) {
                        intent.putExtra(entry.getKey(), (String) entry.getValue());
                    }
                }
            }

            // Pass document reference
            intent.putExtra("documentId", farmerDoc.getId());
            intent.putExtra("barangay", barangay != null ? barangay : selectedBarangay);

            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error opening farmer details", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error navigating to FarmersDetailsActivity", e);
        }
    }

    private void loadAllFarmers() {
        showProgressBar(true);

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            showProgressBar(false);
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("Users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(userDocument -> {
                    if (userDocument.exists()) {
                        String barangay = userDocument.getString("Barangay");
                        if (barangay == null || barangay.isEmpty()) {
                            showProgressBar(false);
                            Toast.makeText(getContext(), "Barangay not assigned", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        navigateToFarmersListActivity(barangay);
                    } else {
                        showProgressBar(false);
                        Toast.makeText(getContext(), "User data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    showProgressBar(false);
                    Toast.makeText(getContext(), "Error getting user data", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error getting user document", e);
                });
    }

    private void navigateToFarmersListActivity(String barangay) {
        try {
            Intent intent = new Intent(getContext(), FarmersListActivity.class);
            intent.putExtra("barangay", barangay);
            startActivity(intent);
            showProgressBar(false);
        } catch (Exception e) {
            showProgressBar(false);
            Toast.makeText(getContext(), "Error opening farmers list", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error navigating to FarmersListActivity", e);
        }
    }

    private void navigateToAddFarmer() {
        try {
            startActivity(new Intent(requireActivity(), AddFarmerAcitivity.class));
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error opening add farmer screen", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error navigating to AddFarmerActivity", e);
        }
    }

    private void showProgressBar(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (buttonSearch != null) buttonSearch.setEnabled(!show);
        if (buttonViewAllFarmers != null) buttonViewAllFarmers.setEnabled(!show);
        if (buttonAddFarmer != null) buttonAddFarmer.setEnabled(!show);
    }
}