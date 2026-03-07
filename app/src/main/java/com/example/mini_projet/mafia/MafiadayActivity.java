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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mafia_day);

        android.widget.FrameLayout dayRoot =
                (android.widget.FrameLayout) getWindow().getDecorView();
        DaySkyView daySky = new DaySkyView(this);
        daySky.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
        dayRoot.addView(daySky, 0);

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

        if (nightResultText != null) {
            getWindow().getDecorView().post(() -> showNightResultCinematic());
        }
    }

    private void showNightResultCinematic() {
        boolean killed  = nightResultText.contains("eliminated");
        boolean saved   = nightResultText.contains("saved");

        int bgColor     = killed ? 0xFF0D0000 : saved ? 0xFF001A08 : 0xFF020818;
        int accentColor = killed ? 0xFFFF1111 : saved ? 0xFF2ECC71 : 0xFF8AB4FF;
        String bigEmoji = killed ? "💀"        : saved ? "🧑‍⚕️"        : "🌙";
        String headline = killed ? getString(R.string.mafia_day_someone_fell)
                : saved  ? getString(R.string.mafia_day_doctor_saved)
                : getString(R.string.mafia_day_quiet_night);

        android.widget.FrameLayout overlay = new android.widget.FrameLayout(this);
        overlay.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
        overlay.setBackgroundColor(bgColor);
        overlay.setElevation(dpToPx(200));

        NightResultParticleView particles = new NightResultParticleView(this, killed, saved);
        overlay.addView(particles, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);
        center.setPadding(dpToPx(32), 0, dpToPx(32), 0);
        overlay.addView(center, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));

        TextView tvIcon = new TextView(this);
        tvIcon.setText(bigEmoji);
        tvIcon.setTextSize(88);
        tvIcon.setGravity(Gravity.CENTER);
        tvIcon.setScaleX(0f); tvIcon.setScaleY(0f); tvIcon.setAlpha(0f);
        LinearLayout.LayoutParams iLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        iLp.bottomMargin = dpToPx(20);
        tvIcon.setLayoutParams(iLp);
        center.addView(tvIcon);

        TextView tvHeadline = new TextView(this);
        tvHeadline.setText(headline);
        tvHeadline.setTextSize(22);
        tvHeadline.setTypeface(null, android.graphics.Typeface.BOLD);
        tvHeadline.setLetterSpacing(0.15f);
        tvHeadline.setGravity(Gravity.CENTER);
        tvHeadline.setTextColor(accentColor);
        tvHeadline.setAlpha(0f);
        tvHeadline.setTranslationY(dpToPx(30));
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hLp.bottomMargin = dpToPx(14);
        tvHeadline.setLayoutParams(hLp);
        center.addView(tvHeadline);

        TextView tvDetail = new TextView(this);
        tvDetail.setText(nightResultText);
        tvDetail.setTextSize(14);
        tvDetail.setGravity(Gravity.CENTER);
        tvDetail.setTextColor(0xFF8A9BC4);
        tvDetail.setLineSpacing(0, 1.6f);
        tvDetail.setAlpha(0f);
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        dLp.bottomMargin = dpToPx(48);
        tvDetail.setLayoutParams(dLp);
        center.addView(tvDetail);

        TextView btnVote = new TextView(this);
        btnVote.setText(R.string.mafia_day_go_to_vote);
        btnVote.setTextSize(15);
        btnVote.setTypeface(null, android.graphics.Typeface.BOLD);
        btnVote.setGravity(Gravity.CENTER);
        btnVote.setLetterSpacing(0.1f);
        btnVote.setTextColor(ContextCompat.getColor(this, R.color.bg_dark));
        btnVote.setBackgroundResource(R.drawable.bg_button_gold);
        btnVote.setAlpha(0f);
        btnVote.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(56)));
        center.addView(btnVote);

        android.view.ViewGroup root = (android.view.ViewGroup) getWindow().getDecorView();
        root.addView(overlay);

        android.animation.ObjectAnimator flash = android.animation.ObjectAnimator
                .ofArgb(overlay, "backgroundColor",
                        bgColor,
                        (accentColor & 0x00FFFFFF) | 0x44000000,
                        bgColor);
        flash.setDuration(500);
        flash.start();

        tvIcon.postDelayed(() ->
                tvIcon.animate().scaleX(1.3f).scaleY(1.3f).alpha(1f)
                        .setDuration(400)
                        .setInterpolator(new android.view.animation.OvershootInterpolator(4f))
                        .withEndAction(() -> tvIcon.animate()
                                .scaleX(1f).scaleY(1f).setDuration(200).start())
                        .start(), 100);

        if (killed) {
            tvIcon.postDelayed(() -> {
                android.animation.ObjectAnimator shake = android.animation.ObjectAnimator
                        .ofFloat(tvIcon, "translationX",
                                0f, -20f, 20f, -15f, 15f, -8f, 8f, 0f);
                shake.setDuration(500);
                shake.start();
            }, 600);
        }

        tvHeadline.postDelayed(() ->
                tvHeadline.animate().alpha(1f).translationY(0f)
                        .setDuration(400)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .start(), 500);

        tvDetail.postDelayed(() ->
                tvDetail.animate().alpha(1f).setDuration(400).start(), 900);

        btnVote.postDelayed(() ->
                btnVote.animate().alpha(1f)
                        .setDuration(350)
                        .setInterpolator(new android.view.animation.OvershootInterpolator(2f))
                        .start(), 1400);

        btnVote.setOnClickListener(vv ->
                overlay.animate().alpha(0f).setDuration(350)
                        .withEndAction(() -> root.removeView(overlay))
                        .start());
    }

    private class NightResultParticleView extends android.view.View {
        private final java.util.Random rnd = new java.util.Random();
        private final android.graphics.Paint paint =
                new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final boolean killed, saved;
        private final float[][] pts = new float[35][6];
        private boolean initialized = false;

        NightResultParticleView(android.content.Context ctx, boolean killed, boolean saved) {
            super(ctx);
            this.killed = killed;
            this.saved  = saved;
        }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            if (!initialized && w > 0) {
                for (float[] p : pts) {
                    p[0] = rnd.nextFloat() * w;
                    p[1] = rnd.nextFloat() * h;
                    p[2] = 2f + rnd.nextFloat() * 6f;
                    p[3] = 5f + rnd.nextFloat() * 12f;
                    p[4] = 0.3f + rnd.nextFloat() * 0.7f;
                    p[5] = -1.5f + rnd.nextFloat() * 3f;
                }
                initialized = true;
            }
        }

        @Override
        protected void onDraw(android.graphics.Canvas canvas) {
            int w = getWidth(), h = getHeight();
            for (float[] p : pts) {
                if (killed) {
                    paint.setColor(0xFFAA0000);
                    paint.setAlpha((int)(p[4] * 255));
                    paint.setStyle(android.graphics.Paint.Style.FILL);
                    android.graphics.Path drop = new android.graphics.Path();
                    drop.addCircle(p[0], p[1], p[3]*0.5f, android.graphics.Path.Direction.CW);
                    drop.moveTo(p[0], p[1]-p[3]*0.5f);
                    drop.lineTo(p[0]-p[3]*0.25f, p[1]-p[3]*1.3f);
                    drop.lineTo(p[0]+p[3]*0.25f, p[1]-p[3]*1.3f);
                    drop.close();
                    canvas.drawPath(drop, paint);
                    p[1] += p[2]; p[0] += p[5];
                    if (p[1] > h+p[3]) { p[1] = -p[3]; p[0] = rnd.nextFloat()*w; }
                } else if (saved) {
                    paint.setColor(0xFF2ECC71);
                    paint.setAlpha((int)(p[4] * 255));
                    paint.setStyle(android.graphics.Paint.Style.FILL);
                    float s = p[3]*0.28f;
                    canvas.drawRect(p[0]-s, p[1]-p[3]*0.5f, p[0]+s, p[1]+p[3]*0.5f, paint);
                    canvas.drawRect(p[0]-p[3]*0.5f, p[1]-s, p[0]+p[3]*0.5f, p[1]+s, paint);
                    p[1] -= p[2]; p[0] += p[5];
                    if (p[1] < -p[3]) { p[1] = h+p[3]; p[0] = rnd.nextFloat()*w; }
                } else {
                    float twinkle = 0.3f + 0.7f * (float)(Math.sin(p[5] + p[2]) * 0.5 + 0.5);
                    paint.setColor(0xFF8AB4FF);
                    paint.setAlpha((int)(twinkle * p[4] * 200));
                    paint.setStyle(android.graphics.Paint.Style.FILL);
                    canvas.drawCircle(p[0], p[1], p[3]*0.4f, paint);
                    paint.setAlpha((int)(twinkle * p[4] * 80));
                    canvas.drawLine(p[0]-p[3], p[1], p[0]+p[3], p[1], paint);
                    canvas.drawLine(p[0], p[1]-p[3], p[0], p[1]+p[3], paint);
                    p[5] += 0.02f;
                }
                if (p[0] < 0 || p[0] > w) p[5] = -p[5];
            }
            postInvalidateDelayed(16);
        }
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

        tv_day_round.setText(getString(R.string.mafia_round, round));
        btn_eliminate.setOnClickListener(v -> confirmElimination());
        btn_skip_vote.setOnClickListener(v -> confirmSkip());
    }

    private void setupNightResult() {
        if (nightResultText == null) return;
        if (nightResultText.contains("saved")) {
            tv_night_result_emoji.setText("🧑‍⚕️");
        } else if (nightResultText.contains("eliminated")) {
            tv_night_result_emoji.setText("💀");
            int mafiaAlive = 0;
            for (Player p : players)
                if (p.isAlive() && p.getRole() == Player.Role.MAFIA) mafiaAlive++;
            nightResultText += (mafiaAlive == 1 ? getString(R.string.mafia_day_mafia_hiding_one) : getString(R.string.mafia_day_mafia_hiding_plural, mafiaAlive));
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
            Toast.makeText(this, R.string.mafia_day_all_votes_cast, Toast.LENGTH_SHORT).show();
            return;
        }
        int current = voteMap.getOrDefault(playerId, 0);
        if (current >= totalVoters - 1) {
            Toast.makeText(this, getString(R.string.mafia_day_no_self_vote, (totalVoters - 1)), Toast.LENGTH_SHORT).show();
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
            tv_leading_count.setText(getString(R.string.mafia_leading_count, votes));
        }
    }

    private void refreshVotesRemaining() {
        int remaining = totalVoters - totalVotes;
        tv_votes_remaining.setText(remaining == 0
                ? getString(R.string.mafia_day_all_votes_cast)
                : getString(R.string.mafia_votes_remaining, remaining));
    }

    private void confirmElimination() {
        if (voteMap.isEmpty() || totalVotes == 0) {
            Toast.makeText(this, R.string.mafia_day_no_votes_cast, Toast.LENGTH_SHORT).show();
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
        tvTitle.setText(R.string.mafia_day_draw_title);
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
        tvSub.setText(R.string.mafia_day_draw_msg);
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
        btnContinue.setText(R.string.mafia_day_begin_night);
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
                verdict     = getString(R.string.mafia_elim_title_mafia);
                flavour     = getString(R.string.mafia_elim_desc_mafia);
                break;
            case DOCTOR:
                bgColor     = 0xFF0A1A0F;
                accentColor = 0xFF2ECC71;
                emoji       = "🧑‍⚕️";
                verdict     = getString(R.string.mafia_elim_title_doctor);
                flavour     = getString(R.string.mafia_elim_desc_doctor);
                break;
            case DETECTIVE:
                bgColor     = 0xFF0A0F1A;
                accentColor = 0xFF3B9EFF;
                emoji       = "🕵️‍♀️";
                verdict     = getString(R.string.mafia_elim_title_detective);
                flavour     = getString(R.string.mafia_elim_desc_detective);
                break;
            default:
                bgColor     = 0xFF0F0F1A;
                accentColor = 0xFF8A9BC4;
                emoji       = "👤";
                verdict     = getString(R.string.mafia_elim_title_civilian);
                flavour     = getString(R.string.mafia_elim_desc_civilian);
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
        btnContinue.setText(R.string.mafia_elim_continue);
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
        android.widget.FrameLayout root =
                (android.widget.FrameLayout) getWindow().getDecorView();

        android.widget.FrameLayout overlay = new android.widget.FrameLayout(this);
        overlay.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
        overlay.setBackgroundColor(0xEE030A18);
        overlay.setElevation(dpToPx(150));

        SkipFlashView flash = new SkipFlashView(this);
        overlay.addView(flash, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));

        android.widget.LinearLayout center = new android.widget.LinearLayout(this);
        center.setOrientation(android.widget.LinearLayout.VERTICAL);
        center.setGravity(android.view.Gravity.CENTER);
        overlay.addView(center, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));

        android.widget.TextView tvMoon = new android.widget.TextView(this);
        tvMoon.setText("🌙");
        tvMoon.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 80);
        tvMoon.setGravity(android.view.Gravity.CENTER);
        tvMoon.setScaleX(0f);
        tvMoon.setScaleY(0f);
        tvMoon.setAlpha(0f);
        center.addView(tvMoon);

        android.widget.TextView tvTitle = new android.widget.TextView(this);
        tvTitle.setText(R.string.mafia_day_skipping_vote);
        tvTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 24);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setLetterSpacing(0.2f);
        tvTitle.setTextColor(0xFFB0C8FF);
        tvTitle.setGravity(android.view.Gravity.CENTER);
        tvTitle.setAlpha(0f);
        android.widget.LinearLayout.LayoutParams titleLp =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        titleLp.setMargins(0, dpToPx(18), 0, 0);
        tvTitle.setLayoutParams(titleLp);
        center.addView(tvTitle);

        android.widget.TextView tvSub = new android.widget.TextView(this);
        tvSub.setText(R.string.mafia_day_skip_msg);
        tvSub.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        tvSub.setTextColor(0xFF7A9BC4);
        tvSub.setGravity(android.view.Gravity.CENTER);
        tvSub.setLineSpacing(0, 1.6f);
        tvSub.setAlpha(0f);
        android.widget.LinearLayout.LayoutParams subLp =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        subLp.setMargins(dpToPx(40), dpToPx(12), dpToPx(40), 0);
        tvSub.setLayoutParams(subLp);
        center.addView(tvSub);

        android.widget.TextView tvTap = new android.widget.TextView(this);
        tvTap.setText(R.string.mafia_day_tap_continue);
        tvTap.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11);
        tvTap.setTextColor(0x66B0C8FF);
        tvTap.setGravity(android.view.Gravity.CENTER);
        tvTap.setAlpha(0f);
        android.widget.LinearLayout.LayoutParams tapLp =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        tapLp.setMargins(0, dpToPx(48), 0, 0);
        tvTap.setLayoutParams(tapLp);
        center.addView(tvTap);

        root.addView(overlay);

        tvMoon.animate()
                .scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(450)
                .setInterpolator(new android.view.animation.OvershootInterpolator(2.4f))
                .withEndAction(() -> {
                    tvMoon.animate().rotation(20f).setDuration(130)
                            .withEndAction(() ->
                                    tvMoon.animate().rotation(-15f).setDuration(110)
                                            .withEndAction(() ->
                                                    tvMoon.animate().rotation(0f).setDuration(90).start())
                                            .start())
                            .start();
                }).start();

        tvTitle.animate().alpha(1f).translationYBy(-dpToPx(12))
                .setStartDelay(180).setDuration(380).start();
        tvSub.animate().alpha(1f).translationYBy(-dpToPx(8))
                .setStartDelay(320).setDuration(360).start();
        tvTap.animate().alpha(1f)
                .setStartDelay(900).setDuration(500).start();

        Runnable proceed = () -> overlay.animate().alpha(0f).setDuration(380)
                .withEndAction(() -> { root.removeView(overlay); goToNightPhase(); }).start();
        overlay.postDelayed(proceed, 2200);

        overlay.setOnClickListener(vv -> {
            overlay.removeCallbacks(proceed);
            overlay.animate().alpha(0f).setDuration(280)
                    .withEndAction(() -> { root.removeView(overlay); goToNightPhase(); }).start();
        });
    }
    private static class SkipFlashView extends android.view.View {
        private final java.util.Random rnd = new java.util.Random(55443L);
        private final android.graphics.Paint p =
                new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final float[][] pts = new float[45][7];
        private boolean ready = false;
        private float time = 0f;

        SkipFlashView(android.content.Context ctx) {
            super(ctx);
            postDelayed(() -> {
                int w = getWidth(), h = getHeight();
                if (w == 0) return;
                for (float[] pt : pts) {
                    pt[0] = rnd.nextFloat() * w;
                    pt[1] = h * 0.3f + rnd.nextFloat() * h * 0.7f;
                    pt[2] = (rnd.nextFloat() - 0.5f) * 5f;
                    pt[3] = -1.5f - rnd.nextFloat() * 4.5f;
                    pt[4] = 5f + rnd.nextFloat() * 13f;
                    pt[5] = 0.5f + rnd.nextFloat() * 0.5f;
                    pt[6] = rnd.nextInt(3);
                }
                ready = true;
                invalidate();
            }, 60);
        }

        @Override
        protected void onDraw(android.graphics.Canvas canvas) {
            if (!ready) return;
            time += 0.04f;
            p.setStyle(android.graphics.Paint.Style.FILL);

            for (float[] pt : pts) {
                pt[0] += pt[2];
                pt[1] += pt[3];
                pt[5] -= 0.010f;
                if (pt[5] <= 0f || pt[1] < -60f) {
                    pt[0] = rnd.nextFloat() * getWidth();
                    pt[1] = getHeight() + 20f;
                    pt[2] = (rnd.nextFloat() - 0.5f) * 5f;
                    pt[3] = -1.5f - rnd.nextFloat() * 4.5f;
                    pt[5] = 0.4f + rnd.nextFloat() * 0.5f;
                    pt[6] = rnd.nextInt(3);
                }

                float pulse = 0.65f + 0.35f * (float) Math.sin(time * 2.5f + pt[0] * 0.05f);
                int al = (int)(pt[5] * pulse * 255);
                int col = (int)pt[6] == 0 ? 0x00C0D8FF :
                        (int)pt[6] == 1 ? 0x00FFE890 : 0x00D0A8FF;
                p.setColor(al << 24 | col);
                float s = pt[4] * pulse;

                if ((int)pt[6] == 1) {
                    canvas.drawCircle(pt[0], pt[1], s * 0.55f, p);
                } else {
                    p.setStyle(android.graphics.Paint.Style.STROKE);
                    p.setStrokeWidth(1.8f);
                    canvas.drawLine(pt[0]-s, pt[1], pt[0]+s, pt[1], p);
                    canvas.drawLine(pt[0], pt[1]-s, pt[0], pt[1]+s, p);
                    if ((int)pt[6] == 0) {
                        float sd = s * 0.68f;
                        canvas.drawLine(pt[0]-sd,pt[1]-sd, pt[0]+sd,pt[1]+sd, p);
                        canvas.drawLine(pt[0]-sd,pt[1]+sd, pt[0]+sd,pt[1]-sd, p);
                    }
                    p.setStyle(android.graphics.Paint.Style.FILL);
                }
            }
            postInvalidateDelayed(30);
        }
    }

    private boolean checkWinCondition() {
        int mafiaAlive = 0, townAlive = 0;
        for (Player p : players) {
            if (!p.isAlive()) continue;
            if (p.getRole() == Player.Role.MAFIA) mafiaAlive++;
            else townAlive++;
        }

        if (mafiaAlive == 0) {
            String title = getString(R.string.mafia_day_town_wins_title);
            String msg   = getString(R.string.mafia_day_town_wins_msg);
            if (isHost && MafiaServerHolder.isHosting())
                MafiaServerHolder.get().broadcastGameOver(title, msg);
            showGameOver(title, msg);
            return true;
        }
        if (mafiaAlive >= townAlive) {
            String title = getString(R.string.mafia_day_mafia_wins_title);
            String msg   = getString(R.string.mafia_day_mafia_wins_msg) + buildMafiaReveal();
            if (isHost && MafiaServerHolder.isHosting())
                MafiaServerHolder.get().broadcastGameOver(title, msg);
            showGameOver(title, msg);
            return true;
        }
        return false;
    }

    private String buildMafiaReveal() {
        StringBuilder sb = new StringBuilder(getString(R.string.mafia_day_mafia_reveal));
        for (Player p : players)
            if (p.getRole() == Player.Role.MAFIA)
                sb.append("• ").append(p.getName()).append("\n");
        return sb.toString().trim();
    }

    private void showGameOver(String title, String message) {
        btn_eliminate.setClickable(false);
        btn_skip_vote.setClickable(false);

        boolean mafiaWins = title.contains("MAFIA");

        android.widget.FrameLayout overlay = new android.widget.FrameLayout(this);
        overlay.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
        overlay.setBackgroundColor(mafiaWins ? 0xFF0D0000 : 0xFF00050F);
        overlay.setElevation(dpToPx(100));

        GameOverParticleView particles = new GameOverParticleView(this, mafiaWins);
        overlay.addView(particles, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);
        center.setPadding(dpToPx(32), 0, dpToPx(32), 0);
        overlay.addView(center, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));

        TextView tvIcon = new TextView(this);
        tvIcon.setText(mafiaWins ? "🧛" : "🏆");
        tvIcon.setTextSize(88);
        tvIcon.setGravity(Gravity.CENTER);
        tvIcon.setScaleX(0f); tvIcon.setScaleY(0f); tvIcon.setAlpha(0f);
        LinearLayout.LayoutParams iLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        iLp.bottomMargin = dpToPx(20);
        tvIcon.setLayoutParams(iLp);
        center.addView(tvIcon);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(34);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setLetterSpacing(0.25f);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setTextColor(mafiaWins ? 0xFFFF1111 : 0xFFF0B429);
        tvTitle.setAlpha(0f); tvTitle.setTranslationY(dpToPx(30));
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tLp.bottomMargin = dpToPx(16);
        tvTitle.setLayoutParams(tLp);
        center.addView(tvTitle);

        TextView tvMsg = new TextView(this);
        tvMsg.setText(message);
        tvMsg.setTextSize(14);
        tvMsg.setGravity(Gravity.CENTER);
        tvMsg.setTextColor(0xFF8A9BC4);
        tvMsg.setLineSpacing(0, 1.6f);
        tvMsg.setAlpha(0f);
        LinearLayout.LayoutParams mLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mLp.bottomMargin = dpToPx(48);
        tvMsg.setLayoutParams(mLp);
        center.addView(tvMsg);

        TextView btnPlayAgain = new TextView(this);
        btnPlayAgain.setText(R.string.mafia_day_play_again);
        btnPlayAgain.setTextSize(14);
        btnPlayAgain.setTypeface(null, Typeface.BOLD);
        btnPlayAgain.setGravity(Gravity.CENTER);
        btnPlayAgain.setTextColor(ContextCompat.getColor(this, R.color.bg_dark));
        btnPlayAgain.setBackgroundResource(R.drawable.bg_button_gold);
        btnPlayAgain.setAlpha(0f);
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(56));
        bLp.bottomMargin = dpToPx(12);
        btnPlayAgain.setLayoutParams(bLp);
        center.addView(btnPlayAgain);

        TextView btnMainMenu = new TextView(this);
        btnMainMenu.setText(R.string.mafia_day_main_menu);
        btnMainMenu.setTextSize(14);
        btnMainMenu.setTypeface(null, Typeface.BOLD);
        btnMainMenu.setGravity(Gravity.CENTER);
        btnMainMenu.setTextColor(ContextCompat.getColor(this, R.color.gold));
        btnMainMenu.setBackgroundResource(R.drawable.bg_button_outline);
        btnMainMenu.setAlpha(0f);
        btnMainMenu.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(52)));
        center.addView(btnMainMenu);

        android.view.ViewGroup root = (android.view.ViewGroup) getWindow().getDecorView();
        root.addView(overlay);

        android.animation.ObjectAnimator flash = android.animation.ObjectAnimator
                .ofArgb(overlay, "backgroundColor",
                        mafiaWins ? 0xFF0D0000 : 0xFF00050F,
                        mafiaWins ? 0xFF550000 : 0xFF002244,
                        mafiaWins ? 0xFF0D0000 : 0xFF00050F);
        flash.setDuration(600);
        flash.start();

        tvIcon.postDelayed(() ->
                tvIcon.animate().scaleX(1.3f).scaleY(1.3f).alpha(1f)
                        .setDuration(400)
                        .setInterpolator(new android.view.animation.OvershootInterpolator(4f))
                        .withEndAction(() -> tvIcon.animate()
                                .scaleX(1f).scaleY(1f).setDuration(200).start())
                        .start(), 100);

        tvTitle.postDelayed(() ->
                tvTitle.animate().alpha(1f).translationY(0f)
                        .setDuration(400)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .start(), 500);

        tvMsg.postDelayed(() ->
                tvMsg.animate().alpha(1f).setDuration(400).start(), 900);

        btnPlayAgain.postDelayed(() -> {
            btnPlayAgain.animate().alpha(1f).setDuration(300).start();
            btnMainMenu.animate().alpha(1f).setDuration(300).setStartDelay(100).start();
        }, 1300);

        overlay.postDelayed(() -> {
            android.animation.ObjectAnimator pulse = android.animation.ObjectAnimator
                    .ofFloat(tvTitle, "scaleX", 1f, 1.06f, 1f);
            pulse.setDuration(900);
            pulse.setRepeatCount(android.animation.ObjectAnimator.INFINITE);
            pulse.start();
            android.animation.ObjectAnimator pulseY = android.animation.ObjectAnimator
                    .ofFloat(tvTitle, "scaleY", 1f, 1.06f, 1f);
            pulseY.setDuration(900);
            pulseY.setRepeatCount(android.animation.ObjectAnimator.INFINITE);
            pulseY.start();
        }, 900);

        btnPlayAgain.setOnClickListener(v -> {
            startActivity(new Intent(this, Mafiasetupactivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });

        btnMainMenu.setOnClickListener(v -> {
            startActivity(new Intent(this, com.example.mini_projet.HomeActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            finish();
        });
    }

    private class GameOverParticleView extends android.view.View {
        private final java.util.Random rnd = new java.util.Random();
        private final android.graphics.Paint paint =
                new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final boolean mafiaWins;

        private final float[][] drops  = new float[40][6];
        private final float[][] rockets = new float[8][7];
        private final java.util.List<float[]> sparks = new java.util.ArrayList<>();
        private boolean initialized = false;
        private final int[] fwColors = {
                0xFFFFC107, 0xFFFF5722, 0xFF4CAF50, 0xFF2196F3,
                0xFFE91E63, 0xFFFFEB3B, 0xFF00BCD4, 0xFFFF9800
        };

        GameOverParticleView(android.content.Context ctx, boolean mafiaWins) {
            super(ctx);
            this.mafiaWins = mafiaWins;
        }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            if (!initialized && w > 0) {
                if (mafiaWins) {
                    for (float[] d : drops) {
                        d[0] = rnd.nextFloat() * w;
                        d[1] = rnd.nextFloat() * h;
                        d[2] = 4f + rnd.nextFloat() * 7f;
                        d[3] = 7f + rnd.nextFloat() * 13f;
                        d[4] = 0.5f + rnd.nextFloat() * 0.5f;
                        d[5] = -1.5f + rnd.nextFloat() * 3f;
                    }
                } else {
                    for (int i = 0; i < rockets.length; i++) {
                        resetRocket(rockets[i], w, h, i);
                    }
                }
                initialized = true;
            }
        }

        private void resetRocket(float[] r, int w, int h, int i) {
            r[0] = (w / (rockets.length + 1f)) * (i + 1) + rnd.nextFloat() * 60 - 30;
            r[1] = h + 10;
            r[2] = -1f + rnd.nextFloat() * 2f;
            r[3] = -(12f + rnd.nextFloat() * 8f);
            r[4] = 4f;
            r[5] = 1f;
            r[6] = 0;
        }

        private void burst(float x, float y, int color) {
            for (int i = 0; i < 28; i++) {
                double angle = Math.random() * Math.PI * 2;
                float speed = 3f + rnd.nextFloat() * 7f;
                sparks.add(new float[]{
                        x, y,
                        (float)(Math.cos(angle) * speed),
                        (float)(Math.sin(angle) * speed),
                        5f + rnd.nextFloat() * 5f,
                        1f,
                        color
                });
            }
        }

        @Override
        protected void onDraw(android.graphics.Canvas canvas) {
            int w = getWidth(), h = getHeight();
            if (!initialized) { postInvalidateDelayed(16); return; }

            if (mafiaWins) {
                for (float[] d : drops) {
                    paint.setColor(0xFFCC0000);
                    paint.setAlpha((int)(d[4] * 255));
                    paint.setStyle(android.graphics.Paint.Style.FILL);

                    android.graphics.Path drop = new android.graphics.Path();
                    drop.addCircle(d[0], d[1], d[3] * 0.5f,
                            android.graphics.Path.Direction.CW);
                    drop.moveTo(d[0], d[1] - d[3] * 0.5f);
                    drop.lineTo(d[0] - d[3] * 0.25f, d[1] - d[3] * 1.4f);
                    drop.lineTo(d[0] + d[3] * 0.25f, d[1] - d[3] * 1.4f);
                    drop.close();
                    canvas.drawPath(drop, paint);

                    d[1] += d[2]; d[0] += d[5];
                    if (d[1] > h + d[3]) { d[1] = -d[3]; d[0] = rnd.nextFloat() * w; }
                    if (d[0] < 0 || d[0] > w) d[5] = -d[5];
                }
            } else {
                for (int i = 0; i < rockets.length; i++) {
                    float[] r = rockets[i];
                    if (r[6] == 0) {
                        paint.setColor(0xFFFFFFFF);
                        paint.setAlpha(180);
                        paint.setStyle(android.graphics.Paint.Style.FILL);
                        canvas.drawCircle(r[0], r[1], r[4], paint);
                        for (int t = 1; t <= 5; t++) {
                            paint.setAlpha(40 * (6 - t));
                            canvas.drawCircle(r[0] - r[2]*t, r[1] + t*6, r[4]*0.6f, paint);
                        }
                        r[0] += r[2]; r[1] += r[3];
                        r[3] += 0.3f;
                        if (r[3] >= -2f) {
                            r[6] = 1;
                            burst(r[0], r[1], fwColors[i % fwColors.length]);
                            postDelayed(() -> resetRocket(r, w, h, (int)(rnd.nextFloat()*8)),
                                    (long)(800 + rnd.nextFloat() * 1200));
                        }
                    }
                }

                java.util.Iterator<float[]> it = sparks.iterator();
                while (it.hasNext()) {
                    float[] sp = it.next();
                    paint.setColor((int)sp[6]);
                    paint.setAlpha((int)(sp[5] * 255));
                    paint.setStyle(android.graphics.Paint.Style.FILL);
                    canvas.drawCircle(sp[0], sp[1], sp[4] * sp[5], paint);
                    sp[0] += sp[2]; sp[1] += sp[3];
                    sp[3] += 0.15f;
                    sp[5] -= 0.018f;
                    sp[2] *= 0.97f; sp[3] *= 0.97f;
                    if (sp[5] <= 0) it.remove();
                }
            }
            postInvalidateDelayed(16);
        }
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
        return count == 1 ? leader : null;
    }

    private List<Player> getAlivePlayers() {
        List<Player> alive = new ArrayList<>();
        for (Player p : players) if (p.isAlive()) alive.add(p);
        return alive;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private static class DaySkyView extends android.view.View {
        private final java.util.Random rnd = new java.util.Random(55443L);
        private final android.graphics.Paint p =
                new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private float sX, sY, sR;
        private final float[][] clouds = new float[9][7];
        private final float[][] birds  = new float[7][6];
        private float time=0f; private boolean ready=false; private int W,H;

        DaySkyView(android.content.Context ctx){super(ctx);}
        private float dp(float v){return v*getResources().getDisplayMetrics().density;}

        @Override protected void onSizeChanged(int w,int h,int ow,int oh){
            if(w<=0||h<=0)return; W=w; H=h; rnd.setSeed(55443L);
            sX=w*0.72f; sY=h*0.14f; sR=dp(44);
            for(int i=0;i<clouds.length;i++){
                clouds[i][0]=rnd.nextFloat()*w;
                clouds[i][1]=h*(0.06f+rnd.nextFloat()*0.38f);
                clouds[i][2]=dp(55)+rnd.nextFloat()*dp(80);
                clouds[i][3]=dp(18)+rnd.nextFloat()*dp(28);
                clouds[i][4]=i<4?dp(1)*(0.3f+rnd.nextFloat()*0.4f):dp(1)*(0.7f+rnd.nextFloat()*0.9f);
                clouds[i][5]=i<4?0.40f+rnd.nextFloat()*0.22f:0.72f+rnd.nextFloat()*0.22f;
                clouds[i][6]=i<4?0:1;
            }
            for(float[]b:birds){
                b[0]=rnd.nextFloat()*w; b[1]=h*(0.05f+rnd.nextFloat()*0.26f);
                b[2]=dp(1)*(1.2f+rnd.nextFloat()*1.8f); b[3]=rnd.nextFloat()*6.283f;
                b[4]=0.08f+rnd.nextFloat()*0.12f; b[5]=0.6f+rnd.nextFloat()*0.9f;
            }
            ready=true;
        }

        @Override protected void onDraw(android.graphics.Canvas canvas){
            if(!ready){postInvalidateDelayed(30);return;} time+=0.018f;

            android.graphics.LinearGradient sky=new android.graphics.LinearGradient(
                    0,0,0,H,new int[]{0xFF083870,0xFF1468B8,0xFF2E94E0,0xFF78C8F0,0xFFBEE8F8,0xFFE4F5FF},
                    new float[]{0f,0.18f,0.42f,0.65f,0.84f,1f},android.graphics.Shader.TileMode.CLAMP);
            p.setShader(sky); canvas.drawRect(0,0,W,H,p); p.setShader(null);

            android.graphics.LinearGradient horiz=new android.graphics.LinearGradient(
                    0,H*0.68f,0,H,new int[]{0x00FFE0A0,0x28FFD080,0x14FFBB60},
                    null,android.graphics.Shader.TileMode.CLAMP);
            p.setShader(horiz); canvas.drawRect(0,H*0.68f,W,H,p); p.setShader(null);

            float br=1f+0.04f*(float)Math.sin(time*0.7f);
            for(int g=10;g>=1;g--){
                p.setColor((int)(255*0.015f*(11-g)/10f)<<24|0x00FFE080);
                canvas.drawCircle(sX,sY,sR*br+dp(g*10),p);
            }

            android.graphics.RadialGradient sa=new android.graphics.RadialGradient(
                    sX,sY,sR*2.8f,new int[]{0x40FFE890,0x20FFD060,0x00000000},
                    new float[]{0f,0.45f,1f},android.graphics.Shader.TileMode.CLAMP);
            p.setShader(sa); canvas.drawCircle(sX,sY,sR*2.8f,p); p.setShader(null);

            float rot=time*0.18f;
            p.setStyle(android.graphics.Paint.Style.STROKE);
            for(int i=0;i<16;i++){
                float ang=rot+i*(float)(Math.PI*2.0/16);
                float pulse=0.55f+0.45f*(float)Math.sin(time*1.2f+i*0.6f);
                p.setStrokeWidth(dp(1)*(1.4f+0.8f*pulse)*(i%2==0?1.4f:0.8f));
                p.setColor((int)(pulse*155)<<24|0x00FFE890);
                float inn=sR*1.18f, out=sR*(1.6f+0.28f*pulse);
                canvas.drawLine(sX+(float)Math.cos(ang)*inn,sY+(float)Math.sin(ang)*inn,
                        sX+(float)Math.cos(ang)*out,sY+(float)Math.sin(ang)*out,p);
            }
            p.setStyle(android.graphics.Paint.Style.FILL);

            android.graphics.RadialGradient sb=new android.graphics.RadialGradient(
                    sX-sR*0.25f,sY-sR*0.25f,sR*1.1f,
                    new int[]{0xFFFFFFF0,0xFFFFF880,0xFFFFE040,0xFFFFBB00},
                    new float[]{0f,0.38f,0.72f,1f},android.graphics.Shader.TileMode.CLAMP);
            p.setShader(sb); canvas.drawCircle(sX,sY,sR,p); p.setShader(null);
            android.graphics.RadialGradient ss=new android.graphics.RadialGradient(
                    sX-sR*0.32f,sY-sR*0.32f,sR*0.48f,new int[]{0x50FFFFFF,0x00FFFFFF},
                    null,android.graphics.Shader.TileMode.CLAMP);
            p.setShader(ss); canvas.drawCircle(sX,sY,sR,p); p.setShader(null);

            drawClouds(canvas,0); drawClouds(canvas,1);

            for(float[]b:birds){
                b[0]+=b[2]; if(b[0]>W+dp(40))b[0]=-dp(40);
                b[3]+=b[4];
                drawBird(canvas,b[0],b[1],b[5],(float)(Math.sin(b[3])*Math.PI*0.45f));
            }
            for(float[]c:clouds){c[0]+=c[4]; if(c[0]>W+c[2]*1.5f)c[0]=-c[2]*1.5f;}
            postInvalidateDelayed(30);
        }

        private void drawClouds(android.graphics.Canvas cv,int layer){
            for(float[]c:clouds){
                if((int)c[6]!=layer)continue;
                drawPuff(cv,c[0],c[1],c[2],c[3],(int)(c[5]*255));
                drawPuff(cv,c[0]+c[2]*0.45f,c[1]-c[3]*0.12f,c[2]*0.65f,c[3]*0.78f,(int)(c[5]*0.78f*255));
                drawPuff(cv,c[0]-c[2]*0.42f,c[1]-c[3]*0.06f,c[2]*0.58f,c[3]*0.70f,(int)(c[5]*0.65f*255));
            }
        }

        private void drawPuff(android.graphics.Canvas cv,float cx,float cy,float rx,float ry,int al){
            android.graphics.RadialGradient g=new android.graphics.RadialGradient(
                    cx,cy,Math.max(rx,ry),new int[]{al<<24|0x00FFFFFF,0x00FFFFFF},
                    new float[]{0f,1f},android.graphics.Shader.TileMode.CLAMP);
            p.setShader(g); cv.save(); cv.scale(1f,ry/rx,cx,cy);
            cv.drawCircle(cx,cy,rx,p); cv.restore(); p.setShader(null);
        }

        private void drawBird(android.graphics.Canvas cv,float x,float y,float sc,float flap){
            p.setStyle(android.graphics.Paint.Style.STROKE);
            p.setStrokeWidth(dp(1)*sc*1.1f); p.setColor(0xAA223344);
            float ws=dp(7)*sc, lift=(float)Math.sin(flap)*ws*0.55f;
            cv.drawLine(x,y,x-ws*0.55f,y-lift*0.4f,p);
            cv.drawLine(x-ws*0.55f,y-lift*0.4f,x-ws,y-lift,p);
            cv.drawLine(x,y,x+ws*0.55f,y-lift*0.4f,p);
            cv.drawLine(x+ws*0.55f,y-lift*0.4f,x+ws,y-lift,p);
            p.setStyle(android.graphics.Paint.Style.FILL);
        }
    }
}