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
import com.maramagagriculturalaid.app.FarmersData.AddFarmerAcitivity;

import java.util.ArrayList;
import java.util.List;


public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    // UI Components
    private TextView greetingText;
    private TextView titleText;
    private TextView emailText;
    private AppCompatTextView createFarmersData;
    private AppCompatTextView editFarmersData;
    private LinearLayout pendingSection;
    private LinearLayout approvedSection;
    private LinearLayout rejectedSection;
    private TextView pendingCount;
    private TextView approvedCount;
    private TextView rejectedCount;
    private RecyclerView activityRecyclerView;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;

    // Sample data
    private int pendingRequests = 12;
    private int approvedRequests = 45;
    private int rejectedRequests = 8;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Get current user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
        } else {
            Log.e(TAG, "No user is signed in");
        }

        initializeViews(view);
        loadUserData();
        setupClickListeners();
        loadDashboardData();
        setupRecyclerView();
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
        pendingCount = view.findViewById(R.id.pendingCount);
        approvedCount = view.findViewById(R.id.approvedCount);
        rejectedCount = view.findViewById(R.id.rejectedCount);
        activityRecyclerView = view.findViewById(R.id.activityRecyclerView);
    }

    private void loadUserData() {
        if (userId == null) {
            Log.e(TAG, "Cannot load user data: userId is null");
            return;
        }

        DocumentReference docRef = db.collection("Users").document(userId);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    String name = document.getString("Barangay");
                    String email = document.getString("Email");
                    String role = document.getString("Role");

                    // Update UI with user data
                    if (name != null) {
                        titleText.setText("Barangay" + " " + name);
                    }

                    if (email != null) {
                        emailText.setText(email);
                    }

                    Log.d(TAG, "User data loaded successfully");
                } else {
                    Log.d(TAG, "Document not found");
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "User document not found", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Log.e(TAG, "Fetch failed: ", task.getException());
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load user info", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupClickListeners() {
        createFarmersData.setOnClickListener(v -> {
            if (getActivity() != null) {
                Intent intent = new Intent(getActivity(), AddFarmerAcitivity.class);
                startActivity(intent);
            }
        });

        editFarmersData.setOnClickListener(v ->
                Toast.makeText(getContext(), "Edit Farmers Data clicked", Toast.LENGTH_SHORT).show());

        pendingSection.setOnClickListener(v ->
                Toast.makeText(getContext(), "Pending Requests clicked", Toast.LENGTH_SHORT).show());

        approvedSection.setOnClickListener(v ->
                Toast.makeText(getContext(), "Approved Requests clicked", Toast.LENGTH_SHORT).show());

        rejectedSection.setOnClickListener(v ->
                Toast.makeText(getContext(), "Rejected Requests clicked", Toast.LENGTH_SHORT).show());
    }


    private void loadDashboardData() {
        pendingCount.setText(String.valueOf(pendingRequests));
        approvedCount.setText(String.valueOf(approvedRequests));
        rejectedCount.setText(String.valueOf(rejectedRequests));
    }

    private void setupRecyclerView() {
        activityRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        List<String> sampleActivities = new ArrayList<>();
        sampleActivities.add("Request A approved");
        sampleActivities.add("Request B rejected");
        sampleActivities.add("Request C pending");

        // Replace this with your actual RecyclerView.Adapter
        activityRecyclerView.setAdapter(new SimpleStringAdapter(sampleActivities));
    }

    // Dummy Adapter for demonstration
    private static class SimpleStringAdapter extends RecyclerView.Adapter<SimpleStringAdapter.ViewHolder> {
        private final List<String> items;

        public SimpleStringAdapter(List<String> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView textView = new TextView(parent.getContext());
            int padding = (int) (16 * parent.getContext().getResources().getDisplayMetrics().density);
            textView.setPadding(padding, padding, padding, padding);
            return new ViewHolder(textView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ((TextView) holder.itemView).setText(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }
}