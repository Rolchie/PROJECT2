package com.maramagagriculturalaid.app.Login;

import android.content.Intent;
import android.os.Bundle;


import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.maramagagriculturalaid.app.MainActivity2;
import com.maramagagriculturalaid.app.R;

public class BarangayLogin extends Fragment {

     TextInputEditText editTextEmail, editTextPassword;
     Button buttonReg;
     FirebaseAuth mAuth;
     ProgressBar progressBar;

    @Override
    public void onStart() {
        super.onStart();
        if (mAuth == null) {
            mAuth= FirebaseAuth.getInstance();
        }
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            Intent intent = new Intent(requireContext(), MainActivity2.class);
            startActivity(intent);
            requireActivity().finish();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_barangay_login, container, false);

        mAuth = FirebaseAuth.getInstance();

        editTextEmail = view.findViewById(R.id.et_username);
        editTextPassword = view.findViewById(R.id.et_password);
        buttonReg = view.findViewById(R.id.Blogin);
        progressBar = view.findViewById(R.id.progressBar);

        buttonReg.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            String email, password;
            email = String.valueOf(editTextEmail.getText());
            password = String.valueOf(editTextPassword.getText());

            if(TextUtils.isEmpty(email)) {
                Toast.makeText(requireContext(), "Enter email", Toast.LENGTH_SHORT).show();
                return;
            }

            if(TextUtils.isEmpty(password)) {
                Toast.makeText(requireContext(), "Enter password", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String uid = mAuth.getCurrentUser().getUid();
                            FirebaseFirestore.getInstance().collection("Users").document(uid)
                                    .get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        progressBar.setVisibility(View.GONE);
                                        if (documentSnapshot.exists()) {
                                            String role = documentSnapshot.getString("Role");
                                            if ("Barangay".equals(role)) {
                                                Toast.makeText(requireContext(), "Login Successful", Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(requireContext(), MainActivity2.class));
                                                requireActivity().finish();
                                            } else {
                                                mAuth.signOut();
                                                Toast.makeText(requireContext(), "Access denied: Not a Barangay account.", Toast.LENGTH_SHORT).show();
                                            }
                                        } else {
                                            mAuth.signOut();
                                            Toast.makeText(requireContext(), "User role not found.", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        progressBar.setVisibility(View.GONE);
                                        mAuth.signOut();
                                        Toast.makeText(requireContext(), "Failed to fetch user role.", Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), "Login Failed.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }); return view;
    }
}