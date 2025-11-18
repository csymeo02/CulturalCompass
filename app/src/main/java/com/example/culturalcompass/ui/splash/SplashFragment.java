package com.example.culturalcompass.ui.splash;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.culturalcompass.MainActivity;
import com.example.culturalcompass.R;
import com.example.culturalcompass.ui.login.LoginFragment;
import com.example.culturalcompass.ui.map.MapFragment;

public class SplashFragment extends Fragment {

    private ProgressBar progressBar;

    private boolean animationDone = false;
    private boolean initDone = false;
    private boolean navigated = false;

    private ValueAnimator animator;
    private final Handler main = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_splash, container, false);
        progressBar = v.findViewById(R.id.progressBar);
        progressBar.setProgress(0);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Hide UI for full splash
        ((MainActivity) requireActivity()).hideChrome();

        // Animate 0 → 100 instantly when finished
        animateProgress();

        // Background init
        new Thread(() -> {
            doInit();
            initDone = true;
            main.post(this::tryExit);
        }).start();
    }

    private void animateProgress() {
        animator = ValueAnimator.ofInt(0, 100);
        animator.setDuration(2000);
        animator.addUpdateListener(a -> {
            if (!isAdded()) return;
            progressBar.setProgress((Integer) a.getAnimatedValue());
        });

        animator.addListener(new SimpleAnimatorEnd(() -> {
            animationDone = true;
            tryExit();
        }));

        animator.start();
    }

    private void tryExit() {
        if (!isAdded() || navigated) return;

        if (animationDone && initDone) {
            navigated = true;

            if (animator != null && animator.isRunning()) {
                animator.cancel();
            }

            goNext();
        }
    }

    private void goNext() {
        if (!isAdded()) return;

        MainActivity act = (MainActivity) requireActivity();
        act.exitSplash();


        act.getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(0, 0)
                .replace(R.id.flFragment, new LoginFragment())
                .commitAllowingStateLoss();
    }

    private void doInit() {
        try { Thread.sleep(800); } catch (InterruptedException ignored) {}
    }

    private static class SimpleAnimatorEnd implements android.animation.Animator.AnimatorListener {
        private final Runnable onEnd;
        SimpleAnimatorEnd(Runnable onEnd) { this.onEnd = onEnd; }
        public void onAnimationStart(android.animation.Animator animation) {}
        public void onAnimationCancel(android.animation.Animator animation) {}
        public void onAnimationRepeat(android.animation.Animator animation) {}
        public void onAnimationEnd(android.animation.Animator animation) { if (onEnd != null) onEnd.run(); }
    }
}
