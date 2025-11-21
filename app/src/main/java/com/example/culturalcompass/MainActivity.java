package com.example.culturalcompass;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.culturalcompass.model.Session;
import com.example.culturalcompass.ui.assistant.AIAssistantFragment;
import com.example.culturalcompass.ui.favorites.FavoritesFragment;
import com.example.culturalcompass.ui.login.LoginFragment;
import com.example.culturalcompass.ui.map.MapFragment;
import com.example.culturalcompass.ui.register.RegisterFragment;
import com.example.culturalcompass.ui.settings.SettingsFragment;
import com.example.culturalcompass.ui.splash.SplashFragment;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private boolean forceHide = true;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private int currentTabId = R.id.nav_map;  // Disable reloading same tab
    public static PlacesClient placesClient;

    private TextView textGreeting;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ---------------------- PLACES API INIT ----------------------
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(
                    getApplicationContext(),
                    getString(R.string.google_maps_key)
            );
        }
        placesClient = Places.createClient(getApplicationContext());
        // --------------------------------------------------------------

        db = FirebaseFirestore.getInstance();

        // Header binding
        View headerView = findViewById(R.id.header);
        textGreeting = headerView.findViewById(R.id.textGreeting);

        // Load greeting text
        updateGreetingFromAuth();

        // SETTINGS BUTTON HANDLER
        ImageView settingsButton = findViewById(R.id.iconSettings);
        if (settingsButton != null) {
            settingsButton.setOnClickListener(v ->
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.flFragment, new SettingsFragment())
                            .addToBackStack(null)
                            .commit()
            );
        }

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Start at Splash
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.flFragment, new SplashFragment())
                    .commit();
        }

        // Bottom navigation
        bottomNavigationView.setOnItemSelectedListener(item -> {

            // prevent reloading same tab
            if (item.getItemId() == currentTabId) return false;
            currentTabId = item.getItemId();

            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.nav_map) {
                selectedFragment = new MapFragment();
            } else if (item.getItemId() == R.id.nav_favorites) {
                selectedFragment = new FavoritesFragment();
            } else if (item.getItemId() == R.id.nav_assistant) {
                selectedFragment = new AIAssistantFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.flFragment, selectedFragment)
                        .commit();
            }
            return true;
        });
    }

    // ------------------------------------------------------------------------
    // LOAD GREETING USING FIREBASE AUTH EMAIL + FIRESTORE PROFILE
    // ------------------------------------------------------------------------

    private void updateGreetingFromAuth() {
        String email = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getEmail() : null;

        if (email == null) {
            textGreeting.setText("Hello");
            return;
        }

        db.collection("users")
                .document(email)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        if (name == null || name.isEmpty()) {
                            textGreeting.setText("Hello");
                        } else {
                            textGreeting.setText("Hello, " + name);
                        }
                    }
                })
                .addOnFailureListener(e -> textGreeting.setText("Hello"));
    }

    // Called after login finished → refresh greeting
    public void updateGreeting() {
        updateGreetingFromAuth();
    }

    // ------------------------------------------------------------------------
    // UI Visibility Control — used by Splash/Login
    // ------------------------------------------------------------------------

    public void hideChrome() {
        forceHide = true;
        findViewById(R.id.header).setVisibility(View.GONE);
        findViewById(R.id.bottomNavigationView).setVisibility(View.GONE);
    }

    public void showChrome() {
        if (!forceHide) {
            findViewById(R.id.header).setVisibility(View.VISIBLE);
            findViewById(R.id.bottomNavigationView).setVisibility(View.VISIBLE);
        }
    }

    public void exitSplash() {
        forceHide = false;
        handler.postDelayed(this::showChrome, 200);
    }

    // ------------------------------------------------------------------------

    public void navigateToLogin() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.flFragment, new LoginFragment())
                .commit();
    }

    public void navigateToRegister() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.flFragment, new RegisterFragment())
                .addToBackStack(null)
                .commit();
    }

    public void navigateToHome() {
        exitSplash();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.flFragment, new MapFragment())
                .commit();

        updateGreeting(); // refresh text
    }
}
