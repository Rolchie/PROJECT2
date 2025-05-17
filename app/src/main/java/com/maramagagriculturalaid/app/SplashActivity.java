package com.maramagagriculturalaid.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.maramagagriculturalaid.app.Municipal.MunicipalActivity;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Add delay then check user authentication and role
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            checkUserAndNavigate();
        }, 2000); // 2 seconds delay
    }

    private void checkUserAndNavigate() {
        if (mAuth.getCurrentUser() != null) {
            // User is already logged in - check their role
            String userId = mAuth.getCurrentUser().getUid();
            Log.d(TAG, "User is logged in with ID: " + userId);

            DocumentReference docRef = db.collection("Users").document(userId);
            docRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        // Get user role data
                        String userType = document.getString("UserType");
                        String municipal = document.getString("Municipal");
                        String barangay = document.getString("Barangay");

                        Log.d(TAG, "UserType: " + userType + ", Municipal: " + municipal + ", Barangay: " + barangay);

                        // Navigate based on user role
                        Intent intent;
                        if ("Municipal".equalsIgnoreCase(userType) ||
                                (municipal != null && !municipal.trim().isEmpty())) {
                            // User is Municipal - go to MunicipalActivity
                            Log.d(TAG, "Navigating to MunicipalActivity");
                            intent = new Intent(SplashActivity.this, MunicipalActivity.class);
                        } else if ("Barangay".equalsIgnoreCase(userType) ||
                                (barangay != null && !barangay.trim().isEmpty())) {
                            // User is Barangay - go to MainActivity2
                            Log.d(TAG, "Navigating to MainActivity2 (Barangay)");
                            intent = new Intent(SplashActivity.this, MainActivity2.class);
                        } else {
                            // Unknown user type - sign out and go to login
                            Log.w(TAG, "Unknown user type. Signing out user.");
                            mAuth.signOut();
                            intent = new Intent(SplashActivity.this, MainActivity.class);
                        }

                        startActivity(intent);
                        finish();

                    } else {
                        // User document doesn't exist - sign out and go to login
                        Log.w(TAG, "User document not found. Signing out user.");
                        mAuth.signOut();
                        navigateToLogin();
                    }
                } else {
                    // Error loading user document - go to login
                    Log.e(TAG, "Error getting user document", task.getException());
                    Toast.makeText(SplashActivity.this, "Error loading user data", Toast.LENGTH_SHORT).show();
                    navigateToLogin();
                }
            });
        } else {
            // No user logged in - go to login screen
            Log.d(TAG, "No user logged in. Going to login screen.");
            navigateToLogin();
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}