package com.maramagagriculturalaid.app.FarmersData;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.maramagagriculturalaid.app.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FarmersListActivity extends AppCompatActivity {

    private static final String TAG = "FarmersListActivity";

    // UI Components - Updated for new layout
    private ImageButton btnBack;
    private TextView tvFarmersTitle;
    private TextView tvFarmersCount;
    private EditText etSearch;
    private AppCompatButton btnSearch;
    private AppCompatButton btnSortById;
    private AppCompatButton btnSortByName;
    private AppCompatButton btnFilter;
    private RecyclerView rvFarmersList;
    private FrameLayout loadingView;
    private LinearLayout emptyStateView;
    private AppCompatButton btnAddFarmerEmpty;
    private FloatingActionButton fabAddFarmer;

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
        // Header components
        btnBack = findViewById(R.id.btnBack);
        tvFarmersTitle = findViewById(R.id.tvFarmersTitle);
        tvFarmersCount = findViewById(R.id.tvFarmersCount);

        // Search components
        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);

        // Sort and filter buttons
        btnSortById = findViewById(R.id.btnSortById);
        btnSortByName = findViewById(R.id.btnSortByName);

        // List components
        rvFarmersList = findViewById(R.id.rvFarmersList);
        loadingView = findViewById(R.id.loadingView);
        emptyStateView = findViewById(R.id.emptyStateView);
        btnAddFarmerEmpty = findViewById(R.id.btnAddFarmerEmpty);
        fabAddFarmer = findViewById(R.id.fabAddFarmer);

        // Set initial button states
        updateSortButtonStates();
    }

    private void setupRecyclerView() {
        farmersList = new ArrayList<>();
        originalFarmersList = new ArrayList<>();

        // Create the adapter with click listener to navigate to details
        farmerAdapter = new FarmersAdapter(farmersList, farmer -> {
            // Navigate to farmer details
            Intent intent = new Intent(FarmersListActivity.this, FarmersDetailsActivity.class);
            intent.putExtra("documentId", farmer.getDocumentId());
            intent.putExtra("barangayId", farmer.getBarangayId());
            intent.putExtra("farmerId", farmer.getId());
            startActivity(intent);

            Log.d(TAG, "Navigating to details for farmer: " + farmer.getFullName() +
                    " (ID: " + farmer.getId() + ", Doc ID: " + farmer.getDocumentId() + ")");
        });

        rvFarmersList.setLayoutManager(new LinearLayoutManager(this));
        rvFarmersList.setAdapter(farmerAdapter);
    }

    private void setupListeners() {
        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Search functionality
        btnSearch.setOnClickListener(v -> performSearch());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Real-time search as user types
                filterFarmersList(s.toString());
            }
        });

        // Sort buttons
        btnSortById.setOnClickListener(v -> {
            currentSortOption = SortOption.ID;
            updateSortButtonStates();
            sortFarmersList();
        });

        btnSortByName.setOnClickListener(v -> {
            currentSortOption = SortOption.NAME;
            updateSortButtonStates();
            sortFarmersList();
        });

        // Add farmer buttons
        fabAddFarmer.setOnClickListener(v -> navigateToAddFarmer());
        btnAddFarmerEmpty.setOnClickListener(v -> navigateToAddFarmer());
    }

    private void navigateToAddFarmer() {
        Intent intent = new Intent(this, AddFarmerAcitivity.class);
        startActivity(intent);
    }

    private void updateSortButtonStates() {
        // Reset all button backgrounds to default
        btnSortById.setBackgroundResource(R.drawable.buttons);
        btnSortByName.setBackgroundResource(R.drawable.buttons);

        // Highlight the active sort button
        switch (currentSortOption) {
            case ID:
                btnSortById.setBackgroundResource(R.drawable.buttons); // You might want a different drawable for selected state
                break;
            case NAME:
                btnSortByName.setBackgroundResource(R.drawable.buttons); // You might want a different drawable for selected state
                break;
        }
    }

    private void performSearch() {
        String query = etSearch.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, "Please enter a search term", Toast.LENGTH_SHORT).show();
            return;
        }
        filterFarmersList(query);
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
                                tvFarmersTitle.setText("All Farmers");
                                loadAllFarmers();
                            } else if ("Barangay".equals(role)) {
                                userBarangayId = document.getString("Barangay");
                                if (userBarangayId != null && !userBarangayId.isEmpty()) {
                                    tvFarmersTitle.setText("Farmers in " + userBarangayId);
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
                            String barangayName = barangayDoc.getString("name");
                            if (barangayName == null) {
                                barangayName = barangayId; // Fallback to ID if name is null
                            }

                            String finalBarangayName = barangayName;
                            db.collection("Barangays").document(barangayId)
                                    .collection("Farmers").get()
                                    .addOnCompleteListener(farmersTask -> {
                                        completedBarangays[0]++;

                                        if (farmersTask.isSuccessful()) {
                                            for (QueryDocumentSnapshot farmerDoc : farmersTask.getResult()) {
                                                Farmer farmer = createFarmerFromDocument(farmerDoc, barangayId, finalBarangayName);
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
                        String barangayName = barangayTask.getResult().getString("name");
                        if (barangayName == null) {
                            barangayName = barangayId; // Fallback to ID if name is null
                        }

                        final String finalBarangayName = barangayName;

                        db.collection("Barangays").document(barangayId)
                                .collection("Farmers").get()
                                .addOnCompleteListener(task -> {
                                    showLoading(false);

                                    if (task.isSuccessful()) {
                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                            Farmer farmer = createFarmerFromDocument(document, barangayId, finalBarangayName);
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

    private Farmer createFarmerFromDocument(QueryDocumentSnapshot document, String barangayId, String barangayName) {
        Farmer farmer = new Farmer();
        farmer.setDocumentId(document.getId());
        farmer.setId(document.getString("farmerId"));
        farmer.setFirstName(document.getString("firstName"));
        farmer.setLastName(document.getString("lastName"));
        farmer.setMiddleInitial(document.getString("middleInitial"));
        farmer.setPhoneNumber(document.getString("phoneNumber"));

        // Handle date values safely
        if (document.contains("birthday")) {
            Object birthdayObj = document.get("birthday");
            if (birthdayObj instanceof com.google.firebase.Timestamp) {
                farmer.setBirthday(((com.google.firebase.Timestamp) birthdayObj).toDate());
            } else if (birthdayObj instanceof Date) {
                farmer.setBirthday((Date) birthdayObj);
            }
        }

        farmer.setAddress(document.getString("address"));
        farmer.setFarmType(document.getString("farmType"));
        farmer.setLocation(document.getString("location"));
        farmer.setExactLocation(document.getString("exactLocation"));
        farmer.setCropsGrown(document.getString("cropsGrown"));

        // Handle numeric values safely
        if (document.contains("lotSize")) {
            Object lotSizeObj = document.get("lotSize");
            if (lotSizeObj instanceof Double) {
                farmer.setLotSize((Double) lotSizeObj);
            } else if (lotSizeObj instanceof Long) {
                farmer.setLotSize(((Long) lotSizeObj).doubleValue());
            }
        }

        farmer.setLivestock(document.getString("livestock"));

        // Handle integer values safely
        if (document.contains("livestockCount")) {
            Object countObj = document.get("livestockCount");
            if (countObj instanceof Long) {
                farmer.setLivestockCount(((Long) countObj).intValue());
            } else if (countObj instanceof Integer) {
                farmer.setLivestockCount((Integer) countObj);
            }
        }

        farmer.setDateAdded(document.getString("dateAdded"));
        farmer.setBarangayId(barangayId);
        farmer.setBarangay(barangayName);

        return farmer;
    }

    private void filterFarmersList(String query) {
        if (query.isEmpty()) {
            farmersList.clear();
            farmersList.addAll(originalFarmersList);
            farmerAdapter.notifyDataSetChanged();
        } else {
            List<Farmer> filteredList = new ArrayList<>();
            String lowerQuery = query.toLowerCase();

            for (Farmer farmer : originalFarmersList) {
                String farmerId = farmer.getId() != null ? farmer.getId().toLowerCase() : "";
                String fullName = farmer.getFullName() != null ? farmer.getFullName().toLowerCase() : "";
                String barangay = farmer.getBarangay() != null ? farmer.getBarangay().toLowerCase() : "";
                String phoneNumber = farmer.getPhoneNumber() != null ? farmer.getPhoneNumber().toLowerCase() : "";

                if (farmerId.contains(lowerQuery) ||
                        fullName.contains(lowerQuery) ||
                        barangay.contains(lowerQuery) ||
                        phoneNumber.contains(lowerQuery)) {
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
                    String date1 = f1.getDateAdded() != null ? f1.getDateAdded() : "";
                    String date2 = f2.getDateAdded() != null ? f2.getDateAdded() : "";
                    return date2.compareTo(date1); // Newest first
                });
                break;
        }
        farmerAdapter.notifyDataSetChanged();
    }

    private void updateFarmersCount() {
        int count = farmerAdapter.getItemCount();
        tvFarmersCount.setText(count + " Farmers");
    }

    private void checkEmptyState() {
        if (farmerAdapter.getItemCount() == 0) {
            rvFarmersList.setVisibility(View.GONE);
            emptyStateView.setVisibility(View.VISIBLE);
        } else {
            rvFarmersList.setVisibility(View.VISIBLE);
            emptyStateView.setVisibility(View.GONE);
        }
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            loadingView.setVisibility(View.VISIBLE);
            rvFarmersList.setVisibility(View.GONE);
            emptyStateView.setVisibility(View.GONE);
        } else {
            loadingView.setVisibility(View.GONE);
            // RecyclerView and empty state visibility will be handled by checkEmptyState()
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this activity
        if (isMunicipalUser) {
            loadAllFarmers();
        } else if (userBarangayId != null) {
            loadBarangayFarmers(userBarangayId);
        }
    }
}
