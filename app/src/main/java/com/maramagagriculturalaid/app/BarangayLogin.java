package com.maramagagriculturalaid.app;

import android.content.Intent;
import android.os.Bundle;


import androidx.appcompat.widget.AppCompatButton;
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
        // Inflate the layout for this fragment
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
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            Toast.makeText(requireContext(), "Login Successful", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(requireContext(), MainActivity2.class);
                            startActivity(intent);
                            requireActivity().finish();
                        } else {
                            Toast.makeText(requireContext(), "Login Failed.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }); return view;
    }
}