package com.example.mini_projet;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.mini_projet.mafia.Mafiasetupactivity;
import com.example.mini_projet.mafia.MafiaHostActivity;
import com.example.mini_projet.mafia.MafiaJoinActivity;
import com.example.mini_projet.spyfall.ui.MainActivity;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        LinearLayout cardSpyfall = findViewById(R.id.card_spyfall);
        LinearLayout cardMafia   = findViewById(R.id.card_mafia);

        applyPressEffect(cardSpyfall);
        applyPressEffect(cardMafia);

        // Spyfall — unchanged
        cardSpyfall.setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));

        // Mafia — show mode picker: Pass & Play / Host / Join
        cardMafia.setOnClickListener(v -> showMafiaModeDialog());
    }

    /**
     * Shows a bottom-sheet-style dialog so the player can choose:
     *   🃏  Pass & Play   — everyone shares one device (original flow)
     *   📡  Host Game     — start a server, friends join via network
     *   🔗  Join Game     — discover and connect to a host
     */
    private void showMafiaModeDialog() {
        // Build custom view
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_dark));
        container.setPadding(dp(24), dp(8), dp(24), dp(24));

        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText("CHOOSE MODE");
        tvTitle.setTextSize(11);
        tvTitle.setLetterSpacing(0.18f);
        tvTitle.setTextColor(0xFF8A9BC4);
        tvTitle.setPadding(0, dp(16), 0, dp(20));
        tvTitle.setGravity(Gravity.CENTER);
        container.addView(tvTitle);

        // Pass & Play button
        container.addView(modeButton("🃏  Pass & Play",
                "One device, everyone takes turns",
                v -> startActivity(new Intent(this, Mafiasetupactivity.class))));

        container.addView(modeDivider());

        // Host button
        container.addView(modeButton("📡  Host Game",
                "Start a server — friends join via WiFi",
                v -> startActivity(new Intent(this, MafiaHostActivity.class))));

        container.addView(modeDivider());

        // Join button
        container.addView(modeButton("🔗  Join Game",
                "Find and join a host on the same network",
                v -> startActivity(new Intent(this, MafiaJoinActivity.class))));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(container)
                .setCancelable(true)
                .create();

        // Re-wire buttons so they also dismiss the dialog
        String[] modes = {"🃏  Pass & Play", "📡  Host Game", "🔗  Join Game"};
        Class<?>[] targets = {Mafiasetupactivity.class, MafiaHostActivity.class, MafiaJoinActivity.class};
        container.removeAllViews();
        container.addView(tvTitle);

        for (int i = 0; i < modes.length; i++) {
            if (i > 0) container.addView(modeDivider());
            final Intent intent = new Intent(this, targets[i]);
            String desc = i == 0 ? "One device, everyone takes turns"
                    : i == 1 ? "Start a server — friends join via WiFi"
                    : "Find and join a host on the same network";
            container.addView(modeButton(modes[i], desc, v -> {
                dialog.dismiss();
                startActivity(intent);
            }));
        }

        dialog.show();
    }

    private LinearLayout modeButton(String title, String subtitle, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(8), dp(18), dp(8), dp(18));
        row.setClickable(true);
        row.setFocusable(true);
        row.setBackground(null);
        row.setOnClickListener(listener);

        // Ripple on press
        row.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN)
                row.setAlpha(0.6f);
            else if (event.getAction() == MotionEvent.ACTION_UP
                    || event.getAction() == MotionEvent.ACTION_CANCEL)
                row.setAlpha(1f);
            return false;
        });

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(16);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(0xFFF0F4FF);
        tvTitle.setPadding(0, 0, 0, dp(4));
        row.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText(subtitle);
        tvSub.setTextSize(12);
        tvSub.setTextColor(0xFF8A9BC4);
        row.addView(tvSub);

        return row;
    }

    private View modeDivider() {
        View d = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        d.setLayoutParams(lp);
        d.setBackgroundColor(0x208A9BC4);
        return d;
    }

    private void applyPressEffect(View v) {
        v.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    view.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).start();
                    break;
                case MotionEvent.ACTION_UP:
                    view.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
                    view.performClick();
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
                    break;
            }
            return false;
        });
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}