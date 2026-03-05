package com.example.mini_projet.spyfall.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mini_projet.R;
import com.example.mini_projet.spyfall.game.GameEngine;
import com.example.mini_projet.spyfall.game.GameState;
import com.example.mini_projet.spyfall.game.WordList;

import java.util.ArrayList;
import java.util.List;

public class ThemePickerFragment extends Fragment {

    private final GameEngine            engine         = GameEngine.getInstance();
    private final List<WordList.Theme>  selected       = new ArrayList<>();
    private final List<View>            tileViews      = new ArrayList<>();

    public ThemePickerFragment() { super(R.layout.fragment_theme_picker); }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view == null) return null;

        GridLayout  grid       = view.findViewById(R.id.grid_themes);
        TextView    tvCount    = view.findViewById(R.id.tv_selected_count);
        Button      btnAll     = view.findViewById(R.id.btn_select_all);
        Button      btnStart   = view.findViewById(R.id.btn_start);

        WordList.Theme[] themes = WordList.Theme.values();
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int columns     = 3;
        int tilePx      = (screenWidth - dpToPx(24)) / columns;

        for (WordList.Theme theme : themes) {
            View tile = inflater.inflate(R.layout.item_theme_card, grid, false);

            TextView tvEmoji = tile.findViewById(R.id.tv_theme_emoji);
            TextView tvName  = tile.findViewById(R.id.tv_theme_name);
            TextView tvWc    = tile.findViewById(R.id.tv_theme_count);
            TextView tvCheck = tile.findViewById(R.id.tv_check);

            tvEmoji.setText(theme.emoji);
            tvName.setText(theme.label);
            tvWc.setText(WordList.getWordsForTheme(theme).length + " words");

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width  = tilePx;
            lp.height = tilePx;
            lp.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
            tile.setLayoutParams(lp);

            tile.setOnClickListener(v -> {
                if (selected.contains(theme)) {
                    selected.remove(theme);
                    setTileSelected(tile, tvCheck, false);
                } else {
                    selected.add(theme);
                    setTileSelected(tile, tvCheck, true);
                }
                updateCountLabel(tvCount);
            });

            tileViews.add(tile);
            grid.addView(tile);
        }

        btnAll.setOnClickListener(v -> {
            if (selected.size() == themes.length) {
                selected.clear();
                for (int i = 0; i < tileViews.size(); i++) {
                    View tile = tileViews.get(i);
                    setTileSelected(tile, tile.findViewById(R.id.tv_check), false);
                }
                btnAll.setText("ALL");
            } else {
                selected.clear();
                for (int i = 0; i < themes.length; i++) {
                    selected.add(themes[i]);
                    View tile = tileViews.get(i);
                    setTileSelected(tile, tile.findViewById(R.id.tv_check), true);
                }
                btnAll.setText("NONE");
            }
            updateCountLabel(tvCount);
        });

        btnStart.setOnClickListener(v -> {
            if (selected.isEmpty()) {
                Toast.makeText(requireContext(),
                        "Pick at least one theme!", Toast.LENGTH_SHORT).show();
                return;
            }
            engine.setSelectedThemes(new ArrayList<>(selected));
            if (engine.startGame()) {
                if (engine.getMode() == GameEngine.Mode.HOST
                        && engine.getHostServer() != null) {
                    engine.getHostServer().sendStartGame();
                }
                ((MainActivity) requireActivity()).navigateTo(engine.getGameState());
            }
        });

        updateCountLabel(tvCount);
        return view;
    }

    private void setTileSelected(View tile, TextView check, boolean on) {
        tile.setBackgroundResource(on ? R.drawable.bg_card_selected : R.drawable.bg_card);
        check.setVisibility(on ? View.VISIBLE : View.INVISIBLE);
        TextView tvName = tile.findViewById(R.id.tv_theme_name);
        if (tvName != null) tvName.setTextColor(on ? 0xFFF0B429 : 0xFFF0F4FF);
        TextView tvCount = tile.findViewById(R.id.tv_theme_count);
        if (tvCount != null) tvCount.setTextColor(on ? 0xFF7A5C14 : 0xFF8A9BC4);
    }

    private void updateCountLabel(TextView tvCount) {
        int n = selected.size();
        if (n == 0) {
            tvCount.setText("Select at least one theme");
            tvCount.setTextColor(0xFFE53E3E);
        } else if (n == WordList.Theme.values().length) {
            tvCount.setText("All themes selected — " + WordList.totalWords() + " words");
            tvCount.setTextColor(0xFF38B2AC);
        } else {
            int wordCount = 0;
            for (WordList.Theme t : selected)
                wordCount += WordList.getWordsForTheme(t).length;
            tvCount.setText(n + " theme" + (n > 1 ? "s" : "") + " — " + wordCount + " words");
            tvCount.setTextColor(0xFFF0B429);
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}