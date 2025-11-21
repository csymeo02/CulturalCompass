package com.example.culturalcompass.ui.profile;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.culturalcompass.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.*;
import java.util.*;

public class UserEditInfoFragment extends Fragment {

    private EditText etName, etSurname, etBirthday, etPassword, etConfirm;
    private Button btnApply;

    private FirebaseFirestore db;
    private SimpleDateFormat df;
    private DatePickerDialog datePicker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        df = new SimpleDateFormat("dd-MM-yyyy");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_user_edit_info, container, false);

        etName = v.findViewById(R.id.etName);
        etSurname = v.findViewById(R.id.etSurname);
        etBirthday = v.findViewById(R.id.etBirthday);
        etPassword = v.findViewById(R.id.etPassword);
        etConfirm = v.findViewById(R.id.etConfirmPassword);
        btnApply = v.findViewById(R.id.btnApply);

        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        // LOAD EXISTING DATA
        db.collection("users").document(email).get()
                .addOnSuccessListener(doc -> {
                    etName.setText(doc.getString("name"));
                    etSurname.setText(doc.getString("surname"));

                    Date b = doc.getDate("birthdate");
                    if (b != null) etBirthday.setText(df.format(b));
                });

        // DATE PICKER
        Calendar cal = Calendar.getInstance();
        datePicker = new DatePickerDialog(requireContext(),
                (view, y, m, d) -> {
                    Calendar c = Calendar.getInstance();
                    c.set(y, m, d);
                    etBirthday.setText(df.format(c.getTime()));
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        Button btnCancel = v.findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(x -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .popBackStack();
        });


        etBirthday.setOnClickListener(x -> datePicker.show());

        btnApply.setOnClickListener(x -> saveChanges());

        return v;
    }

    private void saveChanges() {

        String name = etName.getText().toString().trim();
        String surname = etSurname.getText().toString().trim();
        String birthdayStr = etBirthday.getText().toString().trim();
        String pass = etPassword.getText().toString();
        String confirm = etConfirm.getText().toString();

        // VALIDATION SAME AS REGISTER
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(surname) || TextUtils.isEmpty(birthdayStr)) {
            Toast.makeText(getContext(), "All fields except password are required", Toast.LENGTH_SHORT).show();
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

        // Birthday
        Date birthdate;
        try {
            birthdate = df.parse(birthdayStr);
            if (birthdate.after(new Date())) {
                Toast.makeText(getContext(), "Birthday cannot be in the future", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (ParseException e) {
            Toast.makeText(getContext(), "Invalid birthday format", Toast.LENGTH_SHORT).show();
            return;
        }


        // PASSWORD VALIDATION
        boolean changePassword = !pass.isEmpty() || !confirm.isEmpty();

        if (changePassword) {
            if (!pass.equals(confirm) ) {
                Toast.makeText(getContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (pass.length() < 6 ) {
                Toast.makeText(getContext(), "Passwords length less than 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // FIRESTORE UPDATE
        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        Map<String,Object> update = new HashMap<>();
        update.put("name", name);
        update.put("surname", surname);
        update.put("birthdate", birthdate);

        db.collection("users").document(email).update(update)
                .addOnSuccessListener(a -> {

                    if (changePassword) {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        user.updatePassword(pass)
                                .addOnSuccessListener(v -> {
                                    Toast.makeText(getContext(), "Info updated successfully", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(getContext(), "Info updated successfully", Toast.LENGTH_SHORT).show();
                    }

                    // RETURN TO VIEW INFO
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.flFragment, new UserViewInfoFragment())
                            .commit();
                });
    }
}