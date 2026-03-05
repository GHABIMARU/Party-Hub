package com.example.mini_projet.mafia;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.mini_projet.R;

import java.util.ArrayList;
import java.util.List;


public class MafiaRoleRevealActivity extends AppCompatActivity {

    public static final String EXTRA_PLAYERS = "extra_players";
    public static final String EXTRA_ROUND   = "extra_round";
    public static final String EXTRA_IS_HOST = "is_host";

    // ── Views: Lock screen ────────────────────────────────────────────────────
    private LinearLayout layout_lock;
    private TextView     tv_lock_name;
    private TextView     tv_lock_subtitle;
    private TextView     tv_progress;
    private TextView     btn_show_role;
    private TextView     btn_seen_role;

    // ── Views: Role + Action screen ───────────────────────────────────────────
    private ScrollView   scroll_role_visible;
    private TextView     tv_role_emoji;
    private TextView     tv_role_name_label;
    private TextView     tv_role_description;

    // ── Views: Action panel ───────────────────────────────────────────────────
    private LinearLayout layout_action_panel;
    private TextView     tv_action_title;
    private LinearLayout ll_action_player_list;
    private LinearLayout layout_action_selected;
    private TextView     tv_action_selected_name;
    private TextView     btn_confirm_action;

    // ── Views: Civilian ───────────────────────────────────────────────────────
    private TextView btn_civilian_done;

    // ── State ─────────────────────────────────────────────────────────────────
    private ArrayList<Player> players;
    private int     round        = 1;
    private int     currentIndex = 0;
    private boolean isHost       = false;

