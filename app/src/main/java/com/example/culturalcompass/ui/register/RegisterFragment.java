package com.example.culturalcompass.ui.register;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.culturalcompass.MainActivity;
import com.example.culturalcompass.R;
import com.example.culturalcompass.model.User;
import com.example.culturalcompass.ui.login.LoginFragment;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Register screen — stores user info in Firestore
 * Email = user ID
 */
public class RegisterFragment extends Fragment {

    private EditText etName, etSurname, etEmail, etPassword, etConfirmPassword, etBirthday;
    private Button btnRegister;

    private TextView tvGoLogin;

    private FirebaseFirestore db;
    private DatePickerDialog datePickerDialog;
    private SimpleDateFormat dateFormat;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_register, container, false);

        etName = view.findViewById(R.id.etName);
        etSurname = view.findViewById(R.id.etSurname);
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        etConfirmPassword = view.findViewById(R.id.etVerifyPassword);
        etBirthday = view.findViewById(R.id.etBirthday);
        btnRegister = view.findViewById(R.id.btnRegister);

        tvGoLogin = view.findViewById(R.id.tvGoLogin);


        // ---------- DATE PICKER ----------
        initDatePicker();
        etBirthday.setOnClickListener(v -> datePickerDialog.show());

        // ---------- REDIRECT TO LOGIN ----------
        tvGoLogin.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.flFragment, new LoginFragment())
                    .commit();
        });

        // ---------- REGISTER BUTTON ----------
        btnRegister.setOnClickListener(v -> attemptRegister());

        return view;
    }

    private void initDatePicker() {
        Calendar cal = Calendar.getInstance();

        datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);
                    String formatted = dateFormat.format(selected.getTime());
                    etBirthday.setText(formatted);
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
    }

    private void attemptRegister() {

        String name = etName.getText().toString().trim();
        String surname = etSurname.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString();
        String confirmPass = etConfirmPassword.getText().toString();
        String birthdayStr = etBirthday.getText().toString().trim();

        // ---------- VALIDATION ----------
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(surname) ||
                TextUtils.isEmpty(email) || TextUtils.isEmpty(pass) ||
                TextUtils.isEmpty(confirmPass) || TextUtils.isEmpty(birthdayStr)) {

            Toast.makeText(getContext(), "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!name.matches("[a-zA-Z ]+")) {
            Toast.makeText(getContext(), "Name must contain only English letters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!surname.matches("[a-zA-Z ]+")) {
            Toast.makeText(getContext(), "Surname must contain only English letters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(getContext(), "Invalid email format", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!pass.equals(confirmPass)) {
            Toast.makeText(getContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        Date birthdate;
        try {
            birthdate = dateFormat.parse(birthdayStr);
            if (birthdate.after(new Date())) {
                Toast.makeText(getContext(), "Birthday cannot be in the future", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (ParseException e) {
            Toast.makeText(getContext(), "Invalid birthday format", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("users").document(email).get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot.exists()) {
                        Toast.makeText(getContext(), "Email already in use", Toast.LENGTH_SHORT).show();
                        return; // STOP — do NOT update database
                    }

                    // ---------------- HASH PASSWORD ----------------
                    String hashedPassword = sha256(pass);

                    // ---------------- CREATE USER ----------------
                    User user = new User(email, name, surname, hashedPassword, birthdate);

                    // ---------------- SAVE USER ----------------
                    db.collection("users").document(email)
                            .set(user)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Account created!", Toast.LENGTH_SHORT).show();

                                ((MainActivity) requireActivity()).getSupportFragmentManager()
                                        .beginTransaction()
                                        .replace(R.id.flFragment, new LoginFragment())
                                        .commit();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                            );
                });


//        // ---------- HASH PASSWORD ----------
//        String hashedPassword = sha256(pass);
//
//        // ---------- CREATE USER OBJECT ----------
//        User user = new User( email, name, surname, hashedPassword, birthdate);
//
//        // ---------- SAVE TO FIRESTORE ----------
//        db.collection("users")
//                .document(email)   // email = primary key
//                .set(user)
//                .addOnSuccessListener(aVoid -> {
//                    Toast.makeText(getContext(), "Account created!", Toast.LENGTH_SHORT).show();
//
//                    // Go to login screen
//                    ((MainActivity) requireActivity()).getSupportFragmentManager()
//                            .beginTransaction()
//                            .replace(R.id.flFragment, new LoginFragment())
//                            .commit();
//
//                })
//                .addOnFailureListener(e ->
//                        Toast.makeText(getContext(), "Error saving account: " + e.getMessage(), Toast.LENGTH_LONG).show()
//                );
    }

    // ---------- SHA-256 HASH ----------
    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return input; // fallback
        }
    }
}
