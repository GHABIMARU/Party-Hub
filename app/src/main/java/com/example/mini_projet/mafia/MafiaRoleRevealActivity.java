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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.mini_projet.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Pass-and-play role reveal + night actions.
 */
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

        android.widget.FrameLayout skyRoot = findViewById(R.id.root_night_sky);
        NightSkyView nightSky = new NightSkyView(this);
        nightSky.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
        skyRoot.addView(nightSky, 0);

        bindViews();
        showLockScreen(0);
    }

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

    private void showLockScreen(int index) {
        currentIndex    = index;
        actionSelection = null;
        roleHasBeenSeen = false;

        List<Player> alive = getAlivePlayers();
        Player p = alive.get(index);
        tv_progress.setText(getString(R.string.mafia_night_progress, (index + 1), alive.size()));
        tv_lock_name.setText(getString(R.string.mafia_night_player_turn, p.getName()));
        tv_lock_subtitle.setText(getString(R.string.mafia_night_pass_phone_instruction, p.getName()));

        btn_show_role.setText(R.string.mafia_night_hold_to_see_role);
        btn_seen_role.setVisibility(View.GONE);

        layout_lock.setVisibility(View.VISIBLE);
        scroll_role_visible.setVisibility(View.GONE);
        layout_action_panel.setVisibility(View.GONE);
        btn_confirm_action.setVisibility(View.GONE);
        btn_civilian_done.setVisibility(View.GONE);
        layout_action_selected.setVisibility(View.INVISIBLE);

        fillRoleContent(p);
    }

    private void setupHoldToReveal() {
        btn_show_role.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    layout_lock.setVisibility(View.GONE);
                    scroll_role_visible.setVisibility(View.VISIBLE);
                    if (!roleHasBeenSeen) {
                        List<Player> aliveNow = getAlivePlayers();
                        if (currentIndex < aliveNow.size()) {
                            showRoleAnimation(aliveNow.get(currentIndex).getRole());
                        }
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    scroll_role_visible.setVisibility(View.GONE);
                    layout_lock.setVisibility(View.VISIBLE);
                    if (!roleHasBeenSeen) {
                        roleHasBeenSeen = true;
                        btn_show_role.setText(R.string.mafia_night_hold_recheck);
                        List<Player> alive = getAlivePlayers();
                        if (currentIndex < alive.size()) {
                            switch (alive.get(currentIndex).getRole()) {
                                case MAFIA:
                                    btn_seen_role.setText(R.string.mafia_night_who_to_kill);
                                    break;
                                case DOCTOR:
                                    btn_seen_role.setText(R.string.mafia_night_who_to_protect);
                                    break;
                                case DETECTIVE:
                                    btn_seen_role.setText(R.string.mafia_night_who_to_suspect);
                                    break;
                                default:
                                    boolean isLast = (currentIndex == alive.size() - 1);
                                    btn_seen_role.setText(isLast
                                            ? R.string.mafia_night_pass_start_day
                                            : R.string.mafia_night_pass_next);
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

    private void showRoleAnimation(Player.Role role) {
        int bgColor, accentColor;
        String emoji, title;
        switch (role) {
            case MAFIA:
                bgColor     = 0xEE1A0000;
                accentColor = 0xFFFF1111;
                emoji       = "🧛";
                title       = getString(R.string.mafia_reveal_title_mafia);
                break;
            case DOCTOR:
                bgColor     = 0xEE001A08;
                accentColor = 0xFF2ECC71;
                emoji       = "🧑‍⚕️";
                title       = getString(R.string.mafia_reveal_title_doctor);
                break;
            case DETECTIVE:
                bgColor     = 0xEE000D1A;
                accentColor = 0xFF3B9EFF;
                emoji       = "🕵️‍♀️";
                title       = getString(R.string.mafia_reveal_title_detective);
                break;
            default:
                bgColor     = 0xEE0A0A18;
                accentColor = 0xFFB0BEC5;
                emoji       = "👤";
                title       = getString(R.string.mafia_reveal_title_civilian);
                break;
        }

        android.widget.FrameLayout overlay = new android.widget.FrameLayout(this);
        overlay.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
        overlay.setBackgroundColor(bgColor);
        overlay.setElevation(dp(100));

        RoleParticleView particles = new RoleParticleView(this, role, accentColor);
        overlay.addView(particles, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);
        overlay.addView(center, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));

        TextView tvEmoji = new TextView(this);
        tvEmoji.setText(emoji);
        tvEmoji.setTextSize(96);
        tvEmoji.setGravity(Gravity.CENTER);
        tvEmoji.setScaleX(0f);
        tvEmoji.setScaleY(0f);
        tvEmoji.setAlpha(0f);
        LinearLayout.LayoutParams eLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        eLp.bottomMargin = dp(24);
        tvEmoji.setLayoutParams(eLp);
        center.addView(tvEmoji);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(24);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setLetterSpacing(0.2f);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setTextColor(accentColor);
        tvTitle.setAlpha(0f);
        tvTitle.setTranslationY(dp(40));
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        center.addView(tvTitle);

        android.view.ViewGroup root = findViewById(android.R.id.content);
        root.addView(overlay);

        tvEmoji.animate()
                .scaleX(1.2f).scaleY(1.2f).alpha(1f)
                .setDuration(350)
                .setInterpolator(new android.view.animation.OvershootInterpolator(4f))
                .withEndAction(() -> tvEmoji.animate()
                        .scaleX(1f).scaleY(1f).setDuration(150).start())
                .start();

        tvTitle.animate()
                .translationY(0f).alpha(1f)
                .setStartDelay(200)
                .setDuration(400)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        android.animation.ObjectAnimator pulse = android.animation.ObjectAnimator
                .ofArgb(overlay, "backgroundColor",
                        bgColor, (accentColor & 0x00FFFFFF) | 0x55000000, bgColor);
        pulse.setDuration(700);
        pulse.setStartDelay(150);
        pulse.start();

        overlay.postDelayed(() -> overlay.animate()
                .alpha(0f).setDuration(400)
                .withEndAction(() -> root.removeView(overlay))
                .start(), 1800);

        overlay.setOnClickListener(vv -> {
            overlay.removeCallbacks(null);
            overlay.animate().alpha(0f).setDuration(200)
                    .withEndAction(() -> root.removeView(overlay)).start();
        });
    }

    private static class RoleParticleView extends android.view.View {
        private final java.util.Random rnd = new java.util.Random();
        private final android.graphics.Paint paint =
                new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final Player.Role role;
        private final int color;
        private final float[][] pts = new float[30][6];
        private boolean initialized = false;

        RoleParticleView(android.content.Context ctx, Player.Role role, int color) {
            super(ctx);
            this.role  = role;
            this.color = color;
        }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            if (!initialized && w > 0) {
                for (float[] p : pts) {
                    p[0] = rnd.nextFloat() * w;
                    p[1] = rnd.nextFloat() * h;
                    p[2] = 3f + rnd.nextFloat() * 6f;
                    p[3] = 8f + rnd.nextFloat() * 14f;
                    p[4] = 0.4f + rnd.nextFloat() * 0.6f;
                    p[5] = -2f + rnd.nextFloat() * 4f;
                }
                initialized = true;
            }
        }

        @Override
        protected void onDraw(android.graphics.Canvas canvas) {
            int w = getWidth(), h = getHeight();
            for (float[] p : pts) {
                paint.setColor(color);
                paint.setAlpha((int)(p[4] * 255));

                switch (role) {
                    case MAFIA:
                        paint.setStyle(android.graphics.Paint.Style.FILL);
                        android.graphics.Path drop = new android.graphics.Path();
                        drop.addCircle(p[0], p[1], p[3] * 0.5f,
                                android.graphics.Path.Direction.CW);
                        drop.moveTo(p[0], p[1] - p[3] * 0.5f);
                        drop.lineTo(p[0] - p[3] * 0.25f, p[1] - p[3] * 1.3f);
                        drop.lineTo(p[0] + p[3] * 0.25f, p[1] - p[3] * 1.3f);
                        drop.close();
                        canvas.drawPath(drop, paint);
                        break;
                    case DOCTOR:
                        paint.setStyle(android.graphics.Paint.Style.FILL);
                        float s = p[3] * 0.28f;
                        canvas.drawRect(p[0]-s, p[1]-p[3]*0.5f,
                                p[0]+s, p[1]+p[3]*0.5f, paint);
                        canvas.drawRect(p[0]-p[3]*0.5f, p[1]-s,
                                p[0]+p[3]*0.5f, p[1]+s, paint);
                        break;
                    case DETECTIVE:
                        paint.setStyle(android.graphics.Paint.Style.STROKE);
                        paint.setStrokeWidth(2.5f);
                        canvas.drawCircle(p[0], p[1], p[3] * 0.5f, paint);
                        break;
                    default:
                        paint.setStyle(android.graphics.Paint.Style.FILL);
                        canvas.drawCircle(p[0], p[1], p[3] * 0.4f, paint);
                        paint.setAlpha((int)(p[4] * 100));
                        float sp = p[3] * 1.3f;
                        canvas.drawLine(p[0]-sp, p[1], p[0]+sp, p[1], paint);
                        canvas.drawLine(p[0], p[1]-sp, p[0], p[1]+sp, paint);
                        p[1] -= p[2];
                        p[0] += p[5];
                        if (p[1] < -p[3]) { p[1] = h + p[3]; p[0] = rnd.nextFloat() * w; }
                        if (p[0] < -p[3] || p[0] > w + p[3]) p[5] = -p[5];
                        continue;
                }

                p[1] += p[2];
                p[0] += p[5];
                if (p[1] > h + p[3]) { p[1] = -p[3]; p[0] = rnd.nextFloat() * w; }
                if (p[0] < -p[3] || p[0] > w + p[3]) p[5] = -p[5];
            }
            postInvalidateDelayed(16);
        }
    }

    private void fillRoleContent(Player p) {
        switch (p.getRole()) {
            case MAFIA:
                tv_role_emoji.setText("🧛");
                tv_role_name_label.setText(R.string.mafia_role_mafia);
                tv_role_description.setText(buildMafiaTeamInfo(p));
                break;
            case DOCTOR:
                tv_role_emoji.setText("🧑‍⚕️");
                tv_role_name_label.setText(R.string.mafia_role_doctor_name);
                tv_role_description.setText(R.string.mafia_role_doctor_full_desc_long);
                break;
            case DETECTIVE:
                tv_role_emoji.setText("🕵️‍♀️");
                tv_role_name_label.setText(R.string.mafia_role_detective_name);
                tv_role_description.setText(R.string.mafia_role_detective_full_desc_long);
                break;
            default:
                tv_role_emoji.setText("👤");
                tv_role_name_label.setText(R.string.mafia_role_civilian);
                tv_role_description.setText(R.string.mafia_role_civilian_desc_long);
                break;
        }
    }

    private void unlockAction() {
        List<Player> alive = getAlivePlayers();
        Player p = alive.get(currentIndex);

        if (p.getRole() == Player.Role.CIVILIAN) {
            advanceToNextPlayer();
            return;
        }

        boolean isLast = (currentIndex == alive.size() - 1);
        btn_confirm_action.setText(isLast ? R.string.mafia_night_confirm_start_day : R.string.mafia_night_confirm_next);

        layout_lock.setVisibility(View.GONE);
        scroll_role_visible.setVisibility(View.VISIBLE);

        switch (p.getRole()) {
            case MAFIA:
                showActionPanel(getString(R.string.mafia_night_action_kill), p);
                break;
            case DOCTOR:
                showActionPanel(getString(R.string.mafia_night_action_protect), p);
                break;
            case DETECTIVE:
                showActionPanel(getString(R.string.mafia_night_action_investigate), p);
                break;
        }
    }

    private void showActionPanel(String title, Player actor) {
        tv_action_title.setText(title);
        layout_action_panel.setVisibility(View.VISIBLE);
        btn_confirm_action.setVisibility(View.VISIBLE);
        btn_civilian_done.setVisibility(View.GONE);

        ll_action_player_list.removeAllViews();
        List<Player> alive = getAlivePlayers();
        for (Player target : alive) {
            if (actor.getRole() == Player.Role.MAFIA
                    && target.getRole() == Player.Role.MAFIA) continue;
            if (actor.getRole() != Player.Role.DOCTOR) {
                if (target == actor) continue;
                if (target.getId() == actor.getId()) continue;
                if (target.getName().equals(actor.getName())) continue;
            }

            LinearLayout row = buildActionRow(target, actor.getRole());
            ll_action_player_list.addView(row);
        }
    }

    private LinearLayout buildActionRow(Player target, Player.Role actorRole) {
        int highlightColor;
        switch (actorRole) {
            case MAFIA:      highlightColor = 0xFFCC0000; break;
            case DOCTOR:     highlightColor = 0xFF2ECC71; break;
            case DETECTIVE:  highlightColor = 0xFF3B9EFF; break;
            default:         highlightColor = 0xFFF0B429; break;
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
            tv_action_selected_name.setText(getString(R.string.mafia_night_selected_prefix, target.getName()));
            layout_action_selected.setVisibility(View.VISIBLE);
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
            android.graphics.drawable.GradientDrawable selectedBg =
                    new android.graphics.drawable.GradientDrawable();
            selectedBg.setColor(highlightColor);
            selectedBg.setCornerRadius(dp(12));
            row.setBackground(selectedBg);
            tv.setTextColor(0xFFFFFFFF);
        });
        return row;
    }

    private void confirmAction() {
        if (actionSelection == null) {
            Toast.makeText(this, R.string.mafia_night_select_first, Toast.LENGTH_SHORT).show();
            return;
        }

        List<Player> alive = getAlivePlayers();
        Player current = alive.get(currentIndex);

        switch (current.getRole()) {
            case MAFIA:
                if (targetKill == null) {
                    targetKill = actionSelection;
                } else if (targetKill.getId() != actionSelection.getId()) {
                    targetKill = (Math.random() < 0.5) ? targetKill : actionSelection;
                }
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

    private void showDetectiveResult(String suspectName, boolean isMafia) {
        showInvestigationAnimation(suspectName, isMafia, () -> showDetectiveDialog(suspectName, isMafia));
    }

    private void showInvestigationAnimation(String suspectName, boolean isMafia, Runnable onDone) {
        android.view.ViewGroup root = findViewById(android.R.id.content);

        android.widget.FrameLayout overlay = new android.widget.FrameLayout(this);
        overlay.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
        overlay.setBackgroundColor(isMafia ? 0xFF0D0000 : 0xFF001A08);
        overlay.setElevation(dp(200));

        InvestigationParticleView particles = new InvestigationParticleView(this, isMafia);
        overlay.addView(particles, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);
        overlay.addView(center, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));

        TextView tvLens = new TextView(this);
        tvLens.setText("🕵️‍♀️");
        tvLens.setTextSize(72);
        tvLens.setGravity(Gravity.CENTER);
        tvLens.setScaleX(0f);
        tvLens.setScaleY(0f);
        tvLens.setAlpha(0f);
        LinearLayout.LayoutParams lensLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lensLp.bottomMargin = dp(16);
        tvLens.setLayoutParams(lensLp);
        center.addView(tvLens);

        TextView tvResult = new TextView(this);
        tvResult.setText(isMafia ? "🧛" : "😇");
        tvResult.setTextSize(64);
        tvResult.setGravity(Gravity.CENTER);
        tvResult.setAlpha(0f);
        tvResult.setScaleX(0f);
        tvResult.setScaleY(0f);
        LinearLayout.LayoutParams resLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        resLp.bottomMargin = dp(20);
        tvResult.setLayoutParams(resLp);
        center.addView(tvResult);

        TextView tvVerdict = new TextView(this);
        tvVerdict.setText(isMafia ? getString(R.string.mafia_det_verdict_found) : getString(R.string.mafia_det_verdict_innocent));
        tvVerdict.setTextSize(32);
        tvVerdict.setTypeface(null, android.graphics.Typeface.BOLD);
        tvVerdict.setLetterSpacing(0.25f);
        tvVerdict.setGravity(Gravity.CENTER);
        tvVerdict.setTextColor(isMafia ? 0xFFFF1111 : 0xFF2ECC71);
        tvVerdict.setAlpha(0f);
        tvVerdict.setTranslationY(dp(30));
        LinearLayout.LayoutParams vLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        vLp.bottomMargin = dp(10);
        tvVerdict.setLayoutParams(vLp);
        center.addView(tvVerdict);

        TextView tvName = new TextView(this);
        tvName.setText(suspectName);
        tvName.setTextSize(20);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        tvName.setGravity(Gravity.CENTER);
        tvName.setTextColor(0xFFF0F4FF);
        tvName.setAlpha(0f);
        tvName.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        center.addView(tvName);

        root.addView(overlay);

        tvLens.animate().scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(400)
                .setInterpolator(new android.view.animation.OvershootInterpolator(3f))
                .start();

        overlay.postDelayed(() -> {
            android.animation.ObjectAnimator shake = android.animation.ObjectAnimator
                    .ofFloat(tvLens, "translationX", 0f, -18f, 18f, -14f, 14f, -8f, 8f, 0f);
            shake.setDuration(400);
            shake.start();
        }, 400);

        overlay.postDelayed(() -> {
            int flashColor = isMafia ? 0xAAFF0000 : 0xAA00FF88;
            android.animation.ObjectAnimator flash = android.animation.ObjectAnimator
                    .ofArgb(overlay, "backgroundColor",
                            isMafia ? 0xFF0D0000 : 0xFF001A08,
                            flashColor,
                            isMafia ? 0xFF0D0000 : 0xFF001A08);
            flash.setDuration(500);
            flash.start();

            tvLens.animate().alpha(0f).scaleX(0.5f).scaleY(0.5f)
                    .setDuration(250).start();

            tvResult.animate().scaleX(1.3f).scaleY(1.3f).alpha(1f)
                    .setDuration(300)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(5f))
                    .withEndAction(() -> tvResult.animate()
                            .scaleX(1f).scaleY(1f).setDuration(150).start())
                    .start();
        }, 800);

        overlay.postDelayed(() -> {
            tvVerdict.animate().alpha(1f).translationY(0f)
                    .setDuration(350)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
            tvName.animate().alpha(1f).setDuration(300).setStartDelay(150).start();
        }, 1100);

        overlay.postDelayed(() -> {
            overlay.animate().alpha(0f).setDuration(400)
                    .withEndAction(() -> {
                        root.removeView(overlay);
                        onDone.run();
                    }).start();
        }, 2400);
    }

    private static class InvestigationParticleView extends android.view.View {
        private final java.util.Random rnd = new java.util.Random();
        private final android.graphics.Paint paint =
                new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final boolean isMafia;
        private final float[][] pts = new float[25][6];
        private boolean initialized = false;

        InvestigationParticleView(android.content.Context ctx, boolean isMafia) {
            super(ctx);
            this.isMafia = isMafia;
        }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            if (!initialized && w > 0) {
                for (float[] p : pts) {
                    p[0] = rnd.nextFloat() * w;
                    p[1] = rnd.nextFloat() * h;
                    p[2] = 2f + rnd.nextFloat() * 5f;
                    p[3] = 6f + rnd.nextFloat() * 12f;
                    p[4] = 0.3f + rnd.nextFloat() * 0.7f;
                    p[5] = -2f + rnd.nextFloat() * 4f;
                }
                initialized = true;
            }
        }

        @Override
        protected void onDraw(android.graphics.Canvas canvas) {
            int w = getWidth(), h = getHeight();
            for (float[] p : pts) {
                if (isMafia) {
                    paint.setColor(0xFFCC0000);
                    paint.setAlpha((int)(p[4] * 255));
                    paint.setStyle(android.graphics.Paint.Style.FILL);
                    android.graphics.Path drop = new android.graphics.Path();
                    drop.addCircle(p[0], p[1], p[3] * 0.5f, android.graphics.Path.Direction.CW);
                    drop.moveTo(p[0], p[1] - p[3] * 0.5f);
                    drop.lineTo(p[0] - p[3] * 0.25f, p[1] - p[3] * 1.3f);
                    drop.lineTo(p[0] + p[3] * 0.25f, p[1] - p[3] * 1.3f);
                    drop.close();
                    canvas.drawPath(drop, paint);
                } else {
                    paint.setColor(0xFF2ECC71);
                    paint.setAlpha((int)(p[4] * 255));
                    paint.setStyle(android.graphics.Paint.Style.FILL);
                    float r = p[3] * 0.5f;
                    android.graphics.Path star = new android.graphics.Path();
                    star.moveTo(p[0], p[1] - r);
                    star.lineTo(p[0] + r*0.3f, p[1] - r*0.3f);
                    star.lineTo(p[0] + r, p[1]);
                    star.lineTo(p[0] + r*0.3f, p[1] + r*0.3f);
                    star.lineTo(p[0], p[1] + r);
                    star.lineTo(p[0] - r*0.3f, p[1] + r*0.3f);
                    star.lineTo(p[0] - r, p[1]);
                    star.lineTo(p[0] - r*0.3f, p[1] - r*0.3f);
                    star.close();
                    canvas.drawPath(star, paint);
                }
                p[1] += p[2];
                p[0] += p[5];
                if (p[1] > h + p[3]) { p[1] = -p[3]; p[0] = rnd.nextFloat() * w; }
                if (p[0] < -p[3] || p[0] > w + p[3]) p[5] = -p[5];
            }
            postInvalidateDelayed(16);
        }
    }

    private void showDetectiveDialog(String suspectName, boolean isMafia) {
        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setGravity(android.view.Gravity.CENTER);
        root.setBackgroundColor(isMafia ? 0xFF1A0A0A : 0xFF0A1A0F);
        root.setPadding(dp(32), dp(48), dp(32), dp(40));

        android.widget.TextView tvIcon = new android.widget.TextView(this);
        tvIcon.setText(isMafia ? "🧛" : "😇");
        tvIcon.setTextSize(72);
        tvIcon.setGravity(android.view.Gravity.CENTER);
        android.widget.LinearLayout.LayoutParams iconLp =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        iconLp.bottomMargin = dp(20);
        tvIcon.setLayoutParams(iconLp);
        root.addView(tvIcon);

        android.widget.TextView tvVerdict = new android.widget.TextView(this);
        tvVerdict.setText(isMafia ? R.string.mafia_role_mafia : R.string.mafia_det_verdict_innocent);
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

        android.widget.TextView tvDesc = new android.widget.TextView(this);
        tvDesc.setText(isMafia
                ? getString(R.string.mafia_det_result_mafia, suspectName)
                : getString(R.string.mafia_det_result_innocent, suspectName));
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

        android.widget.TextView btnOk = new android.widget.TextView(this);
        btnOk.setText(R.string.mafia_night_hide_pass);
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

    private void advanceToNextPlayer() {
        int next = currentIndex + 1;
        if (next < getAlivePlayers().size()) {
            showLockScreen(next);
        } else {
            resolveNightAndGoToDay();
        }
    }

    private void resolveNightAndGoToDay() {
        boolean saved = (targetSave != null && targetKill != null
                && targetSave.getId() == targetKill.getId());

        String result;
        if (targetKill == null) {
            result = getString(R.string.mafia_day_no_strike);
        } else if (saved) {
            result = getString(R.string.mafia_day_saved, targetKill.getName());
        } else {
            targetKill.setAlive(false);
            result = getString(R.string.mafia_day_eliminated, targetKill.getName());
        }

        if (isHost && MafiaServerHolder.isHosting()) {
            MafiaNetworkServer server = MafiaServerHolder.get();
            server.broadcastNightResult(result);
        }

        Intent intent = new Intent(this, MafiadayActivity.class);
        intent.putExtra(MafiadayActivity.EXTRA_PLAYERS, new ArrayList<>(getAlivePlayers()));
        intent.putExtra(MafiadayActivity.EXTRA_ROUND, round);
        intent.putExtra(MafiadayActivity.EXTRA_NIGHT_RESULT, result);
        intent.putExtra(MafiadayActivity.EXTRA_IS_HOST, isHost);
        startActivity(intent);
        finish();
    }

    private String buildMafiaTeamInfo(Player current) {
        StringBuilder teamSb = new StringBuilder();
        for (Player p : players) {
            if (p.getRole() == Player.Role.MAFIA) {
                teamSb.append("• ").append(p.getName());
                if (p.getId() == current.getId()) teamSb.append(" (you)");
                teamSb.append("\n");
            }
        }
        return getString(R.string.mafia_role_mafia_team_info, teamSb.toString().trim());
    }

    private List<Player> getAlivePlayers() {
        List<Player> alive = new ArrayList<>();
        for (Player p : players) { if (p.isAlive()) alive.add(p); }
        return alive;
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private static class NightSkyView extends android.view.View {
        private final java.util.Random rnd = new java.util.Random();
        private final android.graphics.Paint paintStar =
                new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.Paint paintMoon =
                new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.Paint paintGlow =
                new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);

        private final float[][] stars = new float[80][5];
        private boolean initialized = false;
        private float moonY = 0f;
        private float time  = 0f;

        NightSkyView(android.content.Context ctx) {
            super(ctx);
        }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            if (!initialized && w > 0) {
                for (float[] s : stars) {
                    s[0] = rnd.nextFloat() * w;
                    s[1] = rnd.nextFloat() * h * 0.85f;
                    s[2] = 0.8f + rnd.nextFloat() * 2.5f;
                    s[3] = rnd.nextFloat() * (float)(Math.PI * 2);
                    s[4] = 0.01f + rnd.nextFloat() * 0.03f;
                }
                moonY = h * 0.12f;
                initialized = true;
            }
        }

        @Override
        protected void onDraw(android.graphics.Canvas canvas) {
            int w = getWidth(), h = getHeight();
            time += 0.016f;

            android.graphics.LinearGradient skyGrad = new android.graphics.LinearGradient(
                    0, 0, 0, h,
                    new int[]{ 0xFF020818, 0xFF060D20, 0xFF0A1028, 0xFF0D0818 },
                    new float[]{ 0f, 0.4f, 0.75f, 1f },
                    android.graphics.Shader.TileMode.CLAMP);
            paintStar.setShader(skyGrad);
            canvas.drawRect(0, 0, w, h, paintStar);
            paintStar.setShader(null);

            float moonX = w * 0.78f;
            float moonR = dp(36);

            paintGlow.setStyle(android.graphics.Paint.Style.FILL);
            for (int g = 4; g >= 1; g--) {
                paintGlow.setColor((0x08FFFDE7));
                paintGlow.setAlpha(8 * g);
                canvas.drawCircle(moonX, moonY, moonR + dp(g * 6), paintGlow);
            }

            paintMoon.setStyle(android.graphics.Paint.Style.FILL);
            paintMoon.setColor(0xFFFFF8DC);
            canvas.drawCircle(moonX, moonY, moonR, paintMoon);

            paintMoon.setColor(0xFFE8E0B0);
            canvas.drawCircle(moonX - moonR*0.3f, moonY + moonR*0.2f, moonR*0.18f, paintMoon);
            canvas.drawCircle(moonX + moonR*0.35f, moonY - moonR*0.25f, moonR*0.12f, paintMoon);
            canvas.drawCircle(moonX + moonR*0.1f, moonY + moonR*0.45f, moonR*0.09f, paintMoon);
            canvas.drawCircle(moonX - moonR*0.5f, moonY - moonR*0.1f, moonR*0.07f, paintMoon);

            paintStar.setStyle(android.graphics.Paint.Style.FILL);
            for (float[] s : stars) {
                s[3] += s[4];
                float twinkle = 0.4f + 0.6f * (float)(Math.sin(s[3]) * 0.5 + 0.5);
                int alpha = (int)(twinkle * 255);

                paintStar.setColor(0xFFE8F0FF);
                paintStar.setAlpha(alpha / 4);
                canvas.drawCircle(s[0], s[1], s[2] * 2.5f, paintStar);

                paintStar.setAlpha(alpha);
                canvas.drawCircle(s[0], s[1], s[2], paintStar);

                if (s[2] > 1.8f) {
                    paintStar.setAlpha(alpha / 2);
                    float sp = s[2] * 3f;
                    canvas.drawLine(s[0]-sp, s[1], s[0]+sp, s[1], paintStar);
                    canvas.drawLine(s[0], s[1]-sp, s[0], s[1]+sp, paintStar);
                }
            }

            postInvalidateDelayed(33);
        }

        private int dp(int d) {
            return Math.round(d * getResources().getDisplayMetrics().density);
        }
    }
}