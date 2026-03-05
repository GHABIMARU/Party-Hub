package com.example.mini_projet.spyfall.ui;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    public DiscussionFragment() { super(R.layout.fragment_discussion); }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view == null) return null;

        tvTimer          = view.findViewById(R.id.tv_timer);
        Button btnGoVote = view.findViewById(R.id.btn_go_to_vote);

        if (engine.getMode() == GameEngine.Mode.CLIENT) {
            btnGoVote.setVisibility(View.GONE);
        } else {
            btnGoVote.setOnClickListener(v -> { cancelTimer(); goToVoting(); });
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
                tvTimer.setTextColor(secs <= 10 ? 0xFFE53E3E : 0xFFF0B429);
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