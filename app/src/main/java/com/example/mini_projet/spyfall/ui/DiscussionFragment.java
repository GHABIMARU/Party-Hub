package com.example.mini_projet.spyfall.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import com.example.mini_projet.shared.model.Message;

public class DiscussionFragment extends Fragment {

    private final GameEngine engine = GameEngine.getInstance();
    private CountDownTimer timer;
    private TextView tvTimer;
    private View     timerContainer;

    public DiscussionFragment() { super(R.layout.fragment_discussion); }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view == null) return null;

        tvTimer        = view.findViewById(R.id.tv_timer);
        timerContainer = view.findViewById(R.id.tv_timer).getParent() instanceof View
                ? (View) view.findViewById(R.id.tv_timer).getParent() : null;
        Button btnGoVote = view.findViewById(R.id.btn_go_to_vote);

        // ── Entrance animations ────────────────────────────────────────────
        view.setAlpha(0f);
        view.animate().alpha(1f).setDuration(300).start();

        tvTimer.setScaleX(0f);
        tvTimer.setScaleY(0f);
        tvTimer.animate().scaleX(1f).scaleY(1f)
                .setStartDelay(350)
                .setDuration(500)
                .setInterpolator(new OvershootInterpolator(1.8f))
                .start();

        btnGoVote.setAlpha(0f);
        btnGoVote.setTranslationY(30f);
        btnGoVote.animate().alpha(1f).translationY(0f)
                .setStartDelay(500).setDuration(350)
                .setInterpolator(new DecelerateInterpolator()).start();

        if (engine.getMode() == GameEngine.Mode.CLIENT) {
            btnGoVote.setVisibility(View.GONE);
        } else {
            btnGoVote.setOnClickListener(v -> {
                btnGoVote.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80)
                        .withEndAction(() -> { cancelTimer(); goToVoting(); }).start();
            });
        }

        startTimer();
        return view;
    }

    private void startTimer() {
        timer = new CountDownTimer(60_000, 1000) {
            @Override public void onTick(long ms) {
                if (tvTimer == null) return;
                long secs = ms / 1000;
                tvTimer.setText(String.valueOf(secs));

                // Color transition: gold → red in last 10s
                int color = secs <= 10 ? 0xFFE53E3E : 0xFFF0B429;
                tvTimer.setTextColor(color);

                // Pulse scale every second
                tvTimer.animate().scaleX(1.08f).scaleY(1.08f).setDuration(120)
                        .withEndAction(() ->
                                tvTimer.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
                        ).start();

                // Urgent shake in last 5 seconds
                if (secs <= 5) {
                    ObjectAnimator shake = ObjectAnimator.ofFloat(tvTimer, "translationX",
                            0, -6, 6, -4, 4, 0);
                    shake.setDuration(300);
                    shake.start();
                }
            }
            @Override public void onFinish() {
                if (tvTimer != null) { tvTimer.setText("0"); tvTimer.setTextColor(0xFFE53E3E); }
                if (engine.getMode() != GameEngine.Mode.CLIENT) goToVoting();
            }
        }.start();
    }

    private void goToVoting() {
        engine.goToVoting();
        if (engine.getMode() == GameEngine.Mode.HOST)
            engine.getHostServer().broadcast(new Message("STATE_CHANGE", null, GameState.VOTING.name()));
        if (getActivity() != null)
            ((MainActivity) requireActivity()).navigateTo(GameState.VOTING);
    }

    private void cancelTimer() { if (timer != null) { timer.cancel(); timer = null; } }

    @Override
    public void onDestroyView() { cancelTimer(); super.onDestroyView(); }
}