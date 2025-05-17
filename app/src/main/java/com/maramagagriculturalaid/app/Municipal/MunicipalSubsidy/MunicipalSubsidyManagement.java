package com.maramagagriculturalaid.app.Municipal.MunicipalSubsidy;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.maramagagriculturalaid.app.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MunicipalSubsidyManagement extends AppCompatActivity {

    private static final String TAG = "MunicipalSubsidyMgmt";

    // UI Components
    private RecyclerView recyclerSubsidyList;
    private LinearLayout emptyState;
    private ProgressBar progressBar;
    private CardView cardPending, cardApproved, cardRejected;
    private TextView tvPendingCount, tvApprovedCount, tvRejectedCount;
    private TextView tvEmptyMessage;
    private ImageButton btnBack;
    private TextView tvTitle;

    // Data
    private MunicipalSubsidyAdapter adapter;
    private List<MunicipalSubsidyItem> allSubsidyItems = new ArrayList<>();
    private List<MunicipalSubsidyItem> filteredSubsidyItems = new ArrayList<>();

    // Firestore
    private FirebaseFirestore db;

    // Filter state
    private String currentFilter = "ALL"; // ALL, Pending, Approved, Rejected

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subsidy_list);

        // Get filter from intent if available
        if (getIntent().hasExtra("filter")) {
            currentFilter = getIntent().getStringExtra("filter");
        }

        // Get default tab from intent if available
        if (getIntent().hasExtra("defaultTab")) {
            String defaultTab = getIntent().getStringExtra("defaultTab");
            if (defaultTab != null) {
                switch (defaultTab.toLowerCase()) {
                    case "pending":
                        currentFilter = "Pending";
                        break;
                    case "approved":
                        currentFilter = "Approved";
                        break;
                    case "rejected":
                        currentFilter = "Rejected";
                        break;
                    default:
                        currentFilter = "ALL";
                        break;
                }
            }
        }

        // Get status from intent if available (for backward compatibility)
        if (getIntent().hasExtra("status")) {
            String status = getIntent().getStringExtra("status");
            if (status != null) {
                currentFilter = status;
            }
        }

        Log.d(TAG, "Starting activity with filter: " + currentFilter);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        initViews();

        // Set up RecyclerView
        setupRecyclerView();

        // Set up click listeners
        setupClickListeners();

        // Load data from Firestore
        loadAllSubsidyApplications();
    }

    private void initViews() {
        // Find views by ID
        recyclerSubsidyList = findViewById(R.id.recycler_subsidy_list);
        emptyState = findViewById(R.id.empty_state);
        progressBar = findViewById(R.id.progress_bar);

        cardPending = findViewById(R.id.card_pending);
        cardApproved = findViewById(R.id.card_approved);
        cardRejected = findViewById(R.id.card_rejected);

        tvPendingCount = findViewById(R.id.tv_pending_count);
        tvApprovedCount = findViewById(R.id.tv_approved_count);
        tvRejectedCount = findViewById(R.id.tv_rejected_count);
        tvEmptyMessage = findViewById(R.id.tv_empty_message);

        btnBack = findViewById(R.id.btn_back);
        tvTitle = findViewById(R.id.tv_title);

        // Set title for municipal view
        if (tvTitle != null) {
            tvTitle.setText("Subsidy Management");
        }
    }

    private void setupRecyclerView() {
        // Initialize RecyclerView with a LinearLayoutManager
        recyclerSubsidyList.setLayoutManager(new LinearLayoutManager(this));

        // Initialize the adapter with context and filtered items
        adapter = new MunicipalSubsidyAdapter(this, filteredSubsidyItems);

        // Set municipal mode for the adapter
        adapter.setMunicipalMode(true);

        // Set the adapter to RecyclerView
        recyclerSubsidyList.setAdapter(adapter);

        // Add item decoration for spacing between items
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                recyclerSubsidyList.getContext(),
                DividerItemDecoration.VERTICAL
        );
        recyclerSubsidyList.addItemDecoration(dividerItemDecoration);
    }

    private void setupClickListeners() {
        // Back button click listener
        btnBack.setOnClickListener(v -> {
            Log.d(TAG, "Back button clicked");
            onBackPressed();
        });

        // Filter card click listeners
        cardPending.setOnClickListener(v -> {
            Log.d(TAG, "Pending filter clicked");
            filterSubsidyItems("Pending");
        });

        cardApproved.setOnClickListener(v -> {
            Log.d(TAG, "Approved filter clicked");
            filterSubsidyItems("Approved");
        });

        cardRejected.setOnClickListener(v -> {
            Log.d(TAG, "Rejected filter clicked");
            filterSubsidyItems("Rejected");
        });

        // Add click listener to show all applications when long clicking on the filter layout
        findViewById(R.id.status_filter_layout).setOnLongClickListener(v -> {
            Log.d(TAG, "Show all filter activated");
            filterSubsidyItems("ALL");
            return true;
        });
    }

    private void loadAllSubsidyApplications() {
        // Show loading state
        showLoading(true);

        // Clear existing data
        allSubsidyItems.clear();
        filteredSubsidyItems.clear();

        Log.d(TAG, "Loading subsidy applications from all barangays");

        // First, get all barangays
        db.collection("Barangays")
                .get()
                .addOnCompleteListener(barangayTask -> {
                    if (barangayTask.isSuccessful() && barangayTask.getResult() != null) {
                        List<QueryDocumentSnapshot> barangayDocs = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : barangayTask.getResult()) {
                            barangayDocs.add(doc);
                        }

                        if (barangayDocs.isEmpty()) {
                            showLoading(false);
                            showEmptyState("No barangays found");
                            return;
                        }

                        Log.d(TAG, "Found " + barangayDocs.size() + " barangays");

                        // Use AtomicInteger to track completed barangay queries
                        AtomicInteger completedQueries = new AtomicInteger(0);
                        int totalBarangays = barangayDocs.size();

                        // For each barangay, get its subsidy requests
                        for (QueryDocumentSnapshot barangayDoc : barangayDocs) {
                            String barangayName = barangayDoc.getId();
                            Log.d(TAG, "Loading subsidy requests for barangay: " + barangayName);

                            db.collection("Barangays")
                                    .document(barangayName)
                                    .collection("SubsidyRequests")
                                    .orderBy("timestamp", Query.Direction.DESCENDING)
                                    .get()
                                    .addOnCompleteListener(subsidyTask -> {
                                        if (subsidyTask.isSuccessful() && subsidyTask.getResult() != null) {
                                            Log.d(TAG, "Successfully retrieved " + subsidyTask.getResult().size() +
                                                    " subsidy requests from " + barangayName);

                                            for (DocumentSnapshot document : subsidyTask.getResult()) {
                                                try {
                                                    // Create MunicipalSubsidyItem from document
                                                    MunicipalSubsidyItem item = createSubsidyItemFromDocument(document, barangayName);

                                                    if (item != null) {
                                                        allSubsidyItems.add(item);
                                                        Log.d(TAG, "Added subsidy item from " + barangayName + ": " +
                                                                item.getFarmerName() + " with status: " + item.getStatus());
                                                    }
                                                } catch (Exception e) {
                                                    Log.e(TAG, "Error processing document " + document.getId() + " from " + barangayName, e);
                                                }
                                            }
                                        } else {
                                            Log.e(TAG, "Error getting subsidy requests from " + barangayName + ": ", subsidyTask.getException());
                                        }

                                        // Check if all barangays have been processed
                                        if (completedQueries.incrementAndGet() == totalBarangays) {
                                            // All barangays processed, update UI
                                            runOnUiThread(() -> {
                                                showLoading(false);
                                                Log.d(TAG, "Loaded total of " + allSubsidyItems.size() + " subsidy items from all barangays");
                                                updateStatusCounts();
                                                filterSubsidyItems(currentFilter);
                                            });
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to load subsidy applications from " + barangayName, e);

                                        // Still increment counter even on failure
                                        if (completedQueries.incrementAndGet() == totalBarangays) {
                                            runOnUiThread(() -> {
                                                showLoading(false);
                                                updateStatusCounts();
                                                filterSubsidyItems(currentFilter);
                                            });
                                        }
                                    });
                        }
                    } else {
                        showLoading(false);
                        Log.e(TAG, "Error getting barangays: ", barangayTask.getException());
                        String errorMessage = barangayTask.getException() != null ?
                                barangayTask.getException().getMessage() : "Unknown error occurred";

                        Toast.makeText(MunicipalSubsidyManagement.this,
                                "Error loading barangays: " + errorMessage,
                                Toast.LENGTH_LONG).show();

                        showEmptyState("Error loading data. Please try again.");
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Failed to load barangays", e);
                    Toast.makeText(this, "Failed to load barangays. Check your connection.",
                            Toast.LENGTH_LONG).show();
                    showEmptyState("Failed to load data. Please check your connection.");
                });
    }

    private MunicipalSubsidyItem createSubsidyItemFromDocument(DocumentSnapshot document, String barangayName) {
        try {
            MunicipalSubsidyItem item = new MunicipalSubsidyItem();

            // Set document ID
            item.setId(document.getId());

            // Set barangay
            item.setBarangay(barangayName);

            // Map document fields to MunicipalSubsidyItem
            item.setFarmerName(document.getString("farmerName"));
            item.setFarmerId(document.getString("farmerId"));
            item.setSubsidyType(document.getString("supportType")); // Map supportType to subsidyType
            item.setStatus(document.getString("status"));
            item.setTimestamp(document.get("timestamp"));

            // Handle date fields
            item.setDateApplied(document.getString("dateApplied"));
            item.setDateApproved(document.getString("dateApproved"));

            // If dateApplied is not available, try to format timestamp
            if (item.getDateApplied() == null && item.getTimestamp() != null) {
                try {
                    java.util.Date date;
                    if (item.getTimestamp() instanceof Long) {
                        date = new java.util.Date((Long) item.getTimestamp());
                    } else if (item.getTimestamp() instanceof com.google.firebase.Timestamp) {
                        date = ((com.google.firebase.Timestamp) item.getTimestamp()).toDate();
                    } else {
                        date = new java.util.Date();
                    }
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
                    item.setDateApplied(sdf.format(date));
                } catch (Exception e) {
                    Log.w(TAG, "Could not format timestamp for document " + document.getId(), e);
                }
            }

            // Set default status if null
            if (item.getStatus() == null) {
                item.setStatus("Pending");
            }

            return item;

        } catch (Exception e) {
            Log.e(TAG, "Error creating MunicipalSubsidyItem from document " + document.getId(), e);
            return null;
        }
    }

    private void filterSubsidyItems(String filter) {
        currentFilter = filter;
        filteredSubsidyItems.clear();

        Log.d(TAG, "Filtering subsidy items with filter: " + filter);

        // Apply filter
        if (filter.equals("ALL")) {
            filteredSubsidyItems.addAll(allSubsidyItems);
            Log.d(TAG, "Showing all " + filteredSubsidyItems.size() + " subsidy items");
        } else {
            for (MunicipalSubsidyItem item : allSubsidyItems) {
                if (item.getStatus() != null && item.getStatus().equalsIgnoreCase(filter)) {
                    filteredSubsidyItems.add(item);
                }
            }
            Log.d(TAG, "Found " + filteredSubsidyItems.size() + " " + filter + " subsidy items");
        }

        // Update adapter with filtered items
        if (adapter != null) {
            adapter.updateData(filteredSubsidyItems);
        }

        // Update UI
        updateFilterSelection();

        // Show empty state if needed
        if (filteredSubsidyItems.isEmpty()) {
            if (allSubsidyItems.isEmpty()) {
                showEmptyState("No subsidy applications found in any barangay");
            } else {
                showEmptyState("No " + filter.toLowerCase() + " applications found");
            }
        } else {
            emptyState.setVisibility(View.GONE);
        }
    }

    private void showEmptyState(String message) {
        emptyState.setVisibility(View.VISIBLE);
        recyclerSubsidyList.setVisibility(View.GONE);
        if (tvEmptyMessage != null) {
            tvEmptyMessage.setText(message);
        }
        Log.d(TAG, "Showing empty state: " + message);
    }

    private void updateFilterSelection() {
        // Reset all cards to default appearance
        resetCardElevations();

        // Highlight selected filter with elevation
        switch (currentFilter) {
            case "Pending":
                cardPending.setCardElevation(8);
                break;
            case "Approved":
                cardApproved.setCardElevation(8);
                break;
            case "Rejected":
                cardRejected.setCardElevation(8);
                break;
            case "ALL":
                // Keep all cards at default elevation for "ALL" filter
                break;
        }

        Log.d(TAG, "Updated filter selection to: " + currentFilter);
    }

    private void resetCardElevations() {
        cardPending.setCardElevation(0);
        cardApproved.setCardElevation(0);
        cardRejected.setCardElevation(0);
    }

    private void updateStatusCounts() {
        int pendingCount = 0;
        int approvedCount = 0;
        int rejectedCount = 0;

        for (MunicipalSubsidyItem item : allSubsidyItems) {
            if (item.getStatus() != null) {
                String status = item.getStatus().toLowerCase().trim();
                switch (status) {
                    case "pending":
                        pendingCount++;
                        break;
                    case "approved":
                        approvedCount++;
                        break;
                    case "rejected":
                        rejectedCount++;
                        break;
                    default:
                        Log.w(TAG, "Unknown status: " + item.getStatus());
                        break;
                }
            }
        }

        // Update UI on main thread
        int finalPendingCount = pendingCount;
        int finalApprovedCount = approvedCount;
        int finalRejectedCount = rejectedCount;
        runOnUiThread(() -> {
            tvPendingCount.setText(String.valueOf(finalPendingCount));
            tvApprovedCount.setText(String.valueOf(finalApprovedCount));
            tvRejectedCount.setText(String.valueOf(finalRejectedCount));
        });

        Log.d(TAG, "Updated status counts - Pending: " + pendingCount +
                ", Approved: " + approvedCount + ", Rejected: " + rejectedCount);
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            recyclerSubsidyList.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
            Log.d(TAG, "Showing loading state");
        } else {
            progressBar.setVisibility(View.GONE);
            recyclerSubsidyList.setVisibility(View.VISIBLE);
            Log.d(TAG, "Hiding loading state");
        }
    }

    public void refreshData() {
        Log.d(TAG, "Refreshing data");
        loadAllSubsidyApplications();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Activity resumed");
        // Optionally refresh data when returning to this activity
        // refreshData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Activity destroyed");
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "Back pressed");
        super.onBackPressed();
    }

    // Helper method to get current filter for external access
    public String getCurrentFilter() {
        return currentFilter;
    }

    // Method to handle filter changes from external sources
    public void setFilter(String filter) {
        if (filter != null && !filter.equals(currentFilter)) {
            filterSubsidyItems(filter);
        }
    }

    // Method to get total items count
    public int getTotalItemsCount() {
        return allSubsidyItems.size();
    }

    // Method to get items by status
    public List<MunicipalSubsidyItem> getItemsByStatus(String status) {
        List<MunicipalSubsidyItem> result = new ArrayList<>();
        for (MunicipalSubsidyItem item : allSubsidyItems) {
            if (item.getStatus() != null && item.getStatus().equalsIgnoreCase(status)) {
                result.add(item);
            }
        }
        return result;
    }
}