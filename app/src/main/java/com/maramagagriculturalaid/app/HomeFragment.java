package com.maramagagriculturalaid.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.maramagagriculturalaid.app.FarmersData.ActivityItem;
import com.maramagagriculturalaid.app.FarmersData.AddFarmerAcitivity;
import com.maramagagriculturalaid.app.FarmersData.FarmersDataFragment;
import com.maramagagriculturalaid.app.SubsidyManagement.SubsidyListActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private TextView greetingText, titleText, emailText, tvpendingCount, tvapprovedCount, tvrejectedCount;
    private AppCompatTextView createFarmersData, editFarmersData;
    private LinearLayout pendingSection, approvedSection, rejectedSection;
    private RecyclerView activityRecyclerView;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;
    private String userBarangay;

    private List<ActivityItem> allActivities = new ArrayList<>();
    private RecentActivityAdapter activityAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
        }

        initializeViews(view);
        loadUserData();
        setupRecyclerView();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        if (userBarangay != null && !userBarangay.isEmpty()) {
            loadRecentActivities();
        } else {
            Log.w(TAG, "onResume: userBarangay is null or empty");
        }
    }

    private void initializeViews(View view) {
        greetingText = view.findViewById(R.id.greetingText);
        titleText = view.findViewById(R.id.titleText);
        emailText = view.findViewById(R.id.emailText);
        createFarmersData = view.findViewById(R.id.createFarmersData);
        editFarmersData = view.findViewById(R.id.editFarmersData);
        pendingSection = view.findViewById(R.id.pendingSection);
        approvedSection = view.findViewById(R.id.approvedSection);
        rejectedSection = view.findViewById(R.id.rejectedSection);
        tvpendingCount = view.findViewById(R.id.tvpendingCount);
        tvapprovedCount = view.findViewById(R.id.tvapprovedCount);
        tvrejectedCount = view.findViewById(R.id.tvrejectedCount);
        activityRecyclerView = view.findViewById(R.id.activityRecyclerView);
    }

    private void loadUserData() {
        Log.d(TAG, "Loading user data for userId: " + userId);
        DocumentReference docRef = db.collection("Users").document(userId);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    userBarangay = document.getString("Barangay");
                    String email = document.getString("Email");

                    Log.d(TAG, "User barangay loaded: '" + userBarangay + "'");
                    Log.d(TAG, "User email loaded: '" + email + "'");

                    if (userBarangay != null) {
                        titleText.setText("Barangay " + userBarangay);
                        loadRecentActivities();
                    } else {
                        Log.w(TAG, "User barangay is null");
                    }
                    if (email != null) {
                        emailText.setText(email);
                    }
                    setupClickListeners();
                } else {
                    Log.e(TAG, "User document does not exist");
                    Toast.makeText(getContext(), "User document not found", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e(TAG, "Failed to load user data", task.getException());
                Toast.makeText(getContext(), "Failed to load user info", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupClickListeners() {
        createFarmersData.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), AddFarmerAcitivity.class)));

        editFarmersData.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity2) {
                ((MainActivity2) getActivity()).replaceFragment(new FarmersDataFragment());
            }
        });

        pendingSection.setOnClickListener(v -> openSubsidyList("Pending"));
        approvedSection.setOnClickListener(v -> openSubsidyList("Approved"));
        rejectedSection.setOnClickListener(v -> openSubsidyList("Rejected"));
    }

    private void openSubsidyList(String filter) {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), SubsidyListActivity.class);
            intent.putExtra("filter", filter);
            if (userBarangay != null) {
                intent.putExtra("barangay", userBarangay);
            }
            startActivity(intent);
        }
    }

    private void loadRecentActivities() {
        Log.d(TAG, "=== LOADING RECENT ACTIVITIES ===");
        Log.d(TAG, "User barangay: '" + userBarangay + "'");

        if (userBarangay == null || userBarangay.isEmpty()) {
            Log.w(TAG, "Cannot load activities - barangay is null or empty");
            showTestActivities(); // Show test activities for debugging
            return;
        }

        allActivities.clear();
        Log.d(TAG, "Cleared activities list");

        // Load recent farmers first
        loadRecentFarmers();
    }

    private void showTestActivities() {
        Log.d(TAG, "Showing test activities for debugging");
        allActivities.clear();

        // Add test activities
        ActivityItem testActivity1 = new ActivityItem();
        testActivity1.setType("farmer_added");
        testActivity1.setTitle("Added farmer");
        testActivity1.setDescription("Test Farmer 1");
        testActivity1.setTimestamp(System.currentTimeMillis() - (2 * 60 * 60 * 1000)); // 2 hours ago
        allActivities.add(testActivity1);

        ActivityItem testActivity2 = new ActivityItem();
        testActivity2.setType("subsidy_added");
        testActivity2.setTitle("Added subsidy application");
        testActivity2.setDescription("Test Farmer 2 - Rice");
        testActivity2.setTimestamp(System.currentTimeMillis() - (1 * 24 * 60 * 60 * 1000)); // 1 day ago
        allActivities.add(testActivity2);

        ActivityItem testActivity3 = new ActivityItem();
        testActivity3.setType("farmer_edited");
        testActivity3.setTitle("Edited farmer");
        testActivity3.setDescription("Test Farmer 3");
        testActivity3.setTimestamp(System.currentTimeMillis() - (30 * 60 * 1000)); // 30 minutes ago
        allActivities.add(testActivity3);

        Log.d(TAG, "Added " + allActivities.size() + " test activities");
        updateActivityDisplay();
    }

    private void loadRecentFarmers() {
        Log.d(TAG, "=== LOADING FARMERS ===");

        // First, let's see ALL farmers to debug
        db.collection("Farmers")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Total farmers in database: " + task.getResult().size());

                        if (task.getResult().size() == 0) {
                            Log.w(TAG, "No farmers found in database at all!");
                            showTestActivities();
                            return;
                        }

                        int matchingFarmers = 0;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Log.d(TAG, "--- Farmer Document ---");
                                Log.d(TAG, "Document ID: " + document.getId());

                                // Log all fields in the document
                                Map<String, Object> data = document.getData();
                                for (String key : data.keySet()) {
                                    Log.d(TAG, "Field '" + key + "': " + data.get(key));
                                }

                                // Check all possible barangay field names
                                String docBarangay = document.getString("barangay");
                                if (docBarangay == null) {
                                    docBarangay = document.getString("Barangay");
                                }
                                if (docBarangay == null) {
                                    docBarangay = document.getString("location");
                                }
                                if (docBarangay == null) {
                                    docBarangay = document.getString("address");
                                }

                                Log.d(TAG, "Document barangay: '" + docBarangay + "'");
                                Log.d(TAG, "User barangay: '" + userBarangay + "'");

                                // Check if this farmer belongs to the user's barangay (case insensitive)
                                boolean barangayMatches = false;
                                if (docBarangay != null && userBarangay != null) {
                                    barangayMatches = docBarangay.trim().equalsIgnoreCase(userBarangay.trim());
                                }

                                Log.d(TAG, "Barangay matches: " + barangayMatches);

                                if (barangayMatches) {
                                    matchingFarmers++;

                                    ActivityItem activity = new ActivityItem();
                                    activity.setType("farmer_added");
                                    activity.setTitle("Added farmer");

                                    // Get farmer name
                                    String farmerName = document.getString("fullName");
                                    if (farmerName == null || farmerName.trim().isEmpty()) {
                                        String firstName = document.getString("firstName");
                                        String lastName = document.getString("lastName");
                                        farmerName = "";
                                        if (firstName != null) farmerName += firstName;
                                        if (lastName != null) farmerName += " " + lastName;
                                        farmerName = farmerName.trim();
                                    }

                                    if (farmerName.isEmpty()) {
                                        farmerName = "Unknown Farmer";
                                    }

                                    activity.setFarmerName(farmerName);
                                    activity.setDescription(farmerName);

                                    // Handle timestamp - try multiple field names
                                    long timestamp = System.currentTimeMillis(); // Default to now
                                    Object createdAtObj = document.get("createdAt");
                                    if (createdAtObj == null) {
                                        createdAtObj = document.get("timestamp");
                                    }
                                    if (createdAtObj == null) {
                                        createdAtObj = document.get("dateCreated");
                                    }
                                    if (createdAtObj == null) {
                                        createdAtObj = document.get("created_at");
                                    }

                                    Log.d(TAG, "Timestamp object: " + createdAtObj + " (type: " + (createdAtObj != null ? createdAtObj.getClass().getSimpleName() : "null") + ")");

                                    if (createdAtObj instanceof Long) {
                                        timestamp = (Long) createdAtObj;
                                    } else if (createdAtObj instanceof com.google.firebase.Timestamp) {
                                        timestamp = ((com.google.firebase.Timestamp) createdAtObj).getSeconds() * 1000;
                                    } else {
                                        Log.w(TAG, "No valid timestamp found, using current time");
                                        timestamp = System.currentTimeMillis();
                                    }

                                    activity.setTimestamp(timestamp);
                                    allActivities.add(activity);

                                    Log.d(TAG, "✓ Added farmer activity: " + farmerName + " at " + timestamp);
                                } else {
                                    Log.d(TAG, "✗ Farmer doesn't match barangay");
                                }

                                Log.d(TAG, "--- End Farmer Document ---");

                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing farmer document: " + document.getId(), e);
                            }
                        }

                        Log.d(TAG, "=== FARMERS SUMMARY ===");
                        Log.d(TAG, "Total farmers: " + task.getResult().size());
                        Log.d(TAG, "Matching farmers: " + matchingFarmers);
                        Log.d(TAG, "Activities added: " + allActivities.size());

                        if (allActivities.isEmpty()) {
                            Log.w(TAG, "No farmer activities found, showing test activities");
                            showTestActivities();
                        } else {
                            // Load crop applications
                            loadRecentCropApplications();
                        }

                    } else {
                        Log.e(TAG, "Error loading farmers", task.getException());
                        showTestActivities();
                    }
                });
    }

    private void loadRecentCropApplications() {
        Log.d(TAG, "=== LOADING CROP APPLICATIONS ===");

        db.collection("Crop")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Total crop applications: " + task.getResult().size());

                        int pending = 0, approved = 0, rejected = 0;
                        int matchingCrops = 0;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                // Check barangay
                                String docBarangay = document.getString("barangay");
                                if (docBarangay == null) {
                                    docBarangay = document.getString("Barangay");
                                }

                                // Count all for status regardless of barangay
                                String status = document.getString("status");
                                if (status != null) {
                                    switch (status.toLowerCase().trim()) {
                                        case "pending":
                                            pending++;
                                            break;
                                        case "approved":
                                            approved++;
                                            break;
                                        case "rejected":
                                            rejected++;
                                            break;
                                    }
                                } else {
                                    pending++;
                                }

                                // Only add to activities if it matches the barangay
                                if (docBarangay != null && docBarangay.equalsIgnoreCase(userBarangay)) {
                                    matchingCrops++;

                                    ActivityItem activity = new ActivityItem();
                                    activity.setType("subsidy_added");
                                    activity.setTitle("Added subsidy application");

                                    String farmerName = document.getString("farmerName");
                                    String cropsGrown = document.getString("cropsGrown");

                                    activity.setFarmerName(farmerName);
                                    activity.setCropsGrown(cropsGrown);

                                    String description = farmerName != null ? farmerName : "Unknown Farmer";
                                    if (cropsGrown != null && !cropsGrown.trim().isEmpty()) {
                                        description += " - " + cropsGrown;
                                    }
                                    activity.setDescription(description);

                                    // Handle timestamp
                                    long timestamp = System.currentTimeMillis();
                                    Object createdAtObj = document.get("createdAt");
                                    if (createdAtObj instanceof Long) {
                                        timestamp = (Long) createdAtObj;
                                    } else if (createdAtObj instanceof com.google.firebase.Timestamp) {
                                        timestamp = ((com.google.firebase.Timestamp) createdAtObj).getSeconds() * 1000;
                                    }

                                    activity.setTimestamp(timestamp);
                                    allActivities.add(activity);

                                    Log.d(TAG, "Added subsidy activity: " + description);
                                }

                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing crop document: " + document.getId(), e);
                            }
                        }

                        Log.d(TAG, "=== CROP APPLICATIONS SUMMARY ===");
                        Log.d(TAG, "Total crops: " + task.getResult().size());
                        Log.d(TAG, "Matching crops: " + matchingCrops);
                        Log.d(TAG, "Total activities now: " + allActivities.size());

                        // Update status counts
                        updateStatusCounts(pending, approved, rejected);
                        updateActivityDisplay();

                    } else {
                        Log.e(TAG, "Error loading crop applications", task.getException());
                        updateActivityDisplay();
                    }
                });
    }

    private void updateStatusCounts(int pending, int approved, int rejected) {
        Log.d(TAG, "Updating status counts - Pending: " + pending + ", Approved: " + approved + ", Rejected: " + rejected);
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                tvpendingCount.setText(String.valueOf(pending));
                tvapprovedCount.setText(String.valueOf(approved));
                tvrejectedCount.setText(String.valueOf(rejected));
            });
        }
    }

    private void updateActivityDisplay() {
        Log.d(TAG, "=== UPDATING ACTIVITY DISPLAY ===");
        Log.d(TAG, "Total activities before sorting: " + allActivities.size());

        // Sort all activities by timestamp (most recent first)
        Collections.sort(allActivities, new Comparator<ActivityItem>() {
            @Override
            public int compare(ActivityItem a, ActivityItem b) {
                return Long.compare(b.getTimestamp(), a.getTimestamp());
            }
        });

        // Take only the most recent 3 activities
        List<ActivityItem> recentActivities = new ArrayList<>();
        for (int i = 0; i < Math.min(3, allActivities.size()); i++) {
            recentActivities.add(allActivities.get(i));
        }

        Log.d(TAG, "Displaying recent activities count: " + recentActivities.size());
        for (int i = 0; i < recentActivities.size(); i++) {
            ActivityItem activity = recentActivities.get(i);
            Log.d(TAG, "Activity " + (i+1) + ": " + activity.getTitle() + " - " + activity.getDescription() + " (timestamp: " + activity.getTimestamp() + ")");
        }

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (activityAdapter == null) {
                    Log.d(TAG, "Creating new adapter");
                    activityAdapter = new RecentActivityAdapter(recentActivities);
                    activityRecyclerView.setAdapter(activityAdapter);
                } else {
                    Log.d(TAG, "Updating existing adapter");
                    activityAdapter.updateActivities(recentActivities);
                }
                Log.d(TAG, "Adapter updated successfully");
            });
        } else {
            Log.w(TAG, "Activity is null, cannot update UI");
        }
    }

    private void setupRecyclerView() {
        activityRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Show loading state
        List<ActivityItem> loading = new ArrayList<>();
        activityAdapter = new RecentActivityAdapter(loading);
        activityRecyclerView.setAdapter(activityAdapter);

        Log.d(TAG, "RecyclerView setup complete");
    }
}