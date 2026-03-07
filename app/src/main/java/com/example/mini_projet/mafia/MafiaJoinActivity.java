package com.example.mini_projet.mafia;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.mini_projet.R;

import java.util.ArrayList;
import java.util.List;


public class MafiaJoinActivity extends AppCompatActivity
        implements MafiaNetworkClient.ClientCallback, MafiaEventBus.Listener {

    private MafiaNetworkClient client;

    private LinearLayout stepName;
    private LinearLayout stepWaiting;

    private EditText etName;
    private Button   btnFind;
    private TextView tvStatus;

    private TextView     tvWaitStatus;
    private LinearLayout llPlayers;
    private TextView     tvPlayerCount;

    private String        myName       = "";
    private Player.Role   myRole       = Player.Role.CIVILIAN;
    private List<Integer> mafiaTeamIds = new ArrayList<>();
    private List<Player>  lobbyPlayers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MafiaEventBus.register(this);   // relay SEND_VOTE to server
        buildUi();
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_dark));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(40), dp(20), dp(20));
        scroll.addView(root);

        // Header
        TextView tvHeader = label("JOIN GAME", 22, true, 0xFFF0F4FF);
        tvHeader.setPadding(0, 0, 0, dp(4));
        root.addView(tvHeader);

        TextView tvSub = label("Connect to a host on the same WiFi or hotspot", 13, false, 0xFF8A9BC4);
        marginBottom(tvSub, dp(28));
        root.addView(tvSub);

        // ── STEP 1: name + find ───────────────────────────────────────────────
        stepName = new LinearLayout(this);
        stepName.setOrientation(LinearLayout.VERTICAL);
        root.addView(stepName);

        stepName.addView(sectionLabel("YOUR NAME"));

        LinearLayout nameCard = card();
        marginBottom(nameCard, dp(20));
        stepName.addView(nameCard);

        etName = new EditText(this);
        etName.setHint("Enter your name");
        etName.setHintTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        etName.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        etName.setTextSize(16);
        etName.setBackground(null);
        etName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        etName.setSingleLine(true);
        etName.setPadding(0, dp(14), 0, dp(14));
        nameCard.addView(etName);

        btnFind = goldButton("FIND & JOIN");
        btnFind.setOnClickListener(v -> onFindClicked());
        stepName.addView(btnFind);

        tvStatus = label("", 13, false, 0xFF8A9BC4);
        tvStatus.setGravity(Gravity.CENTER);
        tvStatus.setPadding(0, dp(16), 0, 0);
        tvStatus.setLineSpacing(0, 1.5f);
        tvStatus.setVisibility(View.GONE);
        stepName.addView(tvStatus);

        // ── STEP 2: waiting room ──────────────────────────────────────────────
        stepWaiting = new LinearLayout(this);
        stepWaiting.setOrientation(LinearLayout.VERTICAL);
        stepWaiting.setVisibility(View.GONE);
        root.addView(stepWaiting);

        LinearLayout statusCard = card();
        marginBottom(statusCard, dp(24));
        stepWaiting.addView(statusCard);

        tvWaitStatus = label("Connected! Waiting for host to start...", 14, true, 0xFF38B2AC);
        tvWaitStatus.setLineSpacing(0, 1.4f);
        statusCard.addView(tvWaitStatus);

        tvPlayerCount = sectionLabel("PLAYERS IN LOBBY");
        marginBottom(tvPlayerCount, dp(10));
        stepWaiting.addView(tvPlayerCount);

        llPlayers = new LinearLayout(this);
        llPlayers.setOrientation(LinearLayout.VERTICAL);
        stepWaiting.addView(llPlayers);

        setContentView(scroll);
    }

    // ── Find / connect ────────────────────────────────────────────────────────
    private void onFindClicked() {
        myName = etName.getText().toString().trim();
        if (myName.isEmpty()) {
            Toast.makeText(this, "Enter your name first", Toast.LENGTH_SHORT).show();
            return;
        }
        btnFind.setEnabled(false);
        btnFind.setText("SEARCHING...");
        tvStatus.setText("Broadcasting to find host...");
        tvStatus.setTextColor(0xFF8A9BC4);
        tvStatus.setVisibility(View.VISIBLE);

        client = new MafiaNetworkClient();
        client.setCallback(this);
        client.discoverAndConnect();
    }

    // ── ClientCallback ────────────────────────────────────────────────────────

    @Override
    public void onConnected() {
        tvStatus.setText("Found host! Joining...");
        tvStatus.setTextColor(0xFF38B2AC);
        client.sendJoin(myName);
    }

    @Override
    public void onFailed() {
        tvStatus.setText("Host not found.\n\nMake sure you're on the same WiFi or hotspot as the host, then try again.");
        tvStatus.setTextColor(0xFFE53E3E);
        btnFind.setEnabled(true);
        btnFind.setText("TRY AGAIN");
    }

    @Override
    public void onLobbyUpdate(List<Player> players) {
        lobbyPlayers = players;

        if (stepWaiting.getVisibility() != View.VISIBLE) {
            stepName.setVisibility(View.GONE);
            stepWaiting.setVisibility(View.VISIBLE);
        }

        tvPlayerCount.setText("PLAYERS IN LOBBY (" + players.size() + ")");
        llPlayers.removeAllViews();
        for (Player p : players) {
            TextView chip = label("    " + p.getName(), 15, false, 0xFF8A9BC4);
            marginBottom(chip, dp(6));
            llPlayers.addView(chip);
        }
    }

    @Override
    public void onRoleAssigned(Player.Role role, List<Integer> mafiaTeamIds) {
        this.myRole       = role;
        this.mafiaTeamIds = mafiaTeamIds;
        tvWaitStatus.setText("Role assigned! Get ready...");
        tvWaitStatus.setTextColor(0xFFF0B429);
    }

    @Override
    public void onGameStart(int round) {
        int myId = client.getMyId();

        // Apply roles we know about to our local player list
        for (Player p : lobbyPlayers) {
            if (p.getId() == myId) {
                p.setRole(myRole);
            }
            if (myRole == Player.Role.MAFIA && mafiaTeamIds.contains(p.getId())) {
                p.setRole(Player.Role.MAFIA);
            }
        }

        Intent intent = new Intent(this, MafiaNetworkRoleRevealActivity.class);
        intent.putExtra(MafiaNetworkRoleRevealActivity.EXTRA_PLAYERS, new ArrayList<>(lobbyPlayers));
        intent.putExtra(MafiaNetworkRoleRevealActivity.EXTRA_MY_ID,   myId);
        intent.putExtra(MafiaNetworkRoleRevealActivity.EXTRA_ROUND,   round);
        startActivity(intent);
        // Do NOT call finish() — we stay alive as the TCP relay
    }

    @Override
    public void onEliminated(String playerName, String roleName) {
        MafiaEventBus.post(MafiaEventBus.EVENT_ELIMINATED, playerName + ":" + roleName);
    }

    @Override
    public void onNightResult(String text) {
        MafiaEventBus.post(MafiaEventBus.EVENT_NIGHT_RESULT, text);
    }

    @Override
    public void onStateChange(String state) {
        MafiaEventBus.post(MafiaEventBus.EVENT_STATE, state);
    }

    @Override
    public void onGameOver(String title, String message) {
        MafiaEventBus.post(MafiaEventBus.EVENT_GAME_OVER, title + "|" + message);
    }

    // ── EventBus — relay SEND_VOTE from game screens to host via TCP ──────────
    @Override
    public void onEvent(String type, String payload) {
        if ("SEND_VOTE".equals(type) && client != null && client.isConnected()) {
            // payload = "myId:targetId"  sent by MafiaNetworkDayActivity
            String[] parts = payload.split(":", 2);
            if (parts.length == 2) {
                client.send("VOTE", parts[0].trim(), parts[1].trim());
            }
        }
        // All other event types (STATE, NIGHT_RESULT, GAME_OVER) are forwarded
        // in the ClientCallback methods above — no double-post needed here
    }

    // ── View helpers ──────────────────────────────────────────────────────────
    private TextView label(String t, float size, boolean bold, int color) {
        TextView tv = new TextView(this);
        tv.setText(t); tv.setTextSize(size); tv.setTextColor(color);
        if (bold) tv.setTypeface(null, Typeface.BOLD);
        return tv;
    }

    private TextView sectionLabel(String t) {
        TextView tv = label(t, 10, false, 0xFF8A9BC4);
        tv.setLetterSpacing(0.15f);
        return tv;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackgroundResource(R.drawable.bg_card);
        int p = dp(16);
        c.setPadding(p, p, p, p);
        c.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        return c;
    }

    private Button goldButton(String text) {
        Button b = new Button(this);
        b.setText(text); b.setTextSize(15); b.setAllCaps(true);
        b.setTypeface(null, Typeface.BOLD);
        b.setTextColor(ContextCompat.getColor(this, R.color.bg_dark));
        b.setBackgroundResource(R.drawable.bg_button_gold);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56));
        b.setLayoutParams(lp);
        return b;
    }

    private void marginBottom(View v, int margin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = margin;
        v.setLayoutParams(lp);
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        MafiaEventBus.unregister(this);
        if (client != null) client.disconnect();
        super.onDestroy();
    }
}