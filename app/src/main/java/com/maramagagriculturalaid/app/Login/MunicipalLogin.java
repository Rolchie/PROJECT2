package com.maramagagriculturalaid.app.Login;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.maramagagriculturalaid.app.MainActivity2;
import com.maramagagriculturalaid.app.Municipal.MunicipalActivity;
import com.maramagagriculturalaid.app.R;
import com.maramagagriculturalaid.app.Logs.EventLogger;

public class MunicipalLogin extends Fragment {

    TextInputEditText editTextEmail, editTextPassword;
    AppCompatButton buttonLogin;
    FirebaseAuth mAuth;
    SessionsManager sessionManager;

    // Enhanced loading state elements
    CardView loadingState;
    LinearProgressIndicator enhancedProgress;

    @Override
    public void onStart() {
        super.onStart();
        if (mAuth == null) {
            mAuth = FirebaseAuth.getInstance();
        }

        if (sessionManager == null) {
            sessionManager = new SessionsManager(requireContext());
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null && sessionManager.isMunicipal()){
            // Log auto-login event
            EventLogger.logUserAction(currentUser.getEmail(), "auto-logged in as Municipal (session active)");
            Intent intent = new Intent(requireContext(), MunicipalActivity.class);
            startActivity(intent);
            requireActivity().finish();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_municipal_login, container, false);

        // Initialize EventLogger with context
        EventLogger.init(requireContext());
        EventLogger.logSystemEvent("Municipal Login screen opened");

        mAuth = FirebaseAuth.getInstance();
        sessionManager = new SessionsManager(requireContext());

        // Initialize views
        editTextEmail = view.findViewById(R.id.et_username);
        editTextPassword = view.findViewById(R.id.et_password);
        buttonLogin = view.findViewById(R.id.Mlogin);

        loadingState = view.findViewById(R.id.loading_state);
        enhancedProgress = view.findViewById(R.id.enhanced_progress);

        buttonLogin.setOnClickListener(v -> {
            String email = String.valueOf(editTextEmail.getText());
            String password = String.valueOf(editTextPassword.getText());

            if(TextUtils.isEmpty(email)) {
                showError("Please enter your email");
                EventLogger.logError("Municipal login attempt with empty email", null);
                return;
            }

            if(TextUtils.isEmpty(password)) {
                showError("Please enter your password");
                EventLogger.logError("Municipal login attempt with empty password for email: " + email, null);
                return;
            }

            showLoading(true);
            EventLogger.logUserAction(email, "attempting Municipal login");

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String uid = mAuth.getCurrentUser().getUid();
                            EventLogger.logUserAction(email, "authentication successful, checking Municipal role");

                            FirebaseFirestore.getInstance().collection("Users").document(uid)
                                    .get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        showLoading(false);
                                        if (documentSnapshot.exists()) {
                                            String role = documentSnapshot.getString("Role");
                                            if ("Municipal".equals(role)) {
                                                // Save session data
                                                sessionManager.createLoginSession(uid, email, SessionsManager.ROLE_MUNICIPAL);

                                                EventLogger.logUserAction(email, "login successful as Municipal");
                                                showSuccess("Login Successful");
                                                startActivity(new Intent(requireContext(), MunicipalActivity.class));
                                                requireActivity().finish();
                                            } else {
                                                EventLogger.logError("Access denied for " + email + ": Not a Municipal account (Role: " + role + ")", null);
                                                mAuth.signOut();
                                                showError("Access denied: Not a Municipal account.");
                                            }
                                        } else {
                                            EventLogger.logError("User role not found for " + email, null);
                                            mAuth.signOut();
                                            showError("User role not found.");
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        showLoading(false);
                                        EventLogger.logError("Failed to fetch user role for " + email, e);
                                        mAuth.signOut();
                                        showError("Failed to fetch user role: " + e.getMessage());
                                    });
                        } else {
                            showLoading(false);
                            EventLogger.logError("Municipal login failed for " + email, task.getException());
                            showError("Login Failed: " + task.getException().getMessage());
                        }
                    });
        });

        return view;
    }

    private void storeUserFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("MainActivity", "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();
                    Log.d("MainActivity", "FCM Token: " + token);

                    // Store token in Firestore
                    String userId = getCurrentUserId(); // Get current user ID
                    String barangay = getCurrentUserBarangay(); // Get user's barangay

                    FirebaseFirestore db = FirebaseFirestore.getInstance();

                    // Store token in user document
                    db.collection("Users")
                            .document(userId)
                            .update("fcmToken", token, "barangay", barangay)
                            .addOnSuccessListener(aVoid -> Log.d("MainActivity", "Token stored successfully"))
                            .addOnFailureListener(e -> Log.e("MainActivity", "Error storing token", e));
                });
    }

    private String getCurrentUserId() {
        // If you're using Firebase Authentication:
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            return user.getUid();
        }

        // If you're using your own authentication system,
        // return the user ID from your system
        return "default_user_id"; // Replace with your actual implementation
    }

    // Helper method to get user's barangay
    private String getCurrentUserBarangay() {
        // Implement your logic to get the user's barangay
        // This could come from SharedPreferences, user profile, etc.
        return "Anahawon"; // Replace with your actual implementation
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            buttonLogin.setVisibility(View.INVISIBLE);
            loadingState.setVisibility(View.VISIBLE);
            enhancedProgress.setIndeterminate(true);
        } else {
            loadingState.setVisibility(View.GONE);
            buttonLogin.setVisibility(View.VISIBLE);
        }
    }

    private void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void showSuccess(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}
