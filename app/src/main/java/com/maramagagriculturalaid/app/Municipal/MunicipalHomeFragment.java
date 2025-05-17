package com.maramagagriculturalaid.app.Municipal;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentReference;
import com.maramagagriculturalaid.app.Municipal.BarangayOverview.BarangayOverview;
import com.maramagagriculturalaid.app.R;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import com.maramagagriculturalaid.app.Municipal.MunicipalSubsidy.MunicipalSubsidyManagement;

public class MunicipalHomeFragment extends Fragment {

    private static final String TAG = "MunicipalHomeFragment";

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String userId;

    // UI Elements
    private View rootView;
    private TextView greetingText;
    private TextView titleText;
    private TextView emailText;
    private TextView totalFarmersValue;
    private TextView tvpendingCount;
    private TextView tvapprovedCount;
    private TextView tvrejectedCount;
    private Button viewDetailsButton;
    private LinearLayout pendingSection;
    private LinearLayout approvedSection;
    private LinearLayout rejectedSection;
    private RecyclerView activityRecyclerView;

    // Data
    private List<ActivityItem> activityItems = new ArrayList<>();
    private MunicipalActivityAdapter activityAdapter;

    public MunicipalHomeFragment() {
        // Required empty public constructor
    }

    public static MunicipalHomeFragment newInstance() {
        return new MunicipalHomeFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_municipal_home, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            userId = currentUser.getUid();
        }

        // Initialize UI elements
        initializeViews();

        // Set greeting based on time of day
        setGreeting();

        // Set user information
        setUserInfo();

        // Load statistics data
        loadStatistics();

        // Set up click listeners
        setupClickListeners();