    private Player targetKill        = null;
    private Player targetSave        = null;
    private Player targetInvestigate = null;
    private Player actionSelection   = null;
    private boolean roleHasBeenSeen  = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mafia_role_reveal);

        players = (ArrayList<Player>) getIntent().getSerializableExtra(EXTRA_PLAYERS);
        round   = getIntent().getIntExtra(EXTRA_ROUND, 1);
        isHost  = getIntent().getBooleanExtra(EXTRA_IS_HOST, false);

        bindViews();
        showLockScreen(0);
    }

    // ── Bind views ────────────────────────────────────────────────────────────
    private void bindViews() {
        layout_lock           = findViewById(R.id.layout_lock);
        tv_lock_name          = findViewById(R.id.tv_lock_name);
        tv_lock_subtitle      = findViewById(R.id.tv_lock_subtitle);
        tv_progress           = findViewById(R.id.tv_progress);
        btn_show_role         = findViewById(R.id.btn_show_role);
        btn_seen_role         = findViewById(R.id.btn_seen_role);

        scroll_role_visible   = findViewById(R.id.scroll_role_visible);
        tv_role_emoji         = findViewById(R.id.tv_role_emoji);
        tv_role_name_label    = findViewById(R.id.tv_role_name_label);
        tv_role_description   = findViewById(R.id.tv_role_description);

        layout_action_panel   = findViewById(R.id.layout_action_panel);
        tv_action_title       = findViewById(R.id.tv_action_title);
        ll_action_player_list = findViewById(R.id.ll_action_player_list);
        layout_action_selected= findViewById(R.id.layout_action_selected);
        tv_action_selected_name = findViewById(R.id.tv_action_selected_name);
        btn_confirm_action    = findViewById(R.id.btn_confirm_action);
        btn_civilian_done     = findViewById(R.id.btn_civilian_done);

        setupHoldToReveal();
        btn_seen_role.setOnClickListener(v -> unlockAction());
        btn_confirm_action.setOnClickListener(v -> confirmAction());
        btn_civilian_done.setOnClickListener(v -> advanceToNextPlayer());
    }

    // ── STEP 1: Lock screen ───────────────────────────────────────────────────
    private void showLockScreen(int index) {
        currentIndex    = index;
        actionSelection = null;
        roleHasBeenSeen = false;

        List<Player> alive = getAlivePlayers();
        Player p = alive.get(index);
        tv_progress.setText((index + 1) + " / " + alive.size());
        tv_lock_name.setText(p.getName() + ", it's your turn");
        tv_lock_subtitle.setText("Pass the phone to " + p.getName()
                + ".\nNobody else should look at the screen.");

        btn_show_role.setText("👁  HOLD TO SEE YOUR ROLE");
        btn_seen_role.setVisibility(View.GONE);

        layout_lock.setVisibility(View.VISIBLE);
        scroll_role_visible.setVisibility(View.GONE);
        layout_action_panel.setVisibility(View.GONE);
        btn_confirm_action.setVisibility(View.GONE);
        btn_civilian_done.setVisibility(View.GONE);
        layout_action_selected.setVisibility(View.INVISIBLE);

        fillRoleContent(p);
    }

    // ── STEP 2: Hold-to-reveal ────────────────────────────────────────────────
    private void setupHoldToReveal() {
        btn_show_role.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    layout_lock.setVisibility(View.GONE);
                    scroll_role_visible.setVisibility(View.VISIBLE);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    scroll_role_visible.setVisibility(View.GONE);
                    layout_lock.setVisibility(View.VISIBLE);
                    if (!roleHasBeenSeen) {
                        roleHasBeenSeen = true;
                        btn_show_role.setText("👁  HOLD AGAIN TO RE-CHECK");
                        List<Player> alive = getAlivePlayers();
                        if (currentIndex < alive.size()) {
                            switch (alive.get(currentIndex).getRole()) {
                                case MAFIA:
                                    btn_seen_role.setText("🔴  Who do you want to kill?");
                                    break;
                                case DOCTOR:
                                    btn_seen_role.setText("🩺  Who do you want to protect?");
                                    break;
                                case DETECTIVE:
                                    btn_seen_role.setText("🔎  Who do you suspect?");
                                    break;
                                default:
                                    // Civilian — skip action screen, just pass phone
                                    boolean isLast = (currentIndex == alive.size() - 1);
                                    btn_seen_role.setText(isLast
                                            ? "📱  Pass the phone — Start Day ☀️"
                                            : "📱  Pass the phone to the next person →");
                                    break;
                            }
                        }
                        btn_seen_role.setVisibility(View.VISIBLE);
                    }
                    return true;
            }
            return false;
        });
    }

    // ── Pre-fill role card ────────────────────────────────────────────────────
    private void fillRoleContent(Player p) {
        switch (p.getRole()) {
            case MAFIA:
                tv_role_emoji.setText("🔴");
                tv_role_name_label.setText("MAFIA");
                tv_role_description.setText(buildMafiaTeamInfo(p));
                break;
            case DOCTOR:
                tv_role_emoji.setText("🩺");
                tv_role_name_label.setText("DOCTOR");
                tv_role_description.setText("Each night, choose one player to protect.\nIf the Mafia targets them, they survive.\n\nYou may protect yourself.");
                break;
            case DETECTIVE:
                tv_role_emoji.setText("🔎");
                tv_role_name_label.setText("DETECTIVE");
                tv_role_description.setText("Each night, investigate one player.\nYou will learn if they are Mafia or not.\n\nUse this knowledge wisely in the day phase.");
                break;
            default:
                tv_role_emoji.setText("👤");
                tv_role_name_label.setText("CIVILIAN");
                tv_role_description.setText("You have no special night power.\nListen carefully during the day phase\nand help identify the Mafia.");
                break;
        }
    }

    // ── STEP 3: Unlock action panel ───────────────────────────────────────────
    private void unlockAction() {
        List<Player> alive = getAlivePlayers();
        Player p = alive.get(currentIndex);

        // Civilian — skip the role screen entirely, advance straight away
        if (p.getRole() == Player.Role.CIVILIAN) {
            advanceToNextPlayer();
            return;
        }

        boolean isLast = (currentIndex == alive.size() - 1);
        btn_confirm_action.setText(isLast ? "CONFIRM — START DAY  ☀️" : "CONFIRM — PASS PHONE  →");

        layout_lock.setVisibility(View.GONE);
        scroll_role_visible.setVisibility(View.VISIBLE);

        switch (p.getRole()) {
            case MAFIA:
                showActionPanel("🔴  Choose a player to eliminate tonight:", p);
                break;
            case DOCTOR:
                showActionPanel("🩺  Choose a player to protect tonight:", p);
                break;
            case DETECTIVE:
                showActionPanel("🔎  Choose a player to investigate:", p);
                break;
        }
    }

    // ── Action panel ──────────────────────────────────────────────────────────
    private void showActionPanel(String title, Player actor) {
        tv_action_title.setText(title);
        layout_action_panel.setVisibility(View.VISIBLE);
        btn_confirm_action.setVisibility(View.VISIBLE);
        btn_civilian_done.setVisibility(View.GONE);

        ll_action_player_list.removeAllViews();
        for (Player target : getAlivePlayers()) {
            if (actor.getRole() == Player.Role.MAFIA
                    && target.getRole() == Player.Role.MAFIA) continue;

            LinearLayout row = buildActionRow(target, actor.getRole());
            ll_action_player_list.addView(row);
        }
    }

    private LinearLayout buildActionRow(Player target, Player.Role actorRole) {
        // Pick highlight color based on role
        int highlightColor;
        switch (actorRole) {
            case MAFIA:      highlightColor = 0xFFCC0000; break; // blood red
            case DOCTOR:     highlightColor = 0xFF2ECC71; break; // green
            case DETECTIVE:  highlightColor = 0xFF3B9EFF; break; // blue
            default:         highlightColor = 0xFFF0B429; break; // gold fallback
        }

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_card);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(6);
        row.setLayoutParams(lp);
        row.setClickable(true);
        row.setFocusable(true);

        TextView tv = new TextView(this);
        tv.setText(target.getName());
        tv.setTextSize(15);
        tv.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(tv);

        row.setOnClickListener(v -> {
            actionSelection = target;
            tv_action_selected_name.setText("Selected: " + target.getName());
            layout_action_selected.setVisibility(View.VISIBLE);
            // Reset all rows back to default
            for (int i = 0; i < ll_action_player_list.getChildCount(); i++) {
                LinearLayout r = (LinearLayout) ll_action_player_list.getChildAt(i);
                r.setAlpha(1f);
                ((TextView) r.getChildAt(0)).setTextColor(
                        ContextCompat.getColor(this, R.color.text_secondary));
                android.graphics.drawable.GradientDrawable defaultBg =
                        new android.graphics.drawable.GradientDrawable();
                defaultBg.setColor(0xFF1E2235);
                defaultBg.setCornerRadius(dp(12));
                r.setBackground(defaultBg);
            }
            // Paint selected row with role color
            android.graphics.drawable.GradientDrawable selectedBg =
                    new android.graphics.drawable.GradientDrawable();
            selectedBg.setColor(highlightColor);
            selectedBg.setCornerRadius(dp(12));
            row.setBackground(selectedBg);
            tv.setTextColor(0xFFFFFFFF);
        });
        return row;
    }

    // ── Confirm action ────────────────────────────────────────────────────────
    private void confirmAction() {
        if (actionSelection == null) {
            android.widget.Toast.makeText(this,
                    "Select a player first", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        List<Player> alive = getAlivePlayers();
        Player current = alive.get(currentIndex);

        switch (current.getRole()) {
            case MAFIA:
                targetKill = actionSelection;
                advanceToNextPlayer();
                break;

            case DOCTOR:
                targetSave = actionSelection;
                advanceToNextPlayer();
                break;

            case DETECTIVE:
                targetInvestigate = actionSelection;
                boolean isMafia = actionSelection.getRole() == Player.Role.MAFIA;
                showDetectiveResult(actionSelection.getName(), isMafia);
                break;
        }
    }

    // ── Detective result — rich full-screen card ──────────────────────────────
    private void showDetectiveResult(String suspectName, boolean isMafia) {
        // Build custom view
        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setGravity(android.view.Gravity.CENTER);
        root.setBackgroundColor(isMafia ? 0xFF1A0A0A : 0xFF0A1A0F);
        root.setPadding(dp(32), dp(48), dp(32), dp(40));

        // Big result icon
        android.widget.TextView tvIcon = new android.widget.TextView(this);
        tvIcon.setText(isMafia ? "🧛‍♀️" : "😇");
        tvIcon.setTextSize(72);
        tvIcon.setGravity(android.view.Gravity.CENTER);
        android.widget.LinearLayout.LayoutParams iconLp =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        iconLp.bottomMargin = dp(20);
        tvIcon.setLayoutParams(iconLp);
        root.addView(tvIcon);

        // Verdict label
        android.widget.TextView tvVerdict = new android.widget.TextView(this);
        tvVerdict.setText(isMafia ? "MAFIA MEMBER" : "INNOCENT");
        tvVerdict.setTextSize(26);
        tvVerdict.setTypeface(null, android.graphics.Typeface.BOLD);
        tvVerdict.setLetterSpacing(0.2f);
        tvVerdict.setGravity(android.view.Gravity.CENTER);
        tvVerdict.setTextColor(isMafia ? 0xFFFF4444 : 0xFF38B2AC);
        android.widget.LinearLayout.LayoutParams vLp =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        vLp.bottomMargin = dp(16);
        tvVerdict.setLayoutParams(vLp);
        root.addView(tvVerdict);

        // Suspect name
        android.widget.TextView tvName = new android.widget.TextView(this);
        tvName.setText(suspectName);
        tvName.setTextSize(20);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        tvName.setGravity(android.view.Gravity.CENTER);
        tvName.setTextColor(0xFFF0F4FF);
        android.widget.LinearLayout.LayoutParams nLp =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        nLp.bottomMargin = dp(12);
        tvName.setLayoutParams(nLp);
        root.addView(tvName);

        // Description line
        android.widget.TextView tvDesc = new android.widget.TextView(this);
        tvDesc.setText(isMafia
                ? suspectName + " is a member of the Mafia.\n\n⚠️  Keep this secret.\nDon't reveal it directly during the day."
                : suspectName + " is not part of the Mafia.\n\n🔎  Keep investigating.\nDon't reveal it directly during the day.");
        tvDesc.setTextSize(13);
        tvDesc.setGravity(android.view.Gravity.CENTER);
        tvDesc.setTextColor(0xFF8A9BC4);
        tvDesc.setLineSpacing(0, 1.6f);
        android.widget.LinearLayout.LayoutParams dLp =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        dLp.bottomMargin = dp(48);
        tvDesc.setLayoutParams(dLp);
        root.addView(tvDesc);

        // Confirm button
        android.widget.TextView btnOk = new android.widget.TextView(this);
        btnOk.setText("🔒  HIDE SCREEN — PASS PHONE");
        btnOk.setTextSize(14);
        btnOk.setTypeface(null, android.graphics.Typeface.BOLD);
        btnOk.setGravity(android.view.Gravity.CENTER);
        btnOk.setTextColor(ContextCompat.getColor(this, R.color.bg_dark));
        btnOk.setBackgroundResource(R.drawable.bg_button_gold);
        btnOk.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(56)));

        root.addView(btnOk);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(root)
                .setCancelable(false)
                .create();

        btnOk.setOnClickListener(v -> {
            dialog.dismiss();
            advanceToNextPlayer();
        });

        dialog.show();
    }

    // ── Advance to next player ────────────────────────────────────────────────
    private void advanceToNextPlayer() {
        int next = currentIndex + 1;
        if (next < getAlivePlayers().size()) {
            showLockScreen(next);
        } else {
            resolveNightAndGoToDay();
        }
    }

    // ── Resolve night → go to day ─────────────────────────────────────────────
    private void resolveNightAndGoToDay() {
        boolean saved = (targetSave != null && targetKill != null
                && targetSave.getId() == targetKill.getId());

        String result;
        if (targetKill == null) {
            result = "The Mafia did not strike tonight. 🌙";
        } else if (saved) {
            result = targetKill.getName() + " was targeted but saved by the Doctor! 🩺";
        } else {
            targetKill.setAlive(false);
            result = targetKill.getName() + " was eliminated by the Mafia. 💀";
        }

        // ── Broadcast to all clients if we are the host ───────────────────────
        if (isHost && MafiaServerHolder.isHosting()) {
            MafiaNetworkServer server = MafiaServerHolder.get();
            server.broadcastNightResult(result);
            // STATE:DAY is broadcast from MafiadayActivity once the host
            // taps "Continue" on the night result and reaches the day screen
        }

        // Host device navigates to day
        Intent intent = new Intent(this, MafiadayActivity.class);
        intent.putExtra(MafiadayActivity.EXTRA_PLAYERS, new ArrayList<>(getAlivePlayers()));
        intent.putExtra(MafiadayActivity.EXTRA_ROUND, round);
        intent.putExtra(MafiadayActivity.EXTRA_NIGHT_RESULT, result);
        intent.putExtra(MafiadayActivity.EXTRA_IS_HOST, isHost);
        startActivity(intent);
        finish();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String buildMafiaTeamInfo(Player current) {
        StringBuilder sb = new StringBuilder("You are Mafia.\n\nYour team:\n");
        for (Player p : players) {
            if (p.getRole() == Player.Role.MAFIA) {
                sb.append("• ").append(p.getName());
                if (p.getId() == current.getId()) sb.append(" (you)");
                sb.append("\n");
            }
        }
        sb.append("\nChoose one town player to eliminate tonight.\n");
        sb.append("You cannot target your own Mafia teammates.\n");
        sb.append("If multiple Mafia, the last one to confirm sets the final target.");
        return sb.toString().trim();
    }

    private List<Player> getAlivePlayers() {
        List<Player> alive = new ArrayList<>();
        for (Player p : players) { if (p.isAlive()) alive.add(p); }
        return alive;
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}