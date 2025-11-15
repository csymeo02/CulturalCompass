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
import androidx.navigation.fragment.NavHostFragment;
//import androidx.navigation.fragment.NavHostFragment;

import com.example.culturalcompass.R;

public class SplashFragment extends Fragment {

    private ProgressBar progressBar;
    private volatile boolean initFinished = false;
    private volatile boolean reached95 = false;
    private final Handler main = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_splash, container, false);
        progressBar = v.findViewById(R.id.progressBar);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Start animation 0 -> 95% in ~2s
        animateProgressTo(95, 2000, () -> {
            reached95 = true;
            tryFinish();
        });

        // Start real initialization in background
        new Thread(() -> {
            doRealInitialization();          // replace with real tasks when ready
            initFinished = true;
            main.post(this::tryFinish);
        }).start();
    }

    private void animateProgressTo(int target, long durationMs, Runnable endAction) {
        int start = progressBar.getProgress();
        ValueAnimator animator = ValueAnimator.ofInt(start, target);
        animator.setDuration(durationMs);
        animator.addUpdateListener(a ->
                progressBar.setProgress((Integer) a.getAnimatedValue()));
        if (endAction != null) animator.addListener(new SimpleAnimatorEnd(endAction));
        animator.start();
    }

    private void tryFinish() {
        if (!isAdded()) return;
        if (reached95 && initFinished) {
            // 95 -> 100 in 300ms, hold briefly, then navigate
            animateProgressTo(100, 300, () -> main.postDelayed(this::goNext, 150));
        }
    }

    private void goNext() {
        if (!isAdded()) return;
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_splash_to_home);
    }

    // TODO: replace with real startup work (Firebase init, read prefs, remote config, etc.)
    private void doRealInitialization() {
        try {
            Thread.sleep(1200); // simulate variable work; remove when real tasks added
        } catch (InterruptedException ignored) {}
    }

    // Small helper so we don’t implement all animator callbacks
    private static class SimpleAnimatorEnd implements android.animation.Animator.AnimatorListener {
        private final Runnable onEnd;
        SimpleAnimatorEnd(Runnable onEnd) { this.onEnd = onEnd; }
        public void onAnimationStart(android.animation.Animator animation) {}
        public void onAnimationCancel(android.animation.Animator animation) {}
        public void onAnimationRepeat(android.animation.Animator animation) {}
        public void onAnimationEnd(android.animation.Animator animation) { if (onEnd != null) onEnd.run(); }
    }
}
