package com.example.culturalcompass;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.culturalcompass.model.Session;
import com.example.culturalcompass.ui.assistant.AIAssistantFragment;
import com.example.culturalcompass.ui.favorites.FavoritesFragment;
import com.example.culturalcompass.ui.map.MapFragment;
import com.example.culturalcompass.ui.settings.SettingsFragment;
import com.example.culturalcompass.ui.splash.SplashFragment;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    private boolean forceHide = true; // hide UI during splash/login
    private final Handler handler = new Handler(Looper.getMainLooper());

    private int currentTabId = R.id.nav_map;  // avoid reloading same tab
    public static PlacesClient placesClient;  // shared Places client

    private TextView textGreeting;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // init Google Places once
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(
                    getApplicationContext(),
                    getString(R.string.google_maps_key)
            );
        }
        placesClient = Places.createClient(getApplicationContext());

        db = FirebaseFirestore.getInstance();

        // header + greeting text
        View headerView = findViewById(R.id.header);
        textGreeting = headerView.findViewById(R.id.textGreeting);

        // load user's greeting if logged in
        updateGreetingFromAuth();

        // settings icon on header
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

        // show splash on app start
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.flFragment, new SplashFragment())
                    .commit();
        }

        // bottom bar navigation
        bottomNavigationView.setOnItemSelectedListener(item -> {

            if (item.getItemId() == currentTabId) return false; // ignore same tab
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

        // request FCM push token (just logs it)
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    Log.d(TAG, "FCM Registration token: " + token);
                });
    }

    // load greeting text from Firestore profile
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

                        // store globally for AI assistant
                        Session.currentUsername = name;
                    }
                })
                .addOnFailureListener(e -> textGreeting.setText("Hello"));
    }

    // called after login to refresh header
    public void updateGreeting() {
        updateGreetingFromAuth();
    }

    // hide header + bottom nav (used in login/splash)
    public void hideChrome() {
        forceHide = true;
        findViewById(R.id.header).setVisibility(View.GONE);
        findViewById(R.id.bottomNavigationView).setVisibility(View.GONE);
    }

    // show header + bottom nav again
    public void showChrome() {
        if (!forceHide) {
            findViewById(R.id.header).setVisibility(View.VISIBLE);
            findViewById(R.id.bottomNavigationView).setVisibility(View.VISIBLE);
        }
    }

    // leave splash screen with a small delay
    public void exitSplash() {
        forceHide = false;
        handler.postDelayed(this::showChrome, 200);
    }

    // navigate to map screen (home)
    public void navigateToHome() {
        exitSplash();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.flFragment, new MapFragment())
                .commit();

        updateGreeting(); // refresh greeting text
    }
}
