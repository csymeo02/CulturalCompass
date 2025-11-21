package com.example.culturalcompass.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.*;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.culturalcompass.MainActivity;
import com.example.culturalcompass.R;
import com.example.culturalcompass.ui.login.LoginFragment;
import com.example.culturalcompass.ui.profile.UserViewInfoFragment;   // ⭐ ADDED
import com.google.firebase.auth.FirebaseAuth;

public class SettingsFragment extends Fragment {

    private Switch switchNotifications, switchDarkMode, switchLocation, switchAutoSync;
    private Button btnLogout, btnPersonalInfo; // ⭐ ADDED

    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        prefs = requireContext().getSharedPreferences("settings", getContext().MODE_PRIVATE);

        switchNotifications = v.findViewById(R.id.switchNotifications);
        switchDarkMode = v.findViewById(R.id.switchDarkMode);
        switchLocation = v.findViewById(R.id.switchLocation);
        switchAutoSync = v.findViewById(R.id.switchAutoSync);
        btnLogout = v.findViewById(R.id.btnLogout);
        btnPersonalInfo = v.findViewById(R.id.btnPersonalInfo); // ⭐ ADDED

        // ---- NEW: Navigate to personal info ----
        btnPersonalInfo.setOnClickListener(x -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.flFragment, new UserViewInfoFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // ---- Load saved settings ----
        loadSettings();

        // ---- Save settings on toggle ----
        switchNotifications.setOnCheckedChangeListener((b, checked) -> save("notifications", checked));
        switchDarkMode.setOnCheckedChangeListener((b, checked) -> save("darkMode", checked));
        switchLocation.setOnCheckedChangeListener((b, checked) -> save("location", checked));
        switchAutoSync.setOnCheckedChangeListener((b, checked) -> save("autoSync", checked));

        // ---- Logout ----
        btnLogout.setOnClickListener(view -> logout());
    }

    private void loadSettings() {
        switchNotifications.setChecked(prefs.getBoolean("notifications", true));
        switchDarkMode.setChecked(prefs.getBoolean("darkMode", false));
        switchLocation.setChecked(prefs.getBoolean("location", true));
        switchAutoSync.setChecked(prefs.getBoolean("autoSync", true));
    }

    private void save(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
        Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show();
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();

        Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show();

        ((MainActivity) requireActivity()).getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.flFragment, new LoginFragment())
                .commit();
    }
}
//package com.example.culturalcompass.ui.settings;
//
//import android.content.SharedPreferences;
//import android.os.Bundle;
//import android.view.*;
//import android.widget.Button;
//import android.widget.Switch;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//
//import com.example.culturalcompass.MainActivity;
//import com.example.culturalcompass.R;
//import com.example.culturalcompass.ui.login.LoginFragment;
//import com.google.firebase.auth.FirebaseAuth;
//
//public class SettingsFragment extends Fragment {
//
//    private Switch switchNotifications, switchDarkMode, switchLocation, switchAutoSync;
//    private Button btnLogout;
//
//    private SharedPreferences prefs;
//
//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
//                             @Nullable Bundle savedInstanceState) {
//        return inflater.inflate(R.layout.fragment_settings, container, false);
//    }
//
//    @Override
//    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated(v, savedInstanceState);
//
//        prefs = requireContext().getSharedPreferences("settings", getContext().MODE_PRIVATE);
//
//        switchNotifications = v.findViewById(R.id.switchNotifications);
//        switchDarkMode = v.findViewById(R.id.switchDarkMode);
//        switchLocation = v.findViewById(R.id.switchLocation);
//        switchAutoSync = v.findViewById(R.id.switchAutoSync);
//        btnLogout = v.findViewById(R.id.btnLogout);
//
//        // ---- Load saved settings ----
//        loadSettings();
//
//        // ---- Save settings on toggle ----
//        switchNotifications.setOnCheckedChangeListener((b, checked) -> save("notifications", checked));
//        switchDarkMode.setOnCheckedChangeListener((b, checked) -> save("darkMode", checked));
//        switchLocation.setOnCheckedChangeListener((b, checked) -> save("location", checked));
//        switchAutoSync.setOnCheckedChangeListener((b, checked) -> save("autoSync", checked));
//
//        // ---- Logout ----
//        btnLogout.setOnClickListener(view -> logout());
//    }
//
//    private void loadSettings() {
//        switchNotifications.setChecked(prefs.getBoolean("notifications", true));
//        switchDarkMode.setChecked(prefs.getBoolean("darkMode", false));
//        switchLocation.setChecked(prefs.getBoolean("location", true));
//        switchAutoSync.setChecked(prefs.getBoolean("autoSync", true));
//    }
//
//    private void save(String key, boolean value) {
//        prefs.edit().putBoolean(key, value).apply();
//        Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show();
//    }
//
//    private void logout() {
//        FirebaseAuth.getInstance().signOut();
//
//        Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show();
//
//        ((MainActivity) requireActivity()).getSupportFragmentManager()
//                .beginTransaction()
//                .replace(R.id.flFragment, new LoginFragment())
//                .commit();
//    }
//}
