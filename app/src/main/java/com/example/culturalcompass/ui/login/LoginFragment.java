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
import com.example.culturalcompass.ui.map.MapFragment;
import com.example.culturalcompass.ui.register.RegisterFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;      // CHANGE HERE (added)
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginFragment extends Fragment {

    private EditText etEmail, etPassword;
    private TextView tvError, tvForgotPassword;

    private FirebaseAuth auth;             // CHANGE HERE (use FirebaseAuth)
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();        // CHANGE HERE

        ((MainActivity) requireActivity()).hideChrome(); // hide bottom nav

        etEmail = v.findViewById(R.id.etEmailLogin);
        etPassword = v.findViewById(R.id.etPasswordLogin);
        tvError = v.findViewById(R.id.tvLoginError);

        Button btnLogin = v.findViewById(R.id.btnLogin);
        TextView tvGoRegister = v.findViewById(R.id.tvGoRegister);

        tvForgotPassword = v.findViewById(R.id.tvForgotPassword);

        btnLogin.setOnClickListener(view -> login());

        tvGoRegister.setOnClickListener(view ->
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.flFragment, new RegisterFragment())
                        .addToBackStack(null)
                        .commit()
        );

        // CHANGE HERE - Forgot password
        tvForgotPassword.setOnClickListener(view -> resetPassword());
    }

    // CHANGE HERE — Auto-login
    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            ((MainActivity) requireActivity()).navigateToHome();
        }
    }

    private void login() {
        tvError.setVisibility(View.GONE);

        String email = etEmail.getText().toString().trim();
        String pwd = etPassword.getText().toString();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Invalid email.");
            return;
        }

        if (TextUtils.isEmpty(pwd)) {
            showError("Password is required.");
            return;
        }

        // CHANGE HERE — FirebaseAuth login
        auth.signInWithEmailAndPassword(email, pwd)
                .addOnSuccessListener(result -> {
                    ((MainActivity) requireActivity()).navigateToHome();   // CHANGE HERE
                })
                .addOnFailureListener(e -> {
                    showError("Wrong email or password.");
                });
    }

    // CHANGE HERE — Forgot password (FirebaseAuth)
    private void resetPassword() {
        String email = etEmail.getText().toString().trim();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(getContext(), "Enter valid email first", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused ->
                        Toast.makeText(getContext(), "Reset email sent!", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }

    // REMOVE SHA-256 — no longer used
    // private String sha256(String base) { ... }
}


//package com.example.culturalcompass.ui.login;
//
//import android.os.Bundle;
//import android.text.TextUtils;
//import android.util.Patterns;
//import android.view.*;
//import android.widget.*;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//
//import com.example.culturalcompass.MainActivity;
//import com.example.culturalcompass.R;
//import com.example.culturalcompass.ui.map.MapFragment;
//import com.example.culturalcompass.ui.register.RegisterFragment;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.firestore.FirebaseFirestore;
//
//import java.nio.charset.StandardCharsets;
//import java.security.MessageDigest;
//
//public class LoginFragment extends Fragment {
//
//    private EditText etEmail, etPassword;
//    private TextView tvError, tvForgotPassword;
//
//    private FirebaseAuth auth;
//    private FirebaseFirestore db;
//
//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
//                             @Nullable Bundle savedInstanceState) {
//        return inflater.inflate(R.layout.fragment_login, container, false);
//    }
//
//    @Override
//    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated(v, savedInstanceState);
//
//        db = FirebaseFirestore.getInstance();
//
//        ((MainActivity) requireActivity()).hideChrome(); // hide bottom nav
//
//        etEmail = v.findViewById(R.id.etEmailLogin);
//        etPassword = v.findViewById(R.id.etPasswordLogin);
//        tvError = v.findViewById(R.id.tvLoginError);
//
//        Button btnLogin = v.findViewById(R.id.btnLogin);
//        TextView tvGoRegister = v.findViewById(R.id.tvGoRegister);
//
//        tvForgotPassword = v.findViewById(R.id.tvForgotPassword);
//
//
//        btnLogin.setOnClickListener(view -> login());
//        tvGoRegister.setOnClickListener(view ->
//                requireActivity().getSupportFragmentManager().beginTransaction()
//                        .replace(R.id.flFragment, new RegisterFragment())
//                        .addToBackStack(null)
//                        .commit()
//        );
//    }
//
//    private void login() {
//        tvError.setVisibility(View.GONE);
//
//        String email = etEmail.getText().toString().trim();
//        String pwd = etPassword.getText().toString();
//
//        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
//            showError("Invalid email.");
//            return;
//        }
//
//        if (TextUtils.isEmpty(pwd)) {
//            showError("Password is required.");
//            return;
//        }
//
//        String hash = sha256(pwd);
//
//        db.collection("users").document(email)
//                .get()
//                .addOnSuccessListener(doc -> {
//                    if (!doc.exists()) {
//                        showError("Account not found.");
//                        return;
//                    }
//
//                    String storedHash = doc.getString("passwordHash");
//
//                    if (storedHash != null && storedHash.equals(hash)) {
//
//                        ((MainActivity) requireActivity()).navigateToHome();
//
//                    } else {
//                        showError("Wrong password.");
//                    }
//                })
//                .addOnFailureListener(e -> showError("Login failed."));
//    }
//
//    private void showError(String msg) {
//        tvError.setText(msg);
//        tvError.setVisibility(View.VISIBLE);
//    }
//
//    private String sha256(String base) {
//        try {
//            MessageDigest digest = MessageDigest.getInstance("SHA-256");
//            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
//            StringBuilder hex = new StringBuilder();
//            for (byte b : hash) {
//                String h = Integer.toHexString(0xff & b);
//                if (h.length() == 1) hex.append('0');
//                hex.append(h);
//            }
//            return hex.toString();
//        } catch (Exception ex) {
//            return null;
//        }
//    }
//}
