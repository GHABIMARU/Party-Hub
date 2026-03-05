package com.example.mini_projet.mafia;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.mini_projet.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Mafiasetupactivity extends AppCompatActivity {

    // ── Step 1 views ──────────────────────────────────────────────────────────
    private LinearLayout layout_step1;
    private Button       btn_players_minus, btn_players_plus;
    private Button       btn_mafia_minus, btn_mafia_plus;
    private TextView     tv_player_count, tv_mafia_count;
    private Switch       switch_doctor, switch_detective;
    private Button       btn_next_to_names;

    // ── Step 2 views ──────────────────────────────────────────────────────────
    private LinearLayout layout_step2;
    private LinearLayout ll_name_inputs;       // populated dynamically
    private TextView     btn_back_to_settings;
    private Button       btn_start_game;

    // ── State ─────────────────────────────────────────────────────────────────
    private int playerCount = 6;
    private int mafiaCount  = 2;

    private static final int MIN_PLAYERS = 4;
    private static final int MAX_PLAYERS = 12;
    private static final int MIN_MAFIA   = 1;

    public static final String EXTRA_PLAYERS = "extra_players";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mafia_setup);

        bindViews();
        updatePlayerCountUI();
        updateMafiaCountUI();
        setListeners();
    }

    // ── Bind all views ────────────────────────────────────────────────────────
    private void bindViews() {
        // Step 1
        layout_step1       = findViewById(R.id.layout_step1);
        btn_players_minus  = findViewById(R.id.btn_players_minus);
        btn_players_plus   = findViewById(R.id.btn_players_plus);
        tv_player_count    = findViewById(R.id.tv_player_count);
        btn_mafia_minus    = findViewById(R.id.btn_mafia_minus);
        btn_mafia_plus     = findViewById(R.id.btn_mafia_plus);
        tv_mafia_count     = findViewById(R.id.tv_mafia_count);
        switch_doctor      = findViewById(R.id.switch_doctor);
        switch_detective   = findViewById(R.id.switch_detective);
        btn_next_to_names  = findViewById(R.id.btn_next_to_names);

        // Step 2
        layout_step2       = findViewById(R.id.layout_step2);
        ll_name_inputs     = findViewById(R.id.ll_name_inputs);
        btn_back_to_settings = findViewById(R.id.btn_back_to_settings);
        btn_start_game     = findViewById(R.id.btn_start_game);
    }

    // ── Listeners ─────────────────────────────────────────────────────────────
    private void setListeners() {

        btn_players_minus.setOnClickListener(v -> {
            if (playerCount > MIN_PLAYERS) {
                playerCount--;
                if (mafiaCount > maxAllowedMafia()) {
                    mafiaCount = maxAllowedMafia();
                    updateMafiaCountUI();
                }
                updatePlayerCountUI();
            } else {
                Toast.makeText(this, "Minimum " + MIN_PLAYERS + " players", Toast.LENGTH_SHORT).show();
            }
        });

        btn_players_plus.setOnClickListener(v -> {
            if (playerCount < MAX_PLAYERS) {
                playerCount++;
                updatePlayerCountUI();
            } else {
                Toast.makeText(this, "Maximum " + MAX_PLAYERS + " players", Toast.LENGTH_SHORT).show();
            }
        });

        btn_mafia_minus.setOnClickListener(v -> {
            if (mafiaCount > MIN_MAFIA) {
                mafiaCount--;
                updateMafiaCountUI();
            } else {
                Toast.makeText(this, "Need at least 1 Mafia", Toast.LENGTH_SHORT).show();
            }
        });

        btn_mafia_plus.setOnClickListener(v -> {
            if (mafiaCount < maxAllowedMafia()) {
                mafiaCount++;
                updateMafiaCountUI();
            } else {
                Toast.makeText(this, "Too many Mafia for player count", Toast.LENGTH_SHORT).show();
            }
        });

        // NEXT → go to name entry step
        btn_next_to_names.setOnClickListener(v -> {
            boolean includeDoctor    = switch_doctor.isChecked();
            boolean includeDetective = switch_detective.isChecked();

            int specialRoles = mafiaCount
                    + (includeDoctor    ? 1 : 0)
                    + (includeDetective ? 1 : 0);

            if (specialRoles >= playerCount) {
                Toast.makeText(this,
                        "Not enough civilians — reduce roles or add players",
                        Toast.LENGTH_LONG).show();
                return;
            }

            showNameInputStep();
        });

        // BACK → return to settings step
        btn_back_to_settings.setOnClickListener(v -> showSettingsStep());

        // START GAME
        btn_start_game.setOnClickListener(v -> startGame());
    }

    // ════════════════════════════════════════════════════════════════════════
    // STEP 1 ↔ STEP 2 transitions
    // ════════════════════════════════════════════════════════════════════════

    private void showSettingsStep() {
        layout_step1.setVisibility(View.VISIBLE);
        layout_step2.setVisibility(View.GONE);
    }

    private void showNameInputStep() {
        layout_step1.setVisibility(View.GONE);
        layout_step2.setVisibility(View.VISIBLE);
        buildNameInputs();
    }

    // ── Build one input row per player ────────────────────────────────────────
    private void buildNameInputs() {
        ll_name_inputs.removeAllViews();

        for (int i = 0; i < playerCount; i++) {
            final int index = i;

            // Row: number label + edit text
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rowLp.setMargins(0, 0, 0, dpToPx(i < playerCount - 1 ? 12 : 0));
            row.setLayoutParams(rowLp);

            // Player number badge
            TextView tvNum = new TextView(this);
            tvNum.setText(String.valueOf(i + 1));
            tvNum.setTextSize(13);
            tvNum.setTypeface(null, android.graphics.Typeface.BOLD);
            tvNum.setTextColor(ContextCompat.getColor(this, R.color.gold));
            tvNum.setMinWidth(dpToPx(28));
            tvNum.setGravity(Gravity.CENTER);

            // Name input
            EditText etName = new EditText(this);
            etName.setId(View.generateViewId());
            etName.setTag("name_input_" + i);
            etName.setHint("Player " + (i + 1));
            etName.setHintTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            etName.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            etName.setTextSize(15);
            etName.setBackground(null);
            etName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            etName.setSingleLine(true);
            etName.setPadding(dpToPx(12), dpToPx(14), dpToPx(12), dpToPx(14));

            LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            etName.setLayoutParams(etLp);

            // Divider below (except last)
            row.addView(tvNum);
            row.addView(etName);
            ll_name_inputs.addView(row);

            // Thin divider between rows
            if (i < playerCount - 1) {
                View divider = new View(this);
                LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1));
                divLp.setMargins(dpToPx(28), 0, 0, dpToPx(12));
                divider.setLayoutParams(divLp);
                divider.setBackgroundColor(ContextCompat.getColor(this, R.color.text_secondary));
                divider.setAlpha(0.15f);
                ll_name_inputs.addView(divider);
            }
        }
    }

    // ── Collect names and start game ──────────────────────────────────────────
    private void startGame() {
        // Collect and validate names
        List<String> names = new ArrayList<>();

        for (int i = 0; i < playerCount; i++) {
            EditText et = ll_name_inputs.findViewWithTag("name_input_" + i);
            String name = (et != null && et.getText().length() > 0)
                    ? et.getText().toString().trim()
                    : "Player " + (i + 1);   // fallback if left blank

            // Check for duplicates
            if (names.contains(name)) {
                Toast.makeText(this,
                        "\"" + name + "\" is already used — each player needs a unique name",
                        Toast.LENGTH_LONG).show();
                return;
            }
            names.add(name);
        }

        // Build roles
        boolean includeDoctor    = switch_doctor.isChecked();
        boolean includeDetective = switch_detective.isChecked();

        List<Player.Role> roles = new ArrayList<>();
        for (int i = 0; i < mafiaCount; i++) roles.add(Player.Role.MAFIA);
        if (includeDoctor)    roles.add(Player.Role.DOCTOR);
        if (includeDetective) roles.add(Player.Role.DETECTIVE);
        while (roles.size() < playerCount) roles.add(Player.Role.CIVILIAN);
        Collections.shuffle(roles);

        // Build players with real names
        ArrayList<Player> players = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            players.add(new Player(i, names.get(i), roles.get(i)));
        }

        // Launch Role Reveal
        Intent intent = new Intent(this, MafiaRoleRevealActivity.class);
        intent.putExtra(MafiaRoleRevealActivity.EXTRA_PLAYERS, players);
        intent.putExtra(MafiaRoleRevealActivity.EXTRA_ROUND, 1);
        startActivity(intent);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private int maxAllowedMafia() { return playerCount / 3; }
    private void updatePlayerCountUI() { tv_player_count.setText(String.valueOf(playerCount)); }
    private void updateMafiaCountUI()  { tv_mafia_count.setText(String.valueOf(mafiaCount)); }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}