package com.maramagagriculturalaid.app.SubsidyManagement;

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
import com.maramagagriculturalaid.app.R;

import java.util.ArrayList;
import java.util.List;

public class SubsidyListActivity extends AppCompatActivity {

    private static final String TAG = "SubsidyListActivity";

    // UI Components
    private RecyclerView recyclerSubsidyList;
    private LinearLayout emptyState;
    private ProgressBar progressBar;
    private CardView cardPending, cardApproved, cardRejected;
    private TextView tvPendingCount, tvApprovedCount, tvRejectedCount;
    private TextView tvEmptyMessage;
    private ImageButton btnBack;

    // Data
    private SubsidyAdapter adapter;
    private List<SubsidyApplication> allApplications = new ArrayList<>();
    private List<SubsidyApplication> filteredApplications = new ArrayList<>();
    private List<String> documentIds = new ArrayList<>(); // To store document IDs

    // Firestore
    private FirebaseFirestore db;

    // Filter state
    private String currentFilter = "ALL"; // ALL, Pending, Approved, Rejected

    // Current barangay - this would typically come from user session or intent
    private String currentBarangay = "Anahawon"; // Default value

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subsidy_list);

        // Get barangay from intent if available
        if (getIntent().hasExtra("barangay")) {
            currentBarangay = getIntent().getStringExtra("barangay");
        }

        // Get filter from intent if available
        if (getIntent().hasExtra("filter")) {
            currentFilter = getIntent().getStringExtra("filter");
        }

        Log.d(TAG, "Starting activity with barangay: " + currentBarangay + " and filter: " + currentFilter);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        initViews();

        // Set up RecyclerView
        setupRecyclerView();

        // Set up click listeners
        setupClickListeners();

        // Load data from Firestore
        loadSubsidyApplications();
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
    }

    private void setupRecyclerView() {
        // Initialize RecyclerView with a LinearLayoutManager
        recyclerSubsidyList.setLayoutManager(new LinearLayoutManager(this));

        // Initialize the adapter with context and filtered applications
        adapter = new SubsidyAdapter(this, filteredApplications);

        // Set the current barangay in the adapter
        adapter.setCurrentBarangay(currentBarangay);

        // Set the adapter to RecyclerView
        recyclerSubsidyList.setAdapter(adapter);

        // Optional: Add item decoration for spacing between items if needed
        recyclerSubsidyList.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }

    private void setupClickListeners() {
        // Back button click listener
        btnBack.setOnClickListener(v -> onBackPressed());

        // Filter card click listeners
        cardPending.setOnClickListener(v -> filterApplications("Pending"));
        cardApproved.setOnClickListener(v -> filterApplications("Approved"));
        cardRejected.setOnClickListener(v -> filterApplications("Rejected"));
    }

    private void loadSubsidyApplications() {
        // Show loading state
        showLoading(true);

        // Clear existing data
        allApplications.clear();
        documentIds.clear();

        Log.d(TAG, "Loading subsidy applications from Firestore for barangay: " + currentBarangay);

        // Query Firestore for subsidy requests
        db.collection("Barangays")
                .document(currentBarangay)
                .collection("SubsidyRequests")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Successfully retrieved " + task.getResult().size() + " documents");

                        for (DocumentSnapshot document : task.getResult()) {
                            Log.d(TAG, "Document ID: " + document.getId());

                            // Convert document to SubsidyApplication object
                            SubsidyApplication application = document.toObject(SubsidyApplication.class);

                            // Store document ID for reference
                            if (application != null) {
                                allApplications.add(application);
                                documentIds.add(document.getId()); // Store the document ID in class-level list

                                // Log application details for debugging
                                Log.d(TAG, "Added application: " + application.getFarmerName() +
                                        " with status: " + application.getStatus() +
                                        " and document ID: " + document.getId());
                            } else {
                                Log.e(TAG, "Failed to convert document to SubsidyApplication: " + document.getId());
                            }
                        }

                        // Update UI
                        updateStatusCounts();
                        filterApplications(currentFilter); // Apply the current filter
                        showLoading(false);

                    } else {
                        Log.e(TAG, "Error getting documents: ", task.getException());
                        Toast.makeText(SubsidyListActivity.this,
                                "Error loading applications: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                        showLoading(false);
                        showEmptyState("Error loading data. Please try again.");
                    }
                });
    }

    private void filterApplications(String filter) {
        currentFilter = filter;
        filteredApplications.clear();

        // Create a parallel list to keep track of document IDs for filtered items
        List<String> filteredDocumentIds = new ArrayList<>();

        Log.d(TAG, "Filtering applications with filter: " + filter);

        // Apply filter
        if (filter.equals("ALL")) {
            filteredApplications.addAll(allApplications);
            filteredDocumentIds.addAll(documentIds);
            Log.d(TAG, "Showing all " + filteredApplications.size() + " applications");
        } else {
            for (int i = 0; i < allApplications.size(); i++) {
                SubsidyApplication app = allApplications.get(i);
                if (app.getStatus() != null && app.getStatus().equalsIgnoreCase(filter)) {
                    filteredApplications.add(app);
                    if (i < documentIds.size()) {
                        filteredDocumentIds.add(documentIds.get(i));
                    }
                }
            }
            Log.d(TAG, "Found " + filteredApplications.size() + " " + filter + " applications");
        }

        // Update adapter with filtered applications
        adapter.updateData(filteredApplications);

        // Set document IDs in adapter
        adapter.setDocumentIds(filteredDocumentIds);

        // Update UI
        updateFilterSelection();

        // Show empty state if needed
        if (filteredApplications.isEmpty()) {
            if (allApplications.isEmpty()) {
                showEmptyState("No subsidy applications found");
            } else {
                showEmptyState("No " + filter.toLowerCase() + " applications found");
            }
        } else {
            emptyState.setVisibility(View.GONE);
        }
    }

    private void showEmptyState(String message) {
        emptyState.setVisibility(View.VISIBLE);
        tvEmptyMessage.setText(message);
        Log.d(TAG, "Showing empty state: " + message);
    }

    private void updateFilterSelection() {
        // Reset all cards to default appearance
        cardPending.setCardElevation(0);
        cardApproved.setCardElevation(0);
        cardRejected.setCardElevation(0);

        // Highlight selected filter
        switch (currentFilter) {
            case "Pending":
                cardPending.setCardElevation(4);
                break;
            case "Approved":
                cardApproved.setCardElevation(4);
                break;
            case "Rejected":
                cardRejected.setCardElevation(4);
                break;
        }

        Log.d(TAG, "Updated filter selection to: " + currentFilter);
    }

    private void updateStatusCounts() {
        int pendingCount = 0;
        int approvedCount = 0;
        int rejectedCount = 0;

        for (SubsidyApplication app : allApplications) {
            if (app.getStatus() != null) {
                if (app.getStatus().equalsIgnoreCase("Pending")) {
                    pendingCount++;
                } else if (app.getStatus().equalsIgnoreCase("Approved")) {
                    approvedCount++;
                } else if (app.getStatus().equalsIgnoreCase("Rejected")) {
                    rejectedCount++;
                }
            }
        }

        tvPendingCount.setText(String.valueOf(pendingCount));
        tvApprovedCount.setText(String.valueOf(approvedCount));
        tvRejectedCount.setText(String.valueOf(rejectedCount));

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

    @Override
    protected void onResume() {
        super.onResume();
    }
}