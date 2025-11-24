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

import com.example.culturalcompass.MainActivity;
import com.example.culturalcompass.R;
import com.example.culturalcompass.model.User;
import com.example.culturalcompass.ui.login.LoginFragment;
import com.google.firebase.auth.FirebaseAuth;                // ← ADDED
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class RegisterFragment extends Fragment {

    private EditText etName, etSurname, etEmail, etPassword, etConfirmPassword, etBirthday;
    private Button btnRegister;
    private TextView tvGoLogin;

    private FirebaseAuth auth;                               // ← ADDED
    private FirebaseFirestore db;

    private DatePickerDialog datePickerDialog;
    private SimpleDateFormat dateFormat;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();                   // ← ADDED (Firebase Authentication)
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

        // ---------- GO TO LOGIN ----------
        tvGoLogin.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.flFragment, new LoginFragment())
                    .commit();
        });

        // ---------- REGISTER ----------
        btnRegister.setOnClickListener(v -> attemptRegister());

        return view;
    }

    private void initDatePicker() {
        Calendar cal = Calendar.getInstance();

//        datePickerDialog = new DatePickerDialog(
//                requireContext(),
//                (view, year, month, dayOfMonth) -> {
//                    Calendar selected = Calendar.getInstance();
//                    selected.set(year, month, dayOfMonth);
//                    String formatted = dateFormat.format(selected.getTime());
//                    etBirthday.setText(formatted);
//                },
//                cal.get(Calendar.YEAR),
//                cal.get(Calendar.MONTH),
//                cal.get(Calendar.DAY_OF_MONTH)
//        );
        datePickerDialog = new DatePickerDialog(
                requireContext(),
                R.style.CustomDatePicker,   // <--- Added for the color
                (view, y, m, d) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(y, m, d);
                    etBirthday.setText(dateFormat.format(selected.getTime()));
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

        // --------------------------------------------------------------------
        // CHANGE HERE — Use FirebaseAuth for REAL signup
        // --------------------------------------------------------------------
        auth.createUserWithEmailAndPassword(email, pass)        // ← ADDED
                .addOnSuccessListener(result -> {

                    // SAVE USER PROFILE TO FIRESTORE
                    User user = new User(email, name, surname, birthdate);   // ← UPDATED (no password)

                    db.collection("users")
                            .document(email)
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

                })
                .addOnFailureListener(e -> {
                    // Handle Firebase errors properly
                    Toast.makeText(getContext(), "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}

