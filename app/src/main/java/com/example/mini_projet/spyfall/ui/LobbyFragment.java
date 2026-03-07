package com.example.mini_projet.spyfall.ui;

import android.animation.ObjectAnimator;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mini_projet.R;
import com.example.mini_projet.spyfall.game.GameEngine;
import com.example.mini_projet.spyfall.game.GameState;
import com.example.mini_projet.shared.model.Message;
import com.example.mini_projet.shared.network.ClientConnection;
import com.example.mini_projet.shared.network.HostServer;
import com.example.mini_projet.shared.model.Player;
import com.example.mini_projet.shared.utils.Utils;

public class LobbyFragment extends Fragment {

    private final GameEngine engine = GameEngine.getInstance();

    private LinearLayout playersView;
    private TextView     tvNoPlayers;
    private TextView     tvPlayerCount;
    private Handler      uiHandler;
    private Runnable     pollRunnable;
    private NumberPicker impostorPicker;
    private TextView     tvImpostorLabel;
    private LinearLayout impostorCard;

    private GameEngine.Mode selectedMode = GameEngine.Mode.PASS_PLAY;

    public LobbyFragment() { super(R.layout.fragment_lobby); }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view == null) return null;

        uiHandler = new Handler(Looper.getMainLooper());

        ScrollView scrollView = view.findViewById(R.id.lobby_scroll);
        LinearLayout root = (LinearLayout) scrollView.getChildAt(0);

        // ── MODE SELECTOR ─────────────────────────────────────────────────────
        RadioGroup modeGroup = new RadioGroup(requireContext());
        modeGroup.setOrientation(RadioGroup.HORIZONTAL);
        LinearLayout.LayoutParams mgParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        mgParams.bottomMargin = dp(16);
        modeGroup.setLayoutParams(mgParams);

        RadioButton rbPassPlay = makeRadioBtn("🃏  Pass & Play");
        RadioButton rbHost     = makeRadioBtn("📡  Host");
        RadioButton rbJoin     = makeRadioBtn("🔗  Join");
        modeGroup.addView(rbPassPlay);
        modeGroup.addView(rbHost);
        modeGroup.addView(rbJoin);
        root.addView(modeGroup, 1);

        // ── STATUS LABEL ──────────────────────────────────────────────────────
        TextView tvInfo = new TextView(requireContext());
        tvInfo.setTextSize(13f);
        tvInfo.setTextColor(0xFF8A9BC4);
        tvInfo.setLineSpacing(0, 1.5f);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        infoParams.bottomMargin = dp(12);
        tvInfo.setLayoutParams(infoParams);
        tvInfo.setVisibility(View.GONE);
        root.addView(tvInfo, 2);

        // ── IMPOSTOR CARD ─────────────────────────────────────────────────────
        impostorCard = buildImpostorCard();
        root.addView(impostorCard, 5);

        // ── WIRE XML VIEWS ────────────────────────────────────────────────────
        EditText nameInput   = view.findViewById(R.id.name_input);
        Button   addBtn      = view.findViewById(R.id.add_player_btn);
        playersView          = view.findViewById(R.id.players_view);
        tvNoPlayers          = view.findViewById(R.id.tv_no_players);
        tvPlayerCount        = view.findViewById(R.id.tv_player_count);
        Button   startBtn    = view.findViewById(R.id.start_game_btn);

        modeGroup.check(rbPassPlay.getId());

        // ── MODE CHANGE ───────────────────────────────────────────────────────
        modeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            tvInfo.setVisibility(View.GONE);
            if (checkedId == rbPassPlay.getId()) {
                selectedMode = GameEngine.Mode.PASS_PLAY;
                addBtn.setVisibility(View.VISIBLE);
                nameInput.setHint("Agent codename...");
                startBtn.setText("START MISSION");
                impostorCard.setVisibility(View.VISIBLE);
            } else if (checkedId == rbHost.getId()) {
                selectedMode = GameEngine.Mode.HOST;
                addBtn.setVisibility(View.GONE);
                nameInput.setHint("Your codename (host)");
                startBtn.setText("START HOSTING");
                impostorCard.setVisibility(View.VISIBLE);
            } else {
                selectedMode = GameEngine.Mode.CLIENT;
                addBtn.setVisibility(View.GONE);
                nameInput.setHint("Your codename");
                startBtn.setText("FIND & JOIN");
                impostorCard.setVisibility(View.GONE);
                tvInfo.setText("Make sure you are on the host's hotspot or WiFi.");
                tvInfo.setVisibility(View.VISIBLE);
            }
        });

        // ── ADD PLAYER ────────────────────────────────────────────────────────
        addBtn.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            if (name.isEmpty()) {
                if (engine.getPlayers().size() < GameEngine.MIN_PLAYERS)
                    Toast.makeText(requireContext(), "Enter a name", Toast.LENGTH_SHORT).show();
                return;
            }
            engine.addPlayer(name);
            nameInput.setText("");
            nameInput.requestFocus();

            // Bounce the add button
            addBtn.animate().scaleX(0.85f).scaleY(0.85f).setDuration(60)
                    .withEndAction(() -> addBtn.animate().scaleX(1f).scaleY(1f)
                            .setDuration(150)
                            .setInterpolator(new OvershootInterpolator(2.5f)).start())
                    .start();

            refreshPlayersList();
            updateImpostorPickerMax();
        });

        // ── START BUTTON ──────────────────────────────────────────────────────
        startBtn.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            if (name.isEmpty() && selectedMode != GameEngine.Mode.PASS_PLAY) {
                Toast.makeText(requireContext(), "Enter your name first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (impostorPicker != null) {
                impostorPicker.clearFocus();
                engine.setImpostorCount(impostorPicker.getValue());
            }
            engine.setMode(selectedMode);

            // Press animation
            startBtn.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80)
                    .withEndAction(() -> {
                        startBtn.animate().scaleX(1f).scaleY(1f).setDuration(120)
                                .setInterpolator(new OvershootInterpolator(1.5f)).start();
                        switch (selectedMode) {
                            case PASS_PLAY: startPassPlay(); break;
                            case HOST:      startHosting(name, nameInput, addBtn, startBtn, tvInfo); break;
                            case CLIENT:    joinGame(name, nameInput, addBtn, startBtn, tvInfo); break;
                        }
                    }).start();
        });

        // ── ENTRANCE ANIMATION ────────────────────────────────────────────────
        animateEntrance(root);

        refreshPlayersList();
        return view;
    }

    private void animateEntrance(LinearLayout root) {
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            child.setAlpha(0f);
            child.setTranslationY(dp(30));
            child.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(100 + i * 70L)
                    .setDuration(350)
                    .setInterpolator(new OvershootInterpolator(0.8f))
                    .start();
        }
    }

    // ── Impostor counter card ─────────────────────────────────────────────────
    private LinearLayout buildImpostorCard() {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setGravity(android.view.Gravity.CENTER_VERTICAL);
        int pad = dp(20);
        card.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(16);
        card.setLayoutParams(cardParams);

        LinearLayout leftCol = new LinearLayout(requireContext());
        leftCol.setOrientation(LinearLayout.VERTICAL);
        leftCol.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView label = new TextView(requireContext());
        label.setText("IMPOSTORS");
        label.setTextSize(10f);
        label.setLetterSpacing(0.2f);
        label.setTextColor(0xFF8A9BC4);

        tvImpostorLabel = new TextView(requireContext());
        tvImpostorLabel.setText("1 impostor");
        tvImpostorLabel.setTextSize(15f);
        tvImpostorLabel.setTextColor(0xFFF0F4FF);
        tvImpostorLabel.setPadding(0, dp(4), 0, 0);

        leftCol.addView(label);
        leftCol.addView(tvImpostorLabel);
        card.addView(leftCol);

        impostorPicker = new NumberPicker(requireContext());
        impostorPicker.setMinValue(1);
        impostorPicker.setMaxValue(1);
        impostorPicker.setValue(1);
        impostorPicker.setWrapSelectorWheel(false);

        try {
            java.lang.reflect.Field f = NumberPicker.class.getDeclaredField("mInputText");
            f.setAccessible(true);
            EditText et = (EditText) f.get(impostorPicker);
            if (et != null) et.setTextColor(0xFFF0B429);
        } catch (Exception ignored) {}

        impostorPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            engine.setImpostorCount(newVal);
            updateImpostorLabel(newVal);
        });

        card.addView(impostorPicker);
        return card;
    }

    private void updateImpostorPickerMax() {
        if (impostorPicker == null) return;
        int playerCount = engine.getPlayers().size();
        int max = Math.max(1, (playerCount - 1) / 2);
        impostorPicker.setMaxValue(max);
        if (impostorPicker.getValue() > max) impostorPicker.setValue(max);
        updateImpostorLabel(impostorPicker.getValue());
    }

    private void updateImpostorLabel(int count) {
        if (tvImpostorLabel != null)
            tvImpostorLabel.setText(count + (count == 1 ? " impostor" : " impostors"));
    }

    // ── Game modes ────────────────────────────────────────────────────────────
    private void startPassPlay() {
        if (engine.getPlayers().size() < GameEngine.MIN_PLAYERS) {
            Toast.makeText(requireContext(),
                    "Need at least " + GameEngine.MIN_PLAYERS + " players", Toast.LENGTH_SHORT).show();
            return;
        }
        ((MainActivity) requireActivity()).navigateTo(GameState.THEME_PICKER);
    }

    private void startHosting(String name, EditText nameInput,
                              Button addBtn, Button startBtn, TextView tvInfo) {
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Enter your name first", Toast.LENGTH_SHORT).show();
            return;
        }
        engine.addPlayer(name);
        engine.setMyPlayerId(0);
        HostServer host = new HostServer();
        host.startServer();
        engine.setHostServer(host);
        engine.handleMessage(new Message("PLAYER_LIST", null, "0:" + name + ";"));

        String localIp = Utils.getLocalIp();
        tvInfo.setText("✅  Hosting  •  IP: " + (localIp != null ? localIp : "unknown")
                + "\nAgents auto-discover you on the same network.");
        tvInfo.setTextColor(0xFF38B2AC);
        tvInfo.setVisibility(View.VISIBLE);

        nameInput.setEnabled(false);
        addBtn.setVisibility(View.GONE);
        startBtn.setText("START GAME");
        updateImpostorPickerMax();

        startBtn.setOnClickListener(v -> {
            if (impostorPicker != null) {
                impostorPicker.clearFocus();
                engine.setImpostorCount(impostorPicker.getValue());
            }
            if (engine.getPlayers().size() < GameEngine.MIN_PLAYERS) {
                Toast.makeText(requireContext(),
                        "Need at least " + GameEngine.MIN_PLAYERS + " agents", Toast.LENGTH_SHORT).show();
                return;
            }
            ((MainActivity) requireActivity()).navigateTo(GameState.THEME_PICKER);
        });

        startPolling();
    }

    private void joinGame(String name, EditText nameInput,
                          Button addBtn, Button startBtn, TextView tvInfo) {
        startBtn.setEnabled(false);
        startBtn.setText("SCANNING...");
        nameInput.setEnabled(false);
        addBtn.setVisibility(View.GONE);
        tvInfo.setText("🔍  Broadcasting to find host...");
        tvInfo.setTextColor(0xFF8A9BC4);
        tvInfo.setVisibility(View.VISIBLE);

        ClientConnection client = new ClientConnection();
        engine.setClientConnection(client);

        client.discoverAndConnect(
                () -> {
                    client.send(new Message("JOIN", null, name));
                    uiHandler.post(() -> {
                        startBtn.setText("WAITING FOR HOST...");
                        tvInfo.setText("✅  Connected! Waiting for host to start.");
                        tvInfo.setTextColor(0xFF38B2AC);
                        startPolling();
                    });
                },
                () -> uiHandler.post(() -> {
                    tvInfo.setText("❌  Host not found. Check network and try again.");
                    tvInfo.setTextColor(0xFFE53E3E);
                    startBtn.setEnabled(true);
                    startBtn.setText("FIND & JOIN");
                    nameInput.setEnabled(true);
                })
        );
    }

    // ── Polling ───────────────────────────────────────────────────────────────
    private void startPolling() {
        stopPolling();
        pollRunnable = new Runnable() {
            @Override public void run() {
                refreshPlayersList();
                updateImpostorPickerMax();
                uiHandler.postDelayed(this, 1000);
            }
        };
        uiHandler.post(pollRunnable);
    }

    private void stopPolling() {
        if (uiHandler != null && pollRunnable != null) {
            uiHandler.removeCallbacks(pollRunnable);
            pollRunnable = null;
        }
    }

    public void refreshPlayersList() {
        if (playersView == null) return;
        playersView.removeAllViews();
        java.util.List<Player> list = engine.getPlayers();

        if (tvNoPlayers != null)
            tvNoPlayers.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        if (tvPlayerCount != null)
            tvPlayerCount.setText(String.valueOf(list.size()));

        for (int idx = 0; idx < list.size(); idx++) {
            Player p = list.get(idx);
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(R.drawable.bg_player_item);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.bottomMargin = dp(6);
            row.setLayoutParams(rowParams);
            row.setPadding(dp(14), dp(10), dp(14), dp(10));

            // Bullet
            TextView tvBullet = new TextView(requireContext());
            tvBullet.setText("⬡");
            tvBullet.setTextColor(0xFF3D4F72);
            tvBullet.setTextSize(12f);
            tvBullet.setPadding(0, 0, dp(10), 0);

            TextView tvName = new TextView(requireContext());
            tvName.setText(p.getName());
            tvName.setTextColor(0xFFF0F4FF);
            tvName.setTextSize(14f);
            tvName.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView btnDelete = new TextView(requireContext());
            btnDelete.setText("✕");
            btnDelete.setTextColor(0xFF3D4F72);
            btnDelete.setTextSize(15f);
            btnDelete.setPadding(dp(12), dp(2), dp(4), dp(2));
            btnDelete.setClickable(true);
            btnDelete.setFocusable(true);
            int playerId = p.getId();
            btnDelete.setOnClickListener(v -> {
                // Fade out then remove
                row.animate().alpha(0f).translationX(dp(20)).setDuration(180)
                        .withEndAction(() -> {
                            engine.removePlayer(playerId);
                            refreshPlayersList();
                            updateImpostorPickerMax();
                        }).start();
            });
            btnDelete.setOnTouchListener((v, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN)
                    btnDelete.setTextColor(0xFFE53E3E);
                else if (event.getAction() == android.view.MotionEvent.ACTION_UP
                        || event.getAction() == android.view.MotionEvent.ACTION_CANCEL)
                    btnDelete.setTextColor(0xFF3D4F72);
                v.performClick();
                return false;
            });

            row.addView(tvBullet);
            row.addView(tvName);
            row.addView(btnDelete);

            // Staggered slide-in
            row.setAlpha(0f);
            row.setTranslationX(-dp(20));
            final int delay = idx * 40;
            row.animate().alpha(1f).translationX(0f)
                    .setStartDelay(delay).setDuration(250)
                    .setInterpolator(new OvershootInterpolator(0.6f)).start();

            playersView.addView(row);
        }
        updateImpostorPickerMax();
    }

    private RadioButton makeRadioBtn(String label) {
        RadioButton rb = new RadioButton(requireContext());
        rb.setId(View.generateViewId());
        rb.setText(label);
        rb.setTextColor(0xFF8A9BC4);
        rb.setButtonTintList(ColorStateList.valueOf(0xFFF0B429));
        rb.setPadding(0, 0, dp(20), 0);
        rb.setTextSize(13f);
        return rb;
    }

    private int dp(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() { stopPolling(); super.onDestroyView(); }
}