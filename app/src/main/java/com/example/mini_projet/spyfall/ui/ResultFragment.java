package com.example.mini_projet.spyfall.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mini_projet.R;
import com.example.mini_projet.spyfall.game.GameEngine;
import com.example.mini_projet.spyfall.game.GameState;

public class ResultFragment extends Fragment {

    private final GameEngine engine = GameEngine.getInstance();

    public ResultFragment() { super(R.layout.fragment_result); }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view == null) return null;

        TextView tvResultEmoji = view.findViewById(R.id.tv_result_emoji);
        TextView tvResult      = view.findViewById(R.id.tv_result);
        Button   btnRestart    = view.findViewById(R.id.btn_restart);
        Button   btnMainMenu   = view.findViewById(R.id.btn_main_menu);
        View     accentBar     = view.findViewById(R.id.result_accent_bar);

        String result = engine.getResult();
        if (result == null) result = "Game Over";

        // Determine outcome colors
        final int accentColor;
        if (result.startsWith("TIE:")) {
            tvResultEmoji.setText("🤝");
            tvResult.setTextColor(0xFFF0B429);
            accentColor = 0xFFF0B429;
        } else if (result.contains("Crewmates")) {
            tvResultEmoji.setText("🎉");
            tvResult.setTextColor(0xFF38B2AC);
            accentColor = 0xFF38B2AC;
        } else if (result.contains("Impostor") || result.contains("win")) {
            tvResultEmoji.setText("😈");
            tvResult.setTextColor(0xFFE53E3E);
            accentColor = 0xFFE53E3E;
        } else {
            tvResultEmoji.setText("🤷");
            tvResult.setTextColor(0xFFF0F4FF);
            accentColor = 0xFF8A9BC4;
        }
        tvResult.setText(result);
        accentBar.setBackgroundColor(accentColor);

        // ── Emoji: delayed drop-in with bounce ────────────────────────────
        tvResultEmoji.setAlpha(0f);
        tvResultEmoji.setTranslationY(-dp(60));
        tvResultEmoji.setScaleX(0.5f);
        tvResultEmoji.setScaleY(0.5f);
        tvResultEmoji.animate()
                .alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                .setStartDelay(100)
                .setDuration(550)
                .setInterpolator(new OvershootInterpolator(2f))
                .start();

        // ── Result text: fade up ──────────────────────────────────────────
        tvResult.setAlpha(0f);
        tvResult.setTranslationY(dp(20));
        tvResult.animate()
                .alpha(1f).translationY(0f)
                .setStartDelay(400)
                .setDuration(400)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // ── Buttons: stagger slide up ─────────────────────────────────────
        btnRestart.setAlpha(0f);
        btnRestart.setTranslationY(dp(30));
        btnRestart.animate()
                .alpha(1f).translationY(0f)
                .setStartDelay(650)
                .setDuration(350)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();

        btnMainMenu.setAlpha(0f);
        btnMainMenu.setTranslationY(dp(30));
        btnMainMenu.animate()
                .alpha(1f).translationY(0f)
                .setStartDelay(750)
                .setDuration(350)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();

        // ── Emoji pulse loop (subtle, after entrance) ─────────────────────
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            ValueAnimator pulse = ValueAnimator.ofFloat(1f, 1.1f, 1f);
            pulse.setDuration(1800);
            pulse.setRepeatCount(ValueAnimator.INFINITE);
            pulse.addUpdateListener(a -> {
                float s = (float) a.getAnimatedValue();
                tvResultEmoji.setScaleX(s);
                tvResultEmoji.setScaleY(s);
            });
            pulse.start();
        }, 800);

        // ── Buttons ───────────────────────────────────────────────────────
        btnRestart.setOnClickListener(v -> {
            btnRestart.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80)
                    .withEndAction(() -> {
                        btnRestart.animate().scaleX(1f).scaleY(1f)
                                .setDuration(100).setInterpolator(new OvershootInterpolator(2f)).start();
                        engine.restartGame();
                        ((MainActivity) requireActivity()).navigateTo(GameState.LOBBY);
                    }).start();
        });

        btnMainMenu.setOnClickListener(v -> {
            btnMainMenu.animate().alpha(0.5f).setDuration(150)
                    .withEndAction(() ->
                            ((MainActivity) requireActivity()).navigateToHome()
                    ).start();
        });

        return view;
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}