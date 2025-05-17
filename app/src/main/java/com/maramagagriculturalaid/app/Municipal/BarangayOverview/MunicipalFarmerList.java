package com.maramagagriculturalaid.app.Municipal.BarangayOverview;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.maramagagriculturalaid.app.FarmersData.Farmer;
import com.maramagagriculturalaid.app.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MunicipalFarmerList extends AppCompatActivity {

    private static final String TAG = "MunicipalFarmerList";

    // UI Components
    private RecyclerView rvFarmersList;
    private FarmersListAdapter adapter;
    private ImageButton btnBack;
    private TextView tvBarangayTitle;
    private TextView tvFarmersCount;
    private EditText etSearch;
    private Button btnSearch;
    private Button btnSortById;
    private Button btnSortByName;
    private ProgressBar progressBar;
    private FrameLayout loadingView;
    private LinearLayout emptyStateView;
    private TextView retryButton;
    private FrameLayout farmersListContainer;

    // Data
    private List<Farmer> farmersList;
    private List<Farmer> originalFarmersList;
    private List<Farmer> filteredFarmersList;
    private String barangayName;
    private boolean isDataLoaded = false;
    private boolean isLoadingData = false;
    private String currentSearchQuery = "";

    // Firebase
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_municipal_farmer_list);

        // Get barangay name from intent
        barangayName = getIntent().getStringExtra("barangayName");
        if (barangayName == null || barangayName.isEmpty()) {
            barangayName = "Unknown Barangay";
            Log.w(TAG, "No barangay name provided in intent");
            Toast.makeText(this, "Error: No barangay specified", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Log.d(TAG, "Loading farmers for barangay: " + barangayName);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize views and setup
        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        setupSearchFunctionality();

        // Load farmers data
        loadFarmersFromFirestore();
    }

    private void initializeViews() {
        // Find all views
        rvFarmersList = findViewById(R.id.rvFarmersList);
        btnBack = findViewById(R.id.btnBack);
        tvBarangayTitle = findViewById(R.id.tvBarangayTitle);
        tvFarmersCount = findViewById(R.id.tvFarmersCount);
        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);
        btnSortById = findViewById(R.id.btnSortById);
        btnSortByName = findViewById(R.id.btnSortByName);
        progressBar = findViewById(R.id.progressBar);
        loadingView = findViewById(R.id.loadingView);
        emptyStateView = findViewById(R.id.emptyStateView);
        farmersListContainer = findViewById(R.id.farmersListContainer);

        // Set barangay title
        tvBarangayTitle.setText("Farmers in " + barangayName);

        // Initialize data lists
        farmersList = new ArrayList<>();
        originalFarmersList = new ArrayList<>();
        filteredFarmersList = new ArrayList<>();

        // Initial farmers count
        updateFarmersCount();
    }

    private void setupRecyclerView() {
        rvFarmersList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FarmersListAdapter(farmersList);

        // Set click listener for farmer items - Navigate to MunicipalFarmerDetails
        adapter.setOnFarmerClickListener(farmer -> {
            Log.d(TAG, "Clicked on farmer: " + farmer.getFullName());

            // Get the farmer's document ID for navigation
            String farmerId = farmer.getDocumentId();
            if (farmerId == null || farmerId.isEmpty()) {
                // Fallback to regular ID if document ID is not available
                farmerId = farmer.getId();
            }

            if (farmerId != null && !farmerId.isEmpty()) {
                // Navigate to MunicipalFarmerDetails activity
                Intent intent = new Intent(MunicipalFarmerList.this, MunicipalFarmersDetails.class);
                intent.putExtra("farmerId", farmerId);
                intent.putExtra("barangayName", barangayName);
                intent.putExtra("farmerName", farmer.getFullName()); // Optional: for title display

                Log.d(TAG, "Navigating to farmer details - ID: " + farmerId + ", Barangay: " + barangayName);
                startActivity(intent);
            } else {
                Log.w(TAG, "Cannot navigate: Farmer ID is null or empty");
                Toast.makeText(this, "Error: Farmer ID not available", Toast.LENGTH_SHORT).show();
            }
        });

        rvFarmersList.setAdapter(adapter);
    }

    private void setupClickListeners() {
        // Back button
        btnBack.setOnClickListener(v -> {
            Log.d(TAG, "Back button clicked");
            finish();
        });

        // Search button
        btnSearch.setOnClickListener(v -> {
            String query = etSearch.getText().toString().trim();
            Log.d(TAG, "Search button clicked with query: " + query);
            performSearch(query);
        });

        // Sort by ID button - automatically sort when clicked
        btnSortById.setOnClickListener(v -> {
            Log.d(TAG, "Sort by ID button clicked");
            sortFarmersById();
            Toast.makeText(this, "Sorted by ID", Toast.LENGTH_SHORT).show();
        });

        // Sort by Name button - automatically sort when clicked
        btnSortByName.setOnClickListener(v -> {
            Log.d(TAG, "Sort by Name button clicked");
            sortFarmersByName();
            Toast.makeText(this, "Sorted by Name", Toast.LENGTH_SHORT).show();
        });

        // Retry button
        if (retryButton != null) {
            retryButton.setOnClickListener(v -> {
                Log.d(TAG, "Retry button clicked");
                refreshFarmers();
            });
        }
    }

    private void setupSearchFunctionality() {
        // Add text watcher for real-time search
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Do nothing
            }

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (!query.equals(currentSearchQuery)) {
                    currentSearchQuery = query;
                    // Perform search with a slight delay to avoid too many calls
                    etSearch.removeCallbacks(searchRunnable);
                    etSearch.postDelayed(searchRunnable, 300);
                }
            }
        });
    }

    private final Runnable searchRunnable = new Runnable() {
        @Override
        public void run() {
            performSearch(currentSearchQuery);
        }
    };

    private void performSearch(String query) {
        Log.d(TAG, "Performing search with query: " + query);

        farmersList.clear();

        if (query.isEmpty()) {
            // Show all farmers if search is empty
            farmersList.addAll(filteredFarmersList.isEmpty() ? originalFarmersList : filteredFarmersList);
        } else {
            // Filter farmers based on search query
            String lowerCaseQuery = query.toLowerCase();
            List<Farmer> sourceList = filteredFarmersList.isEmpty() ? originalFarmersList : filteredFarmersList;

            for (Farmer farmer : sourceList) {
                if (matchesSearchQuery(farmer, lowerCaseQuery)) {
                    farmersList.add(farmer);
                }
            }
        }

        // Update adapter and UI
        adapter.notifyDataSetChanged();
        updateFarmersCount();

        if (farmersList.isEmpty() && !query.isEmpty()) {
            showEmptyState();
        } else {
            showFarmersList();
        }
    }

    private boolean matchesSearchQuery(Farmer farmer, String query) {
        // Check farmer name
        if (farmer.getFullName() != null && farmer.getFullName().toLowerCase().contains(query)) {
            return true;
        }

        // Check farmer ID
        if (farmer.getId() != null && farmer.getId().toLowerCase().contains(query)) {
            return true;
        }

        // Check document ID as fallback
        if (farmer.getDocumentId() != null && farmer.getDocumentId().toLowerCase().contains(query)) {
            return true;
        }

        return false;
    }

    private void sortFarmersById() {
        if (farmersList.isEmpty()) {
            Log.d(TAG, "No farmers to sort by ID");
            return;
        }

        Log.d(TAG, "Sorting farmers by ID");

        Collections.sort(farmersList, (f1, f2) -> {
            String id1 = f1.getId() != null ? f1.getId() : "";
            String id2 = f2.getId() != null ? f2.getId() : "";
            return id1.compareTo(id2);
        });

        // Update adapter
        adapter.notifyDataSetChanged();
        Log.d(TAG, "Farmers sorted by ID successfully");
    }

    private void sortFarmersByName() {
        if (farmersList.isEmpty()) {
            Log.d(TAG, "No farmers to sort by Name");
            return;
        }

        Log.d(TAG, "Sorting farmers by Name");

        Collections.sort(farmersList, (f1, f2) -> {
            String name1 = f1.getFullName() != null ? f1.getFullName() : "";
            String name2 = f2.getFullName() != null ? f2.getFullName() : "";
            return name1.compareToIgnoreCase(name2);
        });

        // Update adapter
        adapter.notifyDataSetChanged();
        Log.d(TAG, "Farmers sorted by Name successfully");
    }

    private void loadFarmersFromFirestore() {
        Log.d(TAG, "Starting to load farmers from Firestore for barangay: " + barangayName);

        // Prevent duplicate loading
        if (isDataLoaded || isLoadingData) {
            Log.d(TAG, "Data already loaded or currently loading, skipping...");
            return;
        }

        // Set loading flag
        isLoadingData = true;

        // Show loading state
        showLoadingState();

        // Clear existing data and use Set to prevent duplicates
        farmersList.clear();
        originalFarmersList.clear();
        filteredFarmersList.clear();
        adapter.notifyDataSetChanged();
        Set<String> processedFarmerIds = new HashSet<>();

        // Query the specific path: Barangays/{barangayName}/Farmers
        db.collection("Barangays")
                .document(barangayName)
                .collection("Farmers")
                .get()
                .addOnCompleteListener(task -> {
                    isLoadingData = false;

                    if (task.isSuccessful()) {
                        int totalFarmers = task.getResult().size();
                        Log.d(TAG, "Found " + totalFarmers + " farmers in " + barangayName);

                        if (totalFarmers == 0) {
                            isDataLoaded = true;
                            showEmptyState();
                            updateFarmersCount();
                            return;
                        }

                        List<Farmer> tempFarmersList = new ArrayList<>();

                        // Process each farmer document
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                String documentId = document.getId();

                                // Check for duplicates using document ID
                                if (processedFarmerIds.contains(documentId)) {
                                    Log.w(TAG, "Duplicate farmer found: " + documentId + ", skipping...");
                                    continue;
                                }

                                Farmer farmer = createFarmerFromDocument(document);
                                if (farmer != null) {
                                    tempFarmersList.add(farmer);
                                    processedFarmerIds.add(documentId);
                                    Log.d(TAG, "Added farmer: " + farmer.getFullName() + " (ID: " + documentId + ")");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing farmer document: " + document.getId(), e);
                            }
                        }

                        // Update lists and UI
                        originalFarmersList.addAll(tempFarmersList);
                        farmersList.addAll(tempFarmersList);

                        if (farmersList.isEmpty()) {
                            showEmptyState();
                        } else {
                            // Default sort by name when data loads
                            sortFarmersByName();
                            showFarmersList();
                            updateFarmersCount();

                            Log.d(TAG, "Successfully loaded " + farmersList.size() + " unique farmers");
                            Toast.makeText(this, "Loaded " + farmersList.size() + " farmers", Toast.LENGTH_SHORT).show();
                        }

                        isDataLoaded = true;

                    } else {
                        Log.e(TAG, "Error getting farmers: ", task.getException());
                        showEmptyState();
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Unknown error";
                        Toast.makeText(this, "Error loading farmers: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    isLoadingData = false;
                    Log.e(TAG, "Failed to load farmers from " + barangayName, e);
                    showEmptyState();
                    Toast.makeText(this, "Failed to load farmers: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private Farmer createFarmerFromDocument(QueryDocumentSnapshot document) {
        try {
            Farmer farmer = new Farmer();

            // DEBUG: Log all available fields in the document
            Log.d(TAG, "=== FARMER DOCUMENT DEBUG ===");
            Log.d(TAG, "Document ID: " + document.getId());
            for (String key : document.getData().keySet()) {
                Object value = document.get(key);
                Log.d(TAG, "Field '" + key + "': " + (value != null ? value.toString() : "null"));
            }
            Log.d(TAG, "=== END DEBUG ===");

            // Set document ID (IMPORTANT: This is used for navigation)
            farmer.setDocumentId(document.getId());

            // Try to get ID from various possible field names
            String farmerId = getStringFromDocument(document, "id");
            if (farmerId == null || farmerId.isEmpty()) {
                farmerId = getStringFromDocument(document, "farmerId");
            }
            if (farmerId == null || farmerId.isEmpty()) {
                farmerId = getStringFromDocument(document, "farmer_id");
            }
            if (farmerId == null || farmerId.isEmpty()) {
                // Use document ID as the farmer ID if no separate ID field exists
                farmerId = document.getId();
            }
            farmer.setId(farmerId);

            // Basic information
            farmer.setFirstName(getStringFromDocument(document, "firstName"));
            farmer.setLastName(getStringFromDocument(document, "lastName"));
            farmer.setMiddleInitial(getStringFromDocument(document, "middleInitial"));
            farmer.setPhoneNumber(getStringFromDocument(document, "phoneNumber"));
            farmer.setAddress(getStringFromDocument(document, "address"));

            // DEBUG: Log what we're setting for ID and name
            Log.d(TAG, "Setting farmer ID: " + farmerId);
            Log.d(TAG, "Setting farmer name: " + farmer.getFullName());

            // Handle birthday (could be Timestamp or Date)
            Object birthdayObj = document.get("birthday");
            if (birthdayObj instanceof Timestamp) {
                farmer.setBirthday(((Timestamp) birthdayObj).toDate());
            } else if (birthdayObj instanceof Date) {
                farmer.setBirthday((Date) birthdayObj);
            }

            // Farm information
            farmer.setFarmType(getStringFromDocument(document, "farmType"));
            farmer.setLocation(getStringFromDocument(document, "location"));
            farmer.setBarangay(barangayName);
            farmer.setBarangayId(getStringFromDocument(document, "barangayId"));
            farmer.setExactLocation(getStringFromDocument(document, "exactLocation"));
            farmer.setCropsGrown(getStringFromDocument(document, "cropsGrown"));
            farmer.setLivestock(getStringFromDocument(document, "livestock"));

            // Handle lot size
            Object lotSizeObj = document.get("lotSize");
            if (lotSizeObj instanceof Number) {
                farmer.setLotSize(((Number) lotSizeObj).doubleValue());
            }

            // Handle livestock count
            Object livestockCountObj = document.get("livestockCount");
            if (livestockCountObj instanceof Number) {
                farmer.setLivestockCount(((Number) livestockCountObj).intValue());
            }

            // Date added
            farmer.setDateAdded(getStringFromDocument(document, "dateAdded"));

            // Final debug log
            Log.d(TAG, "Created farmer - ID: '" + farmer.getId() + "', Name: '" + farmer.getFullName() + "', DocID: '" + farmer.getDocumentId() + "'");

            return farmer;

        } catch (Exception e) {
            Log.e(TAG, "Error creating farmer from document: " + document.getId(), e);
            return null;
        }
    }

    private String getStringFromDocument(QueryDocumentSnapshot document, String field) {
        Object value = document.get(field);
        return value != null ? value.toString() : null;
    }

    private void updateFarmersCount() {
        if (tvFarmersCount != null) {
            int count = farmersList.size();
            String countText = count + " Farmer" + (count != 1 ? "s" : "");
            tvFarmersCount.setText(countText);
        }
    }

    private void showLoadingState() {
        Log.d(TAG, "Showing loading state");
        if (loadingView != null) loadingView.setVisibility(View.VISIBLE);
        if (rvFarmersList != null) rvFarmersList.setVisibility(View.GONE);
        if (emptyStateView != null) emptyStateView.setVisibility(View.GONE);
        if (btnSortById != null) btnSortById.setEnabled(false);
        if (btnSortByName != null) btnSortByName.setEnabled(false);
        if (btnSearch != null) btnSearch.setEnabled(false);
        if (etSearch != null) etSearch.setEnabled(false);
    }

    private void showFarmersList() {
        Log.d(TAG, "Showing farmers list");
        if (loadingView != null) loadingView.setVisibility(View.GONE);
        if (rvFarmersList != null) rvFarmersList.setVisibility(View.VISIBLE);
        if (emptyStateView != null) emptyStateView.setVisibility(View.GONE);
        if (btnSortById != null) btnSortById.setEnabled(true);
        if (btnSortByName != null) btnSortByName.setEnabled(true);
        if (btnSearch != null) btnSearch.setEnabled(true);
        if (etSearch != null) etSearch.setEnabled(true);
    }

    private void showEmptyState() {
        Log.d(TAG, "Showing empty state");
        if (loadingView != null) loadingView.setVisibility(View.GONE);
        if (rvFarmersList != null) rvFarmersList.setVisibility(View.GONE);
        if (emptyStateView != null) emptyStateView.setVisibility(View.VISIBLE);
        if (btnSortById != null) btnSortById.setEnabled(false);
        if (btnSortByName != null) btnSortByName.setEnabled(false);
        if (btnSearch != null) btnSearch.setEnabled(true);
        if (etSearch != null) etSearch.setEnabled(true);
        updateFarmersCount();
    }

    // Method to refresh data manually
    public void refreshFarmers() {
        Log.d(TAG, "Manually refreshing farmers data");
        isDataLoaded = false;
        isLoadingData = false;
        etSearch.setText(""); // Clear search
        currentSearchQuery = "";
        loadFarmersFromFirestore();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Activity destroyed");

        // Remove any pending search callbacks
        if (etSearch != null) {
            etSearch.removeCallbacks(searchRunnable);
        }
    }
}
