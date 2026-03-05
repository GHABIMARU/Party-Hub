package com.example.mini_projet.mafia;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MafiadayActivity extends AppCompatActivity
        implements MafiaNetworkServer.VoteCallback {

    public static final String EXTRA_PLAYERS      = "extra_players";
    public static final String EXTRA_ROUND        = "extra_round";
    public static final String EXTRA_NIGHT_RESULT = "extra_night_result";
    public static final String EXTRA_IS_HOST      = "is_host";

    private TextView     tv_day_round;
    private TextView     tv_night_result_emoji;
    private TextView     tv_night_result_text;
    private LinearLayout ll_vote_list;
    private TextView     tv_votes_remaining;
    private TextView     tv_leading_name;
    private TextView     tv_leading_count;
    private TextView     btn_eliminate;
    private TextView     btn_skip_vote;

    private ArrayList<Player> players;
    private int    round;
    private String nightResultText;
    private boolean isHost = false;

    private final Map<Integer, Integer> voteMap   = new HashMap<>();
    private final Map<Integer, TextView> voteBadges = new HashMap<>();
    private int totalVotes  = 0;
    private int totalVoters = 0;
    private int currentVoterIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mafia_day);

        players         = (ArrayList<Player>) getIntent().getSerializableExtra(EXTRA_PLAYERS);
        round           = getIntent().getIntExtra(EXTRA_ROUND, 1);
        nightResultText = getIntent().getStringExtra(EXTRA_NIGHT_RESULT);
        isHost          = getIntent().getBooleanExtra(EXTRA_IS_HOST, false);

        if (isHost && MafiaServerHolder.isHosting()) {
            MafiaServerHolder.get().setVoteCallback(this);
            MafiaServerHolder.get().broadcastState("DAY");
        }

        bindViews();
        setupNightResult();
        buildVoteList();
        checkWinCondition();
    }

    private void bindViews() {
        tv_day_round          = findViewById(R.id.tv_day_round);
        tv_night_result_emoji = findViewById(R.id.tv_night_result_emoji);
        tv_night_result_text  = findViewById(R.id.tv_night_result_text);
        ll_vote_list          = findViewById(R.id.ll_vote_list);
        tv_votes_remaining    = findViewById(R.id.tv_votes_remaining);
        tv_leading_name       = findViewById(R.id.tv_leading_name);
        tv_leading_count      = findViewById(R.id.tv_leading_count);
        btn_eliminate         = findViewById(R.id.btn_eliminate);
        btn_skip_vote         = findViewById(R.id.btn_skip_vote);

        tv_day_round.setText("ROUND " + round);
        btn_eliminate.setOnClickListener(v -> confirmElimination());
        btn_skip_vote.setOnClickListener(v -> confirmSkip());
    }

    private void setupNightResult() {
        if (nightResultText == null) return;
        if (nightResultText.contains("saved")) {
            tv_night_result_emoji.setText("🩺");
        } else if (nightResultText.contains("eliminated")) {
            tv_night_result_emoji.setText("💀");
            int mafiaAlive = 0;
            for (Player p : players)
                if (p.isAlive() && p.getRole() == Player.Role.MAFIA) mafiaAlive++;
            nightResultText += "\n" + mafiaAlive + " Mafia member"
                    + (mafiaAlive == 1 ? "" : "s") + " still hiding.";
        } else {
            tv_night_result_emoji.setText("🌙");
        }
        tv_night_result_text.setText(nightResultText);
    }

    private void buildVoteList() {
        ll_vote_list.removeAllViews();
        voteMap.clear();
        voteBadges.clear();
        totalVotes  = 0;

        List<Player> alive = getAlivePlayers();
        totalVoters = alive.size();

        for (Player p : alive) {
            voteMap.put(p.getId(), 0);
            ll_vote_list.addView(buildVoteCard(p));
        }
        refreshTally();
        refreshVotesRemaining();
    }

    private LinearLayout buildVoteCard(Player player) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_card);
        row.setPadding(dpToPx(16), dpToPx(14), dpToPx(12), dpToPx(14));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, 0, 0, dpToPx(8));
        row.setLayoutParams(rowLp);

        TextView tvName = new TextView(this);
        tvName.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvName.setText(player.getName());
        tvName.setTextSize(15);
        tvName.setTypeface(null, Typeface.BOLD);
        tvName.setTextColor(ContextCompat.getColor(this, R.color.text_primary));

        TextView tvBadge = new TextView(this);
        tvBadge.setText("0");
        tvBadge.setTextSize(18);
        tvBadge.setTypeface(null, Typeface.BOLD);
        tvBadge.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        tvBadge.setGravity(Gravity.CENTER);
        tvBadge.setMinWidth(dpToPx(36));
        voteBadges.put(player.getId(), tvBadge);

        TextView btnAdd = makeCircleBtn("＋");
        btnAdd.setOnClickListener(v -> { addVote(player.getId()); refreshCard(player.getId(), tvBadge); });

        TextView btnSub = makeCircleBtn("－");
        btnSub.setOnClickListener(v -> { removeVote(player.getId()); refreshCard(player.getId(), tvBadge); });

        row.addView(tvName);
        row.addView(tvBadge);
        row.addView(btnSub);
        row.addView(btnAdd);
        return row;
    }

    private TextView makeCircleBtn(String label) {
        TextView btn = new TextView(this);
        int size = dpToPx(36);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.setMargins(dpToPx(4), 0, 0, 0);
        btn.setLayoutParams(lp);
        btn.setText(label);
        btn.setTextSize(16);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setTextColor(ContextCompat.getColor(this, R.color.gold));
        btn.setGravity(Gravity.CENTER);
        btn.setBackgroundResource(R.drawable.bg_card);
        btn.setClickable(true);
        btn.setFocusable(true);
        return btn;
    }

    private void addVote(int playerId) {
        if (totalVotes >= totalVoters) {
            Toast.makeText(this, "All votes already cast", Toast.LENGTH_SHORT).show();
            return;
        }
        int current = voteMap.getOrDefault(playerId, 0);
        if (current >= totalVoters - 1) {
            Toast.makeText(this, "A player cannot vote for themselves — max "
                    + (totalVoters - 1) + " votes", Toast.LENGTH_SHORT).show();
            return;
        }
        voteMap.put(playerId, current + 1);
        totalVotes++;
        refreshTally();
        refreshVotesRemaining();
    }

    private void removeVote(int playerId) {
        int current = voteMap.getOrDefault(playerId, 0);
        if (current <= 0) return;
        voteMap.put(playerId, current - 1);
        totalVotes--;
        refreshTally();
        refreshVotesRemaining();
    }

    @Override
    public void onVoteReceived(int voterId, int targetId) {
        runOnUiThread(() -> {
            addVote(targetId);
            if (isHost && totalVotes >= totalVoters) {
                confirmElimination();
            }
        });
    }

    private void refreshCard(int playerId, TextView badge) {
        int votes = voteMap.getOrDefault(playerId, 0);
        badge.setText(String.valueOf(votes));
        badge.setTextColor(ContextCompat.getColor(this,
                votes > 0 ? R.color.gold : R.color.text_secondary));
    }

    private void refreshTally() {
        Player leader = getLeader();
        if (leader == null || voteMap.getOrDefault(leader.getId(), 0) == 0) {
            tv_leading_name.setText("—");
            tv_leading_count.setText("0 votes");
        } else {
            int votes = voteMap.getOrDefault(leader.getId(), 0);
            tv_leading_name.setText(leader.getName());
            tv_leading_count.setText(votes + (votes == 1 ? " vote" : " votes"));
        }
    }

    private void refreshVotesRemaining() {
        int remaining = totalVoters - totalVotes;
        tv_votes_remaining.setText(remaining == 0
                ? "All votes cast ✓"
                : remaining + (remaining == 1 ? " vote remaining" : " votes remaining"));
    }
    private void confirmElimination() {
        if (voteMap.isEmpty() || totalVotes == 0) {
            Toast.makeText(this, "No votes cast yet", Toast.LENGTH_SHORT).show();
            return;
        }

        int maxVotes = 0;
        for (int v : voteMap.values()) if (v > maxVotes) maxVotes = v;

        List<Player> tied = new ArrayList<>();
        for (Player p : getAlivePlayers()) {
            if (voteMap.getOrDefault(p.getId(), 0) == maxVotes) tied.add(p);
        }

        if (tied.size() > 1) {
            showDrawDialog(tied);
            return;
        }

        Player leader = tied.get(0);
        leader.setAlive(false);

        if (isHost && MafiaServerHolder.isHosting()) {
            MafiaServerHolder.get().broadcastEliminated(leader.getName(), leader.getRole().name());
            MafiaServerHolder.get().broadcastLobby();
        }

        showEliminationDialog(leader);
    }

    private void showDrawDialog(List<Player> tied) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(0xFF12141F);
        root.setPadding(dpToPx(32), dpToPx(40), dpToPx(32), dpToPx(32));

        TextView tvIcon = new TextView(this);
        tvIcon.setText("⚖️");
        tvIcon.setTextSize(64);
        tvIcon.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams iLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        iLp.bottomMargin = dpToPx(16);
        tvIcon.setLayoutParams(iLp);
        root.addView(tvIcon);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("IT'S A DRAW");
        tvTitle.setTextSize(24);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setLetterSpacing(0.2f);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setTextColor(0xFFF0B429);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tLp.bottomMargin = dpToPx(10);
        tvTitle.setLayoutParams(tLp);
        root.addView(tvTitle);

        StringBuilder names = new StringBuilder();
        for (Player p : tied) names.append("• ").append(p.getName()).append("\n");
        TextView tvTied = new TextView(this);
        tvTied.setText(names.toString().trim());
        tvTied.setTextSize(15);
        tvTied.setGravity(Gravity.CENTER);
        tvTied.setTextColor(0xFFF0F4FF);
        tvTied.setLineSpacing(0, 1.5f);
        LinearLayout.LayoutParams nLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        nLp.bottomMargin = dpToPx(12);
        tvTied.setLayoutParams(nLp);
        root.addView(tvTied);

        TextView tvSub = new TextView(this);
        tvSub.setText("No one is eliminated.\nThe night begins.");
        tvSub.setTextSize(13);
        tvSub.setGravity(Gravity.CENTER);
        tvSub.setTextColor(0xFF8A9BC4);
        tvSub.setLineSpacing(0, 1.6f);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sLp.bottomMargin = dpToPx(40);
        tvSub.setLayoutParams(sLp);
        root.addView(tvSub);

        TextView btnContinue = new TextView(this);
        btnContinue.setText("🌙  BEGIN NIGHT");
        btnContinue.setTextSize(14);
        btnContinue.setTypeface(null, android.graphics.Typeface.BOLD);
        btnContinue.setGravity(Gravity.CENTER);
        btnContinue.setTextColor(ContextCompat.getColor(this, R.color.bg_dark));
        btnContinue.setBackgroundResource(R.drawable.bg_button_gold);
        btnContinue.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(56)));
        root.addView(btnContinue);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(root)
                .setCancelable(false)
                .create();

        btnContinue.setOnClickListener(v -> {
            dialog.dismiss();
            goToNightPhase();
        });

        dialog.show();
    }

    private void showEliminationDialog(Player victim) {
        int bgColor, accentColor;
        String emoji, verdict, flavour;
        switch (victim.getRole()) {
            case MAFIA:
                bgColor     = 0xFF1A0A0A;
                accentColor = 0xFFFF4444;
                emoji       = "🧛";
                verdict     = "MAFIA MEMBER";
                flavour     = "The town has rooted out a Mafia member!\nThe town grows safer.";
                break;
            case DOCTOR:
                bgColor     = 0xFF0A1A0F;
                accentColor = 0xFF2ECC71;
                emoji       = "🩺";
                verdict     = "DOCTOR";
                flavour     = "The town has lost its healer.\nThe Mafia grows stronger.";
                break;
            case DETECTIVE:
                bgColor     = 0xFF0A0F1A;
                accentColor = 0xFF3B9EFF;
                emoji       = "🔎";
                verdict     = "DETECTIVE";
                flavour     = "The town has lost its investigator.\nThe Mafia's secrets are safer.";
                break;
            default:
                bgColor     = 0xFF0F0F1A;
                accentColor = 0xFF8A9BC4;
                emoji       = "👤";
                verdict     = "CIVILIAN";
                flavour     = "An innocent civilian has been eliminated.\nThe Mafia hides among the rest.";
                break;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(bgColor);
        root.setPadding(dpToPx(32), dpToPx(40), dpToPx(32), dpToPx(32));

        TextView tvEmoji = new TextView(this);
        tvEmoji.setText(emoji);
        tvEmoji.setTextSize(64);
        tvEmoji.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams eLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        eLp.bottomMargin = dpToPx(16);
        tvEmoji.setLayoutParams(eLp);
        root.addView(tvEmoji);

        TextView tvVerdict = new TextView(this);
        tvVerdict.setText(verdict);
        tvVerdict.setTextSize(24);
        tvVerdict.setTypeface(null, android.graphics.Typeface.BOLD);
        tvVerdict.setLetterSpacing(0.2f);
        tvVerdict.setGravity(Gravity.CENTER);
        tvVerdict.setTextColor(accentColor);
        LinearLayout.LayoutParams vLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        vLp.bottomMargin = dpToPx(10);
        tvVerdict.setLayoutParams(vLp);
        root.addView(tvVerdict);

        TextView tvName = new TextView(this);
        tvName.setText(victim.getName());
        tvName.setTextSize(22);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        tvName.setGravity(Gravity.CENTER);
        tvName.setTextColor(0xFFF0F4FF);
        LinearLayout.LayoutParams nLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        nLp.bottomMargin = dpToPx(16);
        tvName.setLayoutParams(nLp);
        root.addView(tvName);

        TextView tvDesc = new TextView(this);
        tvDesc.setText(flavour);
        tvDesc.setTextSize(13);
        tvDesc.setGravity(Gravity.CENTER);
        tvDesc.setTextColor(0xFF8A9BC4);
        tvDesc.setLineSpacing(0, 1.6f);
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        dLp.bottomMargin = dpToPx(40);
        tvDesc.setLayoutParams(dLp);
        root.addView(tvDesc);

        TextView btnContinue = new TextView(this);
        btnContinue.setText("CONTINUE  →");
        btnContinue.setTextSize(14);
        btnContinue.setTypeface(null, android.graphics.Typeface.BOLD);
        btnContinue.setGravity(Gravity.CENTER);
        btnContinue.setTextColor(ContextCompat.getColor(this, R.color.bg_dark));
        btnContinue.setBackgroundResource(R.drawable.bg_button_gold);
        btnContinue.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(56)));
        root.addView(btnContinue);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(root)
                .setCancelable(false)
                .create();

        btnContinue.setOnClickListener(v -> {
            dialog.dismiss();
            if (!checkWinCondition()) goToNightPhase();
        });

        dialog.show();
    }

    private void confirmSkip() {
        new AlertDialog.Builder(this)
                .setTitle("Skip Voting?")
                .setMessage("No one will be eliminated. Night begins.")
                .setPositiveButton("SKIP", (d, w) -> goToNightPhase())
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private boolean checkWinCondition() {
        int mafiaAlive = 0, townAlive = 0;
        for (Player p : players) {
            if (!p.isAlive()) continue;
            if (p.getRole() == Player.Role.MAFIA) mafiaAlive++;
            else townAlive++;
        }

        if (mafiaAlive == 0) {
            String title = "🏘️  TOWN WINS";
            String msg   = "The Mafia has been eliminated.\nThe town is safe!";
            if (isHost && MafiaServerHolder.isHosting())
                MafiaServerHolder.get().broadcastGameOver(title, msg);
            showGameOver(title, msg);
            return true;
        }
        if (mafiaAlive >= townAlive) {
            String title = "🧛  MAFIA WINS";
            String msg   = "The Mafia controls the town.\n\n" + buildMafiaReveal();
            if (isHost && MafiaServerHolder.isHosting())
                MafiaServerHolder.get().broadcastGameOver(title, msg);
            showGameOver(title, msg);
            return true;
        }
        return false;
    }

    private String buildMafiaReveal() {
        StringBuilder sb = new StringBuilder("Mafia members were:\n");
        for (Player p : players)
            if (p.getRole() == Player.Role.MAFIA)
                sb.append("• ").append(p.getName()).append("\n");
        return sb.toString().trim();
    }

    private void showGameOver(String title, String message) {
        btn_eliminate.setClickable(false);
        btn_skip_vote.setClickable(false);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dpToPx(24), dpToPx(8), dpToPx(24), dpToPx(24));

        TextView tvMsg = new TextView(this);
        tvMsg.setText(message);
        tvMsg.setTextSize(14);
        tvMsg.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        tvMsg.setLineSpacing(0, 1.4f);
        tvMsg.setPadding(0, 0, 0, dpToPx(24));
        container.addView(tvMsg);

        TextView btnPlayAgain = new TextView(this);
        btnPlayAgain.setText("PLAY AGAIN");
        btnPlayAgain.setTextSize(14);
        btnPlayAgain.setTypeface(null, Typeface.BOLD);
        btnPlayAgain.setTextColor(ContextCompat.getColor(this, R.color.bg_dark));
        btnPlayAgain.setGravity(Gravity.CENTER);
        btnPlayAgain.setBackgroundResource(R.drawable.bg_button_gold);
        LinearLayout.LayoutParams lpA = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(52));
        lpA.setMargins(0, 0, 0, dpToPx(12));
        btnPlayAgain.setLayoutParams(lpA);

        TextView btnMainMenu = new TextView(this);
        btnMainMenu.setText("MAIN MENU");
        btnMainMenu.setTextSize(14);
        btnMainMenu.setTypeface(null, Typeface.BOLD);
        btnMainMenu.setTextColor(ContextCompat.getColor(this, R.color.gold));
        btnMainMenu.setGravity(Gravity.CENTER);
        btnMainMenu.setBackgroundResource(R.drawable.bg_button_outline);
        btnMainMenu.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(52)));

        container.addView(btnPlayAgain);
        container.addView(btnMainMenu);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(container)
                .setCancelable(false)
                .create();

        btnPlayAgain.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, Mafiasetupactivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });

        btnMainMenu.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, com.example.mini_projet.HomeActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            finish();
        });

        dialog.show();
    }

    private void goToNightPhase() {
        if (isHost && MafiaServerHolder.isHosting()) {
            MafiaServerHolder.get().broadcastState("ROLE_REVEAL");
        }

        Intent intent = new Intent(this, MafiaRoleRevealActivity.class);
        intent.putExtra(MafiaRoleRevealActivity.EXTRA_PLAYERS, new ArrayList<>(getAlivePlayers()));
        intent.putExtra(MafiaRoleRevealActivity.EXTRA_ROUND, round + 1);
        intent.putExtra(MafiaRoleRevealActivity.EXTRA_IS_HOST, isHost);
        startActivity(intent);
        finish();
    }

    private Player getLeader() {
        int maxVotes = 0;
        for (int v : voteMap.values()) if (v > maxVotes) maxVotes = v;
        if (maxVotes == 0) return null;
        Player leader = null;
        int count = 0;
        for (Player p : getAlivePlayers()) {
            if (voteMap.getOrDefault(p.getId(), 0) == maxVotes) { leader = p; count++; }
        }
        return count == 1 ? leader : null; // null = draw
    }

    private List<Player> getAlivePlayers() {
        List<Player> alive = new ArrayList<>();
        for (Player p : players) if (p.isAlive()) alive.add(p);
        return alive;
    }

    private String getRoleEmoji(Player.Role role) {
        switch (role) {
            case MAFIA:     return "🧛";
            case DOCTOR:    return "🩺";
            case DETECTIVE: return "🔎";
            default:        return "👤";
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}