package com.example.mini_projet.mafia;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.mini_projet.R;

import java.util.ArrayList;
import java.util.List;

public class MafiaNetworkDayActivity extends AppCompatActivity
        implements MafiaEventBus.Listener {

    private ArrayList<Player> players;
    private int myId;
    private int round;

    private LinearLayout llPlayers;
    private TextView     tvStatus;
    private TextView     tvTitle;
    private TextView     btnVote;
    private Player       selectedTarget = null;
    private boolean      voted          = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        players = (ArrayList<Player>) getIntent().getSerializableExtra("players");
        myId    = getIntent().getIntExtra("my_id", -1);
        round   = getIntent().getIntExtra("round", 1);
        MafiaEventBus.register(this);
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_dark));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(40), dp(20), dp(20));
        scroll.addView(root);

        // Header
        tvTitle = label("☀️  DAY  •  Round " + round, 22, true, 0xFFF0F4FF);
        marginBottom(tvTitle, dp(6));
        root.addView(tvTitle);

        TextView tvSub = label("Discuss and vote to eliminate a suspect", 13, false, 0xFF8A9BC4);
        marginBottom(tvSub, dp(28));
        root.addView(tvSub);

        root.addView(sectionLabel("ALIVE PLAYERS — tap to vote"));

        llPlayers = new LinearLayout(this);
        llPlayers.setOrientation(LinearLayout.VERTICAL);
        marginBottom(llPlayers, dp(24));
        root.addView(llPlayers);
        buildPlayerList();

        // Status
        tvStatus = label("", 13, false, 0xFF8A9BC4);
        tvStatus.setGravity(Gravity.CENTER);
        marginBottom(tvStatus, dp(16));
        tvStatus.setVisibility(View.GONE);
        root.addView(tvStatus);

        // Vote button
        btnVote = new TextView(this);
        btnVote.setText("SUBMIT VOTE");
        btnVote.setTextSize(15);
        btnVote.setTypeface(null, Typeface.BOLD);
        btnVote.setTextColor(ContextCompat.getColor(this, R.color.bg_dark));
        btnVote.setBackgroundResource(R.drawable.bg_button_gold);
        btnVote.setGravity(Gravity.CENTER);
        btnVote.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56)));
        btnVote.setOnClickListener(v -> onVoteSubmit());
        root.addView(btnVote);

        setContentView(scroll);
    }

    private void buildPlayerList() {
        llPlayers.removeAllViews();
        List<Player> alive = new ArrayList<>();
        for (Player p : players) if (p.isAlive()) alive.add(p);

        for (Player p : alive) {
            boolean isMe = (p.getId() == myId);

            TextView row = new TextView(this);
            row.setText("    " + p.getName() + (isMe ? "  (you)" : ""));
            row.setTextSize(16);
            row.setTextColor(isMe ? 0xFF8A9BC4 : 0xFFD0D8F0);
            row.setPadding(dp(16), dp(16), dp(16), dp(16));
            row.setBackgroundResource(R.drawable.bg_player_item);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(8);
            row.setLayoutParams(lp);

            if (!isMe && !voted) {
                row.setOnClickListener(v -> {
                    selectedTarget = p;
                    for (int i = 0; i < llPlayers.getChildCount(); i++)
                        llPlayers.getChildAt(i).setAlpha(0.45f);
                    row.setAlpha(1f);
                    tvStatus.setText("Voting for: " + p.getName());
                    tvStatus.setTextColor(0xFFF0B429);
                    tvStatus.setVisibility(View.VISIBLE);
                });
            }
            llPlayers.addView(row);
        }
    }

    private void onVoteSubmit() {
        if (voted) return;
        if (selectedTarget == null) {
            Toast.makeText(this, "Tap a player to vote for them first", Toast.LENGTH_SHORT).show();
            return;
        }
        voted = true;
        btnVote.setEnabled(false);
        btnVote.setText("VOTE SUBMITTED ✓");
        tvStatus.setText("Waiting for all players to vote...");
        tvStatus.setTextColor(0xFF8A9BC4);
        tvStatus.setVisibility(View.VISIBLE);

        // Send vote to host via MafiaJoinActivity relay
        MafiaEventBus.post("SEND_VOTE", myId + ":" + selectedTarget.getId());
    }

    // ── EventBus ──────────────────────────────────────────────────────────────
    @Override
    public void onEvent(String type, String payload) {
        runOnUiThread(() -> {
            switch (type) {

                case MafiaEventBus.EVENT_ELIMINATED: {
                    // "PlayerName:ROLE"
                    String[] parts = payload.split(":", 2);
                    String name = parts.length > 0 ? parts[0] : "Someone";
                    String role = parts.length > 1 ? parts[1] : "CIVILIAN";
                    String emoji = roleEmoji(role);
                    // Mark that player as dead in local list
                    for (Player p : players) {
                        if (p.getName().equals(name)) { p.setAlive(false); break; }
                    }
                    // Show dialog
                    new AlertDialog.Builder(this)
                            .setTitle("☀️  Eliminated")
                            .setMessage(name + " has been eliminated.\n\nTheir role was: " + emoji + "  " + role)
                            .setPositiveButton("OK", null)
                            .setCancelable(false)
                            .show();
                    break;
                }

                case MafiaEventBus.EVENT_STATE: {
                    if ("ROLE_REVEAL".equals(payload)) {
                        // Next night — go back to role reveal
                        Intent i = new Intent(this, MafiaNetworkRoleRevealActivity.class);
                        i.putExtra(MafiaNetworkRoleRevealActivity.EXTRA_PLAYERS, players);
                        i.putExtra(MafiaNetworkRoleRevealActivity.EXTRA_MY_ID,   myId);
                        i.putExtra(MafiaNetworkRoleRevealActivity.EXTRA_ROUND,   round + 1);
                        startActivity(i);
                        finish();
                    }
                    break;
                }

                case MafiaEventBus.EVENT_GAME_OVER: {
                    String[] p = payload.split("\\|", 2);
                    Intent i = new Intent(this, MafiaNetworkGameOverActivity.class);
                    i.putExtra("title",   p.length > 0 ? p[0] : "Game Over");
                    i.putExtra("message", p.length > 1 ? p[1] : "");
                    startActivity(i);
                    finish();
                    break;
                }
            }
        });
    }

    private String roleEmoji(String role) {
        switch (role) {
            case "MAFIA":     return "🧛";
            case "DOCTOR":    return "🩺";
            case "DETECTIVE": return "🔎";
            default:          return "👤";
        }
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
        marginBottom(tv, dp(10));
        return tv;
    }

    private void marginBottom(View v, int m) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = m;
        v.setLayoutParams(lp);
    }

    private int dp(int d) {
        return Math.round(d * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        MafiaEventBus.unregister(this);
        super.onDestroy();
    }
}
