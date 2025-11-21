package com.example.culturalcompass.ui.login;

import android.os.Bundle;
import android.text.TextUtils;

import android.util.Patterns;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.culturalcompass.MainActivity;
import com.example.culturalcompass.R;
import com.example.culturalcompass.model.Session;
import com.example.culturalcompass.model.User;
import com.example.culturalcompass.ui.register.RegisterFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginFragment extends Fragment {

    private EditText etEmail, etPassword;
    private TextView tvError, tvForgotPassword;

    private FirebaseAuth auth;            // Firebase Authentication
    private FirebaseFirestore db;         // Firestore (user profiles)

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Hide header + bottom nav on login screen
        ((MainActivity) requireActivity()).hideChrome();

        // Get UI references
        etEmail = v.findViewById(R.id.etEmailLogin);
        etPassword = v.findViewById(R.id.etPasswordLogin);
        tvError = v.findViewById(R.id.tvLoginError);
        tvForgotPassword = v.findViewById(R.id.tvForgotPassword);

        Button btnLogin = v.findViewById(R.id.btnLogin);
        TextView tvGoRegister = v.findViewById(R.id.tvGoRegister);

        // Login button
        btnLogin.setOnClickListener(view -> login());

        // Go to register
        tvGoRegister.setOnClickListener(view ->
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.flFragment, new RegisterFragment())
                        .addToBackStack(null)
                        .commit()
        );

        // Forgot password with FirebaseAuth
        tvForgotPassword.setOnClickListener(view -> resetPassword());
    }

    // Auto-login if already authenticated
    @Override
    public void onStart() {
        super.onStart();

        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser != null) {
            String email = firebaseUser.getEmail();
            if (email != null) {
                // Load profile and go home
                loadUserProfileAndGoHome(email);
            } else {
                ((MainActivity) requireActivity()).navigateToHome();
            }
        }
    }

    /**
     * LOGIN with FirebaseAuth
     */
    private void login() {
        tvError.setVisibility(View.GONE);

        String email = etEmail.getText().toString().trim();
        String pwd = etPassword.getText().toString();

        // Validation
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Invalid email.");
            return;
        }

        if (TextUtils.isEmpty(pwd)) {
            showError("Password is required.");
            return;
        }

        // FirebaseAuth login
        auth.signInWithEmailAndPassword(email, pwd)
                .addOnSuccessListener(result -> {
                    // After successful Firebase login → load profile from Firestore
                    loadUserProfileAndGoHome(email);
                })
                .addOnFailureListener(e -> {
                    showError("Wrong email or password.");
                });
    }

    /**
     * Loads user profile document from Firestore:
     * /users/{email}
     *
     * Puts it into Session.currentUser
     * Updates greeting text
     * Navigates to home
     */
    private void loadUserProfileAndGoHome(String email) {
        db.collection("users").document(email)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // Convert Firestore document to User model
                        User user = doc.toObject(User.class);

                        // Save in global session
                        Session.currentUser = user;

                        // Update header greeting if available
                        ((MainActivity) requireActivity()).updateGreeting();
                    }

                    // Go to home screen
                    ((MainActivity) requireActivity()).navigateToHome();
                })
                .addOnFailureListener(e -> {
                    // If Firestore fails but Auth succeeded → still go home
                    ((MainActivity) requireActivity()).navigateToHome();
                });
    }

    /**
     * Sends "reset password" email via FirebaseAuth
     */
    private void resetPassword() {
        String email = etEmail.getText().toString().trim();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(getContext(), "Enter a valid email first.", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused ->
                        Toast.makeText(getContext(),
                                "Password reset email sent!", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    /**
     * Shows an error message under login form
     */
    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }
}
