package com.example.mini_projet.mafia;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.mini_projet.R;

import java.util.ArrayList;

public class MafiaNetworkNightResultActivity extends AppCompatActivity
        implements MafiaEventBus.Listener {

    private ArrayList<Player> players;
    private int myId;
    private int round;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String result = getIntent().getStringExtra("result");
        players = (ArrayList<Player>) getIntent().getSerializableExtra("players");
        myId    = getIntent().getIntExtra("my_id", -1);
        round   = getIntent().getIntExtra("round", 1);

        MafiaEventBus.register(this);
        buildUi(result != null ? result : "The night passes quietly...");
    }

    private void buildUi(String result) {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_dark));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(60), dp(28), dp(28));
        scroll.addView(root);

        // Emoji
        TextView tvEmoji = new TextView(this);
        tvEmoji.setTextSize(56);
        tvEmoji.setGravity(Gravity.CENTER);
        if (result.contains("eliminated") || result.contains("Mafia")) {
            tvEmoji.setText("💀");
        } else if (result.contains("saved") || result.contains("Doctor")) {
            tvEmoji.setText("🧑‍⚕️");
        } else {
            tvEmoji.setText("🌙");
        }
        LinearLayout.LayoutParams eLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        eLp.bottomMargin = dp(20);
        tvEmoji.setLayoutParams(eLp);
        root.addView(tvEmoji);

        // Result text
        TextView tvResult = new TextView(this);
        tvResult.setText(result);
        tvResult.setTextSize(18);
        tvResult.setTypeface(null, Typeface.BOLD);
        tvResult.setTextColor(0xFFF0F4FF);
        tvResult.setGravity(Gravity.CENTER);
        tvResult.setLineSpacing(0, 1.5f);
        LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rLp.bottomMargin = dp(40);
        tvResult.setLayoutParams(rLp);
        root.addView(tvResult);

        // Waiting label
        TextView tvWait = new TextView(this);
        tvWait.setText("☀️  Day phase starting soon...\nWaiting for the host.");
        tvWait.setTextSize(13);
        tvWait.setTextColor(0xFF8A9BC4);
        tvWait.setGravity(Gravity.CENTER);
        tvWait.setLineSpacing(0, 1.5f);
        root.addView(tvWait);

        setContentView(scroll);
    }

    // ── EventBus ──────────────────────────────────────────────────────────────
    @Override
    public void onEvent(String type, String payload) {
        runOnUiThread(() -> {
            switch (type) {
                case MafiaEventBus.EVENT_STATE:
                    if ("DAY".equals(payload)) {
                        Intent i = new Intent(this, MafiaNetworkDayActivity.class);
                        i.putExtra("players", players);
                        i.putExtra("my_id",   myId);
                        i.putExtra("round",   round);
                        startActivity(i);
                        finish();
                    }
                    break;
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

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        MafiaEventBus.unregister(this);
        super.onDestroy();
    }
}