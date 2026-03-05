package com.example.mini_projet.mafia;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.mini_projet.R;

import java.util.ArrayList;
import java.util.List;


public class MafiaHostActivity extends AppCompatActivity
        implements MafiaNetworkServer.HostCallback {

    private MafiaNetworkServer server;
    private LinearLayout stepName;
    private LinearLayout stepLobby;
    private EditText etHostName;
    private TextView     tvIp;
    private TextView     tvPlayerCount;
    private LinearLayout llPlayerChips;
    private TextView     tvMafiaCount;
    private Switch       swDoctor;
    private Switch       swDetective;
    private Button       btnStart;
    private int    mafiaCount = 2;
    private String hostName   = "";
    private static final int MIN_MAFIA = 1;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_dark));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(40), dp(20), dp(20));
        scroll.addView(root);

        TextView tvHeader = label("HOST GAME", 22, true, 0xFFF0F4FF);
        tvHeader.setPadding(0, 0, 0, dp(4));
        root.addView(tvHeader);

        TextView tvSub = label("Friends join you over WiFi or hotspot", 13, false, 0xFF8A9BC4);
        marginBottom(tvSub, dp(28));
        root.addView(tvSub);

        stepName = new LinearLayout(this);
        stepName.setOrientation(LinearLayout.VERTICAL);
        root.addView(stepName);

        stepName.addView(sectionLabel("YOUR NAME"));

        LinearLayout nameCard = card();
        marginBottom(nameCard, dp(20));
        stepName.addView(nameCard);

        etHostName = new EditText(this);
        etHostName.setHint("Enter your name");
        etHostName.setHintTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        etHostName.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        etHostName.setTextSize(16);
        etHostName.setBackground(null);
        etHostName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        etHostName.setSingleLine(true);
        etHostName.setPadding(0, dp(14), 0, dp(14));
        nameCard.addView(etHostName);

        Button btnContinue = goldButton("START HOSTING");
        btnContinue.setOnClickListener(v -> onContinueClicked());
        stepName.addView(btnContinue);

        stepLobby = new LinearLayout(this);
        stepLobby.setOrientation(LinearLayout.VERTICAL);
        stepLobby.setVisibility(View.GONE);
        root.addView(stepLobby);

        LinearLayout ipCard = new LinearLayout(this);
        ipCard.setOrientation(LinearLayout.HORIZONTAL);
        ipCard.setBackgroundResource(R.drawable.bg_card);
        ipCard.setGravity(Gravity.CENTER_VERTICAL);
        int pad = dp(20);
        ipCard.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams ipCardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        ipCardLp.bottomMargin = dp(24);
        ipCard.setLayoutParams(ipCardLp);
        stepLobby.addView(ipCard);

        TextView tvIcon = new TextView(this);
        tvIcon.setText("📡");
        tvIcon.setTextSize(36);
        tvIcon.setPadding(0, 0, dp(16), 0);
        ipCard.addView(tvIcon);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        ipCard.addView(textCol);

        tvIp = label("🟢  Server ready", 15, true, 0xFF38B2AC);
        textCol.addView(tvIp);

        TextView tvShareHint = label("Friends: open Mafia → Join Game\nMake sure you're on the same WiFi or hotspot", 12, false, 0xFF8A9BC4);
        tvShareHint.setPadding(0, dp(6), 0, 0);
        tvShareHint.setLineSpacing(0, 1.4f);
        textCol.addView(tvShareHint);

        tvPlayerCount = sectionLabel("PLAYERS (1 in lobby)");
        marginBottom(tvPlayerCount, dp(10));
        stepLobby.addView(tvPlayerCount);

        llPlayerChips = new LinearLayout(this);
        llPlayerChips.setOrientation(LinearLayout.VERTICAL);
        marginBottom(llPlayerChips, dp(24));
        stepLobby.addView(llPlayerChips);

        LinearLayout configCard = card();
        marginBottom(configCard, dp(24));
        stepLobby.addView(configCard);

        configCard.addView(sectionLabel("GAME SETTINGS"));

        LinearLayout mafiaRow = new LinearLayout(this);
        mafiaRow.setOrientation(LinearLayout.HORIZONTAL);
        mafiaRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams mrLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        mrLp.topMargin = dp(12);
        mafiaRow.setLayoutParams(mrLp);

        TextView tvMafiaTitle = label("Mafia members", 14, true, 0xFFF0F4FF);
        tvMafiaTitle.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        mafiaRow.addView(tvMafiaTitle);

        TextView btnMinus = label("  −  ", 22, true, 0xFFF0B429);
        btnMinus.setClickable(true);
        btnMinus.setFocusable(true);
        btnMinus.setOnClickListener(v -> {
            if (mafiaCount > MIN_MAFIA) { mafiaCount--; refreshMafiaLabel(); }
            else Toast.makeText(this, "Minimum 1 Mafia", Toast.LENGTH_SHORT).show();
        });
        mafiaRow.addView(btnMinus);

        tvMafiaCount = label(String.valueOf(mafiaCount), 22, true, 0xFFF0B429);
        tvMafiaCount.setMinWidth(dp(36));
        tvMafiaCount.setGravity(Gravity.CENTER);
        mafiaRow.addView(tvMafiaCount);

        TextView btnPlus = label("  +  ", 22, true, 0xFFF0B429);
        btnPlus.setClickable(true);
        btnPlus.setFocusable(true);
        btnPlus.setOnClickListener(v -> {
            int max = Math.max(1, server.getPlayers().size() / 3);
            if (mafiaCount < max) { mafiaCount++; refreshMafiaLabel(); }
            else Toast.makeText(this, "Too many Mafia for this player count", Toast.LENGTH_SHORT).show();
        });
        mafiaRow.addView(btnPlus);
        configCard.addView(mafiaRow);

        configCard.addView(divider(dp(14)));

        swDoctor = new Switch(this);
        swDoctor.setChecked(true);
        configCard.addView(toggleRow("Doctor", "Protects one player each night", swDoctor));

        configCard.addView(divider(dp(4)));

        swDetective = new Switch(this);
        swDetective.setChecked(true);
        configCard.addView(toggleRow("Detective", "Investigates one player each night", swDetective));

        btnStart = goldButton("START GAME");
        btnStart.setOnClickListener(v -> onStartClicked());
        stepLobby.addView(btnStart);

        setContentView(scroll);
    }

    private void onContinueClicked() {
        hostName = etHostName.getText().toString().trim();
        if (hostName.isEmpty()) {
            Toast.makeText(this, "Enter your name first", Toast.LENGTH_SHORT).show();
            return;
        }
        stepName.setVisibility(View.GONE);
        stepLobby.setVisibility(View.VISIBLE);

        server = new MafiaNetworkServer();
        server.setCallback(this);
        server.start();
        server.addHostPlayer(hostName);
        MafiaServerHolder.set(server);

        tvIp.setText("🟢  Server ready — waiting for players...");

        refreshPlayerChips(server.getPlayers());
    }

    private void onStartClicked() {
        List<Player> current = server.getPlayers();
        if (current.size() < 4) {
            Toast.makeText(this, "Need at least 4 players", Toast.LENGTH_SHORT).show();
            return;
        }
        int special = mafiaCount
                + (swDoctor.isChecked()    ? 1 : 0)
                + (swDetective.isChecked() ? 1 : 0);
        if (special >= current.size()) {
            Toast.makeText(this, "Too many special roles — reduce or add players",
                    Toast.LENGTH_LONG).show();
            return;
        }

        btnStart.setEnabled(false);
        btnStart.setText("STARTING...");

        server.startGame(mafiaCount, swDoctor.isChecked(), swDetective.isChecked());

        ArrayList<Player> players = new ArrayList<>(server.getPlayers());
        uiHandler.postDelayed(() -> {
            Intent i = new Intent(this, MafiaRoleRevealActivity.class);
            i.putExtra(MafiaRoleRevealActivity.EXTRA_PLAYERS, players);
            i.putExtra(MafiaRoleRevealActivity.EXTRA_ROUND, 1);
            i.putExtra("is_host", true);
            startActivity(i);
        }, 400);
    }

    @Override
    public void onPlayerJoined(List<Player> currentPlayers) {
        refreshPlayerChips(currentPlayers);
    }

    @Override
    public void onError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void refreshPlayerChips(List<Player> players) {
        if (tvPlayerCount == null) return;
        uiHandler.post(() -> {
            tvPlayerCount.setText("PLAYERS (" + players.size() + " in lobby)");
            if (tvIp != null)
                tvIp.setText("🟢  Server ready  •  " + players.size() + " connected");
            llPlayerChips.removeAllViews();
            for (Player p : players) {
                TextView chip = label("    " + p.getName(), 15, false, 0xFF8A9BC4);
                marginBottom(chip, dp(6));
                llPlayerChips.addView(chip);
            }
            int max = Math.max(1, players.size() / 3);
            if (mafiaCount > max) { mafiaCount = max; refreshMafiaLabel(); }
        });
    }

    private void refreshMafiaLabel() {
        if (tvMafiaCount != null) tvMafiaCount.setText(String.valueOf(mafiaCount));
    }

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

    private LinearLayout toggleRow(String title, String subtitle, Switch sw) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52)));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        col.addView(label(title,    14, true,  0xFFF0F4FF));
        col.addView(label(subtitle, 11, false, 0xFF8A9BC4));
        row.addView(col);
        row.addView(sw);
        return row;
    }

    private View divider(int topMargin) {
        View d = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        lp.topMargin = topMargin; lp.bottomMargin = dp(14);
        d.setLayoutParams(lp);
        d.setBackgroundColor(0x268A9BC4);
        return d;
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
        if (server != null) server.stop();
        MafiaServerHolder.clear();
        super.onDestroy();
    }
}
