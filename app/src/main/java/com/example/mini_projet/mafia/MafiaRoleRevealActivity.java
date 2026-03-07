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

/**
 * Pass-and-play role reveal + night actions.
 *
 * When running as host (is_host=true in Intent extras):
 *   - After all night actions are done → resolves night → broadcasts
 *     NIGHT_RESULT to all clients so they navigate to their night result screen
 *   - Then broadcasts STATE:DAY so clients go to the day/voting screen
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

        // Add night sky directly into the XML root FrameLayout at index 0
        // (using android.R.id.content would place it behind the system window)
        android.widget.FrameLayout skyRoot =
                (android.widget.FrameLayout) findViewById(R.id.root_night_sky);
        NightSkyView nightSky = new NightSkyView(this);
        nightSky.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
        skyRoot.addView(nightSky, 0); // index 0 = behind layout_lock and scroll_role_visible

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
                    // First reveal only → show role animation
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
                        btn_show_role.setText("👁  HOLD AGAIN TO RE-CHECK");
                        List<Player> alive = getAlivePlayers();
                        if (currentIndex < alive.size()) {
                            switch (alive.get(currentIndex).getRole()) {
                                case MAFIA:
                                    btn_seen_role.setText("🧛  Who do you want to kill?");
                                    break;
                                case DOCTOR:
                                    btn_seen_role.setText("🧑‍⚕️  Who do you want to protect?");
                                    break;
                                case DETECTIVE:
                                    btn_seen_role.setText("🕵️‍♀️  Who do you suspect?");
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

    // ── Role reveal animation ─────────────────────────────────────────────────
    private void showRoleAnimation(Player.Role role) {
        int bgColor, accentColor;
        String emoji, title;
        switch (role) {
            case MAFIA:
                bgColor     = 0xEE1A0000;
                accentColor = 0xFFFF1111;
                emoji       = "🧛";
                title       = "YOU ARE MAFIA";
                break;
            case DOCTOR:
                bgColor     = 0xEE001A08;
                accentColor = 0xFF2ECC71;
                emoji       = "🧑‍⚕️";
                title       = "YOU ARE THE DOCTOR";
                break;
            case DETECTIVE:
                bgColor     = 0xEE000D1A;
                accentColor = 0xFF3B9EFF;
                emoji       = "🕵️‍♀️";
                title       = "YOU ARE THE DETECTIVE";
                break;
            default: // CIVILIAN
                bgColor     = 0xEE0A0A18;
                accentColor = 0xFFB0BEC5;
                emoji       = "👤";
                title       = "YOU ARE A CIVILIAN";
                break;
        }

        android.widget.FrameLayout overlay = new android.widget.FrameLayout(this);
        overlay.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
        overlay.setBackgroundColor(bgColor);
        overlay.setElevation(dp(100));

        // Particle layer
        RoleParticleView particles = new RoleParticleView(this, role, accentColor);
        overlay.addView(particles, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));

        // Center content
        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);
        overlay.addView(center, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));

        // Big emoji
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

        // Role title
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

        // Emoji: bounce in
        tvEmoji.animate()
                .scaleX(1.2f).scaleY(1.2f).alpha(1f)
                .setDuration(350)
                .setInterpolator(new android.view.animation.OvershootInterpolator(4f))
                .withEndAction(() -> tvEmoji.animate()
                        .scaleX(1f).scaleY(1f).setDuration(150).start())
                .start();

        // Title: slide up + fade in
        tvTitle.animate()
                .translationY(0f).alpha(1f)
                .setStartDelay(200)
                .setDuration(400)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        // Background pulse flash
        android.animation.ObjectAnimator pulse = android.animation.ObjectAnimator
                .ofArgb(overlay, "backgroundColor",
                        bgColor, (accentColor & 0x00FFFFFF) | 0x55000000, bgColor);
        pulse.setDuration(700);
        pulse.setStartDelay(150);
        pulse.start();

        // Auto-dismiss after 1.8s
        overlay.postDelayed(() -> overlay.animate()
                .alpha(0f).setDuration(400)
                .withEndAction(() -> root.removeView(overlay))
                .start(), 1800);

        // Tap to dismiss early
        overlay.setOnClickListener(vv -> {
            overlay.removeCallbacks(null);
            overlay.animate().alpha(0f).setDuration(200)
                    .withEndAction(() -> root.removeView(overlay)).start();
        });
    }

    // ── Particle view ─────────────────────────────────────────────────────────
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
                    p[2] = 3f + rnd.nextFloat() * 6f;   // speed
                    p[3] = 8f + rnd.nextFloat() * 14f;  // size
                    p[4] = 0.4f + rnd.nextFloat() * 0.6f; // alpha
                    p[5] = -2f + rnd.nextFloat() * 4f;  // drift
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
                        // Blood drops
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
                        // Green cross / plus signs
                        paint.setStyle(android.graphics.Paint.Style.FILL);
                        float s = p[3] * 0.28f;
                        canvas.drawRect(p[0]-s, p[1]-p[3]*0.5f,
                                p[0]+s, p[1]+p[3]*0.5f, paint);
                        canvas.drawRect(p[0]-p[3]*0.5f, p[1]-s,
                                p[0]+p[3]*0.5f, p[1]+s, paint);
                        break;
                    case DETECTIVE:
                        // Hollow circles (lens flares)
                        paint.setStyle(android.graphics.Paint.Style.STROKE);
                        paint.setStrokeWidth(2.5f);
                        canvas.drawCircle(p[0], p[1], p[3] * 0.5f, paint);
                        break;
                    default:
                        // Civilian — soft twinkling star dots floating up
                        paint.setStyle(android.graphics.Paint.Style.FILL);
                        canvas.drawCircle(p[0], p[1], p[3] * 0.4f, paint);
                        paint.setAlpha((int)(p[4] * 100));
                        float sp = p[3] * 1.3f;
                        canvas.drawLine(p[0]-sp, p[1], p[0]+sp, p[1], paint);
                        canvas.drawLine(p[0], p[1]-sp, p[0], p[1]+sp, paint);
                        p[1] -= p[2]; // civilians float UP instead of falling
                        p[0] += p[5];
                        if (p[1] < -p[3]) { p[1] = h + p[3]; p[0] = rnd.nextFloat() * w; }
                        if (p[0] < -p[3] || p[0] > w + p[3]) p[5] = -p[5];
                        continue; // skip the normal fall logic below
                }

                p[1] += p[2];  // fall
                p[0] += p[5];  // drift
                if (p[1] > h + p[3]) { p[1] = -p[3]; p[0] = rnd.nextFloat() * w; }
                if (p[0] < -p[3] || p[0] > w + p[3]) p[5] = -p[5];
            }
            postInvalidateDelayed(16); // 60fps
        }
    }


    private void fillRoleContent(Player p) {
        switch (p.getRole()) {
            case MAFIA:
                tv_role_emoji.setText("🧛");
                tv_role_name_label.setText("MAFIA");
                tv_role_description.setText(buildMafiaTeamInfo(p));
                break;
            case DOCTOR:
                tv_role_emoji.setText("🧑‍⚕️");
                tv_role_name_label.setText("DOCTOR");
                tv_role_description.setText("Each night, choose one player to protect.\nIf the Mafia targets them, they survive.\n\nYou may protect yourself.");
                break;
            case DETECTIVE:
                tv_role_emoji.setText("🕵️‍♀️");
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
                showActionPanel("🧛  Choose a player to eliminate tonight:", p);
                break;
            case DOCTOR:
                showActionPanel("🧑‍⚕️  Choose a player to protect tonight:", p);
                break;
            case DETECTIVE:
                showActionPanel("🕵️‍♀️  Choose a player to investigate:", p);
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
        List<Player> alive = getAlivePlayers();
        for (Player target : alive) {
            if (actor.getRole() == Player.Role.MAFIA
                    && target.getRole() == Player.Role.MAFIA) continue;
            // Mafia and Detective cannot target themselves; Doctor can self-protect
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
                if (targetKill == null) {
                    // First Mafia to choose
                    targetKill = actionSelection;
                } else if (targetKill.getId() != actionSelection.getId()) {
                    // Second Mafia picked a DIFFERENT target — random 50/50
                    targetKill = (Math.random() < 0.5) ? targetKill : actionSelection;
                }
                // If same target — no change needed
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
        // Play cinematic animation first, then show dialog after it finishes
        showInvestigationAnimation(suspectName, isMafia, () -> showDetectiveDialog(suspectName, isMafia));
    }

    // ── Investigation cinematic animation ─────────────────────────────────────
    private void showInvestigationAnimation(String suspectName, boolean isMafia, Runnable onDone) {
        android.view.ViewGroup root = findViewById(android.R.id.content);

        android.widget.FrameLayout overlay = new android.widget.FrameLayout(this);
        overlay.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
        overlay.setBackgroundColor(isMafia ? 0xFF0D0000 : 0xFF001A08);
        overlay.setElevation(dp(200));

        // Particle burst
        InvestigationParticleView particles = new InvestigationParticleView(this, isMafia);
        overlay.addView(particles, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));

        // Center content
        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);
        overlay.addView(center, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));

        // Magnifying glass zooms in
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

        // Result icon appears after lens
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

        // Verdict text
        TextView tvVerdict = new TextView(this);
        tvVerdict.setText(isMafia ? "MAFIA FOUND!" : "INNOCENT");
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

        // Suspect name
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

        // ── Animation sequence ────────────────────────────────────────────────

        // 1. Lens zooms in (0ms)
        tvLens.animate().scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(400)
                .setInterpolator(new android.view.animation.OvershootInterpolator(3f))
                .start();

        // 2. Lens shakes (400ms) — then result icon bursts in
        overlay.postDelayed(() -> {
            android.animation.ObjectAnimator shake = android.animation.ObjectAnimator
                    .ofFloat(tvLens, "translationX", 0f, -18f, 18f, -14f, 14f, -8f, 8f, 0f);
            shake.setDuration(400);
            shake.start();
        }, 400);

        // 3. Background flash + result icon bursts in, lens fades out (800ms)
        overlay.postDelayed(() -> {
            int flashColor = isMafia ? 0xAAFF0000 : 0xAA00FF88;
            android.animation.ObjectAnimator flash = android.animation.ObjectAnimator
                    .ofArgb(overlay, "backgroundColor",
                            isMafia ? 0xFF0D0000 : 0xFF001A08,
                            flashColor,
                            isMafia ? 0xFF0D0000 : 0xFF001A08);
            flash.setDuration(500);
            flash.start();

            // Fade OUT the lens
            tvLens.animate().alpha(0f).scaleX(0.5f).scaleY(0.5f)
                    .setDuration(250).start();

            // Burst IN the result icon
            tvResult.animate().scaleX(1.3f).scaleY(1.3f).alpha(1f)
                    .setDuration(300)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(5f))
                    .withEndAction(() -> tvResult.animate()
                            .scaleX(1f).scaleY(1f).setDuration(150).start())
                    .start();
        }, 800);

        // 4. Verdict slides up (1100ms)
        overlay.postDelayed(() -> {
            tvVerdict.animate().alpha(1f).translationY(0f)
                    .setDuration(350)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
            tvName.animate().alpha(1f).setDuration(300).setStartDelay(150).start();
        }, 1100);

        // 5. Dismiss + show dialog (2400ms)
        overlay.postDelayed(() -> {
            overlay.animate().alpha(0f).setDuration(400)
                    .withEndAction(() -> {
                        root.removeView(overlay);
                        onDone.run();
                    }).start();
        }, 2400);
    }

    // ── Investigation particles ────────────────────────────────────────────────
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
                    // Red blood drops falling
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
                    // Green sparkle stars for innocent
                    paint.setColor(0xFF2ECC71);
                    paint.setAlpha((int)(p[4] * 255));
                    paint.setStyle(android.graphics.Paint.Style.FILL);
                    float r = p[3] * 0.5f;
                    // Simple 4-point star
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

        // Big result icon
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
                : suspectName + " is not part of the Mafia.\n\n😇  Keep investigating.\nDon't reveal it directly during the day.");
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
            result = targetKill.getName() + " was targeted but saved by the Doctor! 🧑‍⚕️";
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

    // ── Night sky background — stars + moon ───────────────────────────────────
    private static class NightSkyView extends android.view.View {
        private final java.util.Random rnd = new java.util.Random();
        private final android.graphics.Paint paintStar =
                new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.Paint paintMoon =
                new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.Paint paintGlow =
                new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);

        // Each star: x, y, radius, twinkle phase, twinkle speed
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
                    s[0] = rnd.nextFloat() * w;               // x
                    s[1] = rnd.nextFloat() * h * 0.85f;       // y (upper 85%)
                    s[2] = 0.8f + rnd.nextFloat() * 2.5f;     // radius
                    s[3] = rnd.nextFloat() * (float)(Math.PI * 2); // phase
                    s[4] = 0.01f + rnd.nextFloat() * 0.03f;   // twinkle speed
                }
                moonY = h * 0.12f;
                initialized = true;
            }
        }

        @Override
        protected void onDraw(android.graphics.Canvas canvas) {
            int w = getWidth(), h = getHeight();
            time += 0.016f;

            // ── Sky gradient background ────────────────────────────────────────
            android.graphics.LinearGradient skyGrad = new android.graphics.LinearGradient(
                    0, 0, 0, h,
                    new int[]{ 0xFF020818, 0xFF060D20, 0xFF0A1028, 0xFF0D0818 },
                    new float[]{ 0f, 0.4f, 0.75f, 1f },
                    android.graphics.Shader.TileMode.CLAMP);
            paintStar.setShader(skyGrad);
            canvas.drawRect(0, 0, w, h, paintStar);
            paintStar.setShader(null);

            // ── Moon ──────────────────────────────────────────────────────────
            float moonX = w * 0.78f;
            float moonR = dp(36);

            // Moon outer glow
            paintGlow.setStyle(android.graphics.Paint.Style.FILL);
            for (int g = 4; g >= 1; g--) {
                paintGlow.setColor((0x08FFFDE7));
                paintGlow.setAlpha(8 * g);
                canvas.drawCircle(moonX, moonY, moonR + dp(g * 6), paintGlow);
            }

            // Moon body — full moon
            paintMoon.setStyle(android.graphics.Paint.Style.FILL);
            paintMoon.setColor(0xFFFFF8DC);
            canvas.drawCircle(moonX, moonY, moonR, paintMoon);

            // Moon surface craters for realism
            paintMoon.setColor(0xFFE8E0B0);
            canvas.drawCircle(moonX - moonR*0.3f, moonY + moonR*0.2f, moonR*0.18f, paintMoon);
            canvas.drawCircle(moonX + moonR*0.35f, moonY - moonR*0.25f, moonR*0.12f, paintMoon);
            canvas.drawCircle(moonX + moonR*0.1f, moonY + moonR*0.45f, moonR*0.09f, paintMoon);
            canvas.drawCircle(moonX - moonR*0.5f, moonY - moonR*0.1f, moonR*0.07f, paintMoon);

            // ── Stars ─────────────────────────────────────────────────────────
            paintStar.setStyle(android.graphics.Paint.Style.FILL);
            for (float[] s : stars) {
                s[3] += s[4]; // advance twinkle phase
                float twinkle = 0.4f + 0.6f * (float)(Math.sin(s[3]) * 0.5 + 0.5);
                int alpha = (int)(twinkle * 255);

                // Star glow
                paintStar.setColor(0xFFE8F0FF);
                paintStar.setAlpha(alpha / 4);
                canvas.drawCircle(s[0], s[1], s[2] * 2.5f, paintStar);

                // Star core
                paintStar.setAlpha(alpha);
                canvas.drawCircle(s[0], s[1], s[2], paintStar);

                // Cross sparkle on bigger stars
                if (s[2] > 1.8f) {
                    paintStar.setAlpha(alpha / 2);
                    float sp = s[2] * 3f;
                    canvas.drawLine(s[0]-sp, s[1], s[0]+sp, s[1], paintStar);
                    canvas.drawLine(s[0], s[1]-sp, s[0], s[1]+sp, paintStar);
                }
            }

            postInvalidateDelayed(33); // ~30fps is smooth enough for background
        }

        private int dp(int d) {
            return Math.round(d * getResources().getDisplayMetrics().density);
        }
    }
}