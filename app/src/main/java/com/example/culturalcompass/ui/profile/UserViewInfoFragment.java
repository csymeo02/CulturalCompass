package com.example.culturalcompass.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.culturalcompass.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;

public class UserViewInfoFragment extends Fragment {

    private TextView tvEmail, tvName, tvSurname, tvBirthday;
    private Button btnEdit;
    private FirebaseFirestore db;
    private SimpleDateFormat df;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        df = new SimpleDateFormat("dd-MM-yyyy");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_user_view_info, container, false);

        tvEmail = v.findViewById(R.id.textEmail);
        tvName = v.findViewById(R.id.textName);
        tvSurname = v.findViewById(R.id.textSurname);
        tvBirthday = v.findViewById(R.id.textBirthdate);
        btnEdit = v.findViewById(R.id.buttonEdit);

        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        db.collection("users").document(email).get().addOnSuccessListener(doc -> {

            tvEmail.setText("Email: " + email);
            tvName.setText("Name: " + doc.getString("name"));
            tvSurname.setText("Surname: " + doc.getString("surname"));

            Date b = doc.getDate("birthdate");
            if (b != null) tvBirthday.setText("Birthday: " + df.format(b));
        });

        btnEdit.setOnClickListener(v1 -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.flFragment, new UserEditInfoFragment())
                    .addToBackStack(null)
                    .commit();
        });

        return v;
    }
}
