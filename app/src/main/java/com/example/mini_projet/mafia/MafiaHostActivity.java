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

    // ── Step containers ───────────────────────────────────────────────────────
    private LinearLayout stepName;
    private LinearLayout stepLobby;

    // ── Step 1 ────────────────────────────────────────────────────────────────
    private EditText etHostName;

    // ── Step 2 lobby views ────────────────────────────────────────────────────
    private TextView     tvIp;
    private TextView     tvPlayerCount;
    private LinearLayout llPlayerChips;
    private TextView     tvMafiaCount;
    private Switch       swDoctor;
    private Switch       swDetective;
    private Button       btnStart;

    // ── Config state ──────────────────────────────────────────────────────────
    private int    mafiaCount = 2;
    private String hostName   = "";
    private static final int MIN_MAFIA = 1;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    // ── onCreate ──────────────────────────────────────────────────────────────
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

        // Header
        TextView tvHeader = label(getString(R.string.mafia_host_title), 22, true, 0xFFF0F4FF);
        tvHeader.setPadding(0, 0, 0, dp(4));
        root.addView(tvHeader);

        TextView tvSub = label(getString(R.string.mafia_host_subtitle), 13, false, 0xFF8A9BC4);
        marginBottom(tvSub, dp(28));
        root.addView(tvSub);

        // ── STEP 1: enter your name ───────────────────────────────────────────
        stepName = new LinearLayout(this);
        stepName.setOrientation(LinearLayout.VERTICAL);
        root.addView(stepName);

        stepName.addView(sectionLabel(getString(R.string.mafia_host_your_name)));

        LinearLayout nameCard = card();
        marginBottom(nameCard, dp(20));
        stepName.addView(nameCard);

        etHostName = new EditText(this);
        etHostName.setHint(R.string.mafia_host_name_hint);
        etHostName.setHintTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        etHostName.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        etHostName.setTextSize(16);
        etHostName.setBackground(null);
        etHostName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        etHostName.setSingleLine(true);
        etHostName.setPadding(0, dp(14), 0, dp(14));
        nameCard.addView(etHostName);

        Button btnContinue = goldButton(getString(R.string.mafia_host_start_hosting));
        btnContinue.setOnClickListener(v -> onContinueClicked());
        stepName.addView(btnContinue);

        // ── STEP 2: lobby ─────────────────────────────────────────────────────
        stepLobby = new LinearLayout(this);
        stepLobby.setOrientation(LinearLayout.VERTICAL);
        stepLobby.setVisibility(View.GONE);
        root.addView(stepLobby);

        // Server status card — no IP shown
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

        // Antenna icon
        TextView tvIcon = new TextView(this);
        tvIcon.setText("📡");
        tvIcon.setTextSize(36);
        tvIcon.setPadding(0, 0, dp(16), 0);
        ipCard.addView(tvIcon);

        // Text column
        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        ipCard.addView(textCol);

        tvIp = label(getString(R.string.mafia_host_server_ready), 15, true, 0xFF38B2AC);
        textCol.addView(tvIp);

        TextView tvShareHint = label(getString(R.string.mafia_host_share_hint), 12, false, 0xFF8A9BC4);
        tvShareHint.setPadding(0, dp(6), 0, 0);
        tvShareHint.setLineSpacing(0, 1.4f);
        textCol.addView(tvShareHint);

        // Player list
        tvPlayerCount = sectionLabel(getString(R.string.mafia_net_players_lobby, 1));
        marginBottom(tvPlayerCount, dp(10));
        stepLobby.addView(tvPlayerCount);

        llPlayerChips = new LinearLayout(this);
        llPlayerChips.setOrientation(LinearLayout.VERTICAL);
        marginBottom(llPlayerChips, dp(24));
        stepLobby.addView(llPlayerChips);

        // Config card
        LinearLayout configCard = card();
        marginBottom(configCard, dp(24));
        stepLobby.addView(configCard);

        configCard.addView(sectionLabel(getString(R.string.mafia_host_game_settings)));

        // Mafia count row
        LinearLayout mafiaRow = new LinearLayout(this);
        mafiaRow.setOrientation(LinearLayout.HORIZONTAL);
        mafiaRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams mrLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        mrLp.topMargin = dp(12);
        mafiaRow.setLayoutParams(mrLp);

        TextView tvMafiaTitle = label(getString(R.string.mafia_host_mafia_members), 14, true, 0xFFF0F4FF);
        tvMafiaTitle.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        mafiaRow.addView(tvMafiaTitle);

        TextView btnMinus = label("  −  ", 22, true, 0xFFF0B429);
        btnMinus.setClickable(true);
        btnMinus.setFocusable(true);
        btnMinus.setOnClickListener(v -> {
            if (mafiaCount > MIN_MAFIA) { mafiaCount--; refreshMafiaLabel(); }
            else Toast.makeText(this, R.string.mafia_host_min_mafia_error, Toast.LENGTH_SHORT).show();
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
            else Toast.makeText(this, R.string.mafia_host_too_many_mafia_error, Toast.LENGTH_SHORT).show();
        });
        mafiaRow.addView(btnPlus);
        configCard.addView(mafiaRow);

        configCard.addView(divider(dp(14)));

        swDoctor = new Switch(this);
        swDoctor.setChecked(true);
        configCard.addView(toggleRow(getString(R.string.mafia_role_doctor), getString(R.string.mafia_role_doctor_desc), swDoctor));

        configCard.addView(divider(dp(4)));

        swDetective = new Switch(this);
        swDetective.setChecked(true);
        configCard.addView(toggleRow(getString(R.string.mafia_role_detective), getString(R.string.mafia_role_detective_desc), swDetective));

        btnStart = goldButton(getString(R.string.mafia_setup_start));
        btnStart.setOnClickListener(v -> onStartClicked());
        stepLobby.addView(btnStart);

        setContentView(scroll);
    }

    // ── Step 1: enter name → start server ────────────────────────────────────
    private void onContinueClicked() {
        hostName = etHostName.getText().toString().trim();
        if (hostName.isEmpty()) {
            Toast.makeText(this, R.string.mafia_enter_name_first, Toast.LENGTH_SHORT).show();
            return;
        }
        stepName.setVisibility(View.GONE);
        stepLobby.setVisibility(View.VISIBLE);

        server = new MafiaNetworkServer();
        server.setCallback(this);
        server.start();
        server.addHostPlayer(hostName);   // host = player id 0
        MafiaServerHolder.set(server);    // make server reachable from game screens

        tvIp.setText(R.string.mafia_net_server_ready);

        refreshPlayerChips(server.getPlayers());
    }

    // ── Step 2: start game ────────────────────────────────────────────────────
    private void onStartClicked() {
        List<Player> current = server.getPlayers();
        if (current.size() < 4) {
            Toast.makeText(this, R.string.mafia_setup_min_players_error, Toast.LENGTH_SHORT).show();
            return;
        }
        int special = mafiaCount
                + (swDoctor.isChecked()    ? 1 : 0)
                + (swDetective.isChecked() ? 1 : 0);
        if (special >= current.size()) {
            Toast.makeText(this, R.string.mafia_host_too_many_special_roles,
                    Toast.LENGTH_LONG).show();
            return;
        }

        btnStart.setEnabled(false);
        btnStart.setText(R.string.mafia_host_starting);

        // Assign roles + send YOUR_ROLE + START to all TCP clients
        server.startGame(mafiaCount, swDoctor.isChecked(), swDetective.isChecked());

        // Host device launches the pass-and-play game with full player list
        ArrayList<Player> players = new ArrayList<>(server.getPlayers());
        uiHandler.postDelayed(() -> {
            Intent i = new Intent(this, MafiaRoleRevealActivity.class);
            i.putExtra(MafiaRoleRevealActivity.EXTRA_PLAYERS, players);
            i.putExtra(MafiaRoleRevealActivity.EXTRA_ROUND, 1);
            // Flag: this is a hosted game — after each night the host must broadcast result
            i.putExtra("is_host", true);
            startActivity(i);
        }, 400);
    }

    // ── HostCallback — called on UI thread ────────────────────────────────────
    @Override
    public void onPlayerJoined(List<Player> currentPlayers) {
        refreshPlayerChips(currentPlayers);
    }

    @Override
    public void onError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void refreshPlayerChips(List<Player> players) {
        if (tvPlayerCount == null) return;
        uiHandler.post(() -> {
            tvPlayerCount.setText(getString(R.string.mafia_net_players_lobby, players.size()));
            if (tvIp != null)
                tvIp.setText(getString(R.string.mafia_net_server_connected, players.size()));
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

    // ── View factories ────────────────────────────────────────────────────────
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