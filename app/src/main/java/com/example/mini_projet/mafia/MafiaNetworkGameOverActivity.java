package com.example.mini_projet.mafia;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.mini_projet.R;

/** Final screen for network clients — shows who won. */
public class MafiaNetworkGameOverActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String title   = getIntent().getStringExtra("title");
        String message = getIntent().getStringExtra("message");
        if (title   == null) title   = "Game Over";
        if (message == null) message = "";
        buildUi(title, message);
    }

    private void buildUi(String title, String message) {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_dark));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(80), dp(28), dp(28));
        scroll.addView(root);

        // Emoji
        TextView tvEmoji = new TextView(this);
        tvEmoji.setTextSize(56);
        tvEmoji.setGravity(Gravity.CENTER);
        tvEmoji.setText(title.contains("Town") ? "🎉" : title.contains("Mafia") ? "😈" : "🏁");
        LinearLayout.LayoutParams eLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        eLp.bottomMargin = dp(16);
        tvEmoji.setLayoutParams(eLp);
        root.addView(tvEmoji);

        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(24);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setTextColor(title.contains("Town") ? 0xFF38B2AC : 0xFFE53E3E);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        tLp.bottomMargin = dp(16);
        tvTitle.setLayoutParams(tLp);
        root.addView(tvTitle);

        // Message
        TextView tvMsg = new TextView(this);
        tvMsg.setText(message);
        tvMsg.setTextSize(15);
        tvMsg.setTextColor(0xFF8A9BC4);
        tvMsg.setGravity(Gravity.CENTER);
        tvMsg.setLineSpacing(0, 1.5f);
        LinearLayout.LayoutParams mLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        mLp.bottomMargin = dp(48);
        tvMsg.setLayoutParams(mLp);
        root.addView(tvMsg);

        // Main menu button
        TextView btnMenu = new TextView(this);
        btnMenu.setText("MAIN MENU");
        btnMenu.setTextSize(15);
        btnMenu.setTypeface(null, Typeface.BOLD);
        btnMenu.setTextColor(ContextCompat.getColor(this, R.color.bg_dark));
        btnMenu.setBackgroundResource(R.drawable.bg_button_gold);
        btnMenu.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56));
        btnMenu.setLayoutParams(btnLp);
        btnMenu.setOnClickListener(v -> {
            startActivity(new Intent(this, com.example.mini_projet.HomeActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            finish();
        });
        root.addView(btnMenu);

        setContentView(scroll);
    }

    private int dp(int d) { return Math.round(d * getResources().getDisplayMetrics().density); }
}