        // Load recent activities
        loadRecentActivities();
    }

    private void initializeViews() {
        // Profile section
        greetingText = rootView.findViewById(R.id.greetingText);
        titleText = rootView.findViewById(R.id.titleText);
        emailText = rootView.findViewById(R.id.emailText);

        // Statistics section
        totalFarmersValue = rootView.findViewById(R.id.totalFarmersValue);
        viewDetailsButton = rootView.findViewById(R.id.viewDetailsButton);

        // Subsidy status section
        tvpendingCount = rootView.findViewById(R.id.tvpendingCount);
        tvapprovedCount = rootView.findViewById(R.id.tvapprovedCount);
        tvrejectedCount = rootView.findViewById(R.id.tvrejectedCount);
        pendingSection = rootView.findViewById(R.id.pendingSection);
        approvedSection = rootView.findViewById(R.id.approvedSection);
        rejectedSection = rootView.findViewById(R.id.rejectedSection);

        // Activity section - Updated to use MunicipalActivityAdapter
        activityRecyclerView = rootView.findViewById(R.id.activityRecyclerView);
        if (activityRecyclerView != null) {
            activityRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            activityAdapter = new MunicipalActivityAdapter(activityItems, getContext());
            activityRecyclerView.setAdapter(activityAdapter);
        }
    }

    private void setGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);

        String greeting;
        if (hourOfDay >= 0 && hourOfDay < 12) {
            greeting = "Good morning,";
        } else if (hourOfDay >= 12 && hourOfDay < 18) {
            greeting = "Good afternoon,";
        } else {
            greeting = "Good evening,";
        }

        if (greetingText != null) {
            greetingText.setText(greeting);
        }
    }

    private void setUserInfo() {
        if (currentUser != null && userId != null) {
            // Get user information from Users collection (consistent with your app structure)
            DocumentReference docRef = db.collection("Users").document(userId);
            docRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && isAdded()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        String name = document.getString("Municipal");
                        String email = document.getString("Email");

                        Log.d(TAG, "User data - Municipal: " + name + ", Email: " + email);

                        if (name != null && !name.isEmpty() && titleText != null) {
                            titleText.setText(name + " Municipality");
                        } else if (titleText != null) {
                            titleText.setText("Municipal Official");
                        }

                        if (email != null && !email.isEmpty() && emailText != null) {
                            emailText.setText(email);
                        } else if (emailText != null && currentUser.getEmail() != null) {
                            emailText.setText(currentUser.getEmail());
                        }
                    } else {
                        Log.d(TAG, "User document not found");
                        if (isAdded()) {
                            Toast.makeText(getContext(), "User document not found", Toast.LENGTH_SHORT).show();
                        }
                        // Set default values
                        if (titleText != null) {
                            titleText.setText("Municipal Official");
                        }
                        if (emailText != null && currentUser.getEmail() != null) {
                            emailText.setText(currentUser.getEmail());
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to load user info", task.getException());
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed to load user info", Toast.LENGTH_SHORT).show();
                    }
                    // Set default values
                    if (titleText != null) {
                        titleText.setText("Municipal Official");
                    }
                    if (emailText != null && currentUser.getEmail() != null) {
                        titleText.setText(currentUser.getEmail());
                    }
                }
            });
        } else {
            // Handle case where user is not logged in
            if (titleText != null) {
                titleText.setText("Municipal Official");
            }
            if (emailText != null) {
                emailText.setText("Not logged in");
            }
        }
    }

    private void loadStatistics() {
        // Load total farmers count from all barangays
        loadTotalFarmersCount();

        // Load subsidy counts for the subsidy status section
        loadSubsidyCounts();

        // Load total pending subsidy requests for the barangay overview container
        loadTotalPendingSubsidyRequests();
    }

    private void loadTotalFarmersCount() {
        final AtomicInteger totalFarmers = new AtomicInteger(0);
        final AtomicInteger barangaysProcessed = new AtomicInteger(0);

        // Get all barangays first
        db.collection("Barangays")
                .get()
                .addOnSuccessListener(barangaySnapshots -> {
                    int totalBarangays = barangaySnapshots.size();
                    if (totalBarangays == 0) {
                        if (totalFarmersValue != null) {
                            totalFarmersValue.setText("0");
                        }
                        return;
                    }

                    // For each barangay, count farmers
                    for (QueryDocumentSnapshot barangayDoc : barangaySnapshots) {
                        String barangayId = barangayDoc.getId();

                        db.collection("Barangays")
                                .document(barangayId)
                                .collection("Farmers")
                                .get()
                                .addOnSuccessListener(farmerSnapshots -> {
                                    totalFarmers.addAndGet(farmerSnapshots.size());

                                    // Check if we've processed all barangays
                                    if (barangaysProcessed.incrementAndGet() == totalBarangays) {
                                        if (isAdded() && totalFarmersValue != null) {
                                            totalFarmersValue.setText(String.format(Locale.getDefault(), "%,d", totalFarmers.get()));
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error getting farmers for barangay " + barangayId, e);
                                    if (barangaysProcessed.incrementAndGet() == totalBarangays) {
                                        if (isAdded() && totalFarmersValue != null) {
                                            totalFarmersValue.setText(String.format(Locale.getDefault(), "%,d", totalFarmers.get()));
                                        }
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting barangays for farmer count", e);
                    if (isAdded() && totalFarmersValue != null) {
                        totalFarmersValue.setText("0");
                    }
                });
    }

    private void loadTotalPendingSubsidyRequests() {
        Log.d(TAG, "Starting to load total pending subsidy requests for barangay overview");

        final AtomicInteger totalPendingSubsidies = new AtomicInteger(0);
        final AtomicInteger barangaysProcessed = new AtomicInteger(0);

        // Get all barangays first
        db.collection("Barangays")
                .get()
                .addOnSuccessListener(barangaySnapshots -> {
                    int totalBarangays = barangaySnapshots.size();
                    Log.d(TAG, "Found " + totalBarangays + " barangays for subsidy count");

                    if (totalBarangays == 0) {
                        updateTotalPendingSubsidyUI(0);
                        return;
                    }

                    // For each barangay, count SubsidyRequests
                    for (QueryDocumentSnapshot barangayDoc : barangaySnapshots) {
                        String barangayId = barangayDoc.getId();
                        Log.d(TAG, "Counting subsidies in barangay: " + barangayId);

                        db.collection("Barangays")
                                .document(barangayId)
                                .collection("SubsidyRequests")
                                .get()
                                .addOnSuccessListener(subsidySnapshots -> {
                                    int subsidyCount = subsidySnapshots.size();
                                    Log.d(TAG, "Found " + subsidyCount + " subsidy requests in " + barangayId);

                                    // Add all subsidy requests to total count
                                    totalPendingSubsidies.addAndGet(subsidyCount);

                                    // Check if we've processed all barangays
                                    int processed = barangaysProcessed.incrementAndGet();
                                    Log.d(TAG, "Processed " + processed + " of " + totalBarangays + " barangays for subsidy count");

                                    if (processed == totalBarangays) {
                                        int finalCount = totalPendingSubsidies.get();
                                        Log.d(TAG, "Final total pending subsidies count: " + finalCount);
                                        updateTotalPendingSubsidyUI(finalCount);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error getting subsidy requests for barangay " + barangayId, e);

                                    // Still increment processed count even on failure
                                    int processed = barangaysProcessed.incrementAndGet();
                                    if (processed == totalBarangays) {
                                        updateTotalPendingSubsidyUI(totalPendingSubsidies.get());
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting barangays for total subsidy count", e);
                    updateTotalPendingSubsidyUI(0);
                });
    }

    private void updateTotalPendingSubsidyUI(int totalPendingSubsidies) {
        Log.d(TAG, "Updating barangay overview with total pending subsidies: " + totalPendingSubsidies);

        if (isAdded()) {
            // Find the TextView for total pending subsidies in the barangay overview container
            // You'll need to replace this with the actual ID from your layout
            TextView totalPendingSubsidiesValue = rootView.findViewById(R.id.totalSubsidyValue);

            if (totalPendingSubsidiesValue != null) {
                totalPendingSubsidiesValue.setText(String.format(Locale.getDefault(), "%,d", totalPendingSubsidies));
                Log.d(TAG, "Set total pending subsidies to: " + totalPendingSubsidies);
            } else {
                Log.w(TAG, "totalPendingSubsidiesValue TextView not found - check the ID in your layout");
            }
        } else {
            Log.w(TAG, "Fragment not added, cannot update total pending subsidies UI");
        }
    }

    private void loadSubsidyCounts() {
        // Initialize counts
        final int[] pendingCount = {0};
        final int[] approvedCount = {0};
        final int[] rejectedCount = {0};
        final AtomicInteger barangaysProcessed = new AtomicInteger(0);

        // First get all barangays
        db.collection("Barangays")
                .get()
                .addOnSuccessListener(barangaySnapshots -> {
                    int totalBarangays = barangaySnapshots.size();
                    if (totalBarangays == 0) {
                        updateCountsUI(pendingCount[0], approvedCount[0], rejectedCount[0]);
                        return;
                    }

                    // For each barangay, get its SubsidyRequests
                    for (QueryDocumentSnapshot barangayDoc : barangaySnapshots) {
                        String barangayId = barangayDoc.getId();

                        db.collection("Barangays")
                                .document(barangayId)
                                .collection("SubsidyRequests")
                                .get()
                                .addOnSuccessListener(subsidySnapshots -> {
                                    for (QueryDocumentSnapshot subsidyDoc : subsidySnapshots) {
                                        String status = subsidyDoc.getString("status");
                                        if (status != null) {
                                            switch (status) {
                                                case "Pending":
                                                    pendingCount[0]++;
                                                    break;
                                                case "Approved":
                                                    approvedCount[0]++;
                                                    break;
                                                case "Rejected":
                                                    rejectedCount[0]++;
                                                    break;
                                            }
                                        }
                                    }

                                    // Check if we've processed all barangays
                                    if (barangaysProcessed.incrementAndGet() == totalBarangays) {
                                        updateCountsUI(pendingCount[0], approvedCount[0], rejectedCount[0]);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error getting subsidies for barangay " + barangayId, e);
                                    if (barangaysProcessed.incrementAndGet() == totalBarangays) {
                                        updateCountsUI(pendingCount[0], approvedCount[0], rejectedCount[0]);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting barangays", e);
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed to load barangay data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateCountsUI(int pending, int approved, int rejected) {
        if (isAdded()) {
            if (tvpendingCount != null) {
                tvpendingCount.setText(String.valueOf(pending));
            }
            if (tvapprovedCount != null) {
                tvapprovedCount.setText(String.valueOf(approved));
            }
            if (tvrejectedCount != null) {
                tvrejectedCount.setText(String.valueOf(rejected));
            }
        }
    }

    private void setupClickListeners() {
        // View All Barangays button - Updated to navigate to BarangayOverviewAttachment
        if (viewDetailsButton != null) {
            viewDetailsButton.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), BarangayOverview.class);
                startActivity(intent);
            });
        }

        // Pending subsidies section
        if (pendingSection != null) {
            pendingSection.setOnClickListener(v -> {
                Log.d(TAG, "Pending subsidies section clicked");
                // Navigate to MunicipalSubsidyManagement with pending filter
                Intent intent = new Intent(getActivity(), MunicipalSubsidyManagement.class);
                intent.putExtra("defaultTab", "pending");
                intent.putExtra("status", "Pending");
                startActivity(intent);
            });
        }

        // Approved subsidies section
        if (approvedSection != null) {
            approvedSection.setOnClickListener(v -> {
                Log.d(TAG, "Approved subsidies section clicked");
                // Navigate to MunicipalSubsidyManagement with approved filter
                Intent intent = new Intent(getActivity(), MunicipalSubsidyManagement.class);
                intent.putExtra("defaultTab", "approved");
                intent.putExtra("status", "Approved");
                startActivity(intent);
            });
        }

        // Rejected subsidies section
        if (rejectedSection != null) {
            rejectedSection.setOnClickListener(v -> {
                Log.d(TAG, "Rejected subsidies section clicked");
                // Navigate to MunicipalSubsidyManagement with rejected filter
                Intent intent = new Intent(getActivity(), MunicipalSubsidyManagement.class);
                intent.putExtra("defaultTab", "rejected");
                intent.putExtra("status", "Rejected");
                startActivity(intent);
            });
        }
    }

    private void loadRecentActivities() {
        // Clear existing items
        activityItems.clear();

        // Load activities from Firestore
        db.collection("activities")
                .orderBy("timestamp")
                .limit(10)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && isAdded()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String title = document.getString("title");
                            String description = document.getString("description");
                            String iconType = document.getString("iconType");
                            String timestamp = document.getString("timestamp");

                            ActivityItem item = new ActivityItem(title, description, iconType, timestamp);
                            activityItems.add(item);
                        }

                        // Notify adapter of data change
                        if (activityAdapter != null) {
                            activityAdapter.notifyDataSetChanged();
                        }
                    } else {
                        Log.d(TAG, "Error getting activities: ", task.getException());
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when fragment resumes
        loadStatistics();
        loadRecentActivities();
    }

    // Activity Item class for RecyclerView
    public static class ActivityItem {
        private String title;
        private String description;
        private String iconType;
        private String timestamp;

        public ActivityItem(String title, String description, String iconType, String timestamp) {
            this.title = title;
            this.description = description;
            this.iconType = iconType;
            this.timestamp = timestamp;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getIconType() {
            return iconType;
        }

        public String getTimestamp() {
            return timestamp;
        }
    }
}