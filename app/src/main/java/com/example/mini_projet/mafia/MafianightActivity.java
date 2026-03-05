package com.example.mini_projet.mafia;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.mini_projet.R;

import java.util.ArrayList;
import java.util.List;

public class MafianightActivity extends AppCompatActivity {

    // ── Views ─────────────────────────────────────────────────────────────────
    private TextView     tv_night_round;
    private TextView     tv_acting_role_emoji;
    private TextView     tv_acting_role_label;
    private TextView     tv_acting_instruction;
    private LinearLayout ll_player_list_night;
    private LinearLayout layout_selected_player;
    private TextView     tv_selected_player_name;
    private TextView     btn_confirm_night_action;
    private LinearLayout layout_pass_phone;
    private TextView     tv_pass_phone_message;
    private TextView     btn_ready;
    private LinearLayout layout_night_content;

    // ── State ─────────────────────────────────────────────────────────────────
    private ArrayList<Player> players;
    private int round = 2;

    private Player targetKill        = null;
    private Player targetSave        = null;
    private Player targetInvestigate = null;
    private Player actionSelection   = null;

    // 0=Mafia, 1=Doctor, 2=Detective
    private int currentPhase = 0;
    private boolean hasDoctor;
    private boolean hasDetective;

    public static final String EXTRA_PLAYERS = "extra_players";
    public static final String EXTRA_ROUND   = "extra_round";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mafia_night);

        players = (ArrayList<Player>) getIntent().getSerializableExtra(EXTRA_PLAYERS);
        round   = getIntent().getIntExtra(EXTRA_ROUND, 2);

        hasDoctor    = hasRole(Player.Role.DOCTOR);
        hasDetective = hasRole(Player.Role.DETECTIVE);

        bindViews();
        tv_night_round.setText("ROUND " + round);

        // Start with Mafia's private pass-phone screen
        showPassPhone(
                "🌙  Night " + round,
                "Everyone look away.\n\nMafia — tap when you're ready.",
                () -> loadPhase(0)
        );
    }

    private void bindViews() {
        tv_night_round           = findViewById(R.id.tv_night_round);
        tv_acting_role_emoji     = findViewById(R.id.tv_acting_role_emoji);
        tv_acting_role_label     = findViewById(R.id.tv_acting_role_label);
        tv_acting_instruction    = findViewById(R.id.tv_acting_instruction);
        ll_player_list_night     = findViewById(R.id.ll_player_list_night);
        layout_selected_player   = findViewById(R.id.layout_selected_player);
        tv_selected_player_name  = findViewById(R.id.tv_selected_player_name);
        btn_confirm_night_action = findViewById(R.id.btn_confirm_night_action);
        layout_pass_phone        = findViewById(R.id.layout_pass_phone);
        tv_pass_phone_message    = findViewById(R.id.tv_pass_phone_message);
        btn_ready                = findViewById(R.id.btn_ready);
        layout_night_content     = findViewById(R.id.layout_night_content);

        btn_confirm_night_action.setOnClickListener(v -> confirmPhase());
    }

    // ── Pass-phone lock screen ────────────────────────────────────────────────
    private Runnable onReadyAction;

    private void showPassPhone(String title, String message, Runnable onReady) {
        onReadyAction = onReady;
        tv_pass_phone_message.setText(title + "\n\n" + message);
        layout_pass_phone.setVisibility(View.VISIBLE);
        layout_night_content.setVisibility(View.GONE);

        btn_ready.setOnClickListener(v -> {
            layout_pass_phone.setVisibility(View.GONE);
            layout_night_content.setVisibility(View.VISIBLE);
            if (onReadyAction != null) onReadyAction.run();
        });
    }

    // ── Load phase ────────────────────────────────────────────────────────────
    private void loadPhase(int phase) {
        currentPhase  = phase;
        actionSelection = null;
        layout_selected_player.setVisibility(View.INVISIBLE);

        switch (phase) {
            case 0:
                tv_acting_role_emoji.setText("🧛");
                tv_acting_role_label.setText("MAFIA");
                tv_acting_instruction.setText(buildMafiaInfo() + "\n\nSelect a player to eliminate:");
                break;
            case 1:
                tv_acting_role_emoji.setText("🩺");
                tv_acting_role_label.setText("DOCTOR");
                tv_acting_instruction.setText("Choose one player to protect tonight.\nIf the Mafia targets them, they survive.");
                break;
            case 2:
                tv_acting_role_emoji.setText("🔎");
                tv_acting_role_label.setText("DETECTIVE");
                tv_acting_instruction.setText("Investigate one player.\nYou will instantly learn if they are Mafia.");
                break;
        }

        buildPlayerList();
    }

    private String buildMafiaInfo() {
        StringBuilder sb = new StringBuilder("Mafia team: ");
        boolean first = true;
        for (Player p : players) {
            if (p.getRole() == Player.Role.MAFIA && p.isAlive()) {
                if (!first) sb.append(", ");
                sb.append(p.getName());
                first = false;
            }
        }
        sb.append("\n\nChoose a town player to eliminate.\nYou cannot target Mafia teammates.");
        return sb.toString();
    }

    // ── Player list ───────────────────────────────────────────────────────────
    private void buildPlayerList() {
        ll_player_list_night.removeAllViews();
        for (Player p : getAlivePlayers()) {
            // Phase 0 = Mafia choosing: exclude all Mafia members (can't kill teammates)
            if (currentPhase == 0 && p.getRole() == Player.Role.MAFIA) continue;
            ll_player_list_night.addView(buildRow(p));
        }
    }

    private LinearLayout buildRow(Player target) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClickable(true);
        row.setFocusable(true);
        row.setPadding(dpToPx(8), 0, dpToPx(8), 0);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(48));
        lp.setMargins(0, 0, 0, dpToPx(6));
        row.setLayoutParams(lp);
        row.setBackgroundResource(R.drawable.bg_card);

        TextView name = new TextView(this);
        name.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        name.setText(target.getName());
        name.setTextSize(15);
        name.setTextColor(ContextCompat.getColor(this, R.color.text_primary));

        TextView indicator = new TextView(this);
        indicator.setText("○");
        indicator.setTextSize(20);
        indicator.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        indicator.setTag("ind_" + target.getId());

        row.addView(name);
        row.addView(indicator);
        row.setOnClickListener(v -> selectTarget(target));
        return row;
    }

    private void selectTarget(Player target) {
        actionSelection = target;

        for (int i = 0; i < ll_player_list_night.getChildCount(); i++) {
            LinearLayout row = (LinearLayout) ll_player_list_night.getChildAt(i);
            row.setBackgroundResource(R.drawable.bg_card);
            for (int j = 0; j < row.getChildCount(); j++) {
                View child = row.getChildAt(j);
                if (child instanceof TextView) {
                    String tag = (String) child.getTag();
                    if (tag != null && tag.startsWith("ind_")) {
                        ((TextView) child).setText("○");
                        ((TextView) child).setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                    }
                }
            }
        }

        for (int i = 0; i < ll_player_list_night.getChildCount(); i++) {
            LinearLayout row = (LinearLayout) ll_player_list_night.getChildAt(i);
            for (int j = 0; j < row.getChildCount(); j++) {
                View child = row.getChildAt(j);
                if (("ind_" + target.getId()).equals(child.getTag())) {
                    ((TextView) child).setText("●");
                    ((TextView) child).setTextColor(ContextCompat.getColor(this, R.color.gold));
                }
            }
        }

        tv_selected_player_name.setText(target.getName());
        layout_selected_player.setVisibility(View.VISIBLE);
    }

    // ── Confirm phase ─────────────────────────────────────────────────────────
    private void confirmPhase() {
        if (actionSelection == null) {
            Toast.makeText(this, "Select a player first", Toast.LENGTH_SHORT).show();
            return;
        }

        switch (currentPhase) {
            case 0:
                targetKill = actionSelection;
                advancePhase();
                break;
            case 1:
                targetSave = actionSelection;
                advancePhase();
                break;
            case 2:
                targetInvestigate = actionSelection;
                boolean isMafia = actionSelection.getRole() == Player.Role.MAFIA;
                new AlertDialog.Builder(this)
                        .setTitle(isMafia ? "🧛 MAFIA CONFIRMED" : "✅ NOT MAFIA")
                        .setMessage(actionSelection.getName() + " is "
                                + (isMafia ? "a Mafia member." : "NOT Mafia.")
                                + "\n\nRemember this for the day discussion.")
                        .setPositiveButton("GOT IT — HIDE SCREEN", (d, w) -> advancePhase())
                        .setCancelable(false)
                        .show();
                break;
        }
    }

    private void advancePhase() {
        int next = -1;
        if (currentPhase == 0) {
            if (hasDoctor)    next = 1;
            else if (hasDetective) next = 2;
        } else if (currentPhase == 1) {
            if (hasDetective) next = 2;
        }

        if (next == -1) {
            resolveAndGoToDay();
        } else {
            // ✅ Must be final/effectively-final to use inside lambda
            final int finalNext = next;
            final String finalTitle = (next == 1) ? "🩺  Doctor's Turn"   : "🔎  Detective's Turn";
            final String finalMsg   = (next == 1) ? "Mafia, look away.\n\nDoctor — tap when ready."
                    : "Look away.\n\nDetective — tap when ready.";
            showPassPhone(finalTitle, finalMsg, () -> loadPhase(finalNext));
        }
    }

    private void resolveAndGoToDay() {
        String result;
        boolean saved = (targetSave != null && targetKill != null
                && targetSave.getId() == targetKill.getId());

        if (targetKill == null) {
            result = "The Mafia did not strike tonight. 🌙";
        } else if (saved) {
            result = targetKill.getName() + " was targeted but saved by the Doctor! 🩺";
        } else {
            targetKill.setAlive(false);
            result = targetKill.getName() + " was eliminated by the Mafia. 💀";
        }

        showPassPhone(
                "☀️  Dawn Breaks",
                "Everyone open your eyes.\n\nThe town will learn what happened tonight.",
                () -> {
                    Intent intent = new Intent(this, MafiadayActivity.class);
                    intent.putExtra(MafiadayActivity.EXTRA_PLAYERS, players);
                    intent.putExtra(MafiadayActivity.EXTRA_ROUND, round);
                    intent.putExtra(MafiadayActivity.EXTRA_NIGHT_RESULT, result);
                    startActivity(intent);
                    finish();
                }
        );
    }

    private List<Player> getAlivePlayers() {
        List<Player> alive = new ArrayList<>();
        for (Player p : players) { if (p.isAlive()) alive.add(p); }
        return alive;
    }

    private boolean hasRole(Player.Role role) {
        for (Player p : players) { if (p.getRole() == role && p.isAlive()) return true; }
        return false;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}