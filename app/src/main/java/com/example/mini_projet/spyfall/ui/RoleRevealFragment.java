package com.example.mini_projet.spyfall.ui;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
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

    private RevealState state       = RevealState.INTRO;
    private boolean     hasRevealed = false;
    private String      currentRole;
    private String      currentPlayerName;

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

        // ── Entrance: player name pops in ─────────────────────────────────
        tvPlayerName.setAlpha(0f);
        tvPlayerName.setScaleX(0.7f);
        tvPlayerName.setScaleY(0.7f);
        tvPlayerName.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setStartDelay(200)
                .setDuration(400)
                .setInterpolator(new OvershootInterpolator(1.5f))
                .start();

        // ── Touch: hold to reveal ─────────────────────────────────────────
        touchRoot.setOnTouchListener((v, event) -> {
            if (isTouchOnView(event, nextBtn) && nextBtn.getVisibility() == View.VISIBLE)
                return false;
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: showReveal(); return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: showIntro(); return true;
            }
            return false;
        });

        nextBtn.setOnClickListener(v -> {
            nextBtn.animate().scaleX(0.93f).scaleY(0.93f).setDuration(70)
                    .withEndAction(() -> {
                        nextBtn.animate().scaleX(1f).scaleY(1f)
                                .setDuration(120)
                                .setInterpolator(new OvershootInterpolator(2f)).start();
                        handleContinue();
                    }).start();
        });

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
                if (p.getId() == myId) { currentPlayerName = p.getName().toUpperCase(); break; }
            }
            String role = engine.getMyRole();
            currentRole = (role != null) ? role : "ERROR";
        }
    }

    private void applyRoleToRevealLayout() {
        if ("IMPOSTOR".equals(currentRole)) {
            roleText.setText(getString(R.string.reveal_impostor_text));
            roleText.setTextColor(0xFFE53E3E);
        } else if ("ERROR".equals(currentRole)) {
            roleText.setText(getString(R.string.reveal_error));
            roleText.setTextColor(0xFF8A9BC4);
        } else {
            roleText.setText("🌍\n" + currentRole);
            roleText.setTextColor(0xFF38B2AC);
        }
    }

    private void showReveal() {
        if (state == RevealState.REVEAL) return;
        state = RevealState.REVEAL;

        // Crossfade intro → reveal
        layoutIntro.animate().alpha(0f).setDuration(150)
                .withEndAction(() -> layoutIntro.setVisibility(View.INVISIBLE)).start();

        layoutReveal.setAlpha(0f);
        layoutReveal.setVisibility(View.VISIBLE);
        layoutReveal.setScaleX(0.9f);
        layoutReveal.setScaleY(0.9f);
        layoutReveal.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(200)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();

        nextBtn.setVisibility(View.GONE);

        // Pulse the role text color
        if ("IMPOSTOR".equals(currentRole)) {
            pulseBackground(touchRoot, 0xFF0A0E1A, 0x221E0505);
        } else {
            pulseBackground(touchRoot, 0xFF0A0E1A, 0x22001A19);
        }
    }

    private void showIntro() {
        if (state == RevealState.INTRO) return;
        state = RevealState.INTRO;
        hasRevealed = true;

        layoutReveal.animate().alpha(0f).setDuration(150)
                .withEndAction(() -> layoutReveal.setVisibility(View.INVISIBLE)).start();

        layoutIntro.setAlpha(0f);
        layoutIntro.setVisibility(View.VISIBLE);
        layoutIntro.animate().alpha(1f).setDuration(200).start();

        // Reset background
        touchRoot.setBackgroundColor(0xFF0A0E1A);

        if (hasRevealed && tvWaiting.getVisibility() != View.VISIBLE) {
            nextBtn.setVisibility(View.VISIBLE);
            nextBtn.setAlpha(0f);
            nextBtn.setTranslationY(dp(20));
            nextBtn.animate().alpha(1f).translationY(0f)
                    .setDuration(300)
                    .setInterpolator(new OvershootInterpolator(1.5f))
                    .start();
        }
    }

    private void applyIntroState() {
        state = RevealState.INTRO;
        layoutIntro.setVisibility(View.VISIBLE);
        layoutReveal.setVisibility(View.INVISIBLE);
        nextBtn.setVisibility(View.GONE);
    }

    private void pulseBackground(View v, int from, int to) {
        ValueAnimator anim = ValueAnimator.ofObject(new ArgbEvaluator(), from, to);
        anim.setDuration(300);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.setRepeatMode(ValueAnimator.REVERSE);
        anim.addUpdateListener(a -> v.setBackgroundColor((int) a.getAnimatedValue()));
        anim.start();
        v.setTag(anim);
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
        float x = event.getRawX(), y = event.getRawY();
        return x >= loc[0] && x <= loc[0] + target.getWidth()
                && y >= loc[1] && y <= loc[1] + target.getHeight();
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    public void refreshRole() {
        resolveCurrentPlayer();
        applyRoleToRevealLayout();
    }
}