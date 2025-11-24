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
import com.example.culturalcompass.ui.register.RegisterFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginFragment extends Fragment {

    private EditText etEmail, etPassword;
    private TextView tvError, tvForgotPassword;

    private FirebaseAuth auth;        // firebase login system
    private FirebaseFirestore db;     // user profiles

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

        // Firebase init
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // hide the action bar + bottom nav on login screen
        ((MainActivity) requireActivity()).hideChrome();

        // grab UI elements
        etEmail = v.findViewById(R.id.etEmailLogin);
        etPassword = v.findViewById(R.id.etPasswordLogin);
        tvError = v.findViewById(R.id.tvLoginError);
        tvForgotPassword = v.findViewById(R.id.tvForgotPassword);

        Button btnLogin = v.findViewById(R.id.btnLogin);
        TextView tvGoRegister = v.findViewById(R.id.tvGoRegister);

        // login button clicked
        btnLogin.setOnClickListener(view -> login());

        // go to register screen
        tvGoRegister.setOnClickListener(view ->
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.flFragment, new RegisterFragment())
                        .addToBackStack(null)
                        .commit()
        );

        // forgot password email
        tvForgotPassword.setOnClickListener(view -> resetPassword());
    }

    @Override
    public void onStart() {
        super.onStart();

        // if already logged in → skip login page
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser != null) {
            String email = firebaseUser.getEmail();
            if (email != null) {
                loadUserProfileAndGoHome(email); // load name and go home
            } else {
                ((MainActivity) requireActivity()).navigateToHome();
            }
        }
    }

    private void login() {
        tvError.setVisibility(View.GONE);

        String email = etEmail.getText().toString().trim();
        String pwd = etPassword.getText().toString();

        // basic checks
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Invalid email.");
            return;
        }

        if (TextUtils.isEmpty(pwd)) {
            showError("Password is required.");
            return;
        }

        // firebase login
        auth.signInWithEmailAndPassword(email, pwd)
                .addOnSuccessListener(result -> {
                    // after successful login → get user info from Firestore
                    loadUserProfileAndGoHome(email);
                })
                .addOnFailureListener(e -> showError("Wrong email or password."));
    }

    private void loadUserProfileAndGoHome(String email) {
        // fetch user document to get their name
        db.collection("users").document(email)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // pull the user's name
                        String name = doc.getString("name");

                        if (name != null && !name.isEmpty()) {
                            Session.currentUsername = name;
                        } else {
                            // fallback: use "email-before-@"
                            String prefix = email.substring(0, email.indexOf("@"));
                            Session.currentUsername = prefix;
                        }

                        // update the "Hello, X" greeting
                        ((MainActivity) requireActivity()).updateGreeting();
                    }

                    // go to home page either way
                    ((MainActivity) requireActivity()).navigateToHome();
                })
                .addOnFailureListener(e -> {
                    // even if Firestore fails → still go home if auth was ok
                    ((MainActivity) requireActivity()).navigateToHome();
                });
    }

    private void resetPassword() {
        // send a reset email if the address is valid
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

    private void showError(String msg) {
        // show error below input fields
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }
}
