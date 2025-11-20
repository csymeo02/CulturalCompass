package com.example.culturalcompass;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.culturalcompass.model.Session;
import com.example.culturalcompass.ui.login.LoginFragment;
import com.example.culturalcompass.ui.map.MapFragment;
import com.example.culturalcompass.ui.favorites.FavoritesFragment;
import com.example.culturalcompass.ui.assistant.AIAssistantFragment;
import com.example.culturalcompass.ui.register.RegisterFragment;
import com.example.culturalcompass.ui.splash.SplashFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private boolean forceHide = true;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int currentTabId = R.id.nav_map;  // default after splash

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Start with SPLASH
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.flFragment, new SplashFragment())
                    .commit();
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {

            // --- block reloading same tab ---
            if (item.getItemId() == currentTabId) {
                return false;  // ignore the click
            }
            currentTabId = item.getItemId();
            // ---------------------------------

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
        exitSplash();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.flFragment, new MapFragment())
                .commit();

        updateGreeting();   // <— add this
    }


    public void updateGreeting() {
        TextView greeting = findViewById(R.id.textGreeting);

        if (Session.currentUser != null) {
            greeting.setText("Hello, " + Session.currentUser.getName());
        }
    }


}
