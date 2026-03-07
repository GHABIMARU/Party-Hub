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
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;

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
        TextView btnChangeLanguage = findViewById(R.id.btn_change_language);

        applyPressEffect(cardSpyfall);
        applyPressEffect(cardMafia);
        if (btnChangeLanguage != null) applyPressEffect(btnChangeLanguage);

        // Spyfall
        cardSpyfall.setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));

        // Mafia — show mode picker
        cardMafia.setOnClickListener(v -> showMafiaModeDialog());

        // Languages
        if (btnChangeLanguage != null) {
            btnChangeLanguage.setOnClickListener(v -> showLanguageDialog());
        }
    }

    /**
     * Shows custom-styled language picker
     */
    private void showLanguageDialog() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_dark));
        container.setPadding(dp(24), dp(8), dp(24), dp(24));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(getString(R.string.home_languages));
        tvTitle.setTextSize(11);
        tvTitle.setLetterSpacing(0.18f);
        tvTitle.setTextColor(0xFF8A9BC4);
        tvTitle.setPadding(0, dp(16), 0, dp(20));
        tvTitle.setGravity(Gravity.CENTER);
        container.addView(tvTitle);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(container)
                .setCancelable(true)
                .create();

        String[] langs = {"English", "Español", "Français", "العربية"};
        String[] codes = {"en", "es", "fr", "ar"};

        for (int i = 0; i < langs.length; i++) {
            if (i > 0) container.addView(modeDivider());
            final String code = codes[i];
            container.addView(modeButton(langs[i], "", v -> {
                dialog.dismiss();
                setLocale(code);
            }));
        }

        dialog.show();
    }

    private void setLocale(String langCode) {
        LocaleListCompat appLocales = LocaleListCompat.forLanguageTags(langCode);
        AppCompatDelegate.setApplicationLocales(appLocales);
    }

    /**
     * Shows a bottom-sheet-style dialog so the player can choose:
     *   🃏  Pass & Play   — everyone shares one device
     *   📡  Host Game     — start a server, friends join via network
     *   🔗  Join Game     — discover and connect to a host
     */
    private void showMafiaModeDialog() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_dark));
        container.setPadding(dp(24), dp(8), dp(24), dp(24));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(getString(R.string.mafia_mode_choose));
        tvTitle.setTextSize(11);
        tvTitle.setLetterSpacing(0.18f);
        tvTitle.setTextColor(0xFF8A9BC4);
        tvTitle.setPadding(0, dp(16), 0, dp(20));
        tvTitle.setGravity(Gravity.CENTER);
        container.addView(tvTitle);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(container)
                .setCancelable(true)
                .create();

        String[] modes = {
                getString(R.string.mafia_mode_pass_play),
                getString(R.string.mafia_mode_host),
                getString(R.string.mafia_mode_join)
        };
        String[] descs = {
                getString(R.string.mafia_mode_pass_play_desc),
                getString(R.string.mafia_mode_host_desc),
                getString(R.string.mafia_mode_join_desc)
        };
        Class<?>[] targets = {Mafiasetupactivity.class, MafiaHostActivity.class, MafiaJoinActivity.class};

        for (int i = 0; i < modes.length; i++) {
            if (i > 0) container.addView(modeDivider());
            final Intent intent = new Intent(this, targets[i]);
            container.addView(modeButton(modes[i], descs[i], v -> {
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
        row.setOnClickListener(listener);

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
        if (subtitle != null && !subtitle.isEmpty()) {
            tvTitle.setPadding(0, 0, 0, dp(4));
        }
        row.addView(tvTitle);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView tvSub = new TextView(this);
            tvSub.setText(subtitle);
            tvSub.setTextSize(12);
            tvSub.setTextColor(0xFF8A9BC4);
            row.addView(tvSub);
        }

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