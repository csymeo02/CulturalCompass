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
import com.example.culturalcompass.ui.map.MapFragment;
import com.example.culturalcompass.ui.register.RegisterFragment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.culturalcompass.model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class LoginFragment extends Fragment {

    private EditText etEmail, etPassword;
    private TextView tvError;
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

        ((MainActivity) requireActivity()).hideChrome(); // hide bottom nav

        etEmail = v.findViewById(R.id.etEmailLogin);
        etPassword = v.findViewById(R.id.etPasswordLogin);
        tvError = v.findViewById(R.id.tvLoginError);

        Button btnLogin = v.findViewById(R.id.btnLogin);
        TextView tvGoRegister = v.findViewById(R.id.tvGoRegister);

        btnLogin.setOnClickListener(view -> login());
        tvGoRegister.setOnClickListener(view ->
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.flFragment, new RegisterFragment())
                        .addToBackStack(null)
                        .commit()
        );
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

        String hash = sha256(pwd);

        db.collection("users").document(email)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        showError("Account not found.");
                        return;
                    }

                    String storedHash = doc.getString("passwordHash");

                    if (storedHash != null && storedHash.equals(hash)) {
                        User user = doc.toObject(User.class);
                        Session.currentUser = user;

                        ((MainActivity) requireActivity()).navigateToHome();

                    } else {
                        showError("Wrong password.");
                    }
                })
                .addOnFailureListener(e -> showError("Login failed."));
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }

    private String sha256(String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception ex) {
            return null;
        }
    }
}
