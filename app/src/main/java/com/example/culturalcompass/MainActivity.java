package com.example.culturalcompass;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.culturalcompass.ui.login.LoginFragment;
import com.example.culturalcompass.ui.map.MapFragment;
import com.example.culturalcompass.ui.favorites.FavoritesFragment;
import com.example.culturalcompass.ui.assistant.AIAssistantFragment;
import com.example.culturalcompass.ui.register.RegisterFragment;
import com.example.culturalcompass.ui.settings.SettingsFragment;
import com.example.culturalcompass.ui.splash.SplashFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private boolean forceHide = true;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView textGreeting;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        View headerView = findViewById(R.id.header);
        textGreeting = headerView.findViewById(R.id.textGreeting);

        db = FirebaseFirestore.getInstance();


        //-------------Greeting with user name-----------

        loadUserNameFromFirestore();


        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        // ---------- SETTINGS BUTTON CLICK ----------
        ImageView settingsButton = findViewById(R.id.iconSettings);
        if (settingsButton != null) {
            settingsButton.setOnClickListener(v -> {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.flFragment, new SettingsFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        // Start with SPLASH
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.flFragment, new SplashFragment())
                    .commit();
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int id = item.getItemId();
            if (id == R.id.nav_map) {
                selectedFragment = new MapFragment();
            } else if (id == R.id.nav_favorites) {
                selectedFragment = new FavoritesFragment();
            } else if (id == R.id.nav_assistant) {
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


    private void loadUserNameFromFirestore() {
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
                            name = "User";
                        }

                        textGreeting.setText("Hello, " + name);
                    }
                })
                .addOnFailureListener(e -> textGreeting.setText("Hello"));
    }

    // --- UI Visibility Control -----------------------------------------------

    public boolean isSplashActive() {
        return forceHide;  // splash is active when forceHide = true
    }

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

        //  Add delay before showing header + bottom nav
        handler.postDelayed(() -> showChrome(), 200);

    }

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
        // show bottom nav again
        exitSplash();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.flFragment, new MapFragment())
                .commit();
    }

}
