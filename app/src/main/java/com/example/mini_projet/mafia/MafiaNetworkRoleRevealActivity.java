package com.example.mini_projet.mafia;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
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


public class MafiaNetworkRoleRevealActivity extends AppCompatActivity
        implements MafiaEventBus.Listener {

    public static final String EXTRA_PLAYERS = "net_players";
    public static final String EXTRA_MY_ID   = "net_my_id";
    public static final String EXTRA_ROUND   = "net_round";

    private ArrayList<Player> players;
    private int    myId;
    private int    round;
    private Player me;

    private Player  actionTarget    = null;
    private boolean actionConfirmed = false;
    private boolean roleRevealed    = false;

    private LinearLayout layoutLock;
    private ScrollView   layoutReveal;
    private TextView     tvLockTitle;
    private TextView     tvLockSubtitle;
    private TextView     tvProgress;
    private TextView     tvRoleEmoji;
    private TextView     tvRoleName;
    private TextView     tvRoleDesc;
    private LinearLayout layoutAction;
    private TextView     tvActionTitle;
    private LinearLayout llActionList;
    private TextView     tvSelectedName;
    private TextView     btnConfirm;
    private TextView     btnCivilianReady;
    private TextView     tvWaiting;
    private TextView     btnShowRole;
    private TextView     btnSeenRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mafia_role_reveal);

        players = (ArrayList<Player>) getIntent().getSerializableExtra(EXTRA_PLAYERS);
        myId    = getIntent().getIntExtra(EXTRA_MY_ID, -1);
        round   = getIntent().getIntExtra(EXTRA_ROUND, 1);

        for (Player p : players) if (p.getId() == myId) { me = p; break; }

        MafiaEventBus.register(this);
        bindViews();
    }

    private void bindViews() {
        layoutLock      = findViewById(R.id.layout_lock);
        layoutReveal    = findViewById(R.id.scroll_role_visible);
        tvLockTitle     = findViewById(R.id.tv_lock_name);
        tvLockSubtitle  = findViewById(R.id.tv_lock_subtitle);
        tvProgress      = findViewById(R.id.tv_progress);
        tvRoleEmoji     = findViewById(R.id.tv_role_emoji);
        tvRoleName      = findViewById(R.id.tv_role_name_label);
        tvRoleDesc      = findViewById(R.id.tv_role_description);
        layoutAction    = findViewById(R.id.layout_action_panel);
        tvActionTitle   = findViewById(R.id.tv_action_title);
        llActionList    = findViewById(R.id.ll_action_player_list);
        tvSelectedName  = findViewById(R.id.tv_action_selected_name);
        btnConfirm      = findViewById(R.id.btn_confirm_action);
        btnCivilianReady= findViewById(R.id.btn_civilian_done);
        tvWaiting       = findViewById(R.id.tv_waiting);
        btnShowRole     = findViewById(R.id.btn_show_role);
        btnSeenRole     = findViewById(R.id.btn_seen_role);

        tvLockTitle.setText(me != null ? me.getName().toUpperCase() + " — your role" : "Your Role");
        tvLockSubtitle.setText("Hold the button below to reveal your secret role");
        tvProgress.setText("Round " + round);

        fillRole();

        btnShowRole.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    layoutLock.setVisibility(View.GONE);
                    layoutReveal.setVisibility(View.VISIBLE);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    layoutReveal.setVisibility(View.GONE);
                    layoutLock.setVisibility(View.VISIBLE);
                    if (!roleRevealed) {
                        roleRevealed = true;
                        btnShowRole.setText("HOLD AGAIN TO RE-CHECK");
                        if (me != null) {
                            switch (me.getRole()) {
                                case MAFIA:
                                    btnSeenRole.setText("🧛  Who do you want to kill?");
                                    break;
                                case DOCTOR:
                                    btnSeenRole.setText("🩺  Who do you want to protect?");
                                    break;
                                case DETECTIVE:
                                    btnSeenRole.setText("🔎  Who do you suspect?");
                                    break;
                                default:
                                    btnSeenRole.setText("✅  I've seen my role");
                                    break;
                            }
                        }
                        btnSeenRole.setVisibility(View.VISIBLE);
                    }
                    return true;
            }
            return false;
        });

        btnSeenRole.setOnClickListener(v -> unlockAction());
        btnConfirm.setOnClickListener(v -> confirmAction());

        layoutReveal.setVisibility(View.GONE);
        layoutAction.setVisibility(View.GONE);
        btnConfirm.setVisibility(View.GONE);
        btnCivilianReady.setVisibility(View.GONE);
        btnSeenRole.setVisibility(View.GONE);
        if (tvWaiting != null) tvWaiting.setVisibility(View.GONE);
    }

    private void fillRole() {
        if (me == null) return;
        switch (me.getRole()) {
            case MAFIA:
                tvRoleEmoji.setText("🧛");
                tvRoleName.setText("MAFIA");
                tvRoleDesc.setText(buildMafiaInfo());
                break;
            case DOCTOR:
                tvRoleEmoji.setText("🩺");
                tvRoleName.setText("DOCTOR");
                tvRoleDesc.setText("Each night, choose one player to protect.\nIf the Mafia targets them, they survive.");
                break;
            case DETECTIVE:
                tvRoleEmoji.setText("🔎");
                tvRoleName.setText("DETECTIVE");
                tvRoleDesc.setText("Each night, investigate one player.\nYou will learn if they are Mafia or not.");
                break;
            default:
                tvRoleEmoji.setText("👤");
                tvRoleName.setText("CIVILIAN");
                tvRoleDesc.setText("You have no special night power.\nHelp identify the Mafia during the day.");
                break;
        }
    }

    private String buildMafiaInfo() {
        StringBuilder sb = new StringBuilder("You are Mafia.\n\nYour team:\n");
        for (Player p : players) {
            if (p.getRole() == Player.Role.MAFIA) {
                sb.append("• ").append(p.getName());
                if (p.getId() == myId) sb.append(" (you)");
                sb.append("\n");
            }
        }
        sb.append("\nChoose one town player to eliminate tonight.");
        return sb.toString().trim();
    }

    private void unlockAction() {
        layoutReveal.setVisibility(View.VISIBLE);
        layoutLock.setVisibility(View.GONE);
        btnSeenRole.setVisibility(View.GONE);

        if (me == null) return;

        switch (me.getRole()) {
            case MAFIA:
                tvActionTitle.setText("🧛  Choose a player to eliminate:");
                buildActionList();
                btnConfirm.setText("CONFIRM KILL");
                btnConfirm.setVisibility(View.VISIBLE);
                layoutAction.setVisibility(View.VISIBLE);
                break;
            case DOCTOR:
                tvActionTitle.setText("🩺  Choose a player to protect:");
                buildActionList();
                btnConfirm.setText("CONFIRM PROTECT");
                btnConfirm.setVisibility(View.VISIBLE);
                layoutAction.setVisibility(View.VISIBLE);
                break;
            case DETECTIVE:
                tvActionTitle.setText("🔎  Choose a player to investigate:");
                buildActionList();
                btnConfirm.setText("INVESTIGATE");
                btnConfirm.setVisibility(View.VISIBLE);
                layoutAction.setVisibility(View.VISIBLE);
                break;
            case CIVILIAN:
                layoutAction.setVisibility(View.GONE);
                btnConfirm.setVisibility(View.GONE);
                btnCivilianReady.setText("GOT IT — WAITING FOR NIGHT RESULT");
                btnCivilianReady.setVisibility(View.VISIBLE);
                btnCivilianReady.setOnClickListener(v -> showWaiting());
                break;
        }
    }

    private void buildActionList() {
        llActionList.removeAllViews();
        for (Player p : players) {
            if (!p.isAlive()) continue;
            if (me.getRole() == Player.Role.MAFIA && p.getRole() == Player.Role.MAFIA) continue;

            TextView row = new TextView(this);
            row.setText("    " + p.getName());
            row.setTextSize(15);
            row.setTextColor(0xFF8A9BC4);
            row.setPadding(dp(8), dp(14), dp(8), dp(14));
            row.setBackgroundResource(R.drawable.bg_player_item);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(6);
            row.setLayoutParams(lp);
            row.setOnClickListener(v -> {
                actionTarget = p;
                tvSelectedName.setText("Selected: " + p.getName());
                for (int i = 0; i < llActionList.getChildCount(); i++)
                    llActionList.getChildAt(i).setAlpha(0.45f);
                row.setAlpha(1f);
            });
            llActionList.addView(row);
        }
    }

    private void confirmAction() {
        if (actionTarget == null) {
            Toast.makeText(this, "Select a player first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (actionConfirmed) return;
        actionConfirmed = true;

        if (me.getRole() == Player.Role.DETECTIVE) {
            boolean isMafia = actionTarget.getRole() == Player.Role.MAFIA;
            Toast.makeText(this,
                    isMafia ? "🧛 " + actionTarget.getName() + " IS Mafia!"
                            : "✅ " + actionTarget.getName() + " is NOT Mafia",
                    Toast.LENGTH_LONG).show();
        }

        showWaiting();
    }

    private void showWaiting() {
        layoutAction.setVisibility(View.GONE);
        btnConfirm.setVisibility(View.GONE);
        btnCivilianReady.setVisibility(View.GONE);
        layoutReveal.setVisibility(View.GONE);
        layoutLock.setVisibility(View.GONE);

        if (tvWaiting == null) {
            ScrollView sv = new ScrollView(this);
            sv.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_dark));
            LinearLayout root = new LinearLayout(this);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setGravity(Gravity.CENTER);
            root.setPadding(dp(28), dp(80), dp(28), dp(28));
            sv.addView(root);

            TextView em = new TextView(this);
            em.setText("🌙");
            em.setTextSize(52);
            em.setGravity(Gravity.CENTER);
            root.addView(em);

            TextView tw = new TextView(this);
            tw.setText("Night is falling...\n\nWaiting for the host to resolve the night.");
            tw.setTextSize(15);
            tw.setTextColor(0xFF8A9BC4);
            tw.setGravity(Gravity.CENTER);
            tw.setLineSpacing(0, 1.6f);
            root.addView(tw);

            setContentView(sv);
        } else {
            tvWaiting.setText("🌙  Night is falling...\n\nWaiting for the host to resolve the night.");
            tvWaiting.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onEvent(String type, String payload) {
        runOnUiThread(() -> {
            switch (type) {
                case MafiaEventBus.EVENT_NIGHT_RESULT: {
                    Intent i = new Intent(this, MafiaNetworkNightResultActivity.class);
                    i.putExtra("result",  payload);
                    i.putExtra("players", players);
                    i.putExtra("my_id",   myId);
                    i.putExtra("round",   round);
                    startActivity(i);
                    finish();
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

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        MafiaEventBus.unregister(this);
        super.onDestroy();
    }
}