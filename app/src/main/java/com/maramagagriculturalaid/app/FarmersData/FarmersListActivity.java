package com.maramagagriculturalaid.app.FarmersData;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.maramagagriculturalaid.app.R;


import java.util.ArrayList;
import java.util.List;

public class FarmersListActivity extends AppCompatActivity {

    private static final String TAG = "FarmersListActivity";

    // UI Components
    private ImageButton buttonBack;
    private TextView titleTextView;
    private TextView farmersCountText;
    private TextView textViewSortBy;
    private ImageButton buttonSort;
    private AppCompatEditText editTextSearch;
    private RecyclerView recyclerViewFarmers;
    private TextView emptyStateText;
    private View progressBar;

    // Data
    private List<Farmer> farmersList;
    private List<Farmer> originalFarmersList;
    private FarmersAdapter farmerAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // User role and barangay
    private boolean isMunicipalUser = false;
    private String userBarangayId = null;

    // Sort options
    private enum SortOption {
        ID, NAME, DATE_ADDED
    }
    private SortOption currentSortOption = SortOption.ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farmers_list);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI components
        initializeViews();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup listeners
        setupListeners();

        // Check user role and then load data
        checkUserRoleAndLoadData();
    }

    private void initializeViews() {
        buttonBack = findViewById(R.id.buttonBack);
        titleTextView = findViewById(R.id.titleTextView);
        farmersCountText = findViewById(R.id.farmersCountText);
        textViewSortBy = findViewById(R.id.textViewSortBy);
        buttonSort = findViewById(R.id.buttonSort);
        editTextSearch = findViewById(R.id.editTextSearch);
        recyclerViewFarmers = findViewById(R.id.recyclerViewFarmers);
        emptyStateText = findViewById(R.id.emptyStateText);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        farmersList = new ArrayList<>();
        originalFarmersList = new ArrayList<>();
        farmerAdapter = new FarmersAdapter(farmersList, farmer -> {
            Intent intent = new Intent(FarmersListActivity.this, FarmersDetailsActivity.class);
            intent.putExtra("documentId", farmer.getDocumentId());
            intent.putExtra("barangayId", farmer.getBarangayId());
            startActivity(intent);
        });
        recyclerViewFarmers.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewFarmers.setAdapter(farmerAdapter);
    }

    private void setupListeners() {
        buttonBack.setOnClickListener(v -> finish());

        View.OnClickListener sortClickListener = v -> showSortOptions();
        buttonSort.setOnClickListener(sortClickListener);
        textViewSortBy.setOnClickListener(sortClickListener);

        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterFarmersList(s.toString());
            }
        });
    }

    private void checkUserRoleAndLoadData() {
        showLoading(true);
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            showLoading(false);
            Toast.makeText(this, "You must be logged in to view farmers", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = currentUser.getUid();
        db.collection("Users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String role = document.getString("Role");
                            if ("Municipal".equals(role)) {
                                isMunicipalUser = true;
                                loadAllFarmers();
                            } else if ("Barangay".equals(role)) {
                                userBarangayId = document.getString("Barangay");
                                if (userBarangayId != null && !userBarangayId.isEmpty()) {
                                    loadBarangayFarmers(userBarangayId);
                                } else {
                                    showLoading(false);
                                    Toast.makeText(this, "No barangay assigned to this user", Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            } else {
                                showLoading(false);
                                Toast.makeText(this, "Insufficient permissions to view farmers", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        } else {
                            showLoading(false);
                            Toast.makeText(this, "User document not found", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        showLoading(false);
                        Log.e(TAG, "Error getting user document: ", task.getException());
                        Toast.makeText(this, "Failed to verify user role", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void loadAllFarmers() {
        showLoading(true);
        farmersList.clear();
        originalFarmersList.clear();

        db.collection("Barangays").get()
                .addOnCompleteListener(barangaysTask -> {
                    if (barangaysTask.isSuccessful()) {
                        int totalBarangays = barangaysTask.getResult().size();
                        final int[] completedBarangays = {0};

                        if (totalBarangays == 0) {
                            showLoading(false);
                            updateFarmersCount();
                            checkEmptyState();
                            return;
                        }

                        for (QueryDocumentSnapshot barangayDoc : barangaysTask.getResult()) {
                            String barangayId = barangayDoc.getId();
                            String barangayName = barangayDoc.getString("Name");

                            db.collection("Barangays").document(barangayId)
                                    .collection("Farmers").get()
                                    .addOnCompleteListener(farmersTask -> {
                                        completedBarangays[0]++;

                                        if (farmersTask.isSuccessful()) {
                                            for (QueryDocumentSnapshot farmerDoc : farmersTask.getResult()) {
                                                Farmer farmer = farmerDoc.toObject(Farmer.class);
                                                farmer.setDocumentId(farmerDoc.getId());
                                                farmer.setBarangayId(barangayId);
                                                farmer.setBarangay(barangayName);
                                                farmersList.add(farmer);
                                                originalFarmersList.add(farmer);
                                            }
                                        } else {
                                            Log.e(TAG, "Error getting farmers for barangay " + barangayId, farmersTask.getException());
                                        }

                                        if (completedBarangays[0] >= totalBarangays) {
                                            showLoading(false);
                                            updateFarmersCount();
                                            sortFarmersList();
                                            checkEmptyState();
                                        }
                                    });
                        }
                    } else {
                        showLoading(false);
                        Log.e(TAG, "Error getting barangays: ", barangaysTask.getException());
                        Toast.makeText(this, "Failed to load barangays", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadBarangayFarmers(String barangayId) {
        showLoading(true);
        farmersList.clear();
        originalFarmersList.clear();

        db.collection("Barangays").document(barangayId).get()
                .addOnCompleteListener(barangayTask -> {
                    if (barangayTask.isSuccessful() && barangayTask.getResult() != null) {
                        String barangayName = barangayTask.getResult().getString("Name");

                        db.collection("Barangays").document(barangayId)
                                .collection("Farmers").get()
                                .addOnCompleteListener(task -> {
                                    showLoading(false);

                                    if (task.isSuccessful()) {
                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                            Farmer farmer = document.toObject(Farmer.class);
                                            farmer.setDocumentId(document.getId());
                                            farmer.setBarangayId(barangayId);
                                            farmer.setBarangay(barangayName);
                                            farmersList.add(farmer);
                                            originalFarmersList.add(farmer);
                                        }

                                        updateFarmersCount();
                                        sortFarmersList();
                                        checkEmptyState();
                                    } else {
                                        Log.e(TAG, "Error getting farmers: ", task.getException());
                                        Toast.makeText(this, "Failed to load farmers data", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        showLoading(false);
                        Log.e(TAG, "Error getting barangay: ", barangayTask.getException());
                        Toast.makeText(this, "Failed to load barangay data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showSortOptions() {
        PopupMenu popupMenu = new PopupMenu(this, buttonSort);
        popupMenu.getMenu().add("ID");
        popupMenu.getMenu().add("Name");
        popupMenu.getMenu().add("Date Added");

        popupMenu.setOnMenuItemClickListener(item -> {
            String sortBy = item.getTitle().toString();
            textViewSortBy.setText(sortBy);

            switch (sortBy) {
                case "ID":
                    currentSortOption = SortOption.ID;
                    break;
                case "Name":
                    currentSortOption = SortOption.NAME;
                    break;
                case "Date Added":
                    currentSortOption = SortOption.DATE_ADDED;
                    break;
            }

            sortFarmersList();
            return true;
        });

        popupMenu.show();
    }

    private void filterFarmersList(String query) {
        if (query.isEmpty()) {
            farmersList.clear();
            farmersList.addAll(originalFarmersList);
            farmerAdapter.notifyDataSetChanged();
        } else {
            List<Farmer> filteredList = new ArrayList<>();
            for (Farmer farmer : originalFarmersList) {
                String farmerId = farmer.getId() != null ? farmer.getId().toLowerCase() : "";
                String fullName = farmer.getFullName() != null ? farmer.getFullName().toLowerCase() : "";

                if (farmerId.contains(query.toLowerCase()) || fullName.contains(query.toLowerCase())) {
                    filteredList.add(farmer);
                }
            }
            farmersList.clear();
            farmersList.addAll(filteredList);
            farmerAdapter.notifyDataSetChanged();
        }
        updateFarmersCount();
        checkEmptyState();
    }

    private void sortFarmersList() {
        switch (currentSortOption) {
            case ID:
                farmersList.sort((f1, f2) -> {
                    String id1 = f1.getId() != null ? f1.getId() : "";
                    String id2 = f2.getId() != null ? f2.getId() : "";
                    return id1.compareToIgnoreCase(id2);
                });
                break;
            case NAME:
                farmersList.sort((f1, f2) -> {
                    String name1 = f1.getFullName() != null ? f1.getFullName() : "";
                    String name2 = f2.getFullName() != null ? f2.getFullName() : "";
                    return name1.compareToIgnoreCase(name2);
                });
                break;
            case DATE_ADDED:
                farmersList.sort((f1, f2) -> {
                    // If you have a Date field instead of String, adjust this comparison
                    String date1 = f1.getBirthday() != null ? f1.getBirthday().toString() : "";
                    String date2 = f2.getBirthday() != null ? f2.getBirthday().toString() : "";
                    return date2.compareTo(date1); // Newest first
                });
                break;
        }
        farmerAdapter.notifyDataSetChanged();
    }

    private void updateFarmersCount() {
        int count = farmerAdapter.getItemCount();
        farmersCountText.setText(String.format("Total Farmers: %d", count));
    }

    private void checkEmptyState() {
        if (farmerAdapter.getItemCount() == 0) {
            recyclerViewFarmers.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.VISIBLE);
        } else {
            recyclerViewFarmers.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(View.GONE);
        }
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        recyclerViewFarmers.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        emptyStateText.setVisibility(isLoading ? View.GONE : emptyStateText.getVisibility());
    }
}