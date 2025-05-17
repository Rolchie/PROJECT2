package com.maramagagriculturalaid.app.FarmersData;

// import android.content.Intent; // Not needed for fragment navigation
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.maramagagriculturalaid.app.R;

public class FarmersDataFragment extends Fragment {

    private static final String TAG = "FarmersDataFragment";

    private AppCompatEditText editTextFarmerId;
    private AppCompatButton buttonSearch, buttonViewAllFarmers, buttonAddFarmer;
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
        // Validate farmerId before proceeding
        if (farmerId == null || farmerId.trim().isEmpty()) {
            Toast.makeText(getContext(), "Farmer ID not provided", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressBar(true);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showProgressBar(false);
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("Users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(userDocument -> {
                    if (userDocument.exists()) {
                        String userRole = userDocument.getString("Role");
                        String barangay = userDocument.getString("Barangay");

                        Log.d(TAG, "User Role: " + userRole);
                        Log.d(TAG, "Barangay: " + barangay);

                        if (barangay == null || barangay.trim().isEmpty()) {
                            showProgressBar(false);
                            Toast.makeText(getContext(), "Barangay not assigned to user", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (!"Barangay".equals(userRole)) {
                            showProgressBar(false);
                            Toast.makeText(getContext(), "User is not a Barangay user", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Search farmer within the user's barangay
                        db.collection("Barangays").document(barangay)
                                .collection("Farmers")
                                .whereEqualTo("farmerId", farmerId)
                                .get()
                                .addOnCompleteListener(task -> {
                                    showProgressBar(false);
                                    if (task.isSuccessful()) {
                                        if (task.getResult().isEmpty()) {
                                            Toast.makeText(getContext(), "No farmer found with ID: " + farmerId,
                                                    Toast.LENGTH_LONG).show();
                                        } else {
                                            for (QueryDocumentSnapshot document : task.getResult()) {
                                                String documentId = document.getId();
                                                navigateToFarmerDetails(documentId, barangay);
                                                break; // Only expect one result, so break here
                                            }
                                        }
                                    } else {
                                        Log.e(TAG, "Error searching for farmer", task.getException());
                                        Toast.makeText(getContext(), "Error: " + task.getException().getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });

                    } else {
                        showProgressBar(false);
                        Toast.makeText(getContext(), "User document not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    showProgressBar(false);
                    Log.e(TAG, "Error getting user data", e);
                    Toast.makeText(getContext(), "Error getting user data: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }



    // Updated to include barangayId
    private void navigateToFarmerDetails(String documentId, String barangayId) {
        Intent intent = new Intent(getActivity(), FarmersDetailsActivity.class);
        intent.putExtra("FARMER_ID", documentId);
        intent.putExtra("BARANGAY_ID", barangayId);
        startActivity(intent);
    }

    private void loadAllFarmers() {
        showProgressBar(true);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showProgressBar(false);
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("Users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(userDocument -> {
                    if (userDocument.exists()) {
                        String userRole = userDocument.getString("Role");
                        String barangay = userDocument.getString("Barangay");

                        if (barangay == null || barangay.isEmpty()) {
                            showProgressBar(false);
                            Toast.makeText(getContext(), "Barangay not assigned", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        db.collection("Barangays")
                                .document(barangay)
                                .collection("Farmers")
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    showProgressBar(false);
                                    if (queryDocumentSnapshots.isEmpty()) {
                                        Toast.makeText(getContext(), "No farmers found", Toast.LENGTH_SHORT).show();
                                    } else {
                                        navigateToFarmersListActivity(userRole, barangay);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    showProgressBar(false);
                                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "Firestore error: ", e);
                                });

                    } else {
                        showProgressBar(false);
                        Toast.makeText(getContext(), "User data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    showProgressBar(false);
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "User doc error: ", e);
                });
    }


    private void navigateToFarmersListActivity(String userRole, String userBarangayId) {
        Intent intent = new Intent(getContext(), FarmersListActivity.class);
        // Pass user role and barangayId to the FarmersListActivity
        intent.putExtra("Role", userRole);
        intent.putExtra("Barangays", userBarangayId);
        startActivity(intent);
    }

    private void navigateToAddFarmer() {
        Intent intent = new Intent(requireActivity(), AddFarmerAcitivity.class);
        startActivity(intent);
    }

    private void showProgressBar(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        buttonSearch.setEnabled(!show);
        buttonViewAllFarmers.setEnabled(!show);
        buttonAddFarmer.setEnabled(!show);
    }
}