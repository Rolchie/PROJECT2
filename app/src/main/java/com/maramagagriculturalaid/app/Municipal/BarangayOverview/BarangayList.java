package com.maramagagriculturalaid.app.Municipal.BarangayOverview;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.maramagagriculturalaid.app.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BarangayList extends AppCompatActivity {

    private static final String TAG = "BarangayList";
    private RecyclerView rvBarangayList;
    private BarangayListAdapter adapter;
    private List<Barangay> barangayList;
    private List<Barangay> originalBarangayList;
    private EditText etSearch;
    private Button btnSearch;
    private ImageButton btnBack;
    private ImageButton refreshButton;
    private ProgressBar progressBar;
    private TextView tvBarangayCount;
    private FrameLayout loadingView;
    private LinearLayout emptyStateView;
    private TextView retryButton;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_baranggay_list);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();

        // Set up RecyclerView
        setupRecyclerView();

        // Load data from Firestore
        loadBarangaysFromFirestore();

        // Set up click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        rvBarangayList = findViewById(R.id.rvBarangayList);
        etSearch = findViewById(R.id.et_search);
        btnSearch = findViewById(R.id.btn_search);
        btnBack = findViewById(R.id.btn_back);
        progressBar = findViewById(R.id.progressBar);
        tvBarangayCount = findViewById(R.id.tvBarangayCount);
        loadingView = findViewById(R.id.loadingView);
        emptyStateView = findViewById(R.id.emptyStateView);

        // Initialize data lists
        barangayList = new ArrayList<>();
        originalBarangayList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        rvBarangayList.setLayoutManager(new LinearLayoutManager(this));

        // Set up adapter with click listener
        adapter = new BarangayListAdapter(barangayList, new BarangayListAdapter.OnBarangayClickListener() {
            @Override
            public void onBarangayClick(int position) {
                Barangay selectedBarangay = barangayList.get(position);

                // Navigate to MunicipalFarmerList activity
                Intent intent = new Intent(BarangayList.this, MunicipalFarmerList.class);
                intent.putExtra("barangayName", selectedBarangay.getName());
                intent.putExtra("farmersCount", selectedBarangay.getFarmersCount());
                startActivity(intent);
            }
        });
        rvBarangayList.setAdapter(adapter);
    }

    private void setupClickListeners() {
        // Set up search button
        btnSearch.setOnClickListener(v -> {
            String query = etSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                searchBarangays(query);
            } else {
                // Reset to original list if search is empty
                barangayList.clear();
                barangayList.addAll(originalBarangayList);
                adapter.notifyDataSetChanged();
                updateBarangayCount();
                Toast.makeText(this, "Showing all barangays", Toast.LENGTH_SHORT).show();
            }
        });

        // Set up back button
        btnBack.setOnClickListener(v -> finish());

        // Optional: Search as user types
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            String query = etSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                searchBarangays(query);
            }
            return true;
        });
    }

    private void loadBarangaysFromFirestore() {
        Log.d(TAG, "Starting to load barangays from Firestore");

        // Show loading state
        showLoading(true);

        // Clear existing lists
        barangayList.clear();
        originalBarangayList.clear();

        // First, get all barangay documents
        db.collection("Barangays")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int totalBarangays = task.getResult().size();
                        Log.d(TAG, "Found " + totalBarangays + " barangay documents");

                        if (totalBarangays == 0) {
                            showLoading(false);
                            showEmptyState(true);
                            updateBarangayCount();
                            return;
                        }

                        // Use a map to store barangays and prevent duplicates
                        Map<String, Barangay> barangayMap = new HashMap<>();

                        // Initialize all barangays with 0 farmers first
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String barangayName = document.getId();
                            Log.d(TAG, "Found barangay: " + barangayName);
                            barangayMap.put(barangayName, new Barangay(barangayName, 0, "Municipality"));
                        }

                        Log.d(TAG, "Initialized " + barangayMap.size() + " barangays");

                        // Now count farmers for each barangay
                        countFarmersForAllBarangays(barangayMap);

                    } else {
                        Log.e(TAG, "Error getting barangay documents: ", task.getException());
                        showLoading(false);
                        showEmptyState(true);
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Unknown error";
                        Toast.makeText(this, "Error loading barangays: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load barangays", e);
                    showLoading(false);
                    showEmptyState(true);
                    Toast.makeText(this, "Failed to load barangays: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void countFarmersForAllBarangays(Map<String, Barangay> barangayMap) {
        Log.d(TAG, "Starting to count farmers for " + barangayMap.size() + " barangays");

        final int totalBarangays = barangayMap.size();
        final int[] processedCount = {0};

        // If no barangays, show empty result
        if (totalBarangays == 0) {
            showLoading(false);
            showEmptyState(true);
            return;
        }

        // Count farmers for each barangay
        for (String barangayName : barangayMap.keySet()) {
            Log.d(TAG, "Counting farmers for: " + barangayName);

            db.collection("Barangays")
                    .document(barangayName)
                    .collection("Farmers")
                    .get()
                    .addOnCompleteListener(farmerTask -> {
                        int farmersCount = 0;
                        if (farmerTask.isSuccessful()) {
                            farmersCount = farmerTask.getResult().size();
                            Log.d(TAG, "Barangay " + barangayName + " has " + farmersCount + " farmers");
                        } else {
                            Log.e(TAG, "Error counting farmers for " + barangayName, farmerTask.getException());
                        }

                        // Update the barangay with correct farmer count
                        synchronized (barangayMap) {
                            barangayMap.put(barangayName, new Barangay(barangayName, farmersCount, "Municipality"));
                        }

                        // Check if all barangays have been processed
                        synchronized (processedCount) {
                            processedCount[0]++;
                            Log.d(TAG, "Processed " + processedCount[0] + " of " + totalBarangays + " barangays");

                            if (processedCount[0] == totalBarangays) {
                                // All barangays processed, update UI
                                updateUIWithBarangays(barangayMap);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to count farmers for " + barangayName, e);

                        // Still count as processed
                        synchronized (processedCount) {
                            processedCount[0]++;
                            Log.d(TAG, "Processed " + processedCount[0] + " of " + totalBarangays + " barangays (with error)");

                            if (processedCount[0] == totalBarangays) {
                                updateUIWithBarangays(barangayMap);
                            }
                        }
                    });
        }
    }

    private void updateUIWithBarangays(Map<String, Barangay> barangayMap) {
        Log.d(TAG, "Updating UI with " + barangayMap.size() + " barangays");

        runOnUiThread(() -> {
            // Convert map to list and sort
            List<Barangay> finalList = new ArrayList<>(barangayMap.values());
            finalList.sort((b1, b2) -> b1.getName().compareToIgnoreCase(b2.getName()));

            // Update both lists
            originalBarangayList.clear();
            originalBarangayList.addAll(finalList);

            barangayList.clear();
            barangayList.addAll(finalList);

            // Notify adapter and update UI
            adapter.notifyDataSetChanged();
            updateBarangayCount();
            showLoading(false);
            showEmptyState(barangayList.isEmpty());

            Log.d(TAG, "Successfully loaded " + barangayList.size() + " barangays");

            // Debug: Print all loaded barangays
            for (Barangay barangay : barangayList) {
                Log.d(TAG, "Loaded: " + barangay.getName() + " (" + barangay.getFarmersCount() + " farmers)");
            }
        });
    }

    private void searchBarangays(String query) {
        if (query.isEmpty()) {
            barangayList.clear();
            barangayList.addAll(originalBarangayList);
        } else {
            List<Barangay> filteredList = new ArrayList<>();
            for (Barangay barangay : originalBarangayList) {
                if (barangay.getName().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(barangay);
                }
            }
            barangayList.clear();
            barangayList.addAll(filteredList);
        }
        adapter.notifyDataSetChanged();
        updateBarangayCount();
        showEmptyState(barangayList.isEmpty() && !originalBarangayList.isEmpty());

        Toast.makeText(this, "Found " + barangayList.size() + " barangays", Toast.LENGTH_SHORT).show();
    }

    private void updateBarangayCount() {
        if (tvBarangayCount != null) {
            int count = barangayList.size();
            String countText = count + (count == 1 ? " Barangay" : " Barangays");
            tvBarangayCount.setText(countText);
        }
    }

    private void showLoading(boolean show) {
        if (loadingView != null) {
            loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (btnSearch != null) {
            btnSearch.setEnabled(!show);
        }
        if (refreshButton != null) {
            refreshButton.setEnabled(!show);
        }
        if (rvBarangayList != null) {
            rvBarangayList.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void showEmptyState(boolean show) {
        if (emptyStateView != null) {
            emptyStateView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (rvBarangayList != null && !show) {
            rvBarangayList.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Only reload if the list is empty to avoid unnecessary network calls
        if (barangayList.isEmpty()) {
            Log.d(TAG, "Barangay list is empty, reloading...");
            loadBarangaysFromFirestore();
        }
    }
}