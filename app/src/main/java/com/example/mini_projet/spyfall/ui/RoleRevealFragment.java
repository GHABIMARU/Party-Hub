package com.example.mini_projet.spyfall.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mini_projet.R;
import com.example.mini_projet.spyfall.game.GameEngine;
import com.example.mini_projet.shared.model.Message;
import com.example.mini_projet.shared.model.Player;

public class RoleRevealFragment extends Fragment {

    private enum RevealState { INTRO, REVEAL }

    private final GameEngine engine = GameEngine.getInstance();

    private View     touchRoot;
    private View     layoutIntro;
    private View     layoutReveal;
    private TextView tvPlayerName;
    private TextView roleText;
    private Button   nextBtn;
    private TextView tvWaiting;

    private RevealState state        = RevealState.INTRO;
    private boolean     hasRevealed  = false;

    private String currentRole;
    private String currentPlayerName;

    public RoleRevealFragment() { super(R.layout.fragment_role_reveal); }

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view == null) return null;

        touchRoot    = view.findViewById(R.id.touch_root);
        layoutIntro  = view.findViewById(R.id.layout_intro);
        layoutReveal = view.findViewById(R.id.layout_reveal);
        tvPlayerName = view.findViewById(R.id.tv_player_name);
        roleText     = view.findViewById(R.id.role_text);
        nextBtn      = view.findViewById(R.id.next_btn);
        tvWaiting    = view.findViewById(R.id.tv_waiting);

        resolveCurrentPlayer();

        tvPlayerName.setText(currentPlayerName);

        applyRoleToRevealLayout();

        touchRoot.setOnTouchListener((v, event) -> {
            if (isTouchOnView(event, nextBtn) && nextBtn.getVisibility() == View.VISIBLE) {
                return false;
            }

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    showReveal();
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    showIntro();
                    return true;
            }
            return false;
        });

         nextBtn.setOnClickListener(v -> handleContinue());

        if (engine.getMode() != GameEngine.Mode.PASS_PLAY) {

        }

        applyIntroState();
        return view;
    }


    private void resolveCurrentPlayer() {
        if (engine.getMode() == GameEngine.Mode.PASS_PLAY) {
            Player player = engine.getCurrentRevealPlayer();
            if (player != null) {
                currentPlayerName = player.getName().toUpperCase();
                currentRole = engine.isImpostor(player.getId()) ? "IMPOSTOR" : engine.getSecretWord();
            } else {
                currentPlayerName = "UNKNOWN";
                currentRole = "ERROR";
            }
        } else {
            int myId = engine.getMyPlayerId();
            currentPlayerName = "AGENT " + myId;
            for (Player p : engine.getPlayers()) {
                if (p.getId() == myId) {
                    currentPlayerName = p.getName().toUpperCase();
                    break;
                }
            }
            String role = engine.getMyRole();
            currentRole = (role != null) ? role : "ERROR";
        }
    }


    private void applyRoleToRevealLayout() {
        if ("IMPOSTOR".equals(currentRole)) {
            roleText.setText("🕵️\nYOU ARE\nTHE IMPOSTOR");
            roleText.setTextColor(0xFFE53E3E);
        } else if ("ERROR".equals(currentRole)) {
            roleText.setText("Error — please restart");
            roleText.setTextColor(0xFF8A9BC4);
        } else {
            roleText.setText("🌍\n" + currentRole);
            roleText.setTextColor(0xFF38B2AC);
        }
    }


    private void showReveal() {
        if (state == RevealState.REVEAL) return;
        state = RevealState.REVEAL;
        layoutIntro.setVisibility(View.INVISIBLE);
        layoutReveal.setVisibility(View.VISIBLE);
        nextBtn.setVisibility(View.GONE);
    }

    private void showIntro() {
        if (state == RevealState.INTRO) return;
        state = RevealState.INTRO;
        hasRevealed = true;
        layoutReveal.setVisibility(View.INVISIBLE);
        layoutIntro.setVisibility(View.VISIBLE);

        if (engine.getMode() == GameEngine.Mode.PASS_PLAY || hasRevealed) {
            if (tvWaiting.getVisibility() != View.VISIBLE) {
                nextBtn.setVisibility(View.VISIBLE);
            }
        }
    }

    private void applyIntroState() {
        state = RevealState.INTRO;
        layoutIntro.setVisibility(View.VISIBLE);
        layoutReveal.setVisibility(View.INVISIBLE);
        nextBtn.setVisibility(View.GONE);
    }


    private void handleContinue() {
        nextBtn.setEnabled(false);

        if (engine.getMode() == GameEngine.Mode.PASS_PLAY) {
            engine.advanceReveal();
            ((MainActivity) requireActivity()).navigateTo(engine.getGameState());

        } else {
            nextBtn.setVisibility(View.GONE);
            tvWaiting.setVisibility(View.VISIBLE);
            int myId = engine.getMyPlayerId();
            if (engine.getMode() == GameEngine.Mode.CLIENT) {
                engine.getClientConnection().send(
                        new Message("READY", String.valueOf(myId), "ready"));
            } else {
                engine.addReady(myId);
            }
        }
    }


    private boolean isTouchOnView(MotionEvent event, View target) {
        if (target == null || target.getVisibility() != View.VISIBLE) return false;
        int[] loc = new int[2];
        target.getLocationOnScreen(loc);
        float x = event.getRawX();
        float y = event.getRawY();
        return x >= loc[0] && x <= loc[0] + target.getWidth()
                && y >= loc[1] && y <= loc[1] + target.getHeight();
    }

    public void refreshRole() {
        resolveCurrentPlayer();
        applyRoleToRevealLayout();
    }
}