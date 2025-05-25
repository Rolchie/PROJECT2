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
import com.maramagagriculturalaid.app.FarmersData.AddFarmerAcitivity;
import com.maramagagriculturalaid.app.FarmersData.FarmersDataFragment;
import com.maramagagriculturalaid.app.SubsidyManagement.SubsidyListActivity;

import java.util.ArrayList;
import java.util.List;

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

    private List<ActivityItem> recentActivities = new ArrayList<>();
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
            loadSubsidyRequestCounts();
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

                    if (userBarangay != null) {
                        titleText.setText("Barangay " + userBarangay);
                        loadRecentActivities();
                        loadSubsidyRequestCounts();
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
        if (userBarangay == null || userBarangay.isEmpty()) {
            Log.w(TAG, "Cannot load activities - barangay is null or empty");
            return;
        }

        Log.d(TAG, "Loading recent activities from: Barangays/" + userBarangay + "/Recent Activities");

        recentActivities.clear();

        // Load from the Recent Activities collection in your database
        db.collection("Barangays")
                .document(userBarangay)
                .collection("Recent Activities")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(3) // Only get the 3 most recent
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Successfully loaded activities. Count: " + task.getResult().size());

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Log.d(TAG, "Processing activity document: " + document.getId());
                                Log.d(TAG, "Document data: " + document.getData());

                                ActivityItem activity = new ActivityItem();

                                // Get activity data from document
                                String type = document.getString("type");
                                String title = document.getString("title");
                                String description = document.getString("description");
                                String farmerName = document.getString("farmerName");
                                String cropsGrown = document.getString("cropsGrown");

                                // Handle timestamp
                                long timestamp = System.currentTimeMillis(); // Default
                                Object timestampObj = document.get("timestamp");
                                if (timestampObj instanceof Long) {
                                    timestamp = (Long) timestampObj;
                                } else if (timestampObj instanceof com.google.firebase.Timestamp) {
                                    timestamp = ((com.google.firebase.Timestamp) timestampObj).getSeconds() * 1000;
                                }

                                // Set activity data
                                activity.setType(type != null ? type : "unknown");
                                activity.setTitle(title != null ? title : "Activity");
                                activity.setDescription(description != null ? description : "No description");
                                activity.setFarmerName(farmerName);
                                activity.setCropsGrown(cropsGrown);
                                activity.setTimestamp(timestamp);

                                recentActivities.add(activity);

                                Log.d(TAG, "Added activity: " + activity.getTitle() + " - " + activity.getDescription());

                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing activity document: " + document.getId(), e);
                            }
                        }

                        updateActivityDisplay();

                    } else {
                        Log.e(TAG, "Error loading recent activities", task.getException());
                        showEmptyActivities();
                    }
                });
    }

    private void loadSubsidyRequestCounts() {
        if (userBarangay == null || userBarangay.isEmpty()) {
            return;
        }

        Log.d(TAG, "Loading subsidy request counts from: Barangays/" + userBarangay + "/SubsidyRequests");

        // Load subsidy requests to count statuses
        db.collection("Barangays")
                .document(userBarangay)
                .collection("SubsidyRequests")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int pending = 0, approved = 0, rejected = 0;

                        for (QueryDocumentSnapshot document : task.getResult()) {
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
                                pending++; // Default to pending if no status
                            }
                        }

                        Log.d(TAG, "Subsidy counts - Pending: " + pending + ", Approved: " + approved + ", Rejected: " + rejected);
                        updateStatusCounts(pending, approved, rejected);

                    } else {
                        Log.e(TAG, "Error loading subsidy requests", task.getException());
                    }
                });
    }

    private void updateStatusCounts(int pending, int approved, int rejected) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                tvpendingCount.setText(String.valueOf(pending));
                tvapprovedCount.setText(String.valueOf(approved));
                tvrejectedCount.setText(String.valueOf(rejected));
            });
        }
    }

    private void updateActivityDisplay() {
        Log.d(TAG, "Updating activity display with " + recentActivities.size() + " activities");

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
            });
        }
    }

    private void showEmptyActivities() {
        Log.d(TAG, "Showing empty activities state");
        recentActivities.clear();
        updateActivityDisplay();
    }

    private void setupRecyclerView() {
        activityRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        activityRecyclerView.setNestedScrollingEnabled(false);

        // Initialize with empty list
        activityAdapter = new RecentActivityAdapter(recentActivities);
        activityRecyclerView.setAdapter(activityAdapter);

        Log.d(TAG, "RecyclerView setup complete");
    }
}