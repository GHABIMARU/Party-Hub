package com.example.mini_projet;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;

import com.example.mini_projet.mafia.Mafiasetupactivity;
import com.example.mini_projet.mafia.MafiaHostActivity;
import com.example.mini_projet.mafia.MafiaJoinActivity;
import com.example.mini_projet.spyfall.ui.MainActivity;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private String selectedLangCode = "en";

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

        cardSpyfall.setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));

        cardMafia.setOnClickListener(v -> showMafiaModeDialog());

        if (btnChangeLanguage != null) {
            btnChangeLanguage.setOnClickListener(v -> showLanguageDialog());
        }

        // Get current locale
        LocaleListCompat current = AppCompatDelegate.getApplicationLocales();
        if (!current.isEmpty()) {
            selectedLangCode = current.get(0).getLanguage();
        }
    }

    private void showLanguageDialog() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_language_picker);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        LinearLayout container = dialog.findViewById(R.id.lang_container);
        Button btnContinue = dialog.findViewById(R.id.btn_continue);
        TextView tvTitle = dialog.findViewById(R.id.tv_lang_title);
        
        String[] names = {"العربية", "Français", "Español", "English"};
        String[] subs  = {"اختر لغتك", "Choisissez votre langue", "Elige tu idioma", "Choose your language"};
        String[] flags = {"🇲🇦", "🇫🇷", "🇪🇸", "🇬🇧"};
        String[] codes = {"ar", "fr", "es", "en"};
        int[] colors   = {0xFFE53E3E, 0xFF448AFF, 0xFFF0B429, 0xFF38B2AC};

        List<View> itemViews = new ArrayList<>();
        final String[] currentSelected = {selectedLangCode};

        for (int i = 0; i < codes.length; i++) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_language, container, false);
            TextView tvName = itemView.findViewById(R.id.tv_lang_name);
            TextView tvSub  = itemView.findViewById(R.id.tv_lang_sub);
            TextView tvFlag = itemView.findViewById(R.id.tv_flag);
            View bar = itemView.findViewById(R.id.selection_bar);
            ImageView check = itemView.findViewById(R.id.iv_check);
            View radioOuter = itemView.findViewById(R.id.radio_outer);

            tvName.setText(names[i]);
            tvSub.setText(subs[i]);
            tvFlag.setText(flags[i]);
            bar.setBackgroundColor(colors[i]);

            final int index = i;
            itemView.setOnClickListener(v -> {
                currentSelected[0] = codes[index];
                updateSelection(itemViews, codes, currentSelected[0], colors, btnContinue, tvTitle, subs);
            });

            container.addView(itemView);
            itemViews.add(itemView);
        }

        updateSelection(itemViews, codes, currentSelected[0], colors, btnContinue, tvTitle, subs);

        btnContinue.setOnClickListener(v -> {
            selectedLangCode = currentSelected[0];
            setLocale(selectedLangCode);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateSelection(List<View> views, String[] codes, String selected, int[] colors, Button btnContinue, TextView tvTitle, String[] subs) {
        for (int i = 0; i < views.size(); i++) {
            View v = views.get(i);
            boolean isSel = codes[i].equals(selected);
            
            v.findViewById(R.id.selection_bar).setVisibility(isSel ? View.VISIBLE : View.INVISIBLE);
            v.findViewById(R.id.iv_check).setVisibility(isSel ? View.VISIBLE : View.GONE);
            
            TextView tvName = v.findViewById(R.id.tv_lang_name);
            TextView tvSub = v.findViewById(R.id.tv_lang_sub);
            View radioOuter = v.findViewById(R.id.radio_outer);
            
            if (isSel) {
                v.setBackgroundResource(R.drawable.bg_lang_card);
                tvName.setTextColor(colors[i]);
                radioOuter.setBackgroundTintList(ColorStateList.valueOf(colors[i]));
                radioOuter.setAlpha(1.0f);
                btnContinue.setBackgroundTintList(ColorStateList.valueOf(colors[i]));
                
                if (tvTitle != null) {
                    tvTitle.setText(subs[i]);
                }

                // Update Continue text based on language
                if (codes[i].equals("ar")) btnContinue.setText("متابعة ←");
                else if (codes[i].equals("fr")) btnContinue.setText("CONTINUER →");
                else if (codes[i].equals("es")) btnContinue.setText("CONTINUAR →");
                else btnContinue.setText("CONTINUE →");

            } else {
                v.setBackgroundResource(R.drawable.bg_lang_card);
                tvName.setTextColor(Color.WHITE);
                radioOuter.setBackgroundTintList(null);
                radioOuter.setAlpha(0.3f);
            }
        }
    }

    private void setLocale(String langCode) {
        LocaleListCompat appLocales = LocaleListCompat.forLanguageTags(langCode);
        AppCompatDelegate.setApplicationLocales(appLocales);
    }

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
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
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
